
#include "global.h"

// 打印输出模块

// 生成输出
void emit(int t, int tval) {
    switch (t)
    {
    case '+': case '-': case '*': case '/':
        printf("%c\n", t);
        break;
    case DIV:
        printf("DIV\n"); break;
    case MOD:
        printf("MOD\n"); break;
    case NUM:
        printf("%d\n", tval); break;
    case ID:
        printf("%s\n", symtabel[tval].lexptr); break;
    default:
        printf("token %d, tokenval %d\n", t, tval); break;
    }
}

