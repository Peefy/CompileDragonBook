
# 《You Guide to the CPython Source Code》笔记

## 第1部分：CPython简介

python在控制台上键入或从python.org安装Python发行版时，就正在运行`CPython`。`CPython`是由不同开发人员团队维护和编写的众多Python运行时之一。还有一些其他运行时，例如`PyPy`，`Cython`和`Jython`。

CPython的独特之处在于它既包含一个运行时，又包含所有Python运行时使用的共享语言规范。CPython是Python的“官方”或参考实现。

Python语言规范是对Python语言的描述的文档。例如

* assert是一个保留关键字
* []用于索引，切片和创建空列表。
* def关键字用于定义一个函数

Python发行版中包含的内容：

* 在命令行中输入python时(后不带任何*.py参数)，将显示一个交互式提示窗口，在窗口中可以键入python代码并立即显示代码运行结果(类似Matlab的命令行窗口)
* 包含了许许多多丰富的标准库，比如可以从标准库中导入内置模块json
* 可以使用从Internet安装软件包pip
* 可以使用内置unittest库来测试应用程序

### CPython源代码的组成

CPython源代码分发附带了一系列工具，库和组件。笔记专注于编译器部分。

要下载CPython源代码的副本，可以使用如下git指令。

```sh
$ git clone https://github.com/python/cpython
$ cd cpython
$ git checkout v3.8.0b4
```

新下载的cpython目录中，将找到以下子目录：

```
cpython/
│
├── Doc      ← Source for the documentation
├── Grammar  ← The computer-readable language definition
├── Include  ← The C header files
├── Lib      ← Standard library modules written in Python
├── Mac      ← macOS support files
├── Misc     ← Miscellaneous files
├── Modules  ← Standard Library Modules written in C
├── Objects  ← Core types and the object model
├── Parser   ← The Python parser source code
├── PC       ← Windows build support files
├── PCbuild  ← Windows build support files for older Windows versions
├── Programs ← Source code for the python executable and other binaries
├── Python   ← The CPython interpreter source code
└── Tools    ← Standalone tools useful for building or extending Python
```

### 编译CPython（macOS）

在macOS上编译CPython很简单。您首先需要基本的C编译器工具包。命令行开发工具是一个应用程序，您可以在macOS中通过App Store更新。您需要在终端上执行初始安装。

要在macOS中打开终端，请转到启动板，然后单击“ 其他”，然后选择“ 终端”应用。您将需要将此应用程序保存到Dock中，因此，右键单击Icon并选择Keep in Dock。

现在，在终端中，通过运行以下命令安装C编译器和工具包：

```sh
$ xcode-select --install
```

还需要OpenSSL的工作副本，以用于从PyPi.org网站获取软件包。如果以后计划使用此版本安装其他软件包，则需要SSL验证。

在macOS上安装OpenSSL的最简单方法是使用HomeBrew。如果已经安装了HomeBrew，则可以使用以下brew install命令安装CPython的依赖项：

```sh
$ brew install openssl xz zlib
```

构建将花费几分钟并生成一个名为的二进制文件python.exe。每次更改源代码时，都需要make使用相同的标志重新运行。该python.exe二进制文件是CPython的调试二进制文件。

### 编译CPython（Linux）

