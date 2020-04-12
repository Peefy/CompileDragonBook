
# 前言

**DSL 领域特定语言**

领域特定语言（英语：domain-specific language、DSL）指的是专注于某个应用程序领域的计算机语言。又译作领域专用语言。

在定义DSL是什么的问题上，Fowler 认为业界经常使用的一些特征，例如“关注于领域”、“有限的表现”和“语言本质”是非常模糊的。因此，唯一能够确定DSL边界的方法是考虑“一门语言的一种特定用法”和“该语言的设计者或使用者的意图”：
如果XSLT的设计者将其设计为XML的转换工具，那么认为XSLT是一个DSL。如果一个用户使用DSL的目的是该DSL所要达到的目的，那么它是一个DSL，但是如果有人以通用的方式来使用一个DSL，那么它（在这种用法下）就不再是一个DSL了

域特定语言（英语：domain-specific language、DSL）指的是专注于某个应用程序领域的计算机语言。又译作领域专用语言。不同于普通的跨领域通用计算机语言(GPL)，领域特定语言只用在某些特定的领域。 比如用来显示网页的HTML，以及Emacs所使用的Emac LISP语言。

DSL可以简化复杂的代码，促进与客户沟通的效率，提高工作效率，清除发展瓶颈。

**编译相关的程序**

* **解释程序 (interpreter)**-
* **汇编程序（assembler）**-
* **连接程序（linker）**-
* **装入程序（loader）**-
* **预处理器（preprocessor）**-
* **编辑器（editor）**-
* **调试程序（debugger）**-
* **描述器（profiler）**-
* **项目管理程序（project manager）**-

**编译相关的步骤**

* **扫描程序（scanner）**-
* **语法分析（parser）**-
* **语义分析（semantic analyzer）**-
* **优化程序（source code optimizer）**-
* **代码生成（code generator）**-
* **目标代码（target code optimizer）**-

**编译相关的记号**

* **记号（token）**-
* **语法树（syntax tree）**-
* **符号表（symbol table）**-
* **常数表（literal table）**-
* **中间代码（intermediate code）**-
* **临时文件（temporary file）**-

# 第一部分

## 第1章 入门例子

### 1.1 哥特式建筑安全系统

想要构建一套这样的安全系统，公司的人进入之后，设置某种无线网络，安装一些小的设备。如果发生某些有趣的事情，这些设备会发出一条四字符的消息。比如，打开抽屉，抽屉上的感应器就会发出一条消息：D2OP。还有一些小的控制设备，响应这样的四字符命令消息。比如，某个设备收到D1UL消息，就会打开一扇门。

假设有这样一系列系统，它们共享着大多数组件和行为，却彼此间差异巨大。在这个例子中，对所有的客户来说，控制器发送和接收消息的方式是相同的。但是产生的事件序列和发送的命令却不尽相同。

把控制器看做是**状态机(state machine)**，每个感应器都可以发送**事件(event)**，改变控制器的**状态(state)**。当控制器进入某种状态时，就会在网络上发出一条命令消息。

### 1.2 状态机模型

对于指定控制器如何运作而言，状态机是一个恰当的抽象，下一步就是确保这个抽象能够运用到软件自身。如果人们在考虑控制器行为时，也要考虑事件，状态和转换，那么，希望这些词汇也可以出现在软件代码里。从本质上说，这就是**领域驱动设计(Domain-Driven Design)**中的**DDD原则**。也就是说在领域人员和程序员之间构建的一种共享语言。

对于Java程序来说，自然的方式就是以状态机为Domain Model。通过接收事件消息和发送命令消息，控制器得以同设备通信。这些消息都是四字母编码，可以通过通信通道进行发送。在控制器代码里面，想用**符号名(symbolic name)**引用这些消息。创建了事件类和命令类，它们都有代码(code)和名字(name)。把它们放到单独的类里面(有一个超类)，因为在控制器的代码里，它们扮演者不同的角色。

```java
class AbstractEvent {
    private String name, code;
    public AbstractEvent(String name, String code) {
        this.name = name;
        this.code = code;
    } 
    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
public class Command extends AbstractEvent {}
public class Event extends AbstractEvent {}
```

状态类记录了它会发送的命令及其相应的转换

```java
class State {
    private String name;
    private List<Command> actions = new ArrayList<Command>();
    private Map<String, Transition> transitions = new HashMap<String, Transtion>();
}

class State {
    public void addTranstion(Event event, State targetState) {
        assert null != targetState;
        transtitions.put(event.getCode(), new Transition(this, event, targetState));
    }
}

class Transition {
    private final State source, target;
    private final Event trigger;

    public Transtion(State source, Event trigger, State target){
        this.source = source;
        this.target = target;
        this.trigger = trigger;
    }

    public State getSource() {
        return source;
    }

    public State getTarget() {
        return target;
    }

    public Event getTrigger() {
        return trigger;
    }

    public String getEventCode() {
        return trigger.getCode();
    }
}
```

