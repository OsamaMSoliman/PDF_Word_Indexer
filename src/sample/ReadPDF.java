package sample;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class ReadPDF implements Runnable {
    public static int idPdfCounter;
    public static int MaxNumOfThreads;
    public static Semaphore semaphore;
    public static BlockingQueue<String> PathQ;

    public static void main(String[] args) throws InterruptedException {
        MaxNumOfThreads = 10;
        ReadPDF.Initialize();
        ExecutorService executor = Executors.newFixedThreadPool(MaxNumOfThreads);
        for (int i = 0; i < MaxNumOfThreads; i++) {
            executor.submit(new ReadPDF());
        }
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.HOURS);
        System.out.println("Exit!");
    }

    public static void Initialize() {
        semaphore = new Semaphore(MaxNumOfThreads);
        PathQ = new LinkedBlockingQueue<>();
        try (Stream<Path> paths = Files.walk(Paths.get("./src/root"))) {
//            paths.filter(Files::isRegularFile).forEach(System.out::println);
            paths.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".pdf"))
                    .forEach(p -> {
                        System.out.println("file found: " + p);
                        PathQ.add(p.toString());
                        idPdfCounter++;
                    });
        } catch (IOException e) {
            e.printStackTrace();
            Logger.getLogger(ReadPDF.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    private void ExtractTextToDB(String filepath, int pdfId) throws IOException {
        try (PDDocument pdDoc = PDDocument.load(new File(filepath))) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            for (int i = 0; i < pdDoc.getNumberOfPages(); i++) {
                pdfStripper.setStartPage(i);
                pdfStripper.setEndPage(i);
                for (String txt : pdfStripper.getText(pdDoc).split(" |\\r?\\n")) {
                    if (txt.trim().length() > 3) {
                        System.out.println(txt);
                        DB.getInstance().Insert(new Word(txt, pdfId, i));
                    }
                }
            }
        }
    }

    //Todo: save path, id, state in the db as another table
    //Todo: Log everything in all classes
    //Todo: map text to page and pdf file

    @Override
    public void run() {
        while (true) {
            try {
                semaphore.acquire();
                String localToThread_FilePath;
                int localToThread_PdfId;
                synchronized (PathQ) {
                    if (idPdfCounter-- <= 0)
                        break;
                    localToThread_PdfId = idPdfCounter;
                    localToThread_FilePath = PathQ.remove();
                }
                ExtractTextToDB(localToThread_FilePath, localToThread_PdfId);
            } catch (IOException | InterruptedException e) {
                Logger.getLogger(ReadPDF.class.getName()).log(Level.SEVERE, null, e);
                e.printStackTrace();
            } finally {
                semaphore.release();
            }
        }
        System.out.println(Thread.currentThread().getName() + " Done!");
    }
}