[参考](https://github.com/python/cpython)

### 编译CPython（Windows）

[参考](https://github.com/python/cpython)

### 编译器概述

编译器的目的是将一种语言转换为另一种语言。将编译器视为翻译器。

一些编译器将编译为低级机器代码，可以直接在系统上执行。其他编译器将编译为中间语言，由虚拟机执行。

编译器有两种类型：

* 自托管的编译器是用其编译语言编写的编译器，例如Go编译器。
* 源到源编译器是用另一种语言编写的已经具有编译器的编译器。

选择编译器时要做出的一个重要决定是系统可移植性要求。Java和.NET CLR将被编译成一种中间语言，从而使编译后的代码可跨多个系统体系结构移植。C，Go，C ++和Pascal将编译为一个低级可执行文件，该可执行文件仅在与其编译的系统相似的系统上工作。

因为Python应用程序通常作为源代码分发，所以Python运行时的作用是转换Python源代码并一步执行它。在内部，CPython运行时会编译您的代码。一个普遍的误解是Python是一种解释语言。它实际上是编译的。

Python代码未编译为机器代码。它被编译成一种特殊的低级中间语言，称为字节码，只有CPython才能理解。此代码存储在.pyc隐藏目录中的文件中，并缓存以执行。如果您在不更改源代码的情况下两次运行相同的Python应用程序，那么第二次运行总是会更快。这是因为它会加载已编译的字节码并直接执行。

CPython中的C是对C编程语言的引用，这意味着此Python发行版是用C语言编写的。CPython中的编译器是用纯C编写的。但是，许多标准库模块是用纯Python编写的，或者是C和Python的组合。

如果要从头开始编写新的编程语言，则需要一个可执行的应用程序来编译您的编译器！您需要一个编译器来执行任何事情，因此，在开发新语言时，通常首先会使用较老的，更成熟的语言编写它们。

一个很好的例子是Go编程语言。第一个Go编译器是用C编写的，然后可以对Go进行编译，然后用Go重写了该编译器。

CPython保留了C的传统：许多标准库模块（例如ssl模块或sockets模块）都是用C编写的，用于访问底层操作系统API。Windows和Linux内核中用于创建网络套接字，使用文件系统或与显示交互的API 都是用C编写的。Python的可扩展性层专注于C语言是有意义的。

有一个用Python编写的Python编译器叫做PyPy。PyPy的徽标是Ouroboros，代表了编译器的自托管性质。

Python交叉编译器的另一个示例是Jython。Jython用Java编写，并从Python源代码编译为Java字节码。与CPython可以轻松导入C库并从Python使用它们一样，Jython可以轻松导入和引用Java模块和类。

### Python语言规范

CPython源代码中包含Python语言的定义。这是所有Python解释器使用的参考规范。

该规范具有人类可读和机器可读的格式。文档中详细介绍了Python语言，允许的内容以及每个语句的行为方式。

位于里面Doc/reference的目录是reStructuredText的每一个用Python语言特征的解释。这形成了docs.python.org上的Python官方参考指南。

```
cpython/Doc/reference
|
├── compound_stmts.rst
├── datamodel.rst
├── executionmodel.rst
├── expressions.rst
├── grammar.rst
├── import.rst
├── index.rst
├── introduction.rst
├── lexical_analysis.rst
├── simple_stmts.rst
└── toplevel_components.rst
```

在compound_stmts.rst复合语句的文档内部，可以看到定义`with`语句的简单示例。

该with语句可以在Python中以多种方式使用，最简单的方法是实例化上下文管理器和嵌套的代码块：

```py
with x():
   ...
```

可以使用as关键字将结果分配给变量：

```py
with x() as y:
   ...
```

还可以使用逗号将上下文管理器链接在一起：

```py
with x() as y, z() as jk:
   ...
```

### Python 语法

该文档包含该语言的人类可读规范，并且机器可读规范包含在一个文件中Grammar/Grammar。

语法文件以称为**Backus-Naur Form（BNF）**的上下文标记编写。
BNF并非特定于Python，在许多其他语言中通常用作语法的表示法。

Python的语法文件使用具有正则表达式语法的**Extended-BNF（EBNF）规范**。因此，可以在语法文件中使用：

* `*` 重复
* `+` 至少一次重复
* `[]` 用于可选
* `|` 替代
* `()` 用于分组

比如with语句的定义：

```
with_stmt: 'with' with_item (',' with_item)*  ':' suite
with_item: test ['as' expr]
```

引号中的任何内容都是字符串文字，这是定义关键字的方式。因此with_stmt指定为：

* 以单词开头 with
* 后跟一个with_item，它是test和（可选），单词as和一个表达式
* 跟随一个或多个项目，每个项目之间用逗号分隔
* 以a结尾 :
* 其次是 suite

在这两行中引用了一些其他定义：

* **suite** 指的是具有一个或多个语句的代码块
* **test** 指被评估的简单陈述
* **expr** 指一个简单的表达

如果要详细研究这些内容，则整个Python语法都在此文件中定义。

#### 使用 pgen

语法文件本身从未被Python编译器使用。而是pgen使用由工具创建的解析器表。pgen读取语法文件并将其转换为解析器表。如果更改了语法文件，则必须重新生成解析器表并重新编译Python。

*注意：该pgen应用程序已在Python 3.8中从C重写为纯Python。*

要查看pgen实际效果，可以更改Python语法的一部分。在第51行附近，将看到一个pass语句的定义：

```
pass_stmt: 'pass'
```

更改该行以接受关键字'pass'或'proceed'作为关键字：

```
pass_stmt: 'pass' | 'proceed'
```

现在需要重建语法文件。在macOS和Linux上，运行make regen-grammar以运行pgen更改后的语法文件。

应该看到类似于以下的输出，显示新文件Include/graminit.h和Python/graminit.c文件已生成：

```
# using Tools/scripts/generate_token.py
...
python3 ./Tools/scripts/update_file.py ./Include/graminit.h ./Include/graminit.h.new
python3 ./Tools/scripts/update_file.py ./Python/graminit.c ./Python/graminit.c.new
```

*注意： pgen通过将EBNF语句转换为非确定性有限自动机（NFA）进行工作，然后将其转换为确定性有限自动机（DFA）。解析器将DFA用作CPython特有的特殊方式来解析表。*

使用重新生成的解析器表，需要重新编译CPython才能查看新语法。

如果代码编译成功，则可以执行新的CPython二进制文件并启动REPL。

在REPL中，现在可以尝试定义一个函数，而不是使用该pass语句，而应使用proceed编译成Python语法的关键字Alternative：

```sh
Python 3.8.0b4 (tags/v3.8.0b4:d93605de72, Aug 30 2019, 10:00:03) 
[Clang 10.0.1 (clang-1001.0.46.4)] on darwin
Type "help", "copyright", "credits" or "license" for more information.
>>> def example():
...    proceed
... 
>>> example()
```

#### Python Tokens 词素

文件Grammar夹中的语法文件旁边是一个Tokens文件，其中包含在解析树中作为叶节点发现的每个唯一类型。

*注意：该Tokens文件是Python 3.8的新功能。*

部分Tokens如下

```
LPAR                    '('
RPAR                    ')'
LSQB                    '['
RSQB                    ']'
COLON                   ':'
COMMA                   ','
SEMI                    ';'
```

与Grammar文件一样，如果更改Tokens文件，则需要pgen再次运行。

将按行和字符查看词素列表。使用该-e标志输出确切的词素名称：

```sh
$ ./python.exe -m tokenize -e test_tokens.py

0,0-0,0:            ENCODING       'utf-8'        
1,0-1,14:           COMMENT        '# Hello world!'
1,14-1,15:          NL             '\n'           
2,0-2,3:            NAME           'def'          
2,4-2,15:           NAME           'my_function'  
2,15-2,16:          LPAR           '('            
2,16-2,17:          RPAR           ')'            
2,17-2,18:          COLON          ':'            
2,18-2,19:          NEWLINE        '\n'           
3,0-3,3:            INDENT         '   '          
3,3-3,7:            NAME           'proceed'         
3,7-3,8:            NEWLINE        '\n'           
4,0-4,0:            DEDENT         ''             
4,0-4,0:            ENDMARKER      ''    
```

在输出中，tokenize模块隐含了一些不在文件中的令牌。的ENCODING令牌utf-8，末尾为空行，DEDENT用于关闭函数声明，并提供ENDMARKER来结束文件。最佳做法是在Python源文件的末尾添加一个空行。

该tokenize模块是用纯Python编写的，位于Lib/tokenize.pyCPython源代码中。

CPython源代码中有两个标记器：一个是用Python编写的，在此进行了演示，另一个是用C编写的。用Python编写的标记器是一个实用程序，一个用C编写的标记器供Python编译器使用。它们具有相同的输出和行为。用C语言编写的版本旨在提高性能，而使用Python编写的模块则用于调试。

要查看C标记器的详细读数，可以使用带-d标志的Python运行。使用test_tokens.py先前创建的脚本，使用以下命令运行它：

```sh
$ ./python.exe -d test_tokens.py

Token NAME/'def' ... It's a keyword
 DFA 'file_input', state 0: Push 'stmt'
 DFA 'stmt', state 0: Push 'compound_stmt'
 DFA 'compound_stmt', state 0: Push 'funcdef'
 DFA 'funcdef', state 0: Shift.
Token NAME/'my_function' ... It's a token we know
 DFA 'funcdef', state 1: Shift.
Token LPAR/'(' ... It's a token we know
 DFA 'funcdef', state 2: Push 'parameters'
 DFA 'parameters', state 0: Shift.
Token RPAR/')' ... It's a token we know
 DFA 'parameters', state 1: Shift.
  DFA 'parameters', state 2: Direct pop.
Token COLON/':' ... It's a token we know
 DFA 'funcdef', state 3: Shift.
Token NEWLINE/'' ... It's a token we know
 DFA 'funcdef', state 5: [switch func_body_suite to suite] Push 'suite'
 DFA 'suite', state 0: Shift.
Token INDENT/'' ... It's a token we know
 DFA 'suite', state 1: Shift.
Token NAME/'proceed' ... It's a keyword
 DFA 'suite', state 3: Push 'stmt'
...
  ACCEPT.
```

在输出中，您可以看到它突出显示proceed为关键字。在下一章中，我们将看到如何执行Python二进制代码到达词素生成器，以及从那里执行代码的过程。

### CPython中的内存管理
