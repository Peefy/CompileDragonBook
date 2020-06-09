from lark import Lark

LARK_NAME = './src/lark/hello.lark'

with open(LARK_NAME) as fs:
   l = Lark(fs)
   print( l.parse("Hello, World!") )


