package main;

import java.io.*;

import lexer.*;
import parser.*;

/**
 * Main
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("Hello Java Compiler!");
        Lexer lex = new Lexer();
        Parser parser = new Parser(lex);
        parser.program();
        System.out.println(""); 
    }
    
}