
# ANTLR 解析器生成器

ANTLR（ANother Tool for Language Recognition 另一种语言识别工具）是功能强大的解析器生成器，用于读取，处理，执行或翻译结构化文本或二进制文件。 它被广泛用于构建语言，工具和框架。 ANTLR通过语法生成可以构建和遍历语法树的语法分析器。

## Quick Start

* OS X

```zsh
$ cd /usr/local/lib
$ sudo curl -O https://www.antlr.org/download/antlr-4.8-complete.jar
$ export CLASSPATH=".:/usr/local/lib/antlr-4.8-complete.jar:$CLASSPATH"
$ alias antlr4='java -jar /usr/local/lib/antlr-4.8-complete.jar'
$ alias grun='java org.antlr.v4.gui.TestRig'
```

* LINUX

```sh
$ cd /usr/local/lib
$ wget https://www.antlr.org/download/antlr-4.8-complete.jar
$ export CLASSPATH=".:/usr/local/lib/antlr-4.8-complete.jar:$CLASSPATH"
$ alias antlr4='java -jar /usr/local/lib/antlr-4.8-complete.jar'
$ alias grun='java org.antlr.v4.gui.TestRig'
```

## VS Code ANTLR4 插件

## ANTLR 语法

```antlr
/** Optional javadoc style comment */
grammar Name; 
options {...}
import ... ;

tokens {...}
channels {...} // lexer only
@actionName {...}

rule1 // parser and lexer rules, possibly intermingled
...
ruleN
```

* **grammer**-声明语法头，类似于Java的定义
* **options**-选项，如语言选项，输出选项，回溯选项，记忆选项等等
* **@actionName**-动作（Actions）实际上是用目标语言写成的、嵌入到规则中的代码（以花括号包裹）。它们通常直接操作输入的标号，但是他们也可以用来调用相应的外部代码。常用属性或动作说明：
1. `@header { package com.zetyun.aiops.antlr.test; }`这个动作很有用，即在运行脚本后，生成的类中自动带上这个包路径，避免了手动加入的麻烦。
2. `@members { int i; public TParser(TokenStream input, int foo) { this(input); i = foo; }}`
3. `@after {System.out.println("after matching rule; before finally");}`
* **rule**-文法的核心，表示规则，以 `:` 开始， `;` 结束， 多规则以 `|` 分隔。

```antlr
ID : [a-zA-Z0-9|'_']+ ;    //数字 
STR:'\'' ('\'\'' | ~('\''))* '\''; 
WS: [ \t\n\r]+ -> skip ; // 系统级规则 ，即忽略换行与空格

sqlStatement
    : ddlStatement 
    | dmlStatement     | transactionStatement
    | replicationStatement     | preparedStatement
    | administrationStatement     | utilityStatement
    ;
```

## ANTLR 注释

* 单行、多行、javadoc风格
* javadoc风格只能在开头使用

```antlr
/** 
 * This grammar is an example illustrating the three kinds
 * of comments.
 */
grammar T;

/* a multi-line
  comment
*/

/** This rule matches a declarator for my language */

decl : ID ; // match a variable name
```

## ANTLR 标识符

* 符号(Token)名大写开头
* 解析规则(Parser rule)名小写开头,后面可以跟字母、数字、下划线

```antlr
ID, LPAREN, RIGHT_CURLY // token names
expr, simpleDeclarator, d2, header_file // rule names
```

## ANTLR 遍历模式

### Listener (观察者模式，通过结点监听，触发处理方法)

* 程序员不需要显示定义遍历语法树的顺序，实现简单
* 缺点，不能显示控制遍历语法树的顺序
* 动作代码与文法产生式解耦，利于文法产生式的重用
* 没有返回值，需要使用map、栈等结构在节点间传值

### Visitor (访问者模式，主动遍历)

* 程序员可以显示定义遍历语法树的顺序
* 不需要与antlr遍历类ParseTreeWalker一起使用，直接对tree操作
* 动作代码与文法产生式解耦，利于文法产生式的重用
* visitor方法可以直接返回值，返回值的类型必须一致，不需要使用map这种节点间传值方式，效率高

## ANTLR4 示例

### Hello ANTLR4

```antlr
// Define a grammar called Hello 定义一个语法名字：Hello
grammar Hello;
r  : 'hello' ID ;         // match keyword hello followed by an identifier 匹配关键字hello，后跟标识符
ID : [a-z]+ ;             // match lower-case identifiers 匹配全是小写字母的标识符
WS : [ \t\r\n]+ -> skip ; // skip spaces, tabs, newlines  跳过空格，制表符，换行符
```

### 简单计算器

```antlr
grammar Math;

@header{package com.src.ANTLR4;} 

prog : stat+;

stat: expr NEWLINE          # printExpr
    | ID '=' expr NEWLINE   # assign
    | NEWLINE               # blank
    ;

expr:  expr op=('*'|'/') expr   # MulDiv
| expr op=('+'|'-') expr        # AddSub
| INT                           # int
| ID                            # id
| '(' expr ')'                  # parens
;

MUL : '*' ; // assigns token name to '*' used above in grammar
DIV : '/' ;
ADD : '+' ;
SUB : '-' ;
ID : [a-zA-Z]+ ;
INT : [0-9]+ ;
NEWLINE:'\r'? '\n' ;
WS : [ \t]+ -> skip;
```

```java
package com.src.antlr4;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import com.zetyun.aiops.core.math.MathLexer;
import com.zetyun.aiops.core.math.MathParser;

public class Math {
    public static void main(String[] args) {
        CharStream input = CharStreams.fromString("12*2+12\r\n");
        MathLexer lexer = new MathLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        MathParser parser = new MathParser(tokens);
        ParseTree tree = parser.prog(); // parse
    }
}
```


