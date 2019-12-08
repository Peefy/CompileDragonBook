
#ifndef __CHAPTER_2_H__
#define __CHAPTER_2_H__

#include <stdlib.h>
#include <stdio.h>
#include <ctype.h>

#define NONE  256
#define NUM   257

#define CHAR_TO_NUM(c)   (c - '0')   // 字符数字转整型数字
#define IS_ALPHA(c)   ('a' <= c && 'c' <= 'z') || ('A' <= c && 'c' <= 'Z') //是否是字母
#define IS_IDEN_CHAR(c) (IS_ALPHA(c) || (c == '_'))   // 是否是标识符词素

static char lookahead;
static int tokenval = NONE;
static int lineno = 1;



void expr() {
    term();
    rest();
}

void expr_override() {
    term();
    while (1) {
        if (lookahead == '+') {
            match('+'); term(); putchar('+'); 
        } 
        else if (lookahead == '-') {
            match('-'); term(); putchar('-'); 
        }
    }
}

void rest() {
    if (lookahead == '+') {
        match('+');
        term();
        putchar('+');
        rest();
    } 
    else if (lookahead == '-') {
        match('-');
        term();
        putchar('-');
        rest();
    }
}

void rest_override() {
    L : 
    if (lookahead == '+') {
        match('+'); term(); putchar('+'); goto L;
    } 
    else if (lookahead == '-') {
        match('-'); term(); putchar('-'); goto L;
    }
}

void term() {
    if (isdigit(lookahead)) {
        putchar(lookahead);
        match(lookahead);
    }
    else {
        error();
    }       
}

void match(char c) {
    lookahead == c ? lookahead = getchar() : error();
}

void error() {
    // 打印错误信息
    printf("syntax error\n");
    // 停止
    exit(1);
}

void factor() {
    if (lookahead == '(') {
        match('('); expr(); match(')');
    }
    else if (lookahead == NUM) {
        printf(" %d ", tokenval); match(NUM);
    }
    else {
        error();
    }
}

int lexan() {
    int t;
    while (1) { 
        // 从输入流中获取一个字符的ASCII码
        t = getchar();
        // 去除空格和制表符
        if (t == ' ' || t == '\t')
            ;
        // 换行符
        else if (t == '\n' || t == '\r')
            lineno += 1;
        else if (isdigit(t)) {
            tokenval = CHAR_TO_NUM(t);
            t = getchar();
            while (isdigit(t)) {
                tokenval = tokenval * 10 + CHAR_TO_NUM(t);
            }    
            ungetc(t, stdin);
            return NUM;
        } 
        else { 
            tokenval = NONE;
            return t;
        } 
    }  
} 

#endif
