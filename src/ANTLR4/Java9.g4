/*
 * [The "BSD license"]
 *  Copyright (c) 2014 Terence Parr
 *  Copyright (c) 2014 Sam Harwell
 *  Copyright (c) 2017 Chan Chung Kwong
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 *  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 *  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * A Java 9 grammar for ANTLR 4 derived from the Java Language Specification
 * chapter 19.
 *
 * NOTE: This grammar results in a generated parser that is much slower
 *       than the Java 7 grammar in the grammars-v4/java directory. This
 *     one is, however, extremely close to the spec.
 *
 * You can test with
 *
 *  $ antlr4 Java9.g4
 *  $ javac *.java
 *  $ grun Java9 compilationUnit *.java
 *
 * Or,
~/antlr/code/grammars-v4/java9 $ java Test .
/Users/parrt/antlr/code/grammars-v4/java9/./Java9BaseListener.java
/Users/parrt/antlr/code/grammars-v4/java9/./Java9Lexer.java
/Users/parrt/antlr/code/grammars-v4/java9/./Java9Listener.java
/Users/parrt/antlr/code/grammars-v4/java9/./Java9Parser.java
/Users/parrt/antlr/code/grammars-v4/java9/./Test.java
Total lexer+parser time 30844ms.
~/antlr/code/grammars-v4/java9 $ java Test examples/module-info.java
/home/kwong/projects/grammars-v4/java9/examples/module-info.java
Total lexer+parser time 914ms.
~/antlr/code/grammars-v4/java9 $ java Test examples/TryWithResourceDemo.java
/home/kwong/projects/grammars-v4/java9/examples/TryWithResourceDemo.java
Total lexer+parser time 3634ms.
~/antlr/code/grammars-v4/java9 $ java Test examples/helloworld.java
/home/kwong/projects/grammars-v4/java9/examples/helloworld.java
Total lexer+parser time 2497ms.

 */
grammar Java9;

/*
 * Productions from §3 (Lexical Structure)
 */
// 字面量类型
literal
	:	IntegerLiteral            // 整数：十进制，二进制，八进制，十六进制浮点数，相比于C语言整数可以加下划线
	|	FloatingPointLiteral      // 浮点数：分为十进制和十六进制浮点数
	|	BooleanLiteral            // 布尔类型
	|	CharacterLiteral          // 字符字面量
	|	StringLiteral             // 字符串字面量
	|	NullLiteral               // 空类型
	;

/*
 * Productions from §4 (Types, Values, and Variables) 类型，值，变量
 */

// 原始类型
primitiveType
	:	annotation* numericType  // 注解
	|	annotation* 'boolean'    
	;

// 基本数值类型
numericType
	:	integralType
	|	floatingPointType
	;

// 整数类型
integralType
	:	'byte'
	|	'short'
	|	'int'
	|	'long'
	|	'char'
	;

// 浮点类型
floatingPointType
	:	'float'
	|	'double'
	;

// 引用类型
referenceType
	:	classOrInterfaceType      // 类或者接口类型
	|	typeVariable              // 类型变量
	|	arrayType                 // 数组类型
	;

/*classOrInterfaceType
	:	classType
	|	interfaceType
	;
*/
// 类或者接口类型
classOrInterfaceType
	:	(	classType_lfno_classOrInterfaceType
		|	interfaceType_lfno_classOrInterfaceType
		)
		(	classType_lf_classOrInterfaceType
		|	interfaceType_lf_classOrInterfaceType
		)*
	;

// class对象类型
classType
	:	annotation* identifier typeArguments?
	|	classOrInterfaceType '.' annotation* identifier typeArguments?
	;

// 类的.类或者接口类型
classType_lf_classOrInterfaceType
	:	'.' annotation* identifier typeArguments?
	;

// .类或者接口类型
classType_lfno_classOrInterfaceType
	:	annotation* identifier typeArguments?
	;

// 接口类型
interfaceType
	:	classType
	;

// 接口的类的.类或者接口类型
interfaceType_lf_classOrInterfaceType
	:	classType_lf_classOrInterfaceType
	;

// 接口的类的类或者接口类型
interfaceType_lfno_classOrInterfaceType
	:	classType_lfno_classOrInterfaceType
	;

// 类型变量
typeVariable
	:	annotation* identifier
	;

// 数组类型
arrayType
	:	primitiveType dims
	|	classOrInterfaceType dims
	|	typeVariable dims
	;

// 数组和多维数组
dims
	:	annotation* '[' ']' (annotation* '[' ']')*
	;

// 类型参数
typeParameter
	:	typeParameterModifier* identifier typeBound?
	;

// 类型参数修饰器
typeParameterModifier
	:	annotation
	;

// 类型约束
typeBound
	:	'extends' typeVariable
	|	'extends' classOrInterfaceType additionalBound*
	;

// 附加类型约束
additionalBound
	:	'&' interfaceType
	;

// 泛型参数
typeArguments
	:	'<' typeArgumentList '>'
	;

// 类型参数列表
typeArgumentList
	:	typeArgument (',' typeArgument)*
	;

// 类型参数
typeArgument
	:	referenceType   // 引用类型
	|	wildcard        // Java泛型约束
	;

// Java泛型约束
wildcard
	:	annotation* '?' wildcardBounds?
	;

// Java泛型约束类型
wildcardBounds
	:	'extends' referenceType
	|	'super' referenceType
	;

/*
 * Productions from §6 (Names)
 */

// 模块名称
moduleName
	:	identifier                 // 标识符
	|	moduleName '.' identifier  // 带点的级联模块
	;

// 包名称
packageName
	:	identifier
	|	packageName '.' identifier
	;

// 类型名称，单独的一个标识或者带包名的标识
typeName
	:	identifier
	|	packageOrTypeName '.' identifier
	;

// 带包名类型或者标识符类型
packageOrTypeName
	:	identifier
	|	packageOrTypeName '.' identifier
	;

// 表达式名称
expressionName
	:	identifier
	|	ambiguousName '.' identifier
	;

