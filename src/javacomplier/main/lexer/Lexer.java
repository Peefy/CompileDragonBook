package lexer;

import java.io.*;
import java.util.*;

import symbols.*;

/**
 * Lexer
 */
public class Lexer {
    /**
     * 
     */
    public static int line = 1;

    /**
     * 
     */
    char peek = ' ';

    /**
     * 符号表
     */
    Hashtable<String, Word> words = new Hashtable<>();

    /**
     * 
     * @param w
     */
    void reserse(Word w) {
        words.put(w.lexname, w);
    }

    /**
     * 
     */
    public Lexer() {
        reserse(new Word("if", Tag.IF));
        reserse(new Word("else", Tag.ELSE));
        reserse(new Word("while", Tag.WHILE));
        reserse(new Word("do", Tag.DO));
        reserse(new Word("break", Tag.BREAK));
        reserse(Word.True);
        reserse(Word.Flase);
        reserse(Type.Int);
        reserse(Type.Char);
        reserse(Type.Bool);
        reserse(Type.Float);
    }

    /**
     * 
     * @throws IOException
     */
    void readch() throws IOException {
        peek = (char)System.in.read();
    }
    
    /**
     * 
     * @param c
     * @return
     * @throws IOException
     */
    boolean readch(char c) throws IOException {
        readch();
        if (peek != c)
            return false;
        peek = ' ';
        return true;
    }

    /**
     * 
     * @return
     * @throws IOException
     */
    public Token scan() throws IOException {
        for (;;readch()) {
            if (peek == ' ' || peek == '\t')
                continue;
            else if (peek == '\n')
                line = line + 1;
            else 
                break;
        }
        switch (peek) {
            case '&':
                if (readch('&'))
                    return Word.and;
                else 
                    return new Token('&');
            case '|':
                if (readch('|'))
                    return Word.or;
                else 
                    return new Token('|');
            case '=':
                if (readch('='))
                    return Word.eq;
                else 
                    return new Token('=');
            case '!':
                if (readch('='))
                    return Word.ne;
                else 
                    return new Token('!');
            case '<':
                if (readch('='))
                    return Word.le;
                else 
                    return new Token('<');
            case '>':
                if (readch('='))
                    return Word.ge;
                else 
                    return new Token('>');
        }
        if (Character.isDigit(peek)) {
            int v = 0;
            do {
                v = 10 * v + Character.digit(peek, 10);
                readch();
            } while (Character.isDigit(peek));
            if (peek != '.') 
                return new Num(v);
            float x = v;
            float d = 10;
            for(;;) {
                readch();
                if (Character.isDigit(peek) == false) 
                    break;
                x = x + Character.digit(peek, 10) / d;
                d = d * 10;
            }
            return new Real(x);
        }
        if (Character.isLetter(peek)) {
            StringBuffer b = new StringBuffer();
            do {
                b.append(peek);
                readch();
            } while ( Character.isLetterOrDigit(peek) );
            String s = b.toString();
            Word w = words.get(s);
            if (w != null)
                return w;
            w = new Word(s, Tag.ID);
            words.put(s, w);
            return w;
        }
        Token tok = new Token(peek);
        peek = ' ';
        return tok;
    }
}