
# LLVM

LLVM（Low Level Virtual Machine）是构架编译器(compiler)的框架系统，以C++编写而成，用于优化以任意程序语言编写的程序的编译时间(compile-time)、链接时间(link-time)、运行时间(run-time)以及空闲时间(idle-time)，对开发者保持开放，并兼容已有脚本。

LLVM的主要作用是它可以作为多种语言的后端，它可以提供可编程语言无关的优化和针对很多种CPU的代码生成功能。此外llvm目前已经不仅仅是个编程框架，它目前还包含了很多的子项目，比如最具盛名的clang.

LLVM项目包含多个组件。该项目的核心本身称为“LLVM”。它包含处理中间表示并将其转换为目标文件所需的所有工具，库和头文件。工具包括汇编程序，反汇编程序，位代码分析器和位代码优化器。它还包含基本的回归测试。

类似C的语言使用Clang前端。该组件将C，C ++，Objective C和Objective C ++代码编译为LLVM位代码，并使用LLVM从那里编译为目标文件。

## LLVM 入门

* **SRC_ROOT**-这是LLVM源树的顶级目录。
* **OBJ_ROOT**-这是LLVM对象树的顶层目录（即，将放置目标文件和编译的程序的树。它可以与SRC_ROOT相同）。

LLVM可以使用SVN，Git完成版本控制，以及make，cmake等自动构建。

### LLVM 目录布局

* `llvm/examples`-使用LLVM IR和JIT的简单示例。
* `llvm/include`-从LLVM库导出的公共头文件。三个主要子目录：

1. llvm/include/llvm 所有特定LLVM的头文件和子目录LLVM的不同部分：Analysis，CodeGen，Target，Transforms，等...
2. llvm/include/llvm/Support LLVM附带的通用支持库，但不一定特定于LLVM。例如，某些C ++ STL实用程序和命令行选项处理库在此处存储头文件。
3. llvm/include/llvm/Config 由配置的头文件cmake。它们包装“标准” UNIX和C头文件。源代码可以包括这些头文件，这些头文件会自动处理cmake 生成的条件#include 。
   
* `llvm/lib`-大多数源文件在这里。通过将代码放入库中，LLVM使得在工具之间共享代码变得容易。

1. llvm/lib/IR/ 实现诸如Instruction和BasicBlock之类的核心类的核心LLVM源文件。
2. llvm/lib/AsmParser/ LLVM汇编语言解析器库的源代码。
3. llvm/lib/Bitcode/ 用于读取和写入位码的代码。
4. llvm/lib/Analysis/ 各种程序分析，例如调用图，归纳变量，自然循环标识等。
5. llvm/lib/Transforms/ IR到IR程序的转换，例如积极的死代码消除，稀疏的条件常数传播，内联，循环不变代码运动，死全局消除等。
6. llvm/lib/Target/ 描述用于代码生成的目标体系结构的文件。例如， llvm/lib/Target/X86保存X86机器描述。
7. llvm/lib/CodeGen/ 代码生成器的主要部分：指令选择器，指令调度和寄存器分配。
8. llvm/lib/MC/ （FIXME：待定）....？
9. llvm/lib/ExecutionEngine/ 在解释的和JIT编译的场景中，用于在运行时直接执行位代码的库。
10. llvm/lib/Support/ 源代码，对应于头文件中llvm/include/ADT/ 和llvm/include/Support/。
* `llvm/projects`-项目并非严格属于LLVM，而是与LLVM一起提供。这也是用于创建自己的基于LLVM的项目的目录，该项目利用LLVM构建系统。
* `llvm/test`-LLVM基础架构上的功能和回归测试以及其他完整性检查。它们旨在快速运行并覆盖很多领域，而并非详尽无遗。
* `test-suite`-LLVM的全面正确性，性能和基准测试套件。这是一个，因为它在各种许可下都包含大量的第三方代码。
* `llvm/tools`-在上述库的基础上构建的可执行文件，它们构成用户界面的主要部分。始终可以通过键入来获得有关工具的帮助。
* `llvm/utils`-用于处理LLVM源代码的实用程序；有些是构建过程的一部分，因为它们是基础结构各部分的代码生成器。

## 使用LLVM完成一个语言的前端

