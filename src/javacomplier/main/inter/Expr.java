package inter;

import lexer.*;
import symbols.*;

/**
 * Expr 表达式结点
 */
public class Expr extends Node {
    /**
     * 
     */
    public Token op;

    /**
     * 
     */
    public Type type;

    /**
     * 
     * @param tok
     * @param p
     */
    public Expr(Token tok, Type p) {
        op = tok;
        type = p;
    }

    /**
     * 
     * @return
     */
    public Expr gen() {
        return this;
    }

    /**
     * 
     * @return
     */
    public Expr reduce() {
        return this;
    }

    /**
     * 
     * @param t true
     * @param f false
     */
    public void jumping(int t, int f) {
        emitjumps(toString(), t, f);
    }

    /**
     * 
     * @param test
     * @param t true
     * @param f false
     */
    public void emitjumps(String test, int t, int f) {
        if (t != 0 && f != 0) {
            emit("if " + test + " goto L" + t);
            emit("goto L" + f);
        }
        else if (t != 0) {
            emit("if " + test + " goto L" + t);
        }
        else if (f != 0) {
            emit("iffalse " + test + " goto L" + f)
        }
    }

    /**
     * 
     */
    @Override
    public String toString() {
        return op.toString();
    }

}