package inter;

import symbols.*;

/**
 * If
 */
public class If extends Stmt {
    Expr expr;
    Stmt stmt;
    public If(Expr x, Stmt s) {
        expr = x;
        stmt = s;
        if (expr.type != Type.Bool)
            expr.error("boolean required in if");
    }
    
    @Override
    public void gen(int b, int a) {
        int label = newlabel();  // stmt代码的标号
        expr.jumping(0, a);      // 为真时控制流穿越，为假时转向a
        emitlabel(label);
        stmt.gen(label, a);
    }

}