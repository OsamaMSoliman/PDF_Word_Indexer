package sample;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPrintable;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class PdfReader implements Runnable {
    public static int idPdfCounter;
    public static int MaxNumOfThreads;
    public static Semaphore semaphore;
    public static BlockingQueue<String> PathQ;

//    public static void main(String[] args) throws InterruptedException {
//        PdfReader.Initialize(10);
//        ExecutorService executor = Executors.newFixedThreadPool(MaxNumOfThreads);
//        for (int i = 0; i < MaxNumOfThreads; i++) {
//            executor.submit(new PdfReader());
//        }
//        executor.shutdown();
//        executor.awaitTermination(1, TimeUnit.HOURS);
//        System.out.println("Exit!");
//    }

    public static void Initialize(int maxNumOfThreads) {
        MaxNumOfThreads = maxNumOfThreads;
        semaphore = new Semaphore(MaxNumOfThreads);
        PathQ = new LinkedBlockingQueue<>();
        try (Stream<Path> paths = Files.walk(Paths.get("./"))) { /*test on: ./src/root*/
//            paths.filter(Files::isRegularFile).forEach(System.out::println);
            paths.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".pdf"))
                    .forEach(p -> {
//                        System.out.println("file found: " + p);
                        if (!DB.getInstance().IsPDFProcessed(p.toString())) {
                            PathQ.add(p.toString());
                            DB.getInstance().InsertDirectly(new PdfFile(++idPdfCounter, p.toString()));
                        }
                    });
            maxPdfCounter = idPdfCounter;
        } catch (IOException e) {
            e.printStackTrace();
            Logger.getLogger(PdfReader.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    private void ExtractTextToDB(String filepath, int pdfId) throws IOException {
        try (PDDocument pdDoc = PDDocument.load(new File(filepath))) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            for (int i = 0; i < pdDoc.getNumberOfPages(); i++) {
                pdfStripper.setStartPage(i);
                pdfStripper.setEndPage(i);
                String[] txt = pdfStripper.getText(pdDoc).split(" |\\r?\\n");
                for (int j = 0; j < txt.length; j++) {
                    if (txt[j].trim().length() > 3) {
//                        System.out.println(txt[j]);
                        DB.getInstance().InsertDirectly(new Word(txt[j], pdfId, i));
                        int finalJ = j;
                        Platform.runLater(() -> wordPercentProperty.set(finalJ / (double) txt.length));
                    }
                }
                Platform.runLater(() -> wordPercentProperty.set(1));
            }
            DB.getInstance().UpdatePDFStatus(filepath);
        }
    }

    //Todo: Log everything in all classes
    public static DoubleProperty pdfPercentProperty = new SimpleDoubleProperty(-1.0);
    public DoubleProperty wordPercentProperty = new SimpleDoubleProperty(-1.0);
    private static int maxPdfCounter;

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
                    localToThread_PdfId = maxPdfCounter - idPdfCounter;
                    localToThread_FilePath = PathQ.remove();
                    Platform.runLater(() -> pdfPercentProperty.set((localToThread_PdfId) / (double) maxPdfCounter));
                }
                ExtractTextToDB(localToThread_FilePath, localToThread_PdfId);
            } catch (IOException | InterruptedException e) {
                Logger.getLogger(PdfReader.class.getName()).log(Level.SEVERE, null, e);
                e.printStackTrace();
            } finally {
                semaphore.release();
            }
        }
        System.out.println(Thread.currentThread().getName() + " Done!");
        if (signalReceiver != null) signalReceiver.signal();
        else System.out.println("signalReceiver = " + signalReceiver);
    }

    private Signal signalReceiver;

    public void setSignalReceiver(Signal signalReceiver) {
        this.signalReceiver = signalReceiver;
    }

    public static BufferedImage GetPdfPage(int pdfId, int pageNum) {
        String pdfFilename = DB.getInstance().GetPdfPath(pdfId);
        if (pdfFilename == null) return null;
        BufferedImage bi = null;
        try (PDDocument p = PDDocument.load(new File(pdfFilename))) {
            PDFRenderer pdfRenderer = new PDFRenderer(p);
            bi = pdfRenderer.renderImageWithDPI(pageNum, 150, ImageType.ARGB);
        } catch (IOException e) { e.printStackTrace(); }
        return bi;
    }

}
