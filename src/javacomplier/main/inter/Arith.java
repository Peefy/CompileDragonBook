package inter;

import lexer.*;
import symbols.*;

/**
 * Arith
 */
public class Arith extends Op {

    /**
     * 
     */
    public Expr expr1, expr2;

    /**
     * 
     * @param tok
     * @param x1
     * @param x2
     */
    public Arith(Token tok, Expr x1, Expr x2) {
        super(tok, null);
        expr1 = x1;
        expr2 = x2;
        type = Type.max(expr1.type, expr2.type);
        if (type == null) 
            error("type error");
    }

    /**
     * 把表达式的子表达式规约为地址
     */
    public Expr gen() {
        return new Arith(op, expr1.reduce(), expr2.reduce());
    }

    /**
     * 
     */
    @Override
    public String toString() {
        return expr1.toString() + " " + op.toString() + " " + expr2.toString();
    }
    
}