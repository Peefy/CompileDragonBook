package lexer;

/**
 * Token
 */
public class Token {
    /**
     * 
     */
    public final int tag;

    /**
     * 
     * @param t
     */
    public Token(int t) {
        tag = t;
    }

    /**
     * 
     */
    @Override
    public String toString() {
        return String.valueOf((char)tag);
    }
    
}