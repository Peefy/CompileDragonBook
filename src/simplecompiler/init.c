
#include "global.h"

// 初始化模块

// 关键字
entry keywords[] = {
    {"div", DIV},
    {"mod", MOD},
    {"0", 0},  // 关键字结尾
};

// 将关键字填入符号表
void init() {
    entry * p;
    for (p = keywords;p->token;p++) {
        insert(p->lexptr, p->token);
    }
}

void main() {
    init();
    parse();
    exit(0);
}
