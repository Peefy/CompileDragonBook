
#include "global.h"

// 符号模块

#define STRMAX 999    // lexemes数组的大小
#define SYMMAX 1000   // symtable的大小

char lexemes[STRMAX];
int lastchar = -1;   // lexemes中最后引用的位置
entry symtabel[SYMMAX];
int lastentry = 0;   // symtable中最后引用的位置

// 返回s符号表项的位置
int lookup(char s[]) {
    int p = 0;
    for (p = lastentry;p > 0; p = p - 1) {
        if (strcmp(symtabel[p].lexptr, s) == 0)
            return p;
    }
    return 0;
}

// 插入符号表，返回s表项的位置
int insert(char s[], int tok) {
    int len;
    len = strlen(s);
    if (lastentry + 1 >= SYMMAX) {
        error("symbol table full!");
    }
    if (lastchar + len + 1 >= STRMAX) {
        error("lexemes array full!");
    }
    lastentry = lastentry + 1;
    symtabel[lastentry].token = tok;
    symtabel[lastentry].lexptr = &lexemes[lastchar + 1];
    lastchar = lastchar + len + 1;
    strcpy(symtabel[lastentry].lexptr, s);
    return lastentry;
}
