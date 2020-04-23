
<!-- 龙书第2版第626--643页-->

# 一个完整的编译器前端

[完整代码链接](https://github.com/Peefy/CompileDragonBook.Cpp/blob/master/src/javacomplier/main)

使用Java代码实现，由5个包组成，`main`，`lexer`，`symbol`，`parser`和`inter`。包inter中包含的类处理用抽象语法表示的语言结构。因为语法分析器的代码和其他各个包交互，所以它将在最后描述。

作为语法分析器的输入时，源程序就是一个由词法单元组成的流，因此面向对象特性和语法分析器代码之间没有什么关系。当由语法分析器输出时，源程序就是一棵**抽象语法树(AST)**，树中的结构或结点被实现为对象。这些对象负责构造一棵AST的结点，类型检查，生成三地址中间代码.

## 源语言

这个语言的一个程序由一个块组成，该块中包含可选的声明和语句。语法符号`basic`表示基本类型

```
program -> block
block -> {decls stmts}
decls -> decls decl | e
decl -> type id
type -> type[num] | basic
stmts -> stmts stmt | e
```

把赋值当成是一个语句（而不是表达式中的运算符）可以简化翻译工作。

**面向对象和面向步骤**

在一个面向对象方法中，一个构造的所有代码都集中在这个与构造对应的类中，但是在面向步骤的方法中，这个方法中的代码是按照步骤进行组织的，因此一个类型检查过程中对每个构造都有一个case分支，且一个代码生成过程对每个构造也都有一个case分支，等等。

对这两者进行衡量，可知使用面向对象方法会使得改变或增加一个构造(比如for语句)，变得更容易；而使用面向步骤的方法会使得改变或增加一个步骤(比如类型检查)变得比较容易。使用对象来实现时，增加一个新的构造可以通过写一个自包含的类来实现；但是如果要改变一个步骤，比如插入自动类型转换的代码，就需要改变所有受影响的类。使用面向步骤的方式时，增加一个新的构造可能会引起各个步骤中的多个过程的改变。

```bnf
stmt -> loc = bool;
    | if (bool) stmt
    | if (bool) stmt else stmt
    | while (bool) stmt
    | do stmt while (bool);
    | break;
    | block
loc -> loc[bool] | id;
```

表达式的产生式处理了运算符的结合性和优先级，它们对每个优先级级别都使用了一个非终结符号，而非终结符号factor用来表示括号中的表达式，标识符，数组引用和常量。

```bnf
bool -> bool || join | join
join -> join && equality | equality
equality -> equality == rel | equality != rel | rel
rel -> expr < expr | expr <= expr | expr => expr | 
        expr > expr | expr
expr -> expr + term | expr - term | term
term -> term * unary | term / unary | unary
unary -> !unary | -unary | facfor
factor -> (bool) | loc | num | id | true | false
```

## main package

程序的执行从类Main的方法main开始。方法main创建了一个词法分析器和一个语法分析器，然后调用语法分析器中的方法program

```java
package main;

import java.io.*;

import lexer.*;
import parser.*;

/**
 * Main
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("Hello Java Compiler!");
        Lexer lex = new Lexer();
        try {
            Parser parser = new Parser(lex);
            parser.program();
        } catch (IOException e) {
            System.out.println("error"); 
        }
        System.out.println("finish"); 
    }
    
}
```

## 词法分析器

```java
package lexer;

/**
 * Tag
 * 其中的三个常量INDEX，MINUS，TEMP不是词法单元，将在语法分析中使用
 */
public class Tag {
    /**
     * 
     */
    public static final int 
        AND = 256,     // &&
        BASIC = 257,   // 
        BREAK = 258,   // break
        DO = 259,      // do
        ELSE = 260,    // else
        EQ = 261,      // eq
        FALSE = 262,   // false
        GE = 263,      // <=
        ID = 264,      // id
        IF = 265,      // if
        INDEX = 266,
        LE = 267,      // >=
        MINUS = 268,   // -
        NE = 269,      // !=
        NUM = 270,     // num
        OR = 271,      // ||
        REAL = 272,    // float
        TEMP = 273,    // temp
        TRUE = 274,    // true
        WHILE = 275;   // while
        // >>
        // << 
}
```

其中三个常量INDEX，MINUS和TEMP不是词法单元，它们将在抽象语法树中使用

```java
package lexer;

/**
 * Token
 */
public class Token {
    /**
     * 
     */
    public final int tag;

    /**
     * 
     * @param t
     */
    public Token(int t) {
        tag = t;
    }

    /**
     * 
     */
    @Override
    public String toString() {
        return String.valueOf((char)tag);
    }
    
}
```

类Word用与管理保留字，标识符和像&&这样的复合词法单元的词素。它也可以用来管理在中间代码中运算符的书写形式；比如单目减号。对象Word.True和Word.False在类Word中定义。对应的基本类型int，char，bool和float的对象在类Type中定义。

```java
package lexer;

/**
 * Word 保留字，标识符，符合词法单元词素
 */
public class Word extends Token {
    /**
     * 
     */
    String lexname = "";

    /**
     * 
     */
    public Word(String s, int tag) {
        super(tag);
        lexname = s;
    }

    /**
     * 
     */
    @Override
    public String toString() {
        return lexname;
    }

    /**
     * 
     */
    public static final Word 
        and = new Word("&&", Tag.AND),
        or = new Word("||", Tag.OR),
        eq = new Word("&&", Tag.EQ),
        ne = new Word("&&", Tag.NE),
        le = new Word("&&", Tag.LE),
        ge = new Word("&&", Tag.GE),
        minus = new Word("&&", Tag.MINUS),
        True = new Word("&&", Tag.TRUE),
        Flase = new Word("&&", Tag.FALSE),
        temp = new Word("&&", Tag.TEMP);

}
```

类Real用于处理浮点数

```java
package lexer;

/**
 * Real
 */
public class Real extends Token {
    /**
     * 
     */
    public final float value;

    /**
     * 
     * @param v
     */
    public Real(float v) {
        super(Tag.REAL);
        value = v;
    }

    /**
     * 
     */
    @Override
    public String toString() {
        return Float.toString(value);
    }
    
}
```

## 符号表和类型

包symbols实现了符号表和类型。类Lexer把字符串映射为字，类Env把字符串词法单元映射为类Id的对象。类Id和其他的对应于表达式和语句的类一起都在inter包中定义。

```java
package symbols;

import java.util.*;

import lexer.*;
import inter.*;

/**
 * Env 把字符串词法映射为类Id的对象
 */
public class Env {
    /**
     * 
     */
    private Hashtable<Token, Id> table;

    /**
     * 
     */
    protected Env prev;

    /**
     * 
     * @param n
     */
    public Env(Env n) {
        table = new Hashtable<>();
        prev = n;
    }

    /**
     * 
     * @param w
     * @param i
     */
    public void put(Token w, Id i) {
        table.put(w, i);
    }
    
    /**
     * 
     * @param w
     * @return
     */
    public Id get(Token w) {
        for (Env e = this; e != null ; e = e.prev) {
            Id found = (Id)(table.get(w));
            if (found != null)
                return found;
        }
        return null;
    }

}
```

把类Type定义为类Word的子类，因为像int这样的基本类型名字就是保留字，将被词法分析器从词素映射为适当的对象。对应于基本类型的对象是Type.Int,Type.Float,Type.Char,Type.Bool。这些对象从超类中继承了字段tag，相应的值被设置为Tag.BASIC，因此语法分析器以同样的方式处理它们。

```java
package symbols;

import lexer.*;

/**
 * Type
 */
public class Type extends Word {
    /**
     * 用于存储分配
     */
    public int width = 0;  

    public Type(String s, int tag, int w) {
        super(s, tag);
        width = w;
    }
    
    public static final Type
        Int = new Type("int", Tag.BASIC, 4),
        Float = new Type("float", Tag.BASIC, 8),
        Char = new Type("char", Tag.BASIC, 1),
        Bool = new Type("bool", Tag.BASIC, 1);

    public static boolean isNumeric(Type p) {
        return p == Type.Char || p == Type.Int || p == Type.Float;
    }

    public static Type max(Type p1, Type p2) {
        if (!isNumeric(p1) || isNumeric(p2))
            return null;
        else if (p1 == Type.Float || p2 == Type.Float)
            return Type.Float;
        else if (p1 == Type.Int || p2 == Type.Int)
            return Type.Int;
        return Type.Char;
    }
}
```

函数numeric和max用于类型转换，在两个数字类型之间允许进行类型转换，“数字”的类型包括Char，Int和Float。当一个算术运算符应用于两个数字类型时，结果类型是两个类型的max值。

数组是这个源语言中唯一的构造类型。

```java
package symbols;

import lexer.*;

/**
 * Array
 */
public class Array extends Type {
    /**
     * 
     */
    public Type of;
    /**
     * 
     */
    public int size = 1;

    /**
     * 
     * @param sz
     * @param p
     */
    public Array(int sz, Type p) {
        super("[]", Tag.INDEX, sz * p.width);
        size = sz;
        of = p;
    }
    
    /**
     * 
     */
    @Override
    public String toString() {
        return "[" + size + "]" + of.toString();
    }

}
```

## 表达式的中间代码

包inter包含了Node的类层次结构。Node有两个子类：对应于表达式结点的Expr和对应于语句结点的Stmt。Expr的某些方法处理布尔表达式和跳转代码。

抽象语法树中的结点被实现为类Node的对象。为了报告错误，字段lexline保存了本结点对应的构造在源程序中的行号。

```java
package inter;

import lexer.*;

/**
 * Node
 */
public class Node {
    /**
     * 
     */
    int lexline = 0;

    /**
     * 
     */
    public Node() {
        lexline = Lexer.line;
    }
 
    /**
     * 
     * @param s
     */
    void error(String s) {
        throw new Error("near line" + lexline + ": " + s);
    }

    /**
     * 
     */
    static int labels = 0;

    /**
     * 
     * @return
     */
    public int newlabel() {
        labels += 1;
        return labels;
    }

    /**
     * 
     * @param i
     */
    public void emitlabel(int i) {
        System.out.println("L" + i + ":");
    }

    /**
     * 
     * @param s
     */
    public void emit(String s) {
        System.out.println("\t" + s);
    }

}
```

表达式构造被实现为Expr的子类。类Expr包含字段op和type。

```java
package inter;

import lexer.*;
import symbols.*;

/**
 * Expr 表达式结点
 */
public class Expr extends Node {
    /**
     * 
     */
    public Token op;

    /**
     * 
     */
    public Type type;

    /**
     * 
     * @param tok
     * @param p
     */
    public Expr(Token tok, Type p) {
        op = tok;
        type = p;
    }

    /**
     * 
     * @return
     */
    public Expr gen() {
        return this;
    }

    /**
     * 
     * @return
     */
    public Expr reduce() {
        return this;
    }

    /**
     * 
     * @param t true
     * @param f false
     */
    public void jumping(int t, int f) {
        emitjumps(toString(), t, f);
    }

    /**
     * 
     * @param test
     * @param t true
     * @param f false
     */
    public void emitjumps(String test, int t, int f) {
        if (t != 0 && f != 0) {
            emit("if " + test + " goto L" + t);
            emit("goto L" + f);
        }
        else if (t != 0) {
            emit("if " + test + " goto L" + t);
        }
        else if (f != 0) {
            emit("iffalse " + test + " goto L" + f);
        }
    }

    /**
     * 
     */
    @Override
    public String toString() {
        return op.toString();
    }

}
```

方法gen返回了一个“项”，该项可以成为一个三地址指令的右部。给定一个表达式`E = E1 + E2`,方法gen返回一个项`x1 + x2`，其中x1和x2分别是存放E1和E2值的地址。如果这个对象是一个地址，就可以返回this值。Expr的子类通常会重新实现gen。

方法reduce把一个表达式计算(归约)成为一个单一的地址。也就是说，它返回一个常量，一个标识符，或者一个临时名字。给定一个表达式E，方法reduce返回一个存放R的值的临时变量t。如果这个对象是一个地址，那么this仍然是正确的返回值。方法jumping和emitjumps为布尔表达式生成跳转代码。

因为一个标识符就是一个地址，类Id从类Expr中继承了gen和reduce的默认实现

```java
package inter;

import lexer.*;
import symbols.*;

/**
 * Id
 */
public class Id extends Expr {
    /**
     * 相对地址
     */
    public int offset;

    /**
     * 
     * @param id
     * @param p
     * @param b
     */
    public Id(Word id, Type p, int b) {
        super(id, p);
        offset = b;
    }
}
```

对应于一个标识符的类Id的结点是一个叶子结点。函数调用super(id,p)把id和p分别保存在继承得到的字段op和type中。字段offset保存了这个标识符的相对地址。

类Op提供了reduce的一个实现。这个类的子类包括：表示算术运算符的子类Arith，表示单目运算符的子类Unary和表示数组访问的子类Access。这些子类都继承了这个实现。在每种情况下，reduce调用gen来生成一个项，生成一个指令把这个项赋给一个新的临时名字，并返回这个临时名字。

```java
package inter;

import lexer.*;
import symbols.*;

/**
 * Op
 */
public class Op extends Expr {

    public Op(Token tok, Type p) {
        super(tok, p);
    }
    
    public Expr reduce() {
        Expr x = gen();
        Temp t = new Temp(type);
        emit(t.toString() + " = " + x.toString());
        return t;
    }
    
}
```

类Arith实现了双目运算符，比如`+`和`*`。构造函数Arith首先调用super(tok,null)，其中tok是一个表示该运算符的词法单元，null是类型的占位符。相应的类型使用Type.max来确定，这个函数检查两个运算分量是否可以被类型强制为一个常见的数字类型；

```java
package inter;

import lexer.*;
import symbols.*;

/**
 * Arith
 */
public class Arith extends Op {

    /**
     * 
     */
    public Expr expr1, expr2;

    /**
     * 
     * @param tok
     * @param x1
     * @param x2
     */
    public Arith(Token tok, Expr x1, Expr x2) {
        super(tok, null);
        expr1 = x1;
        expr2 = x2;
        type = Type.max(expr1.type, expr2.type);
        if (type == null) 
            error("type error");
    }

    /**
     * 把表达式的子表达式规约为地址
     */
    public Expr gen() {
        return new Arith(op, expr1.reduce(), expr2.reduce());
    }

    /**
     * 
     */
    @Override
    public String toString() {
        return expr1.toString() + " " + op.toString() + " " + expr2.toString();
    }
    
}
```

方法gen把表达式的子表达式归约为地址，并将表达式的运算符作用于这些地址，从而构造出了一个三地址指令的右部。比如，假设gen在`a + b * c`的根部被调用。其中对reduce的调用返回a作为子表达式a的地址，并返回t作为b * c的地址。同时，reduce还生成指令`t = b * c`。方法gen返回了一个新的Arith结点，其中的运算符是`*`，而运算分量是地址a和t。

和所有其他表达式一样，临时名字也有类型。因此，构造函数Temp被调用时有一个类型参数。

```java
package inter;

import lexer.*;
import symbols.*;

/**
 * Temp
 */
public class Temp extends Expr {
    static int count = 0;
    int number = 0;

    public Temp(Type p) {
        super(Word.temp, p);
        number = ++count;
    }

    @Override
    public String toString() {
        return "t" + number;
    }
    
}
```

单目运算符的子类Unary

```java
package inter;

import lexer.*;
import symbols.*;

/**
 * Unary
 */
public class Unary extends Op {
    public Expr expr;

    /**
     * 处理单目减法，对！的处理见Not；
     * @param tok
     * @param x
     */
    public Unary(Token tok, Expr x) {
        super(tok, null);
        type = Type.max(Type.Int, expr.type);
        if (type == null)
            error("type error");
    }

    public Expr gen() {
        return new Unary(op, expr.reduce());
    }

    @Override
    public String toString() {
        return op.toString() + " " + expr.toString();
    }

}
```

## 布尔表达式的跳转代码

布尔表达式B的跳转代码由方法jumping生成。这个方法的参数是两个标号t和f，它们分别称为表达式B的true出口和false出口。如果B的值为真，代码中就包含一个目标为t的跳转指令；如果B的值为假，就有一个目标为f的指令。特殊标号0表示控制流从B穿越，到达B的代码之后的下一个指令。

从类Constant开始。Constant构造函数的参数是一个词法单元tok和一个类型p。它在抽象语法树中构造出一个标号为tok，类型为p的叶子结点。构造函数Constant被重载，重载后的构造函数可以根据一个整数创建一个常量对象。

```java
package inter;

import lexer.*;
import symbols.*;

/**
 * Constant
 */
public class Constant extends Expr {

    public Constant(Token tok, Type p) {
        super(tok, p);
    }

    public Constant(int i) {
        super(new Num(i), Type.Int);
    }

    public static final Constant
        True = new Constant(Word.True, Type.Bool),
        False = new Constant(Word.Flase, Type.Bool);
    
    public void jumping(int t, int f) {
        if (this == True && t != 0) {
            emit("goto L" + t);
        }
        else if (this == False && f != 0) {
            emit("goto L" + f);
        }
    }
    
}
```

方法jumping有两个参数，标号t和f。如果这个常量对象是静对象True，t不是特殊标号0，那么就会生成一个目标为t的跳转指令。否则，如果这是对象False且f非零，那么就会生成一个目标为f的跳转指令。

类Logical为类Or，And和Not提供了一些常见功能。字段expr1和expr2对应于一个逻辑运算符的运算分量。构造函数Logical(tok,a,b)构造出了一个语法树的结点，其运算符为tok，而运算分量为a和b。在完成这些工作时，它调用函数check来保证a和b都是布尔类型。

```java
package inter;

import lexer.*;
import symbols.*;

/**
 * Logical
 */
public class Logical extends Expr {
    public Expr expr1, expr2;

    public Logical(Token tok, Expr x1, Expr x2) {
        super(tok, null);
        expr1 = x1;
        expr2 = x2;
        type = check(expr1.type, expr2.type);
        if (type == null) 
            error("type error");
    }

    public Type check(Type p1, Type p2) {
        if (p1 == Type.Bool && p2 == Type.Bool)
            return Type.Bool;
        return null;
    }

    @Override
    public Expr gen() {
        int f = newlabel();
        int a = newlabel();
        Temp temp = new Temp(type);
        this.jumping(0, f);
        emit(temp.toString() + " = true");
        emit("goto L" + a);
        emitlabel(f);
        emit(temp.toString() + " = false");
        emitlabel(a);
        return temp;
    }
    
    @Override
    public String toString() {
        return expr1.toString() + " " + op.toString() + " " + expr2.toString();
    }

}
```

在类Or中，方法jumping生成了一个布尔表达式B = B1 || B2的跳转代码。当前假设B的true出口t和false出口f都不是特殊标号0。因为如果B1为真，B必然为真，所以B1的true出口必然是t，而它的false出口对应于B2的第一条指令。B2的true和false出口和B的相应出口相同。

```java
package inter;

import lexer.*;

/**
 * Or
 */
public class Or extends Logical {

    /**
     * 
     * @param tok
     * @param x1
     * @param x2
     */
	public Or(Token tok, Expr x1, Expr x2) {
		super(tok, x1, x2);
    }
    
    /**
     * 
     */
    @Override
    public void jumping(int t, int f) {
        int label = t != 0 ? t : newlabel();
        expr1.jumping(label, 0);
        expr2.jumping(t, f);
        if (t == 0) 
        emitlabel(label);
    }
}
```

在一般情况下，B的true出口t可能是特殊标号0。变量label保证了B1的true出口被正确地设置为B的代码的结尾处。如果t为0，那么label被设置为一个新的标号，并在B1和B2的代码被生成后再生成这个新标号。

类And的代码和Or的代码类似

```java
package inter;

import lexer.*;

/**
 * And
 */
public class And extends Logical {
    /**
     * 
     * @param tok
     * @param x1
     * @param x2
     */
    public And(Token tok, Expr x1, Expr x2) {
        super(tok, x1, x2);
    }

    /**
     * 
     */
    @Override
    public void jumping(int t, int f) {
        int label = f != 0 ? f : newlabel();
        expr1.jumping(0, label);
        expr2.jumping(t, f);
        if (f == 0)
            emitlabel(label);
    }
    
}
```

虽然这个类Not实现的是一个单目运算符，这个类和其他布尔运算符之间仍然具有相当多的共同之处，因此把它作为一个Logical的一个子类。它的超类具有两个运算分量，因此对super的调用中x2出现了两次。

```java
package inter;

import lexer.*;

/**
 * Not
 */
public class Not extends Logical {

    public Not(Token tok, Expr x2) {
        super(tok, x2, x2);
    }

    @Override
    public void jumping(int t, int f) {
        expr2.jumping(f, t);
    }

    @Override
    public String toString() {
        return op.toString() + " " + expr2.toString();
    }
    
}
```

类Rel实现了运算符`<`,`<=`,`==`,`!=`,`>=`,`>`。函数check检查两个运算分量是否具有相同的类型，但是它们不是数组类型。为简单起见，这里不允许强制类型转换。

```java
package inter;

import lexer.*;
import symbols.*;

/**
 * Rel
 */
public class Rel extends Logical {

    public Rel(Token tok, Expr x1, Expr x2) {
        super(tok, x1, x2);
    }

    @Override
    public Type check(Type p1, Type p2) {
        if (p1 instanceof Array || p2 instanceof Array)
            return null;
        else if (p1 == p2)
            return Type.Bool;
        else
            return null;
    }
    
    @Override
    public void jumping(int t, int f) {
        Expr a = expr1.reduce();
        Expr b = expr2.reduce();
        String test = a.toString() + " " + op.toString() + " " + b.toString();
        emitjumps(test, t, f);
    }

}
```

在生成类Access的代码时演示了方法emitjumps的另一种用法。源语言允许把布尔值赋给标识符和数组元素，因此一个布尔表达式可能是一个数组访问。类Access有一个方法gen，用来生成“正常”代码，另一个方法jumping用来生成跳转代码。方法jumping在把这个数组访问归约为一个临时变量后调用emitjumps。这个类的构造函数被调用时的参数为一个平坦化的一个数组a，一个下标i和该数组的元素类型p。在生成数组地址计算代码的过程中完成了类型检查。

```java
package inter;

import lexer.*;
import symbols.*;

/**
 * Access
 */
public class Access extends Op {

    public Id array;
    public Expr index;

    public Access(Id a, Expr i, Type p) {
        super(new Word("[]", Tag.INDEX), p);
        array = a;
        index = i;
    }

    @Override
    public Expr gen() {
        return new Access(array, index.reduce(), type);
    }

    @Override
    public void jumping(int t, int f) {
        emitjumps(reduce().toString(), t, f);
    }

    @Override
    public String toString() {
        return array.toString() + " [" + index.toString() + " ]";
    }
}
```

跳转代码还可以被用来返回一个布尔值。本节中较早描述的类Logical有一个方法gen。这个方法返回一个临时变量temp。这个变量的值由这个表达式的跳转代码中的控制流决定。在这个布尔表达式的true出口，temp被赋予true值；在false出口，temp被赋予false值。这个表达式的跳转代码中的true出口是下一条指令，而false出口是一个新标号f。下一条指令把true值赋给temp，后面紧跟目标为新标号a的跳转指令。

## 语句的中间代码

每个语句构造被实现为Stmt的一个子类。一个构造的组成部分对应的字段是相应子类的对象。例如，类While有一个对应于测试表达式的字段和一个子语句字段。

每个语句构造被实现为一个Stmt的一个子类。一个构造的组成部分对应的字段是相应子类的一个对象。例如，类While有一个对应于测试表达式的字段和一个子语句字段。

```java
package inter;

/**
 * Stmt 语句结点
 */
public class Stmt extends Node {
    public Stmt() { }

    public static Stmt Null = new Stmt();

    /**
     * 调用时的参数是语句开始处的标号和语句的下一条指令的标号
     * @param b
     * @param a
     */
    public void gen(int b, int a) { }
    
    /**
     * 保存语句的下一条指令的标号
     */
    int after = 0;

    /**
     * 用于break语句
     */
    public static Stmt Enclosing = Stmt.Null;
    
}
```

方法gen被调用时的两个参数分别是标号a和b，其中b标记这个语句的代码的开始处，而a标记这个语句的代码之后的第一条指令。方法gen是子类中的gen方法的占位符。子类While和Do把它们的标号a存放在字段after中。当任何内层的break语句要跳出这个外层构造时就可以使用这些标号。对象Stmt.Enclosing在语法分析时被用于跟踪外层构造。(对于包含continue语句的源语言，可以使用同样的方法来跟踪一个continue语句的外层构造)。

类If的构造函数为语句`if(E) S`构造一个结点。字段expr和stmt分别保存了E和S对应的结点。请注意，小写字母组成的expr是一个类Expr的字段的名字。类似地，stmt是类为Stmt的字段的名字.

```java
package inter;

import symbols.*;

/**
 * If
 */
public class If extends Stmt {
    Expr expr;
    Stmt stmt;
    public If(Expr x, Stmt s) {
        expr = x;
        stmt = s;
        if (expr.type != Type.Bool)
            expr.error("boolean required in if");
    }
    
    @Override
    public void gen(int b, int a) {
        int label = newlabel();  // stmt代码的标号
        expr.jumping(0, a);      // 为真时控制流穿越，为假时转向a
        emitlabel(label);
        stmt.gen(label, a);
    }

}
```

一个If对象的代码包含了expr的跳转代码，然后是stmt的代码。调用expr.jumping(0, a)指明如果expr的值为真，控制流必须穿越expr的代码；否则控制流必须转向标号a。

类Else处理条件语句的else部分。它的实现和类If的实现类似：

```java
package inter;

import symbols.*;

/**
 * If
 */
public class Else extends Stmt {
    Expr expr;
    Stmt stmt1;
    Stmt stmt2;
    public Else(Expr x, Stmt s1, Stmt s2) {
        expr = x;
        stmt1 = s1;
        stmt2 = s2;
        if (expr.type != Type.Bool)
            expr.error("boolean required in if");
    }
    
    @Override
    public void gen(int b, int a) {
        int label1 = newlabel();  
        int label2 = newlabel();
        expr.jumping(0, label2);      
        emitlabel(label1);
        stmt1.gen(label1, a);
        emit("goto L" + a);
        emitlabel(label2);
        stmt2.gen(label2, a);
    }

}
```

一个while对象的构造过程分成两个部分：构造函数while()创建了一个子结点为空的结点；初始化函数int(x,s)把子结点expr设置成为x，把子结点stmt设置成为s。函数gen(b,a)用于生成三地址代码。它和类If中的相应函数gen()在本质上有着相通之处。不同之处在于标号a被保存在字段after中，且stmt的代码之后紧跟这一个目标为b的跳转指令。这个指令使得while循环进入下一次迭代。

```java
package inter;

import symbols.*;

/**
 * While
 */
public class While extends Stmt {
    Expr expr;
    Stmt stmt;
    public While() {
        expr = null;
        stmt = null;
    }
    
    public void init(Expr x, Stmt s) {
        expr = x;
        stmt = s;
        if (expr.type != Type.Bool) 
            expr.error("boolean required in while");
    }

    @Override
    public void gen(int b, int a) {
        after = a;
        expr.jumping(0, a);
        int label = newlabel();
        emitlabel(label);
        stmt.gen(label, b);
        emit("goto L" + b);
    }

}
```

类Do和类While非常相似

```java
package inter;

import symbols.*;

/**
 * Do
 */
public class Do extends Stmt {
    Expr expr;
    Stmt stmt;

    public Do() {
        
    }

    public Do(Expr x, Stmt s) {
        expr = x;
        stmt = s;
        if (expr.type != Type.Bool)
            expr.error("boolean required in do");
    }

    @Override
    public void gen(int b, int a) {
        after = a;
        int label = newlabel();
        stmt.gen(b, label);
        emitlabel(label);
        expr.jumping(b, 0);
    }
    
}
```

类Set实现了左部为标识符且右部为一个表达式的赋值语句。在类Set中的大部分代码的目的是构造一个结点并进行类型检查。函数gen生成一个三地址指令。

```java
package inter;

import symbols.*;

/**
 * Set
 */
public class Set extends Stmt {
    public Id id;
    public Expr expr;

    public Set(Id i, Expr x) {
        id = i;
        expr = x;
        if ( check(id.type, expr.type) == null )
            error("type error");
    }

    public Type check(Type p1, Type p2) {
        if (Type.isNumeric(p1) && Type.isNumeric(p2))
            return p2;
        else if (p1 == Type.Bool && p2 == Type.Bool ) 
            return p2;
        else 
            return null;
    }

    public void gen(int b, int a) {
        emit(id.toString() + " = " + expr.gen().toString());
    }
    
}
```

类SetElem实现了对数组元素的赋值.

```java
package inter;

import symbols.*;

/**
 * Set
 */
public class SetElem extends Stmt {
    public Id array;
    public Expr index;
    public Expr expr;

    public SetElem(Access x, Expr y) {
        array = x.array;
        index = x;
        expr = y;
        if ( check(x.type, expr.type) == null )
            error("type error");
    }

    public Type check(Type p1, Type p2) {
        if (p1 instanceof Array || p2 instanceof Array)
            return null;
        else if (p1 == p2) 
            return p2;
        else if (Type.isNumeric(p1) && Type.isNumeric(p2))
            return p2;
        else
            return null;
    }

    public void gen(int b, int a) {
        String s1 = index.reduce().toString();
        String s2 = expr.reduce().toString();
        emit(array.toString() + " [ " + s1 + " ] " + s2);
    }
    
}
```

类Seq实现了一个语句序列。对空语句的测试是为了避免使用标号。*注意：空语句Stmt.Null不会产生任何代码，因为类Stmt中的方法gen不做任何处理*

```java
package inter;


/**
 * Set
 */
public class Seq extends Stmt {
    Stmt stmt1;
    Stmt stmt2;

    public Seq(Stmt s1, Stmt s2) {
        stmt1 = s1;
        stmt2 = s2;
    }

    @Override
    public void gen(int b, int a) {
        if (stmt1 == Stmt.Null)
            stmt2.gen(b, a);
        else if (stmt2 == Stmt.Null)
            stmt1.gen(b, a);
        else {
            int label = newlabel();
            stmt1.gen(b, label);
            stmt2.gen(label, a);
        }
    }
    
}
```

一个break语句把控制流转出它的外围循环或外围switch语句。类Break使用字段stmt来保存它的外围语句构造(语法分析器保证Stmt.Enclosing表示了其外围构造对应的语法树结点)。一个Break对象的代码是一个目标为标号stmt.after的跳转指令。这个标号标记了紧跟在stmt的代码之后的指令。

```java
package inter;


/**
 * If
 */
public class Break extends Stmt {
    Stmt stmt;
    public Break() {
        if (Stmt.Enclosing == Stmt.Null)
            error("unenclosed break");
        stmt = Stmt.Enclosing;
    }
    
    @Override
    public void gen(int b, int a) {
        emit("goto L" + stmt.after);
    }

}
```

## 语法分析器

语法分析器读入一个由词法单元组成的流，构建出一棵抽象语法树。

```java
package parser;

import java.io.*;

import lexer.*;
import symbols.*;
import inter.*;

/**
 * Parser
 */
public class Parser {
    /**
     * 这个语法分析器的词法分析器
     */
    Lexer lex;
    /**
     * 向前看词法单元
     */
    Token look;
    /**
     * 当前或顶层的符号表 
     */
    Env top = null;
    /**
     * 用于变量声明的存储位置
     */
    int used = 0;

    /**
     * 
     * @param lexer
     */
    public Parser(Lexer lexer) throws IOException {
        lex = lexer;
        move();
    }

    private void move() throws IOException {
        look = lex.scan();
    }

    private void error(String s) throws IOException {
        throw new Error("near line " + Lexer.line + ": " + s);
    }

    void match(int t) throws IOException {
        if (look.tag == t)
            move();
        else 
            error("syntax error");
    }

    /**
     * 
     */
    public void program() throws IOException {
        Stmt s = block();
        int begin = s.newlabel();
        int after = s.newlabel();
        s.emitlabel(begin);
        s.gen(begin, after);
        s.emitlabel(after);
    }

    private Stmt block() throws IOException {
        match('{');
        Env savedEnv = top;
        top = new Env(top);
        decls();
        Stmt s = stmts();
        match('}');
        top = savedEnv;
        return s;
    }

    private void decls() throws IOException {
        while (look.tag == Tag.BASIC) {
            Type p = type();
            Token tok = look;
            match(Tag.ID);
            match(';');
            Id id = new Id((Word)tok, p, used);
            top.put(tok, id);
        }
    }

    private Type type() throws IOException {
        Type p = (Type)look;
        match(Tag.BASIC);
        if (look.tag != '[')
            return p;
        else 
            return dims(p);
    }

    private Type dims(Type p) throws IOException {
        match('[');
        Token tok = look;
        match(Tag.NUM);
        match(']');
        if (look.tag == '[');
            p = dims(p);
        return new Array(((Num)tok).value, p);
    }

    private Stmt stmts() throws IOException {
        if (look.tag == '}')
            return Stmt.Null;
        else 
            return new Seq(stmt(), stmts());
    }

    private Stmt stmt() throws IOException {
        Expr x;
        Stmt s1, s2;
        Stmt savedStmt;
        switch(look.tag) {
            case ';':
                move();
                return Stmt.Null;
            case Tag.IF:
                match(Tag.IF);
                match('(');
                x = bool();
                match(')');
                s1 = stmt();
                if (look.tag != Tag.ELSE)
                    return new If(x, s1);
                match(Tag.ELSE);
                s2 = stmt();
                return new Else(x, s1, s2);
            case Tag.WHILE:
                While whilenode = new While();
                savedStmt = Stmt.Enclosing;
                Stmt.Enclosing = whilenode;
                match(Tag.WHILE);
                match('(');
                x = bool();
                match(')');
                s1 = stmt();
                whilenode.init(x, s1);
                Stmt.Enclosing = savedStmt;
                return whilenode;
            case Tag.DO:
                Do donode = new Do();
                savedStmt = Stmt.Enclosing;
                Stmt.Enclosing = donode;
                match(Tag.DO);
                s1 = stmt();
                match(Tag.WHILE);
                match('(');
                x = bool();
                match(')');
                Stmt.Enclosing = savedStmt;
                return donode;
            case Tag.BREAK:
                match(Tag.BREAK);
                match(';');
                return new Break();
            case '{':
                return block();
            default:
                return assign();
        }
    }

    private Expr bool() throws IOException {
        Expr x = join();
        while (look.tag == Tag.OR) {
            Token tok = look;
            move();
            x = new Or(tok, x, join());
        }
        return x;
    }

    private Expr join() throws IOException {
        Expr x = equality();
        while (look.tag == Tag.AND) {
            Token tok = look;
            move();
            x = new And(tok, x, equality());
        }
        return x;
    }

    private Expr equality() throws IOException {
        Expr x = rel();
        while (look.tag == Tag.EQ || look.tag == Tag.NE) {
            Token tok = look;
            move();
            x = new Rel(tok, x, rel());
        }
        return x;
    }

    private Expr rel() throws IOException {
        Expr x = expr();
        switch (look.tag) {
            case '<':
            case Tag.LE:
            case Tag.GE:
            case '>':
                Token tok = look;
                move();
                return new Rel(tok, x, expr());
            default:
                return x;
        }
    }

    private Expr expr() throws IOException {
        Expr x = term();
        while (look.tag == '+' || look.tag == '-') {
            Token tok = look;
            move();
            x = new Arith(tok, x, term());
        }
        return x;
    }

    private Expr term() throws IOException {
        Expr x = unary();
        while (look.tag == '+' || look.tag == '/') {
            Token tok = look;
            move();
            x = new Arith(tok, x, unary());
        }
        return x;
    }

    private Expr unary() throws IOException {
        if (look.tag == '-') {
            move();
            return new Unary(Word.minus, unary());
        }
        else if (look.tag == '!') {
            Token tok = look;
            move();
            return new Not(tok, unary());
        }
        else
            return factor();
    }

    private Expr factor() throws IOException {
        Expr x = null;
        switch (look.tag) {
            case '(':
                move(); 
                x = bool();
                match(')');
                return x;
            case Tag.NUM:
                x = new Constant(look, Type.Int);
                move();
                return x;
            case Tag.REAL:
                x = new Constant(look, Type.Float);
                move();
                return x;
            case Tag.TRUE:
                x = Constant.True;
                move();
                return x;
            case Tag.FALSE:
                x = Constant.False;
                move();
                return x;
            default:
                error("syntax error");
                return x;
            case Tag.ID:
                Id id = top.get(look);
                if (id == null)
                    error(look.toString() + " undeclare");
                move();
                if (look.tag != '[')
                    return id;
                else 
                    return offset(id);       
        }
    }

    private Stmt assign() throws IOException {
        Stmt stmt;
        Token t = look;
        match(Tag.ID);
        Id id = top.get(t);
        if (id == null) 
            error(t.toString() + " undeclared");
        if (look.tag == '=') {
            move();
            stmt = new Set(id, bool());
        }
        else {
            Access x = offset(id);
            match('=');
            stmt = new SetElem(x, bool());
        }
        match(';');
        return stmt;
    }

    private Access offset(Id a) throws IOException {
        Expr i;
        Expr w;
        Expr t1;
        Expr t2;
        Expr loc;
        Type type = a.type;
        match('['); 
        i = bool();
        match(']');
        type = ((Array)type).of;
        w = new Constant(type.width);
        t1 = new Arith(new Token('*'), i, w);
        loc = t1;
        while (look.tag == '[') {
            match('[');
            i = bool();
            match(']');
            type = ((Array)type).of;
            w = new Constant(type.width);
            t1 = new Arith(new Token('*'), i, w);
            t2 = new Arith(new Token('+'), loc, t1);
            loc = t2;
        }
        return new Access(a, loc, type);
    }


}
```

