package sample;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DB {

    // Testing
//    public static void main(String[] args) throws SQLException {
//        Word[] words = {
//                new Word("hello", 0, 0),
//                new Word("hello", 0, 0),
//                new Word("world", 0, 1),
//                new Word("bye", 1, 0)
//        };
//        for (Word w : words)
//            DB.getInstance().InsertDirectly(w);
////          DB.getInstance().InsertToBatch(w);
////        DB.getInstance().NoMoreInserts(true);
//
//        List<Word> resultSet = DB.getInstance().SelectAllWords();
//        for (Word d : resultSet) {
//            System.out.print(ID_COLUMN + " = " + d.get_Id() + ", ");
////            if (DB.getInstance().Delete(d.get_Id()))
////                System.out.println("was deleted!");
//        }
//        DB.getInstance().Shutdown();
//    }

    public static DB getInstance() {
        if (instance == null)
            instance = new DB();
        return instance;
    }

    private static DB instance;
    private Connection connection = null;
    private PreparedStatement preparedStatement;
    private static final int MaxBatchSize = 50;
    public int totalInsertsCount = 0;

    //table name, columns, queries
    //WORDS table
    private static String WORDS_TABLE_NAME = "WordIndex";
    private static String ID_COLUMN = "_id";
    private static String WORD_COLUMN = "Word";
    private static String PDF_ID_COLUMN = "Pdf_Id";
    private static String PAGE_NUM_COLUMN = "Page_Num";
    private static final String createWordsTableQuery = "CREATE TABLE IF NOT EXISTS " + WORDS_TABLE_NAME + "(" +
            ID_COLUMN + "       INTEGER PRIMARY KEY NOT NULL," +
            WORD_COLUMN + "     VARCHAR UNIQUE      NOT NULL," +
            PDF_ID_COLUMN + "   INTEGER             NOT NULL," +
            PAGE_NUM_COLUMN + " INTEGER             NOT NULL);";
    private static final String selectAllWordsQuery = "SELECT * FROM " + WORDS_TABLE_NAME;
    private static final String deleteWordQuery = "DELETE FROM " + WORDS_TABLE_NAME + " WHERE " + ID_COLUMN + "=?;";
    private static final String insertWordQuery = "INSERT INTO " + WORDS_TABLE_NAME + " (" +
            WORD_COLUMN + "," + PDF_ID_COLUMN + "," + PAGE_NUM_COLUMN + ") VALUES(?,?,?);";

    //PDF table
    private static String PDF_TABLE_NAME = "PdfFiles";
    private static String PDF_PATH_COLUMN = "Full_Path";
    private static String PDF_Status_COLUMN = "Status";
    private static final String createPDFTableQuery = "CREATE TABLE IF NOT EXISTS " + PDF_TABLE_NAME + "(" +
            PDF_ID_COLUMN + "       INTEGER PRIMARY KEY NOT NULL," +
            PDF_PATH_COLUMN + "     VARCHAR UNIQUE      NOT NULL," +
            PDF_Status_COLUMN + "   INTEGER             DEFAULT 0);";
    private static final String selectAllPDFsQuery = "SELECT * FROM " + PDF_TABLE_NAME;
    private static final String selectPDFQueryByPath = "SELECT " + PDF_Status_COLUMN +
            " FROM " + PDF_TABLE_NAME + " WHERE " + PDF_PATH_COLUMN + " = ?";
    private static final String selectPDFQueryByID = "SELECT " + PDF_PATH_COLUMN +
            " FROM " + PDF_TABLE_NAME + " WHERE " + PDF_ID_COLUMN + " = ?";
    private static final String insertPFDQuery = "INSERT INTO " + PDF_TABLE_NAME + " (" +
            PDF_ID_COLUMN + "," + PDF_PATH_COLUMN + ") VALUES(?,?);";
    private static final String updatePFDQuery = "UPDATE " + PDF_TABLE_NAME + " SET " +
            PDF_Status_COLUMN + " = 1  WHERE " + PDF_PATH_COLUMN + " = ?;";


    // load the sqlite-JDBC driver using the current class loader
    // create a database connection
    // create table if not exists
    // if the error message is "out of memory",
    // it probably means no database file is found
    private DB() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:Database.db");
            try (Statement statement = connection.createStatement()) {
//                statement.setQueryTimeout(30);  // set timeout to 30 sec.
                statement.executeUpdate(createWordsTableQuery);
                statement.executeUpdate(createPDFTableQuery);
            }
        } catch (ClassNotFoundException | SQLException e) { System.err.println(e.getMessage()); }
    }

    //Shutdown connection
    private void Shutdown() {
        if (connection != null) try { connection.close(); } catch (SQLException e) { e.printStackTrace(); }

    }

    //region INSERT
    public boolean InsertDirectly(Word word) {
        try (PreparedStatement ps = connection.prepareStatement(insertWordQuery)) {
            ps.setString(1, word.Value);
            ps.setInt(2, word.Pdf_Id);
            ps.setInt(3, word.Page_Num);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getErrorCode() == 19)
                System.out.println("Value = [" + word + "] is Duplicated");
            else
                e.printStackTrace();
            return false;
        }
    }

    public boolean InsertDirectly(PdfFile pdfFile) {
        try (PreparedStatement ps = connection.prepareStatement(insertPFDQuery)) {
            ps.setInt(1, pdfFile._Id);
            ps.setString(2, pdfFile.FilePath);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getErrorCode() == 19)
                System.out.println("Value = [" + pdfFile + "] is Duplicated");
            else
                e.printStackTrace();
            return false;
        }
    }

    public void InsertToBatch(Word word) {
        try {
            if (preparedStatement == null || preparedStatement.isClosed()) {
                preparedStatement = connection.prepareStatement(insertWordQuery);
                connection.setAutoCommit(false);
            }
            preparedStatement.setString(1, word.Value);
            preparedStatement.setInt(2, word.Pdf_Id);
            preparedStatement.setInt(3, word.Page_Num);
            preparedStatement.addBatch();
            if (++totalInsertsCount % MaxBatchSize == 0) {
                NoMoreInserts(false);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void NoMoreInserts(boolean closeStatement) {
        if(preparedStatement != null){
            try {
                preparedStatement.executeBatch();
                connection.commit();
            } catch (SQLException e) { e.printStackTrace(); }
            if (closeStatement) {
                try {
                    preparedStatement.close();
                    connection.setAutoCommit(true);
                } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }
    //endregion

    //region SELECT
    public List<Word.WordModel> SelectAllWords() {
        List<Word.WordModel> words = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(selectAllWordsQuery)) {
            while (rs.next()) {
                words.add(new Word(rs.getString(WORD_COLUMN), rs.getInt(PDF_ID_COLUMN), rs.getInt(PAGE_NUM_COLUMN)).setId(rs.getInt(ID_COLUMN)).getModel());
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return words;
    }

    public List<PdfFile.PdfFileModel> SelectAllPdfs() {
        List<PdfFile.PdfFileModel> pdfs = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(selectAllPDFsQuery)) {
            while (rs.next()) {
                pdfs.add(new PdfFile(rs.getInt(PDF_ID_COLUMN), rs.getString(PDF_PATH_COLUMN)).setIsProcessed(rs.getBoolean(PDF_Status_COLUMN)).getModel());
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return pdfs;
    }

    public boolean IsPDFProcessed(String path) {
        try (PreparedStatement ps = connection.prepareStatement(selectPDFQueryByPath)) {
            ps.setString(1, path);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(PDF_Status_COLUMN) == 1;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public String GetPdfPath(int pdfId) {
        try (PreparedStatement ps = connection.prepareStatement(selectPDFQueryByID)) {
            ps.setInt(1, pdfId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString(PDF_PATH_COLUMN);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }
    //endregion

    //region UPDATE
    public void UpdatePDFStatus(String path) {
        try (PreparedStatement ps = connection.prepareStatement(updatePFDQuery)) {
            ps.setString(1, path);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
    //endregion

    //region DELETE
    public boolean Delete(int id) {
        int count = 0;
        try (PreparedStatement statement = connection.prepareStatement(deleteWordQuery)) {
            statement.setInt(1, id);
            count = statement.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
        return count > 0;
    }
    //endregion
}