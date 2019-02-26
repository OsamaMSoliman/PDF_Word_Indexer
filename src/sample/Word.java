package sample;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Word {
    private int _Id;
    public String Value;
    public int Pdf_Id;
    public int Page_Num;

    public Word(String word, int pdf_Id, int page_Num) {
        this.Value = word;
        this.Pdf_Id = pdf_Id;
        this.Page_Num = page_Num;
    }

    public Word setId(int id) {
        this._Id = id;
        return this;
    }

    public int get_Id() { return _Id; }


    @Override
    public String toString() { return String.format("\t%d\t%s\t%d\t%d\t", _Id, Value, Pdf_Id, Page_Num); }

    private WordModel model;

    public WordModel getModel() {
        if (model == null)
            model = new WordModel(_Id, Value, Pdf_Id, Page_Num);
        return model;
    }

    public class WordModel {
        final IntegerProperty _Id;
        final StringProperty Value;
        final IntegerProperty Pdf_Id;
        final IntegerProperty Page_Num;

        public WordModel(int _Id, String value, int pdf_Id, int page_Num) {
            this._Id = new SimpleIntegerProperty(_Id);
            this.Value = new SimpleStringProperty(value);
            this.Pdf_Id = new SimpleIntegerProperty(pdf_Id);
            this.Page_Num = new SimpleIntegerProperty(page_Num);
        }
    }
}
