package sample;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("GUI.fxml"));
        primaryStage.setTitle("PDF INDEXER");
        primaryStage.setScene(new Scene(root, 400, 600));
        primaryStage.setMinHeight(600);
        primaryStage.setMinWidth(400);
        primaryStage.show();
    }

    private static ExecutorService executor;

    public static void main(String[] args) {

        launch(args);
        try {
            if (executor != null)
                executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

//        //TODO: check this code when u come back, it should use the printer, note move it to another function
//        PrintService ps = PrintServiceLookup.lookupDefaultPrintService();
//        DocPrintJob job = ps.createPrintJob();
//        job.addPrintJobListener(new PrintJobAdapter() {
//            public void printDataTransferCompleted(PrintJobEvent event) {
//                System.out.println("data transfer complete");
//            }
//
//            public void printJobNoMoreEvents(PrintJobEvent event) {
//                System.out.println("received no more events");
//            }
//        });
//        FileInputStream fis = new FileInputStream("C:/test.jpg");
//        Doc doc = new SimpleDoc(fis, DocFlavor.INPUT_STREAM.AUTOSENSE, null);
//        // Doc doc=new SimpleDoc(fis, DocFlavor.INPUT_STREAM.JPEG, null);
//        PrintRequestAttributeSet attrib = new HashPrintRequestAttributeSet();
//        attrib.add(new Copies(1));
//        job.print(doc, attrib);

    }

    public static PdfReader[] Run(int numOfThreads) {
        PdfReader[] readers = new PdfReader[numOfThreads];
        PdfReader.Initialize(numOfThreads);
        executor = Executors.newFixedThreadPool(PdfReader.MaxNumOfThreads);
        for (int i = 0; i < PdfReader.MaxNumOfThreads; i++) {
            readers[i] = new PdfReader();
            executor.submit(readers[i]);
        }
        executor.shutdown();
//        System.out.println("Exit!");
        return readers;
    }

    public static void LoadDB(ObservableList<PdfFile.PdfFileModel> PDFsList, ObservableList<Word.WordModel> wordsList) {
        wordsList.addAll(DB.getInstance().SelectAllWords());
        PDFsList.addAll(DB.getInstance().SelectAllPdfs());
    }
}
