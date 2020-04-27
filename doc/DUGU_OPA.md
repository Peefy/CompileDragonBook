

## DuGu OPA

[-opa，一个统一policy的方案，自带了json like dsl和controller](https://github.com/open-policy-agent/opa) 

[目前opa会把dsl的执行计划(policies)通过wasm打包编译为WebAssembly，然后让controller调用wasm程序做判断](https://github.com/open-policy-agent/opa/tree/master/wasm)  

[统一controller的部分，一个基于k8s的operator](https://github.com/open-policy-agent/gatekeeper)

[python interpreter的官方实现](https://github.com/python/cpython)

[一个社区的go实现的python解释器](https://github.com/go-python/gpython)  

```py
def resource_assert(deployment, assert_log):
    if deployment.spec.relicas < 3:
        assert_log("prod deployment should have at least 3 replicas.")

def replica_validate():
    return ResourceAssertPolicy(
        name="minimum-relica-valiadate",
        description="Three replicas at least in prod.",
        resourceAssertion=[
            ResourceAssertion(Deployment, resource_assert)
        ]
    )

Policies("resource-rules", {[
    replica_validate(),
]})
```

## OPA

开放策略代理（Open Policy Agent, OPA）是一个开放源代码的通用策略引擎，可在整个堆栈中实施统一的，基于上下文的策略。

### OPA的工作方式

OPA提供了一种高级声明性语言，以在整个堆栈中编写和实施策略。

使用OPA，可以定义规则来控制系统的行为。这些规则可以回答以下问题：

* 用户X可以调用资源Z上的操作Y吗？
* 应将工作负载W部署到哪些群集？
* 创建资源R之前，必须在资源R上设置哪些标签？

将服务与OPA集成在一起，因此不必在服务中对这些类型的策略决策进行硬编码。服务在需要策略决策时通过执行查询与OPA集成。

当向OPA查询策略决策时，OPA会评估规则和数据以得出答案。该策略决策作为查询结果发送回。

例如，在一个简单的API授权用例中：

* 编写允许（或拒绝）访问您的服务API的规则。
* 的服务在收到API请求时查询OPA。
* OPA退货允许（或拒绝）您的服务决策。
* 服务通过相应地接受或拒绝请求来执行决策。

### OPA与其他容器或者集群集成的案例（docker，k8s等）

例子

顶层json声明语言`data.json`

```json
{
    "management_chain": {
        "bob": [
            "ken",
            "janet"
        ],
        "alice": [
            "janet"
        ]
    }
}
```

使用

```cmd
opa run data.json
```

## CPython的类OPA实现系统架构

1. 编写python函数或者python语言子集函数function并调用dugu_py_opa包中的函数完成策略编写和断言（通过给用户编写的包或者如VS Code、Vim、Atom等编辑器插件完成开发，一般主流的编译器都提供插件开发支持）。
2. 使用python解释器自带的AST抽象语法树包完成**词法**和**语法**翻译等，生成如等json结构的抽象语法树，完成python DSL->中间语言的功能。
3. 调用生成器将生成的中间语言翻译成如go语言相关的词法语法和包调用，并生成命令行程序供其他程序调用或者直接嵌入到docker或者k8s容器中，由容器的go解释器完成对生成的命令行应用运行

## CPython如何通过反射机制来扩展语言

### CPython装饰器实现原理
