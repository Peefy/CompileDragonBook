
## DSL 领域设计语言

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

## OPA GO
