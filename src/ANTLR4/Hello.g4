// Define a grammar called Hello 定义一个语法名字：Hello
grammar Hello;
r  : 'hello' ID ;         // match keyword hello followed by an identifier 匹配关键字hello，后跟标识符
ID : [a-z]+ ;             // match lower-case identifiers 匹配全是小写字母的标识符
WS : [ \t\r\n]+ -> skip ; // skip spaces, tabs, newlines  跳过空格，制表符，换行符