### 词法分析器

比如对于简单的BASIC语言：

```basic
# Compute the x'th fibonacci number.
def fib(x)
  if x < 3 then
    1
  else
    fib(x-1)+fib(x-2)

# This expression will compute the 40th number.
fib(40)
```

在实现语言方面，首先需要的是处理文本文件并识别其内容的能力。传统方法是使用“词法分析器”（又称“扫描器”）将输入分解为“token”。词法分析器返回的每个令牌都包含令牌代码和潜在的一些元数据（例如数字的数值）。

```ocaml
(* The lexer returns these 'Kwd' if it is an unknown character, otherwise one of
 * these others for known things. *)
type token =
  (* commands *)
  | Def | Extern

  (* primary *)
  | Ident of string | Number of float

  (* unknown *)
  | Kwd of char
```

词法分析器返回的每个词法符号都是词法变量值之一。诸如'+'之类的未知字符将作为返回 。如果当前令牌是标识符，则值为字符串。如果当前标记是数字文字（如1.0），则值为Token.Kwd '+' Token.Ident sToken.Number 1.0

词法分析器的实际实现是由名为的函数驱动的函数的集合Lexer.lex。Lexer.lex调用该函数以从标准输入返回下一个标记

```ocaml
(*===----------------------------------------------------------------------===
 * Lexer
 *===----------------------------------------------------------------------===*)

let rec lex = parser
  (* Skip any whitespace. *)
  | [< ' (' ' | '\n' | '\r' | '\t'); stream >] -> lex stream
```

Lexer.lex通过递归从标准输入读取字符来工作。它会在识别出它们后存储在一个变量中。它要做的第一件事是忽略词法符号之间的空格。这是通过上面的`char Stream.tToken.token`递归调用完成的。

接下来Lexer.lex要做的是识别标识符和特定的关键字，例如“def”

```ocaml
  (* identifier: [a-zA-Z][a-zA-Z0-9] *)
  | [< ' ('A' .. 'Z' | 'a' .. 'z' as c); stream >] ->
      let buffer = Buffer.create 1 in
      Buffer.add_char buffer c;
      lex_ident buffer stream

...

and lex_ident buffer = parser
  | [< ' ('A' .. 'Z' | 'a' .. 'z' | '0' .. '9' as c); stream >] ->
      Buffer.add_char buffer c;
      lex_ident buffer stream
  | [< stream=lex >] ->
      match Buffer.contents buffer with
      | "def" -> [< 'Token.Def; stream >]
      | "extern" -> [< 'Token.Extern; stream >]
      | id -> [< 'Token.Ident id; stream >]
```

识别数字:

```ocaml
  (* number: [0-9.]+ *)
  | [< ' ('0' .. '9' as c); stream >] ->
      let buffer = Buffer.create 1 in
      Buffer.add_char buffer c;
      lex_number buffer stream

...

and lex_number buffer = parser
  | [< ' ('0' .. '9' | '.' as c); stream >] ->
      Buffer.add_char buffer c;
      lex_number buffer stream
  | [< stream=lex >] ->
      [< 'Token.Number (float_of_string (Buffer.contents buffer)); stream >]
```

这是用于处理输入的非常简单的代码。从输入读取数值时，使用ocaml float_of_string 函数将其转换为存储在中的数值 Token.Number。请注意，这没有进行足够的错误检查：Failure如果字符串“ 1.23.45.67” ，它将引发错误。随意扩展它:)。接下来处理注释：

```ocaml
  (* Comment until end of line. *)
  | [< ' ('#'); stream >] ->
      lex_comment stream

...

and lex_comment = parser
  | [< ' ('\n'); stream=lex >] -> stream
  | [< 'c; e=lex_comment >] -> e
  | [< >] -> [< >]
```

通过跳到行尾来处理注释，然后返回下一个标记。最后，如果输入与以上情况之一不匹配，则该输入可能是运算符，例如“+”，或者是文件结尾。这些使用以下代码处理：

```ocaml
(* Otherwise, just return the character as its ascii value. *)
| [< 'c; stream >] ->
    [< 'Token.Kwd c; lex stream >]

(* end of stream. *)
| [< >] -> [< >]
```

### 抽象语法树AST
