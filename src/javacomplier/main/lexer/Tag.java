package lexer;

/**
 * Tag
 * 其中的三个常量INDEX，MINUS，TEMP不是词法单元，将在语法分析中使用
 */
public class Tag {
    public static final int 
        AND = 256,     // &&
        BASIC = 257,   // 
        BREAK = 258,   // break
        DO = 259,      // do
        ELSE = 260,    // else
        EQ = 261,      // eq
        FALSE = 262,   // false
        GE = 263,      // <=
        ID = 264,      // id
        IF = 265,      // if
        INDEX = 266,
        LE = 267,      // >=
        MINUS = 268,   // -
        NE = 269,      // !=
        NUM = 270,     // num
        OR = 271,      // ||
        REAL = 272,    // float
        TEMP = 273,    // temp
        TRUE = 274,    // true
        WHILE = 275;   // while
        // >>
        // << 
}