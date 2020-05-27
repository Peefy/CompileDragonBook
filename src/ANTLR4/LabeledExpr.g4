grammar LabeledExpr;
/* 起始规则，语法分析的起点 */
prog: stat+;
stat: expr NEWLINE        # printExpr
    | ID '=' expr NEWLINE # assign
    | NEWLINE             # blank
    ;
expr: expr op=('*'|'/') expr # MulDiv
    | expr op=('+'|'-') expr # AddSub
    | INT                    # int
    | ID                     # id
    | '(' expr ')'           # parens
    ;
ID:  [a-zA-Z]+;     // 匹配标识符
INT: [0-9]+;        // 匹配整数
NEWLINE: 'r'? '\n'; // 告诉语法分析器一个新行的开始(即语句终止标志)
WS:  [\t]+ -> skip; // 丢弃空白字符
MUL:  '*';
DIV:  '/';
ADD:  '+';
SUB:  '-';
