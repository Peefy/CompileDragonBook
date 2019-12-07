#定义变量，使用变量:$(变量名)
CC=g++
#定义变量srcs，表示需要编译的源文件，需要表明路径，如果直接写表示这些cpp文件和makefile在同一个目录下，如果有多个源文件，每行以\结尾
SRCS=./src/main.cpp\
        ./src/smartpointer.cpp\
		./src/stack.cpp\
		./src/demo.cpp
#定义变量OBJS,表示将原文件中所有以.cpp结尾的文件替换成以.o结尾，即将.cpp源文件编译成.o文件
OBJS=$(SRCS:.cpp=.o)

#
 
#定义变量，表示最终生成的可执行文件名
EXEC=maincpp
all:start run 
#start，表示开始执行，冒号后面的$(OBJS)表示要生成最终可执行文件，需要依赖那些.o文件的
start:$(OBJS)
        #相当于执行：g++ -o maincpp .o文件列表，-o表示生成的目标文件
		$(CC) -o $(EXEC) $(OBJS)
#表示我的.o文件来自于.cpp文件
.cpp.o:
        #如果在依赖关系中，有多个需要编译的.cpp文件，那么这个语句就需要执行多次。-c $<指的是需要编译的.cpp文件,-o $@指这个cpp文件编译后的中间文件名。比如在依赖关系中，有a.cpp和b.cpp，即$(OBJS)的值为a.cpp b.cpp，那么这条语句需要执行2次，第一次的$@为a.o,$<为a.cpp，第二次的$@为b.o,$<为b.cpp。-c表示只编译不链接，-o表示生成的目标文件
        #-DMYLINUX:-D为参数，MYLINUX为在cpp源文件中定义的宏名称，如#ifdef MYLINUX。注意-D和宏名称中间没有空格
		$(CC) -o $@ -c $< -DMYLINUX
#执行make clean指令
.PHONY:clean
clean:
		-rm -rf $(OBJS)
        #执行make clean指令时，需要执行的操作，比如下面的指令时指删除所有.o文件
run:
	./$(EXEC)
