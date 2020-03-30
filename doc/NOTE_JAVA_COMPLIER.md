
<!-- 龙书第2版第626--643页-->

# 一个完整的编译器前端

[完整代码链接](https://github.com/Peefy/CompileDragonBook.Cpp/blob/master/src/javacomplier/main)

使用Java代码实现，由5个包组成，`main`，`lexer`，`symbol`，`parser`和`inter`。包inter中包含的类处理用抽象语法表示的语言结构。因为语法分析器的代码和其他各个包交互，所以它将在最后描述。

作为语法分析器的输入时，源程序就是一个由词法单元组成的流，因此面向对象特性和语法分析器代码之间没有什么关系。当由语法分析器输出时，源程序就是一棵**抽象语法树(AST)**，树中的结构或结点被实现为对象。这些对象负责构造一棵AST的结点，类型检查，生成三地址中间代码.

## 源语言

这个语言的一个程序由一个块组成，该块中包含可选的声明和语句。语法符号`basic`表示基本类型

```
program -> block
block -> {decls stmts}
decls -> decls decl | e
decl -> type id
type -> type[num] | basic
stmts -> stmts stmt | e
```

把赋值当成是一个语句（而不是表达式中的运算符）可以简化翻译工作

**面向对象和面向步骤**

在一个面向对象方法中，一个构造的所有代码都集中在这个与构造对应的类中，但是在面向步骤的方法中，这个方法中的代码是按照步骤进行组织的，因此一个类型检查过程中对每个构造都有一个case分支，且一个代码生成过程对每个构造也都有一个case分支，等等。

对这两者进行衡量，可知使用面向对象方法会使得改变或增加一个构造(比如for语句)，变得更容易；而使用面向步骤的方法会使得改变或增加一个步骤(比如类型检查)变得比较容易。使用对象来实现时，增加一个新的构造可以通过写一个自包含的类来实现；但是如果要改变一个步骤，比如插入自动类型转换的代码，就需要改变所有受影响的类。使用面向步骤的方式时，增加一个新的构造可能会引起各个步骤中的多个过程的改变。

```
stmt -> loc = bool;
    | if (bool) stmt
    | if (bool) stmt else stmt
    | while (bool) stmt
    | do stmt while (bool);
    | break;
    | block
loc -> loc[bool] | id;
```

表达式的产生式处理了运算符的结合性和优先级，它们对每个优先级级别都使用了一个非终结符号，而非终结符号factor用来表示括号中的表达式，标识符，数组引用和常量。

```
bool -> bool || join | join
join -> join && equality | equality
equality -> equality == rel | equality != rel | rel
rel -> expr < expr | expr <= expr | expr => expr | 
        expr > expr | expr
expr -> expr + term | expr - term | term
term -> term * unary | term / unary | unary
unary -> !unary | -unary | facfor
factor -> (bool) | loc | num | id | true | false
```

## main package

## 词法分析器

## 符号表和类型

## 表达式的中间代码

## 布尔表达式的跳转代码

## 语句的中间代码

## 语法分析器

## 创建前端

## 