// 方法名称
methodName
	:	identifier
	;

// 模糊名称
ambiguousName
	:	identifier
	|	ambiguousName '.' identifier
	;

/*
 * Productions from §7 (Packages)
 */

// 完整的Java文件声明
compilationUnit
	:	ordinaryCompilation
	|	modularCompilation
	;

// 带Java类型的文件声明
ordinaryCompilation
	:	packageDeclaration? importDeclaration* typeDeclaration* EOF
	;

// 模块
modularCompilation
	:	importDeclaration* moduleDeclaration
	;

// 包声明
packageDeclaration
	:	packageModifier* 'package' packageName ';'
	;

// 包名指示器
packageModifier
	:	annotation
	;

// import语法
importDeclaration
	:	singleTypeImportDeclaration
	|	typeImportOnDemandDeclaration
	|	singleStaticImportDeclaration
	|	staticImportOnDemandDeclaration   // 静态导入类
	;

// 单类型导入
singleTypeImportDeclaration
	:	'import' typeName ';'
	;

// 导入整个包
typeImportOnDemandDeclaration
	:	'import' packageOrTypeName '.' '*' ';'
	;

// 单个方法静态导入
singleStaticImportDeclaration
	:	'import' 'static' typeName '.' identifier ';'
	;

// 静态导入整个类型
staticImportOnDemandDeclaration
	:	'import' 'static' typeName '.' '*' ';'
	;

// 类型声明 
typeDeclaration
	:	classDeclaration         // 类声明
	|	interfaceDeclaration     // 接口声明
	|	';'
	;

// 模块声明
moduleDeclaration
	:	annotation* 'open'? 'module' moduleName '{' moduleDirective* '}'
	;

// 
moduleDirective
	:	'requires' requiresModifier* moduleName ';'
	|	'exports' packageName ('to' moduleName (',' moduleName)*)? ';'
	|	'opens' packageName ('to' moduleName (',' moduleName)*)? ';'
	|	'uses' typeName ';'
	|	'provides' typeName 'with' typeName (',' typeName)* ';'
	;

// require指示器
requiresModifier
	:	'transitive'
	|	'static'
	;

/*
 * Productions from §8 (Classes)
 */

// 类声明(普通类和枚举类型)
classDeclaration
	:	normalClassDeclaration     // 一般类声明
	|	enumDeclaration            // 枚举声明   
	;

// 一般类声明，支持单类和多接口继承
normalClassDeclaration
	:	classModifier* 'class' identifier typeParameters? superclass? superinterfaces? classBody
	;

// 类指示器
classModifier
	:	annotation
	|	'public'
	|	'protected'
	|	'private'
	|	'abstract'
	|	'static'
	|	'final'
	|	'strictfp'
	;

// 泛型参数
typeParameters
	:	'<' typeParameterList '>'
	;

// 泛型参数列表
typeParameterList
	:	typeParameter (',' typeParameter)*
	;

// 类继承
superclass
	:	'extends' classType
	;

// 接口继承
superinterfaces
	:	'implements' interfaceTypeList
	;

// 接口类型列表
interfaceTypeList
	:	interfaceType (',' interfaceType)*
	;

// 类本体
classBody
	:	'{' classBodyDeclaration* '}'
	;

// 类本体声明
classBodyDeclaration
	:	classMemberDeclaration   // 类成员声明
	|	instanceInitializer      // 类块声明
	|	staticInitializer        // 类静态块声明
	|	constructorDeclaration   // 构造参数声明 
	;

// 类成员声明
classMemberDeclaration
	:	fieldDeclaration
	|	methodDeclaration
	|	classDeclaration
	|	interfaceDeclaration
	|	';'
	;

// 类成员声明
fieldDeclaration
	:	fieldModifier* unannType variableDeclaratorList ';'
	;

// 类成员指示器
fieldModifier
	:	annotation
	|	'public'
	|	'protected'
	|	'private'
	|	'static'
	|	'final'
	|	'transient'
	|	'volatile'
	;

// 变量声明列表
variableDeclaratorList
	:	variableDeclarator (',' variableDeclarator)*
	;

// 变量声明（赋值是可选的）
variableDeclarator
	:	variableDeclaratorId ('=' variableInitializer)?
	;

// 变量声明Id，包括单类型和数组类型
variableDeclaratorId
	:	identifier dims?
	;

// 变量初始化类型，包括简单表达式和数组初始化
variableInitializer
	:	expression
	|	arrayInitializer
	;

// 未声明类型
unannType
	:	unannPrimitiveType
	|	unannReferenceType
	;

// 未声明原始类型
unannPrimitiveType
	:	numericType
	|	'boolean'
	;

// 未声明引用类型，包括类，类型参数和数组类型
unannReferenceType
	:	unannClassOrInterfaceType
	|	unannTypeVariable
	|	unannArrayType
	;

/*unannClassOrInterfaceType
	:	unannClassType
	|	unannInterfaceType
	;
*/

// 未声明类和接口类型
unannClassOrInterfaceType
	:	(	unannClassType_lfno_unannClassOrInterfaceType
		|	unannInterfaceType_lfno_unannClassOrInterfaceType
		)
		(	unannClassType_lf_unannClassOrInterfaceType
		|	unannInterfaceType_lf_unannClassOrInterfaceType
		)*
	;

// 未声明类类型
unannClassType
	:	identifier typeArguments?
	|	unannClassOrInterfaceType '.' annotation* identifier typeArguments?
	;

// 未声明的类的.类或接口类型
unannClassType_lf_unannClassOrInterfaceType
	:	'.' annotation* identifier typeArguments?
	;

// 未声明的类的类或接口类型
unannClassType_lfno_unannClassOrInterfaceType
	:	identifier typeArguments?
	;

// 未声明接口类型
unannInterfaceType
	:	unannClassType
	;

// 未声明接口类型
unannInterfaceType_lf_unannClassOrInterfaceType
	:	unannClassType_lf_unannClassOrInterfaceType
	;

