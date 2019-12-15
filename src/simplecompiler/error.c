
#include "global.h"

// 错误处理模块

// 生成所有的出错信息
void error(const char* m) {
    fprintf(stderr, "line: %d : %s\n", lineno, m);
    exit(1);  /* 非正常终止 */
}
