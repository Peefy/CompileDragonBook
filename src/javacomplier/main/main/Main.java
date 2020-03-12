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
        try {
            Parser parser = new Parser(lex);
            parser.program();
        } catch (IOException e) {
            System.out.println("error"); 
        }
        System.out.println("finish"); 
    }
    
}