
# ANTLR 解析器生成器

ANTLR（ANother Tool for Language Recognition 另一种语言识别工具）是功能强大的解析器生成器，用于读取，处理，执行或翻译结构化文本或二进制文件。 它被广泛用于构建语言，工具和框架。 ANTLR通过语法生成可以构建和遍历语法树的语法分析器。

一门语言的正式描述称为**语法(grammer)**，ANTLR能够为该语言生成一个语法分析器，并自动建立语法分析树----一种描述语法与输入文本匹配关系的数据结构。ANTLR也能够自动生成树的遍历器，这样就可以访问树中的节点，执行自定义的业务逻辑代码。

## ANTLR4的新特点

ANTLR4极大地简化了匹配某些句法结构(如编程语言的算术表达式)所需的语法规则。长久以来，处理表达式都是ANTLR语法以及手工编写的递归下降语法分析器的难题。识别表达式最自然的语法对于传统的自顶向下的语法分析器生成器(如ANTLR3)是无效的，但是ANTLR4可以使用如下的左递归表达式:

```antlr
expr : expr '*' expr  // 匹配乘法
     | expr '+' expr  // 匹配加法
     | INT            // 匹配简单的整数因子
     ;
```

类似`expr`的自引用规则是递归的，更准确地说，是**左递归(left recursive)**的，因为它的至少一个备选分支直接引用了它自己。

ANTLR4自动将类似expr的左递归规则重写成了等价的非左递归形式。唯一的约束是左递归必须是直接的，也就是说直接引用自身。一条规则不能匹配另外一条规则。

ANTLR生成的语法分析器能够自动建立语法分析树的视图，其他程序可以遍历此树，并在所需处理的结构处触发回调函数。在先前的ANTLR3中，用户需要补充语法来创建树。ANTLR4还提供了自动生成语法树遍历器的实现：监听器(listener)或者访问器(visitor)。监听器与在XML文档的解析过程中响应SAX事件的处理器相似。

ANTLR的`LL(*)`语法分析策略不如ANTLR4的`ALL(*)`强大，所以ANTLR3为了能够正确识别输入的文本，有时候不得不进行回溯。

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

## Hello ANTLR4

```antlr
// Define a grammar called Hello 定义一个语法名字：Hello
grammar Hello;
r  : 'hello' ID ;         // match keyword hello followed by an identifier 匹配关键字hello，后跟标识符
ID : [a-z]+ ;             // match lower-case identifiers 匹配全是小写字母的标识符
WS : [ \t\r\n]+ -> skip ; // skip spaces, tabs, newlines  跳过空格，制表符，换行符
```

然后使用`antlr`和`grun`别名来运行测试它,`grun`相当于一个主程序

```sh
antlr4 Hello.g4
javac *.java
grun Hello r -tokens
```

