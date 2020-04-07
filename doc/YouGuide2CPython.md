
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


