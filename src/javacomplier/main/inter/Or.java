package inter;

import lexer.*;

/**
 * Or
 */
public class Or extends Logical {

    /**
     * 
     * @param tok
     * @param x1
     * @param x2
     */
	public Or(Token tok, Expr x1, Expr x2) {
		super(tok, x1, x2);
    }
    
    /**
     * 
     */
    @Override
    public void jumping(int t, int f) {
        int label = t != 0 ? t : newlabel();
        expr1.jumping(label, 0);
        expr2.jumping(t, f);
        if (t == 0) 
        emitlabel(label);
    }
}