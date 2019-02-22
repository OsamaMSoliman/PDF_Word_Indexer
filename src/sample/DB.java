package sample;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DB {

    // Testing
    public static void main(String[] args) throws SQLException {
        Word[] words = {
                new Word("hello", 0, 0),
                new Word("world", 0, 1),
                new Word("hello", 0, 0),
                new Word("world", 0, 1),
                new Word("hello", 0, 0),
                new Word("world", 0, 1),
                new Word("hello", 0, 0),
                new Word("world", 0, 1),
                new Word("hello", 0, 0),
                new Word("world", 0, 1),
                new Word("hello", 0, 0),
                new Word("world", 0, 1),
                new Word("bye", 1, 0)
        };
        for (Word d : words)
            DB.getInstance().Insert(d);
        DB.getInstance().NoMoreInserts(true);

        List<Word> resultSet = DB.getInstance().SelectAll();
        for (Word d : resultSet) {
            System.out.print(ID_COLUMN + " = " + d.get_Id() + ", ");
            if (DB.getInstance().Delete(d.get_Id()))
                System.out.println("was deleted!");
        }
        DB.getInstance().Shutdown();
    }

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

    //table name and columns
    private static String TABLE_NAME = "WordIndex";
    private static String ID_COLUMN = "_id";
    private static String WORD_COLUMN = "Word";
    private static String PDF_ID_COLUMN = "Pdf_Id";
    private static String PAGE_NUM_COLUMN = "Page_Num";
    private static final String createTableQuery = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "(" +
            ID_COLUMN + "       INTEGER PRIMARY KEY NOT NULL," +
            WORD_COLUMN + "     VARCHAR             NOT NULL," +
            PDF_ID_COLUMN + "   INTEGER             NOT NULL," +
            PAGE_NUM_COLUMN + " INTEGER             NOT NULL);";
    private static final String selectAllQuery = "SELECT * FROM " + TABLE_NAME;
    private static final String deleteQuery = "DELETE FROM " + TABLE_NAME + " WHERE " + ID_COLUMN + "=?;";
    private static final String insertQuery = "INSERT INTO " + TABLE_NAME + " (" +
            WORD_COLUMN + "," + PDF_ID_COLUMN + "," + PAGE_NUM_COLUMN + ") VALUES(?,?,?);";

    // load the sqlite-JDBC driver using the current class loader
    // create a database connection
    // create table if not exists
    // if the error message is "out of memory",
    // it probably means no database file is found
    private DB() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:sample.db");
            try (Statement statement = connection.createStatement()) {
//                statement.setQueryTimeout(30);  // set timeout to 30 sec.
                statement.executeUpdate(createTableQuery);
            }
        } catch (ClassNotFoundException | SQLException e) { System.err.println(e.getMessage()); }
    }

    //Shutdown connection
    private void Shutdown() {
        if (connection != null) try { connection.close(); } catch (SQLException e) { e.printStackTrace(); }

    }

    //INSERT (apply batching)
    public void Insert(Word word) {
        try {
            if (preparedStatement == null || preparedStatement.isClosed()) {
                preparedStatement = connection.prepareStatement(insertQuery);
                connection.setAutoCommit(false);
            }
            preparedStatement.setString(1, word.word);
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

    //SELECT
    public List<Word> SelectAll() {
        List<Word> words = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(selectAllQuery)) {
            while (rs.next()) {
                words.add(new Word(rs.getString(WORD_COLUMN), rs.getInt(PDF_ID_COLUMN), rs.getInt(PAGE_NUM_COLUMN)).setId(rs.getInt(ID_COLUMN)));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return words;
    }

    //DELETE
    public boolean Delete(int id) {
        int count = 0;
        try (PreparedStatement statement = connection.prepareStatement(deleteQuery)) {
            statement.setInt(1, id);
            count = statement.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
        return count > 0;
    }
}