状态机保存了其起始状态。

```java
class StateMachine...
    private State start;

    public StateMachine(State start) {
        this.start = start;
    }
```

这样，从这个状态可以到达状态机里的任何状态

```java
class StateMachine...
    public Collection<State> getStates() {
        List<State> result = new ArrayList<State>();
        collectState(result, start);
        return result;
    }

    private void collectStates(Collection<State> result, State s) {
        if (result.contains(s)) return;
        result.add(s);
        for (State next : s.getAllTargets())
            collectStates(result, next);
    }

class State...
    Collection<State> getAllTargets() {
        List<State> result = new ArrayList<State>();
        for (Transtion t : transitions.values())
            result.add(t.getTarget());
        return result;
    }
```

为了重置事件，在状态机上保存了一个列表。

```java
class StateMachine...
    private List<Event> resetEvents = new ArrayList<Event>();

    public void addResetEvents(Event... events) {
        for (Event e : events) 
            resetEvents.add(e);
    }
```

像这样用一个单独结构处理重置事并不是必需的。简单🉐地在状态机上声明一些额外的转换，也可以处理这种情况，如下所示：

```java
class StateMachine...
    private void addResetEvent_byAddingTransitions(Event e) {
        for (State s : getStates()) 
            if (!s.hasTransition(e.getCode(), s.addTransition(e,start)));
    }
```

倾向于在状态机上设置显式的重置事件，这样可以更好地表现意图。虽然这样做确实使状态机有点复杂，但它也更加清晰地表现出通用状态机该如何运作，要定义特定状态机也会更加清晰。

处理完结构，再来看看行为。事实证明，这真的相当简单。控制器有个handle方法，它以从设备接收到的事件代码为参数。

```java
class Controller...{
    private State currentState;
    private StateMachine machine;

    public CommandChannel getCommandChannel() {
        return commandsChannel;
    }

    private CommandChannel commandsChannel;

    public void handle(String eventCode) {
        if (currentState.hasTransition(eventCode))
            transitionTo(currentState.targetState(eventCode));
        else if (machine.isResetEvent(eventCode))
            transitionTo(machine.getStart());
        // ignore unknown events
    }

    private void trasitionTo(State target) {
        currentState = target;
        currentState.executeActions(commandsChannel);
    }
}
class State {
    public boolean hasTransition(String eventCode) {
        return transtions.containsKey(eventCode);
    }

    public State targetState(String eventCode) {
        return transtions.get(eventCode).getTarget();
    }

    public void executeActions(CommandChannel commandsChannel) {
        for (Command c : actions) commandChannel.send(c.getCode());
    }
}
class StateMachine... {
    public boolean isResetEvent(String eventCode) {
        return resetEventCode().contains(eventCode);
    }
}
```

对于未在状态上注册的事件，它会直接忽略。对于可识别的任何事件，它就会转换为目标状态，并执行这个目标状态上定义的命令。

### 1.3 控制器编写程序

<!-- DSL看到了第33页-->

### 1.3 为格兰特小姐的控制器编写程序

### 1.4 语言和语义模型

### 1.5 使用代码生成

### 1.6 使用语言工作台

### 1.7 可视化

## 第2章 使用DSL

### 2.1 定义DSL

### 2.2 为何需要DSL

### 2.3 DSL的问题

### 2.4 广义的语言处理

### 2.5 DSL的生命周期

### 2.6 设计优良的DSL从何而来

## 第3章 实现DSL

### 3.1 DSL处理之架构

### 3.2 解析器的工作方式

### 3.3 文法，语法和语义

### 3.4 解析中的数据

### 3.5 宏

### 3.6 测试DSL

### 3.7 错误处理

### 3.8 DSL迁移

## 第4章 实现内部DSL

### 4.1 连贯API与命令 - 查询API

### 4.2 解析层的需求

### 4.3 使用函数

### 4.4 字面量集合

### 4.5 基于文法选择内部元素

### 4.6 闭包

### 4.7 解析树操作

### 4.8 标注

### 4.9 为字面量提供扩展

### 4.10 消除语法噪音

### 4.11 动态接收

### 4.12 提供类型检查

## 第5章 实现外部DSL

### 5.1 语法分析策略

### 5.2 输出生成策略

### 5.3 解析中的概念

### 5.4 混入另一种语言

