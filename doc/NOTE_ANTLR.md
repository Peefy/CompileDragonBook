
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

或者打印出LISP风格文本格式的语法分析树

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

为了编写一个语言类应用程序，必须对每个输入的词组或者子词组执行一些适当的操作。进行这项工作最简单的方式是操作语法分析器自动生成的语法分析树。这种方式的优点在于，可以使用Java。

词法分析器处理字符序列并将生成的词法符号提供给语法分析器，语法分析器随机根据这些信息来检查语法的正确性并建造出一棵语法分析树。这个过程对应的ANTLR类是`CharStream`、`Lexer`、`Token`、`Parser`以及ParseTree。连接词法分析器和语法分析器的“管道”。

ANTLR尽可能很多地使用共享数据结构来节约内存。语法分析树中的叶子结点(词法符号)仅仅是盛放词法符号流中的词法符号的容器。每个词法符号都记录了自己在字符序列中的开始位置和结束位置，而非保存子字符串的拷贝。其中，不存在空白字符对应的词法符号的原因是，假定词法分析器会丢弃空白字符。

因为语法分析树根节点包含了使用规则识别词组过程中的全部信息，它们被称为上下文(context)对象。每个上下文对象都知道自己识别出的词组中，开始和结束位置处的词法符号，同时提供访问该词组全部元素的途径。例如，AssignContext类提供了方法`ID()`和方法`expr()`来访问标识符节点和代表表达式的子树。

给定这些类型的具体实现，可以手工写出对语法分析树进行深度优先遍历的代码。这样，在访问其中的节点时，可以进行一切所需的操作。这个过程中的典型操作是诸如计算结果、更新数据结构或者产生输出一类的事情。实际上，可以利用ANTLR自动生成并遍历树的机制，而不需要每次都重复编写遍历树的代码。

### 语法分析树监听器和访问器

ANTLR的运行库提供了两种遍历树的机制。默认情况下，ANTLR使用内建的遍历器访问生成的语法分析树，并为每个遍历时可能触发的事件生成一个语法分析树监听器接口。监听器非常类似于XML解析器生成的SAX文档对象。SAX监听器接收类似`startDocument()`和`endDocument()`的事件通知。一个监听器的方法实际上就是回调函数，正如在图形界面程序中响应复选框点击事件一样。除了监听器的方式，还将介绍另外一种遍历语法分析树的方式：**访问者模式(visitor pattern)**。

#### 语法分析树监听器

为了将遍历树时触发的事件转化为监听器的调用，ANTLR运行库提供了ParseTree-Walker类。可以自行实现ParseTreeListener接口，在其中填充自己的逻辑代码(通常是调用程序的其他部分)，从而构建出自己的语言类应用程序。

ANTLR为每个语法文件生成ParseTreeListener的子类，在该类中，语法中的每条规则都对应的enter方法和exit方法。例如，当遍历器访问到assign规则对应的节点时，它就会调用`enterAssign()`方法，然后将对应的语法分析树节点----AssignContext的实例。

#### 语法分析树访问器

有时候，希望控制遍历语法分析树的过程，通过显式的方法调用来访问子节点。在命令行中加入`-visitor`选项可以指示ANTLR为一个语法生成访问器接口(visitor interface)，语法中的每条规则对应接口中的一个visit方法。

ANTLR会提供访问器接口和一个默认实现类，只关注感兴趣的方法即可。

### 小结

* **语言**-一门语言是一个有效语句的结合。语句由词组组成，词组由子词组组成，子词组又由更小的子词组组成，一次类推。
* **语法**-语法定义了语言的语义规则。语法中的每条规则定义了一种词组结构
* **语义树或语法分析树**-代表了语句的结构，其中的每个子树的根节点都使用了一个抽象的名字给其包含的元素命名。
* **词法符号**-词法符号就是一门语言的基本词汇符号，它们可以代表像是"标识符"这样的一类符号，也可以代表一个单一的运算符，或者代表一个关键字。
* **词法分析器或者词法符号生成器**-将输入的字符序列分解称一系列词法符号。一个词法分析器负责分析词法。
* **语法分析器**-语法分析器通过检查语句的结构是否符合语法规则的定义来验证该语句在特定语言中是否合法。语法分析的过程类似于走迷宫。ANTLR能够生成被称为`ALL(*)`的自顶向下的语法分析器，`ALL(*)`是指它可以利用剩余的所有输入文本来进行决策。自顶乡下的语法分析器以结果为导向，首先匹配粗粒度的规则，这样的规则通常命名为`program`或者`inputFile`
* **递归下降的语法分析器**-这是自顶向下的语法分析器的一种实现，每条规则都对应语法分析器中的一个函数
* **前向预测**-语法分析器使用前向预测来进行决策，具体方法是：将输入的符号与每个备选分支的起始符号进行比较。

