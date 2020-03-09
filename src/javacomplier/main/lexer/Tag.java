package lexer;

/**
 * Tag
 * 其中的三个常量INDEX，MINUS，TEMP不是词法单元，将在语法分析中使用
 */
public class Tag {
    public static final int 
        AND = 256,
        BASIC = 257,
        BREAK = 258,
        DO = 259,
        ELSE = 260,
        EQ = 261,
        FALSE = 262,
        GE = 263,
        ID = 264,
        IF = 265,
        INDEX = 266,
        LE = 267,
        MINUS = 268,
        NE = 269,
        NUM = 270,
        OR = 271,
        REAL = 272,
        TEMP = 273,
        TRUE = 274,
        WHILE = 275;
}