// 未声明接口类型
unannInterfaceType_lfno_unannClassOrInterfaceType
	:	unannClassType_lfno_unannClassOrInterfaceType
	;

// 未声明值变量类型
unannTypeVariable
	:	identifier
	;

// 未声明数组类型
unannArrayType
	:	unannPrimitiveType dims
	|	unannClassOrInterfaceType dims
	|	unannTypeVariable dims
	;

// 方法声明
methodDeclaration
	:	methodModifier* methodHeader methodBody
	;

// 方法指示器
methodModifier
	:	annotation
	|	'public'
	|	'protected'
	|	'private'
	|	'abstract'
	|	'static'
	|	'final'
	|	'synchronized'
	|	'native'
	|	'strictfp'
	;

// 方法头，包括泛型方法
methodHeader
	:	result methodDeclarator throws_?
	|	typeParameters annotation* result methodDeclarator throws_?
	;

// 函数返回结果
result
	:	unannType
	|	'void'
	;

// 方法体明，参数列表
methodDeclarator
	:	identifier '(' formalParameterList? ')' dims?
	;

// 一般参数列表
formalParameterList
	:	formalParameters ',' lastFormalParameter
	|	lastFormalParameter
	|	receiverParameter
	;

// 一般参数
formalParameters
	:	formalParameter (',' formalParameter)*
	|	receiverParameter (',' formalParameter)*
	;

// 一般参数
formalParameter
	:	variableModifier* unannType variableDeclaratorId
	;

// 变量指示器
variableModifier
	:	annotation
	|	'final'
	;

// 最后一般参数，用于表示灵活参数列表
lastFormalParameter
	:	variableModifier* unannType annotation* '...' variableDeclaratorId
	|	formalParameter
	;

// 接收参数
receiverParameter
	:	annotation* unannType (identifier '.')? 'this'
	;

// 异常声明
throws_
	:	'throws' exceptionTypeList
	;

// 异常类型列表
exceptionTypeList
	:	exceptionType (',' exceptionType)*
	;

// 异常类型不能未原始类型
exceptionType
	:	classType
	|	typeVariable
	;

// 方法体
methodBody
	:	block
	|	';'
	;

// 块初始化器
instanceInitializer
	:	block
	;

// 静态块初始化器
staticInitializer
	:	'static' block
	;

// 构造函数声明，是一个无返回值的参数
constructorDeclaration
	:	constructorModifier* constructorDeclarator throws_? constructorBody
	;

// 构造函数指示器
constructorModifier
	:	annotation
	|	'public'
	|	'protected'
	|	'private'
	;

// 构造函数声明
constructorDeclarator
	:	typeParameters? simpleTypeName '(' formalParameterList? ')'
	;

// 简单类型
simpleTypeName
	:	identifier
	;

// 构造函数体
constructorBody
	:	'{' explicitConstructorInvocation? blockStatements? '}'
	;

// 构造函数内部声明
explicitConstructorInvocation
	:	typeArguments? 'this' '(' argumentList? ')' ';'
	|	typeArguments? 'super' '(' argumentList? ')' ';'
	|	expressionName '.' typeArguments? 'super' '(' argumentList? ')' ';'
	|	primary '.' typeArguments? 'super' '(' argumentList? ')' ';'
	;

// 枚举声明，枚举不能继承类只能继承接口
enumDeclaration
	:	classModifier* 'enum' identifier superinterfaces? enumBody
	;

// 枚举体
enumBody
	:	'{' enumConstantList? ','? enumBodyDeclarations? '}'
	;

// 枚举常数列表
enumConstantList
	:	enumConstant (',' enumConstant)*
	;

// 枚举常数
enumConstant
	:	enumConstantModifier* identifier ('(' argumentList? ')')? classBody?
	;

// 枚举常数指示器，只能有注解
enumConstantModifier
	:	annotation
	;

// 枚举体声明，跟类差不多
enumBodyDeclarations
	:	';' classBodyDeclaration*
	;

/*
 * Productions from §9 (Interfaces)
 */

// 接口声明
interfaceDeclaration
	:	normalInterfaceDeclaration   // 一般接口声明
	|	annotationTypeDeclaration    // 注解类型声明
	;

// 一般接口声明，包括泛型接口
normalInterfaceDeclaration
	:	interfaceModifier* 'interface' identifier typeParameters? extendsInterfaces? interfaceBody
	;

// 接口指示器
interfaceModifier
	:	annotation
	|	'public'
	|	'protected'
	|	'private'
	|	'abstract'
	|	'static'
	|	'strictfp'
	;

// 接口的接口继承
extendsInterfaces
	:	'extends' interfaceTypeList
	;

// 接口体
interfaceBody
	:	'{' interfaceMemberDeclaration* '}'
	;

// 接口成员声明
interfaceMemberDeclaration
	:	constantDeclaration
	|	interfaceMethodDeclaration
	|	classDeclaration
	|	interfaceDeclaration
	|	';'
	;

// 常数声明
constantDeclaration
	:	constantModifier* unannType variableDeclaratorList ';'
	;

// 常数指示器
constantModifier
	:	annotation
	|	'public'
	|	'static'
	|	'final'
	;

// 接口方法声明
interfaceMethodDeclaration
	:	interfaceMethodModifier* methodHeader methodBody
	;

// 接口方法指示器
interfaceMethodModifier
	:	annotation
	|	'public'
	|	'private'//Introduced in Java 9
	|	'abstract'
	|	'default'
	|	'static'
	|	'strictfp'
	;

// 注解类型@interface声明
annotationTypeDeclaration
	:	interfaceModifier* '@' 'interface' identifier annotationTypeBody
	;

// 注解类型体
annotationTypeBody
	:	'{' annotationTypeMemberDeclaration* '}'
	;

// 注解类型成员声明
annotationTypeMemberDeclaration
	:	annotationTypeElementDeclaration
	|	constantDeclaration
	|	classDeclaration
	|	interfaceDeclaration
	|	';'
	;