## 入门的ANTLR项目

比如识别一些像是`{1, 2, 3}`和`{1, {2, 3}, 4}`这样的花括号括起来语句。

### ANTLR工具、运行库以及自动生成的代码

在ANTLR的jar包中存在两个关键部分：ANTLR工具和ANTLR运行库(运行时语法分析)API。通常，对一个语法运行ANTLR时，指的是运行ANTLR工具，即`org.antlr.v4.Tool`类来生成一些代码(语法分析器和词法分析器)，它们能够识别使用这份语法代表的语言所写成的语句。词法分析器将输入的字符流分解为词法符号序列，然后将它们传递给能够进行语法检查的语法分析器。运行库是一个由若干类和方法组成的库，这些类和方法是自动生成的代码（如Parser,Lexer和Token）运行所必须的。因此，完成工作的一般步骤是：首先对一个语法运行ANTLR，然后将生成的代码与jar包中的运行库一起编译，最后将编译好的代码和运行库放在一起运行。

构建一个语言类应用程序的第一步是创建一个能够描述这种语言的语法(即合法语句结构的集合)的语法。

```antlr
grammar ArrayInit;

/* 一条名为init的规则，它匹配一对花括号中的、逗号分隔的value */
init : '{' value (',' value)*  '}';
/* 一个value可以是嵌套的花括号结构，也可以是一个简单的整数，即INT词法符号 */
value : init
      | INT
      ;
INT:  [0-9]+;              // 定义词法符号INT，它由一个或多个数字组成
WS:   [\t\r\n]+ -> skip;   // 定义词法规则“空白符号”，丢弃之
```

ANTLR语法比正则表达式功能更强大，实际上，由于嵌套的花括号结构的存在，正则表达式无法识别这样的初始化语句。正则表达式没有存储的概念，它们无法记住之前匹配过的额输入，因此，它们不能将左右花括号正确配对。

### 测试生成的语法生成器

```sh
$ grun ArrayInit init -tokens 
$ {99, 3, 451}
$ EOF
```

或者

```sh
$ grun ArrayInit init -tree
$ {99, 3, 451}
$ EOF
```

```sh
$ grun ArrayInit init -gui
$ {99, 3, 451}
$ EOF
```

用自然语言表述，语法分析树就是，“输入的是一个由一对花括号包裹的三个值组成的初始化语句”

*注意：文件结束符(end of file EOF)在类UNIX系统上的输入方法是`Ctrl+D`，在Windows上的方法是`Ctrl+Z`*

### 将生成的语法分析器与Java程序集成

```java
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

public class Test {
    public static void main(String[] args) throws Exception {
        ANTLRInputStream input = new ANTLRInputStream(System.in);
        ArrayInitLexer lexer = new ArrayInitLexer(input);
        CommonTokenStrem tokens = new CommonTokenStream(lexer);
        ArrayInitParser lexer = new ArrayInitParser(tokens);
        ParseTree tree = parser.init();
        System.out.println(tree.toStringTree(parser));
    }
}
```

### 构建一个语言类应用

可以使用ANTLR监听器在语法树的遍历中调用回调函数，比如把`{99,3,451}`的short数组翻译成`\u0063\u0003\u01c3`。

最简单的方式是使用ANTLR内置的语法分析树遍历器进行深度优先遍历，然后在它触发的一系列回调函数中进行适当的操作。这样的监听器非常类似于图形界面程序控件上的回调函数。

```java
public class ShortToUnicodeString extends ArrayInitBaseListener {
    @Override
    public void enterInit(ArrayInitPaser.InitContext ctx) {
        System.out.print('\"');
    }

    @Override
    public void exitInit(ArrayInitPaser.InitContext ctx) {
        System.out.print('\"');
    }

    @Override
    public void enterValue(ArrayInitPaser.ValueContext ctx) {
        int value = Integer.valueOf(ctx.INT().getText());
        System.out.printf('\\u%04x', value);
    }
}
```

