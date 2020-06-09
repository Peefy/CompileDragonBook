from lark import Lark

LARK_NAME = './src/lark/array.lark'

with open(LARK_NAME) as fs:
   l = Lark(fs)
   print( l.parse("{1, 2, {1, 2, 3}}") )


