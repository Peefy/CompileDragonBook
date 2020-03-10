package symbols;

import lexer.*;

/**
 * Type
 */
public class Type extends Word {
    /**
     * 用于存储分配
     */
    public int width = 0;  

    public Type(String s, int tag, int w) {
        super(s, tag);
        width = w;
    }
    
    public static final Type
        Int = new Type("int", Tag.BASIC, 4),
        Float = new Type("float", Tag.BASIC, 8),
        Char = new Type("char", Tag.BASIC, 1),
        Bool = new Type("bool", Tag.BASIC, 1);

    public static boolean isNumeric(Type p) {
        return p == Type.Char || p == Type.Int || p == Type.Float;
    }

    public static Type max(Type p1, Type p2) {
        if (!isNumeric(p1) || isNumeric(p2))
            return null;
        else if (p1 == Type.Float || p2 == Type.Float)
            return Type.Float;
        else if (p1 == Type.Int || p2 == Type.Int)
            return Type.Int;
        return Type.Char;
    }
}