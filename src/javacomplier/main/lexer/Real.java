package lexer;

/**
 * Real
 */
public class Real extends Token {
    public final float value;

    public Real(float v) {
        super(Tag.REAL);
        value = v;
    }

    @Override
    public String toString() {
        return Float.toString(value);
    }
    
}