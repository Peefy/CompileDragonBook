package inter;

import lexer.*;

/**
 * Node
 */
public class Node {
    /**
     * 
     */
    int lexline = 0;

    /**
     * 
     */
    public Node() {
        lexline = Lexer.line;
    }
 
    /**
     * 
     * @param s
     */
    void error(String s) {
        throw new Error("near line" + lexline + ": " + s);
    }

    /**
     * 
     */
    static int labels = 0;

    /**
     * 
     * @return
     */
    public int newlabel() {
        labels += 1;
        return labels;
    }

    /**
     * 
     * @param i
     */
    public void emitlabel(int i) {
        System.out.println("L" + i + ":");
    }

    /**
     * 
     * @param s
     */
    public void emit(String s) {
        System.out.println("\t" + s);
    }

}