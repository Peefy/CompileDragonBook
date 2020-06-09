
# lark

A modern parsing library for Python, implementing Earley & LALR(1) and an easy interface.用于Python的现代解析库，实现Earley和LALR（1）和简单的接口

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
