
# Lark-Parser

用于Python的现代解析库，实现Earley和LALR(1)和简单的接口

Lark-Parser的特点：

* 基于EBNF的高级语法语言
* 三种解析算法可供选择：Earley，LALR(1)和CYK
* 根据语法推断自动进行树构建
* 具有regexp支持和自动行计数的快速unicode词法分析器

## Hello lark

```cmd
pip install lark-parser
```

```py
from lark import Lark

l = Lark('''
            start: WORD "," WORD "!"
            %import common.WORD   // imports from terminal library
            %ignore " "           // Disregard spaces in text
         ''')

print( l.parse("Hello, World!") )
```

```cmd
Tree(start, [Token(WORD, 'Hello'), Token(WORD, 'World')])
```

## Lark-Paresr 官方文档

[链接](https://lark-parser.readthedocs.io/en/latest/philosophy/)

## Lark-Parser 语法

### 定义

一个语法是规则和词法的列表，它们共同定义的语言。词法定义语言的字母，而规则定义语言的结构。在Lark中，词法可以是字符串，正则表达式或这些词法与其他词法的串联。
每个规则都是词法和规则的列表，词法和规则的位置和嵌套定义了所得解析树的结构。

解析算法是一种算法，需要一个语法定义和符号（字母的成员）的序列，以及相匹配的序列的整体通过搜索由所述语法所允许的结构。

### 一般语法和注释

Lark中的文法基于EBNF语法，并进行了一些增强。

可选扩展：

```lark
  a b? c    ->    (a c | a b c)
```

```lark
  a: b*    ->    a: _b_tag
                 _b_tag: (_b_tag b)?
```

文法由一系列定义和词法组成，每个定义和词法各自独立。定义分别是使用以下语法的命名规则或命名词法：

```lark
  rule: <EBNF EXPRESSION>
      | etc.

  TERM: <EBNF EXPRESSION>   // Rules aren't allowed
```

注释//以该行开头和结尾（C ++样式）

Lark从规则`start`开始解析，除非在选项中另有指定。

规则的名称总是小写，而词法的名称总是大写。对于生成的解析树的形状和词法分析器（又称为词法分析器或扫描器）的自动构造，此区别具有实际效果。

词法用于将文本匹配为符号。可以将它们定义为文字和其他词法的组合。

```lark
<NAME> [. <priority>] : <literals-and-or-terminals>
```

文字可以是以下之一：

* "string"
* /regular expression+/
* "case-insensitive string"i
* /re with flags/imulx
* 字面量范围："a".."z"，"1".."9"，等。

词法还支持正则语法运算符，如`|`，`+`，`*`和`?`。

词法是线性结构，因此可能不包含其他词法（不允许递归）。

仅在使用词法分析器时才能为词法分配优先级（未来版本可能支持Earley的动态词法分析）。

优先级可以是正数或负数。如果未为词法指定，则默认为1。

使用词法分析器（标准或上下文）时，语法作者有责任确保文字不发生冲突，或者如果文字发生冲突，则以所需顺序进行匹配。文字根据以下优先级进行匹配：

1. 最高优先级优先（优先级指定为：TERM.number：...）
2. 匹配长度（对于正则表达式，使用最长的理论匹配）
3. 文字/模​​式定义的长度
4. 名称

```lark
IF: "if"
INTEGER : /[0-9]+/
INTEGER2 : ("0".."9")+          //# Same as INTEGER
DECIMAL.2: INTEGER? "." INTEGER  //# Will be matched before INTEGER
WHITESPACE: (" " | /\t/ )+
SQL_SELECT: "select"i
```

最终将每个词法编译为正则表达式。其中的所有运算符和引用都映射到它们各自的表达式。

例如，在以下语法中，A1和A2等效：

```lark
A1: "a" | "b"
A2: /a|b/
```

这意味着即使在使用Earley的情况下，在内部词法中，Lark也无法检测或解决歧义。例如，对于此语法：

```
start           : (A | B)+
A               : "a" | "ab"
B               : "b"
```

得到以下结果：

```py
>>> p.parse("ab")
Tree(start, [Token(A, 'a'), Token(B, 'b')])
```

如果遇到这种情况，建议的解决方案是改用规则。例如："

```py
>>> p = Lark("""start: (a | b)+
...             !a: "a" | "ab"
...             !b: "b"
...             """, ambiguity="explicit")
>>> print(p.parse("ab").pretty())
_ambig
  start
    a   ab
  start
    a   a
    b   b
```

### 规则

```lark
<name> : <items-to-match>  [-> <alias> ]
       | ...
```

规则和别名的名称始终为小写。

可以使用OR运算符（由`|`表示）将规则定义扩展到下一行。

别名是特定规则备选方案的名称。它影响树的构造。

每个项目是以下之一：

* `rule` - 规则
* `TERMINAL` - 词法
* `"string literal"` 或者 `/regexp literal/`
* `(item item ..)` -组合规则
* `[item item ..]` -可选规则。与`(item item ..)?`相同，但在不匹配时生成`None`
* `item?` -匹配0个或者1个，至多匹配1个
* `item*` -匹配0个或者多个
* `item+` -匹配1个或者多个
* `item ~ n` -匹配n个
* `item ~ n..m` -匹配n个到m个

```lark
hello_world: "hello" "world"
mul: (mul "*")? number     //# Left-recursion is allowed and encouraged!
expr: expr operator expr
    | value               //# Multi-line, belongs to expr

four_words: word ~ 4
```

### 优先级

仅当使用Earley时才可以为规则分配优先级（未来版本也可能支持LALR）。

优先级可以是正数或负数。在未为词法指定的情况下，假定为1（即默认值）。

### 指令

* `%ignore`-使语法更简洁。这对于LALR（1）算法尤其重要，因为在语法中显式添加空格（或注释或其他无关的元素）会损害其预测能力（基于前瞻1）。

```lark
%ignore <TERMINAL>
```

* `%import`-导入规则时，它们的所有依赖项都将导入到名称空间中，以避免发生冲突。无法覆盖它们的依赖关系（例如，就像继承类时那样）。

```lark
%import <module>.<TERMINAL>
%import <module>.<rule>
%import <module>.<TERMINAL> -> <NEWTERMINAL>
%import <module>.<rule> -> <newrule>
%import <module> (<TERM1>, <TERM2>, <rule1>, <rule2>)
```

```lark
%import common.NUMBER
%import .terminals_file (A, B, C)
%import .rules_file.rulea -> ruleb
```

* `%declare`-声明一个词法而不定义它

## Lark-Parser 自动树构建

Lark-Parser会根据语法结构自动构建一棵树，其中匹配的每个规则都将成为树中的一个分支（节点），并且其子项是其匹配项（按照匹配顺序）。

例如，该规则`node: child1 child2`将创建一个具有两个子节点的树节点。如果作为另一个规则的一部分进行匹配（即，如果它不是根），则新规则的树节点将成为其父节点。

使用`item+`或`item*`将产生一个项目列表，等同于`item item item ..`。

`item?`如果匹配则返回该项目，否则不返回任何内容。

如果`maybe_placeholders=False`（默认），则`[]`行为类似于`()?`。

如果`maybe_placeholders=True`，则`[item]`将返回匹配的项目，如果不匹配则返回`None`。

词法始终是树中的值，从不分支。不会出现在树中的词法是：

* 未命名文字（如"keyword"或"+"）
* 名称以下划线开头的词法（例如`_DIGIT`）

将显示在树中的词法为：

* 未命名的正则表达式（如`/[0-9]/`）
* 命名词法，其名称以字母开头（如`DIGIT`）

注意：由文字组成的词法和其他词法始终包含整个匹配项，而不过滤任何部分。

```lark
start:  PNAME pname

PNAME:  "(" NAME ")"
pname:  "(" NAME ")"

NAME:   /\w+/
%ignore /\s+/
```

带有前缀的规则`!`将保留其所有文字。

用户可以使用语法特征集合来更改树的自动构造:

* 名称以下划线开头的规则将内联到其包含的规则中。

```lark
    start: "(" _greet ")"
    _greet: /\w+/ /\w+/
```

* 如果在规则的定义开头带有问号`?`的规则具有单个子代，则将对其进行内联。

```lark
    start: greet greet
    ?greet: "(" /\w+/ ")"
          | /\w+/ /\w+/
```

* 以感叹号开头的规则将保留其所有词法（不会被过滤）。

```lark
    !expr: "(" expr ")"
         | NAME+
    NAME: /\w+/
    %ignore " "
```

* 别名-规则中的选项可以接收别名。然后它将用作选项的分支名称，而不是规则名称。

```lark
    start: greet greet
    greet: "hello"
         | "world" -> planet
```

## Lark-Parser的Transformer和Visitor

Transformer和Visitor提供了一个方便的界面来处理Lark-Paresr返回的解析树。

通过从正确的类（访问者或转换器）继承并实现与要处理的规则相对应的方法来使用它们。每个方法都将子级作为参数。可以使用v_args装饰器进行修改，该装饰器允许内联参数（类似于*args），或将tree meta属性添加为参数。

### 访问者Visitor

访问者Visitor访问树的每个节点，并根据该节点的数据在树上调用适当的方法。

它们自下而上地工作，从叶子节点开始，到树的根部结束。


```py
class IncreaseAllNumbers(Visitor):
  def number(self, tree):
    assert tree.data == "number"
    tree.children[0] += 1

IncreaseAllNumbers().visit(parse_tree)
```

有两种实现访问者接口的类：

* 访客-参观每个节点（无递归）
* Visitor_Recursive-使用递归访问每个节点。

### 转换器Transformer

转换器Transformer访问树的每个节点，并根据该节点的数据在树上调用适当的方法。

它们以自下而上（或：深度优先）的方式工作，从叶子节点开始到树的根部结束。

Transformer可用于实现映射和缩小模式。

由于节点从叶子减少到根，因此回调在任何时候都可以假定子代已经转换（如果适用）。

可以使用乘法将Transformer链接到新的Transformer中。

Transformer可以做的任何事情都Visitor都可以做，但是因为它可以重构树，所以效率稍低。

```py
from lark import Tree, Transformer

class EvalExpressions(Transformer):
    def expr(self, args):
            return eval(args[0])

t = Tree('a', [Tree('expr', ['1+2'])])
print(EvalExpressions().transform( t ))

# Prints: Tree(a, [3])
```

下面这些类都实现了转换器接口：

* Transformer-递归地变换树。这可能是想要的。
* Transformer_InPlace-非递归。原地更改树，而不是返回新实例
* Transformer_InPlaceRecursive-递归。原地更改树，而不是返回新实例
  
默认情况下，转换器仅访问规则。`visit_tokens=True`也会告诉Transformer也访问词法符号。`lexer_callbacks`是慢一点的替代方法，但更易于维护，并且适用于所有算法（即使没有词法分析器也是如此）。

```py
class T(Transformer):
    INT = int
    NUMBER = float
    def NAME(self, name):
        return lookup_dict.get(name, name)


T(visit_tokens=True).transform(tree)
```

v_args 是一个装饰器。默认情况下，转换器/访问者的回调方法接受一个参数：节点子级的列表。v_args可以修改此行为。

在转换器/访问者类定义上使用时，它适用于其中的所有回调方法。

v_args 接受以下三个标志之一：

* inline-提供子项*args而不是列表参数（不建议用于很长的列表）。
* meta-提供两个参数：children和meta（而不是第一个）
* tree -提供整个树作为参数，而不是子树。

```py
@v_args(inline=True)
class SolveArith(Transformer):
    def add(self, left, right):
        return left + right


class ReverseNotation(Transformer_InPlace):
    @v_args(tree=True)
    def tree_node(self, tree):
        tree.children = tree.children[::-1]
```

`__default__` 和 `__default_token__`
如果未找到具有相应名称的函数，则会调用这些函数。

* `__default__`方法具有签名`(data, children, meta)`，data是节点的data属性。默认情况下重建树
* `__default_token__`只是需要Token作为参数。默认情况下，仅返回参数。

Discard在转换器回调中引发异常时，该节点将被丢弃，并且不会出现在父节点中。

## Lark-Parser 类参考

### Lark 类

Lark类是该库的主要接口。对于许多不同的解析器和树构造函数。

`__init __(self，grammar_string，**options)`使用给定的语法创建Lark的实例

`open(cls, grammar_filename, rel_to=None, **options)`使用其文件名给出的语法创建Lark的实例。如果提供了rel_to，则函数将找到与其相关的语法文件名。

```py
    >>> Lark.open("grammar_file.lark", rel_to=__file__, parser="lalr")
    Lark(...)
```

`parse(self, text)`返回文本的完整分析树（类型为Tree）。如果将Transformer提供给__init__，则返回转换结果。

`save(self, f) / load(cls, f)`对于缓存和多处理很有用。save 将实例保存到给定的文件对象中，load 从给定的文件对象加载实例

#### Lark-Parser 选项

* `start`-起始符号。一个字符串，或多个可能开始的字符串列表（默认值："开始"）
* `debug`-显示调试信息，例如警告（默认值：False）
* `transformer` -将转换器应用于每个解析树（相当于在解析之后应用它，但速度更快）
* `propagate_positions` -传播（行，列，end_line，end_column）属性为所有树枝。
* `may_placeholders`-为 True时，`[]`运算符不匹配时返回`None`。-为False时， `[]`行为类似于`?`运算符，并且完全不返回任何值。-（默认= False。建议设置为True）
* `g_regex_flags`-应用于所有词法符号（正则表达式和字符串）的标志
* `keep_all_tokens`-防止树构建器自动删除"标点"标记（默认值：False）
* `cache` -缓存Lark语法分析的结果，以便更快地加载x2到x3。仅适用于LALR。-当为False时，不执行任何操作（默认）-当为True时，缓存到本地目录中的一个临时文件中-当给出一个字符串时，缓存到该字符串指向的路径

#### Lark-Parser 算法

* `parser` -决定使用哪个解析器引擎`'earley'`或`'lalr'`。（默认值：`' earley'`）（旧版也有"cyk"选项）
* `lexer`-决定算法是否使用lexer
1. "auto"（默认）：根据解析器为我选择
2. "standard"：使用标准词法分析器
3. "contextual"：更强的词法分析器（仅适用于parser="lalr"）
4. "dynamic"：灵活而强大（仅使用parser="earley"）
5. "dynamic_complete"：与dynamic相同，但是尝试各种可能的标记化方法。（仅适用于parser="earley"）
* `ambiguity` -决定如何处理解析中的歧义。仅当parser ="earley"-"resolve"时才相关：解析器将自动选择最简单的派生（它一致地选择：对词法符号的贪婪，对规则的非贪婪）-"显式"：解析器将返回包装在"_ambig"树节点（即森林）。

#### 特定域

* `postlex` - Lexer后处理（默认：无）仅适用于标准和上下文词法分析器。
* `priority` -如何评估优先级-自动，无，正常，反转（默认：自动）
* `lexer_callbacks`-词法分析器的回调字典。词汇化过程中可能会更改标记。请谨慎使用。
* `edit_terminals`-回调

### Tree 类

* `data` -规则或别名的名称
* `children` -匹配的子规则和词法符号列表
* `meta`-行号和列号（如果`propagate_positions`启用）
  o 元属性：`line`，`column`，`start_pos`，`end_line`，`end_column`，`end_pos`

* `__init__(self, data, children)`创建一个新树，并将"数据"和"子代"存储在相同名称的属性中。

`pretty(self, indent_str=' ')`返回树的缩进字符串表示形式。非常适合调试。

`find_pred(self, pred)`返回评估pred(node)为true的树的所有节点。

`find_data(self, data)`返回其数据等于给定数据的树的所有节点。

`iter_subtrees(self)`深度优先迭代。遍历所有子树，永远不会两次返回同一节点（Lark的解析树实际上是DAG）。

`iter_subtrees_topdown(self)`广度优先迭代。遍历所有子树，并按照pretty()的顺序返回节点。

`__eq__`，`__hash__`可以对树进行散列和比较。

### Token 类

使用词法分析器时，树中的最终标记将属于Token类，该类继承自Python的字符串。因此，正常的字符串比较和操作将按预期工作。词法符号还具有其他有用的属性：

* `type` -词法符号的名称（按语法指定）。
* `pos_in_stream` -词法符号在文本中的索引
* `line` -文本中词法符号的行（以1开头）
* `column` -文本中词法符号的列（以1开头）
* `end_line` -词法符号结束的行
* `end_column` -词法符号结束后的下一列。例如，如果词法符号是column值为4 的单个字符，则为end_column5。
* `end_pos` -词法符号结束处的索引（基本上是pos_in_stream + len(token)）
  
### Transformer/Visitor/Interpreter 类

[参见上一节](#lark-parser的transformer和visitor)

### UnexpectedInput 类

* `UnexpectedToken` -解析器收到意外的词法符号
* `UnexpectedCharacters` -词法分析器遇到意外字符串
  
捕获这些异常之一后，可以调用以下帮助器方法来创建更好的错误消息：

`get_context(text, span)`返回一个漂亮的字符串，指出文本中的错误span及其周围的上下文字符。（解析器不保存它必须解析的文本的副本，因此必须再次提供它）

`match_examples(parse_fn, examples)`允许通过与示例错误进行匹配来检测输入文本中的错误。

接受解析函数（通常`lark_instance.parse`）和字典`{'example_string': value}`。

该函数将迭代字典，直到找到匹配的错误，然后返回相应的值。

## Lark-Parser 实例

### 数组解析

```lark
start : "{" value ("," value)*  "}" 
value : start | INT
INT:  /[0-9]+/    
          
%ignore " "
```

### 忽略注释并使用lexer_callbacks收集所有注释

```py
from lark import Lark

comments = []

parser = Lark("""
    start: INT*

    COMMENT: /#.*/

    %import common (INT, WS)
    %ignore COMMENT
    %ignore WS
""", parser="lalr", lexer_callbacks={'COMMENT': comments.append})

parser.parse("""
1 2 3  # hello
# world
4 5 6
""")

print(comments)
```

### 使用Transformer或者Visitor遍历树

```py
from lark import Lark, Transformer

class T(Transformer):
    def INT(self, tok):
        "Convert the value of `tok` from string to string+int, while maintaining line number & column."
        return tok.update(value='lark-int:' + tok)

parser = Lark("""
start: INT*
%import common.INT
%ignore " "
""", parser="lalr", transformer=T())

print(parser.parse('3 14 159 123'))
```

```py
class IncreaseAllNumbers(Visitor):
  def number(self, tree):
    assert tree.data == "number"
    tree.children[0] += 1

IncreaseAllNumbers().visit(parse_tree)
```

### 使用earley解析歧义文本

```py
from lark import Lark, Tree, Transformer
from lark.visitors import CollapseAmbiguities

grammar = """
    !start: x y

    !x: "a" "b"
      | "ab"
      | "abc"

    !y: "c" "d"
      | "cd"
      | "d"

"""
parser = Lark(grammar, ambiguity='explicit')

t = parser.parse('abcd')
for x in CollapseAmbiguities().transform(t):
    print(x.pretty())
```

### 使用Visitor访问语法树父亲节点

```py
class Parent(Visitor):
    def visit(self, tree):
        for subtree in tree.children:
            if isinstance(subtree, Tree):
                assert not hasattr(subtree, 'parent')
                subtree.parent = tree
```

### Python2

```lark
// Python 2 grammar for Lark

// NOTE: Work in progress!!! (XXX TODO)
// This grammar should parse all python 2.x code successfully,
// but the resulting parse-tree is still not well-organized.

// Adapted from: https://docs.python.org/2/reference/grammar.html
// Adapted by: Erez Shinan

// Start symbols for the grammar:
//       single_input is a single interactive statement;
//       file_input is a module or sequence of commands read from an input file;
//       eval_input is the input for the eval() and input() functions.
// NB: compound_stmt in single_input is followed by extra _NEWLINE!
single_input: _NEWLINE | simple_stmt | compound_stmt _NEWLINE
?file_input: (_NEWLINE | stmt)*
eval_input: testlist _NEWLINE?

decorator: "@" dotted_name [ "(" [arglist] ")" ] _NEWLINE
decorators: decorator+
decorated: decorators (classdef | funcdef)
funcdef: "def" NAME "(" parameters ")" ":" suite
parameters: [paramlist]
paramlist: param ("," param)* ["," [star_params ["," kw_params] | kw_params]]
           | star_params ["," kw_params]
           | kw_params
star_params: "*" NAME
kw_params: "**" NAME
param: fpdef ["=" test]
fpdef: NAME | "(" fplist ")"
fplist: fpdef ("," fpdef)* [","]

?stmt: simple_stmt | compound_stmt
?simple_stmt: small_stmt (";" small_stmt)* [";"] _NEWLINE
?small_stmt: (expr_stmt | print_stmt  | del_stmt | pass_stmt | flow_stmt
          |  import_stmt | global_stmt | exec_stmt | assert_stmt)
expr_stmt: testlist augassign (yield_expr|testlist) -> augassign2
         | testlist ("=" (yield_expr|testlist))+    -> assign
         | testlist

augassign: ("+=" | "-=" | "*=" | "/=" | "%=" | "&=" | "|=" | "^=" | "<<=" | ">>=" | "**=" | "//=")
// For normal assignments, additional restrictions enforced by the interpreter
print_stmt: "print" ( [ test ("," test)* [","] ] | ">>" test [ ("," test)+ [","] ] )
del_stmt: "del" exprlist
pass_stmt: "pass"
?flow_stmt: break_stmt | continue_stmt | return_stmt | raise_stmt | yield_stmt
break_stmt: "break"
continue_stmt: "continue"
return_stmt: "return" [testlist]
yield_stmt: yield_expr
raise_stmt: "raise" [test ["," test ["," test]]]
import_stmt: import_name | import_from
import_name: "import" dotted_as_names
import_from: "from" ("."* dotted_name | "."+) "import" ("*" | "(" import_as_names ")" | import_as_names)
?import_as_name: NAME ["as" NAME]
?dotted_as_name: dotted_name ["as" NAME]
import_as_names: import_as_name ("," import_as_name)* [","]
dotted_as_names: dotted_as_name ("," dotted_as_name)*
dotted_name: NAME ("." NAME)*
global_stmt: "global" NAME ("," NAME)*
exec_stmt: "exec" expr ["in" test ["," test]]
assert_stmt: "assert" test ["," test]

?compound_stmt: if_stmt | while_stmt | for_stmt | try_stmt | with_stmt | funcdef | classdef | decorated
if_stmt: "if" test ":" suite ("elif" test ":" suite)* ["else" ":" suite]
while_stmt: "while" test ":" suite ["else" ":" suite]
for_stmt: "for" exprlist "in" testlist ":" suite ["else" ":" suite]
try_stmt: ("try" ":" suite ((except_clause ":" suite)+ ["else" ":" suite] ["finally" ":" suite] | "finally" ":" suite))
with_stmt: "with" with_item ("," with_item)*  ":" suite
with_item: test ["as" expr]
// NB compile.c makes sure that the default except clause is last
except_clause: "except" [test [("as" | ",") test]]
suite: simple_stmt | _NEWLINE _INDENT _NEWLINE? stmt+ _DEDENT _NEWLINE?

// Backward compatibility cruft to support:
// [ x for x in lambda: True, lambda: False if x() ]
// even while also allowing:
// lambda x: 5 if x else 2
// (But not a mix of the two)
testlist_safe: old_test [("," old_test)+ [","]]
old_test: or_test | old_lambdef
old_lambdef: "lambda" [paramlist] ":" old_test

?test: or_test ["if" or_test "else" test] | lambdef
?or_test: and_test ("or" and_test)*
?and_test: not_test ("and" not_test)*
?not_test: "not" not_test | comparison
?comparison: expr (comp_op expr)*
comp_op: "<"|">"|"=="|">="|"<="|"<>"|"!="|"in"|"not" "in"|"is"|"is" "not"
?expr: xor_expr ("|" xor_expr)*
?xor_expr: and_expr ("^" and_expr)*
?and_expr: shift_expr ("&" shift_expr)*
?shift_expr: arith_expr (("<<"|">>") arith_expr)*
?arith_expr: term (("+"|"-") term)*
?term: factor (("*"|"/"|"%"|"//") factor)*
?factor: ("+"|"-"|"~") factor | power
?power: molecule ["**" factor]
// _trailer: "(" [arglist] ")" | "[" subscriptlist "]" | "." NAME
?molecule: molecule "(" [arglist] ")" -> func_call
         | molecule "[" [subscriptlist] "]" -> getitem
         | molecule "." NAME -> getattr
         | atom
?atom: "(" [yield_expr|testlist_comp] ")" -> tuple
    |   "[" [listmaker] "]"
    |   "{" [dictorsetmaker] "}"
    |   "`" testlist1 "`"
    |   "(" test ")"
    |   NAME | number | string+
listmaker: test ( list_for | ("," test)* [","] )
?testlist_comp: test ( comp_for | ("," test)+ [","] | ",")
lambdef: "lambda" [paramlist] ":" test
?subscriptlist: subscript ("," subscript)* [","]
subscript: "." "." "." | test | [test] ":" [test] [sliceop]
sliceop: ":" [test]
?exprlist: expr ("," expr)* [","]
?testlist: test ("," test)* [","]
dictorsetmaker: ( (test ":" test (comp_for | ("," test ":" test)* [","])) | (test (comp_for | ("," test)* [","])) )

classdef: "class" NAME ["(" [testlist] ")"] ":" suite

arglist: (argument ",")* (argument [","]
                         | star_args ["," kw_args]
                         | kw_args)

star_args: "*" test
kw_args: "**" test


// The reason that keywords are test nodes instead of NAME is that using NAME
// results in an ambiguity. ast.c makes sure it's a NAME.
argument: test [comp_for] | test "=" test

list_iter: list_for | list_if
list_for: "for" exprlist "in" testlist_safe [list_iter]
list_if: "if" old_test [list_iter]

comp_iter: comp_for | comp_if
comp_for: "for" exprlist "in" or_test [comp_iter]
comp_if: "if" old_test [comp_iter]

testlist1: test ("," test)*

yield_expr: "yield" [testlist]

number: DEC_NUMBER | HEX_NUMBER | OCT_NUMBER | FLOAT | IMAG_NUMBER
string: STRING | LONG_STRING
// Tokens

COMMENT: /#[^\n]*/
_NEWLINE: ( /\r?\n[\t ]*/ | COMMENT )+

STRING : /[ubf]?r?("(?!"").*?(?<!\\)(\\\\)*?"|'(?!'').*?(?<!\\)(\\\\)*?')/i
LONG_STRING.2: /[ubf]?r?(""".*?(?<!\\)(\\\\)*?"""|'''.*?(?<!\\)(\\\\)*?''')/is

DEC_NUMBER: /[1-9]\d*l?/i
HEX_NUMBER: /0x[\da-f]*l?/i
OCT_NUMBER: /0o?[0-7]*l?/i
%import common.FLOAT -> FLOAT
%import common.INT -> _INT
%import common.CNAME -> NAME
IMAG_NUMBER: (_INT | FLOAT) ("j"|"J")


%ignore /[\t \f]+/  // WS
%ignore /\\[\t \f]*\r?\n/   // LINE_CONT
%ignore COMMENT
%declare _INDENT _DEDENT
```

### Python3

```lark
// Python 3 grammar for Lark

// NOTE: Work in progress!!! (XXX TODO)
// This grammar should parse all python 3.x code successfully,
// but the resulting parse-tree is still not well-organized.

// Adapted from: https://docs.python.org/3/reference/grammar.html
// Adapted by: Erez Shinan

// Start symbols for the grammar:
//       single_input is a single interactive statement;
//       file_input is a module or sequence of commands read from an input file;
//       eval_input is the input for the eval() functions.
// NB: compound_stmt in single_input is followed by extra NEWLINE!
single_input: _NEWLINE | simple_stmt | compound_stmt _NEWLINE
file_input: (_NEWLINE | stmt)*
eval_input: testlist _NEWLINE*

decorator: "@" dotted_name [ "(" [arguments] ")" ] _NEWLINE
decorators: decorator+
decorated: decorators (classdef | funcdef | async_funcdef)

async_funcdef: "async" funcdef
funcdef: "def" NAME "(" parameters? ")" ["->" test] ":" suite

parameters: paramvalue ("," paramvalue)* ["," [ starparams | kwparams]]
          | starparams
          | kwparams
starparams: "*" typedparam? ("," paramvalue)* ["," kwparams]
kwparams: "**" typedparam

?paramvalue: typedparam ["=" test]
?typedparam: NAME [":" test]

varargslist: (vfpdef ["=" test] ("," vfpdef ["=" test])* ["," [ "*" [vfpdef] ("," vfpdef ["=" test])* ["," ["**" vfpdef [","]]] | "**" vfpdef [","]]]
  | "*" [vfpdef] ("," vfpdef ["=" test])* ["," ["**" vfpdef [","]]]
  | "**" vfpdef [","])

vfpdef: NAME

?stmt: simple_stmt | compound_stmt
?simple_stmt: small_stmt (";" small_stmt)* [";"] _NEWLINE
?small_stmt: (expr_stmt | del_stmt | pass_stmt | flow_stmt | import_stmt | global_stmt | nonlocal_stmt | assert_stmt)
?expr_stmt: testlist_star_expr (annassign | augassign (yield_expr|testlist)
         | ("=" (yield_expr|testlist_star_expr))*)
annassign: ":" test ["=" test]
?testlist_star_expr: (test|star_expr) ("," (test|star_expr))* [","]
!augassign: ("+=" | "-=" | "*=" | "@=" | "/=" | "%=" | "&=" | "|=" | "^=" | "<<=" | ">>=" | "**=" | "//=")
// For normal and annotated assignments, additional restrictions enforced by the interpreter
del_stmt: "del" exprlist
pass_stmt: "pass"
?flow_stmt: break_stmt | continue_stmt | return_stmt | raise_stmt | yield_stmt
break_stmt: "break"
continue_stmt: "continue"
return_stmt: "return" [testlist]
yield_stmt: yield_expr
raise_stmt: "raise" [test ["from" test]]
import_stmt: import_name | import_from
import_name: "import" dotted_as_names
// note below: the ("." | "...") is necessary because "..." is tokenized as ELLIPSIS
import_from: "from" (dots? dotted_name | dots) "import" ("*" | "(" import_as_names ")" | import_as_names)
!dots: "."+
import_as_name: NAME ["as" NAME]
dotted_as_name: dotted_name ["as" NAME]
import_as_names: import_as_name ("," import_as_name)* [","]
dotted_as_names: dotted_as_name ("," dotted_as_name)*
dotted_name: NAME ("." NAME)*
global_stmt: "global" NAME ("," NAME)*
nonlocal_stmt: "nonlocal" NAME ("," NAME)*
assert_stmt: "assert" test ["," test]

compound_stmt: if_stmt | while_stmt | for_stmt | try_stmt | with_stmt | funcdef | classdef | decorated | async_stmt
async_stmt: "async" (funcdef | with_stmt | for_stmt)
if_stmt: "if" test ":" suite ("elif" test ":" suite)* ["else" ":" suite]
while_stmt: "while" test ":" suite ["else" ":" suite]
for_stmt: "for" exprlist "in" testlist ":" suite ["else" ":" suite]
try_stmt: ("try" ":" suite ((except_clause ":" suite)+ ["else" ":" suite] ["finally" ":" suite] | "finally" ":" suite))
with_stmt: "with" with_item ("," with_item)*  ":" suite
with_item: test ["as" expr]
// NB compile.c makes sure that the default except clause is last
except_clause: "except" [test ["as" NAME]]
suite: simple_stmt | _NEWLINE _INDENT stmt+ _DEDENT

?test: or_test ("if" or_test "else" test)? | lambdef
?test_nocond: or_test | lambdef_nocond
lambdef: "lambda" [varargslist] ":" test
lambdef_nocond: "lambda" [varargslist] ":" test_nocond
?or_test: and_test ("or" and_test)*
?and_test: not_test ("and" not_test)*
?not_test: "not" not_test -> not
         | comparison
?comparison: expr (_comp_op expr)*
star_expr: "*" expr
?expr: xor_expr ("|" xor_expr)*
?xor_expr: and_expr ("^" and_expr)*
?and_expr: shift_expr ("&" shift_expr)*
?shift_expr: arith_expr (_shift_op arith_expr)*
?arith_expr: term (_add_op term)*
?term: factor (_mul_op factor)*
?factor: _factor_op factor | power

!_factor_op: "+"|"-"|"~"
!_add_op: "+"|"-"
!_shift_op: "<<"|">>"
!_mul_op: "*"|"@"|"/"|"%"|"//"
// <> isn't actually a valid comparison operator in Python. It's here for the
// sake of a __future__ import described in PEP 401 (which really works :-)
!_comp_op: "<"|">"|"=="|">="|"<="|"<>"|"!="|"in"|"not" "in"|"is"|"is" "not"

?power: await_expr ("**" factor)?
?await_expr: AWAIT? atom_expr
AWAIT: "await"

?atom_expr: atom_expr "(" [arguments] ")"      -> funccall
          | atom_expr "[" subscriptlist "]"  -> getitem
          | atom_expr "." NAME               -> getattr
          | atom

?atom: "(" [yield_expr|testlist_comp] ")" -> tuple
     | "[" [testlist_comp] "]"  -> list
     | "{" [dictorsetmaker] "}" -> dict
     | NAME -> var
     | number | string+
     | "(" test ")"
     | "..." -> ellipsis
     | "None"    -> const_none
     | "True"    -> const_true
     | "False"   -> const_false

?testlist_comp: (test|star_expr) [comp_for | ("," (test|star_expr))+ [","] | ","]
subscriptlist: subscript ("," subscript)* [","]
subscript: test | [test] ":" [test] [sliceop]
sliceop: ":" [test]
exprlist: (expr|star_expr) ("," (expr|star_expr))* [","]
testlist: test ("," test)* [","]
dictorsetmaker: ( ((test ":" test | "**" expr) (comp_for | ("," (test ":" test | "**" expr))* [","])) | ((test | star_expr) (comp_for | ("," (test | star_expr))* [","])) )

classdef: "class" NAME ["(" [arguments] ")"] ":" suite

arguments: argvalue ("," argvalue)*  ("," [ starargs | kwargs])?
         | starargs
         | kwargs
         | test comp_for

starargs: "*" test ("," "*" test)* ("," argvalue)* ["," kwargs]
kwargs: "**" test

?argvalue: test ("=" test)?



comp_iter: comp_for | comp_if | async_for
async_for: "async" "for" exprlist "in" or_test [comp_iter]
comp_for: "for" exprlist "in" or_test [comp_iter]
comp_if: "if" test_nocond [comp_iter]

// not used in grammar, but may appear in "node" passed from Parser to Compiler
encoding_decl: NAME

yield_expr: "yield" [yield_arg]
yield_arg: "from" test | testlist


number: DEC_NUMBER | HEX_NUMBER | BIN_NUMBER | OCT_NUMBER | FLOAT_NUMBER | IMAG_NUMBER
string: STRING | LONG_STRING
// Tokens

NAME: /[a-zA-Z_]\w*/
COMMENT: /#[^\n]*/
_NEWLINE: ( /\r?\n[\t ]*/ | COMMENT )+


STRING : /[ubf]?r?("(?!"").*?(?<!\\)(\\\\)*?"|'(?!'').*?(?<!\\)(\\\\)*?')/i
LONG_STRING: /[ubf]?r?(""".*?(?<!\\)(\\\\)*?"""|'''.*?(?<!\\)(\\\\)*?''')/is

DEC_NUMBER: /0|[1-9]\d*/i
HEX_NUMBER.2: /0x[\da-f]*/i
OCT_NUMBER.2: /0o[0-7]*/i
BIN_NUMBER.2 : /0b[0-1]*/i
FLOAT_NUMBER.2: /((\d+\.\d*|\.\d+)(e[-+]?\d+)?|\d+(e[-+]?\d+))/i
IMAG_NUMBER.2: /\d+j/i | FLOAT_NUMBER "j"i

%ignore /[\t \f]+/  // WS
%ignore /\\[\t \f]*\r?\n/   // LINE_CONT
%ignore COMMENT
%declare _INDENT _DEDENT
```



 