
## DSL 领域设计语言

领域特定语言（英语：domain-specific language、DSL）指的是专注于某个应用程序领域的计算机语言。又译作领域专用语言。

在定义DSL是什么的问题上，Fowler 认为业界经常使用的一些特征，例如“关注于领域”、“有限的表现”和“语言本质”是非常模糊的。因此，唯一能够确定DSL边界的方法是考虑“一门语言的一种特定用法”和“该语言的设计者或使用者的意图”：
如果XSLT的设计者将其设计为XML的转换工具，那么我认为XSLT是一个DSL。如果一个用户使用DSL的目的是该DSL所要达到的目的，那么它是一个DSL，但是如果有人以通用的方式来使用一个DSL，那么它（在这种用法下）就不再是一个DSL了

域特定语言（英语：domain-specific language、DSL）指的是专注于某个应用程序领域的计算机语言。又译作领域专用语言。不同于普通的跨领域通用计算机语言(GPL)，领域特定语言只用在某些特定的领域。 比如用来显示网页的HTML，以及Emacs所使用的Emac LISP语言。

[-opa，一个统一policy的方案，自带了json like dsl和controller](https://github.com/open-policy-agent/opa) 

[目前opa会把dsl的执行计划通过wasm打包，然后让controller调用wasm程序做判断](https://github.com/open-policy-agent/opa/tree/master/wasm)  

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
