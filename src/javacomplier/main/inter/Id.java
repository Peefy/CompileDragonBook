package inter;

import lexer.*;
import symbols.*;

/**
 * Id
 */
public class Id extends Expr {
    /**
     * 相对地址
     */
    public int offset;

    /**
     * 
     * @param id
     * @param p
     * @param b
     */
    public Id(Word id, Type p, int b) {
        super(id, p);
        offset = b;
    }
}