// 注解元素声明
annotationTypeElementDeclaration
	:	annotationTypeElementModifier* unannType identifier '(' ')' dims? defaultValue? ';'
	;

// 注解类型元素指示器
annotationTypeElementModifier
	:	annotation
	|	'public'
	|	'abstract'
	;

// 注解默认值
defaultValue
	:	'default' elementValue
	;

// 注解
annotation
	:	normalAnnotation         // 一般注解
	|	markerAnnotation         // 标记注解
	|	singleElementAnnotation  // 单元素注解
	;

// 一般注解
normalAnnotation
	:	'@' typeName '(' elementValuePairList? ')'
	;

// 元素值对列表
elementValuePairList
	:	elementValuePair (',' elementValuePair)*
	;

// 元素值对
elementValuePair
	:	identifier '=' elementValue
	;

// 元素值
elementValue
	:	conditionalExpression
	|	elementValueArrayInitializer
	|	annotation
	;

// 元素值数组初始化
elementValueArrayInitializer
	:	'{' elementValueList? ','? '}'
	;

// 元素值列表
elementValueList
	:	elementValue (',' elementValue)*
	;

// 标记注解
markerAnnotation
	:	'@' typeName
	;

// 单元素注解
singleElementAnnotation
	:	'@' typeName '(' elementValue ')'
	;

/*
 * Productions from §10 (Arrays)
 */

// 数组初始化
arrayInitializer
	:	'{' variableInitializerList? ','? '}'
	;

// 变量初始化列表
variableInitializerList
	:	variableInitializer (',' variableInitializer)*
	;

/*
 * Productions from §14 (Blocks and Statements)
 */

// 块
block
	:	'{' blockStatements? '}'
	;

// 块状态
blockStatements
	:	blockStatement+
	;

// 块状态
blockStatement
	:	localVariableDeclarationStatement
	|	classDeclaration
	|	statement
	;

// 局部变量声明状态
localVariableDeclarationStatement
	:	localVariableDeclaration ';'
	;

// 局部变量声明
localVariableDeclaration
	:	variableModifier* unannType variableDeclaratorList
	;

// 状态
statement
	:	statementWithoutTrailingSubstatement
	|	labeledStatement
	|	ifThenStatement
	|	ifThenElseStatement
	|	whileStatement
	|	forStatement
	;

// 状态 没有短if
statementNoShortIf
	:	statementWithoutTrailingSubstatement
	|	labeledStatementNoShortIf
	|	ifThenElseStatementNoShortIf
	|	whileStatementNoShortIf
	|	forStatementNoShortIf
	;

// 状态 没有子状态
statementWithoutTrailingSubstatement
	:	block
	|	emptyStatement
	|	expressionStatement
	|	assertStatement
	|	switchStatement
	|	doStatement
	|	breakStatement
	|	continueStatement
	|	returnStatement
	|	synchronizedStatement
	|	throwStatement
	|	tryStatement
	;

// 空状态
emptyStatement
	:	';'
	;

// 标记状态
labeledStatement
	:	identifier ':' statement
	;

// 标记状态 没有短if
labeledStatementNoShortIf
	:	identifier ':' statementNoShortIf
	;

// 表达式状态
expressionStatement
	:	statementExpression ';'
	;

// 状态表达式
statementExpression
	:	assignment                // 赋值
	|	preIncrementExpression   
	|	preDecrementExpression
	|	postIncrementExpression
	|	postDecrementExpression
	|	methodInvocation          // 方法调用
	|	classInstanceCreationExpression  // 类创建表达式
	;

// if状态
ifThenStatement
	:	'if' '(' expression ')' statement
	;

// if状态
ifThenElseStatement
	:	'if' '(' expression ')' statementNoShortIf 'else' statement
	;

// if else 状态
ifThenElseStatementNoShortIf
	:	'if' '(' expression ')' statementNoShortIf 'else' statementNoShortIf
	;

// 断言状态
assertStatement
	:	'assert' expression ';'
	|	'assert' expression ':' expression ';'
	;

// switch状态
switchStatement
	:	'switch' '(' expression ')' switchBlock
	;

// switch体
switchBlock
	:	'{' switchBlockStatementGroup* switchLabel* '}'
	;

// switch 两种case方式
switchBlockStatementGroup
	:	switchLabels blockStatements
	;

// switch 标签
switchLabels
	:	switchLabel+
	;

// switch 标签，只允许枚举和常数表达式
switchLabel
	:	'case' constantExpression ':'
	|	'case' enumConstantName ':'
	|	'default' ':'
	;

// 枚举常数
enumConstantName
	:	identifier
	;

// while stat
whileStatement
	:	'while' '(' expression ')' statement
	;

// while stat 没有短if
whileStatementNoShortIf
	:	'while' '(' expression ')' statementNoShortIf
	;

// do stat
doStatement
	:	'do' statement 'while' '(' expression ')' ';'
	;

// for stat 两种for表达式
forStatement
	:	basicForStatement
	|	enhancedForStatement
	;

// for stat 没有短if
forStatementNoShortIf
	:	basicForStatementNoShortIf
	|	enhancedForStatementNoShortIf
	;

// 一般for
basicForStatement
	:	'for' '(' forInit? ';' expression? ';' forUpdate? ')' statement
	;

// 一般for 无short if
basicForStatementNoShortIf
	:	'for' '(' forInit? ';' expression? ';' forUpdate? ')' statementNoShortIf
	;

// for初始化
forInit
	:	statementExpressionList
	|	localVariableDeclaration
	;

// for更新
forUpdate
	:	statementExpressionList
	;

// stat 表达式列表
statementExpressionList
	:	statementExpression (',' statementExpression)*
	;

// 迭代器for
enhancedForStatement
	:	'for' '(' variableModifier* unannType variableDeclaratorId ':' expression ')' statement
	;

// 迭代器for no short if
enhancedForStatementNoShortIf
	:	'for' '(' variableModifier* unannType variableDeclaratorId ':' expression ')' statementNoShortIf
	;

