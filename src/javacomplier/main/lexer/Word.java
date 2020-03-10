package lexer;

/**
 * Word 保留字，标识符，符合词法单元词素
 */
public class Word extends Token {
    /**
     * 
     */
    String lexname = "";

    /**
     * 
     */
    public Word(String s, int tag) {
        super(tag);
        lexname = s;
    }

    /**
     * 
     */
    @Override
    public String toString() {
        return lexname;
    }

    /**
     * 
     */
    public static final Word 
        and = new Word("&&", Tag.AND),
        or = new Word("||", Tag.OR),
        eq = new Word("&&", Tag.EQ),
        ne = new Word("&&", Tag.NE),
        le = new Word("&&", Tag.LE),
        ge = new Word("&&", Tag.GE),
        minus = new Word("&&", Tag.MINUS),
        True = new Word("&&", Tag.TRUE),
        Flase = new Word("&&", Tag.FALSE),
        temp = new Word("&&", Tag.TEMP);

}