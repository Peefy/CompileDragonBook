
DSL 领域设计语言

https://github.com/open-policy-agent/opa   --opa，一个统一policy的方案，自带了json like dsl和controller
https://github.com/open-policy-agent/opa/tree/master/wasm  --目前opa会把dsl的执行计划通过wasm打包，然后让controller调用wasm程序做判断
https://github.com/open-policy-agent/gatekeeper --统一controller的部分，一个基于k8s的operator
https://github.com/python/cpython -- python interpreter的官方实现
https://github.com/go-python/gpython  -- 一个社区的golang实现

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
