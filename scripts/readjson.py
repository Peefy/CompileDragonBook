
import json

# TODO：MyClass
class MyClass:
    def __init__(self):
        pass

    @staticmethod
    def funcname(**arg):
        pass

    def __prifunc(self):
        pass

def _prifunc():
    pass

if __name__ == "__main__":
    with open('./scripts/ast.json') as fd:
        obj = json.load(fd)
        print(obj)
    with open('./scripts/lefttree.json') as fd:
        obj = json.load(fd)
        print(obj)
    with open('./scripts/righttree.json') as fd:
        obj = json.load(fd)
        print(obj)