然后在主程序中调用：

```java
ParseTreeWalker walker = new ParseTreeWalker();
walker.walk(new ShortToUnicodeString(), tree);
System.out.println();
```

## ANTLR 快速指南

### 匹配算术表达式的语言

第一个语法用于构建一个简单的计算器，其对算术表达式的处理具有十分重要的意义，因为它们很常见。为简单起见，只允许基本的算术操作符(加减乘除)、圆括号、整数以及变量出现。例子中的算术表达式限制浮点数的使用，只允许整数出现。

下面的示例包含了本语言的全部特性：

```expr
193
a = 5
b = 6
a+b*2
(1+2)*3
```

用自然语言来说，表达式语言组成的程序就是一系列语句，每个语句都由换行符终止。一个语句可以是一个表达式、一个赋值语句或者是一个空行。下面是对应的ANTLR语法：

```antlr
grammar Expr;
/* 起始规则，语法分析的起点 */
prog: stat+;
stat: expr NEWLINE
    | ID '=' expr NEWLINE
    | NEWLINE
    ;
expr: expr ('*'|'/') expr
    | expr ('+'|'-') expr
    | INT
    | ID
    | '(' expr ')'
    ;
ID:  [a-zA-Z]+;     // 匹配标识符
INT: [0-9]+;        // 匹配整数
NEWLINE: 'r'? '\n'; // 告诉语法分析器一个新行的开始(即语句终止标志)
WS:  [\t]+ -> skip; // 丢弃空白字符
```

* 语法包含一系列描述语言结构的规则。这些规则既包括类似`stat`和`expr`的描述语法结构的规则，也包括描述标识符和整数之类的词汇符号(词法符号)的规则。
* 语法分析器的规则以小写字母开头
* 词法分析器的规则以大写字母开头
* 使用`|`来分隔同一个语言规则的若干备选分支，使用**圆括号**把一些符号组合成子规则。例如，子规则`('*'|'/')`匹配一个乘法符号或者一个除法符号。

**ANTLR4的最重要的新功能之一就是，它能够处理(大部分情况下)左递归规则**。左递归规则是指这样的语言规则：在某个备选分支的起始位置调用了自身。例如，在上述语法中，expr规则的备选分支出现了自身expr规则。使用这种方式指定算术表达式远比传统的自顶向下语法分析器策略简单。在传统的语法分析策略中，需要为运算符的每种优先级编写一条规则。

词法符号定义的标记和正则表达式的元字符非常相似。

* `'+'`-加号代表前面的字符必须至少出现一次（1次或多次）。
* `'*'`-星号代表字符可以不出现，也可以出现一次或者多次（0次、或1次、或多次）。
* `'?'`-问号代表前面的字符最多只可以出现一次（0次、或1次） 

*注意：在WS词法规则后面的`-> skip`操作是一条指令，告诉词法分析器匹配并丢弃空白字符。*通过使用正式的ANTLR标记，而非嵌入一段代码来告诉词法分析器忽略这些字符，就能避免语法和某种特定的目标语言绑定。

### 利用访问器构建一个计算器

可以使用ANTLR4的语法分析树访问器和其他的遍历器来实现语言类应用程序，从而保持语法本身的整洁。

首先给备选分支加上标签：

```antlr
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
WS:  [ \t]+ -> skip; //丢弃制表符和空白符
MUL:  '*';
DIV:  '/';
ADD:  '+';
SUB:  '-';
```