### 5.5 XML DSL

## 第6章 内部DSL vs 外部DSL

### 6.1 学习曲线

### 6.2 创建成本

### 6.3 程序员的熟悉度

### 6.4 与领域专家沟通

### 6.5 与宿主语言混合

### 6.6 强边界

### 6.7 运行时配置

### 6.8 趋于平庸

### 6.9 组合多种DSL

### 6.10 总结

## 第7章 其他计算模型概述

### 7.1 几种计算模型

#### 7.1.1 决策表

#### 7.1.2 产生式规则系统

#### 7.1.3 状态机

#### 7.1.4 依赖网络

#### 7.1.5 选择模型

## 第8章 代码生成

### 8.1 选择生成什么

### 8.2 如何生成

### 8.3 混合生成代码和手写代码

### 8.4 生成可读的代码

### 8.5 解析之前的代码生成

### 8.6 延伸阅读

## 第9章 语言工作台

### 9.1 语言工作台之要素

### 9.2 模式定义语言和元模型

### 9.3 源码编辑和投射编辑

### 9.4 说明性编程

### 9.5 工具之旅

### 9.6 语言工作台和CASE工具

### 9.7 该使用语言工作台嘛

# 第二部分

## 第10章 各种DSL

### 10.1 Graphviz

### 10.2 JMock

### 10.3 CSS

### 10.4 HQL

### 10.5 XAML

### 10.6 FIT

### 10.7 Make

## 第11章 语义模型

### 11.1 工作模型

### 11.2 使用场景

### 11.3 入门例子（Java）

## 第12章 符号表

### 12.1 工作原理

### 12.2 使用场景

### 12.3 参考文献

### 12.4 以外部DSL实现的依赖网络（Java和ANTLR）

### 12.5 在一个内部DSL中使用符号键（Ruby）

### 12.6 用枚举作为静态类型符号（Java）

## 第13章 语境变量

### 13.1 工作原理

### 13.2 使用场景

### 13.3 读取INI文件（C#）

## 第14章 构造器生成器

### 14.1 工作原理

### 14.2 使用场景

### 14.3 构建简单的航班信息(C#)

## 第15章 宏

### 15.1 工作原理

### 文本宏

### 语法宏

### 15.2 使用场景

## 第16章 通知

### 16.1 工作原理

### 16.2 使用场景

### 16.3 一个非常简单的通知（C#）

### 16.4 解析中的通知（Java）

## 第17章 分隔符指导翻译

### 17.1 工作原理

### 17.2 使用场景
 
### 17.3 常客记分（C#）

### 17.4 

# 第三部分

## 第18章 语法指导翻译

### 18.1 工作原理

#### 词法分析器

#### 语法分析器

#### 产生输出

#### 语义预测

### 18.2 使用场景

## 第19章 BNF

### 19.1 工作原理

### 19.2 使用场景

## 第20章 基于正则表达式表的词法分析器

### 20.1 工作原理

### 20.2 使用场景

## 第21章 递归下降法语法解析器

### 21.1 工作原理

### 21.2 使用场景

### 21.3 递归下降

## 第22章 解析器组合子

### 22.1 工作原理

### 22.2 使用场景

## 第23章 解析器生成器

### 23.1 工作原理

### 23.2 使用场景

### 23.3 Hello World（Java和ANTLR）

#### 编写基本的文法

#### 构建语法分析器

#### 为文法添加代码动作

#### 使用代沟

## 第24章 树的构建

### 24.1 工作原理

### 24.2 使用场景

### 24.3 使用ANTLR的树构建语法（Java和ANTLR）

#### 标记解释

#### 解析

#### 组装语义模型

### 24.4 使用代码动作进行树的构建

## 第25章 嵌入式语法翻译

### 25.1 工作原理

### 25.2 使用场景

## 第26章 内嵌解释器

### 26.1 工作原理

### 26.2 使用场景

### 26.3 计算器

## 第27章 外加代码

### 27.1 工作原理

### 27.2 使用场景

### 27.3 嵌入动态代码

#### 语义模型

#### 语法分析器

## 第28章 可变分词方式

### 28.1 工作原理

#### 字符引用

#### 词法状态

#### 修改标记类型

#### 忽略标记类型

### 28.2 使用场景

## 第29章 嵌套的运算符表达式

### 29.1 工作原理

#### 使用自底向上的语法分析器

#### 自顶向下的语法分析器

### 29.2 使用场景

## 第30章 以换行符作为分隔符

### 30.1 工作原理

### 30.2 使用场景

## 第31章 外部DSL

### 31.1 语法缩进

### 31.2 模块化文法

# 第四部分