或者打印出LISP风格文本格式的语法分析树`

```sh
grun Hello r -tree 
hello parrt
```

`grun`的命令行参数选项:

* **-tokens**-打印出词法符号流
* **-tree**-以LISP格式打印出语法分析树
* **-gui**-在对话框中以可视化方式显示语法分析树
* **-ps file.ps**-以PostScript格式生成可视化语法分析树，然后将其存储于file.ps。
* **-encoding encodinggame**-若当前的区域设定无法正确读取输入，使用这个选项制定测试组件
* **-trace**-打印规则的名字以及进入和离开该规则时的词法符号。
* **diagnostics**-开启解析过程中的调试信息输出。通常仅在一些罕见情况下才使用它产生信息，例如输入的文本有歧义。
* **-SLL**-使用另外一种更快但是功能稍弱的解析策略。

### ANTLR 元语言

为了实现一门编程语言，需要构建一个程序，读取输入的语句，对其中的词组和输入符号进行正确的处理。**语言(language)**由一系列有意义的语句组成，**语句(sentence)**由词组组成，**词组(phrase)**是由更小的**子词组(subphrase)**和**词汇符号(vocabulary sumbol)**组成。一般来说，如果一个程序能够分析计算或者“执行”语句，就称之为**解释器(interpreter)**。这样的例子包括计算器、读取配置文件的程序和Python解释器。如果一个程序能够将一门语言的语句转换为另外一门语言的语句，称之为**翻译器(translator)**。这样的例子包括Java和C#的转换器和普通的编译器。

为了达到目的，解释器或者翻译器需要识别出一门特定语言的所有的有意义的语句、词组和子词组。识别一个词组意味着可以将它从众多的组成部分中辨认和区分出来。例如，能够将输入的`"sp=100;"`识别为一个赋值语句，这意味着需要知道sp是被赋值的目标，100是要被赋予的值。与之类似，如果要识别英文语句，就需要辨认出一段对话的不同部分，例如主语、谓语和宾语。

识别语言的程序称为**语法分析器(parser)**或者**句法分析器(syntax analyzer)**。**句法(syntax)**是指约束语言中的各个组成部分之间关系的规则，**语法(grammer)**是一系列规则的集合，每条规则表述出一种词汇结构。ANTLR工具能够将其转换为如同经验丰富的开发者手工构建一般的语法分析器(ANTLR是一个能够生成其他程序的程序)。ANTLR语法本身又遵循了一种专门用来描述其他语言的语法，称之为**ANTLR元语言(ANTLR's meta-language)**。

如果将语法分析的过程分解为两个相似但独立的任务或者说阶段时，实现起来就容易多了。就像读英文一样，不是一个字符一个字符地读句子，而是将句子看作一列单词。在识别整个句子的语法结构之前，先获取单词(词法)，再获取句子(语法)。

将字符聚集为**单词或者符号(词法符号,token)**的过程称为**词法分析(lexical analysis)**或者**词法符号化(tokenizing)**。可以把输入文本转换为词法符号的程序称为**词法分析器(lexer)**。词法分析器可以将相关的词法符号归类，例如**INT(整数)**、**ID(标识符)**、**FLOAT(浮点数)**等。当语法分析器不关心单个符号，而仅关心符号的类型时，词法分析器就需要将词汇符号归类。词法符号包含至少两部分信息：词法符号的类型(从而能够通过类型来识别词法结构)和该词法符号对应的文本。

第二个阶段是实际的语法分析过程，在这个过程中，输入的词法符号被"消费"以识别语句结构，在上例中即为赋值语句。默认情况下，ANTLR生成的语法分析器会建造一种名为**语法分析树(parse tree)**或者**句法树(syntax tree)**的数据结构，该数据结构记录了语法分析器识别出输入语句结构的过程，以及该结构的各组成部分。

语法分析树的内部结点是词组名，这些名字用于识别它们的子结点，并将子结点归类。根结点是最抽象的一个名字，一般使用**stat**(statment的简称)命名。语法分析树的叶子结点永远是输入的词法符号。句子，也即符号的线性组合，本质上是语法分析树在人脑中的串行化。为了能与其他人沟通，需要使用一串单词，使得他们能在脑海中构建出一棵相同的语法分析树。

通过操纵语法分析树，识别同一种语言的不同程序就能复用同一个语法分析器。另外一种解决方案，也是传统的生成语法分析器的方案，是直接在语法文件中嵌入与这种程序相关的代码。ANTLR4仍然允许这种传统的方案，不过使用语法分析树可以使程序更简洁、解耦性更强。

在语言的翻译过程中，一个阶段依赖于前一个阶段的计算结果和信息，因此需要多次进行**树的遍历(tree walk)**，这种情况下语法分析树也是非常有用的。在其他情况下，将一个复杂的程序分解为多个阶段会大大简化编码和测试工作，与其每个阶段都重新解析一下输入的字符流，不如首先生成语法分析树，然后多次访问其中的节点，这样更有效率。

由于使用一系列的规则制定语句的词汇结构，语法分析树的子树的根节点就对应语法规则的名字。

```antlr
assign : ID '=' expr; // 匹配一个类似"sp=100;"的赋值语句
```

使用和调试ANTLR语法的一个基本要求是，理解ANTLR是如何将这样的规则转为人类可阅读的语法分析程序。

### 实现一个语法分析器

ANTLR工具依据类似于之前的`assign`语法规则，产生一个**递归下降语法分析器(recursive-descent parser)**。递归下降的语法分析器实际上是若干递归方法的集合，每个方法对应一条规则。下降的过程就是从语法分析树的根节点开始，朝着叶节点(词法符号)进行解析的过程。首先调用的规则，即语义符号的起始点，就会称为语法分析树的根节点。这种解析的别名是**自上而下的解析**，递归下降的语法分析器仅仅是自上而下的语法分析器的一种实现。

下面是一个ANTLR根据assign规则生成的方法:

```java
// assign : ID '=' expr ';';
void assign() {
    match(ID);     // 根据assign规则生成的方法
    match('=');    // 将当前的输入符号和ID相比较，然后将其消费掉
    expr();        
    match(';');    // 通过调用方法expr()来匹配一个表达式
}
```

递归下降语法分析器通过方法调用描绘出的路线图映射到了语法分析树的节点上。调用`match()`对应了语法分析树的叶子结点。

对`stat`语法规则的解析像是一个switch语句:

```java
void stat() {
    switch (token/*当前的词法符号*/) {
        case ID:
            assign();
            break;
        case IF:
            ifstat();
            break;
        case WHILE:
            whilestat();
            break;
        default:
            throw;
    }
}
```

一般使用**前瞻词法符号(lookahead token)**这个术语，它其实就是下一个输入的词法符号。一个前瞻词法符号是任何一个在被匹配和消费之前就由语法分析器主动找出的词法符号。有些时候，语法分析器需要很多个前瞻词法符号来判断语义规则的哪个方案是正确的，甚至可能要从当前的词法符号的位置开始，一直分析到文件末尾才能做出判断！

### 语法的歧义

```antlr
stat: ID '=' expr ';' // 匹配一个赋值语句
    | ID '=' expr ';' // 重复了前一个备选分支
    ;