```java
import java.util.HashMap;
import java.util.Map;

public class EvalVisitor extends LabledExprBaseVisitor<Integer> {
    Map<String, Integer> memory = new HashMap<String, Integer>();

    /* ID '=' expr NEWLINE */
    @Override
    public Integer visitAssign(LabeledExprParser.AssignContext ctx) {
        String id = ctx.ID().getText();
        int value = visit(ctx.expr());
        memory.put(id, value);
        return value;
    }

    /* expr NEWLINE */
    @Override
    public Integer visitPrintExpr(LabeledExprParser.AssignContext ctx) {
        Integer value = visit(ctx.expr());
        System.out.println(value);
        return 0;
    }

    /* ID */
    @Override
    public Integer visitId(LabeledExprParser.AssignContext ctx) {
        String id = ctx.ID().getText();
        if (memory.containsKey(id))
            return memory.get(id);
        return 0;
    }
    
    /* expr op=('*'|'/') expr */
    @Override
    public Integer visitMulDiv(LabeledExprParser.AssignContext ctx) {
        int left = visit(ctx.expr(0));
        int right = visit(ctx.expr(1));
        if (ctx.op.getType() == LabledExprParser.MUL) 
            return left * right;
        return left / right;
    }

    /* expr op=('+'|'-') expr */
    @Override
    public Integer visitMulDiv(LabeledExprParser.AssignContext ctx) {
        int left = visit(ctx.expr(0));
        int right = visit(ctx.expr(1));
        if (ctx.op.getType() == LabledExprParser.ADD) 
            return left + right;
        return left - right;
    }

}
```

### 利用监听器构建一个翻译程序

将一个Java类中的全部方法抽取出来，生成一个接口文件，保留方法签名中的空白字符和注释。

部分`Java.g4`示例

```antlr
classDeclaration :
    'class' Identifier typeParameters? ('extends' type)?
    ('implements' typeList)?
    classBody;
methodDeclaration :
    type Identifier formalParameters ('[' ']')* methodDelarationRest
    | 'void' Identifier formalParameters methodDelarationRest
    ;
```

基本思想是，在类定义的起始位置打印出接口定义，然后在类定义的结束位置打印出`}`。在遇到每个方法定义时，将会抽取出它的签名。

访问器和监听器机制表现出色，它们使语法分析过程和程序本身高度分离。尽管如此，有些时候，还是需要额外的灵活性和可操控性。

### 定制语法分析过程

监听器和访问器机制是一个创举，这使得自定义的程序代码和语法本身分离开来，让语法更具可读性，避免了将语法和特定的程序混杂子一起。不过，为了灵活性和可操控性，可以直接将代码片段(动作)嵌入语法中。这些动作将被拷贝到ANTLR自动生成的递归下降语法分析器的代码中。

将会看到如何实现特殊的动作，叫做**语义判定(samantic predicate)**,它能够动态地开启或者关闭部分语法。

#### 在语法中嵌入任意动作

如果不想承担建立语法分析树的开销，可以在语法分析的过程中计算并打印结果。另一个方案是，在“表达式语法”中嵌入一些代码。

```antlr
grammar Rows;

@parser::members {
    int col;
    public RowsParser(TokenStream input, int col) {
        // 自定义构造器
        this(input);
        this.col = col
    }
}

file: (row NL)+;

row
locals [int i = 0]
    : (STUFF
    {
        $i++;
        if ($i == col) System.out.println($STUFF.text);
    }
    )+
    ;

TAB : '\t' -> skip; // 匹配但是不将其传递给语法分析器
NL :  '\r'? '\n';   // 匹配并将其传递给语法分析器
STUFF ~[\t\r\n]+    // 匹配除tab符和换行符之外的任何字符
```

语义判定

```antlr
grammar Data;

file : group+
group: INT sequence[$INT.int];
sequence[int n]
locals [int i = l;]
: ( {$i<=$n}? INT {$i++} )* // 匹配n个函数
;
INT : [0-9]+;
WS : [ \t\n\r] // 丢弃所有空白字符
```

### 词法分析特性

#### 孤岛语法：处理相同文件中的不同格式

事实上，有很多常见的文件格式包含多重语言。例如，Java文档注释中的`@author`标签等内容使用的是一种特殊的微型语言；在注释之外的一切内容都是Java代码。需要将模版语言表达式之外的文本按照不同的方式进行处理，这种情况通常称为孤岛语法。

ANTLR提供了一个众所周知的词法分析器特性，称为**词法分析模式(lexical mode)**，使能够方便地处理混杂着不同格式数据的文件。它的基本思想是，当词法分析器看到一些特殊的“哨兵”字符序列时，执行不同模式的切换。

XML是个很好的例子。一个XML解析器除了标签和实体转义时(例如`&pound;`)之外的东西全部当作普通文本。当看到`<`时，词法分析器会切换到“标签内部”模式；当看到`>`或者`/>`时，它就切换回默认模式。

