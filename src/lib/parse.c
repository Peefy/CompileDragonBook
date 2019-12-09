
#include "global.h"

// 词法翻译器模块

int lookahead;

static void expr();
static void term();
static void factor();
static void match(int t);

// 分析并翻译表达式列表
void parse() {
    lookahead = lexan();
    while (lookahead != DONE) {
        // 匹配表达式
        expr(); 
        // 每个表达式结尾要匹配句尾;
        match(';');
    }
}

static void expr() {
    int t;
    term();
    while (1) {
        switch (lookahead)
        {
        // + - 优先级比* / ( ) 低，最后匹配
        case '+': case '-':
            t = lookahead;
            match(lookahead); term(); emit(t, NONE);
            break;
        default:
            return;
        }
    }
}

static void term() {
    int t;
    factor();
    while (1) {
        switch (lookahead)
        {
        case '*': case '/': case DIV: case MOD:
            t = lookahead;
            match(lookahead); factor(); emit(t, NONE);
            break;  
        default:
            return;
        }
    }
}

static void factor() {
    switch (lookahead)
    {
    case '(':
        match('('); expr(); match(')'); 
        break;
    case NUM:
        emit(NUM, tokenval); match(NUM); 
        break;
    case ID:
        emit(ID, tokenval); match(ID); 
        break;
    default:
        break;
    }
}

static void match(int t) {
    if (lookahead == t)
        lookahead = lexan();
    else 
        error("syntax error!");
}
