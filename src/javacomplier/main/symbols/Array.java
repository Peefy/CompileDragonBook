package symbols;

import lexer.*;

/**
 * Array
 */
public class Array extends Type {
    /**
     * 
     */
    public Type of;
    /**
     * 
     */
    public int size = 1;

    /**
     * 
     * @param sz
     * @param p
     */
    public Array(int sz, Type p) {
        super("[]", Tag.INDEX, sz * p.width);
        size = sz;
        of = p;
    }
    
    /**
     * 
     */
    @Override
    public String toString() {
        return "[" + size + "]" + of.toString();
    }

}