
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

## ANTLR4 示例

```antlr
// Define a grammar called Hello 定义一个语法名字：Hello
grammar Hello;
r  : 'hello' ID ;         // match keyword hello followed by an identifier 匹配关键字hello，后跟标识符
ID : [a-z]+ ;             // match lower-case identifiers 匹配全是小写字母的标识符
WS : [ \t\r\n]+ -> skip ; // skip spaces, tabs, newlines  跳过空格，制表符，换行符
```