// break stat
breakStatement
	:	'break' identifier? ';'
	;

// continue stat
continueStatement
	:	'continue' identifier? ';'
	;

// return stat
returnStatement
	:	'return' expression? ';'
	;

// throw stat
throwStatement
	:	'throw' expression ';'
	;

// synchronized stat
synchronizedStatement
	:	'synchronized' '(' expression ')' block
	;

// try stat，一种有finally，一种没有,还有一种带资源释放的try,类似python的with ... as ... 
tryStatement
	:	'try' block catches
	|	'try' block catches? finally_
	|	tryWithResourcesStatement
	;

// catch 
catches
	:	catchClause+
	;

// catch 
catchClause
	:	'catch' '(' catchFormalParameter ')' block
	;

// catch 的参数列表
catchFormalParameter
	:	variableModifier* catchType variableDeclaratorId
	;

// catch类型
catchType
	:	unannClassType ('|' classType)*
	;

// finally
finally_
	:	'finally' block
	;

// 携带资源释放的try
tryWithResourcesStatement
	:	'try' resourceSpecification block catches? finally_?
	;

// 资源声明
resourceSpecification
	:	'(' resourceList ';'? ')'
	;

// 资源列表
resourceList
	:	resource (';' resource)*
	;

// 资源
resource
	:	variableModifier* unannType variableDeclaratorId '=' expression
	|	variableAccess//Introduced in Java 9
	;

// 变量访问
variableAccess
	:	expressionName
	|	fieldAccess
	;

/*
 * Productions from §15 (Expressions)
 */

/*primary
	:	primaryNoNewArray
	|	arrayCreationExpression
	;
*/

// 
primary
	:	(	primaryNoNewArray_lfno_primary
		|	arrayCreationExpression
		)
		(	primaryNoNewArray_lf_primary
		)*
	;

primaryNoNewArray
	:	literal
	|	classLiteral
	|	'this'
	|	typeName '.' 'this'
	|	'(' expression ')'
	|	classInstanceCreationExpression
	|	fieldAccess
	|	arrayAccess
	|	methodInvocation
	|	methodReference
	;

primaryNoNewArray_lf_arrayAccess
	:
	;

primaryNoNewArray_lfno_arrayAccess
	:	literal
	|	typeName ('[' ']')* '.' 'class'
	|	'void' '.' 'class'
	|	'this'
	|	typeName '.' 'this'
	|	'(' expression ')'
	|	classInstanceCreationExpression
	|	fieldAccess
	|	methodInvocation
	|	methodReference
	;

primaryNoNewArray_lf_primary
	:	classInstanceCreationExpression_lf_primary
	|	fieldAccess_lf_primary
	|	arrayAccess_lf_primary
	|	methodInvocation_lf_primary
	|	methodReference_lf_primary
	;

primaryNoNewArray_lf_primary_lf_arrayAccess_lf_primary
	:
	;

primaryNoNewArray_lf_primary_lfno_arrayAccess_lf_primary
	:	classInstanceCreationExpression_lf_primary
	|	fieldAccess_lf_primary
	|	methodInvocation_lf_primary
	|	methodReference_lf_primary
	;

primaryNoNewArray_lfno_primary
	:	literal
	|	typeName ('[' ']')* '.' 'class'
	|	unannPrimitiveType ('[' ']')* '.' 'class'
	|	'void' '.' 'class'
	|	'this'
	|	typeName '.' 'this'
	|	'(' expression ')'
	|	classInstanceCreationExpression_lfno_primary
	|	fieldAccess_lfno_primary
	|	arrayAccess_lfno_primary
	|	methodInvocation_lfno_primary
	|	methodReference_lfno_primary
	;

primaryNoNewArray_lfno_primary_lf_arrayAccess_lfno_primary
	:
	;

primaryNoNewArray_lfno_primary_lfno_arrayAccess_lfno_primary
	:	literal
	|	typeName ('[' ']')* '.' 'class'
	|	unannPrimitiveType ('[' ']')* '.' 'class'
	|	'void' '.' 'class'
	|	'this'
	|	typeName '.' 'this'
	|	'(' expression ')'
	|	classInstanceCreationExpression_lfno_primary
	|	fieldAccess_lfno_primary
	|	methodInvocation_lfno_primary
	|	methodReference_lfno_primary
	;

// 类型字面量
classLiteral
	:	(typeName|numericType|'boolean') ('[' ']')* '.' 'class'
	|	'void' '.' 'class'
	;

classInstanceCreationExpression
	:	'new' typeArguments? annotation* identifier ('.' annotation* identifier)* typeArgumentsOrDiamond? '(' argumentList? ')' classBody?
	|	expressionName '.' 'new' typeArguments? annotation* identifier typeArgumentsOrDiamond? '(' argumentList? ')' classBody?
	|	primary '.' 'new' typeArguments? annotation* identifier typeArgumentsOrDiamond? '(' argumentList? ')' classBody?
	;

classInstanceCreationExpression_lf_primary
	:	'.' 'new' typeArguments? annotation* identifier typeArgumentsOrDiamond? '(' argumentList? ')' classBody?
	;

classInstanceCreationExpression_lfno_primary
	:	'new' typeArguments? annotation* identifier ('.' annotation* identifier)* typeArgumentsOrDiamond? '(' argumentList? ')' classBody?
	|	expressionName '.' 'new' typeArguments? annotation* identifier typeArgumentsOrDiamond? '(' argumentList? ')' classBody?
	;

typeArgumentsOrDiamond
	:	typeArguments
	|	'<' '>'
	;

// 类成员访问
fieldAccess
	:	primary '.' identifier
	|	'super' '.' identifier
	|	typeName '.' 'super' '.' identifier
	;

// 类成员访问
fieldAccess_lf_primary
	:	'.' identifier
	;

// 类成员访问
fieldAccess_lfno_primary
	:	'super' '.' identifier
	|	typeName '.' 'super' '.' identifier
	;