```antlr
lexer grammar XMLLexer;

// 默认的“模式”，所有在标签之外的东西
OPEN  : '<'   -> pushMode(INSIDE);
COMMENT :  '<!--' .*? '-->'  -> skip;
EntityRef: '&' [a-z]+ ';' ;
TEXT   :  ~('<'|'&')+ ;  // 匹配任意除<和&之外的16位字符

// ------- 所有在标签之内的东西 ------
mode INSIDE;
CLOSE  :  '>' -> popMode;
SLASH_CLOSE :  '/>' -> popMode;
EQUALS  :  '=' ;
STRING :    '"'  .*? '"' ;
SlashName  :  '/' Name;
Name  :  ALPHA (ALPHA|DIGIT)*;
S : [ \t\r\n] -> skip;

fragment
ALPHA : [a-zA-Z];

fragment
DIGIT  : [0-9];
```

如果需要令测试组件只运行词法分析器而不运行语法分析器，可以指定参数为语法名加上一个特殊的规则名tokens。

#### 重写输入流

构建一个小工具，能够修改Java源代码并插入`java.io.Serializable`使用的序列化版本标识符(serialVersionUID,类似Eclipse的自动生成功能)。简单的做法是：在原先的词法符号流中插入一个适当代表常量字段的词法符号，然后打印出修改后的输入流。

```java
// 打印除修改后的词法符号流
System.out.println(extractor.rewriter.getText());
```

在监听器的实现中，在类定义的起始位置触发一个插入操作：

```java
import org.antlr.v4.runtime.TokenStrem;
import org.antlr.v4.runtime.TokenStremRewriter;

public class InsertSerialIDListener extends JavaBaseListener {
    TokenStreamRewriter rewiter;
    public InsertSerialIDListener(TokenStream tokens) {
        rewiter = new TokenStreamRewriter(tokens);
    }

    @Override
    public void enterClassBody(JavaParser.ClassBodyContext ctx) {
        String field = "\n\tpublic static final long serialVersionUID = 1L;"
        rewriter.insertAfter(ctx.start, field);
    }
}
```

其中的关键之处在于，`TokenStreamRewriter`对象实际上修改的是词法符号流的“视图”而非词法符号流本身。它认为所有对修改方法的调用都只是一个“指令”，然后将这些修改放入一个队列，在未来词法符号流被重新渲染为文本时，这些修改才会被执行。每次调用`getText()`方法时，`rewriter`对象都会执行上述队列中的指令。

#### 将词法符号流送入不同通道

使用传统方法很难达到保留方法签名中的空白字符和注释，对于大多数语法，词法分析器是可以忽略空白字符和注释的。如果不想让空白字符和注释在语法中到处都是，可以让词法分析器丢弃它们。忽略却保留注释和空白的方法是将词法符号送入一个“隐藏通道”。

```antlr
COMMENT
    : '/*' .*? '*/' -> channel(HIDDEN)   //匹配 '/*' 和 '*/' 之间的任何东西 
    ;
WS  : [ \r\t\u000C\n]+ -> channel(HIDDEN)
```

同之前讨论的`-> skip`一样，`-> channel(HIDDEN)`也是一个词法分析器指令。此处，设置了这些词法符号的通道号，这样，这些词法符号就会被语法分析器忽略。词法符号流仍然保存着这些原始的词法符号序列，只不过在向语法分析器提供数据时忽略了那些处于已关闭通道的词法符号。

### 小结

实现的访问器和监听器让不需要在语法中嵌入动作就能完成计算和翻译工作。内嵌动作是进行特殊的内部控制必不可少的手段，以及切换不同的模式和隐藏通道等都可以解决一些不大但是现实的问题。

## ANTLR 设计语法

ANTLR一些实用的细节，例如建立内部数据结构，提取信息，以及翻译输入内容等。研究编程语言的通用模式，将其在语句中辨识出来。一种语言模式就是一种递归的结构，例如英语的一个句子包含`主语-谓语动词-宾语`。需要从一系列有代表性的输入文件中归纳出一门语言的结构。在完成这样的归纳工作后，就可以正式实用ANTLR语法来表达这门语言了。

在词法层面上，不同编程语言也倾向于实用相同的结构，例如标识符、整数、字符串等等。对单词顺序和单词间依赖关系的限制来源于自然语言，逐渐发展为以下四种抽象的计算机语言模式：

