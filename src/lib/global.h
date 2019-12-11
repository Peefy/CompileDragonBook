
#ifndef _GLOBAL_H_
#define _GLOBAL_H_

#include <stdio.h>    // 输入/输出
#include <ctype.h>    // 加载字符测试程序
#include <string.h>

#define BSIZE 128     // 缓冲区大小
#define NONE  -1
#define EOS   '\0'

#define NUM  256
#define DIV  257
#define MOD  258
#define ID   259
#define DONE 260

#ifndef pass
#define pass
#endif

extern int tokenval;  // 记号的属性值
extern int lineno;    // 行号

// 符号表的表项格式
typedef struct entry {
    // 指向记号词素内容的指针
    char * lexptr;
    // 记号
    int token;
} entry;

entry symtabel[];

/* function declare  */

void error(const char* m);  // 生成所有的出错信息
void emit(int t, int tval);  // 生成输出
int lookup(char s[]); // 返回s符号表项的位置
int insert(char s[], int tok); // 插入符号表，返回s表项的位置
int lexan();  // 词法分析器 或者记号 token 
void parse(); // 分析并翻译表达式列表
void init();

#endif