/*arrayAccess
	:	expressionName '[' expression ']'
	|	primaryNoNewArray '[' expression ']'
	;
*/

// 数组访问
arrayAccess
	:	(	expressionName '[' expression ']'
		|	primaryNoNewArray_lfno_arrayAccess '[' expression ']'
		)
		(	primaryNoNewArray_lf_arrayAccess '[' expression ']'
		)*
	;

// 数组访问
arrayAccess_lf_primary
	:	(	primaryNoNewArray_lf_primary_lfno_arrayAccess_lf_primary '[' expression ']'
		)
		(	primaryNoNewArray_lf_primary_lf_arrayAccess_lf_primary '[' expression ']'
		)*
	;
	
// 数组访问	
arrayAccess_lfno_primary
	:	(	expressionName '[' expression ']'
		|	primaryNoNewArray_lfno_primary_lfno_arrayAccess_lfno_primary '[' expression ']'
		)
		(	primaryNoNewArray_lfno_primary_lf_arrayAccess_lfno_primary '[' expression ']'
		)*
	;

// 函数调用
methodInvocation
	:	methodName '(' argumentList? ')'
	|	typeName '.' typeArguments? identifier '(' argumentList? ')'
	|	expressionName '.' typeArguments? identifier '(' argumentList? ')'
	|	primary '.' typeArguments? identifier '(' argumentList? ')'
	|	'super' '.' typeArguments? identifier '(' argumentList? ')'
	|	typeName '.' 'super' '.' typeArguments? identifier '(' argumentList? ')'
	;

methodInvocation_lf_primary
	:	'.' typeArguments? identifier '(' argumentList? ')'
	;

methodInvocation_lfno_primary
	:	methodName '(' argumentList? ')'
	|	typeName '.' typeArguments? identifier '(' argumentList? ')'
	|	expressionName '.' typeArguments? identifier '(' argumentList? ')'
	|	'super' '.' typeArguments? identifier '(' argumentList? ')'
	|	typeName '.' 'super' '.' typeArguments? identifier '(' argumentList? ')'
	;

argumentList
	:	expression (',' expression)*
	;

// 方法引用
methodReference
	:	expressionName '::' typeArguments? identifier
	|	referenceType '::' typeArguments? identifier
	|	primary '::' typeArguments? identifier
	|	'super' '::' typeArguments? identifier
	|	typeName '.' 'super' '::' typeArguments? identifier
	|	classType '::' typeArguments? 'new'
	|	arrayType '::' 'new'
	;

methodReference_lf_primary
	:	'::' typeArguments? identifier
	;

methodReference_lfno_primary
	:	expressionName '::' typeArguments? identifier
	|	referenceType '::' typeArguments? identifier
	|	'super' '::' typeArguments? identifier
	|	typeName '.' 'super' '::' typeArguments? identifier
	|	classType '::' typeArguments? 'new'
	|	arrayType '::' 'new'
	;

// 数组创建表达式
arrayCreationExpression
	:	'new' primitiveType dimExprs dims?
	|	'new' classOrInterfaceType dimExprs dims?
	|	'new' primitiveType dims arrayInitializer
	|	'new' classOrInterfaceType dims arrayInitializer
	;

// 数组表达式
dimExprs
	:	dimExpr+
	;

// 数组表达式
dimExpr
	:	annotation* '[' expression ']'
	;

// 常数表达式
constantExpression
	:	expression
	;

// 表达式
expression
	:	lambdaExpression
	|	assignmentExpression
	;

// lambda 表达式
lambdaExpression
	:	lambdaParameters '->' lambdaBody
	;

// lambda 参数
lambdaParameters
	:	identifier
	|	'(' formalParameterList? ')'
	|	'(' inferredFormalParameterList ')'
	;

// 
inferredFormalParameterList
	:	identifier (',' identifier)*
	;

// lambda 体
lambdaBody
	:	expression
	|	block
	;

// 赋值表达式
assignmentExpression
	:	conditionalExpression
	|	assignment
	;

// 赋值表达式
assignment
	:	leftHandSide assignmentOperator expression
	;

// 左值表达死
leftHandSide
	:	expressionName
	|	fieldAccess
	|	arrayAccess
	;

// 赋值运算符
assignmentOperator
	:	'='
	|	'*='
	|	'/='
	|	'%='
	|	'+='
	|	'-='
	|	'<<='
	|	'>>='
	|	'>>>='
	|	'&='
	|	'^='
	|	'|='
	;

// 条件表达式
conditionalExpression
	:	conditionalOrExpression
	|	conditionalOrExpression '?' expression ':' (conditionalExpression|lambdaExpression)
	;

// 条件或表达式
conditionalOrExpression
	:	conditionalAndExpression
	|	conditionalOrExpression '||' conditionalAndExpression
	;

// 条件与表达式
conditionalAndExpression
	:	inclusiveOrExpression
	|	conditionalAndExpression '&&' inclusiveOrExpression
	;

// 按位或表达式
inclusiveOrExpression
	:	exclusiveOrExpression
	|	inclusiveOrExpression '|' exclusiveOrExpression
	;

// 按位异或表达式
exclusiveOrExpression
	:	andExpression
	|	exclusiveOrExpression '^' andExpression
	;

// 按位与表达式
andExpression
	:	equalityExpression
	|	andExpression '&' equalityExpression
	;

// 比较表达式
equalityExpression
	:	relationalExpression
	|	equalityExpression '==' relationalExpression
	|	equalityExpression '!=' relationalExpression
	;

// 关系表达式
relationalExpression
	:	shiftExpression
	|	relationalExpression '<' shiftExpression
	|	relationalExpression '>' shiftExpression
	|	relationalExpression '<=' shiftExpression
	|	relationalExpression '>=' shiftExpression
	|	relationalExpression 'instanceof' referenceType
	;

// 按位移动表达式
shiftExpression
	:	additiveExpression
	|	shiftExpression '<' '<' additiveExpression
	|	shiftExpression '>' '>' additiveExpression
	|	shiftExpression '>' '>' '>' additiveExpression
	;

