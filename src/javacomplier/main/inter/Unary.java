package inter;

import lexer.*;
import symbols.*;

/**
 * Unary
 */
public class Unary extends Op {
    public Expr expr;

    /**
     * 处理单目减法，对！的处理见Not；
     * @param tok
     * @param x
     */
    public Unary(Token tok, Expr x) {
        super(tok, null);
        type = Type.max(Type.Int, expr.type);
        if (type == null)
            error("type error");
    }

    public Expr gen() {
        return new Unary(op, expr.reduce());
    }

    @Override
    public String toString() {
        return op.toString() + " " + expr.toString();
    }

}