## 第32章 表达式生成器

### 32.1 工作原理

### 32.2 使用场景

### 32.3 具有和没有生成器的连贯日历

### 32.4 对于日历使用多个生成器

## 第33章 函数序列

### 33.1 工作序列

### 33.2 使用场景

### 33.3 简单的计算机配置

## 第34章 嵌套函数

### 34.1 工作原理

### 34.2 使用场景

### 34.3 简单计算机配置范例

### 34.4 用标记处理多个不同的参数

### 34.5 针对IDE支持使用子类型标记

### 34.6 使用对象初始化器

### 34.7 周期性事件

## 第35章 方法级联

### 35.1 工作原理

#### 生成器还是值

#### 收尾问题

#### 分层结构

#### 渐进式接口

### 35.2 使用场景

### 35.3 简单的计算机配置范例

### 35.4 带有属性的方法级联

### 35.5 渐进式接口

## 第36章 对象范围

### 36.1 工作原理

### 36.2 使用场景

### 36.3 安全代码

#### 语义模型

#### DSL

### 36.4 使用实例求值

### 36.5 使用实例初始化器

## 第37章 闭包

### 37.1 工作原理

### 37.2 使用场景

## 第38章 嵌套闭包

### 38.1 工作原理

### 38.2 使用场景

### 38.3 用嵌套闭包来包装函数序列

### 38.4 简单的C#示例

### 38.5 使用方法级联

### 38.6 带显式闭包参数的函数序列

### 38.7 采用实例级求值

## 第39章 列表的字面构造

### 39.1 工作原理

### 39.2 使用场景

## 第40章 Literal Map

### 40.1 工作原理

### 40.2 使用场景

### 40.3 使用List和Map表达式计算机的配置信息

### 40.4 演化为Greenspun式

## 第41章 动态接收

### 41.1 工作原理

### 41.2 使用场景

### 41.3 积分-使用方法名解析

#### 模型

#### 生成器

### 41.4 积分-使用方法级联

#### 模型

#### 生成器

## 第42章 标注

### 42.1 工作原理

#### 定义标注

#### 处理标注

### 42.2 使用场景

### 42.3 用于运行时处理的特定语法

### 42.4 使用类方法

### 42.5 动态代码生成

## 第43章 解析树操作

### 43.1 工作原理

### 43.2 使用场景

### 43.3 由C#条件生成IMAP查询

#### 语义模型

#### 以C#构建

#### 退后一步

## 第44章 类符号表

### 44.1 工作原理

### 44.2 使用场景

### 44.3 在静态类型中实现类符号表

## 第45章 本文润色

### 45.1 工作原理

### 45.2 使用场景

### 45.3 使用润色的折扣规则

## 第46章 为字面量提供扩展

### 46.1 工作原理

### 46.2 使用场景

### 46.3 食谱配料

# 第五部分 其他计算模型

## 第47章 适应性模型

### 47.1 工作原理

#### 在适应性模型中使用命令式代码

#### 工具

### 使用场景

## 第48章 决策表

### 48.1 工作原理

### 48.2 使用场景

### 48.3 为一个订单计算费用

#### 模型

#### 解析器

## 第49章 依赖网络

### 49.1 工作原理

### 49.2 使用场景

### 49.3 分析饮料

## 第50章 产生式规则系统

### 50.1 工作原理

#### 链式操作

#### 矛盾推导

#### 规则结构里的模式

### 50.2 使用场景

### 50.3 俱乐部会员校验

#### 模型

#### 解析器

#### 演进DSL

### 50.4 适任资格的规则：扩展俱乐部成员

#### 模型

#### 解析器

## 第51章 状态机

### 51.1 工作原理

### 51.2 使用场景

### 51.3 安全面板控制器

## 第52章 基于转换器的代码生成

### 52.1 工作原理

### 52.2 使用场景

### 52.3 安全面板控制器

## 第53章 模版化的生成器

### 53.1 工作原理

### 53.2 使用场景

### 53.3 生成带有嵌套条件的安全控制面板状态机

## 第54章 嵌入助手

### 54.1 工作原理

### 54.2 使用场景

### 54.3 安全控制面板的状态

### 54.4 助手类应该生成HTML吗

## 第55章 基于模型的代码生成

### 55.1 工作原理

### 55.2 使用场景

### 55.3 安全控制面板的状态机

### 55.4 动态载入状态机

## 第56章 无视类型的代码生成

### 56.1 工作原理

### 56.2 使用场景

### 56.3 使用嵌套条件的安全面板状态机

## 第57章 代沟

### 57.1 工作原理

### 57.2 使用场景

### 57.3 根据数据结构生成类
