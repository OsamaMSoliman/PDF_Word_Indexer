package sample;

public class Word {
    private int _Id;
    public String word;
    public int Pdf_Id;
    public int Page_Num;

    public Word(String word, int pdf_Id, int page_Num) {
        this.word = word;
        this.Pdf_Id = pdf_Id;
        this.Page_Num = page_Num;
    }

    public Word setId(int id) {
        this._Id = id;
        return this;
    }

    public int get_Id() { return _Id; }
}