expr: INT;
```

或者下面这个嵌套了一层的歧义语法

```antlr
stat: expr ';'  // 表达式语句
    | ID '(' ')' ';'  // 函数调用语句
    ;
expr: ID '(' ')'
    | INT
    ;
```

一个歧义性语法通常被认为是程序设计上的bug。需要重新组织语法，使得对于每个输入的词组，语法分析器都能够选择唯一匹配的备选分支。如果语法分析器检测到该词组存在歧义，就必须在多个备选分支中做出选择。ANTLR解决歧义问题的方法是：选择所有匹配的备选分支中的第一条。

歧义问题在词法分析器和语法分析器中都会发生，ANTLR的解决方案使得对规则的解析能够正常进行。在词法分析器中，ANTLR解决歧义问题的方法是：匹配在语法定义中最靠前的那条词法规则。 

比如下面的关键字和标识符冲突:

```
BEGIN: 'begin';  // 匹配begin关键字
ID   : [a-z]+    // 匹配一个或者多个小写字母
```

词法分析器会匹配可能的最长字符串来生成一个词法符号，这意味着，输入文本`beginner`只会匹配上例中的ID这条词法规则。ANTLR词法分析器不会把它匹配为关键字BEGIN后跟着标识符ner。

有时候，一门语言的语法本身就存在歧义，无论如何修改语法也不能改变这一点。例如，最常见的数学表达式`1+2*3`可以用两种方式解释，一种是像绝大多数编程语言一样，按照优先级来处理。当然可以隐式地制定表达式中的运算符优先级。

### 使用语法分析树构建语言类应用程序

<!-- ANTLR 权威指南中文版 看到了45页 -->

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


