package inter;

import lexer.*;
import symbols.*;

/**
 * Constant
 */
public class Constant extends Expr {

    public Constant(Token tok, Type p) {
        super(tok, p);
    }

    public Constant(int i) {
        super(new Num(i), Type.Int);
    }

    public static final Constant
        True = new Constant(Word.True, Type.Bool),
        False = new Constant(Word.Flase, Type.Bool);
    
    public void jumping(int t, int f) {
        if (this == True && t != 0) {
            emit("goto L" + t);
        }
        else if (this == False && f != 0) {
            emit("goto L" + f);
        }
    }
    
}