package sample;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable, Signal {

    public TableView<PdfFile.PdfFileModel> PdfTableView;
    public TableColumn<PdfFile.PdfFileModel, Integer> PdfIdColumn;
    public TableColumn<PdfFile.PdfFileModel, String> PdfPathColumn;
    public TableColumn<PdfFile.PdfFileModel, Boolean> PdfStatusColumn;
    private final ObservableList<PdfFile.PdfFileModel> PDFsList = FXCollections.observableArrayList();

    public TableView<Word.WordModel> WordTableView;
    public TableColumn<Word.WordModel, Integer> WordIdColumn;
    public TableColumn<Word.WordModel, String> WordValueColumn;
    public TableColumn<Word.WordModel, Integer> WordPdfIdColumn;
    public TableColumn<Word.WordModel, Integer> WordPdfPageColumn;
    private final ObservableList<Word.WordModel> WordsList = FXCollections.observableArrayList();
    public TextField searchTextField;


    @FXML
    private ProgressBar progressBar;
    public Label pageNum;
    public Button runBtn;
    public Button doneBtn;
    public ImageView pageImageView;
    public ProgressIndicator pi0;
    public ProgressIndicator pi1;
    public ProgressIndicator pi2;
    public ProgressIndicator pi3;
    public ProgressIndicator pi4;
    public ProgressIndicator pi5;
    public ProgressIndicator pi6;
    public ProgressIndicator pi7;
    public ProgressIndicator pi8;
    public ProgressIndicator pi9;
    private ProgressIndicator[] progressIndicators;
    public CheckBox cb0;
    public CheckBox cb1;
    public CheckBox cb2;
    public CheckBox cb3;
    public CheckBox cb4;
    public CheckBox cb5;
    public CheckBox cb6;
    public CheckBox cb7;
    public CheckBox cb8;
    public CheckBox cb9;

    private static int singalCount;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        PdfTableView.setPlaceholder(new Label("Start Scanning PDFs"));
        WordTableView.setPlaceholder(new Label("Start Scanning PDFs"));

        PdfIdColumn.setCellValueFactory(pdf -> pdf.getValue()._Id.asObject());
        PdfPathColumn.setCellValueFactory(pdf -> pdf.getValue().FilePath);
        PdfStatusColumn.setCellValueFactory(pdf -> pdf.getValue().IsProcessed.asObject());
        PdfTableView.setItems(PDFsList);

        WordIdColumn.setCellValueFactory(word -> word.getValue()._Id.asObject());
        WordValueColumn.setCellValueFactory(word -> word.getValue().Value);
        WordPdfIdColumn.setCellValueFactory(word -> word.getValue().Pdf_Id.asObject());
        WordPdfPageColumn.setCellValueFactory(word -> word.getValue().Page_Num.asObject());
        WordTableView.setItems(WordsList);

        progressIndicators = new ProgressIndicator[]{pi0, pi1, pi2, pi3, pi4, pi5, pi6, pi7, pi8, pi9};
        progressBar.progressProperty().bind(PdfReader.pdfPercentProperty);

        CheckBox[] checkBoxs = new CheckBox[]{cb0, cb1, cb2, cb3, cb4, cb5, cb6, cb7, cb8, cb9};
        for (int i = 0; i < checkBoxs.length; i++)
            progressIndicators[i].disableProperty().bind(checkBoxs[i].selectedProperty().not());
        Main.LoadDB(PDFsList, WordsList);


        FilteredList<Word.WordModel> filteredList = new FilteredList<>(WordsList, e -> true);
//        searchTextField.setOnKeyReleased(e -> {
        searchTextField.textProperty().addListener(
                (observable, oldValue, newValue) -> filteredList.setPredicate(
                        wordModel -> newValue == null || newValue.isEmpty() ||
                                wordModel.Value.getValue().toLowerCase().contains(newValue.toLowerCase())));
//            SortedList<Word.WordModel> sortedList = new SortedList<>(filteredList);
//            sortedList.comparatorProperty().bind(WordTableView.comparatorProperty());
        WordTableView.setItems(filteredList);
//        });
    }

    public void runBtnClicked(MouseEvent mouseEvent) {
        int sum = 0;
        for (ProgressIndicator pi : progressIndicators)
            sum += pi.isDisabled() ? 0 : 1;
        if (sum > 0) {
            runBtn.setDisable(true);
            singalCount = sum;
            PdfReader[] pdfReaders = Main.Run(sum);
            for (int i = 0; i < pdfReaders.length; i++) {
                progressIndicators[i].progressProperty().bind(pdfReaders[i].wordPercentProperty);
                pdfReaders[i].setSignalReceiver(this);
            }
        }
    }

    public void DoneBtnClicked(MouseEvent mouseEvent) {
        PDFsList.clear();
        WordsList.clear();
        Main.LoadDB(PDFsList, WordsList);
    }

    public void signal() {
        singalCount--;
        if (singalCount <= 0) {
            doneBtn.setDisable(false);
        }
    }

    public void WordsTableClicked(MouseEvent mouseEvent) {
        Word.WordModel wm = WordTableView.getSelectionModel().getSelectedItem();
        if (wm == null) return;
//        // TOO SLOW even with [System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");]
//        BufferedImage bi  = PdfReader.GetPdfPage(wm.Pdf_Id.getValue(),wm.Page_Num.getValue());
//        if(bi == null) return;
//        pageImageView.setImage(SwingFXUtils.toFXImage(bi, null));
//        System.out.println(wm.Pdf_Id.getValue()+ " " + wm.Page_Num.getValue());
    }

    public void openPdfBtn(ActionEvent actionEvent) {
        Word.WordModel wm = WordTableView.getSelectionModel().getSelectedItem();
        if (wm == null) return;
        String pdfFilename = DB.getInstance().GetPdfPath(wm.Pdf_Id.getValue());
        if(pdfFilename == null) return;
        try {
            if (Desktop.isDesktopSupported())
                Desktop.getDesktop().open(new File(pdfFilename));
        } catch (IOException e) {
            e.printStackTrace();
        }
        pageNum.setText("Page:" + wm.Page_Num.getValue().toString());
    }
}