* **序列**-既一列元素，例如一个数组初始化语句中的值
* **选择**-在多种可选方案中做出选择，例如编程语言中的不同种类的语句
* **词法符号依赖**-一个词法符号需要和某种的另外一个词法符号匹配，例如左右括号匹配
* **嵌套结构**-一种自相似的语言结构，例如编程语言中的嵌套算术表达式或者嵌套语句块。

为实现以上模式，语法规则只要可选方案、词法符号引用和规则引用即可**Backus-Naur-Format,BNF**。为方便起见，还是将这些元素划分为子规则，与**正则表达式**类似，子规则是用`括号()`包围的内联规则。可以用以下符号标记子规则，用于指明其中的语法片段出现的次数：`可选(?)`；`出现0次或多次(*)`；`至少一次(+)`；(扩展巴克斯-诺尔范式，Extended Backus-Naur Format)。

### 从编程语言的范例代码中提取语法

编写语法和编写软件很相似，差异在于处理的是语言规则，而非函数或者过程(procedure)。(记住，ANTLR将会为语法中每条规则生成一个函数)。

任何编程语言项目的基础步骤：讨论语法的整体结构以及如何建立初始的语法框架。

ANTLR语法由一个为该语法命名的头部定义和一系列可以相互引用的语言规则组成。

```antlr
grammar MyG;
rule1 : <stuff>;
rule2 : <more stuff>;
```

和编写软件一样，必须指明需要的语言规则，既其中`<stuff>`的具体内容，以及哪条规则是起始规则。

为了给某种编程语言编写语法，必须要么精通它，要么有很多代表性的、由该语言所编写的样例程序。设计良好的语法反映了编程世界中的功能分解或者自顶向下的设计。设计起始规则的内容实际上就是实用“英语伪代码”来买描述输入文本的整体结构，和编写软件的过程有点类似。

从顶层开始，降低一个层次，描述起始规则右侧所指定的那些元素。它右侧的名词通常是词法符号或者尚未定义的规则。其中，词法符号是那些能够轻易识别出的单词、标点符号或者运算符。词法符号是文法的基本元素。起始规则引用了其他的、需要进一步细化的语言结构。

再降低一层，一个行就是一系列由逗号分隔的字段。一个字段就是一个数字或者字符串。伪代码如下:

```antlr
file: <sequence of rows that are terminated by newlines>
row: <sequence of fields separated by commas>;
field: <number of strings>
```

当完成对规则的定义后，语法草稿就成形了。

### 以现有的语法规范为指南

一份非ANTLR格式的语法规范能够很好地知道编程者理清该语言的结构。但是请把参考手册当作一份指南，而非一份代码。

出于使语法更清晰的目的，参考手册的范围通常都非常宽泛，这意味着其中的语法能够匹配很多实际上不合法的语句。或者，语法可能存在歧义，能够以多种方式匹配相同的输入文本。例如。

刚开始的时候，辨识一条语法规则并使用伪代码编写右侧的内容是一项充满挑战的工作，不过，会随着为不同语言编写语法的过程变得越来越容易。一旦拥有了伪代码，就需要将它翻译为ANTLR标记，从而得到一个能够正常工作的语法。

### 使用ANTLR语法识别常见的语言模式

现在需要关注的是常见的语言模式：序列(sequence),选择(choice)，词法符号依赖(token dependency),以及嵌套结构(nested phrase)。会用正式的语法规则将特定的模式表达出来，通过这种方式，就能够掌握基本的ANTLR标记的用法。

#### 序列模式

在计算机编程语言中，这种结构最常见的形式是一列元素，就像上文中的类定义中包括一系列方法一样。即使是像HTTP、POP和SMTP这样的简单的“协议语言”中，也能够看到序列模式的身影。协议的输入通常是一列指令。例如，下面是登陆一台POP服务器并获取第一条消息的指令序列：

```
USER parrt
PASS secret
RETR 1
```

每个指令自身也是一个序列。大多数指令由一个类似USER和RETR的关键字(保留字)，一个操作数和一个换行符构成。在上述例子中，可以说一个检索指令就是一个关键字，后面跟着一个整数，再后面是一个换行符。使用语法来表述这样的序列，只需要按照顺序将它们列出即可。在ANTLR标记中，检索指令可表达为：

```antlr
retr : 'RETR' INT '\n' ;  // 匹配 “关键字-整数-换行符” 序列
```

