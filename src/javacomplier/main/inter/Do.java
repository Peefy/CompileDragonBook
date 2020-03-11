package inter;

import symbols.*;

/**
 * Do
 */
public class Do extends Stmt {
    Expr expr;
    Stmt stmt;
    public Do(Expr x, Stmt s) {
        expr = x;
        stmt = s;
        if (expr.type != Type.Bool)
            expr.error("boolean required in do");
    }

    @Override
    public void gen(int b, int a) {
        after = a;
        int label = newlabel();
        stmt.gen(b, label);
        emitlabel(label);
        expr.jumping(b, 0);
    }
    
}