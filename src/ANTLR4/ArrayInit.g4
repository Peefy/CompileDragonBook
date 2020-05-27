grammar ArrayInit;

/* 一条名为init的规则，它匹配一对花括号中的、逗号分隔的value */
init : '{' value (',' value)*  '}';
/* 一个value可以是嵌套的花括号结构，也可以是一个简单的整数，即INT词法符号 */
value : init
      | INT
      ;
INT:  [0-9]+;              // 定义词法符号INT，它由一个或多个数字组成
WS:   [\t\r\n]+ -> skip;   // 定义词法规则“空白符号”，丢弃之