注意，可以直接使用类似`RETR`的常量字符来表示任意简单字符序列，诸如关键字或者标点符号等。使用语法规则来为编程语言的特定结构命名，就像在编程时将若干语句组合成一个函数。

使用语法规则来为编程语言的特定结构命名，就好像在编程时将若干个语句组合成一个函数。在上例中，将RETR命名为retr规则。这样，在语法的其他地方，可以直接把规则名作为简称来引用RETR。

序列模式的变体包括：带终止符的序列模式和带分隔符的序列模式。CSV文件同时使用了这两种模式。

下面是在先前的章节中使用ANTLR标记写出的伪代码语法：

```antlr
file : (row '\n')*;       // 以一个'\n'作为终止符的序列
row : field (',' field)*; // 以一个','作为分隔符的序列
field : INT;              // 假设字段都是整数
```

下面的语法匹配类似Java的、每个语句都由分号结束的编程语言：

```antlr
stats : (stat ';')*;  // 匹配零个或多个以';'终止的语句
```

与之相似，下面的语法匹配以逗号分隔的多个表达式，可以在一次函数调用的参数列表中找到这样的例子：

```antlr
expList : expr (',' expr)*;
```

就连ANTLR元语言也使用了序列模式。下面的语法片段显示了ANTLR是如何使用它自身的句法表达“规则定义”这条句法的：

```antlr
// 匹配这样的结构：'规则名:'后面跟着至少一个备选分支,
// 然后是若干条以'|'符号分隔的备选分支，最后是一个';'
rule ID ';' alternative ('|' alternative)* ';'
```

#### 选择模式（多个备选分支）

使用'|'符号作为“或者”来表达编程语言中的选择模式，在ANTLR的规则中，用于分隔多个可选的语法结构----称作备选分支(alternatives)或者可生成的结果(productions)。选择模式在语法中随处可见。

```antlr
field : INT | STRING;
type : 'float' | 'int' | 'void';
```

```
stmt: node_stmt
    | edge_stmt
    | attr_stmt
    | id '=' id
    | subgraph
    ;
```

语法中的序列模式和选择模式使能够编写语言的框架，但是这还不够，接下来，还有两种关键的模式：词法符号依赖和嵌套结构。在语法中，它们通常是一起出现的，不过，为简单起见，先来单独分析词法符号依赖模式。

#### 词法符号依赖模式

需要一种方法来表达对这样的词法符号的依赖。此时，如果在某个语句中看到了某个符号，就必须在同一个语句中找到和它配对的那个符号。为表达出这种语义，在语法中，使用一个序列来指明所有配对的符号，通常这些符号会把其他元素分组或者包裹起来。比如指定一个完整的向量:

```antlr
vector : '[' INT+ ']';
```

几乎所有的用于分组的符号都是成对出现的：

```antlr
expr : expr '(' exprList? ')'
    | expr '[' expr ']'
    ...
    ;
```

也可以在方法声明中看到左右圆括号之间的词法符号依赖模式：

```antlr
functionDecl
    : type ID '(' formalParameters ')'
    ;
formalParameters
    : formalParameter (',' formalParameter)*
    ;
formalParameter
    : type ID
```

*注意：一个有依赖符号并非必须匹配到它所依赖的符号。在C语言基础上发展起来的编程语言通常拥有a?b:c三元运算符。只有在这种情况下，?才依赖其后的:*

此外，词法符号间的依赖并不意味着一定存在嵌套结构。一个向量中可能不允许出现嵌套的向量。

#### 嵌套模式

嵌套的词组是一种自相似的语言结构，既它的子词组也遵循相同的结构。表达式是一种典型的自相似结构，它包含多个嵌套的、以运算符分隔的子表达式。与之相似，一个while循环代码块是一个嵌套在更外层代码块中的代码块。在语法中，使用递归规则来表达这种自相似的语言结构。所以，如果一条规则定义中伪代码引用了它自身，我们就需要一条递归规则 (自引用规则)。

```antlr
stat : 'while' '(' expr ')' stat  // 匹配WHILE语句
    | '{' stat* '}'               // 匹配花括号中若干条语句组成的代码块
    ...                 
    ;
```