// 加减表达式
additiveExpression
	:	multiplicativeExpression
	|	additiveExpression '+' multiplicativeExpression
	|	additiveExpression '-' multiplicativeExpression
	;

// 乘法表达式
multiplicativeExpression
	:	unaryExpression
	|	multiplicativeExpression '*' unaryExpression
	|	multiplicativeExpression '/' unaryExpression
	|	multiplicativeExpression '%' unaryExpression
	;

// 单目表达式
unaryExpression
	:	preIncrementExpression
	|	preDecrementExpression
	|	'+' unaryExpression
	|	'-' unaryExpression
	|	unaryExpressionNotPlusMinus
	;

// 自增表达式
preIncrementExpression
	:	'++' unaryExpression
	;

// 自减表达式
preDecrementExpression
	:	'--' unaryExpression
	;

// 单目表达式没有+,-
unaryExpressionNotPlusMinus
	:	postfixExpression
	|	'~' unaryExpression
	|	'!' unaryExpression
	|	castExpression
	;

/*postfixExpression
	:	primary
	|	expressionName
	|	postIncrementExpression
	|	postDecrementExpression
	;
*/

postfixExpression
	:	(	primary
		|	expressionName
		)
		(	postIncrementExpression_lf_postfixExpression
		|	postDecrementExpression_lf_postfixExpression
		)*
	;

postIncrementExpression
	:	postfixExpression '++'
	;

postIncrementExpression_lf_postfixExpression
	:	'++'
	;

postDecrementExpression
	:	postfixExpression '--'
	;

postDecrementExpression_lf_postfixExpression
	:	'--'
	;

// 类型转换表达式
castExpression
	:	'(' primitiveType ')' unaryExpression
	|	'(' referenceType additionalBound* ')' unaryExpressionNotPlusMinus
	|	'(' referenceType additionalBound* ')' lambdaExpression
	;

// LEXER

identifier : Identifier | 'to' | 'module' | 'open' | 'with' | 'provides' | 'uses' | 'opens' | 'requires' | 'exports';

// §3.9 Keywords

ABSTRACT : 'abstract';
ASSERT : 'assert';
BOOLEAN : 'boolean';
BREAK : 'break';
BYTE : 'byte';
CASE : 'case';
CATCH : 'catch';
CHAR : 'char';
CLASS : 'class';
CONST : 'const';
CONTINUE : 'continue';
DEFAULT : 'default';
DO : 'do';
DOUBLE : 'double';
ELSE : 'else';
ENUM : 'enum';
EXTENDS : 'extends';
FINAL : 'final';
FINALLY : 'finally';
FLOAT : 'float';
FOR : 'for';
IF : 'if';
GOTO : 'goto';
IMPLEMENTS : 'implements';
IMPORT : 'import';
INSTANCEOF : 'instanceof';
INT : 'int';
INTERFACE : 'interface';
LONG : 'long';
NATIVE : 'native';
NEW : 'new';
PACKAGE : 'package';
PRIVATE : 'private';
PROTECTED : 'protected';
PUBLIC : 'public';
RETURN : 'return';
SHORT : 'short';
STATIC : 'static';
STRICTFP : 'strictfp';
SUPER : 'super';
SWITCH : 'switch';
SYNCHRONIZED : 'synchronized';
THIS : 'this';
THROW : 'throw';
THROWS : 'throws';
TRANSIENT : 'transient';
TRY : 'try';
VOID : 'void';
VOLATILE : 'volatile';
WHILE : 'while';
UNDER_SCORE : '_';//Introduced in Java 9

// §3.10.1 Integer Literals

// 整数字面量
IntegerLiteral
	:	DecimalIntegerLiteral
	|	HexIntegerLiteral
	|	OctalIntegerLiteral
	|	BinaryIntegerLiteral
	;

fragment
DecimalIntegerLiteral
	:	DecimalNumeral IntegerTypeSuffix?
	;

fragment
HexIntegerLiteral
	:	HexNumeral IntegerTypeSuffix?
	;

fragment
OctalIntegerLiteral
	:	OctalNumeral IntegerTypeSuffix?
	;

fragment
BinaryIntegerLiteral
	:	BinaryNumeral IntegerTypeSuffix?
	;

fragment
IntegerTypeSuffix
	:	[lL]
	;

fragment
DecimalNumeral
	:	'0'
	|	NonZeroDigit (Digits? | Underscores Digits)
	;

fragment
Digits
	:	Digit (DigitsAndUnderscores? Digit)?
	;

fragment
Digit
	:	'0'
	|	NonZeroDigit
	;

fragment
NonZeroDigit
	:	[1-9]
	;

fragment
DigitsAndUnderscores
	:	DigitOrUnderscore+
	;

fragment
DigitOrUnderscore
	:	Digit
	|	'_'
	;

fragment
Underscores
	:	'_'+
	;

fragment
HexNumeral
	:	'0' [xX] HexDigits
	;

fragment
HexDigits
	:	HexDigit (HexDigitsAndUnderscores? HexDigit)?
	;

fragment
HexDigit
	:	[0-9a-fA-F]
	;

fragment
HexDigitsAndUnderscores
	:	HexDigitOrUnderscore+
	;

fragment
HexDigitOrUnderscore
	:	HexDigit
	|	'_'
	;

fragment
OctalNumeral
	:	'0' Underscores? OctalDigits
	;

fragment
OctalDigits
	:	OctalDigit (OctalDigitsAndUnderscores? OctalDigit)?
	;

fragment
OctalDigit
	:	[0-7]
	;

fragment
OctalDigitsAndUnderscores
	:	OctalDigitOrUnderscore+
	;

fragment
OctalDigitOrUnderscore
	:	OctalDigit
	|	'_'
	;

fragment
BinaryNumeral
	:	'0' [bB] BinaryDigits
	;

fragment
BinaryDigits
	:	BinaryDigit (BinaryDigitsAndUnderscores? BinaryDigit)?
	;

