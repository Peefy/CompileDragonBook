package lexer;

/**
 * Num
 */
public class Num extends Token {
    /**
     * 
     */
    public final int value;

    /**
     * 
     * @param value
     */
    public Num(int value) {
        super(Tag.NUM);
        this.value = value;
    }

    /**
     * 
     */
    @Override
    public String toString() {
        return String.valueOf(value);
    }

}