其中，while中的stat是一个循环结构，它可以是一个语句或者由花括号包裹的一组语句。因为stat规则在前两个备选分支中引用了自身，称它为**直接递归(directly recursive)**的。如果将它的第二个备选分支抽取出来，stat规则和block规则就会互为**间接递归(indirectly recursive)**的

```antlr
stat: 'while' '(' expr ')' stat   // 匹配WHILE语句
    | block                       // 匹配一个语句组成的代码块
    ...                           // 其他种类的语句
    ;
block: '{' stat* '}';             // 匹配花括号中若干条语句组成的代码块
```

大部分编程语言都包含多种形式的自相似结构，这带来的结果是语法中包含很多递归规则。下面是一个简单的例子：只有三种表达式类型：数组索引表达式、括号表达式和整数----的编程语言。下面是用ANTLR标记书写的语法：

```antlr
expr : ID '[' expr ']'
    | '(' expr ')'
    | INT
    ;
```

其中的递归发生的非常自然。因为一个数组的索引值本身也是一个表达式，所以就在对应的备选分支中直接引用了expr。实际上，索引值本身也可以是一个数组索引表达式。从这个例子中可以看到，语言结构上的递归自然而然地使得语言规则发生了递归。

语法分析树的非叶子节点代表了规则，而叶子节点代表了词法符号。一条从根节点到任意节点的路径代表了对应的规则调用栈(同时也是ANTLR自动生成的递归下降语法分析器的调用栈)。因此，代表递归调用的路径上就会存在对多个相同规则的引用。

并非所有的语言都有表达式，例如数据格式定义。不过所接触的大多数语言都包含非常复杂的表达式。

ANTLR的核心语法标记

用法|描述
-|-
`x`|匹配词法符号、规则引用或者子规则`x`
`x` `y` ... `z`|匹配一列规则元素
`(...|...|...)`|一个具有多个备选分支的子规则
`x?`|匹配`x`或者忽略它
`x*`|匹配`x`零次或多次
`x+`|匹配`x`一次或多次
`r:...;`|定义规则`r`
`r:...|...|...;`定义具有多个备选分支的规则`r`

几种常见的计算机语言模式：

* **序列模式**-它是一个有限长度或者任意长度的序列，序列中的元素可以是词法符号或者子规则。序列模式的用例包括了变量声明（类型后面紧跟着标识符）和整数序列。

```antlr
x y ... z
'[' INT+ ']'
```

* **带终止符的序列模式**-它是一个任意场的、可能为空的序列，该序列由一个词法符号分隔开，通常是分号或者换行符，其中的元素可以是词法符号或者子规则。

```antlr
(statement ';')*
(row '\n')*  // 多行数据
```

* **带分隔符的序列模式**-它是一个任意长的，可能为空的序列，该序列由一个词法符号分隔开，通常是逗号，分号或者句号，其中的元素可以是词法符号或者子规则。这样的例子包括函数定义中的参数表、函数调用时传递的参数表、某些词句之间有分隔符却无终止符的编程语言。

```antlr
expr (',' expr)*   // 函数调用时传递的参数
(expr (',' expr)* )? // 函数调用时传递的参数是可选的
'/' ? name ('/' name)* // 简化的目录名
stat ('.' stat)*  // 若干个SmallTalk语句
```

* **选择模式**-它是一组备选分支的集合。这样的例子不包括种类的类型、语句、表达式或者XML标签。

```antlr
type : 'int' | 'float';
stat : ifstat | whilestat | 'return' expr ';';
expr : '(' expr ')' | INT | ID;
tag : '<' Name attribute* '>' | '<' '/' Name '>';
```

* **词法符号依赖**-一个词法符号需要和一个或者多个后续词法符号匹配。这样的例子包括配对的圆括号、花括号、方括号和尖括号。

```antlr
'(' expr ')'   // 嵌套表达式
ID '[' expr ']' // 数组索引表达式
'{' stat* '}'   // 花括号包裹的若干个语句
'<' ID (',' ID)* '>' //范型声明
```

* **嵌套模式**-它是一种自相似的语言结构。这样的例子包括表达式、Java内部类，嵌套的代码块以及嵌套的Python函数定义。

```antlr
expr : '(' expr ')' | ID;
classDef : 'class' ID '{' (classDef | method | field) '}'; 
```

### 处理优先级、左递归和结合性

<!-- ANTLR 权威指南中文版 看到了135页 -->

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