fragment
BinaryDigit
	:	[01]
	;

fragment
BinaryDigitsAndUnderscores
	:	BinaryDigitOrUnderscore+
	;

fragment
BinaryDigitOrUnderscore
	:	BinaryDigit
	|	'_'
	;

// §3.10.2 Floating-Point Literals

FloatingPointLiteral
	:	DecimalFloatingPointLiteral
	|	HexadecimalFloatingPointLiteral
	;

fragment
DecimalFloatingPointLiteral
	:	Digits '.' Digits? ExponentPart? FloatTypeSuffix?
	|	'.' Digits ExponentPart? FloatTypeSuffix?
	|	Digits ExponentPart FloatTypeSuffix?
	|	Digits FloatTypeSuffix
	;

fragment
ExponentPart
	:	ExponentIndicator SignedInteger
	;

fragment
ExponentIndicator
	:	[eE]
	;

fragment
SignedInteger
	:	Sign? Digits
	;

fragment
Sign
	:	[+-]
	;

fragment
FloatTypeSuffix
	:	[fFdD]
	;

fragment
HexadecimalFloatingPointLiteral
	:	HexSignificand BinaryExponent FloatTypeSuffix?
	;

fragment
HexSignificand
	:	HexNumeral '.'?
	|	'0' [xX] HexDigits? '.' HexDigits
	;

fragment
BinaryExponent
	:	BinaryExponentIndicator SignedInteger
	;

fragment
BinaryExponentIndicator
	:	[pP]
	;

// §3.10.3 Boolean Literals

BooleanLiteral
	:	'true'
	|	'false'
	;

// §3.10.4 Character Literals

CharacterLiteral
	:	'\'' SingleCharacter '\''
	|	'\'' EscapeSequence '\''
	;

fragment
SingleCharacter
	:	~['\\\r\n]
	;

// §3.10.5 String Literals

StringLiteral
	:	'"' StringCharacters? '"'
	;

fragment
StringCharacters
	:	StringCharacter+
	;

fragment
StringCharacter
	:	~["\\\r\n]
	|	EscapeSequence
	;

// §3.10.6 Escape Sequences for Character and String Literals

fragment
EscapeSequence
	:	'\\' [btnfr"'\\]
	|	OctalEscape
    |   UnicodeEscape // This is not in the spec but prevents having to preprocess the input
	;

fragment
OctalEscape
	:	'\\' OctalDigit
	|	'\\' OctalDigit OctalDigit
	|	'\\' ZeroToThree OctalDigit OctalDigit
	;

fragment
ZeroToThree
	:	[0-3]
	;

// This is not in the spec but prevents having to preprocess the input
fragment
UnicodeEscape
    :   '\\' 'u'+ HexDigit HexDigit HexDigit HexDigit
    ;

// §3.10.7 The Null Literal

NullLiteral
	:	'null'
	;

// §3.11 Separators

LPAREN : '(';
RPAREN : ')';
LBRACE : '{';
RBRACE : '}';
LBRACK : '[';
RBRACK : ']';
SEMI : ';';
COMMA : ',';
DOT : '.';
ELLIPSIS : '...';
AT : '@';
COLONCOLON : '::';


// §3.12 Operators

ASSIGN : '=';
GT : '>';
LT : '<';
BANG : '!';
TILDE : '~';
QUESTION : '?';
COLON : ':';
ARROW : '->';
EQUAL : '==';
LE : '<=';
GE : '>=';
NOTEQUAL : '!=';
AND : '&&';
OR : '||';
INC : '++';
DEC : '--';
ADD : '+';
SUB : '-';
MUL : '*';
DIV : '/';
BITAND : '&';
BITOR : '|';
CARET : '^';
MOD : '%';
//LSHIFT : '<<';
//RSHIFT : '>>';
//URSHIFT : '>>>';

ADD_ASSIGN : '+=';
SUB_ASSIGN : '-=';
MUL_ASSIGN : '*=';
DIV_ASSIGN : '/=';
AND_ASSIGN : '&=';
OR_ASSIGN : '|=';
XOR_ASSIGN : '^=';
MOD_ASSIGN : '%=';
LSHIFT_ASSIGN : '<<=';
RSHIFT_ASSIGN : '>>=';
URSHIFT_ASSIGN : '>>>=';

// §3.8 Identifiers (must appear after all keywords in the grammar)

Identifier
	:	JavaLetter JavaLetterOrDigit*
	;

fragment
JavaLetter
	:	[a-zA-Z$_] // these are the "java letters" below 0x7F
	|	// covers all characters above 0x7F which are not a surrogate
		~[\u0000-\u007F\uD800-\uDBFF]
		{Character.isJavaIdentifierStart(_input.LA(-1))}?
	|	// covers UTF-16 surrogate pairs encodings for U+10000 to U+10FFFF
		[\uD800-\uDBFF] [\uDC00-\uDFFF]
		{Character.isJavaIdentifierStart(Character.toCodePoint((char)_input.LA(-2), (char)_input.LA(-1)))}?
	;

fragment
JavaLetterOrDigit
	:	[a-zA-Z0-9$_] // these are the "java letters or digits" below 0x7F
	|	// covers all characters above 0x7F which are not a surrogate
		~[\u0000-\u007F\uD800-\uDBFF]
		{Character.isJavaIdentifierPart(_input.LA(-1))}?
	|	// covers UTF-16 surrogate pairs encodings for U+10000 to U+10FFFF
		[\uD800-\uDBFF] [\uDC00-\uDFFF]
		{Character.isJavaIdentifierPart(Character.toCodePoint((char)_input.LA(-2), (char)_input.LA(-1)))}?
	;

//
// Whitespace and comments
//

WS  :  [ \t\r\n\u000C]+ -> skip
    ;

COMMENT
    :   '/*' .*? '*/' -> channel(HIDDEN)
    ;

LINE_COMMENT
    :   '//' ~[\r\n]* -> channel(HIDDEN)
    ;
