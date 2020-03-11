package symbols;

import java.util.*;

import lexer.*;
import inter.*;

/**
 * Env 把字符串词法映射为类Id的对象
 */
public class Env {
    /**
     * 
     */
    private Hashtable<Token, Id> table;

    /**
     * 
     */
    protected Env prev;

    /**
     * 
     * @param n
     */
    public Env(Env n) {
        table = new Hashtable<>();
        prev = n;
    }

    /**
     * 
     * @param w
     * @param i
     */
    public void put(Token w, Id i) {
        table.put(w, i);
    }
    
    /**
     * 
     * @param w
     * @return
     */
    public Id get(Token w) {
        for (Env e = this; e != null ; e = e.prev) {
            Id found = (Id)(table.get(w));
            if (found != null)
                return found;
        }
        return null;
    }

}