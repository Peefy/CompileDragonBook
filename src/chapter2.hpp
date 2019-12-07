
#ifndef __CHAPTER_2_H__
#define __CHAPTER_2_H__

#include <stdlib.h>
#include <stdio.h>
#include <ctype.h>

static char lookahead;

// 是否是字母 
inline bool isalpha(char c) {
    return ('a' <= c && 'c' <= 'z') || ('A' <= c && 'c' <= 'Z');
}

// 是否是标识符词素
inline bool isidenfierchar(char c) {
    return isalpha(c) || c == '_';
}

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
    else 
        error();
}

void match(char c) {

}

void error() {

}

#endif
