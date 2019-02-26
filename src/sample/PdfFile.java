package sample;

import javafx.beans.property.*;

public class PdfFile {
    public int _Id;
    public String FilePath;
    public boolean IsProcessed;

    public PdfFile(int _Id, String filePath) {
        this._Id = _Id;
        FilePath = filePath;
        IsProcessed = false;
    }

    public PdfFile setIsProcessed(boolean isProcessed){
        this.IsProcessed = isProcessed;
        return this;
    }

    @Override
    public String toString() { return String.format("\t%d\t%s\t%s\t", _Id, FilePath, IsProcessed); }

    private PdfFileModel model;

    public PdfFileModel getModel() {
        if (model == null)
            model = new PdfFileModel(_Id, FilePath, IsProcessed);
        return model;
    }

    public class PdfFileModel {
        final IntegerProperty _Id;
        final StringProperty FilePath;
        final BooleanProperty IsProcessed;

        public PdfFileModel(int _Id, String FilePath, boolean IsProcessed) {
            this._Id = new SimpleIntegerProperty(_Id);
            this.FilePath = new SimpleStringProperty(FilePath);
            this.IsProcessed = new SimpleBooleanProperty(IsProcessed);
        }
    }
}
