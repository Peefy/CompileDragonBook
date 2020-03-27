
## CPython的核心部分

* 基本对象
1. dict
2. long/int
3. unicode/str
4. list(timsort)
5. tuple
6. bytes
7. bytearray(buffer protocol)
8. float
9. func(user-defined method)
10. method(builtin method)
11. iter
12. gen(generator/coroutine/async generator)
13. class(bound method/classmethod/staticmethod)
14. complex
15. enum
16. type(mro/metaclass/类/实例的创建过程)
* 模块
1. io
* 库
1. re
* 解释器 (interpreter)
1. gil(全局解释器锁)
2. gc(垃圾回收机制)
3. memory management(内存管理机制)
4. descr(访问(类/实例)属性时发生了什么/__get__/__getattribute__/__getattr__)
5. exception(异常处理机制)
6. module(import实现机制)
7. frame
8. code
9. slots/__slots__(属性在类/实例创建时是如何初始化的)
10. thread(线程)
11. PyObject(基础篇/概述)
* 扩展
* 语法
1. token
2. paser
3. sym table
4. ast

CPython本身无法支持JIT编译(just-in-time compilation)使得纯python的执行速度不如Java和Javascript等语言。

## CPython peefy note

* **Doc**-一些RST文档，RST与Python类似Javadoc与Java，如果下载了Python源码，里面有rst文件夹，可以转为html后用浏览器打开，具体为：安装python的sphinx模块：pip install sphinx
* **Grammer**-定义了语法Grammer和记号token的文件
* **Include**-所有c语言文件的头文件以及API接口
* **Lib**-Python所有的官方库
* **Mac**-Python on macOS
* **Misc**-Python 杂项
* **Modules**-Python的库和组件
* **Objects**-Python内部对象的实现
* **PC**-用于PC发行版的子目录
* **PCbuild**-用于PC Windows平台和MSVC的构建和编译
* **Parser**-解析器
* **Programs**-二进制可执行文件的源文件
* **Python**-主Python共享库的其他源文件
* **Tools**-用于构建Python的一些工具

### CPython源代码布局

对于Python模块，典型的布局为：

* `Lib/<module>.py`
* `Modules/_<module>.c` （如果还有C加速器模块）
* `Lib/test/test_<module>.py`
* `Doc/library/<module>.rst`

对于仅扩展模块，典型布局为：

* `Modules/<module>module.c`
* `Lib/test/test_<module>.py`
* `Doc/library/<module>.rst`

对于内置类型，典型的布局为：

* `Objects/<builtin>object.c`
* `Lib/test/test_<builtin>.py`
* `Doc/library/stdtypes.rst`

对于内置函数，典型布局为：

* `Python/bltinmodule.c`
* `Lib/test/test_builtin.py`
* `Doc/library/functions.rst`

一些例外的类型：

* 内置类型`int`位于`Objects/longobject.c`
* 内置类型`str`位于`Objects/unicodeobject.c`
* 内置模块`sys`位于`Python/sysmodule.c`
* 内置模块`marshal`位于`Python/marshal.c`
* 仅Windows模块`winreg`位于`PC/winreg.c`

## 一些CPython源码教程

[You Guide to the CPython Source Code](https://realpython.com/cpython-source-code-guide/)

[Official CPython Note](https://devguide.python.org/exploring/)

## CPython如何通过反射机制来扩展语言


