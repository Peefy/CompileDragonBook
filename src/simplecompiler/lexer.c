
#include "global.h"

// 词法分析器模块

char lexbuf[BSIZE];
int tokenval = NONE;  // 记号的属性值
int lineno = 1;    // 行号

// 词法分析器 或者记号 token 
int lexan() {
    int t;
    for (;;) {
        t = getchar();  // 从字节流获取一个字符
        if (t == ' ' || t == '\t' || t == '\v') // 去除空白符
            pass;
        else if (t == '\n' || t == '\r')  // 检测换行符
            lineno = lineno + 1;
        else if (isdigit(t)) {
            ungetc(t, stdin);
            scanf("%d", &tokenval);
            return NUM;
        }
        else if (isalpha(t)) {
            int p, b = 0;
            while (isalnum(t)) {
                lexbuf[b] = t;
                t = getchar();
                b = b + 1;
                if (b > BSIZE)
                    error("compiler error");
            }
            lexbuf[b] = EOS;
            if (t != EOF)
                ungetc(t, stdin);
            p = lookup(lexbuf);
            if (p == 0)
                p = insert(lexbuf, ID);
            tokenval = p;
            return symtabel[p].token;
        }
        else if (t == EOF)
            return DONE;
        else {
            tokenval = NONE;
            return t;
        }
    }
}

token nexttoken() {
    while (1) {
        
    }   
}
