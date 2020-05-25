grammar E;

program : statement + ; 
statement: (expression | VAR '=' expression) ';' ; 
expression : (multExpr (('+' |'-' ) multExpr)*) | STRING; 
multExpr : atom ('*' atom)*; 
atom : INT | '(' expression ')'; 
VAR : ('a'..'z' |'A'..'Z' )+ ; 
INT : '0'..'9' + ; 
STRING : '"' (('A'..'Z' | 'a'..'z' | ' ') +) '"' ; 
WS : (' ' |'\t' |'\n' |'\r' )+ {skip();} ;
