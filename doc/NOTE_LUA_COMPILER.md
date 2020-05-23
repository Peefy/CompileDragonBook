
# Lua 编译器源码笔记

主要包括了词法分析器，语法分析器，虚拟机(runtime，堆栈结构，操作码)，垃圾回收器，代码生成器以及Lua对象，Lua函数库，Lua调试器等。其中运行时Runtime的垃圾回收器GC，虚拟机VM，语法解析器Parser，代码生成器Code Generator的工程量最大。

## Lua 主函数和命令行程序

### Lua 全局状态

以下为`lstate.c`和`lstate.h`

有关Lua垃圾收集对象的一些注意事项：Lua中的所有对象在释放之前必须保持某种方式可访问，因此所有对象始终属于这些列表中的一个（并且只有一个），使用'CommonHeader'的'next'字段链接：
* 'allgc'：所有未标记为完成的对象；
* 'finobj'：标记为完成的所有对象；
* 'tobefnz'：所有准备完成的对象；
* 'fixedgc'：不收集的所有对象（当前只有小字符串，例如保留字）。

对于分代回收器，其中一些列表具有世代相传的标记。每个标记指向该特定世代列表中的第一个元素；那一代人直到下一个标记。
* 'allgc'->'survival'：新对象；
* 'survival'->'old'：在一个集合中生存的对象；
* 'old'->'reallyold'：在上一个集合中变旧的对象；
* 'reallyold'-> NULL：对象已存在多个周期。
* 'finobj'->'finobjsur'：标记为完成的新对象；
* 'finobjsur'->'finobjold'：幸存的“”“”;
* 'finobjold'->'finobjrold'：只是旧的“”“”;
* 'finobjrold'-> NULL：确实很旧的“”“”。

此外，还有另一组控制灰色对象的列表。这些列表由“ gclist”字段链接。 （所有可能变为灰色的对象都具有这样的字段。该字段在所有对象中都不相同，但始终具有此名称。）任何灰色对象都必须属于这些列表之一，并且这些列表中的所有对象都必须为灰色：
* “gray”：常规的灰色物体，仍在等待访问。
* 'grayagain'：必须在原子阶段重新访问的对象。
* 那包含着
  - 黑色物体进入写入障碍；
  - 传播阶段的各种弱表；
  - 所有线程。
* 'weak'：需要清除弱值的表；
* 'ephemeron'：具有白色->白色条目的星历表；
* 'allweak'：要清除具有弱键和/或弱值的表。

关于“ nCcalls”：Lua（lua_State）中的每个线程都会计数在C堆栈中仍可以执行的“ C调用”次数，以避免C堆栈溢出。此计数非常近似。它只考虑解释器内部的递归函数，因为可以使用固定量（尽管未知）的堆栈空间来考虑非递归调用。

计数分为两部分：下半部分是计数本身。较高的部分计算堆栈中不可产生的调用数。 （它们在一起，以便我们可以用一条指令更改两者。）

由于对外部C函数的调用会占用未知数量的空间（例如，使用辅助缓冲区的函数），因此对这些函数的调用会使计数加一。

适当的计数不包括Lua分配的CallInfo结构的数目，这是一种“潜在”的调用。因此，当Lua调用一个函数（并“消费”一个CallInfo）时，它既不需要递减也不需要检查“ nCcalls”，因为已经考虑到了其对C堆栈的使用。

函数调用信息

```cpp
/*
** Information about a call. 调用信息
*/
typedef struct CallInfo {
  StkId func;  /* function index in the stack 栈中的函数索引*/ 
  StkId	top;  /* top for this function 该调用函数的栈顶*/
  struct CallInfo *previous, *next;  /* dynamic call link 调用链表*/
  union {
    struct {  /* only for Lua functions */
      const Instruction *savedpc;
      l_signalT trap;
      int nextraargs;  /* # of extra arguments in vararg functions */
    } l;
    struct {  /* only for C functions */
      lua_KFunction k;  /* continuation in case of yields */
      ptrdiff_t old_errfunc;
      lua_KContext ctx;  /* context info. in case of yields */
    } c;
  } u;
  union {
    int funcidx;  /* called-function index 调用函数索引*/ 
    int nyield;  /* number of values yielded 返回数字*/
    struct {  /* info about transferred values (for call/return hooks) 有关转移值的信息（用于调用/返回钩子）*/
      unsigned short ftransfer;  /* offset of first value transferred 转移的第一个值的偏移量*/
      unsigned short ntransfer;  /* number of values transferred 传输值的数量*/
    } transferinfo;
  } u2;
  short nresults;  /* expected number of results from this function 此函数的预期结果数*/
  unsigned short callstatus;
} CallInfo;
```

所有线程共享的**全局状态**

```cpp
/*
** 'global state', shared by all threads of this state
“全局状态”，由该状态的所有线程共享
*/
typedef struct global_State {
  lua_Alloc frealloc;  /* function to reallocate memory 重新分配内存的函数*/
  void *ud;         /* auxiliary data to 'frealloc' 辅助数据对‘frealloc’*/
  l_mem totalbytes;  /* number of bytes currently allocated - GCdebt 当前分配的字节数-GCdebt*/
  l_mem GCdebt;  /* bytes allocated not yet compensated by the collector 分配的字节尚未由收集器补偿*/
  lu_mem GCestimate;  /* an estimate of the non-garbage memory in use 对正在使用的非垃圾内存的估计*/
  lu_mem lastatomic;  /* see function 'genstep' in file 'lgc.c' 参见文件'lgc.c'中的函数'genstep'*/
  stringtable strt;  /* hash table for strings 字符串的哈希表*/
  TValue l_registry;
  TValue nilvalue;  /* a nil value 一个nil空值*/
  unsigned int seed;  /* randomized seed for hashes 散列随机种子*/
  lu_byte currentwhite;
  lu_byte gcstate;  /* state of garbage collector 垃圾收集器的状态 */
  lu_byte gckind;  /* kind of GC running 运行的GC类型*/
  lu_byte genminormul;  /* control for minor generational collections 控制次世代集合*/
  lu_byte genmajormul;  /* control for major generational collections 控制主要的世代回收*/
  lu_byte gcrunning;  /* true if GC is running 如果GC正在运行，则为true*/
  lu_byte gcemergency;  /* true if this is an emergency collection 如果这是紧急收集，则为true*/
  lu_byte gcpause;  /* size of pause between successive GCs 连续GC之间的暂停大小*/
  lu_byte gcstepmul;  /* GC "speed" GC“速度”*/
  lu_byte gcstepsize;  /* (log2 of) GC granularity （粒度的log2）GC*/
  GCObject *allgc;  /* list of all collectable objects 所有可收集对象的列表*/
  GCObject **sweepgc;  /* current position of sweep in list 列表中当前扫描的位置*/
  GCObject *finobj;  /* list of collectable objects with finalizers 具有终结器的可收集对象列表*/
  GCObject *gray;  /* list of gray objects 灰色对象列表*/
  GCObject *grayagain;  /* list of objects to be traversed atomically 原子遍历的对象列表*/
  GCObject *weak;  /* list of tables with weak values 具有弱值的表的列表*/
  GCObject *ephemeron;  /* list of ephemeron tables (weak keys) 星历表列表（弱键）*/
  GCObject *allweak;  /* list of all-weak tables 所有弱表的列表*/
  GCObject *tobefnz;  /* list of userdata to be GC 要作为GC的用户数据列表*/
  GCObject *fixedgc;  /* list of objects not to be collected 不收集的对象列表*/
  /* fields for generational collector 世代收集器的字段*/
  GCObject *survival;  /* start of objects that survived one GC cycle 从一个GC周期中幸存下来的对象开始 */
  GCObject *old;  /* start of old objects 旧对象的开始*/
  GCObject *reallyold;  /* old objects with more than one cycle 超过一个周期的旧对象*/
  GCObject *finobjsur;  /* list of survival objects with finalizers 具有终结器的生存对象列表*/
  GCObject *finobjold;  /* list of old objects with finalizers 具有终结器的旧对象列表*/
  GCObject *finobjrold;  /* list of really old objects with finalizers 带有终结器的真正旧对象的列表*/
  struct lua_State *twups;  /* list of threads with open upvalues 具有开放upvalues的线程列表*/
  lua_CFunction panic;  /* to be called in unprotected errors 在未受保护的错误中被调用*/
  struct lua_State *mainthread; /* 主线程 */
  TString *memerrmsg;  /* message for memory-allocation errors 有关内存分配错误的消息 */
  TString *tmname[TM_N];  /* array with tag-method names 具有标记方法名称的数组 */
  struct Table *mt[LUA_NUMTAGS];  /* metatables for basic types 基本类型的元表 */
  TString *strcache[STRCACHE_N][STRCACHE_M];  /* cache for strings in API 缓存API中的字符串 */
  lua_WarnFunction warnf;  /* warning function 警告函数*/
  void *ud_warn;         /* auxiliary data to 'warnf' 警告函数的辅助函数*/
  unsigned int Cstacklimit;  /* current limit for the C stack C堆栈的当前限制*/
} global_State;
```

每个线程的状态

```cpp
/*
** 'per thread' state
每个线程的状态
*/
struct lua_State {
  CommonHeader;
  lu_byte status;  /* 状态 */
  lu_byte allowhook;  /* 钩子 */
  unsigned short nci;  /* number of items in 'ci' list ‘ci’列表中的项目数*/
  StkId top;  /* first free slot in the stack 堆栈中的第一个空闲插槽 */
  global_State *l_G;
  CallInfo *ci;  /* call info for current function 当前函数的调用信息*/
  const Instruction *oldpc;  /* last pc traced 追踪到最后一个PC*/
  StkId stack_last;  /* last free slot in the stack 堆栈中的最后一个空闲插槽*/
  StkId stack;  /* stack base 堆栈基础*/
  UpVal *openupval;  /* list of open upvalues in this stack 此堆栈中未清upvalue的列表*/
  GCObject *gclist; /* GC对象的链表 */
  struct lua_State *twups;  /* list of threads with open upvalues 具有开放upvalues的线程列表*/
  struct lua_longjmp *errorJmp;  /* current error recover point 当前错误恢复点*/
  CallInfo base_ci;  /* CallInfo for first level (C calling Lua) 一级CallInfo（C调用Lua） */
  volatile lua_Hook hook;
  ptrdiff_t errfunc;  /* current error handling function (stack index) 当前的错误处理函数（堆栈索引） */
  l_uint32 nCcalls;  /* number of allowed nested C calls - 'nci' 允许的嵌套C调用数-'nci' */
  int stacksize;
  int basehookcount;
  int hookcount;
  l_signalT hookmask;
};
```

### Lua 主函数调用函数说明

以下为`lua.c`和`lua.h`

包含了**Lua配置文件**和**Lua辅助函数**的大多数功能，定义了Lua的大、小和发布版本号，发布版本数字，发布字符串等。

```cpp
#define LUA_VERSION_MAJOR	"5"  // Lua 大版本号
#define LUA_VERSION_MINOR	"4"  // Lua 小版本号
#define LUA_VERSION_RELEASE	"0" 

#define LUA_VERSION_NUM			504
#define LUA_VERSION_RELEASE_NUM		(LUA_VERSION_NUM * 100 + 0)

#define LUA_VERSION	"Lua " LUA_VERSION_MAJOR "." LUA_VERSION_MINOR
#define LUA_RELEASE	LUA_VERSION "." LUA_VERSION_RELEASE
#define LUA_COPYRIGHT	LUA_RELEASE "  Copyright (C) 1994-2019 Lua.org, PUC-Rio"
#define LUA_AUTHORS	"R. Ierusalimschy, L. H. de Figueiredo, W. Celes"
```

定义了Lua线程状态

```cpp
/* thread status */
/* 线程状态 */
#define LUA_OK		0
#define LUA_YIELD	1
#define LUA_ERRRUN	2
#define LUA_ERRSYNTAX	3
#define LUA_ERRMEM	4
#define LUA_ERRERR	5
```

定义了Lua基本类型索引和类型定义

```cpp
/*
** basic types
** 基本类型 
*/
#define LUA_TNONE		(-1)

#define LUA_TNIL		0
#define LUA_TBOOLEAN		1
#define LUA_TLIGHTUSERDATA	2
#define LUA_TNUMBER		3
#define LUA_TSTRING		4
#define LUA_TTABLE		5
#define LUA_TFUNCTION		6
#define LUA_TUSERDATA		7
#define LUA_TTHREAD		8

#define LUA_NUMTAGS		9
```

```cpp
/* type of numbers in Lua */
typedef LUA_NUMBER lua_Number;

/* type for integer functions */
typedef LUA_INTEGER lua_Integer;

/* unsigned integer type */
typedef LUA_UNSIGNED lua_Unsigned;

/* type for continuation-function contexts */
/* 延续功能上下文的类型 */
typedef LUA_KCONTEXT lua_KContext;
```

函数类型指针:

```cpp
/*
** Type for C functions registered with Lua
*/
typedef int (*lua_CFunction) (lua_State *L);

/*
** Type for continuation functions
*/
typedef int (*lua_KFunction) (lua_State *L, int status, lua_KContext ctx);

/*
** Type for functions that read/write blocks when loading/dumping Lua chunks
*/
typedef const char * (*lua_Reader) (lua_State *L, void *ud, size_t *sz);

typedef int (*lua_Writer) (lua_State *L, const void *p, size_t sz, void *ud);

/*
** Type for memory-allocation functions
*/
typedef void * (*lua_Alloc) (void *ud, void *ptr, size_t osize, size_t nsize);

/*
** Type for warning functions
*/
typedef void (*lua_WarnFunction) (void *ud, const char *msg, int tocont);
```

Lua状态操作API函数声明

```cpp
/*
** state manipulation
*/
LUA_API lua_State *lua_newstate (lua_Alloc f, void *ud);
LUA_API void       lua_close (lua_State *L);
LUA_API lua_State *lua_newthread (lua_State *L);
LUA_API int        lua_resetthread (lua_State *L);
LUA_API lua_CFunction lua_atpanic (lua_State *L, lua_CFunction panicf);
LUA_API lua_Number lua_version (lua_State *L);
```

堆栈操作API函数声明

```cpp
/*
** basic stack manipulation
** 基本堆栈操作
*/
LUA_API int   (lua_absindex) (lua_State *L, int idx);
LUA_API int   (lua_gettop) (lua_State *L);
LUA_API void  (lua_settop) (lua_State *L, int idx);
LUA_API void  (lua_pushvalue) (lua_State *L, int idx);
LUA_API void  (lua_rotate) (lua_State *L, int idx, int n);
LUA_API void  (lua_copy) (lua_State *L, int fromidx, int toidx);
LUA_API int   (lua_checkstack) (lua_State *L, int n);
LUA_API void  (lua_xmove) (lua_State *from, lua_State *to, int n);
```


访问函数声明（stack -> C）

```cpp
/*
** access functions (stack -> C)
*/

LUA_API int             (lua_isnumber) (lua_State *L, int idx);
LUA_API int             (lua_isstring) (lua_State *L, int idx);
LUA_API int             (lua_iscfunction) (lua_State *L, int idx);
LUA_API int             (lua_isinteger) (lua_State *L, int idx);
LUA_API int             (lua_isuserdata) (lua_State *L, int idx);
LUA_API int             (lua_type) (lua_State *L, int idx);
LUA_API const char     *(lua_typename) (lua_State *L, int tp);

LUA_API lua_Number      (lua_tonumberx) (lua_State *L, int idx, int *isnum);
LUA_API lua_Integer     (lua_tointegerx) (lua_State *L, int idx, int *isnum);
LUA_API int             (lua_toboolean) (lua_State *L, int idx);
LUA_API const char     *(lua_tolstring) (lua_State *L, int idx, size_t *len);
LUA_API lua_Unsigned    (lua_rawlen) (lua_State *L, int idx);
LUA_API lua_CFunction   (lua_tocfunction) (lua_State *L, int idx);
LUA_API void	       *(lua_touserdata) (lua_State *L, int idx);
LUA_API lua_State      *(lua_tothread) (lua_State *L, int idx);
LUA_API const void     *(lua_topointer) (lua_State *L, int idx);
```

比较和算术操作码

```cpp
#define LUA_OPADD	0	/* ORDER TM, ORDER OP */
#define LUA_OPSUB	1
#define LUA_OPMUL	2
#define LUA_OPMOD	3
#define LUA_OPPOW	4
#define LUA_OPDIV	5
#define LUA_OPIDIV	6
#define LUA_OPBAND	7
#define LUA_OPBOR	8
#define LUA_OPBXOR	9
#define LUA_OPSHL	10
#define LUA_OPSHR	11
#define LUA_OPUNM	12
#define LUA_OPBNOT	13

LUA_API void  (lua_arith) (lua_State *L, int op);

#define LUA_OPEQ	0
#define LUA_OPLT	1
#define LUA_OPLE	2

LUA_API int   (lua_rawequal) (lua_State *L, int idx1, int idx2);
LUA_API int   (lua_compare) (lua_State *L, int idx1, int idx2, int op);
```

堆栈函数声明

```cpp
/*
** push functions (C -> stack)
*/
LUA_API void        (lua_pushnil) (lua_State *L);
LUA_API void        (lua_pushnumber) (lua_State *L, lua_Number n);
LUA_API void        (lua_pushinteger) (lua_State *L, lua_Integer n);
LUA_API const char *(lua_pushlstring) (lua_State *L, const char *s, size_t len);
LUA_API const char *(lua_pushstring) (lua_State *L, const char *s);
LUA_API const char *(lua_pushvfstring) (lua_State *L, const char *fmt,
                                                      va_list argp);
LUA_API const char *(lua_pushfstring) (lua_State *L, const char *fmt, ...);
LUA_API void  (lua_pushcclosure) (lua_State *L, lua_CFunction fn, int n);
LUA_API void  (lua_pushboolean) (lua_State *L, int b);
LUA_API void  (lua_pushlightuserdata) (lua_State *L, void *p);
LUA_API int   (lua_pushthread) (lua_State *L);


/*
** get functions (Lua -> stack)
*/
LUA_API int (lua_getglobal) (lua_State *L, const char *name);
LUA_API int (lua_gettable) (lua_State *L, int idx);
LUA_API int (lua_getfield) (lua_State *L, int idx, const char *k);
LUA_API int (lua_geti) (lua_State *L, int idx, lua_Integer n);
LUA_API int (lua_rawget) (lua_State *L, int idx);
LUA_API int (lua_rawgeti) (lua_State *L, int idx, lua_Integer n);
LUA_API int (lua_rawgetp) (lua_State *L, int idx, const void *p);

LUA_API void  (lua_createtable) (lua_State *L, int narr, int nrec);
LUA_API void *(lua_newuserdatauv) (lua_State *L, size_t sz, int nuvalue);
LUA_API int   (lua_getmetatable) (lua_State *L, int objindex);
LUA_API int  (lua_getiuservalue) (lua_State *L, int idx, int n);
```

载入和调用函数声明

```cpp
LUA_API void  (lua_callk) (lua_State *L, int nargs, int nresults,
                           lua_KContext ctx, lua_KFunction k);
#define lua_call(L,n,r)		lua_callk(L, (n), (r), 0, NULL)

LUA_API int   (lua_pcallk) (lua_State *L, int nargs, int nresults, int errfunc,
                            lua_KContext ctx, lua_KFunction k);
#define lua_pcall(L,n,r,f)	lua_pcallk(L, (n), (r), (f), 0, NULL)

LUA_API int   (lua_load) (lua_State *L, lua_Reader reader, void *dt,
                          const char *chunkname, const char *mode);

LUA_API int (lua_dump) (lua_State *L, lua_Writer writer, void *data, int strip);
```

协程函数声明

```cpp
/*
** coroutine functions
*/
LUA_API int  (lua_yieldk)     (lua_State *L, int nresults, lua_KContext ctx,
                               lua_KFunction k);
LUA_API int  (lua_resume)     (lua_State *L, lua_State *from, int narg,
                               int *nres);
LUA_API int  (lua_status)     (lua_State *L);
LUA_API int (lua_isyieldable) (lua_State *L);

#define lua_yield(L,n)		lua_yieldk(L, (n), 0, NULL)
```

GC函数声明

```cpp
/*
** garbage-collection function and options
*/

#define LUA_GCSTOP		0
#define LUA_GCRESTART		1
#define LUA_GCCOLLECT		2
#define LUA_GCCOUNT		3
#define LUA_GCCOUNTB		4
#define LUA_GCSTEP		5
#define LUA_GCSETPAUSE		6
#define LUA_GCSETSTEPMUL	7
#define LUA_GCISRUNNING		9
#define LUA_GCGEN		10
#define LUA_GCINC		11

LUA_API int (lua_gc) (lua_State *L, int what, ...);
```

其他函数声明

```cpp
LUA_API int   (lua_error) (lua_State *L);

LUA_API int   (lua_next) (lua_State *L, int idx);

LUA_API void  (lua_concat) (lua_State *L, int n);
LUA_API void  (lua_len)    (lua_State *L, int idx);

LUA_API size_t   (lua_stringtonumber) (lua_State *L, const char *s);

LUA_API lua_Alloc (lua_getallocf) (lua_State *L, void **ud);
LUA_API void      (lua_setallocf) (lua_State *L, lua_Alloc f, void *ud);

LUA_API void  (lua_toclose) (lua_State *L, int idx);
```

一些有用的宏

```cpp

#define lua_getextraspace(L)	((void *)((char *)(L) - LUA_EXTRASPACE))

#define lua_tonumber(L,i)	lua_tonumberx(L,(i),NULL)
#define lua_tointeger(L,i)	lua_tointegerx(L,(i),NULL)

#define lua_pop(L,n)		lua_settop(L, -(n)-1)

#define lua_newtable(L)		lua_createtable(L, 0, 0)

#define lua_register(L,n,f) (lua_pushcfunction(L, (f)), lua_setglobal(L, (n)))

#define lua_pushcfunction(L,f)	lua_pushcclosure(L, (f), 0)

#define lua_isfunction(L,n)	(lua_type(L, (n)) == LUA_TFUNCTION)
#define lua_istable(L,n)	(lua_type(L, (n)) == LUA_TTABLE)
#define lua_islightuserdata(L,n)	(lua_type(L, (n)) == LUA_TLIGHTUSERDATA)
#define lua_isnil(L,n)		(lua_type(L, (n)) == LUA_TNIL)
#define lua_isboolean(L,n)	(lua_type(L, (n)) == LUA_TBOOLEAN)
#define lua_isthread(L,n)	(lua_type(L, (n)) == LUA_TTHREAD)
#define lua_isnone(L,n)		(lua_type(L, (n)) == LUA_TNONE)
#define lua_isnoneornil(L, n)	(lua_type(L, (n)) <= 0)

#define lua_pushliteral(L, s)	lua_pushstring(L, "" s)

#define lua_pushglobaltable(L)  \
	((void)lua_rawgeti(L, LUA_REGISTRYINDEX, LUA_RIDX_GLOBALS))

#define lua_tostring(L,i)	lua_tolstring(L, (i), NULL)


#define lua_insert(L,idx)	lua_rotate(L, (idx), 1)

#define lua_remove(L,idx)	(lua_rotate(L, (idx), -1), lua_pop(L, 1))

#define lua_replace(L,idx)	(lua_copy(L, -1, (idx)), lua_pop(L, 1))
```

兼容性宏

```cpp
#if defined(LUA_COMPAT_APIINTCASTS)

#define lua_pushunsigned(L,n)	lua_pushinteger(L, (lua_Integer)(n))
#define lua_tounsignedx(L,i,is)	((lua_Unsigned)lua_tointegerx(L,i,is))
#define lua_tounsigned(L,i)	lua_tounsignedx(L,(i),NULL)

#endif

#define lua_newuserdata(L,s)	lua_newuserdatauv(L,s,1)
#define lua_getuservalue(L,idx)	lua_getiuservalue(L,idx,1)
#define lua_setuservalue(L,idx)	lua_setiuservalue(L,idx,1)
```

事件函数声明

```cpp
/*
** Event codes
*/
#define LUA_HOOKCALL	0
#define LUA_HOOKRET	1
#define LUA_HOOKLINE	2
#define LUA_HOOKCOUNT	3
#define LUA_HOOKTAILCALL 4


/*
** Event masks
*/
#define LUA_MASKCALL	(1 << LUA_HOOKCALL)
#define LUA_MASKRET	(1 << LUA_HOOKRET)
#define LUA_MASKLINE	(1 << LUA_HOOKLINE)
#define LUA_MASKCOUNT	(1 << LUA_HOOKCOUNT)

typedef struct lua_Debug lua_Debug;  /* activation record */


/* Functions to be called by the debugger in specific events */
typedef void (*lua_Hook) (lua_State *L, lua_Debug *ar);


LUA_API int (lua_getstack) (lua_State *L, int level, lua_Debug *ar);
LUA_API int (lua_getinfo) (lua_State *L, const char *what, lua_Debug *ar);
LUA_API const char *(lua_getlocal) (lua_State *L, const lua_Debug *ar, int n);
LUA_API const char *(lua_setlocal) (lua_State *L, const lua_Debug *ar, int n);
LUA_API const char *(lua_getupvalue) (lua_State *L, int funcindex, int n);
LUA_API const char *(lua_setupvalue) (lua_State *L, int funcindex, int n);

LUA_API void *(lua_upvalueid) (lua_State *L, int fidx, int n);
LUA_API void  (lua_upvaluejoin) (lua_State *L, int fidx1, int n1,
                                               int fidx2, int n2);

LUA_API void (lua_sethook) (lua_State *L, lua_Hook func, int mask, int count);
LUA_API lua_Hook (lua_gethook) (lua_State *L);
LUA_API int (lua_gethookmask) (lua_State *L);
LUA_API int (lua_gethookcount) (lua_State *L);

LUA_API int (lua_setcstacklimit) (lua_State *L, unsigned int limit);
```

调试类型说明

```cpp
struct lua_Debug {
  int event;
  const char *name;	/* (n) */
  const char *namewhat;	/* (n) 'global', 'local', 'field', 'method' */
  const char *what;	/* (S) 'Lua', 'C', 'main', 'tail' */
  const char *source;	/* (S) */
  size_t srclen;	/* (S) */
  int currentline;	/* (l) */
  int linedefined;	/* (S) */
  int lastlinedefined;	/* (S) */
  unsigned char nups;	/* (u) number of upvalues */
  unsigned char nparams;/* (u) number of parameters */
  char isvararg;        /* (u) */
  char istailcall;	/* (t) */
  unsigned short ftransfer;   /* (r) index of first value transferred */
  unsigned short ntransfer;   /* (r) number of transferred values */
  char short_src[LUA_IDSIZE]; /* (S) */
  /* private part */
  struct CallInfo *i_ci;  /* active function */
};
```

### Lua 运行

全局状态定义

```cpp
static lua_State *globalL = NULL;
```

Lua主函数

```cpp
int main (int argc, char **argv) {
  int status, result;
  lua_State *L = luaL_newstate();  /* create state */  //创建状态机，一个堆栈模型
  if (L == NULL) {   // 内存分配失败，报告错误并退出
    l_message(argv[0], "cannot create state: not enough memory");
    return EXIT_FAILURE;
  }
  lua_pushcfunction(L, &pmain);  /* to call 'pmain' in protected mode */  // 将主函数地址压入栈中，索引0。在保护模式中调用pmain函数
  lua_pushinteger(L, argc);  /* 1st argument */       // 将命令行参数个数压入栈中，索引1
  lua_pushlightuserdata(L, argv); /* 2nd argument */  // 将命令行字符串数组头地址压入栈中，索引2
  status = lua_pcall(L, 2, 1, 0);  /* do the call */  // 调用栈中的主函数。
  result = lua_toboolean(L, -1);  /* get result */    // 获得调用结果
  report(L, status);                                  // 报告调用结果
  lua_close(L);                                       // 关闭状态机
  return (result && status == LUA_OK) ? EXIT_SUCCESS : EXIT_FAILURE;  // 返回错误码，0表示没错误，1表示有错误。
}
```

独立解释器的主体（在保护模式下调用）。读取并处理所有选项。

```cpp
/*
** Main body of stand-alone interpreter (to be called in protected mode).
** Reads the options and handles them all.
*/
static int pmain (lua_State *L) {
  int argc = (int)lua_tointeger(L, 1);       // 读取命令行数据个数
  char **argv = (char **)lua_touserdata(L, 2);  
  int script;
  int args = collectargs(argv, &script);   // 读取命令行参数
  luaL_checkversion(L);  /* check that interpreter has correct version */ // 自校验检查Lua解释器的版本
  if (argv[0] && argv[0][0]) progname = argv[0];   // 读取索引为0第一个的参数，一般为Lua
  if (args == has_error) {  /* bad arg? */         // 参数是否使用正确
    print_usage(argv[script]);  /* 'script' has index of bad arg. */  // 参数不正确，打印参数的正确的用法
    return 0;
  }
  if (args & has_v)  /* option '-v'? */  // 是否打印Lua版本
    print_version();
  if (args & has_E) {  /* option '-E'? */
    lua_pushboolean(L, 1);  /* signal for libraries to ignore env. vars. */
    lua_setfield(L, LUA_REGISTRYINDEX, "LUA_NOENV");
  }
  luaL_openlibs(L);  /* open standard libraries */  // Lua打开标准库
  createargtable(L, argv, argc, script);  /* create table 'arg' */ // 创建参数表
  lua_gc(L, LUA_GCGEN, 0, 0);  /* GC in generational mode */  // 设置Lua垃圾回收器
  if (!(args & has_E)) {  /* no option '-E'? */
    if (handle_luainit(L) != LUA_OK)  /* run LUA_INIT */    // 运行Lua初始化设置，包括读取解释文件等
      return 0;  /* error running LUA_INIT */
  }
  if (!runargs(L, argv, script))  /* execute arguments -e and -l */ // 执行参数-e和-l
    return 0;  /* something failed */
  if (script < argc &&  /* execute main script (if there is one) */ // 执行脚本
      handle_script(L, argv + script) != LUA_OK)
    return 0;
  if (args & has_i)  /* -i option? */
    doREPL(L);  /* do read-eval-print loop */   // 做读评估打印循环
  else if (script == argc && !(args & (has_e | has_v))) {  /* no arguments? */ // 命令行中只有Lua指令，执行交互模式
    if (lua_stdin_is_tty()) {  /* running in interactive mode? */  // 是否在交互模式下运行？
      print_version();
      doREPL(L);  /* do read-eval-print loop */  // 做读评估打印循环
    }
    else dofile(L, NULL);  /* executes stdin as a file */   // 将标准输入执行为文件
  }
  lua_pushboolean(L, 1);  /* signal no errors */  // 没有错误。
  return 1;
}
```

Lua解释器命令行用法

```cpp
static void print_usage (const char *badoption) {
  lua_writestringerror("%s: ", progname);
  if (badoption[1] == 'e' || badoption[1] == 'l')
    lua_writestringerror("'%s' needs argument\n", badoption);
  else
    lua_writestringerror("unrecognized option '%s'\n", badoption);
  lua_writestringerror(
  "usage: %s [options] [script [args]]\n"
  "Available options are:\n"
  "  -e stat  execute string 'stat'\n"
  "  -i       enter interactive mode after executing 'script'\n"
  "  -l name  require library 'name' into global 'name'\n"
  "  -v       show version information\n"
  "  -E       ignore environment variables\n"
  "  -W       turn warnings on\n"
  "  --       stop handling options\n"
  "  -        stop handling options and execute stdin\n"
  ,
  progname);
}
```

执行脚本的程序

```cpp
/* 
* 处理脚本
*/
static int handle_script (lua_State *L, char **argv) {
  int status;
  const char *fname = argv[0];
  if (strcmp(fname, "-") == 0 && strcmp(argv[-1], "--") != 0)
    fname = NULL;  /* stdin */  //校验文件名字
  status = luaL_loadfile(L, fname);  // 打开文件并执行parser,lexer等
  if (status == LUA_OK) {
    int n = pushargs(L);  /* push arguments to script */
    status = docall(L, n, LUA_MULTRET);
  }
  return report(L, status);
}
```

读完文件后载入

```cpp
LUA_API int lua_load (lua_State *L, lua_Reader reader, void *data,
                      const char *chunkname, const char *mode) {
  ZIO z;
  int status;
  lua_lock(L);
  if (!chunkname) chunkname = "?";
  luaZ_init(L, &z, reader, data);
  status = luaD_protectedparser(L, &z, chunkname, mode);
  if (status == LUA_OK) {  /* no errors? */
    LClosure *f = clLvalue(s2v(L->top - 1));  /* get newly created function */
    if (f->nupvalues >= 1) {  /* does it have an upvalue? */
      /* get global table from registry */
      Table *reg = hvalue(&G(L)->l_registry);
      const TValue *gt = luaH_getint(reg, LUA_RIDX_GLOBALS);
      /* set global table as 1st upvalue of 'f' (may be LUA_ENV) */
      setobj(L, f->upvals[0]->v, gt);
      luaC_barrier(L, f->upvals[0], gt);
    }
  }
  lua_unlock(L);
  return status;
}
```

启动受保护的语法分析器:

```cpp
int luaD_protectedparser (lua_State *L, ZIO *z, const char *name,
                                        const char *mode) {
  struct SParser p;
  int status;
  incnny(L);  /* cannot yield during parsing */
  p.z = z; p.name = name; p.mode = mode;
  p.dyd.actvar.arr = NULL; p.dyd.actvar.size = 0;
  p.dyd.gt.arr = NULL; p.dyd.gt.size = 0;
  p.dyd.label.arr = NULL; p.dyd.label.size = 0;
  luaZ_initbuffer(L, &p.buff);
  status = luaD_pcall(L, f_parser, &p, savestack(L, L->top), L->errfunc);
  luaZ_freebuffer(L, &p.buff);
  luaM_freearray(L, p.dyd.actvar.arr, p.dyd.actvar.size);
  luaM_freearray(L, p.dyd.gt.arr, p.dyd.gt.size);
  luaM_freearray(L, p.dyd.label.arr, p.dyd.label.size);
  decnny(L);
  return status;
}
```

```cpp
static void f_parser (lua_State *L, void *ud) {
  LClosure *cl;
  struct SParser *p = cast(struct SParser *, ud);
  int c = zgetc(p->z);  /* read first character */
  if (c == LUA_SIGNATURE[0]) {
    checkmode(L, p->mode, "binary");
    cl = luaU_undump(L, p->z, p->name);
  }
  else {
    checkmode(L, p->mode, "text");
    cl = luaY_parser(L, p->z, &p->buff, &p->dyd, p->name, c);
  }
  lua_assert(cl->nupvalues == cl->p->sizeupvalues);
  luaF_initupvals(L, cl);
}
```

```cpp
LClosure *luaY_parser (lua_State *L, ZIO *z, Mbuffer *buff,
                       Dyndata *dyd, const char *name, int firstchar) {
  LexState lexstate;  // 词法分析器
  FuncState funcstate;  // 函数状态
  LClosure *cl = luaF_newLclosure(L, 1);  /* create main closure */
  setclLvalue2s(L, L->top, cl);  /* anchor it (to avoid being collected) */
  luaD_inctop(L);    // 增加堆栈
  lexstate.h = luaH_new(L);  /* create table for scanner */
  sethvalue2s(L, L->top, lexstate.h);  /* anchor it */
  luaD_inctop(L);    // 增加堆栈
  funcstate.f = cl->p = luaF_newproto(L);
  funcstate.f->source = luaS_new(L, name);  /* create and anchor TString */
  luaC_objbarrier(L, funcstate.f, funcstate.f->source);
  lexstate.buff = buff;
  lexstate.dyd = dyd;
  dyd->actvar.n = dyd->gt.n = dyd->label.n = 0;
  luaX_setinput(L, &lexstate, z, funcstate.f->source, firstchar);
  mainfunc(&lexstate, &funcstate);
  lua_assert(!funcstate.prev && funcstate.nups == 1 && !lexstate.fs);
  /* all scopes should be correctly finished */
  lua_assert(dyd->actvar.n == 0 && dyd->gt.n == 0 && dyd->label.n == 0);
  L->top--;  /* remove scanner's table */
  return cl;  /* closure is on the stack, too */
}
```

```cpp
/*
** compiles the main function, which is a regular vararg function with an
** upvalue named LUA_ENV
**编译main函数，这是一个常规vararg函数，带有
**名为LUA_ENV的增值
*/
static void mainfunc (LexState *ls, FuncState *fs) {
  BlockCnt bl;
  Upvaldesc *env;
  open_func(ls, fs, &bl);
  setvararg(fs, 0);  /* main function is always declared vararg */
  env = allocupvalue(fs);  /* ...set environment upvalue */
  env->instack = 1;
  env->idx = 0;
  env->kind = VDKREG;
  env->name = ls->envn;
  luaX_next(ls);  /* read first token */  //读取第一个标记
  statlist(ls);  /* parse main body */ // 开始语法分析
  check(ls, TK_EOS);
  close_func(ls);
}
```

### Lua 初始化

以下为`linit.c`和`linit.h`

定义了打开所有**Lua 基本库**的函数，比如`io`库，`coroutine`和`utf8`库等。

如果将Lua嵌入程序中并且需要打开标准库，请在程序中调用luaL_openlibs。 如果需要不同的库集，请将此文件复制到项目中并进行编辑以适合的需求。

还可以预加载`preload`库，以便以后的`require`可以打开已经链接到应用程序的库。 为此，请执行以下代码：

```cpp
luaL_getsubtable（L，LUA_REGISTRYINDEX，LUA_PRELOAD_TABLE）;
lua_pushcfunction（L，luaopen_modname）;
lua_setfield（L，-2，modname）;
lua_pop（L，1）; //删除PRELOAD表x
```

```cpp
/*
** these libs are loaded by lua.c and are readily available to any Lua program
这些库是由lua.c加载的，并且可以被任何Lua程序使用
*/
static const luaL_Reg loadedlibs[] = {
  {LUA_GNAME, luaopen_base},
  {LUA_LOADLIBNAME, luaopen_package},
  {LUA_COLIBNAME, luaopen_coroutine},
  {LUA_TABLIBNAME, luaopen_table},
  {LUA_IOLIBNAME, luaopen_io},
  {LUA_OSLIBNAME, luaopen_os},
  {LUA_STRLIBNAME, luaopen_string},
  {LUA_MATHLIBNAME, luaopen_math},
  {LUA_UTF8LIBNAME, luaopen_utf8},
  {LUA_DBLIBNAME, luaopen_debug},
  {NULL, NULL}
};


LUALIB_API void luaL_openlibs (lua_State *L) {
  const luaL_Reg *lib;
  /* "require" functions from 'loadedlibs' and set results to global table */
  for (lib = loadedlibs; lib->func; lib++) {
    luaL_requiref(L, lib->name, lib->func, 1);
    lua_pop(L, 1);  /* remove lib */
  }
}
```

## Lua 配置文件

以下为`luaconf.h`

这里的某些定义可以通过编译器在外部进行更改（例如，使用`-D`选项）。 这些受`#if！defined`防护措施保护。 但是，应在此处直接更改其他几个定义，因为它们会影响Lua ABI（通过在此处进行更改，可以确保连接到Lua的所有软件（例如C库）都将使用相同的配置进行编译）； 或因为它们很少更改。

`LUAI_MAXCSTACK`定义了嵌套调用的最大深度，并且还限制了实现中其他递归算法（例如语法分析）的最大深度。 太大的值可能会使解释器崩溃（C堆栈溢出）。 对于常规计算机，默认值似乎可以，但是对于受限制的硬件，该值可能会太大。 测试文件“ cstack.lua”可能有助于找到一个好的限制。 （它将崩溃，并且限制过高。）

```cpp
#if !defined(LUAI_MAXCSTACK)
#define LUAI_MAXCSTACK		2000
#endif
```

`LUA_USE_C89`控制非ISO-C89功能的使用。 如果希望Lua避免在Windows上使用一些C99功能或Windows特定功能，请定义它。

```cpp
/* #define LUA_USE_C89 */
```

Lua的各平台定义

```cpp
/*
** By default, Lua on Windows use (some) specific Windows features
*/
#if !defined(LUA_USE_C89) && defined(_WIN32) && !defined(_WIN32_WCE)
#define LUA_USE_WINDOWS  /* enable goodies for regular Windows */
#endif


#if defined(LUA_USE_WINDOWS)
#define LUA_DL_DLL	/* enable support for DLL */
#define LUA_USE_C89	/* broadly, Windows is C89 */
#endif


#if defined(LUA_USE_LINUX)
#define LUA_USE_POSIX
#define LUA_USE_DLOPEN		/* needs an extra library: -ldl */
#endif


#if defined(LUA_USE_MACOSX)
#define LUA_USE_POSIX
#define LUA_USE_DLOPEN		/* MacOS does not need -ldl */
#endif
```

Lua基本类型数据定义

```cpp
/* predefined options for LUA_INT_TYPE */
#define LUA_INT_INT		1
#define LUA_INT_LONG		2
#define LUA_INT_LONGLONG	3

/* predefined options for LUA_FLOAT_TYPE */
#define LUA_FLOAT_FLOAT		1
#define LUA_FLOAT_DOUBLE	2
#define LUA_FLOAT_LONGDOUBLE	3

#if defined(LUA_32BITS)		/* { */
/*
** 32-bit integers and 'float'
*/
#if LUAI_IS32INT  /* use 'int' if big enough */
#define LUA_INT_TYPE	LUA_INT_INT
#else  /* otherwise use 'long' */
#define LUA_INT_TYPE	LUA_INT_LONG
#endif
#define LUA_FLOAT_TYPE	LUA_FLOAT_FLOAT

#elif defined(LUA_C89_NUMBERS)	/* }{ */
/*
** largest types available for C89 ('long' and 'double')
*/
#define LUA_INT_TYPE	LUA_INT_LONG
#define LUA_FLOAT_TYPE	LUA_FLOAT_DOUBLE

#endif				/* } */


/*
** default configuration for 64-bit Lua ('long long' and 'double')
*/
#if !defined(LUA_INT_TYPE)
#define LUA_INT_TYPE	LUA_INT_LONGLONG
#endif

#if !defined(LUA_FLOAT_TYPE)
#define LUA_FLOAT_TYPE	LUA_FLOAT_DOUBLE
#endif
```

Lua路径定义

```cpp
/*
** LUA_PATH_SEP is the character that separates templates in a path.
** LUA_PATH_MARK is the string that marks the substitution points in a
** template.
** LUA_EXEC_DIR in a Windows path is replaced by the executable's
** directory.
*/
#define LUA_PATH_SEP            ";"
#define LUA_PATH_MARK           "?"
#define LUA_EXEC_DIR            "!"

#define LUA_VDIR	LUA_VERSION_MAJOR "." LUA_VERSION_MINOR
#if defined(_WIN32)	/* { */
/*
** In Windows, any exclamation mark ('!') in the path is replaced by the
** path of the directory of the executable file of the current process.
*/
#define LUA_LDIR	"!\\lua\\"
#define LUA_CDIR	"!\\"
#define LUA_SHRDIR	"!\\..\\share\\lua\\" LUA_VDIR "\\"

#if !defined(LUA_PATH_DEFAULT)
#define LUA_PATH_DEFAULT  \
		LUA_LDIR"?.lua;"  LUA_LDIR"?\\init.lua;" \
		LUA_CDIR"?.lua;"  LUA_CDIR"?\\init.lua;" \
		LUA_SHRDIR"?.lua;" LUA_SHRDIR"?\\init.lua;" \
		".\\?.lua;" ".\\?\\init.lua"
#endif

#if !defined(LUA_CPATH_DEFAULT)
#define LUA_CPATH_DEFAULT \
		LUA_CDIR"?.dll;" \
		LUA_CDIR"..\\lib\\lua\\" LUA_VDIR "\\?.dll;" \
		LUA_CDIR"loadall.dll;" ".\\?.dll"
#endif

#else			/* }{ */

#define LUA_ROOT	"/usr/local/"
#define LUA_LDIR	LUA_ROOT "share/lua/" LUA_VDIR "/"
#define LUA_CDIR	LUA_ROOT "lib/lua/" LUA_VDIR "/"

#if !defined(LUA_PATH_DEFAULT)
#define LUA_PATH_DEFAULT  \
		LUA_LDIR"?.lua;"  LUA_LDIR"?/init.lua;" \
		LUA_CDIR"?.lua;"  LUA_CDIR"?/init.lua;" \
		"./?.lua;" "./?/init.lua"
#endif

#if !defined(LUA_CPATH_DEFAULT)
#define LUA_CPATH_DEFAULT \
		LUA_CDIR"?.so;" LUA_CDIR"loadall.so;" "./?.so"
#endif

#endif			/* } */

#if !defined(LUA_DIRSEP)

#if defined(_WIN32)
#define LUA_DIRSEP	"\\"
#else
#define LUA_DIRSEP	"/"
#endif

#endif
```

Lua C库导出

```cpp
#if defined(LUA_BUILD_AS_DLL)	/* { */

#if defined(LUA_CORE) || defined(LUA_LIB)	/* { */
#define LUA_API __declspec(dllexport)
#else						/* }{ */
#define LUA_API __declspec(dllimport)
#endif						/* } */

#else				/* }{ */

#define LUA_API		extern

#endif				/* } */


/*
** More often than not the libs go together with the core.
*/
#define LUALIB_API	LUA_API
#define LUAMOD_API	LUA_API

#if defined(__GNUC__) && ((__GNUC__*100 + __GNUC_MINOR__) >= 302) && \
    defined(__ELF__)		/* { */
#define LUAI_FUNC	__attribute__((visibility("internal"))) extern
#else				/* }{ */
#define LUAI_FUNC	extern
#endif				/* } */

#define LUAI_DDEC(dec)	LUAI_FUNC dec
#define LUAI_DDEF	/* empty */
```

Lua类型定义

```cpp
#define l_mathlim(n)		(DBL_##n)

#define LUAI_UACNUMBER	double

#define LUA_NUMBER_FRMLEN	""
#define LUA_NUMBER_FMT		"%.14g"

#define l_mathop(op)		op

#define lua_str2number(s,p)	strtod((s), (p))

#if defined(LLONG_MAX)		/* { */
/* use ISO C99 stuff */

#define LUA_INTEGER		long long
#define LUA_INTEGER_FRMLEN	"ll"

#define LUA_MAXINTEGER		LLONG_MAX
#define LUA_MININTEGER		LLONG_MIN

#define LUA_MAXUNSIGNED		ULLONG_MAX

#elif defined(LUA_USE_WINDOWS) /* }{ */
/* in Windows, can use specific Windows types */

#define LUA_INTEGER		__int64
#define LUA_INTEGER_FRMLEN	"I64"

#define LUA_MAXINTEGER		_I64_MAX
#define LUA_MININTEGER		_I64_MIN

#define LUA_MAXUNSIGNED		_UI64_MAX

#else				/* }{ */

#error "Compiler does not support 'long long'. Use option '-DLUA_32BITS' \
  or '-DLUA_C89_NUMBERS' (see file 'luaconf.h' for details)"

#endif				/* } */

#else				/* }{ */

#error "numeric integer type not defined"

#endif				/* } */
```

## Lua 词法分析器

以下为`llex.c`和`llex.h`

语义信息，词素，词法分析器和词法分析器接口定义

```cpp

// 256 + 1
#define FIRST_RESERVED	257


#if !defined(LUA_ENV)
#define LUA_ENV		"_ENV"
#endif


/*
* WARNING: if you change the order of this enumeration,
* grep "ORDER RESERVED"  保留关键字
*/
enum RESERVED {
  /* terminal symbols denoted by reserved words */
  // 保留关键字终结符
  TK_AND = FIRST_RESERVED, TK_BREAK,
  TK_DO, TK_ELSE, TK_ELSEIF, TK_END, TK_FALSE, TK_FOR, TK_FUNCTION,
  TK_GOTO, TK_IF, TK_IN, TK_LOCAL, TK_NIL, TK_NOT, TK_OR, TK_REPEAT,
  TK_RETURN, TK_THEN, TK_TRUE, TK_UNTIL, TK_WHILE,
  /* other terminal symbols */
  // 其他终结符
  TK_IDIV, TK_CONCAT, TK_DOTS, TK_EQ, TK_GE, TK_LE, TK_NE,
  TK_SHL, TK_SHR,
  TK_DBCOLON, TK_EOS,
  TK_FLT, TK_INT, TK_NAME, TK_STRING
};

/* number of reserved words */
#define NUM_RESERVED	(cast_int(TK_WHILE-FIRST_RESERVED + 1))

// 语义信息
typedef union {
  lua_Number r;
  lua_Integer i;
  TString *ts;
} SemInfo;  /* semantics information */

/* 标记 */
typedef struct Token {
  int token;
  SemInfo seminfo;
} Token;


/* state of the lexer plus state of the parser when shared by all
   functions 词法分析状态机*/
typedef struct LexState {
  int current;  /* current character (charint) */  // 当前的字符
  int linenumber;  /* input line counter */        // 当前的行号
  int lastline;  /* line of last token 'consumed' */ // 最后一个记号的行
  Token t;  /* current token */                    // 当前记号
  Token lookahead;  /* look ahead token */         // 下一个记号
  struct FuncState *fs;  /* current function (parser) */ // 当前函数(语法分析器)
  struct lua_State *L;                              // lua状态机
  ZIO *z;  /* input stream */                     // 输入字符/词素流
  Mbuffer *buff;  /* buffer for tokens */         // 缓冲流
  Table *h;  /* to avoid collection/reuse strings */  // 符号表
  struct Dyndata *dyd;  /* dynamic structures used by the parser */
  TString *source;  /* current source name */
  TString *envn;  /* environment variable name */  // 环境变量
} LexState;


LUAI_FUNC void luaX_init (lua_State *L);
LUAI_FUNC void luaX_setinput (lua_State *L, LexState *ls, ZIO *z,
                              TString *source, int firstchar);
LUAI_FUNC TString *luaX_newstring (LexState *ls, const char *str, size_t l);
LUAI_FUNC void luaX_next (LexState *ls);
LUAI_FUNC int luaX_lookahead (LexState *ls);
LUAI_FUNC l_noret luaX_syntaxerror (LexState *ls, const char *s);
LUAI_FUNC const char *luaX_token2str (LexState *ls, int token);
```

Lua保留关键字

```cpp
/* ORDER RESERVED */
// lua保留关键字
static const char *const luaX_tokens [] = {
    "and", "break", "do", "else", "elseif",
    "end", "false", "for", "function", "goto", "if",
    "in", "local", "nil", "not", "or", "repeat",
    "return", "then", "true", "until", "while",
    "//", "..", "...", "==", ">=", "<=", "~=",
    "<<", ">>", "::", "<eof>",
    "<number>", "<integer>", "<name>", "<string>"
};
```

检查单个字符

```cpp
/* 检查当前的字符是否是`c` */
static int check_next1 (LexState *ls, int c) {
  if (ls->current == c) {
    next(ls);
    return 1;
  }
  else return 0;
}


/*
** Check whether current char is in set 'set' (with two chars) and saves it
检查当前字符是否在集合`set`中（带有两个字符）并保存
*/
static int check_next2 (LexState *ls, const char *set) {
  lua_assert(set[2] == '\0');
  if (ls->current == set[0] || ls->current == set[1]) {
    save_and_next(ls);
    return 1;
  }
  else return 0;
}
```

判断是否是数字:

```cpp
/* LUA_NUMBER */
/*
** This function is quite liberal in what it accepts, as 'luaO_str2num' will reject ill-formed numerals. Roughly, it accepts the following pattern:
** 该函数在接受方面相当自由，因为“ luaO_str2num”将拒绝格式错误的数字。 大致来说，它接受以下模式：
**   %d(%x|%.|([Ee][+-]?))* | 0[Xx](%x|%.|([Pp][+-]?))*
**
** The only tricky part is to accept [+-] only after a valid exponent mark, to avoid reading '3-4' or '0xe+1' as a single number.
唯一棘手的部分是仅在有效指数标记后接受[+-]，以避免将'3-4'或'0xe + 1'读为单个数字。
**
** The caller might have already read an initial dot.
调用者可能已经读取了一个初始点。
*/
static int read_numeral (LexState *ls, SemInfo *seminfo) {
  TValue obj;
  const char *expo = "Ee";
  int first = ls->current;
  lua_assert(lisdigit(ls->current));
  save_and_next(ls);
  if (first == '0' && check_next2(ls, "xX"))  /* hexadecimal? 是否是16进制数字*/
    expo = "Pp";
  for (;;) {
    if (check_next2(ls, expo))  /* exponent mark?  指数标记？如果是16进制就置为Pp，不读取指数标记 */
      check_next2(ls, "-+");  /* optional exponent sign 判断是正指数还是负指数*/
    else if (lisxdigit(ls->current) || ls->current == '.')  /* '%x|%.' 判断浮点数的组成：数字或者小数点*/
      save_and_next(ls);
    else break;
  }
  if (lislalpha(ls->current))  /* is numeral touching a letter? 是否数字后紧接是一个字母*/
    save_and_next(ls);  /* force an error 强制一个错误*/
  save(ls, '\0'); /* 给其后加一个字符串结束符号 */
  if (luaO_str2num(luaZ_buffer(ls->buff), &obj) == 0)  /* format error? */
    lexerror(ls, "malformed number", TK_FLT);
  if (ttisinteger(&obj)) {
    seminfo->i = ivalue(&obj);
    return TK_INT;
  }
  else {
    lua_assert(ttisfloat(&obj));
    seminfo->r = fltvalue(&obj);
    return TK_FLT;
  }
}
```

将字符串转换为整数或者浮点数:

```cpp
size_t luaO_str2num (const char *s, TValue *o) {
  lua_Integer i; lua_Number n;
  const char *e;
  if ((e = l_str2int(s, &i)) != NULL) {  /* try as an integer */
    setivalue(o, i);
  }
  else if ((e = l_str2d(s, &n)) != NULL) {  /* else try as a float */
    setfltvalue(o, n);
  }
  else
    return 0;  /* conversion failed */
  return (e - s) + 1;  /* success; return string size */
}

#define MAXBY10		cast(lua_Unsigned, LUA_MAXINTEGER / 10)
#define MAXLASTD	cast_int(LUA_MAXINTEGER % 10)

static const char *l_str2int (const char *s, lua_Integer *result) {
  lua_Unsigned a = 0;
  int empty = 1;
  int neg;
  while (lisspace(cast_uchar(*s))) s++;  /* skip initial spaces */
  neg = isneg(&s);
  if (s[0] == '0' &&
      (s[1] == 'x' || s[1] == 'X')) {  /* hex? */
    s += 2;  /* skip '0x' */
    for (; lisxdigit(cast_uchar(*s)); s++) {
      a = a * 16 + luaO_hexavalue(*s);
      empty = 0;
    }
  }
  else {  /* decimal */
    for (; lisdigit(cast_uchar(*s)); s++) {
      int d = *s - '0';
      if (a >= MAXBY10 && (a > MAXBY10 || d > MAXLASTD + neg))  /* overflow? */
        return NULL;  /* do not accept it (as integer) */
      a = a * 10 + d;
      empty = 0;
    }
  }
  while (lisspace(cast_uchar(*s))) s++;  /* skip trailing spaces */
  if (empty || *s != '\0') return NULL;  /* something wrong in the numeral */
  else {
    *result = l_castU2S((neg) ? 0u - a : a);
    return s;
  }
}

/*
** Convert string 's' to a Lua number (put in 'result'). Return NULL
** on fail or the address of the ending '\0' on success.
** 'pmode' points to (and 'mode' contains) special things in the string:
** - 'x'/'X' means a hexadecimal numeral
** - 'n'/'N' means 'inf' or 'nan' (which should be rejected)
** - '.' just optimizes the search for the common case (nothing special)
** This function accepts both the current locale or a dot as the radix
** mark. If the conversion fails, it may mean number has a dot but
** locale accepts something else. In that case, the code copies 's'
** to a buffer (because 's' is read-only), changes the dot to the
** current locale radix mark, and tries to convert again.
**将字符串“ s”转换为Lua编号（输入“结果”）。 如果失败，则返回NULL；如果成功，则返回结尾“ \ 0”的地址。 'pmode'指向（并且'mode'包含）字符串中的特殊内容：
**-'x'/'X'表示十六进制数字
**-'n'/'N'表示'inf'或'nan'（应拒绝）
**-'.' 只是针对常见情况优化搜索（没什么特别的）
此函数接受当前语言环境或点作为基数标记。 如果转换失败，则可能意味着数字带有点，但语言环境接受其他内容。 在这种情况下，代码会将“s”复制到缓冲区（因为“s”是只读的），将点更改为当前的语言环境基数标记，然后尝试再次进行转换。
*/
static const char *l_str2d (const char *s, lua_Number *result) {
  const char *endptr;
  const char *pmode = strpbrk(s, ".xXnN");
  int mode = pmode ? ltolower(cast_uchar(*pmode)) : 0;
  if (mode == 'n')  /* reject 'inf' and 'nan' 不解析inf或者nan */
    return NULL;
  endptr = l_str2dloc(s, result, mode);  /* try to convert 尝试转换 */
  if (endptr == NULL) {  /* failed? may be a different locale 失败了 */
    char buff[L_MAXLENNUM + 1];
    const char *pdot = strchr(s, '.');
    if (strlen(s) > L_MAXLENNUM || pdot == NULL)
      return NULL;  /* string too long or no dot; fail */
    strcpy(buff, s);  /* copy string to buffer */
    buff[pdot - s] = lua_getlocaledecpoint();  /* correct decimal point */
    endptr = l_str2dloc(buff, result, mode);  /* try again */
    if (endptr != NULL)
      endptr = s + (endptr - buff);  /* make relative to 's' */
  }
  return endptr;
}
```

读取Lua字符串

```cpp
/* 读取字符串，注意区分转义字符与读取文件的转义字符 */
static void read_string (LexState *ls, int del, SemInfo *seminfo) {
  save_and_next(ls);  /* keep delimiter (for error messages) 保留定界符（用于错误消息） */
  while (ls->current != del) {
    switch (ls->current) {
      case EOZ:
        lexerror(ls, "unfinished string", TK_EOS);
        break;  /* to avoid warnings */
      case '\n':
      case '\r':
        lexerror(ls, "unfinished string", TK_STRING);
        break;  /* to avoid warnings */
      case '\\': {  /* escape sequences 转义字符  */
        int c;  /* final character to be saved */
        save_and_next(ls);  /* keep '\\' for error messages */
        switch (ls->current) {
          case 'a': c = '\a'; goto read_save;
          case 'b': c = '\b'; goto read_save;
          case 'f': c = '\f'; goto read_save;
          case 'n': c = '\n'; goto read_save;
          case 'r': c = '\r'; goto read_save;
          case 't': c = '\t'; goto read_save;
          case 'v': c = '\v'; goto read_save;
          case 'x': c = readhexaesc(ls); goto read_save;
          case 'u': utf8esc(ls);  goto no_save;
          case '\n': case '\r':
            inclinenumber(ls); c = '\n'; goto only_save;
          case '\\': case '\"': case '\'':
            c = ls->current; goto read_save;
          case EOZ: goto no_save;  /* will raise an error next loop */
          case 'z': {  /* zap following span of spaces */
            luaZ_buffremove(ls->buff, 1);  /* remove '\\' */
            next(ls);  /* skip the 'z' */
            while (lisspace(ls->current)) {
              if (currIsNewline(ls)) inclinenumber(ls);
              else next(ls);
            }
            goto no_save;
          }
          default: {
            esccheck(ls, lisdigit(ls->current), "invalid escape sequence");
            c = readdecesc(ls);  /* digital escape '\ddd' */
            goto only_save;
          }
        }
       read_save:
         next(ls);
         /* go through 穿过去继续执行 */
       only_save:
         luaZ_buffremove(ls->buff, 1);  /* remove '\\' */
         save(ls, c);
         /* go through */
       no_save: break;
      }
      default:
        save_and_next(ls);
    }
  }
  save_and_next(ls);  /* skip delimiter */
  seminfo->ts = luaX_newstring(ls, luaZ_buffer(ls->buff) + 1,
                                   luaZ_bufflen(ls->buff) - 2);
}
```

忽略注释

```cpp
/*
** reads a sequence '[=*[' or ']=*]', leaving the last bracket.
** If sequence is well formed, return its number of '='s + 2; otherwise,
** return 1 if there is no '='s or 0 otherwise (an unfinished '[==...').
** reads a sequence '[=*[' or ']=*]', leaving the last bracket.
** If sequence is well formed, return its number of '='s + 2; otherwise,
** return 1 if there is no '='s or 0 otherwise (an unfinished '[==...').
忽略注释
*/
static size_t skip_sep (LexState *ls) {
  size_t count = 0;
  int s = ls->current;
  lua_assert(s == '[' || s == ']');
  save_and_next(ls);
  while (ls->current == '=') {
    save_and_next(ls);
    count++;
  }
  return (ls->current == s) ? count + 2
         : (count == 0) ? 1
         : 0;
}
```

读取跨行注释Lua长字符串

```cpp
/* 读取Lua跨行注释长字符串，Lua特有的 */
static void read_long_string (LexState *ls, SemInfo *seminfo, size_t sep) {
  int line = ls->linenumber;  /* initial line (for error message) */
  save_and_next(ls);  /* skip 2nd '['  */
  if (currIsNewline(ls))  /* string starts with a newline? */
    inclinenumber(ls);  /* skip it */
  for (;;) {
    switch (ls->current) {
      case EOZ: {  /* error */
        const char *what = (seminfo ? "string" : "comment");
        const char *msg = luaO_pushfstring(ls->L,
                     "unfinished long %s (starting at line %d)", what, line);
        lexerror(ls, msg, TK_EOS);
        break;  /* to avoid warnings */
      }
      case ']': {
        if (skip_sep(ls) == sep) {
          save_and_next(ls);  /* skip 2nd ']' */
          goto endloop;
        }
        break;
      }
      case '\n': case '\r': {
        save(ls, '\n');
        inclinenumber(ls);
        if (!seminfo) luaZ_resetbuffer(ls->buff);  /* avoid wasting space */
        break;
      }
      default: {
        if (seminfo) save_and_next(ls);
        else next(ls);
      }
    }
  } endloop:
  if (seminfo)
    seminfo->ts = luaX_newstring(ls, luaZ_buffer(ls->buff) + sep,
                                     luaZ_bufflen(ls->buff) - 2 * sep);
}
```

词法分析器函数:主要是读取各种token，重点是注释，关键字，数字，字符串，标识符。praser最终结果通常是一个抽象语法树 AST,编译为计算机可以运行或者虚拟机可以运行的代码

```cpp
/* 词法分析器主函数 */
static int llex (LexState *ls, SemInfo *seminfo) {
  luaZ_resetbuffer(ls->buff);
  for (;;) {
    switch (ls->current) {
      case '\n': case '\r': {  /* line breaks 换行符 */
        inclinenumber(ls);  /* 增加行号 */
        break;
      }
      case ' ': case '\f': case '\t': case '\v': {  /* spaces 空白字符 */
        next(ls);
        break;
      }
      case '-': {  /* '-' or '--' (comment)  -- 是Lua注释 */
        next(ls);
        if (ls->current != '-') return '-';
        /* else is a comment */
        next(ls);
        if (ls->current == '[') {  /* long comment? 是否是长注释 */
          size_t sep = skip_sep(ls);
          luaZ_resetbuffer(ls->buff);  /* 'skip_sep' may dirty the buffer */
          if (sep >= 2) {
            read_long_string(ls, NULL, sep);  /* skip long comment  读取跨行长字符串 */
            luaZ_resetbuffer(ls->buff);  /* previous call may dirty the buff.  忽略掉注释的长字符串 */
            break;
          }
        }
        /* else short comment 如果是不跨行的短注释 */
        while (!currIsNewline(ls) && ls->current != EOZ)
          next(ls);  /* skip until end of line (or end of file) 那就一直读到行尾为止 */ 
        break;
      }
      case '[': {  /* long string or simply '[' 长字符串或者索引中括号 */
        size_t sep = skip_sep(ls);
        if (sep >= 2) {
          read_long_string(ls, seminfo, sep);
          return TK_STRING;
        }
        else if (sep == 0)  /* '[=...' missing second bracket? */
          lexerror(ls, "invalid long string delimiter", TK_STRING);
        return '[';
      }
      case '=': { /* 判断是一个=的赋值号还是两个=的是否相等符号 */
        next(ls);
        if (check_next1(ls, '=')) return TK_EQ;
        else return '=';
      }
      case '<': { /* `<` `<=` `<<` */
        next(ls);
        if (check_next1(ls, '=')) return TK_LE;
        else if (check_next1(ls, '<')) return TK_SHL;
        else return '<';
      }
      case '>': { /* `>` `>=`  `>>` */
        next(ls);
        if (check_next1(ls, '=')) return TK_GE;
        else if (check_next1(ls, '>')) return TK_SHR;
        else return '>';
      }
      case '/': {  /* `/` `//` */
        next(ls);
        if (check_next1(ls, '/')) return TK_IDIV;
        else return '/';
      }
      case '~': {  /* `~` `~=` */
        next(ls);
        if (check_next1(ls, '=')) return TK_NE;
        else return '~';
      }
      case ':': {  /* `:` `::` */
        next(ls);
        if (check_next1(ls, ':')) return TK_DBCOLON;
        else return ':';
      }
      case '"': case '\'': {  /* short literal strings 短字符串 */ 
        read_string(ls, ls->current, seminfo);
        return TK_STRING;
      }
      case '.': {  /* '.', '..', '...', or number  三个符号或者数字，浮点数字也可以以`.`开头 */
        save_and_next(ls);
        if (check_next1(ls, '.')) {
          if (check_next1(ls, '.'))
            return TK_DOTS;   /* '...' */
          else return TK_CONCAT;   /* '..' */
        }
        else if (!lisdigit(ls->current)) return '.';
        else return read_numeral(ls, seminfo);
      }
      case '0': case '1': case '2': case '3': case '4':
      case '5': case '6': case '7': case '8': case '9': {
        return read_numeral(ls, seminfo);  /* 读取数字 */
      }
      case EOZ: {
        return TK_EOS;
      }
      default: {
        if (lislalpha(ls->current)) {  /* identifier or reserved word? 标识符或者保留字 */
          TString *ts;
          do {
            save_and_next(ls);
          } while (lislalnum(ls->current));
          ts = luaX_newstring(ls, luaZ_buffer(ls->buff),
                                  luaZ_bufflen(ls->buff));
          seminfo->ts = ts;
          if (isreserved(ts))  /* reserved word?  是否是保留字 */
            return ts->extra - 1 + FIRST_RESERVED; /* 返回保留字的类型，其中还包括了诸如and和or之类的逻辑运算符 */
          else {
            return TK_NAME;
          }
        }
        else {  /* single-char tokens (+ - / ...)  单个字符的词素，主要是加法，减法，取负号，按位逻辑运算符等 */
          int c = ls->current;
          next(ls);
          return c;
        }
      }
    }
  }
}

/* 更新当前的token标记 */
void luaX_next (LexState *ls) {
  ls->lastline = ls->linenumber;
  if (ls->lookahead.token != TK_EOS) {  /* is there a look-ahead token? */
    ls->t = ls->lookahead;  /* use this one */
    ls->lookahead.token = TK_EOS;  /* and discharge it */
  }
  else
    ls->t.token = llex(ls, &ls->t.seminfo);  /* read next token */
}

/* 向前看一个标记 */
int luaX_lookahead (LexState *ls) {
  lua_assert(ls->lookahead.token == TK_EOS);
  ls->lookahead.token = llex(ls, &ls->lookahead.seminfo);
  return ls->lookahead.token;
}
```

## Lua 语法分析器

以下为`lparse.c`和`llex.h`

表达式和变量描述符。

可以延迟变量和表达式的代码生成，以允许优化；`expdesc`结构描述了可能延迟的变量/表达式。它具有其“主要”值的说明以及也可以产生其值的条件跳转列表（生成的由短路运算符`and`/`or`表示。

```cpp
/* kinds of variables/expressions 变量或者表达式的类型 */
typedef enum {
  VVOID,  /* when 'expdesc' describes the last expression a list,
             this kind means an empty list (so, no expression) 
             当“expdesc”描述列表的最后一个表达式时，
             这种类型表示一个空列表（因此，没有表达式）*/
  VNIL,  /* constant nil 常量 nil */
  VTRUE,  /* constant true 常量 true */
  VFALSE,  /* constant false 常量 false */
  VK,  /* constant in 'k'; info = index of constant in 'k' 
      'k'中的常数； info ='k'中常数的索引 */
  VKFLT,  /* floating constant; nval = numerical float value 浮点常量，nval = numerical */
  VKINT,  /* integer constant; ival = numerical integer value 整形常量 ival = numerical */
  VKSTR,  /* string constant; strval = TString address;
             (string is fixed by the lexer) 字符串常量 */
  VNONRELOC,  /* expression has its value in a fixed register;
                 info = result register 
                 表达式在固定寄存器中具有其值； 信息=结果寄存器 */
  VLOCAL,  /* local variable; var.ridx = local register;
              var.vidx = relative index in 'actvar.arr' local 变量  */
  VUPVAL,  /* upvalue variable; info = index of upvalue in 'upvalues' 升值变量 */
  VCONST,  /* compile-time constant; info = absolute index in 'actvar.arr' 编译时常量 */
  VINDEXED,  /* indexed variable;
                ind.t = table register;
                ind.idx = key's R index 
                索引固定寻址 */
  VINDEXUP,  /* indexed upvalue;
                ind.t = table upvalue;
                ind.idx = key's K index 
                索引向上寻址 */
  VINDEXI, /* indexed variable with constant integer;
                ind.t = table register;
                ind.idx = key's value 
                具有恒定整数的索引变量；*/
  VINDEXSTR, /* indexed variable with literal string;
                ind.t = table register;
                ind.idx = key's K index 
                带文字字符串的索引变量；*/
  VJMP,  /* expression is a test/comparison;
            info = pc of corresponding jump instruction 
            一种测试/比较表达式； info =相应跳转指令的pc*/
  VRELOC,  /* expression can put result in any register;
              可以将结果放入任何寄存器的表达式；
              info = instruction pc */
  VCALL,  /* expression is a function call; info = instruction pc 调用函数表达式 */
  VVARARG  /* vararg expression; info = instruction pc 参数变量表达式 */
} expkind;


#define vkisvar(k)	(VLOCAL <= (k) && (k) <= VINDEXSTR)
#define vkisindexed(k)	(VINDEXED <= (k) && (k) <= VINDEXSTR)
```

```cpp
/* 表达式变量 */
typedef struct expdesc {
  expkind k;
  union {
    lua_Integer ival;    /* for VKINT 整形 */
    lua_Number nval;  /* for VKFLT 浮点 */
    TString *strval;  /* for VKSTR 字符串 */
    int info;  /* for generic use 一般使用信息 */
    struct {  /* for indexed variables 索引变量 */
      short idx;  /* index (R or "long" K) 索引 */
      lu_byte t;  /* table (register or upvalue) 表(寄存器或者升值) */
    } ind;
    struct {  /* for local variables 局部变量 */
      lu_byte sidx;  /* index in the stack 栈中索引 */
      unsigned short vidx;  /* index in 'actvar.arr' 索引 */
    } var;
  } u;
  int t;  /* patch list of 'exit when true' 条件为真的附加列表 */
  int f;  /* patch list of 'exit when false' 条件为假的附加列表 */
} expdesc;
```

```cpp
/* description of an active local variable 
活动局部变量的描述 */
typedef union Vardesc {
  struct {
    TValuefields;  /* constant value (if it is a compile-time constant) 
                  常量值（如果它是编译时常量） */
    lu_byte kind;
    lu_byte sidx;  /* index of the variable in the stack 堆栈中变量的索引 */
    short pidx;  /* index of the variable in the Proto's 'locvars' array 
                Proto的“locvars”数组中变量的索引 */
    TString *name;  /* variable name 变量名称 */
  } vd;
  TValue k;  /* constant value (if any) 常数值（如果有） */
} Vardesc;
```

```cpp
/* description of pending goto statements and label statements 
未决的goto语句和label语句的描述  */
typedef struct Labeldesc {
  TString *name;  /* label identifier label标识符 */
  int pc;  /* position in code 代码位置指针 */
  int line;  /* line where it appeared 出现的行数 */
  lu_byte nactvar;  /* number of active variables in that position 
                  该位置的活动变量数 */
  lu_byte close;  /* goto that escapes upvalues 逃避升值的goto */
} Labeldesc;


/* list of labels or gotos 
标签或goto列表 */
typedef struct Labellist {
  Labeldesc *arr;  /* array 数组 */
  int n;  /* number of entries in use 在用的元素个数 */
  int size;  /* array size 数组空间 */
} Labellist;


/* dynamic structures used by the parser 
解析器使用的动态结构 */
typedef struct Dyndata {
  struct {  /* list of active local variables 活动局部变量列表 */
    Vardesc *arr;
    int n;
    int size;
  } actvar;
  Labellist gt;  /* list of pending gotos 待处理的goto列表 */
  Labellist label;   /* list of active labels 活动标签的列表 */
} Dyndata;
```

```cpp
/*
** nodes for block list (list of active blocks)
块列表的节点（活动块的列表）
*/
typedef struct BlockCnt {
  struct BlockCnt *previous;  /* chain 前一个活动块 */
  int firstlabel;  /* index of first label in this block
                    该块中第一个标签的索引 */
  int firstgoto;  /* index of first pending goto in this block 
                    此块中第一个挂起的goto的索引 */
  lu_byte nactvar;  /* # active locals outside the block 
                      块外的活动locals*/
  lu_byte upval;  /* true if some variable in the block is an upvalue
                     如果块中的某个变量是高值，则为true */
  lu_byte isloop;  /* true if 'block' is a loop 
                      如果'block'是循环，则为true */
  lu_byte insidetbc;  /* true if inside the scope of a to-be-closed var. 
                      如果在要关闭的var范围内，则为true。 */
} BlockCnt;
```

```cpp
/* state needed to generate code for a given function 
为给定功能生成代码所需的状态 */
typedef struct FuncState {
  Proto *f;  /* current function header 当前函数的标头 */
  struct FuncState *prev;  /* enclosing function 封闭函数 */
  struct LexState *ls;  /* lexical state 词法分析器 */
  struct BlockCnt *bl;  /* chain of current blocks 当前块链 */
  int pc;  /* next position to code (equivalent to 'ncode') 下一个代码指针 */
  int lasttarget;   /* 'label' of last 'jump label' 标签或上个跳转标签 */
  int previousline;  /* last line that was saved in 'lineinfo' 前一行信息 */
  int nk;  /* number of elements in 'k' k中的元素个数 */
  int np;  /* number of elements in 'p' p中的元素个数 */
  int nabslineinfo;  /* number of elements in 'abslineinfo' 绝对行信息的元素个数 */
  int firstlocal;  /* index of first local var (in Dyndata array) 第一个局部变量的索引 */
  int firstlabel;  /* index of first label (in 'dyd->label->arr') 第一个标签的索引 */
  short ndebugvars;  /* number of elements in 'f->locvars' f->locvars的元素个数 */
  lu_byte nactvar;  /* number of active local variables 活跃局部变量的个数 */
  lu_byte nups;  /* number of upvalues 升值的个数 */
  lu_byte freereg;  /* first free register 第一个释放的寄存器 */
  lu_byte iwthabs;  /* instructions issued since last absolute line info 
                    自上次绝对行信息以来发布的指令 */
  lu_byte needclose;  /* function needs to close upvalues when returning 
                      返回时函数需要关闭upvalues */
} FuncState;
```

```cpp
LUAI_FUNC int luaY_nvarstack (FuncState *fs);
LUAI_FUNC LClosure *luaY_parser (lua_State *L, ZIO *z, Mbuffer *buff,
                                 Dyndata *dyd, const char *name, int firstchar);
```

Lua是递归下降语法分析，Lua的文法如下

```txt
statlist -> { stat [';'] }

stat -> funcstat
stat -> assignment
stat -> ';' # empty statement
stat -> ifstat
stat -> whilestat
stat -> DO block END
stat -> forstat
stat -> repeatstat
stat -> localstat
stat -> labelstat
stat -> retstat
stat -> breakstat
stat -> GOTO NAME

funcstat -> FUNCTION funcname body
funcname -> NAME {fieldsel} [':' NAME]
body ->  '(' parlist ')' block END 
fieldsel -> ['.' | ':'] NAME
parlist -> [ param { ',' param } ]

assignment -> suffixedexp restassign
restassign -> ',' suffixedexp restassign | '=' explist
suffixedexp -> primaryexp { '.' NAME | '[' expr ']' | ':' NAME funcargs | funcargs }
primaryexp -> NAME | '(' expr ')'
funcargs -> '(' [ explist ] ')'
funcargs -> constructor
funcargs -> STRING

ifstat -> IF cond THEN block {ELSEIF cond THEN block} [ELSE block] END
cond -> expr
block -> statlist

whilestat -> WHILE cond DO block END

forstat -> FOR (fornum | forlist) END
fornum -> NAME = expr,expr[,expr] forbody
forlist -> NAME {,NAME} IN explist forbody
forbody -> DO block

repeatstat -> REPEAT block UNTIL cond

localstat -> LOCAL attrib NAME {',' attrib NAME} ['=' explist]
attrib -> ['<' NAME '>']

labelstat -> '::' NAME '::'

retstat -> RETURN [explist] [';']
breakstat -> BREAK

explist -> expr { ',' expr }
index -> '[' expr ']'
subexpr -> (simpleexp | unop subexpr) { binop subexpr }
simpleexp -> FLT | INT | STRING | NIL | TRUE | FALSE | ... | constructor | FUNCTION body | suffixedexp
constructor -> '{' [ field { sep field } [sep] ] '}'
field -> listfield | recfield
recfield -> (NAME | '['expr']') = expr
listfield -> expr
sep -> ',' | ';'
expr -> subexpr

test_then_block -> [IF | ELSEIF] cond THEN block
```

Lua 语法分析器Parser的主函数，文法的分析首先从`statlist开始`

```cpp
LClosure *luaY_parser (lua_State *L, ZIO *z, Mbuffer *buff,
                       Dyndata *dyd, const char *name, int firstchar) {
  LexState lexstate;
  FuncState funcstate;
  LClosure *cl = luaF_newLclosure(L, 1);  /* create main closure */
  setclLvalue2s(L, L->top, cl);  /* anchor it (to avoid being collected) */
  luaD_inctop(L);
  lexstate.h = luaH_new(L);  /* create table for scanner */
  sethvalue2s(L, L->top, lexstate.h);  /* anchor it */
  luaD_inctop(L);
  funcstate.f = cl->p = luaF_newproto(L);
  funcstate.f->source = luaS_new(L, name);  /* create and anchor TString */
  luaC_objbarrier(L, funcstate.f, funcstate.f->source);
  lexstate.buff = buff;
  lexstate.dyd = dyd;
  dyd->actvar.n = dyd->gt.n = dyd->label.n = 0;
  luaX_setinput(L, &lexstate, z, funcstate.f->source, firstchar);  // 设置词法分析器的输入
  mainfunc(&lexstate, &funcstate);
  lua_assert(!funcstate.prev && funcstate.nups == 1 && !lexstate.fs);
  /* all scopes should be correctly finished */
  lua_assert(dyd->actvar.n == 0 && dyd->gt.n == 0 && dyd->label.n == 0);
  L->top--;  /* remove scanner's table */
  return cl;  /* closure is on the stack, too */
}

/*
** compiles the main function, which is a regular vararg function with an
** upvalue named LUA_ENV
**编译main函数，这是一个常规vararg函数，带有
**名为LUA_ENV的增值
*/
static void mainfunc (LexState *ls, FuncState *fs) {
  BlockCnt bl;
  Upvaldesc *env;
  open_func(ls, fs, &bl);
  setvararg(fs, 0);  /* main function is always declared vararg */
  env = allocupvalue(fs);  /* ...set environment upvalue */
  env->instack = 1;
  env->idx = 0;
  env->kind = VDKREG;
  env->name = ls->envn;
  luaX_next(ls);  /* read first token */  //读取第一个标记
  statlist(ls);  /* parse main body */ // 开始语法分析
  check(ls, TK_EOS);
  close_func(ls);
}
```

```cpp
static void statlist (LexState *ls) {
  /* statlist -> { stat [';'] } */
  while (!block_follow(ls, 1)) {
    if (ls->t.token == TK_RETURN) {
      statement(ls);
      return;  /* 'return' must be last statement */
    }
    statement(ls);
  }
}

static void statement (LexState *ls) {
  int line = ls->linenumber;  /* may be needed for error messages 错误消息可能需要的行信息 */
  enterlevel(ls);
  switch (ls->t.token) {
    case ';': {  /* stat -> ';' (empty statement) */
      luaX_next(ls);  /* skip ';' */
      break;
    }
    case TK_IF: {  /* stat -> ifstat */
      ifstat(ls, line);
      break;
    }
    case TK_WHILE: {  /* stat -> whilestat */
      whilestat(ls, line);
      break;
    }
    case TK_DO: {  /* stat -> DO block END */
      luaX_next(ls);  /* skip DO */
      block(ls);
      check_match(ls, TK_END, TK_DO, line);
      break;
    }
    case TK_FOR: {  /* stat -> forstat */
      forstat(ls, line);
      break;
    }
    case TK_REPEAT: {  /* stat -> repeatstat */
      repeatstat(ls, line);
      break;
    }
    case TK_FUNCTION: {  /* stat -> funcstat */
      funcstat(ls, line);
      break;
    }
    case TK_LOCAL: {  /* stat -> localstat */
      luaX_next(ls);  /* skip LOCAL */
      if (testnext(ls, TK_FUNCTION))  /* local function? */
        localfunc(ls);
      else
        localstat(ls);
      break;
    }
    case TK_DBCOLON: {  /* stat -> label */
      luaX_next(ls);  /* skip double colon */
      labelstat(ls, str_checkname(ls), line);
      break;
    }
    case TK_RETURN: {  /* stat -> retstat */
      luaX_next(ls);  /* skip RETURN */
      retstat(ls);
      break;
    }
    case TK_BREAK: {  /* stat -> breakstat */
      breakstat(ls);
      break;
    }
    case TK_GOTO: {  /* stat -> 'goto' NAME */
      luaX_next(ls);  /* skip 'goto' */
      gotostat(ls);
      break;
    }
    default: {  /* stat -> func | assignment */
      exprstat(ls);
      break;
    }
  }
  lua_assert(ls->fs->f->maxstacksize >= ls->fs->freereg &&
             ls->fs->freereg >= luaY_nvarstack(ls->fs));
  ls->fs->freereg = luaY_nvarstack(ls->fs);  /* free registers */
  leavelevel(ls);
}
```

`whilestat`示例

```cpp
static void whilestat (LexState *ls, int line) {
  /* whilestat -> WHILE cond DO block END */
  FuncState *fs = ls->fs;
  int whileinit;
  int condexit;
  BlockCnt bl;
  luaX_next(ls);  /* skip WHILE 跳过while关键字(前提是在上一层函数调用中发现了while) */
  whileinit = luaK_getlabel(fs);
  condexit = cond(ls);
  enterblock(fs, &bl, 1);
  checknext(ls, TK_DO);
  block(ls);
  luaK_jumpto(fs, whileinit);
  check_match(ls, TK_END, TK_WHILE, line);
  leaveblock(fs);
  luaK_patchtohere(fs, condexit);  /* false conditions finish the loop 
                                      错误条件结束循环 */
}

static int cond (LexState *ls) {
  /* cond -> exp */
  expdesc v;
  expr(ls, &v);  /* read condition 读取条件 */
  if (v.k == VNIL) v.k = VFALSE;  /* 'falses' are all equal here “假”在这里都相等 */
  luaK_goiftrue(ls->fs, &v);
  return v.f;
}

static void block (LexState *ls) {
  /* block -> statlist */
  FuncState *fs = ls->fs;
  BlockCnt bl;
  enterblock(fs, &bl, 0);
  statlist(ls);
  leaveblock(fs);
}
```

`expr`示例

```cpp
static void expr (LexState *ls, expdesc *v) {
  subexpr(ls, v, 0);
}

/*
** Priority table for binary operators.
二进制运算符的优先级表。
*/
static const struct {
  lu_byte left;  /* left priority for each binary operator  每个二进制运算符的左优先级 */
  lu_byte right; /* right priority 游优先级 */
} priority[] = {  /* ORDER OPR */
   {10, 10}, {10, 10},           /* '+' '-' */
   {11, 11}, {11, 11},           /* '*' '%' */
   {14, 13},                  /* '^' (right associative 右结合) */
   {11, 11}, {11, 11},           /* '/' '//' */
   {6, 6}, {4, 4}, {5, 5},   /* '&' '|' '~' */
   {7, 7}, {7, 7},           /* '<<' '>>' */
   {9, 8},                   /* '..' (right associative 右结合) */
   {3, 3}, {3, 3}, {3, 3},   /* ==, <, <= */
   {3, 3}, {3, 3}, {3, 3},   /* ~=, >, >= */
   {2, 2}, {1, 1}            /* and, or */
};

#define UNARY_PRIORITY	12  /* priority for unary operators 一元运算符的优先级 */


/*
** subexpr -> (simpleexp | unop subexpr) { binop subexpr }
** where 'binop' is any binary operator with a priority higher than 'limit'
其中 “binop” 是优先级高于“限制”的任何二进制运算符
*/
static BinOpr subexpr (LexState *ls, expdesc *v, int limit) {
  BinOpr op;
  UnOpr uop;
  enterlevel(ls);
  uop = getunopr(ls->t.token);
  if (uop != OPR_NOUNOPR) {  /* prefix (unary) operator? 前缀（一元）运算符？ */
    int line = ls->linenumber;
    luaX_next(ls);  /* skip operator 跳过运算符 */
    subexpr(ls, v, UNARY_PRIORITY);
    luaK_prefix(ls->fs, uop, v, line);
  }
  else simpleexp(ls, v);
  /* expand while operators have priorities higher than 'limit' 
  扩展，而操作员的优先级高于“限制” */
  op = getbinopr(ls->t.token);
  while (op != OPR_NOBINOPR && priority[op].left > limit) {
    expdesc v2;
    BinOpr nextop;
    int line = ls->linenumber;
    luaX_next(ls);  /* skip operator 跳过运算符 */
    luaK_infix(ls->fs, op, v);
    /* read sub-expression with higher priority 
    读取具有更高优先级的子表达式 */
    nextop = subexpr(ls, &v2, priority[op].right);
    luaK_posfix(ls->fs, op, v, &v2, line);
    op = nextop;
  }
  leavelevel(ls);
  return op;  /* return first untreated operator 返回第一个未经处理的运算符 */
}
```

<!--TODO-->

## Lua 预编译

以下为`lundump.c`和`lundump.h`

```cpp
typedef struct {
  lua_State *L;
  lua_Writer writer;
  void *data;
  int strip;
  int status;
} DumpState;
```

```cpp
/*
** dump Lua function as precompiled chunk
*/
int luaU_dump(lua_State *L, const Proto *f, lua_Writer w, void *data,
              int strip) {
  DumpState D;
  D.L = L;
  D.writer = w;
  D.data = data;
  D.strip = strip;
  D.status = 0;
  DumpHeader(&D);
  DumpByte(f->sizeupvalues, &D);
  DumpFunction(f, NULL, &D);
  return D.status;
}
```

```cpp
/*
** All high-level dumps go through DumpVector; you can change it to change the endianness of the result
所有高级转储都通过DumpVector进行；可以更改它以更改结果的字节序
*/
#define DumpVector(v,n,D)	DumpBlock(v,(n)*sizeof((v)[0]),D)

#define DumpLiteral(s,D)	DumpBlock(s, sizeof(s) - sizeof(char), D)

static void DumpBlock (const void *b, size_t size, DumpState *D) {
  if (D->status == 0 && size > 0) {
    lua_unlock(D->L);
    D->status = (*D->writer)(D->L, b, size, D->data);
    lua_lock(D->L);
  }
}
```

```cpp
/*
** dump Lua function as precompiled chunk
将Lua函数转储为预编译块
*/
int luaU_dump(lua_State *L, const Proto *f, lua_Writer w, void *data,
              int strip) {
  DumpState D;
  D.L = L;
  D.writer = w;
  D.data = data;
  D.strip = strip;
  D.status = 0;
  DumpHeader(&D);
  DumpByte(f->sizeupvalues, &D);
  DumpFunction(f, NULL, &D);
  return D.status;
}
```

## Lua 缓冲流

以下为`lzio.c`和`lzio.h`

缓冲数组的定义(可用于词法分析器的临时数据存放位置)

```cpp
typedef struct Mbuffer {
  char *buffer;    /* buffer数组定义 */
  size_t n;        /* 占用空间 */
  size_t buffsize; /* 总空间 */
} Mbuffer;

#define luaZ_initbuffer(L, buff) ((buff)->buffer = NULL, (buff)->buffsize = 0)
#define luaZ_buffer(buff)	((buff)->buffer)
#define luaZ_sizebuffer(buff)	((buff)->buffsize)
#define luaZ_bufflen(buff)	((buff)->n)
#define luaZ_buffremove(buff,i)	((buff)->n -= (i))
#define luaZ_resetbuffer(buff) ((buff)->n = 0)
#define luaZ_resizebuffer(L, buff, size) \
	((buff)->buffer = luaM_reallocvchar(L, (buff)->buffer, \
				(buff)->buffsize, size), \
	(buff)->buffsize = size)
#define luaZ_freebuffer(L, buff)	luaZ_resizebuffer(L, buff, 0)
```

缓冲流定义:

```cpp
int luaZ_fill (ZIO *z) {
  size_t size;
  lua_State *L = z->L;
  const char *buff;
  lua_unlock(L);
  buff = z->reader(L, z->data, &size);
  lua_lock(L);
  if (buff == NULL || size == 0)
    return EOZ;
  z->n = size - 1;  /* discount char being returned */
  z->p = buff;
  return cast_uchar(*(z->p++));
}


void luaZ_init (lua_State *L, ZIO *z, lua_Reader reader, void *data) {
  z->L = L;
  z->reader = reader;
  z->data = data;
  z->n = 0;
  z->p = NULL;
}


/* --------------------------------------------------------------- read --- */
size_t luaZ_read (ZIO *z, void *b, size_t n) {
  while (n) {
    size_t m;
    if (z->n == 0) {  /* no bytes in buffer? */
      if (luaZ_fill(z) == EOZ)  /* try to read more */
        return n;  /* no more input; return number of missing bytes */
      else {
        z->n++;  /* luaZ_fill consumed first byte; put it back */
        z->p--;
      }
    }
    m = (n <= z->n) ? n : z->n;  /* min. between n and z->n */
    memcpy(b, z->p, m);
    z->n -= m;
    z->p += m;
    b = (char *)b + m;
    n -= m;
  }
  return 0;
}
```

## Lua 内存管理

以下为`lmem.c`和`lmem.h`

```cpp
/*
** {==================================================================
** Functions to allocate/deallocate arrays for the Parser
为解析器分配/取消分配数组的函数
** ===================================================================
*/

/*
** Minimum size for arrays during parsing, to avoid overhead of reallocating to size 1, then 2, and then 4. All these arrays will be reallocated to exact sizes or erased when parsing ends.
解析过程中数组的最小大小，以避免重新分配为大小1，然后为2，然后为4的开销。所有这些数组将在解析结束时重新分配为确切的大小，或者将其擦除。
*/
#define MINSIZEARRAY	4


void *luaM_growaux_ (lua_State *L, void *block, int nelems, int *psize,
                     int size_elems, int limit, const char *what) {
  void *newblock;
  int size = *psize;
  if (nelems + 1 <= size)  /* does one extra element still fit? */
    return block;  /* nothing to be done */
  if (size >= limit / 2) {  /* cannot double it? */
    if (unlikely(size >= limit))  /* cannot grow even a little? */
      luaG_runerror(L, "too many %s (limit is %d)", what, limit);
    size = limit;  /* still have at least one free place */
  }
  else {
    size *= 2;
    if (size < MINSIZEARRAY)
      size = MINSIZEARRAY;  /* minimum size */
  }
  lua_assert(nelems + 1 <= size && size <= limit);
  /* 'limit' ensures that multiplication will not overflow */
  newblock = luaM_saferealloc_(L, block, cast_sizet(*psize) * size_elems,
                                         cast_sizet(size) * size_elems);
  *psize = size;  /* update only when everything else is OK */
  return newblock;
}


/*
** In prototypes, the size of the array is also its number of
** elements (to save memory). So, if it cannot shrink an array
** to its number of elements, the only option is to raise an
** error.
在原型中，数组的大小也是元素的数量（以节省内存）。 因此，如果无法将数组缩小到其元素数量，则唯一的选择就是引发错误。
*/
void *luaM_shrinkvector_ (lua_State *L, void *block, int *size,
                          int final_n, int size_elem) {
  void *newblock;
  size_t oldsize = cast_sizet((*size) * size_elem);
  size_t newsize = cast_sizet(final_n * size_elem);
  lua_assert(newsize <= oldsize);
  newblock = luaM_saferealloc_(L, block, oldsize, newsize);
  *size = final_n;
  return newblock;
}

/* }================================================================== */


l_noret luaM_toobig (lua_State *L) {
  luaG_runerror(L, "memory allocation error: block too big");
}


/*
** Free memory
释放内存
*/
void luaM_free_ (lua_State *L, void *block, size_t osize) {
  global_State *g = G(L);
  lua_assert((osize == 0) == (block == NULL));
  (*g->frealloc)(g->ud, block, osize, 0);
  g->GCdebt -= osize;
}


/*
** In case of allocation fail, this function will call the GC to try
** to free some memory and then try the allocation again.
** (It should not be called when shrinking a block, because then the
** interpreter may be in the middle of a collection step.)
**万一分配失败，此函数将调用GC尝试释放一些内存，然后再次尝试分配。
**（在缩小块时不应调用它，因为这样，解释器可能处于收集步骤的中间。）
*/
static void *tryagain (lua_State *L, void *block,
                       size_t osize, size_t nsize) {
  global_State *g = G(L);
  if (ttisnil(&g->nilvalue)) {  /* is state fully build? */
    luaC_fullgc(L, 1);  /* try to free some memory... */
    return (*g->frealloc)(g->ud, block, osize, nsize);  /* try again */
  }
  else return NULL;  /* cannot free any memory without a full state */
}


/*
** Generic allocation routine.
** If allocation fails while shrinking a block, do not try again; the
** GC shrinks some blocks and it is not reentrant.
**通用分配例程。
**如果在缩小块时分配失败，请不要重试； 的
** GC缩小了一些块，并且它不是可重入的。
*/
void *luaM_realloc_ (lua_State *L, void *block, size_t osize, size_t nsize) {
  void *newblock;
  global_State *g = G(L);
  lua_assert((osize == 0) == (block == NULL));
  newblock = firsttry(g, block, osize, nsize);
  if (unlikely(newblock == NULL && nsize > 0)) {
    if (nsize > osize)  /* not shrinking a block? */
      newblock = tryagain(L, block, osize, nsize);
    if (newblock == NULL)  /* still no memory? */
      return NULL;  /* do not update 'GCdebt' */
  }
  lua_assert((nsize == 0) == (newblock == NULL));
  g->GCdebt = (g->GCdebt + nsize) - osize;
  return newblock;
}


void *luaM_saferealloc_ (lua_State *L, void *block, size_t osize,
                                                    size_t nsize) {
  void *newblock = luaM_realloc_(L, block, osize, nsize);
  if (unlikely(newblock == NULL && nsize > 0))  /* allocation failed? */
    luaM_error(L);
  return newblock;
}

/* 分配内存 */
void *luaM_malloc_ (lua_State *L, size_t size, int tag) {
  if (size == 0)
    return NULL;  /* that's all */
  else {
    global_State *g = G(L);
    void *newblock = firsttry(g, NULL, tag, size);
    if (unlikely(newblock == NULL)) {
      newblock = tryagain(L, NULL, tag, size);
      if (newblock == NULL)
        luaM_error(L);
    }
    g->GCdebt += size;
    return newblock;
  }
}

```

## Lua 对象

以下为`lobject.c`和`lobject.h`

```cpp
/*
** Union of all Lua values
所有Lua值的并集
*/
typedef union Value {
  struct GCObject *gc;    /* collectable objects 可回收对象 */
  void *p;         /* light userdata 轻量用户数据 */
  int b;           /* booleans 布尔值 */
  lua_CFunction f; /* light C functions  轻量C函数 */
  lua_Integer i;   /* integer numbers  整数 */
  lua_Number n;    /* float numbers 浮点数 */
} Value;
```

```cpp
/*
** Tagged Values. This is the basic representation of values in Lua: an actual value plus a tag with its type.
标记的值。 这是Lua中值的基本表示形式：实际值加上带有其类型的标记。
*/
#define TValuefields	Value value_; lu_byte tt_

typedef struct TValue {
  TValuefields;
} TValue;
```

```cpp
/*
** Entries in the Lua stack Lua堆栈中的条目
*/
typedef union StackValue {
  TValue val;
} StackValue;


/* index to stack elements 堆栈元素的索引 */
typedef StackValue *StkId;

/* convert a 'StackValue' to a 'T
```

Lua可回收对象定义

```cpp
/*
** Common Header for all collectable objects (in macro form, to beincluded in other objects)
所有可收集对象的公共标头（以宏形式包含在其他对象中）
*/
#define CommonHeader	struct GCObject *next; lu_byte tt; lu_byte marked


/* Common type for all collectable objects 所有可收集对象的通用类型 */
typedef struct GCObject {
  CommonHeader;
} GCObject;
```

Lua字符串定义

```cpp
typedef struct TString {
  CommonHeader;
  lu_byte extra;  /* reserved words for short strings; "has hash" for longs 短字符串的保留字； 长期“散列” */
  lu_byte shrlen;  /* length for short strings 短字符串的长度 */
  unsigned int hash; /* 字符串哈希值 */
  union {
    size_t lnglen;  /* length for long strings 长字符串的长度 */
    struct TString *hnext;  /* linked list for hash table 哈希表的链表 */
  } u;
} TString;
```

Lua用户数据定义

```cpp
/* Ensures that addresses after this type are always fully aligned. 
确保此类型之后的地址始终完全对齐。*/
typedef union UValue {
  TValue uv;
  LUAI_MAXALIGN;  /* ensures maximum alignment for udata bytes 确保udata字节的最大对齐 */
} UValue;


/*
** Header for userdata with user values;
** memory area follows the end of this structure.
*/
typedef struct Udata {
  CommonHeader;
  unsigned short nuvalue;  /* number of user values 用户数据的数量 */
  size_t len;  /* number of bytes 字节数 */
  struct Table *metatable; /* 元表 */
  GCObject *gclist;
  UValue uv[1];  /* user values 用户数据 */
} Udata;

/*
** Header for userdata with no user values. These userdata do not need
** to be gray during GC, and therefore do not need a 'gclist' field.
** To simplify, the code always use 'Udata' for both kinds of userdata,
** making sure it never accesses 'gclist' on userdata with no user values.
** This structure here is used only to compute the correct size for
** this representation. (The 'bindata' field in its end ensures correct
** alignment for binary data following this header.)
**没有用户值的userdata标头。 这些用户数据在GC期间不需要为灰色，因此不需要'gclist'字段。
**为简化起见，代码始终对两种类型的用户数据都使用“ Udata”，以确保它永远不会在没有用户值的情况下访问用户数据上的“ gclist”。
**此结构仅用于计算此表示形式的正确大小。 （“ bindata”字段的末尾确保此标头后面的二进制数据正确对齐。）
*/
typedef struct Udata0 {
  CommonHeader;
  unsigned short nuvalue;  /* number of user values */
  size_t len;  /* number of bytes */
  struct Table *metatable;
  union {LUAI_MAXALIGN;} bindata;
} Udata0;
```

函数原型

```cpp
/*
** Description of an upvalue for function prototypes
函数原型升值的描述
*/
typedef struct Upvaldesc {
  TString *name;  /* upvalue name (for debug information) */
  lu_byte instack;  /* whether it is in stack (register) */
  lu_byte idx;  /* index of upvalue (in stack or in outer function's list) */
  lu_byte kind;  /* kind of corresponding variable */
} Upvaldesc;


/*
** Description of a local variable for function prototypes (used for debug information)
函数原型的局部变量的描述（用于调试信息）
*/
typedef struct LocVar {
  TString *varname;
  int startpc;  /* first point where variable is active 变量有效的第一点 */
  int endpc;    /* first point where variable is dead 变量失效的第一点 */
} LocVar;
```

```cpp
/*
** Function Prototypes 函数原型
*/
typedef struct Proto {
  CommonHeader;
  lu_byte numparams;  /* number of fixed (named) parameters 固定（命名）参数的数量 */
  lu_byte is_vararg;
  lu_byte maxstacksize;  /* number of registers needed by this function 该功能所需的寄存器数 */
  int sizeupvalues;  /* size of 'upvalues' 'upvalues'的大小 */
  int sizek;  /* size of 'k' k的大小 */
  int sizecode;
  int sizelineinfo;
  int sizep;  /* size of 'p' p的大小*/
  int sizelocvars;
  int sizeabslineinfo;  /* size of 'abslineinfo' 绝对行信息的大小 */
  int linedefined;  /* debug information 调试信息：定义的行 */
  int lastlinedefined;  /* debug information 调试信息：上一个定义的行  */
  TValue *k;  /* constants used by the function 函数使用的常数 */
  Instruction *code;  /* opcodes 操作码 */
  struct Proto **p;  /* functions defined inside the function 函数内部定义的函数 */
  Upvaldesc *upvalues;  /* upvalue information upvalue信息 */
  ls_byte *lineinfo;  /* information about source lines (debug information) */
  AbsLineInfo *abslineinfo;  /* idem */
  LocVar *locvars;  /* information about local variables (debug information) */
  TString  *source;  /* used for debug information */
  GCObject *gclist;
} Proto;
```

Lua 闭包类型

```cpp
/*
** Upvalues for Lua closures Lua 闭包的升值
*/
typedef struct UpVal {
  CommonHeader;
  lu_byte tbc;  /* true if it represents a to-be-closed variable */
  TValue *v;  /* points to stack or to its own value */
  union {
    struct {  /* (when open) */
      struct UpVal *next;  /* linked list */
      struct UpVal **previous;
    } open;
    TValue value;  /* the value (when closed) */
  } u;
} UpVal;
```

```cpp
#define ClosureHeader \
	CommonHeader; lu_byte nupvalues; GCObject *gclist

typedef struct CClosure {
  ClosureHeader;
  lua_CFunction f;
  TValue upvalue[1];  /* list of upvalues */
} CClosure;


typedef struct LClosure {
  ClosureHeader;
  struct Proto *p;
  UpVal *upvals[1];  /* list of upvalues */
} LClosure;


typedef union Closure {
  CClosure c;
  LClosure l;
} Closure;
```

哈希表

```cpp
/*
** Nodes for Hash tables: A pack of two TValue's (key-value pairs)
** plus a 'next' field to link colliding entries. The distribution
** of the key's fields ('key_tt' and 'key_val') not forming a proper
** 'TValue' allows for a smaller size for 'Node' both in 4-byte
** and 8-byte alignments.
**哈希表的节点：包含两个TValue（键-值对）和一个“ next”字段的链接，用于链接冲突条目。 
键字段的分布（“ key_tt”和“ key_val”）未形成正确的“ TValue”，从而使“节点”的大小在4字节和8字节对齐方式中均较小。
*/
typedef union Node {
  struct NodeKey {
    TValuefields;  /* fields for value */
    lu_byte key_tt;  /* key type */
    int next;  /* for chaining */
    Value key_val;  /* key value */
  } u;
  TValue i_val;  /* direct access to node's value as a proper 'TValue' */
} Node;
```

Lua Table类型定义

```cpp
typedef struct Table {
  CommonHeader;
  lu_byte flags;  /* 1<<p means tagmethod(p) is not present 1 << p表示不存在tagmethod（p） */
  lu_byte lsizenode;  /* log2 of size of 'node' array “节点”数组大小的log2 */
  unsigned int alimit;  /* "limit" of 'array' array 数组限制 */
  TValue *array;  /* array part 数组部分 */
  Node *node;
  Node *lastfree;  /* any free position is before this position 任意在这个位置之前的free位置 */
  struct Table *metatable;
  GCObject *gclist;
} Table;
```

Lua 算术运算

```cpp
static lua_Integer intarith (lua_State *L, int op, lua_Integer v1,
                                                   lua_Integer v2) {
  switch (op) {
    case LUA_OPADD: return intop(+, v1, v2);
    case LUA_OPSUB:return intop(-, v1, v2);
    case LUA_OPMUL:return intop(*, v1, v2);
    case LUA_OPMOD: return luaV_mod(L, v1, v2);
    case LUA_OPIDIV: return luaV_idiv(L, v1, v2);
    case LUA_OPBAND: return intop(&, v1, v2);
    case LUA_OPBOR: return intop(|, v1, v2);
    case LUA_OPBXOR: return intop(^, v1, v2);
    case LUA_OPSHL: return luaV_shiftl(v1, v2);
    case LUA_OPSHR: return luaV_shiftl(v1, -v2);
    case LUA_OPUNM: return intop(-, 0, v1);
    case LUA_OPBNOT: return intop(^, ~l_castS2U(0), v1);
    default: lua_assert(0); return 0;
  }
}


static lua_Number numarith (lua_State *L, int op, lua_Number v1,
                                                  lua_Number v2) {
  switch (op) {
    case LUA_OPADD: return luai_numadd(L, v1, v2);
    case LUA_OPSUB: return luai_numsub(L, v1, v2);
    case LUA_OPMUL: return luai_nummul(L, v1, v2);
    case LUA_OPDIV: return luai_numdiv(L, v1, v2);
    case LUA_OPPOW: return luai_numpow(L, v1, v2);
    case LUA_OPIDIV: return luai_numidiv(L, v1, v2);
    case LUA_OPUNM: return luai_numunm(L, v1);
    case LUA_OPMOD: return luaV_modf(L, v1, v2);
    default: lua_assert(0); return 0;
  }
}


int luaO_rawarith (lua_State *L, int op, const TValue *p1, const TValue *p2,
                   TValue *res) {
  switch (op) {
    case LUA_OPBAND: case LUA_OPBOR: case LUA_OPBXOR:
    case LUA_OPSHL: case LUA_OPSHR:
    case LUA_OPBNOT: {  /* operate only on integers */
      lua_Integer i1; lua_Integer i2;
      if (tointegerns(p1, &i1) && tointegerns(p2, &i2)) {
        setivalue(res, intarith(L, op, i1, i2));
        return 1;
      }
      else return 0;  /* fail */
    }
    case LUA_OPDIV: case LUA_OPPOW: {  /* operate only on floats */
      lua_Number n1; lua_Number n2;
      if (tonumberns(p1, n1) && tonumberns(p2, n2)) {
        setfltvalue(res, numarith(L, op, n1, n2));
        return 1;
      }
      else return 0;  /* fail */
    }
    default: {  /* other operations */
      lua_Number n1; lua_Number n2;
      if (ttisinteger(p1) && ttisinteger(p2)) {
        setivalue(res, intarith(L, op, ivalue(p1), ivalue(p2)));
        return 1;
      }
      else if (tonumberns(p1, n1) && tonumberns(p2, n2)) {
        setfltvalue(res, numarith(L, op, n1, n2));
        return 1;
      }
      else return 0;  /* fail */
    }
  }
}

void luaO_arith (lua_State *L, int op, const TValue *p1, const TValue *p2,
                 StkId res) {
  if (!luaO_rawarith(L, op, p1, p2, s2v(res))) {
    /* could not perform raw operation; try metamethod */
    luaT_trybinTM(L, p1, p2, res, cast(TMS, (op - LUA_OPADD) + TM_ADD));
  }
}
```

将`lua_Number`转为Lua `TString`

```cpp
/*
** Convert a number object to a string, adding it to a buffer
*/
static int tostringbuff (TValue *obj, char *buff) {
  int len;
  lua_assert(ttisnumber(obj));
  if (ttisinteger(obj))
    len = lua_integer2str(buff, MAXNUMBER2STR, ivalue(obj));
  else {
    len = lua_number2str(buff, MAXNUMBER2STR, fltvalue(obj));
    if (buff[strspn(buff, "-0123456789")] == '\0') {  /* looks like an int? */
      buff[len++] = lua_getlocaledecpoint();
      buff[len++] = '0';  /* adds '.0' to result */
    }
  }
  return len;
}


/*
** Convert a number object to a Lua string, replacing the value at 'obj'
*/
void luaO_tostring (lua_State *L, TValue *obj) {
  char buff[MAXNUMBER2STR];
  int len = tostringbuff(obj, buff);
  setsvalue(L, obj, luaS_newlstr(L, buff, len));
}
```

Lua格式化

```cpp
/*
** this function handles only '%d', '%c', '%f', '%p', '%s', and '%%'
   conventional formats, plus Lua-specific '%I' and '%U'
*/
const char *luaO_pushvfstring (lua_State *L, const char *fmt, va_list argp) {
  BuffFS buff;  /* holds last part of the result */
  const char *e;  /* points to next '%' */
  buff.pushed = buff.blen = 0;
  buff.L = L;
  while ((e = strchr(fmt, '%')) != NULL) {
    addstr2buff(&buff, fmt, e - fmt);  /* add 'fmt' up to '%' */
    switch (*(e + 1)) {  /* conversion specifier */
      case 's': {  /* zero-terminated string */
        const char *s = va_arg(argp, char *);
        if (s == NULL) s = "(null)";
        addstr2buff(&buff, s, strlen(s));
        break;
      }
      case 'c': {  /* an 'int' as a character */
        char c = cast_uchar(va_arg(argp, int));
        addstr2buff(&buff, &c, sizeof(char));
        break;
      }
      case 'd': {  /* an 'int' */
        TValue num;
        setivalue(&num, va_arg(argp, int));
        addnum2buff(&buff, &num);
        break;
      }
      case 'I': {  /* a 'lua_Integer' */
        TValue num;
        setivalue(&num, cast(lua_Integer, va_arg(argp, l_uacInt)));
        addnum2buff(&buff, &num);
        break;
      }
      case 'f': {  /* a 'lua_Number' */
        TValue num;
        setfltvalue(&num, cast_num(va_arg(argp, l_uacNumber)));
        addnum2buff(&buff, &num);
        break;
      }
      case 'p': {  /* a pointer */
        const int sz = 3 * sizeof(void*) + 8; /* enough space for '%p' */
        char *bf = getbuff(&buff, sz);
        void *p = va_arg(argp, void *);
        int len = lua_pointer2str(bf, sz, p);
        addsize(&buff, len);
        break;
      }
      case 'U': {  /* a 'long' as a UTF-8 sequence */
        char bf[UTF8BUFFSZ];
        int len = luaO_utf8esc(bf, va_arg(argp, long));
        addstr2buff(&buff, bf + UTF8BUFFSZ - len, len);
        break;
      }
      case '%': {
        addstr2buff(&buff, "%", 1);
        break;
      }
      default: {
        luaG_runerror(L, "invalid option '%%%c' to 'lua_pushfstring'",
                         *(e + 1));
      }
    }
    fmt = e + 2;  /* skip '%' and the specifier */
  }
  addstr2buff(&buff, fmt, strlen(fmt));  /* rest of 'fmt' */
  clearbuff(&buff);  /* empty buffer into the stack */
  if (buff.pushed > 1)
    luaV_concat(L, buff.pushed);  /* join all partial results */
  return svalue(s2v(L->top - 1));
}
```

## Lua API

以下为`lapi.c`和`lapi.h`

对Lua线程状态`lua_State`的操作API，将不同的Lua类型压入栈，从栈中弹出不同的类型

## 建立Lua库的辅助功能

以下为`lauxlib.c`和`lauxlib.h`

在`lua_State`中找出索引等操作API，与**Lua API**类似。

## Lua 限制 (Limit)

以下为`llimit.h`

限制，基本类型和其他一些“与安装有关的”定义

```cpp
/* maximum value for size_t */
#define MAX_SIZET	((size_t)(~(size_t)0))
/* maximum size visible for Lua (must be representable in a lua_Integer) */
#define MAX_SIZE	(sizeof(size_t) < sizeof(lua_Integer) ? MAX_SIZET \
                          : (size_t)(LUA_MAXINTEGER))
#define MAX_LUMEM	((lu_mem)(~(lu_mem)0))
#define MAX_LMEM	((l_mem)(MAX_LUMEM >> 1))
#define MAX_INT		INT_MAX  /* maximum value of an int */
```

类型转换

```cpp
/* type casts (a macro highlights casts in the code) */
#define cast(t, exp)	((t)(exp))

#define cast_void(i)	cast(void, (i))
#define cast_voidp(i)	cast(void *, (i))
#define cast_num(i)	cast(lua_Number, (i))
#define cast_int(i)	cast(int, (i))
#define cast_uint(i)	cast(unsigned int, (i))
#define cast_byte(i)	cast(lu_byte, (i))
#define cast_uchar(i)	cast(unsigned char, (i))
#define cast_char(i)	cast(char, (i))
#define cast_charp(i)	cast(char *, (i))
#define cast_sizet(i)	cast(size_t, (i))
```

运算操作

```cpp
#if !defined(luai_numadd)
#define luai_numadd(L,a,b)      ((a)+(b))
#define luai_numsub(L,a,b)      ((a)-(b))
#define luai_nummul(L,a,b)      ((a)*(b))
#define luai_numunm(L,a)        (-(a))
#define luai_numeq(a,b)         ((a)==(b))
#define luai_numlt(a,b)         ((a)<(b))
#define luai_numle(a,b)         ((a)<=(b))
#define luai_numgt(a,b)         ((a)>(b))
#define luai_numge(a,b)         ((a)>=(b))
#define luai_numisnan(a)        (!luai_numeq((a), (a)))
#endif
```

## Lua 垃圾回收 (GC)

以下为`lgc.c`和`lgc.h`

可回收的对象可能具有以下三种颜色之一：白色，表示未标记该对象； 
灰色，表示该对象已标记，但其引用可能未标记； 和黑色，表示该对象及其所有引用均已标记。 
垃圾收集器在标记对象时的主要不变性是，黑色对象永远不能指向白色对象。 
此外，任何灰色物体都必须在“灰色列表”中（灰色，再次灰色，弱，全弱，星历），
以便在完成收集周期之前可以再次对其进行访问。 当不强制执行不变式时（例如，扫描阶段），
这些列表没有意义。

```cpp
/*
** Union of all collectable objects (only for conversions)
所有可收集对象的并集（仅用于转换）
*/
union GCUnion {
  GCObject gc;  /* common header */
  struct TString ts;
  struct Udata u;
  union Closure cl;
  struct Table h;
  struct Proto p;
  struct lua_State th;  /* thread */
  struct UpVal upv;
};
```

```cpp
/*
** Possible states of the Garbage Collector
垃圾回收器的可能状态
*/
#define GCSpropagate	0
#define GCSenteratomic	1
#define GCSatomic	2
#define GCSswpallgc	3
#define GCSswpfinobj	4
#define GCSswptobefnz	5
#define GCSswpend	6
#define GCScallfin	7
#define GCSpause	8
```

Lua GC API

```cpp
LUAI_FUNC void luaC_fix (lua_State *L, GCObject *o);
LUAI_FUNC void luaC_freeallobjects (lua_State *L);
LUAI_FUNC void luaC_step (lua_State *L);
LUAI_FUNC void luaC_runtilstate (lua_State *L, int statesmask);
LUAI_FUNC void luaC_fullgc (lua_State *L, int isemergency);
LUAI_FUNC GCObject *luaC_newobj (lua_State *L, int tt, size_t sz);
LUAI_FUNC void luaC_barrier_ (lua_State *L, GCObject *o, GCObject *v);
LUAI_FUNC void luaC_barrierback_ (lua_State *L, GCObject *o);
LUAI_FUNC void luaC_checkfinalizer (lua_State *L, GCObject *o, Table *mt);
LUAI_FUNC void luaC_changemode (lua_State *L, int newmode);
```

```cpp
/*
** Performs a full GC cycle; if 'isemergency', set a flag to avoid
** some operations which could change the interpreter state in some
** unexpected ways (running finalizers and shrinking some structures).
执行完整的GC周期； 如果是“紧急状态”，
则设置一个标志以避免某些可能以某些意外方式更改解释器状态的操作（运行终结器并缩小某些结构）。
*/
void luaC_fullgc (lua_State *L, int isemergency) {
  global_State *g = G(L);
  lua_assert(!g->gcemergency);
  g->gcemergency = isemergency;  /* set flag */
  if (g->gckind == KGC_INC)
    fullinc(L, g);
  else
    fullgen(L, g);
  g->gcemergency = 0;
}

/*
** Perform a full collection in incremental mode.
** Before running the collection, check 'keepinvariant'; if it is true,
** there may be some objects marked as black, so the collector has
** to sweep all objects to turn them back to white (as white has not
** changed, nothing will be collected).
**以增量模式执行完整收集。
**在运行收集之前，请选中“ keepinvariant”； 
如果为真，则可能有一些标记为黑色的对象，因此收集器必须清除所有对象以将它们恢复为白色
（由于白色没有更改，因此将不会收集任何内容）。
*/
static void fullinc (lua_State *L, global_State *g) {
  if (keepinvariant(g))  /* black objects? */
    entersweep(L); /* sweep everything to turn them back to white */
  /* finish any pending sweep phase to start a new cycle */
  luaC_runtilstate(L, bitmask(GCSpause));
  luaC_runtilstate(L, bitmask(GCScallfin));  /* run up to finalizers */
  /* estimate must be correct after a full GC cycle */
  lua_assert(g->GCestimate == gettotalbytes(g));
  luaC_runtilstate(L, bitmask(GCSpause));  /* finish collection */
  setpause(g);
}

/*
** Does a full collection in generational mode.
在世代模式下进行完整收集。
*/
static lu_mem fullgen (lua_State *L, global_State *g) {
  enterinc(g);
  return entergen(L, g);
}
```

```cpp
/*
** Enter generational mode. Must go until the end of an atomic cycle
** to ensure that all threads and weak tables are in the gray lists.
** Then, turn all objects into old and finishes the collection.
**进入世代模式。 必须一直执行到原子循环结束，以确保所有线程和弱表都在灰色列表中。
**然后，将所有对象变旧并完成收集。
*/
static lu_mem entergen (lua_State *L, global_State *g) {
  lu_mem numobjs;
  luaC_runtilstate(L, bitmask(GCSpause));  /* prepare to start a new cycle */
  luaC_runtilstate(L, bitmask(GCSpropagate));  /* start new cycle */
  numobjs = atomic(L);  /* propagates all and then do the atomic stuff */
  atomic2gen(L, g);
  return numobjs;
}


/*
** Enter incremental mode. Turn all objects white, make all
** intermediate lists point to NULL (to avoid invalid pointers),
** and go to the pause state.
进入增量模式。 将所有对象变成白色，使所有中间列表指向NULL（以避免无效的指针），然后进入暂停状态。
*/
static void enterinc (global_State *g) {
  whitelist(g, g->allgc);
  g->reallyold = g->old = g->survival = NULL;
  whitelist(g, g->finobj);
  whitelist(g, g->tobefnz);
  g->finobjrold = g->finobjold = g->finobjsur = NULL;
  g->gcstate = GCSpause;
  g->gckind = KGC_INC;
  g->lastatomic = 0;
}


/*
** Change collector mode to 'newmode'.
改变垃圾回收器的模式
*/
void luaC_changemode (lua_State *L, int newmode) {
  global_State *g = G(L);
  if (newmode != g->gckind) {
    if (newmode == KGC_GEN)  /* entering generational mode? */
      entergen(L, g);
    else
      enterinc(g);  /* entering incremental mode */
  }
  g->lastatomic = 0;
}
```

获取不同对象的垃圾引用列表

```cpp
static GCObject **getgclist (GCObject *o) {
  switch (o->tt) {
    case LUA_TTABLE: return &gco2t(o)->gclist;
    case LUA_TLCL: return &gco2lcl(o)->gclist;
    case LUA_TCCL: return &gco2ccl(o)->gclist;
    case LUA_TTHREAD: return &gco2th(o)->gclist;
    case LUA_TPROTO: return &gco2p(o)->gclist;
    case LUA_TUSERDATA: {
      Udata *u = gco2u(o);
      lua_assert(u->nuvalue > 0);
      return &u->gclist;
    }
    default: lua_assert(0); return 0;
  }
}
```

```cpp
/*
** Clear keys for empty entries in tables. If entry is empty
** and its key is not marked, mark its entry as dead. This allows the
** collection of the key, but keeps its entry in the table (its removal
** could break a chain). The main feature of a dead key is that it must
** be different from any other value, to do not disturb searches. 
** Other places never manipulate dead keys, because its associated empty
** value is enough to signal that the entry is logically empty.
清除表中空白条目的键。 如果条目为空并且其键未标记，请将其条目标记为无效。 
这样可以收集密钥，但将其条目保留在表中（将其删除可能会破坏一条链）。 
死键的主要特征是它必须与任何其他值都不同，以免干扰搜索。 
其他地方从不操纵死键，因为其关联的空值足以表明该条目在逻辑上是空的。
*/
static void clearkey (Node *n) {
  lua_assert(isempty(gval(n)));
  if (keyiswhite(n))
    setdeadkey(n);  /* unused and unmarked key; remove it */
}


/*
** tells whether a key or value can be cleared from a weak
** table. Non-collectable objects are never removed from weak
** tables. Strings behave as 'values', so are never removed too. for
** other objects: if really collected, cannot keep them; for objects
** being finalized, keep them in keys, but not in values
告知是否可以从弱表中清除键或值。 无法收集的对象永远不会从弱表中删除。 
字符串表现为“值”，因此也永远不会删除。 其他物品：如果真的被收集，就不能保留； 
对于要完成的对象，请将其保留在键中，但不要保留在值中
*/
static int iscleared (global_State *g, const GCObject *o) {
  if (o == NULL) return 0;  /* non-collectable value */
  else if (novariant(o->tt) == LUA_TSTRING) {
    markobject(g, o);  /* strings are 'values', so are never weak */
    return 0;
  }
  else return iswhite(o);
}
```

```cpp
/*
** create a new collectable object (with given type and size) and link
** it to 'allgc' list.
创建一个新的可收集对象（具有给定的类型和大小）并将其链接到“ allgc”列表。
*/
GCObject *luaC_newobj (lua_State *L, int tt, size_t sz) {
  global_State *g = G(L);
  GCObject *o = cast(GCObject *, luaM_newobject(L, novariant(tt), sz));
  o->marked = luaC_white(g);
  o->tt = tt;
  o->next = g->allgc;
  g->allgc = o;
  return o;
}
```

```cpp
/*
** Mark an object. Userdata, strings, and closed upvalues are visited
** and turned black here. Other objects are marked gray and added
** to appropriate list to be visited (and turned black) later. (Open
** upvalues are already linked in 'headuv' list. They are kept gray
** to avoid barriers, as their values will be revisited by the thread.)
标记对象。 用户数据，字符串和封闭的升值在此处被访问并变为黑色。 其他对象标记为灰色，
然后添加到适当的列表中，以便以后访问（并变为黑色）。 
（打开的upvalues已经在'headuv'列表中链接了。
它们保持灰色以避免出现障碍，因为它们的值将被线程重新访问。）
*/
static void reallymarkobject (global_State *g, GCObject *o) {
  white2gray(o);
  switch (o->tt) {
    case LUA_TSHRSTR:
    case LUA_TLNGSTR: {
      gray2black(o);
      break;
    }
    case LUA_TUPVAL: {
      UpVal *uv = gco2upv(o);
      if (!upisopen(uv))  /* open upvalues are kept gray */
        gray2black(o);
      markvalue(g, uv->v);  /* mark its content */
      break;
    }
    case LUA_TUSERDATA: {
      Udata *u = gco2u(o);
      if (u->nuvalue == 0) {  /* no user values? */
        markobjectN(g, u->metatable);  /* mark its metatable */
        gray2black(o);  /* nothing else to mark */
        break;
      }
      /* else... */
    }  /* FALLTHROUGH */
    case LUA_TLCL: case LUA_TCCL: case LUA_TTABLE:
    case LUA_TTHREAD: case LUA_TPROTO: {
      linkobjgclist(o, g->gray);
      break;
    }
    default: lua_assert(0); break;
  }
}
```

Lua GC 遍历函数

```cpp
/*
** Traverse a table with weak values and link it to proper list. During
** propagate phase, keep it in 'grayagain' list, to be revisited in the
** atomic phase. In the atomic phase, if table has any white value,
** put it in 'weak' list, to be cleared.
遍历具有弱值的表并将其链接到正确的列表。 在传播阶段，将其保留在
“grayagain”列表中，以便在原子阶段进行重新访问。
在原子阶段，如果表具有任何白色值，则将其放入“弱”列表中以进行清除。
*/
static void traverseweakvalue (global_State *g, Table *h) {
  Node *n, *limit = gnodelast(h);
  /* if there is array part, assume it may have white values (it is not
     worth traversing it now just to check) */
  int hasclears = (h->alimit > 0);
  for (n = gnode(h, 0); n < limit; n++) {  /* traverse hash part */
    if (isempty(gval(n)))  /* entry is empty? */
      clearkey(n);  /* clear its key */
    else {
      lua_assert(!keyisnil(n));
      markkey(g, n);
      if (!hasclears && iscleared(g, gcvalueN(gval(n))))  /* a white value? */
        hasclears = 1;  /* table will have to be cleared */
    }
  }
  if (g->gcstate == GCSatomic && hasclears)
    linkgclist(h, g->weak);  /* has to be cleared later */
  else
    linkgclist(h, g->grayagain);  /* must retraverse it in atomic phase */
}


/*
** Traverse an ephemeron table and link it to proper list. Returns true
** iff any object was marked during this traversal (which implies that
** convergence has to continue). During propagation phase, keep table
** in 'grayagain' list, to be visited again in the atomic phase. In
** the atomic phase, if table has any white->white entry, it has to
** be revisited during ephemeron convergence (as that key may turn
** black). Otherwise, if it has any white key, table has to be cleared
** (in the atomic phase). In generational mode, it (like all visited
** tables) must be kept in some gray list for post-processing.
遍历一个星历表并将其链接到正确的列表。 如果在遍历过程中标记了任何对象，
则返回true（这意味着必须继续收敛）。 在传播阶段，将表保留在“灰色”列表中，
以便在原子阶段再次访问。 在原子阶段，如果表具有从白到白的任何条目，
则必须在星历收敛期间重新访问它（因为该键可能会变成黑色）。 
否则，如果它具有任何白键，则必须清除表（在原子阶段）。 
在世代模式下，它（像所有访问过的表一样）必须保留在某个灰色列表中以进行后处理。
*/
static int traverseephemeron (global_State *g, Table *h, int inv) {
  int marked = 0;  /* true if an object is marked in this traversal */
  int hasclears = 0;  /* true if table has white keys */
  int hasww = 0;  /* true if table has entry "white-key -> white-value" */
  unsigned int i;
  unsigned int asize = luaH_realasize(h);
  unsigned int nsize = sizenode(h);
  /* traverse array part */
  for (i = 0; i < asize; i++) {
    if (valiswhite(&h->array[i])) {
      marked = 1;
      reallymarkobject(g, gcvalue(&h->array[i]));
    }
  }
  /* traverse hash part; if 'inv', traverse descending
     (see 'convergeephemerons') */
  for (i = 0; i < nsize; i++) {
    Node *n = inv ? gnode(h, nsize - 1 - i) : gnode(h, i);
    if (isempty(gval(n)))  /* entry is empty? */
      clearkey(n);  /* clear its key */
    else if (iscleared(g, gckeyN(n))) {  /* key is not marked (yet)? */
      hasclears = 1;  /* table must be cleared */
      if (valiswhite(gval(n)))  /* value not marked yet? */
        hasww = 1;  /* white-white entry */
    }
    else if (valiswhite(gval(n))) {  /* value not marked yet? */
      marked = 1;
      reallymarkobject(g, gcvalue(gval(n)));  /* mark it now */
    }
  }
  /* link table into proper list */
  if (g->gcstate == GCSpropagate)
    linkgclist(h, g->grayagain);  /* must retraverse it in atomic phase */
  else if (hasww)  /* table has white->white entries? */
    linkgclist(h, g->ephemeron);  /* have to propagate again */
  else if (hasclears)  /* table has white keys? */
    linkgclist(h, g->allweak);  /* may have to clean white keys */
  else if (g->gckind == KGC_GEN)
    linkgclist(h, g->grayagain);  /* keep it in some list */
  else
    gray2black(h);
  return marked;
}


static void traversestrongtable (global_State *g, Table *h) {
  Node *n, *limit = gnodelast(h);
  unsigned int i;
  unsigned int asize = luaH_realasize(h);
  for (i = 0; i < asize; i++)  /* traverse array part */
    markvalue(g, &h->array[i]);
  for (n = gnode(h, 0); n < limit; n++) {  /* traverse hash part */
    if (isempty(gval(n)))  /* entry is empty? */
      clearkey(n);  /* clear its key */
    else {
      lua_assert(!keyisnil(n));
      markkey(g, n);
      markvalue(g, gval(n));
    }
  }
  if (g->gckind == KGC_GEN) {
    linkgclist(h, g->grayagain);  /* keep it in some gray list */
    black2gray(h);
  }
}

static lu_mem traversetable (global_State *g, Table *h) {
  const char *weakkey, *weakvalue;
  const TValue *mode = gfasttm(g, h->metatable, TM_MODE);
  markobjectN(g, h->metatable);
  if (mode && ttisstring(mode) &&  /* is there a weak mode? */
      (cast_void(weakkey = strchr(svalue(mode), 'k')),
       cast_void(weakvalue = strchr(svalue(mode), 'v')),
       (weakkey || weakvalue))) {  /* is really weak? */
    black2gray(h);  /* keep table gray */
    if (!weakkey)  /* strong keys? */
      traverseweakvalue(g, h);
    else if (!weakvalue)  /* strong values? */
      traverseephemeron(g, h, 0);
    else  /* all weak */
      linkgclist(h, g->allweak);  /* nothing to traverse now */
  }
  else  /* not weak */
    traversestrongtable(g, h);
  return 1 + h->alimit + 2 * allocsizenode(h);
}


static int traverseudata (global_State *g, Udata *u) {
  int i;
  markobjectN(g, u->metatable);  /* mark its metatable */
  for (i = 0; i < u->nuvalue; i++)
    markvalue(g, &u->uv[i].uv);
  if (g->gckind == KGC_GEN) {
    linkgclist(u, g->grayagain);  /* keep it in some gray list */
    black2gray(u);
  }
  return 1 + u->nuvalue;
}


/*
** Traverse a prototype. (While a prototype is being build, its
** arrays can be larger than needed; the extra slots are filled with
** NULL, so the use of 'markobjectN')
*/
static int traverseproto (global_State *g, Proto *f) {
  int i;
  markobjectN(g, f->source);
  for (i = 0; i < f->sizek; i++)  /* mark literals */
    markvalue(g, &f->k[i]);
  for (i = 0; i < f->sizeupvalues; i++)  /* mark upvalue names */
    markobjectN(g, f->upvalues[i].name);
  for (i = 0; i < f->sizep; i++)  /* mark nested protos */
    markobjectN(g, f->p[i]);
  for (i = 0; i < f->sizelocvars; i++)  /* mark local-variable names */
    markobjectN(g, f->locvars[i].varname);
  return 1 + f->sizek + f->sizeupvalues + f->sizep + f->sizelocvars;
}


static int traverseCclosure (global_State *g, CClosure *cl) {
  int i;
  for (i = 0; i < cl->nupvalues; i++)  /* mark its upvalues */
    markvalue(g, &cl->upvalue[i]);
  return 1 + cl->nupvalues;
}

/*
** Traverse a Lua closure, marking its prototype and its upvalues.
** (Both can be NULL while closure is being created.)
*/
static int traverseLclosure (global_State *g, LClosure *cl) {
  int i;
  markobjectN(g, cl->p);  /* mark its prototype */
  for (i = 0; i < cl->nupvalues; i++) {  /* visit its upvalues */
    UpVal *uv = cl->upvals[i];
    markobjectN(g, uv);  /* mark upvalue */
  }
  return 1 + cl->nupvalues;
}


/*
** Traverse a thread, marking the elements in the stack up to its top
** and cleaning the rest of the stack in the final traversal.
** That ensures that the entire stack have valid (non-dead) objects.
*/
static int traversethread (global_State *g, lua_State *th) {
  UpVal *uv;
  StkId o = th->stack;
  if (o == NULL)
    return 1;  /* stack not completely built yet */
  lua_assert(g->gcstate == GCSatomic ||
             th->openupval == NULL || isintwups(th));
  for (; o < th->top; o++)  /* mark live elements in the stack */
    markvalue(g, s2v(o));
  for (uv = th->openupval; uv != NULL; uv = uv->u.open.next)
    markobject(g, uv);  /* open upvalues cannot be collected */
  if (g->gcstate == GCSatomic) {  /* final traversal? */
    StkId lim = th->stack + th->stacksize;  /* real end of stack */
    for (; o < lim; o++)  /* clear not-marked stack slice */
      setnilvalue(s2v(o));
    /* 'remarkupvals' may have removed thread from 'twups' list */
    if (!isintwups(th) && th->openupval != NULL) {
      th->twups = g->twups;  /* link it back to the list */
      g->twups = th;
    }
  }
  else if (!g->gcemergency)
    luaD_shrinkstack(th); /* do not change stack in emergency cycle */
  return 1 + th->stacksize;
}

/*
** traverse one gray object, turning it to black (except for threads,
** which are always gray).
*/
static lu_mem propagatemark (global_State *g) {
  GCObject *o = g->gray;
  gray2black(o);
  g->gray = *getgclist(o);  /* remove from 'gray' list */
  switch (o->tt) {
    case LUA_TTABLE: return traversetable(g, gco2t(o));
    case LUA_TUSERDATA: return traverseudata(g, gco2u(o));
    case LUA_TLCL: return traverseLclosure(g, gco2lcl(o));
    case LUA_TCCL: return traverseCclosure(g, gco2ccl(o));
    case LUA_TPROTO: return traverseproto(g, gco2p(o));
    case LUA_TTHREAD: {
      lua_State *th = gco2th(o);
      linkgclist(th, g->grayagain);  /* insert into 'grayagain' list */
      black2gray(o);
      return traversethread(g, th);
    }
    default: lua_assert(0); return 0;
  }
}
```

Lua GC 扫描函数

```cpp
/*
** clear entries with unmarked keys from all weaktables in list 'l'
清除列表“ l”中所有弱表中未标记键的条目
*/
static void clearbykeys (global_State *g, GCObject *l) {
  for (; l; l = gco2t(l)->gclist) {
    Table *h = gco2t(l);
    Node *limit = gnodelast(h);
    Node *n;
    for (n = gnode(h, 0); n < limit; n++) {
      if (iscleared(g, gckeyN(n)))  /* unmarked key? */
        setempty(gval(n));  /* remove entry */
      if (isempty(gval(n)))  /* is entry empty? */
        clearkey(n);  /* clear its key */
    }
  }
}

/*
** clear entries with unmarked values from all weaktables in list 'l' up
** to element 'f'
清除列表'l'中所有弱表直至元素'f'中具有未标记值的条目
*/
static void clearbyvalues (global_State *g, GCObject *l, GCObject *f) {
  for (; l != f; l = gco2t(l)->gclist) {
    Table *h = gco2t(l);
    Node *n, *limit = gnodelast(h);
    unsigned int i;
    unsigned int asize = luaH_realasize(h);
    for (i = 0; i < asize; i++) {
      TValue *o = &h->array[i];
      if (iscleared(g, gcvalueN(o)))  /* value was collected? */
        setempty(o);  /* remove entry */
    }
    for (n = gnode(h, 0); n < limit; n++) {
      if (iscleared(g, gcvalueN(gval(n))))  /* unmarked value? */
        setempty(gval(n));  /* remove entry */
      if (isempty(gval(n)))  /* is entry empty? */
        clearkey(n);  /* clear its key */
    }
  }
}

static void freeobj (lua_State *L, GCObject *o) {
  switch (o->tt) {
    case LUA_TPROTO:
      luaF_freeproto(L, gco2p(o));
      break;
    case LUA_TUPVAL:
      freeupval(L, gco2upv(o));
      break;
    case LUA_TLCL:
      luaM_freemem(L, o, sizeLclosure(gco2lcl(o)->nupvalues));
      break;
    case LUA_TCCL:
      luaM_freemem(L, o, sizeCclosure(gco2ccl(o)->nupvalues));
      break;
    case LUA_TTABLE:
      luaH_free(L, gco2t(o));
      break;
    case LUA_TTHREAD:
      luaE_freethread(L, gco2th(o));
      break;
    case LUA_TUSERDATA: {
      Udata *u = gco2u(o);
      luaM_freemem(L, o, sizeudata(u->nuvalue, u->len));
      break;
    }
    case LUA_TSHRSTR:
      luaS_remove(L, gco2ts(o));  /* remove it from hash table */
      luaM_freemem(L, o, sizelstring(gco2ts(o)->shrlen));
      break;
    case LUA_TLNGSTR:
      luaM_freemem(L, o, sizelstring(gco2ts(o)->u.lnglen));
      break;
    default: lua_assert(0);
  }
}

/*
** sweep at most 'countin' elements from a list of GCObjects erasing dead
** objects, where a dead object is one marked with the old (non current)
** white; change all non-dead objects back to white, preparing for next
** collection cycle. Return where to continue the traversal or NULL if
** list is finished. ('*countout' gets the number of elements traversed.)
从擦除死对象的GCObjects列表中清除最多“计数”元素，其中死对象是标有旧（非当前）白色的对象； 
将所有非死物体变回白色，为下一个收集周期做准备。 返回继续遍历的位置；
如果列表完成，则返回NULL。 （“ * countout”获取遍历的元素数。）
*/
static GCObject **sweeplist (lua_State *L, GCObject **p, int countin,
                             int *countout) {
  global_State *g = G(L);
  int ow = otherwhite(g);
  int i;
  int white = luaC_white(g);  /* current white */
  for (i = 0; *p != NULL && i < countin; i++) {
    GCObject *curr = *p;
    int marked = curr->marked;
    if (isdeadm(ow, marked)) {  /* is 'curr' dead? */
      *p = curr->next;  /* remove 'curr' from list */
      freeobj(L, curr);  /* erase 'curr' */
    }
    else {  /* change mark to 'white' */
      curr->marked = cast_byte((marked & maskcolors) | white);
      p = &curr->next;  /* go to next element */
    }
  }
  if (countout)
    *countout = i;  /* number of elements traversed */
  return (*p == NULL) ? NULL : p;
}


/*
** sweep a list until a live object (or end of list)
扫描列表，直到活动对象（或列表末尾）
*/
static GCObject **sweeptolive (lua_State *L, GCObject **p) {
  GCObject **old = p;
  do {
    p = sweeplist(L, p, 1, NULL);
  } while (p == old);
  return p;
}
```

Lua **Generational Collector 世代垃圾回收器**

```cpp
/*
** Sweep a list of objects, deleting dead ones and turning
** the non dead to old (without changing their colors).
扫描对象列表，删除已死的对象并将未死的对象变旧（不改变其颜色）。
*/
static void sweep2old (lua_State *L, GCObject **p) {
  GCObject *curr;
  while ((curr = *p) != NULL) {
    if (iswhite(curr)) {  /* is 'curr' dead? */
      lua_assert(isdead(G(L), curr));
      *p = curr->next;  /* remove 'curr' from list */
      freeobj(L, curr);  /* erase 'curr' */
    }
    else {  /* all surviving objects become old */
      setage(curr, G_OLD);
      p = &curr->next;  /* go to next element */
    }
  }
}


/*
** Sweep for generational mode. Delete dead objects. (Because the
** collection is not incremental, there are no "new white" objects
** during the sweep. So, any white object must be dead.) For
** non-dead objects, advance their ages and clear the color of
** new objects. (Old objects keep their colors.)
扫描世代模式。 删除死对象。 （由于收集不是增量收集，因此在扫描期间没有“新的白色”对象。
因此，任何白色对象都必须是死亡的。）对于非死亡对象，请延长其寿命并清除新对象的颜色。 
（旧物件保留其颜色。）
*/
static GCObject **sweepgen (lua_State *L, global_State *g, GCObject **p,
                            GCObject *limit) {
  static const lu_byte nextage[] = {
    G_SURVIVAL,  /* from G_NEW */
    G_OLD1,      /* from G_SURVIVAL */
    G_OLD1,      /* from G_OLD0 */
    G_OLD,       /* from G_OLD1 */
    G_OLD,       /* from G_OLD (do not change) */
    G_TOUCHED1,  /* from G_TOUCHED1 (do not change) */
    G_TOUCHED2   /* from G_TOUCHED2 (do not change) */
  };
  int white = luaC_white(g);
  GCObject *curr;
  while ((curr = *p) != limit) {
    if (iswhite(curr)) {  /* is 'curr' dead? */
      lua_assert(!isold(curr) && isdead(g, curr));
      *p = curr->next;  /* remove 'curr' from list */
      freeobj(L, curr);  /* erase 'curr' */
    }
    else {  /* correct mark and age */
      if (getage(curr) == G_NEW)
        curr->marked = cast_byte((curr->marked & maskgencolors) | white);
      setage(curr, nextage[getage(curr)]);
      p = &curr->next;  /* go to next element */
    }
  }
  return p;
}
```

```cpp
/*
** Mark 'OLD1' objects when starting a new young collection.
** Gray objects are already in some gray list, and so will be visited
** in the atomic step.
在开始新的年轻系列时，标记“ OLD1”对象。 灰色对象已经在某个灰色列表中，因此将在原子步骤中进行访问。
*/
static void markold (global_State *g, GCObject *from, GCObject *to) {
  GCObject *p;
  for (p = from; p != to; p = p->next) {
    if (getage(p) == G_OLD1) {
      lua_assert(!iswhite(p));
      if (isblack(p)) {
        black2gray(p);  /* should be '2white', but gray works too */
        reallymarkobject(g, p);
      }
    }
  }
}


/*
** Finish a young-generation collection.
完成年轻一代的回收。
*/
static void finishgencycle (lua_State *L, global_State *g) {
  correctgraylists(g);
  checkSizes(L, g);
  g->gcstate = GCSpropagate;  /* skip restart */
  if (!g->gcemergency)
    callallpendingfinalizers(L);
}


/*
** Does a young collection. First, mark 'OLD1' objects.  (Only survival
** and "recent old" lists can contain 'OLD1' objects. New lists cannot
** contain 'OLD1' objects, at most 'OLD0' objects that were already
** visited when marked old.) Then does the atomic step. Then,
** sweep all lists and advance pointers. Finally, finish the collection.
做一个年轻的回收。 首先，标记“ OLD1”对象。 （只有生存列表和“最近的旧”列表可以包含“ OLD1”对象。
新列表不能包含“ OLD1”对象，当标记为“旧”时，最多不能包含“ OLD0”对象。）然后执行原子步骤。 
然后，扫描所有列表并前进指针。 最后，完成收集。
*/
static void youngcollection (lua_State *L, global_State *g) {
  GCObject **psurvival;  /* to point to first non-dead survival object */
  lua_assert(g->gcstate == GCSpropagate);
  markold(g, g->survival, g->reallyold);
  markold(g, g->finobj, g->finobjrold);
  atomic(L);

  /* sweep nursery and get a pointer to its last live element */
  psurvival = sweepgen(L, g, &g->allgc, g->survival);
  /* sweep 'survival' and 'old' */
  sweepgen(L, g, psurvival, g->reallyold);
  g->reallyold = g->old;
  g->old = *psurvival;  /* 'survival' survivals are old now */
  g->survival = g->allgc;  /* all news are survivals */

  /* repeat for 'finobj' lists */
  psurvival = sweepgen(L, g, &g->finobj, g->finobjsur);
  /* sweep 'survival' and 'old' */
  sweepgen(L, g, psurvival, g->finobjrold);
  g->finobjrold = g->finobjold;
  g->finobjold = *psurvival;  /* 'survival' survivals are old now */
  g->finobjsur = g->finobj;  /* all news are survivals */

  sweepgen(L, g, &g->tobefnz, NULL);

  finishgencycle(L, g);
}
```

```cpp
/*
** Does a major collection after last collection was a "bad collection".
**
** When the program is building a big structure, it allocates lots of
** memory but generates very little garbage. In those scenarios,
** the generational mode just wastes time doing small collections, and
** major collections are frequently what we call a "bad collection", a
** collection that frees too few objects. To avoid the cost of switching
** between generational mode and the incremental mode needed for full
** (major) collections, the collector tries to stay in incremental mode
** after a bad collection, and to switch back to generational mode only
** after a "good" collection (one that traverses less than 9/8 objects
** of the previous one).
** The collector must choose whether to stay in incremental mode or to
** switch back to generational mode before sweeping. At this point, it
** does not know the real memory in use, so it cannot use memory to
** decide whether to return to generational mode. Instead, it uses the
** number of objects traversed (returned by 'atomic') as a proxy. The
** field 'g->lastatomic' keeps this count from the last collection.
** ('g->lastatomic != 0' also means that the last collection was bad.)
**上次收集之后的主要收集是否为“不良收集”。
**
**程式建立大型结构时，会分配许多
**内存，但几乎不会产生垃圾。在这些情况下，
**世代模式只会浪费时间进行小额回收，并且
**主要回收通常是我们所说的“不良回收”，
**收集的对象太少。避免切换成本
**在世代模式和完全所需的增量模式之间
**（主要）集合，收集器尝试保持增量模式
**收集错误后，并且只能切换回分代模式
**在“好”集合之后（遍历少于9/8个对象的集合
前一个**）。
**收集器必须选择是保持增量模式还是保持
**清扫前切换回世代模式。在这一点上
**不知道正在使用的实际内存，因此无法使用内存来
**决定是否返回分代模式。相反，它使用
**遍历的对象数量（由“原子”返回）作为代理。的
**栏位'g-> lastatomic'保留上一个集合的计数。
**（'g-> lastatomic！= 0'也表示最后一次收集是错误的。）
*/
static void stepgenfull (lua_State *L, global_State *g) {
  lu_mem newatomic;  /* count of traversed objects */
  lu_mem lastatomic = g->lastatomic;  /* count from last collection */
  if (g->gckind == KGC_GEN)  /* still in generational mode? */
    enterinc(g);  /* enter incremental mode */
  luaC_runtilstate(L, bitmask(GCSpropagate));  /* start new cycle */
  newatomic = atomic(L);  /* mark everybody */
  if (newatomic < lastatomic + (lastatomic >> 3)) {  /* good collection? */
    atomic2gen(L, g);  /* return to generational mode */
    setminordebt(g);
  }
  else {  /* another bad collection; stay in incremental mode */
    g->GCestimate = gettotalbytes(g);  /* first estimate */;
    entersweep(L);
    luaC_runtilstate(L, bitmask(GCSpause));  /* finish collection */
    setpause(g);
    g->lastatomic = newatomic;
  }
}
/*
** Does a generational "step".
** Usually, this means doing a minor collection and setting the debt to
** make another collection when memory grows 'genminormul'% larger.
**
** However, there are exceptions.  If memory grows 'genmajormul'%
** larger than it was at the end of the last major collection (kept
** in 'g->GCestimate'), the function does a major collection. At the
** end, it checks whether the major collection was able to free a
** decent amount of memory (at least half the growth in memory since
** previous major collection). If so, the collector keeps its state,
** and the next collection will probably be minor again. Otherwise,
** we have what we call a "bad collection". In that case, set the field
** 'g->lastatomic' to signal that fact, so that the next collection will
** go to 'stepgenfull'.
**
** 'GCdebt <= 0' means an explicit call to GC step with "size" zero;
** in that case, do a minor collection.
**做几代人的“步骤”。
**通常，这意味着进行小额收款并将债务设置为
**当内存增大“ genminormul”％时，进行另一个收集。
**
**但是，也有例外。 如果记忆增长'genmajormul'％
**大于上一个主要回收期的末尾（保留）
**在“ g-> GCestimate”中），该函数会进行主要收集。 在
**最后，它检查主要回收是否能够释放
**不错的内存容量（自
**以前的主要回收）。 如果是这样，收集器将保持其状态，
**，下一个集合可能会再次变得较小。 除此以外，
**我们有所谓的“不良回收”。 在这种情况下，请设置字段
**'g-> lastatomic'表示该事实，以便下一个集合将
**转到“ stepgenfull”。
**
**'GCdebt <= 0'表示显式调用GC步，“大小”为零；
**在这种情况下，请进行次要回收。
*/
static void genstep (lua_State *L, global_State *g) {
  if (g->lastatomic != 0)  /* last collection was a bad one? */
    stepgenfull(L, g);  /* do a full step */
  else {
    lu_mem majorbase = g->GCestimate;  /* memory after last major collection */
    lu_mem majorinc = (majorbase / 100) * getgcparam(g->genmajormul);
    if (g->GCdebt > 0 && gettotalbytes(g) > majorbase + majorinc) {
      lu_mem numobjs = fullgen(L, g);  /* do a major collection */
      if (gettotalbytes(g) < majorbase + (majorinc / 2)) {
        /* collected at least half of memory growth since last major
           collection; keep doing minor collections */
        setminordebt(g);
      }
      else {  /* bad collection */
        g->lastatomic = numobjs;  /* signal that last collection was bad */
        setpause(g);  /* do a long wait for next (major) collection */
      }
    }
    else {  /* regular case; do a minor collection */
      youngcollection(L, g);
      setminordebt(g);
      g->GCestimate = majorbase;  /* preserve base value */
    }
  }
  lua_assert(isdecGCmodegen(g));
}
```

Lua GC 控制器

```cpp
/*
** Set the "time" to wait before starting a new GC cycle; cycle will
** start when memory use hits the threshold of ('estimate' * pause /
** PAUSEADJ). (Division by 'estimate' should be OK: it cannot be zero,
** because Lua cannot even start with less than PAUSEADJ bytes).
设置开始新的GC周期之前要等待的“时间”； 当内存使用量达到（“估计” *暂停/ PAUSEADJ）的阈值时，
周期将开始。 （按“ estimate”进行除法应该可以：它不能为零，因为Lua甚至不能以少于PAUSEADJ字节开头）。
*/
static void setpause (global_State *g) {
  l_mem threshold, debt;
  int pause = getgcparam(g->gcpause);
  l_mem estimate = g->GCestimate / PAUSEADJ;  /* adjust 'estimate' */
  lua_assert(estimate > 0);
  threshold = (pause < MAX_LMEM / estimate)  /* overflow? */
            ? estimate * pause  /* no overflow */
            : MAX_LMEM;  /* overflow; truncate to maximum */
  debt = gettotalbytes(g) - threshold;
  if (debt > 0) debt = 0;
  luaE_setdebt(g, debt);
}


/*
** Enter first sweep phase.
** The call to 'sweeptolive' makes the pointer point to an object
** inside the list (instead of to the header), so that the real sweep do
** not need to skip objects created between "now" and the start of the
** real sweep.
**进入第一个扫描阶段。
**对“ sweeptolive”的调用使指针指向一个对象
**在列表内（而不是标题），以便真正进行扫描
**无需跳过在“现在”和开始之间创建的对象
**真正的扫荡。
*/
static void entersweep (lua_State *L) {
  global_State *g = G(L);
  g->gcstate = GCSswpallgc;
  lua_assert(g->sweepgc == NULL);
  g->sweepgc = sweeptolive(L, &g->allgc);
}
```

```cpp
/*
** Call all finalizers of the objects in the given Lua state, and
** then free all objects, except for the main thread.
**在给定的Lua状态下调用对象的所有终结器，然后释放所有对象，主线程除外。
*/
void luaC_freeallobjects (lua_State *L) {
  global_State *g = G(L);
  luaC_changemode(L, KGC_INC);
  separatetobefnz(g, 1);  /* separate all objects with finalizers */
  lua_assert(g->finobj == NULL);
  callallpendingfinalizers(L);
  deletelist(L, g->allgc, obj2gco(g->mainthread));
  deletelist(L, g->finobj, NULL);
  deletelist(L, g->fixedgc, NULL);  /* collect fixed objects */
  lua_assert(g->strt.nuse == 0);
}


static lu_mem atomic (lua_State *L) {
  global_State *g = G(L);
  lu_mem work = 0;
  GCObject *origweak, *origall;
  GCObject *grayagain = g->grayagain;  /* save original list */
  g->grayagain = NULL;
  lua_assert(g->ephemeron == NULL && g->weak == NULL);
  lua_assert(!iswhite(g->mainthread));
  g->gcstate = GCSatomic;
  markobject(g, L);  /* mark running thread */
  /* registry and global metatables may be changed by API */
  markvalue(g, &g->l_registry);
  markmt(g);  /* mark global metatables */
  work += propagateall(g);  /* empties 'gray' list */
  /* remark occasional upvalues of (maybe) dead threads */
  work += remarkupvals(g);
  work += propagateall(g);  /* propagate changes */
  g->gray = grayagain;
  work += propagateall(g);  /* traverse 'grayagain' list */
  convergeephemerons(g);
  /* at this point, all strongly accessible objects are marked. */
  /* Clear values from weak tables, before checking finalizers */
  clearbyvalues(g, g->weak, NULL);
  clearbyvalues(g, g->allweak, NULL);
  origweak = g->weak; origall = g->allweak;
  separatetobefnz(g, 0);  /* separate objects to be finalized */
  work += markbeingfnz(g);  /* mark objects that will be finalized */
  work += propagateall(g);  /* remark, to propagate 'resurrection' */
  convergeephemerons(g);
  /* at this point, all resurrected objects are marked. */
  /* remove dead objects from weak tables */
  clearbykeys(g, g->ephemeron);  /* clear keys from all ephemeron tables */
  clearbykeys(g, g->allweak);  /* clear keys from all 'allweak' tables */
  /* clear values from resurrected weak tables */
  clearbyvalues(g, g->weak, origweak);
  clearbyvalues(g, g->allweak, origall);
  luaS_clearcache(g);
  g->currentwhite = cast_byte(otherwhite(g));  /* flip current white */
  lua_assert(g->gray == NULL);
  return work;  /* estimate of slots marked by 'atomic' */
}


static int sweepstep (lua_State *L, global_State *g,
                      int nextstate, GCObject **nextlist) {
  if (g->sweepgc) {
    l_mem olddebt = g->GCdebt;
    int count;
    g->sweepgc = sweeplist(L, g->sweepgc, GCSWEEPMAX, &count);
    g->GCestimate += g->GCdebt - olddebt;  /* update estimate */
    return count;
  }
  else {  /* enter next state */
    g->gcstate = nextstate;
    g->sweepgc = nextlist;
    return 0;  /* no work done */
  }
}


static lu_mem singlestep (lua_State *L) {
  global_State *g = G(L);
  switch (g->gcstate) {
    case GCSpause: {
      restartcollection(g);
      g->gcstate = GCSpropagate;
      return 1;
    }
    case GCSpropagate: {
      if (g->gray == NULL) {  /* no more gray objects? */
        g->gcstate = GCSenteratomic;  /* finish propagate phase */
        return 0;
      }
      else
        return propagatemark(g);  /* traverse one gray object */
    }
    case GCSenteratomic: {
      lu_mem work = atomic(L);  /* work is what was traversed by 'atomic' */
      entersweep(L);
      g->GCestimate = gettotalbytes(g);  /* first estimate */;
      return work;
    }
    case GCSswpallgc: {  /* sweep "regular" objects */
      return sweepstep(L, g, GCSswpfinobj, &g->finobj);
    }
    case GCSswpfinobj: {  /* sweep objects with finalizers */
      return sweepstep(L, g, GCSswptobefnz, &g->tobefnz);
    }
    case GCSswptobefnz: {  /* sweep objects to be finalized */
      return sweepstep(L, g, GCSswpend, NULL);
    }
    case GCSswpend: {  /* finish sweeps */
      checkSizes(L, g);
      g->gcstate = GCScallfin;
      return 0;
    }
    case GCScallfin: {  /* call remaining finalizers */
      if (g->tobefnz && !g->gcemergency) {
        int n = runafewfinalizers(L, GCFINMAX);
        return n * GCFINALIZECOST;
      }
      else {  /* emergency mode or no more finalizers */
        g->gcstate = GCSpause;  /* finish collection */
        return 0;
      }
    }
    default: lua_assert(0); return 0;
  }
}


/*
** advances the garbage collector until it reaches a state allowed
** by 'statemask'
推进垃圾收集器，直到达到'statemask'允许的状态为止
*/
void luaC_runtilstate (lua_State *L, int statesmask) {
  global_State *g = G(L);
  while (!testbit(statesmask, g->gcstate))
    singlestep(L);
}
```

```cpp
/*
** Performs a basic incremental step. The debt and step size are
** converted from bytes to "units of work"; then the function loops
** running single steps until adding that many units of work or
** finishing a cycle (pause state). Finally, it sets the debt that
** controls when next step will be performed.
**执行基本的增量步骤。 债务和步长为
**从字节转换为“工作单位”； 然后函数循环
**执行单个步骤，直到添加多个工作单元或
**完成一个循环（暂停状态）。 最后，它设定了债务
**控制何时执行下一步。
*/
static void incstep (lua_State *L, global_State *g) {
  int stepmul = (getgcparam(g->gcstepmul) | 1);  /* avoid division by 0 */
  l_mem debt = (g->GCdebt / WORK2MEM) * stepmul;
  l_mem stepsize = (g->gcstepsize <= log2maxs(l_mem))
                 ? ((cast(l_mem, 1) << g->gcstepsize) / WORK2MEM) * stepmul
                 : MAX_LMEM;  /* overflow; keep maximum value */
  do {  /* repeat until pause or enough "credit" (negative debt) */
    lu_mem work = singlestep(L);  /* perform one single step */
    debt -= work;
  } while (debt > -stepsize && g->gcstate != GCSpause);
  if (g->gcstate == GCSpause)
    setpause(g);  /* pause until next cycle */
  else {
    debt = (debt / stepmul) * WORK2MEM;  /* convert 'work units' to bytes */
    luaE_setdebt(g, debt);
  }
}

/*
** performs a basic GC step if collector is running
如果收集器正在运行，则执行基本的GC步骤
*/
void luaC_step (lua_State *L) {
  global_State *g = G(L);
  lua_assert(!g->gcemergency);
  if (g->gcrunning) {  /* running? */
    if(isdecGCmodegen(g))
      genstep(L, g);
    else
      incstep(L, g);
  }
}
```

## Lua 虚拟机

以下为`lvm.c`和`lvm.h`

Lua虚拟机API函数

```cpp
LUAI_FUNC int luaV_equalobj (lua_State *L, const TValue *t1, const TValue *t2);
LUAI_FUNC int luaV_lessthan (lua_State *L, const TValue *l, const TValue *r);
LUAI_FUNC int luaV_lessequal (lua_State *L, const TValue *l, const TValue *r);
LUAI_FUNC int luaV_tonumber_ (const TValue *obj, lua_Number *n);
LUAI_FUNC int luaV_tointeger (const TValue *obj, lua_Integer *p, int mode);
LUAI_FUNC int luaV_tointegerns (const TValue *obj, lua_Integer *p, int mode);
LUAI_FUNC int luaV_flttointeger (lua_Number n, lua_Integer *p, int mode);
LUAI_FUNC void luaV_finishget (lua_State *L, const TValue *t, TValue *key,
                               StkId val, const TValue *slot);
LUAI_FUNC void luaV_finishset (lua_State *L, const TValue *t, TValue *key,
                               TValue *val, const TValue *slot);
LUAI_FUNC void luaV_finishOp (lua_State *L);
LUAI_FUNC void luaV_execute (lua_State *L, CallInfo *ci);
LUAI_FUNC void luaV_concat (lua_State *L, int total);
LUAI_FUNC lua_Integer luaV_idiv (lua_State *L, lua_Integer x, lua_Integer y);
LUAI_FUNC lua_Integer luaV_mod (lua_State *L, lua_Integer x, lua_Integer y);
LUAI_FUNC lua_Number luaV_modf (lua_State *L, lua_Number x, lua_Number y);
LUAI_FUNC lua_Integer luaV_shiftl (lua_Integer x, lua_Integer y);
LUAI_FUNC void luaV_objlen (lua_State *L, StkId ra, const TValue *rb);
```

```cpp
void luaV_execute (lua_State *L, CallInfo *ci) {
  LClosure *cl;
  TValue *k;
  StkId base;
  const Instruction *pc;
  int trap;
#if LUA_USE_JUMPTABLE
#include "ljumptab.h"
#endif
 tailcall:
  trap = L->hookmask;
  cl = clLvalue(s2v(ci->func));
  k = cl->p->k;
  pc = ci->u.l.savedpc;
  if (trap) {
    if (cl->p->is_vararg)
      trap = 0;  /* hooks will start after VARARGPREP instruction */
    else if (pc == cl->p->code)  /* first instruction (not resuming)? */
      luaD_hookcall(L, ci);
    ci->u.l.trap = 1;  /* there may be other hooks */
  }
  base = ci->func + 1;
  /* main loop of interpreter */
  for (;;) {
    Instruction i;  /* instruction being executed */
    StkId ra;  /* instruction's A register */
    vmfetch();
    lua_assert(base == ci->func + 1);
    lua_assert(base <= L->top && L->top < L->stack + L->stacksize);
    /* invalidate top for instructions not expecting it */
    lua_assert(isIT(i) || (L->top = base));
    vmdispatch (GET_OPCODE(i)) {
      vmcase(OP_MOVE) {
        setobjs2s(L, ra, RB(i));
        vmbreak;
      }
      vmcase(OP_LOADI) {
        lua_Integer b = GETARG_sBx(i);
        setivalue(s2v(ra), b);
        vmbreak;
      }
      vmcase(OP_LOADF) {
        int b = GETARG_sBx(i);
        setfltvalue(s2v(ra), cast_num(b));
        vmbreak;
      }
      vmcase(OP_LOADK) {
        TValue *rb = k + GETARG_Bx(i);
        setobj2s(L, ra, rb);
        vmbreak;
      }
      vmcase(OP_LOADKX) {
        TValue *rb;
        rb = k + GETARG_Ax(*pc); pc++;
        setobj2s(L, ra, rb);
        vmbreak;
      }
      vmcase(OP_LOADBOOL) {
        setbvalue(s2v(ra), GETARG_B(i));
        if (GETARG_C(i)) pc++;  /* skip next instruction (if C) */
        vmbreak;
      }
      vmcase(OP_LOADNIL) {
        int b = GETARG_B(i);
        do {
          setnilvalue(s2v(ra++));
        } while (b--);
        vmbreak;
      }
      vmcase(OP_GETUPVAL) {
        int b = GETARG_B(i);
        setobj2s(L, ra, cl->upvals[b]->v);
        vmbreak;
      }
      vmcase(OP_SETUPVAL) {
        UpVal *uv = cl->upvals[GETARG_B(i)];
        setobj(L, uv->v, s2v(ra));
        luaC_barrier(L, uv, s2v(ra));
        vmbreak;
      }
      vmcase(OP_GETTABUP) {
        const TValue *slot;
        TValue *upval = cl->upvals[GETARG_B(i)]->v;
        TValue *rc = KC(i);
        TString *key = tsvalue(rc);  /* key must be a string */
        if (luaV_fastget(L, upval, key, slot, luaH_getshortstr)) {
          setobj2s(L, ra, slot);
        }
        else
          Protect(luaV_finishget(L, upval, rc, ra, slot));
        vmbreak;
      }
      vmcase(OP_GETTABLE) {
        const TValue *slot;
        TValue *rb = vRB(i);
        TValue *rc = vRC(i);
        lua_Unsigned n;
        if (ttisinteger(rc)  /* fast track for integers? */
            ? (cast_void(n = ivalue(rc)), luaV_fastgeti(L, rb, n, slot))
            : luaV_fastget(L, rb, rc, slot, luaH_get)) {
          setobj2s(L, ra, slot);
        }
        else
          Protect(luaV_finishget(L, rb, rc, ra, slot));
        vmbreak;
      }
      vmcase(OP_GETI) {
        const TValue *slot;
        TValue *rb = vRB(i);
        int c = GETARG_C(i);
        if (luaV_fastgeti(L, rb, c, slot)) {
          setobj2s(L, ra, slot);
        }
        else {
          TValue key;
          setivalue(&key, c);
          Protect(luaV_finishget(L, rb, &key, ra, slot));
        }
        vmbreak;
      }
      vmcase(OP_GETFIELD) {
        const TValue *slot;
        TValue *rb = vRB(i);
        TValue *rc = KC(i);
        TString *key = tsvalue(rc);  /* key must be a string */
        if (luaV_fastget(L, rb, key, slot, luaH_getshortstr)) {
          setobj2s(L, ra, slot);
        }
        else
          Protect(luaV_finishget(L, rb, rc, ra, slot));
        vmbreak;
      }
      vmcase(OP_SETTABUP) {
        const TValue *slot;
        TValue *upval = cl->upvals[GETARG_A(i)]->v;
        TValue *rb = KB(i);
        TValue *rc = RKC(i);
        TString *key = tsvalue(rb);  /* key must be a string */
        if (luaV_fastget(L, upval, key, slot, luaH_getshortstr)) {
          luaV_finishfastset(L, upval, slot, rc);
        }
        else
          Protect(luaV_finishset(L, upval, rb, rc, slot));
        vmbreak;
      }
      vmcase(OP_SETTABLE) {
        const TValue *slot;
        TValue *rb = vRB(i);  /* key (table is in 'ra') */
        TValue *rc = RKC(i);  /* value */
        lua_Unsigned n;
        if (ttisinteger(rb)  /* fast track for integers? */
            ? (cast_void(n = ivalue(rb)), luaV_fastgeti(L, s2v(ra), n, slot))
            : luaV_fastget(L, s2v(ra), rb, slot, luaH_get)) {
          luaV_finishfastset(L, s2v(ra), slot, rc);
        }
        else
          Protect(luaV_finishset(L, s2v(ra), rb, rc, slot));
        vmbreak;
      }
      vmcase(OP_SETI) {
        const TValue *slot;
        int c = GETARG_B(i);
        TValue *rc = RKC(i);
        if (luaV_fastgeti(L, s2v(ra), c, slot)) {
          luaV_finishfastset(L, s2v(ra), slot, rc);
        }
        else {
          TValue key;
          setivalue(&key, c);
          Protect(luaV_finishset(L, s2v(ra), &key, rc, slot));
        }
        vmbreak;
      }
      vmcase(OP_SETFIELD) {
        const TValue *slot;
        TValue *rb = KB(i);
        TValue *rc = RKC(i);
        TString *key = tsvalue(rb);  /* key must be a string */
        if (luaV_fastget(L, s2v(ra), key, slot, luaH_getshortstr)) {
          luaV_finishfastset(L, s2v(ra), slot, rc);
        }
        else
          Protect(luaV_finishset(L, s2v(ra), rb, rc, slot));
        vmbreak;
      }
      vmcase(OP_NEWTABLE) {
        int b = GETARG_B(i);  /* log2(hash size) + 1 */
        int c = GETARG_C(i);  /* array size */
        Table *t;
        if (b > 0)
          b = 1 << (b - 1);  /* size is 2^(b - 1) */
        if (TESTARG_k(i))
          c += GETARG_Ax(*pc) * (MAXARG_C + 1);
        pc++;  /* skip extra argument */
        L->top = ra + 1;  /* correct top in case of emergency GC */
        t = luaH_new(L);  /* memory allocation */
        sethvalue2s(L, ra, t);
        if (b != 0 || c != 0)
          luaH_resize(L, t, c, b);  /* idem */
        checkGC(L, ra + 1);
        vmbreak;
      }
      vmcase(OP_SELF) {
        const TValue *slot;
        TValue *rb = vRB(i);
        TValue *rc = RKC(i);
        TString *key = tsvalue(rc);  /* key must be a string */
        setobj2s(L, ra + 1, rb);
        if (luaV_fastget(L, rb, key, slot, luaH_getstr)) {
          setobj2s(L, ra, slot);
        }
        else
          Protect(luaV_finishget(L, rb, rc, ra, slot));
        vmbreak;
      }
      vmcase(OP_ADDI) {
        op_arithI(L, l_addi, luai_numadd, TM_ADD, GETARG_k(i));
        vmbreak;
      }
      vmcase(OP_ADDK) {
        op_arithK(L, l_addi, luai_numadd, GETARG_k(i));
        vmbreak;
      }
      vmcase(OP_SUBK) {
        op_arithK(L, l_subi, luai_numsub, 0);
        vmbreak;
      }
      vmcase(OP_MULK) {
        op_arithK(L, l_muli, luai_nummul, GETARG_k(i));
        vmbreak;
      }
      vmcase(OP_MODK) {
        op_arithK(L, luaV_mod, luaV_modf, 0);
        vmbreak;
      }
      vmcase(OP_POWK) {
        op_arithfK(L, luai_numpow);
        vmbreak;
      }
      vmcase(OP_DIVK) {
        op_arithfK(L, luai_numdiv);
        vmbreak;
      }
      vmcase(OP_IDIVK) {
        op_arithK(L, luaV_idiv, luai_numidiv, 0);
        vmbreak;
      }
      vmcase(OP_BANDK) {
        op_bitwiseK(L, l_band);
        vmbreak;
      }
      vmcase(OP_BORK) {
        op_bitwiseK(L, l_bor);
        vmbreak;
      }
      vmcase(OP_BXORK) {
        op_bitwiseK(L, l_bxor);
        vmbreak;
      }
      vmcase(OP_SHRI) {
        TValue *rb = vRB(i);
        int ic = GETARG_sC(i);
        lua_Integer ib;
        if (tointegerns(rb, &ib)) {
          pc++; setivalue(s2v(ra), luaV_shiftl(ib, -ic));
        }
        vmbreak;
      }
      vmcase(OP_SHLI) {
        TValue *rb = vRB(i);
        int ic = GETARG_sC(i);
        lua_Integer ib;
        if (tointegerns(rb, &ib)) {
          pc++; setivalue(s2v(ra), luaV_shiftl(ic, ib));
        }
        vmbreak;
      }
      vmcase(OP_ADD) {
        op_arith(L, l_addi, luai_numadd);
        vmbreak;
      }
      vmcase(OP_SUB) {
        op_arith(L, l_subi, luai_numsub);
        vmbreak;
      }
      vmcase(OP_MUL) {
        op_arith(L, l_muli, luai_nummul);
        vmbreak;
      }
      vmcase(OP_MOD) {
        op_arith(L, luaV_mod, luaV_modf);
        vmbreak;
      }
      vmcase(OP_POW) {
        op_arithf(L, luai_numpow);
        vmbreak;
      }
      vmcase(OP_DIV) {  /* float division (always with floats) */
        op_arithf(L, luai_numdiv);
        vmbreak;
      }
      vmcase(OP_IDIV) {  /* floor division */
        op_arith(L, luaV_idiv, luai_numidiv);
        vmbreak;
      }
      vmcase(OP_BAND) {
        op_bitwise(L, l_band);
        vmbreak;
      }
      vmcase(OP_BOR) {
        op_bitwise(L, l_bor);
        vmbreak;
      }
      vmcase(OP_BXOR) {
        op_bitwise(L, l_bxor);
        vmbreak;
      }
      vmcase(OP_SHR) {
        op_bitwise(L, luaV_shiftr);
        vmbreak;
      }
      vmcase(OP_SHL) {
        op_bitwise(L, luaV_shiftl);
        vmbreak;
      }
      vmcase(OP_MMBIN) {
        Instruction pi = *(pc - 2);  /* original arith. expression */
        TValue *rb = vRB(i);
        TMS tm = (TMS)GETARG_C(i);
        StkId result = RA(pi);
        lua_assert(OP_ADD <= GET_OPCODE(pi) && GET_OPCODE(pi) <= OP_SHR);
        Protect(luaT_trybinTM(L, s2v(ra), rb, result, tm));
        vmbreak;
      }
      vmcase(OP_MMBINI) {
        Instruction pi = *(pc - 2);  /* original arith. expression */
        int imm = GETARG_sB(i);
        TMS tm = (TMS)GETARG_C(i);
        int flip = GETARG_k(i);
        StkId result = RA(pi);
        Protect(luaT_trybiniTM(L, s2v(ra), imm, flip, result, tm));
        vmbreak;
      }
      vmcase(OP_MMBINK) {
        Instruction pi = *(pc - 2);  /* original arith. expression */
        TValue *imm = KB(i);
        TMS tm = (TMS)GETARG_C(i);
        int flip = GETARG_k(i);
        StkId result = RA(pi);
        Protect(luaT_trybinassocTM(L, s2v(ra), imm, flip, result, tm));
        vmbreak;
      }
      vmcase(OP_UNM) {
        TValue *rb = vRB(i);
        lua_Number nb;
        if (ttisinteger(rb)) {
          lua_Integer ib = ivalue(rb);
          setivalue(s2v(ra), intop(-, 0, ib));
        }
        else if (tonumberns(rb, nb)) {
          setfltvalue(s2v(ra), luai_numunm(L, nb));
        }
        else
          Protect(luaT_trybinTM(L, rb, rb, ra, TM_UNM));
        vmbreak;
      }
      vmcase(OP_BNOT) {
        TValue *rb = vRB(i);
        lua_Integer ib;
        if (tointegerns(rb, &ib)) {
          setivalue(s2v(ra), intop(^, ~l_castS2U(0), ib));
        }
        else
          Protect(luaT_trybinTM(L, rb, rb, ra, TM_BNOT));
        vmbreak;
      }
      vmcase(OP_NOT) {
        TValue *rb = vRB(i);
        int nrb = l_isfalse(rb);  /* next assignment may change this value */
        setbvalue(s2v(ra), nrb);
        vmbreak;
      }
      vmcase(OP_LEN) {
        Protect(luaV_objlen(L, ra, vRB(i)));
        vmbreak;
      }
      vmcase(OP_CONCAT) {
        int n = GETARG_B(i);  /* number of elements to concatenate */
        L->top = ra + n;  /* mark the end of concat operands */
        ProtectNT(luaV_concat(L, n));
        checkGC(L, L->top); /* 'luaV_concat' ensures correct top */
        vmbreak;
      }
      vmcase(OP_CLOSE) {
        Protect(luaF_close(L, ra, LUA_OK));
        vmbreak;
      }
      vmcase(OP_TBC) {
        /* create new to-be-closed upvalue */
        halfProtect(luaF_newtbcupval(L, ra));
        vmbreak;
      }
      vmcase(OP_JMP) {
        dojump(ci, i, 0);
        vmbreak;
      }
      vmcase(OP_EQ) {
        int cond;
        TValue *rb = vRB(i);
        Protect(cond = luaV_equalobj(L, s2v(ra), rb));
        docondjump();
        vmbreak;
      }
      vmcase(OP_LT) {
        op_order(L, l_lti, LTnum, lessthanothers);
        vmbreak;
      }
      vmcase(OP_LE) {
        op_order(L, l_lei, LEnum, lessequalothers);
        vmbreak;
      }
      vmcase(OP_EQK) {
        TValue *rb = KB(i);
        /* basic types do not use '__eq'; we can use raw equality */
        int cond = luaV_equalobj(NULL, s2v(ra), rb);
        docondjump();
        vmbreak;
      }
      vmcase(OP_EQI) {
        int cond;
        int im = GETARG_sB(i);
        if (ttisinteger(s2v(ra)))
          cond = (ivalue(s2v(ra)) == im);
        else if (ttisfloat(s2v(ra)))
          cond = luai_numeq(fltvalue(s2v(ra)), cast_num(im));
        else
          cond = 0;  /* other types cannot be equal to a number */
        docondjump();
        vmbreak;
      }
      vmcase(OP_LTI) {
        op_orderI(L, l_lti, luai_numlt, 0, TM_LT);
        vmbreak;
      }
      vmcase(OP_LEI) {
        op_orderI(L, l_lei, luai_numle, 0, TM_LE);
        vmbreak;
      }
      vmcase(OP_GTI) {
        op_orderI(L, l_gti, luai_numgt, 1, TM_LT);
        vmbreak;
      }
      vmcase(OP_GEI) {
        op_orderI(L, l_gei, luai_numge, 1, TM_LE);
        vmbreak;
      }
      vmcase(OP_TEST) {
        int cond = !l_isfalse(s2v(ra));
        docondjump();
        vmbreak;
      }
      vmcase(OP_TESTSET) {
        TValue *rb = vRB(i);
        if (l_isfalse(rb) == GETARG_k(i))
          pc++;
        else {
          setobj2s(L, ra, rb);
          donextjump(ci);
        }
        vmbreak;
      }
      vmcase(OP_CALL) {
        int b = GETARG_B(i);
        int nresults = GETARG_C(i) - 1;
        if (b != 0)  /* fixed number of arguments? */
          L->top = ra + b;  /* top signals number of arguments */
        /* else previous instruction set top */
        ProtectNT(luaD_call(L, ra, nresults));
        vmbreak;
      }
      vmcase(OP_TAILCALL) {
        int b = GETARG_B(i);  /* number of arguments + 1 (function) */
        int nparams1 = GETARG_C(i);
        /* delat is virtual 'func' - real 'func' (vararg functions) */
        int delta = (nparams1) ? ci->u.l.nextraargs + nparams1 : 0;
        if (b != 0)
          L->top = ra + b;
        else  /* previous instruction set top */
          b = cast_int(L->top - ra);
        savepc(ci);  /* some calls here can raise errors */
        if (TESTARG_k(i)) {
          /* close upvalues from current call; the compiler ensures
             that there are no to-be-closed variables here, so this
             call cannot change the stack */
          luaF_close(L, base, NOCLOSINGMETH);
          lua_assert(base == ci->func + 1);
        }
        while (!ttisfunction(s2v(ra))) {  /* not a function? */
          luaD_tryfuncTM(L, ra);  /* try '__call' metamethod */
          b++;  /* there is now one extra argument */
          checkstackp(L, 1, ra);
        }
        if (!ttisLclosure(s2v(ra))) {  /* C function? */
          luaD_call(L, ra, LUA_MULTRET);  /* call it */
          updatetrap(ci);
          updatestack(ci);  /* stack may have been relocated */
          ci->func -= delta;
          luaD_poscall(L, ci, cast_int(L->top - ra));
          return;
        }
        ci->func -= delta;
        luaD_pretailcall(L, ci, ra, b);  /* prepare call frame */
        goto tailcall;
      }
      vmcase(OP_RETURN) {
        int n = GETARG_B(i) - 1;  /* number of results */
        int nparams1 = GETARG_C(i);
        if (n < 0)  /* not fixed? */
          n = cast_int(L->top - ra);  /* get what is available */
        savepc(ci);
        if (TESTARG_k(i)) {  /* may there be open upvalues? */
          if (L->top < ci->top)
            L->top = ci->top;
          luaF_close(L, base, LUA_OK);
          updatetrap(ci);
          updatestack(ci);
        }
        if (nparams1)  /* vararg function? */
          ci->func -= ci->u.l.nextraargs + nparams1;
        L->top = ra + n;  /* set call for 'luaD_poscall' */
        luaD_poscall(L, ci, n);
        return;
      }
      vmcase(OP_RETURN0) {
        if (L->hookmask) {
          L->top = ra;
          halfProtectNT(luaD_poscall(L, ci, 0));  /* no hurry... */
        }
        else {  /* do the 'poscall' here */
          int nres = ci->nresults;
          L->ci = ci->previous;  /* back to caller */
          L->top = base - 1;
          while (nres-- > 0)
            setnilvalue(s2v(L->top++));  /* all results are nil */
        }
        return;
      }
      vmcase(OP_RETURN1) {
        if (L->hookmask) {
          L->top = ra + 1;
          halfProtectNT(luaD_poscall(L, ci, 1));  /* no hurry... */
        }
        else {  /* do the 'poscall' here */
          int nres = ci->nresults;
          L->ci = ci->previous;  /* back to caller */
          if (nres == 0)
            L->top = base - 1;  /* asked for no results */
          else {
            setobjs2s(L, base - 1, ra);  /* at least this result */
            L->top = base;
            while (--nres > 0)  /* complete missing results */
              setnilvalue(s2v(L->top++));
          }
        }
        return;
      }
      vmcase(OP_FORLOOP) {
        if (ttisinteger(s2v(ra + 2))) {  /* integer loop? */
          lua_Unsigned count = l_castS2U(ivalue(s2v(ra + 1)));
          if (count > 0) {  /* still more iterations? */
            lua_Integer step = ivalue(s2v(ra + 2));
            lua_Integer idx = ivalue(s2v(ra));  /* internal index */
            chgivalue(s2v(ra + 1), count - 1);  /* update counter */
            idx = intop(+, idx, step);  /* add step to index */
            chgivalue(s2v(ra), idx);  /* update internal index */
            setivalue(s2v(ra + 3), idx);  /* and control variable */
            pc -= GETARG_Bx(i);  /* jump back */
          }
        }
        else {  /* floating loop */
          lua_Number step = fltvalue(s2v(ra + 2));
          lua_Number limit = fltvalue(s2v(ra + 1));
          lua_Number idx = fltvalue(s2v(ra));
          idx = luai_numadd(L, idx, step);  /* increment index */
          if (luai_numlt(0, step) ? luai_numle(idx, limit)
                                  : luai_numle(limit, idx)) {
            chgfltvalue(s2v(ra), idx);  /* update internal index */
            setfltvalue(s2v(ra + 3), idx);  /* and control variable */
            pc -= GETARG_Bx(i);  /* jump back */
          }
        }
        updatetrap(ci);  /* allows a signal to break the loop */
        vmbreak;
      }
      vmcase(OP_FORPREP) {
        TValue *pinit = s2v(ra);
        TValue *plimit = s2v(ra + 1);
        TValue *pstep = s2v(ra + 2);
        savestate(L, ci);  /* in case of errors */
        if (ttisinteger(pinit) && ttisinteger(pstep)) { /* integer loop? */
          lua_Integer init = ivalue(pinit);
          lua_Integer step = ivalue(pstep);
          lua_Integer limit;
          if (step == 0)
            luaG_runerror(L, "'for' step is zero");
          setivalue(s2v(ra + 3), init);  /* control variable */
          if (forlimit(L, init, plimit, &limit, step))
            pc += GETARG_Bx(i) + 1;  /* skip the loop */
          else {  /* prepare loop counter */
            lua_Unsigned count;
            if (step > 0) {  /* ascending loop? */
              count = l_castS2U(limit) - l_castS2U(init);
              if (step != 1)  /* avoid division in the too common case */
                count /= l_castS2U(step);
            }
            else {  /* step < 0; descending loop */
              count = l_castS2U(init) - l_castS2U(limit);
              /* 'step+1' avoids negating 'mininteger' */
              count /= l_castS2U(-(step + 1)) + 1u;
            }
            /* store the counter in place of the limit (which won't be
               needed anymore */
            setivalue(plimit, l_castU2S(count));
          }
        }
        else {  /* try making all values floats */
          lua_Number init; lua_Number limit; lua_Number step;
          if (unlikely(!tonumber(plimit, &limit)))
            luaG_forerror(L, plimit, "limit");
          if (unlikely(!tonumber(pstep, &step)))
            luaG_forerror(L, pstep, "step");
          if (unlikely(!tonumber(pinit, &init)))
            luaG_forerror(L, pinit, "initial value");
          if (step == 0)
            luaG_runerror(L, "'for' step is zero");
          if (luai_numlt(0, step) ? luai_numlt(limit, init)
                                   : luai_numlt(init, limit))
            pc += GETARG_Bx(i) + 1;  /* skip the loop */
          else {
            /* make sure internal values are all float */
            setfltvalue(plimit, limit);
            setfltvalue(pstep, step);
            setfltvalue(s2v(ra), init);  /* internal index */
            setfltvalue(s2v(ra + 3), init);  /* control variable */
          }
        }
        vmbreak;
      }
      vmcase(OP_TFORPREP) {
        /* create to-be-closed upvalue (if needed) */
        halfProtect(luaF_newtbcupval(L, ra + 3));
        pc += GETARG_Bx(i);
        i = *(pc++);  /* go to next instruction */
        lua_assert(GET_OPCODE(i) == OP_TFORCALL && ra == RA(i));
        goto l_tforcall;
      }
      vmcase(OP_TFORCALL) {
       l_tforcall:
        /* 'ra' has the iterator function, 'ra + 1' has the state,
           'ra + 2' has the control variable, and 'ra + 3' has the
           to-be-closed variable. The call will use the stack after
           these values (starting at 'ra + 4')
        */
        /* push function, state, and control variable */
        memcpy(ra + 4, ra, 3 * sizeof(*ra));
        L->top = ra + 4 + 3;
        ProtectNT(luaD_call(L, ra + 4, GETARG_C(i)));  /* do the call */
        updatestack(ci);  /* stack may have changed */
        i = *(pc++);  /* go to next instruction */
        lua_assert(GET_OPCODE(i) == OP_TFORLOOP && ra == RA(i));
        goto l_tforloop;
      }
      vmcase(OP_TFORLOOP) {
        l_tforloop:
        if (!ttisnil(s2v(ra + 4))) {  /* continue loop? */
          setobjs2s(L, ra + 2, ra + 4);  /* save control variable */
          pc -= GETARG_Bx(i);  /* jump back */
        }
        vmbreak;
      }
      vmcase(OP_SETLIST) {
        int n = GETARG_B(i);
        unsigned int last = GETARG_C(i);
        Table *h = hvalue(s2v(ra));
        if (n == 0)
          n = cast_int(L->top - ra) - 1;  /* get up to the top */
        else
          L->top = ci->top;  /* correct top in case of emergency GC */
        last += n;
        if (TESTARG_k(i)) {
          last += GETARG_Ax(*pc) * (MAXARG_C + 1);
          pc++;
        }
        if (last > luaH_realasize(h))  /* needs more space? */
          luaH_resizearray(L, h, last);  /* preallocate it at once */
        for (; n > 0; n--) {
          TValue *val = s2v(ra + n);
          setobj2t(L, &h->array[last - 1], val);
          last--;
          luaC_barrierback(L, obj2gco(h), val);
        }
        vmbreak;
      }
      vmcase(OP_CLOSURE) {
        Proto *p = cl->p->p[GETARG_Bx(i)];
        halfProtect(pushclosure(L, p, cl->upvals, base, ra));
        checkGC(L, ra + 1);
        vmbreak;
      }
      vmcase(OP_VARARG) {
        int n = GETARG_C(i) - 1;  /* required results */
        Protect(luaT_getvarargs(L, ci, ra, n));
        vmbreak;
      }
      vmcase(OP_VARARGPREP) {
        luaT_adjustvarargs(L, GETARG_A(i), ci, cl->p);
        updatetrap(ci);
        if (trap) {
          luaD_hookcall(L, ci);
          L->oldpc = pc + 1;  /* next opcode will be seen as a "new" line */
        }
        updatebase(ci);  /* function has new base after adjustment */
        vmbreak;
      }
      vmcase(OP_EXTRAARG) {
        lua_assert(0);
        vmbreak;
      }
    }
  }
}
```

### Lua 堆栈和调用结构

以下为`ldo.c`和`ldo.h`

Lua 堆栈结构API函数

```cpp
LUAI_FUNC void luaD_seterrorobj (lua_State *L, int errcode, StkId oldtop);
LUAI_FUNC int luaD_protectedparser (lua_State *L, ZIO *z, const char *name,
                                                  const char *mode);
LUAI_FUNC void luaD_hook (lua_State *L, int event, int line,
                                        int fTransfer, int nTransfer);
LUAI_FUNC void luaD_hookcall (lua_State *L, CallInfo *ci);
LUAI_FUNC void luaD_pretailcall (lua_State *L, CallInfo *ci, StkId func, int n);
LUAI_FUNC void luaD_call (lua_State *L, StkId func, int nResults);
LUAI_FUNC void luaD_callnoyield (lua_State *L, StkId func, int nResults);
LUAI_FUNC void luaD_tryfuncTM (lua_State *L, StkId func);
LUAI_FUNC int luaD_pcall (lua_State *L, Pfunc func, void *u,
                                        ptrdiff_t oldtop, ptrdiff_t ef);
LUAI_FUNC void luaD_poscall (lua_State *L, CallInfo *ci, int nres);
LUAI_FUNC int luaD_reallocstack (lua_State *L, int newsize, int raiseerror);
LUAI_FUNC int luaD_growstack (lua_State *L, int n, int raiseerror);
LUAI_FUNC void luaD_shrinkstack (lua_State *L);
LUAI_FUNC void luaD_inctop (lua_State *L);

LUAI_FUNC l_noret luaD_throw (lua_State *L, int errcode);
LUAI_FUNC int luaD_rawrunprotected (lua_State *L, Pfunc f, void *ud);
```

```cpp
/* chain list of long jump buffers
长跳转缓冲器链表 */
struct lua_longjmp {
  struct lua_longjmp *previous;
  luai_jmpbuf b;
  volatile int status;  /* error code 错误码 */
};
```

```cpp
void luaD_seterrorobj (lua_State *L, int errcode, StkId oldtop) {
  switch (errcode) {
    case LUA_ERRMEM: {  /* memory error? 内存错误？*/
      setsvalue2s(L, oldtop, G(L)->memerrmsg); /* reuse preregistered msg. 重用预注册信息。 */
      break;
    }
    case LUA_ERRERR: {
      setsvalue2s(L, oldtop, luaS_newliteral(L, "error in error handling"));
      break;
    }
    case CLOSEPROTECT: {
      setnilvalue(s2v(oldtop));  /* no error message 没有错误讯息 */
      break;
    }
    default: {
      setobjs2s(L, oldtop, L->top - 1);  /* error message on current top 当前顶部的错误消息 */
      break;
    }
  }
  L->top = oldtop + 1;
}
```

Lua 堆栈和调用结构示例

```cpp
void luaD_seterrorobj (lua_State *L, int errcode, StkId oldtop) {
  switch (errcode) {
    case LUA_ERRMEM: {  /* memory error? 内存错误？*/
      setsvalue2s(L, oldtop, G(L)->memerrmsg); /* reuse preregistered msg. 重用预注册信息。 */
      break;
    }
    case LUA_ERRERR: {
      setsvalue2s(L, oldtop, luaS_newliteral(L, "error in error handling"));
      break;
    }
    case CLOSEPROTECT: {
      setnilvalue(s2v(oldtop));  /* no error message 没有错误讯息 */
      break;
    }
    default: {
      setobjs2s(L, oldtop, L->top - 1);  /* error message on current top 当前顶部的错误消息 */
      break;
    }
  }
  L->top = oldtop + 1;
}
```

```cpp
/*
** {==================================================================
** Stack reallocation 堆栈重新分配
** ===================================================================
*/
static void correctstack (lua_State *L, StkId oldstack, StkId newstack) {
  CallInfo *ci;
  UpVal *up;
  if (oldstack == newstack)
    return;  /* stack address did not change */
  L->top = (L->top - oldstack) + newstack;
  for (up = L->openupval; up != NULL; up = up->u.open.next)
    up->v = s2v((uplevel(up) - oldstack) + newstack);
  for (ci = L->ci; ci != NULL; ci = ci->previous) {
    ci->top = (ci->top - oldstack) + newstack;
    ci->func = (ci->func - oldstack) + newstack;
    if (isLua(ci))
      ci->u.l.trap = 1;  /* signal to update 'trap' in 'luaV_execute' */
  }
}

/* some space for error handling */
#define ERRORSTACKSIZE	(LUAI_MAXSTACK + 200)

int luaD_reallocstack (lua_State *L, int newsize, int raiseerror) {
  int lim = L->stacksize;
  StkId newstack = luaM_reallocvector(L, L->stack, lim, newsize, StackValue);
  lua_assert(newsize <= LUAI_MAXSTACK || newsize == ERRORSTACKSIZE);
  lua_assert(L->stack_last - L->stack == L->stacksize - EXTRA_STACK);
  if (unlikely(newstack == NULL)) {  /* reallocation failed? */
    if (raiseerror)
      luaM_error(L);
    else return 0;  /* do not raise an error */
  }
  for (; lim < newsize; lim++)
    setnilvalue(s2v(newstack + lim)); /* erase new segment */
  correctstack(L, L->stack, newstack);
  L->stack = newstack;
  L->stacksize = newsize;
  L->stack_last = L->stack + newsize - EXTRA_STACK;
  return 1;
}
```

```cpp
/* 收缩堆栈 */
void luaD_shrinkstack (lua_State *L) {
  int inuse = stackinuse(L);
  int goodsize = inuse + (inuse / 8) + 2*EXTRA_STACK;
  if (goodsize > LUAI_MAXSTACK)
    goodsize = LUAI_MAXSTACK;  /* respect stack limit */
  /* if thread is currently not handling a stack overflow and its
     good size is smaller than current size, shrink its stack 
     如果线程当前未处理堆栈溢出，并且其正常大小小于当前大小，则缩小其堆栈 */
  if (inuse <= (LUAI_MAXSTACK - EXTRA_STACK) &&
      goodsize < L->stacksize)
    luaD_reallocstack(L, goodsize, 0);  /* ok if that fails */
  else  /* don't change stack */
    condmovestack(L,{},{});  /* (change only for debugging) */
  luaE_shrinkCI(L);  /* shrink CI list */
}
```

```cpp
/*
** Call a hook for the given event. Make sure there is a hook to be
** called. (Both 'L->hook' and 'L->hookmask', which trigger this
** function, can be changed asynchronously by signals.)
调用给定事件的钩子。 确保有一个挂钩被调用。
（触发该功能的'L-> hook'和'L-> hookmask'均可通过信号异步更改。）
*/
void luaD_hook (lua_State *L, int event, int line,
                              int ftransfer, int ntransfer) {
  lua_Hook hook = L->hook;
  if (hook && L->allowhook) {  /* make sure there is a hook 确定存在一个钩子 */
    int mask = CIST_HOOKED;
    CallInfo *ci = L->ci;
    ptrdiff_t top = savestack(L, L->top);
    ptrdiff_t ci_top = savestack(L, ci->top);
    lua_Debug ar;
    ar.event = event;
    ar.currentline = line;
    ar.i_ci = ci;
    if (ntransfer != 0) {
      mask |= CIST_TRAN;  /* 'ci' has transfer information */
      ci->u2.transferinfo.ftransfer = ftransfer;
      ci->u2.transferinfo.ntransfer = ntransfer;
    }
    luaD_checkstack(L, LUA_MINSTACK);  /* ensure minimum stack size 保证最小的堆栈大小 */
    if (L->top + LUA_MINSTACK > ci->top)
      ci->top = L->top + LUA_MINSTACK;
    L->allowhook = 0;  /* cannot call hooks inside a hook 不能在钩子中调用钩子 */
    ci->callstatus |= mask;
    lua_unlock(L);
    (*hook)(L, &ar);
    lua_lock(L);
    lua_assert(!L->allowhook);
    L->allowhook = 1;
    ci->top = restorestack(L, ci_top);
    L->top = restorestack(L, top);
    ci->callstatus &= ~mask;
  }
}

/*
** Executes a call hook for Lua functions. This function is called
** whenever 'hookmask' is not zero, so it checks whether call hooks are
** active.
执行Lua函数的调用钩子。每当“hookmask”不为零时都会调用此函数，因此它将检查调用挂钩是否处于活动状态。
*/
void luaD_hookcall (lua_State *L, CallInfo *ci) {
  int hook = (ci->callstatus & CIST_TAIL) ? LUA_HOOKTAILCALL : LUA_HOOKCALL;
  Proto *p;
  if (!(L->hookmask & LUA_MASKCALL))  /* some other hook? */
    return;  /* don't call hook */
  p = clLvalue(s2v(ci->func))->p;
  L->top = ci->top;  /* prepare top */
  ci->u.l.savedpc++;  /* hooks assume 'pc' is already incremented 
  钩子假设'pc'已经增加 */
  luaD_hook(L, hook, -1, 1, p->numparams);
  ci->u.l.savedpc--;  /* correct 'pc' */
}
```

```cpp
/*
** Call a function (C or Lua). The function to be called is at *func.
** The arguments are on the stack, right after the function.
** When returns, all the results are on the stack, starting at the original
** function position.
**调用函数（C或Lua）。 要调用的函数在* func处。
**参数位于函数之后的堆栈中。
**返回时，所有结果都将从原始函数位置开始存储在堆栈中。
*/
void luaD_call (lua_State *L, StkId func, int nresults) {
  lua_CFunction f;
 retry:
  switch (ttypetag(s2v(func))) {
    case LUA_TCCL:  /* C closure */
      f = clCvalue(s2v(func))->f;
      goto Cfunc;
    case LUA_TLCF:  /* light C function */
      f = fvalue(s2v(func));
     Cfunc: {
      int n;  /* number of returns */
      CallInfo *ci;
      checkstackp(L, LUA_MINSTACK, func);  /* ensure minimum stack size */
      ci = next_ci(L);
      ci->nresults = nresults;
      ci->callstatus = CIST_C;
      ci->top = L->top + LUA_MINSTACK;
      ci->func = func;
      lua_assert(ci->top <= L->stack_last);
      if (L->hookmask & LUA_MASKCALL) {
        int narg = cast_int(L->top - func) - 1;
        luaD_hook(L, LUA_HOOKCALL, -1, 1, narg);
      }
      lua_unlock(L);
      n = (*f)(L);  /* do the actual call */
      lua_lock(L);
      api_checknelems(L, n);
      luaD_poscall(L, ci, n);
      break;
    }
    case LUA_TLCL: {  /* Lua function */
      CallInfo *ci;
      Proto *p = clLvalue(s2v(func))->p;
      int narg = cast_int(L->top - func) - 1;  /* number of real arguments */
      int nfixparams = p->numparams;
      int fsize = p->maxstacksize;  /* frame size */
      checkstackp(L, fsize, func);
      ci = next_ci(L);
      ci->nresults = nresults;
      ci->u.l.savedpc = p->code;  /* starting point */
      ci->callstatus = 0;
      ci->top = func + 1 + fsize;
      ci->func = func;
      for (; narg < nfixparams; narg++)
        setnilvalue(s2v(L->top++));  /* complete missing arguments */
      lua_assert(ci->top <= L->stack_last);
      luaV_execute(L, ci);  /* run the function */
      break;
    }
    default: {  /* not a function */
      checkstackp(L, 1, func);  /* space for metamethod */
      luaD_tryfuncTM(L, func);  /* try to get '__call' metamethod */
      goto retry;  /* try again with metamethod */
    }
  }
}

/*
** Call the C function 'func' in protected mode, restoring basic
** thread information ('allowhook', etc.) and in particular
** its stack level in case of errors.
在保护模式下调用C函数“ func”，以恢复基本线程信息（“ allowhook”等），
尤其是在发生错误时恢复其堆栈级别。
*/
int luaD_pcall (lua_State *L, Pfunc func, void *u,
                ptrdiff_t old_top, ptrdiff_t ef) {
  int status;
  CallInfo *old_ci = L->ci;
  lu_byte old_allowhooks = L->allowhook;
  ptrdiff_t old_errfunc = L->errfunc;
  L->errfunc = ef;
  status = luaD_rawrunprotected(L, func, u);
  if (unlikely(status != LUA_OK)) {  /* an error occurred? */
    StkId oldtop = restorestack(L, old_top);
    L->ci = old_ci;
    L->allowhook = old_allowhooks;
    status = luaF_close(L, oldtop, status);
    oldtop = restorestack(L, old_top);  /* previous call may change stack */
    luaD_seterrorobj(L, status, oldtop);
    luaD_shrinkstack(L);
  }
  L->errfunc = old_errfunc;
  return status;
}
```

### Lua 虚拟机的操作码

以下为`lopnames.h`,`lopcodes.c`和`lopcodes.h`

```cpp
static const char *const opnames[] = {
  "MOVE",
  "LOADI",
  "LOADF",
  "LOADK",
  "LOADKX",
  "LOADBOOL",
  "LOADNIL",
  "GETUPVAL",
  "SETUPVAL",
  "GETTABUP",
  "GETTABLE",
  "GETI",
  "GETFIELD",
  "SETTABUP",
  "SETTABLE",
  "SETI",
  "SETFIELD",
  "NEWTABLE",
  "SELF",
  "ADDI",
  "ADDK",
  "SUBK",
  "MULK",
  "MODK",
  "POWK",
  "DIVK",
  "IDIVK",
  "BANDK",
  "BORK",
  "BXORK",
  "SHRI",
  "SHLI",
  "ADD",
  "SUB",
  "MUL",
  "MOD",
  "POW",
  "DIV",
  "IDIV",
  "BAND",
  "BOR",
  "BXOR",
  "SHL",
  "SHR",
  "MMBIN",
  "MMBINI",
  "MMBINK",
  "UNM",
  "BNOT",
  "NOT",
  "LEN",
  "CONCAT",
  "CLOSE",
  "TBC",
  "JMP",
  "EQ",
  "LT",
  "LE",
  "EQK",
  "EQI",
  "LTI",
  "LEI",
  "GTI",
  "GEI",
  "TEST",
  "TESTSET",
  "CALL",
  "TAILCALL",
  "RETURN",
  "RETURN0",
  "RETURN1",
  "FORLOOP",
  "FORPREP",
  "TFORPREP",
  "TFORCALL",
  "TFORLOOP",
  "SETLIST",
  "CLOSURE",
  "VARARG",
  "VARARGPREP",
  "EXTRAARG",
  NULL
};
```

## Lua 原型 (prototypes) 和闭包 (closures)

以下为`lfunc.c`和`lfunc.h`

```cpp
LUAI_FUNC Proto *luaF_newproto (lua_State *L);
LUAI_FUNC CClosure *luaF_newCclosure (lua_State *L, int nelems);
LUAI_FUNC LClosure *luaF_newLclosure (lua_State *L, int nelems);
LUAI_FUNC void luaF_initupvals (lua_State *L, LClosure *cl);
LUAI_FUNC UpVal *luaF_findupval (lua_State *L, StkId level);
LUAI_FUNC void luaF_newtbcupval (lua_State *L, StkId level);
LUAI_FUNC int luaF_close (lua_State *L, StkId level, int status);
LUAI_FUNC void luaF_unlinkupval (UpVal *uv);
LUAI_FUNC void luaF_freeproto (lua_State *L, Proto *f);
LUAI_FUNC const char *luaF_getlocalname (const Proto *func, int local_number,
                                         int pc);
```

Lua 原型和闭包部分函数实现

```cpp
CClosure *luaF_newCclosure (lua_State *L, int n) {
  GCObject *o = luaC_newobj(L, LUA_TCCL, sizeCclosure(n));
  CClosure *c = gco2ccl(o);
  c->nupvalues = cast_byte(n);
  return c;
}


LClosure *luaF_newLclosure (lua_State *L, int n) {
  GCObject *o = luaC_newobj(L, LUA_TLCL, sizeLclosure(n));
  LClosure *c = gco2lcl(o);
  c->p = NULL;
  c->nupvalues = cast_byte(n);
  while (n--) c->upvals[n] = NULL;
  return c;
}

int luaF_close (lua_State *L, StkId level, int status) {
  UpVal *uv;
  while ((uv = L->openupval) != NULL && uplevel(uv) >= level) {
    TValue *slot = &uv->u.value;  /* new position for value */
    lua_assert(uplevel(uv) < L->top);
    if (uv->tbc && status != NOCLOSINGMETH) {
      /* must run closing method, which may change the stack */
      ptrdiff_t levelrel = savestack(L, level);
      status = callclosemth(L, uplevel(uv), status);
      level = restorestack(L, levelrel);
    }
    luaF_unlinkupval(uv);
    setobj(L, slot, uv->v);  /* move value to upvalue slot */
    uv->v = slot;  /* now current value lives here */
    if (!iswhite(uv))
      gray2black(uv);  /* closed upvalues cannot be gray */
    luaC_barrier(L, uv, slot);
  }
  return status;
}


Proto *luaF_newproto (lua_State *L) {
  GCObject *o = luaC_newobj(L, LUA_TPROTO, sizeof(Proto));
  Proto *f = gco2p(o);
  f->k = NULL;
  f->sizek = 0;
  f->p = NULL;
  f->sizep = 0;
  f->code = NULL;
  f->sizecode = 0;
  f->lineinfo = NULL;
  f->sizelineinfo = 0;
  f->abslineinfo = NULL;
  f->sizeabslineinfo = 0;
  f->upvalues = NULL;
  f->sizeupvalues = 0;
  f->numparams = 0;
  f->is_vararg = 0;
  f->maxstacksize = 0;
  f->locvars = NULL;
  f->sizelocvars = 0;
  f->linedefined = 0;
  f->lastlinedefined = 0;
  f->source = NULL;
  return f;
}

void luaF_freeproto (lua_State *L, Proto *f) {
  luaM_freearray(L, f->code, f->sizecode);
  luaM_freearray(L, f->p, f->sizep);
  luaM_freearray(L, f->k, f->sizek);
  luaM_freearray(L, f->lineinfo, f->sizelineinfo);
  luaM_freearray(L, f->abslineinfo, f->sizeabslineinfo);
  luaM_freearray(L, f->locvars, f->sizelocvars);
  luaM_freearray(L, f->upvalues, f->sizeupvalues);
  luaM_free(L, f);
}
```

## Lua 字符串

以下为`lstring.c`和`lstring.h`

Lua TString 函数

```cpp
LUAI_FUNC unsigned int luaS_hash (const char *str, size_t l, unsigned int seed);
LUAI_FUNC unsigned int luaS_hashlongstr (TString *ts);
LUAI_FUNC int luaS_eqlngstr (TString *a, TString *b);
LUAI_FUNC void luaS_resize (lua_State *L, int newsize);
LUAI_FUNC void luaS_clearcache (global_State *g);
LUAI_FUNC void luaS_init (lua_State *L);
LUAI_FUNC void luaS_remove (lua_State *L, TString *ts);
LUAI_FUNC Udata *luaS_newudata (lua_State *L, size_t s, int nuvalue);
LUAI_FUNC TString *luaS_newlstr (lua_State *L, const char *str, size_t l);
LUAI_FUNC TString *luaS_new (lua_State *L, const char *str);
LUAI_FUNC TString *luaS_createlngstrobj (lua_State *L, size_t l);
```

Lua长字符串是通过内存不连续的几个链表组成的，每个链表都是一个连续的字符数组，数组大小为40.
每次new一个字符串出来，都会检查是否命中缓存。

```cpp
/*
** equality for long strings
判断两个长字符串是否相等
*/
int luaS_eqlngstr (TString *a, TString *b) {
  size_t len = a->u.lnglen;
  lua_assert(a->tt == LUA_TLNGSTR && b->tt == LUA_TLNGSTR);
  return (a == b) ||  /* same instance or... */
    ((len == b->u.lnglen) &&  /* equal length and ... */
     (memcmp(getstr(a), getstr(b), len) == 0));  /* equal contents */
}

/* 求字符串的哈希值 */
unsigned int luaS_hash (const char *str, size_t l, unsigned int seed) {
  unsigned int h = seed ^ cast_uint(l);
  size_t step = (l >> LUAI_HASHLIMIT) + 1;
  for (; l >= step; l -= step)
      h ^= ((h<<5) + (h>>2) + cast_byte(str[l - 1]));
  return h;
}

/* 长字符串的哈希值 */
unsigned int luaS_hashlongstr (TString *ts) {
  lua_assert(ts->tt == LUA_TLNGSTR);
  if (ts->extra == 0) {  /* no hash? */
    ts->hash = luaS_hash(getstr(ts), ts->u.lnglen, ts->hash);
    ts->extra = 1;  /* now it has its hash */
  }
  return ts->hash;
}
```

```cpp
/*
** Resize the string table. If allocation fails, keep the current size.
** (This can degrade performance, but any non-zero size should work correctly.)
** 调整字符串表的大小。 如果分配失败，请保持当前大小。
**（这可能会降低性能，但是任何非零大小都应正常工作。）
*/
void luaS_resize (lua_State *L, int nsize) {
  stringtable *tb = &G(L)->strt;
  int osize = tb->size;
  TString **newvect;
  if (nsize < osize)  /* shrinking table? */
    tablerehash(tb->hash, osize, nsize);  /* depopulate shrinking part */
  newvect = luaM_reallocvector(L, tb->hash, osize, nsize, TString*);
  if (unlikely(newvect == NULL)) {  /* reallocation failed? */
    if (nsize < osize)  /* was it shrinking table? */
      tablerehash(tb->hash, nsize, osize);  /* restore to original size */
    /* leave table as it was */
  }
  else {  /* allocation succeeded */
    tb->hash = newvect;
    tb->size = nsize;
    if (nsize > osize)
      tablerehash(newvect, osize, nsize);  /* rehash for new size */
  }
}


/*
** Clear API string cache. (Entries cannot be empty, so fill them with a non-collectable string.)
清除API字符串缓存。 （条目不能为空，因此请使用不可收集的字符串填充它们。）
*/
void luaS_clearcache (global_State *g) {
  int i, j;
  for (i = 0; i < STRCACHE_N; i++)
    for (j = 0; j < STRCACHE_M; j++) {
      if (iswhite(g->strcache[i][j]))  /* will entry be collected? */
        g->strcache[i][j] = g->memerrmsg;  /* replace it with something fixed */
    }
}


/*
** Initialize the string table and the string cache
初始化字符串表和缓存字符串
*/
void luaS_init (lua_State *L) {
  global_State *g = G(L);
  int i, j;
  stringtable *tb = &G(L)->strt;
  tb->hash = luaM_newvector(L, MINSTRTABSIZE, TString*);
  tablerehash(tb->hash, 0, MINSTRTABSIZE);  /* clear array */
  tb->size = MINSTRTABSIZE;
  /* pre-create memory-error message */
  g->memerrmsg = luaS_newliteral(L, MEMERRMSG);
  luaC_fix(L, obj2gco(g->memerrmsg));  /* it should never be collected */
  for (i = 0; i < STRCACHE_N; i++)  /* fill cache with valid strings */
    for (j = 0; j < STRCACHE_M; j++)
      g->strcache[i][j] = g->memerrmsg;
}

TString *luaS_createlngstrobj (lua_State *L, size_t l) {
  TString *ts = createstrobj(L, l, LUA_TLNGSTR, G(L)->seed);
  ts->u.lnglen = l;
  return ts;
}


void luaS_remove (lua_State *L, TString *ts) {
  stringtable *tb = &G(L)->strt;
  TString **p = &tb->hash[lmod(ts->hash, tb->size)];
  while (*p != ts)  /* find previous element */
    p = &(*p)->u.hnext;
  *p = (*p)->u.hnext;  /* remove element from its list */
  tb->nuse--;
}

/*
** new string (with explicit length)
新建一个字符串
*/
TString *luaS_newlstr (lua_State *L, const char *str, size_t l) {
  if (l <= LUAI_MAXSHORTLEN)  /* short string? */
    return internshrstr(L, str, l);
  else {
    TString *ts;
    if (unlikely(l >= (MAX_SIZE - sizeof(TString))/sizeof(char)))
      luaM_toobig(L);
    ts = luaS_createlngstrobj(L, l);
    memcpy(getstr(ts), str, l * sizeof(char));
    return ts;
  }
}

/*
** Create or reuse a zero-terminated string, first checking in the
cache (using the string address as a key). The cache can contain
** only zero-terminated strings, so it is safe to use 'strcmp' to
** check hits.
创建或重用零结尾的字符串，首先在高速缓存中签入（使用字符串地址作为键）。 缓存只能包含以零结尾的字符串，因此使用'strcmp'检查命中是安全的。
*/
TString *luaS_new (lua_State *L, const char *str) {
  unsigned int i = point2uint(str) % STRCACHE_N;  /* hash */
  int j;
  TString **p = G(L)->strcache[i];
  for (j = 0; j < STRCACHE_M; j++) {
    if (strcmp(str, getstr(p[j])) == 0)  /* hit? */
      return p[j];  /* that is it */
  }
  /* normal route */
  for (j = STRCACHE_M - 1; j > 0; j--)
    p[j] = p[j - 1];  /* move out last element */
  /* new element is first in the list */
  p[0] = luaS_newlstr(L, str, strlen(str));
  return p[0];
}
```

Lua新建一个用户对象

```cpp
Udata *luaS_newudata (lua_State *L, size_t s, int nuvalue) {
  Udata *u;
  int i;
  GCObject *o;
  if (unlikely(s > MAX_SIZE - udatamemoffset(nuvalue)))
    luaM_toobig(L);
  o = luaC_newobj(L, LUA_TUSERDATA, sizeudata(nuvalue, s));
  u = gco2u(o);
  u->len = s;
  u->nuvalue = nuvalue;
  u->metatable = NULL;
  for (i = 0; i < nuvalue; i++)
    setnilvalue(&u->uv[i].uv);
  return u;
}
```

## Lua 标记方法

以下为`ltm.c`和`ltm.h`

```cpp
typedef enum {
  TM_INDEX,
  TM_NEWINDEX,
  TM_GC,
  TM_MODE,
  TM_LEN,
  TM_EQ,  /* last tag method with fast access */
  TM_ADD,
  TM_SUB,
  TM_MUL,
  TM_MOD,
  TM_POW,
  TM_DIV,
  TM_IDIV,
  TM_BAND,
  TM_BOR,
  TM_BXOR,
  TM_SHL,
  TM_SHR,
  TM_UNM,
  TM_BNOT,
  TM_LT,
  TM_LE,
  TM_CONCAT,
  TM_CALL,
  TM_CLOSE,
  TM_N		/* number of elements in the enum */
} TMS;
```

```cpp
LUAI_FUNC const char *luaT_objtypename (lua_State *L, const TValue *o);

LUAI_FUNC const TValue *luaT_gettm (Table *events, TMS event, TString *ename);
LUAI_FUNC const TValue *luaT_gettmbyobj (lua_State *L, const TValue *o,
                                                       TMS event);
LUAI_FUNC void luaT_init (lua_State *L);

LUAI_FUNC void luaT_callTM (lua_State *L, const TValue *f, const TValue *p1,
                            const TValue *p2, const TValue *p3);
LUAI_FUNC void luaT_callTMres (lua_State *L, const TValue *f,
                            const TValue *p1, const TValue *p2, StkId p3);
LUAI_FUNC void luaT_trybinTM (lua_State *L, const TValue *p1, const TValue *p2,
                              StkId res, TMS event);
LUAI_FUNC void luaT_tryconcatTM (lua_State *L);
LUAI_FUNC void luaT_trybinassocTM (lua_State *L, const TValue *p1,
       const TValue *p2, int inv, StkId res, TMS event);
LUAI_FUNC void luaT_trybiniTM (lua_State *L, const TValue *p1, lua_Integer i2,
                               int inv, StkId res, TMS event);
LUAI_FUNC int luaT_callorderTM (lua_State *L, const TValue *p1,
                                const TValue *p2, TMS event);
LUAI_FUNC int luaT_callorderiTM (lua_State *L, const TValue *p1, int v2,
                                 int inv, int isfloat, TMS event);

LUAI_FUNC void luaT_adjustvarargs (lua_State *L, int nfixparams,
                                   struct CallInfo *ci, const Proto *p);
LUAI_FUNC void luaT_getvarargs (lua_State *L, struct CallInfo *ci,
                                              StkId where, int wanted);
```

```cpp
void luaT_init (lua_State *L) {
  static const char *const luaT_eventname[] = {  /* ORDER TM */
    "__index", "__newindex",
    "__gc", "__mode", "__len", "__eq",
    "__add", "__sub", "__mul", "__mod", "__pow",
    "__div", "__idiv",
    "__band", "__bor", "__bxor", "__shl", "__shr",
    "__unm", "__bnot", "__lt", "__le",
    "__concat", "__call", "__close"
  };
  int i;
  for (i=0; i<TM_N; i++) {
    G(L)->tmname[i] = luaS_new(L, luaT_eventname[i]);
    luaC_fix(L, obj2gco(G(L)->tmname[i]));  /* never collect these names */
  }
}
```

```cpp
/*
** function to be used with macro "fasttm": optimized for absence of
** tag methods
与宏 “fasttm” 一起使用的函数：针对缺少标签方法进行了优化
*/
const TValue *luaT_gettm (Table *events, TMS event, TString *ename) {
  const TValue *tm = luaH_getshortstr(events, ename);
  lua_assert(event <= TM_EQ);
  if (notm(tm)) {  /* no tag method? */
    events->flags |= cast_byte(1u<<event);  /* cache this fact */
    return NULL;
  }
  else return tm;
}
```

## Lua 函数库

### Lua 基本函数和基础库

以下为`lbaselib`和`lualib.h`

定义了Lua基本函数，如`print`等

```cpp
static const luaL_Reg base_funcs[] = {
  {"assert", luaB_assert},
  {"collectgarbage", luaB_collectgarbage},
  {"dofile", luaB_dofile},
  {"error", luaB_error},
  {"getmetatable", luaB_getmetatable},
  {"ipairs", luaB_ipairs},
  {"loadfile", luaB_loadfile},
  {"load", luaB_load},
  {"next", luaB_next},
  {"pairs", luaB_pairs},
  {"pcall", luaB_pcall},
  {"print", luaB_print},
  {"warn", luaB_warn},
  {"rawequal", luaB_rawequal},
  {"rawlen", luaB_rawlen},
  {"rawget", luaB_rawget},
  {"rawset", luaB_rawset},
  {"select", luaB_select},
  {"setmetatable", luaB_setmetatable},
  {"tonumber", luaB_tonumber},
  {"tostring", luaB_tostring},
  {"type", luaB_type},
  {"xpcall", luaB_xpcall},
  /* placeholders */
  {LUA_GNAME, NULL},
  {"_VERSION", NULL},
  {NULL, NULL}
};
```

```cpp
static int luaB_print (lua_State *L) {
  int n = lua_gettop(L);  /* number of arguments */
  int i;
  for (i = 1; i <= n; i++) {  /* for each argument */
    size_t l;
    const char *s = luaL_tolstring(L, i, &l);  /* convert it to string */
    if (i > 1)  /* not the first element? */
      lua_writestring("\t", 1);  /* add a tab before it */
    lua_writestring(s, l);  /* print it */
    lua_pop(L, 1);  /* pop result */
  }
  lua_writeline();
  return 0;
}
```

```cpp
static int luaB_tonumber (lua_State *L) {
  if (lua_isnoneornil(L, 2)) {  /* standard conversion? */
    if (lua_type(L, 1) == LUA_TNUMBER) {  /* already a number? */
      lua_settop(L, 1);  /* yes; return it */
      return 1;
    }
    else {
      size_t l;
      const char *s = lua_tolstring(L, 1, &l);
      if (s != NULL && lua_stringtonumber(L, s) == l + 1)
        return 1;  /* successful conversion to number */
      /* else not a number */
      luaL_checkany(L, 1);  /* (but there must be some parameter) */
    }
  }
  else {
    size_t l;
    const char *s;
    lua_Integer n = 0;  /* to avoid warnings */
    lua_Integer base = luaL_checkinteger(L, 2);
    luaL_checktype(L, 1, LUA_TSTRING);  /* no numbers as strings */
    s = lua_tolstring(L, 1, &l);
    luaL_argcheck(L, 2 <= base && base <= 36, 2, "base out of range");
    if (b_str2int(s, (int)base, &n) == s + l) {
      lua_pushinteger(L, n);
      return 1;
    }  /* else not a number */
  }  /* else not a number */
  luaL_pushfail(L);  /* not a number */
  return 1;
}

static int luaB_tostring (lua_State *L) {
  luaL_checkany(L, 1);
  luaL_tolstring(L, 1, NULL);
  return 1;
}

static int luaB_type (lua_State *L) {
  int t = lua_type(L, 1);
  luaL_argcheck(L, t != LUA_TNONE, 1, "value expected");
  lua_pushstring(L, lua_typename(L, t));
  return 1;
}
```

```cpp
static int luaB_getmetatable (lua_State *L) {
  luaL_checkany(L, 1);
  if (!lua_getmetatable(L, 1)) {
    lua_pushnil(L);
    return 1;  /* no metatable */
  }
  luaL_getmetafield(L, 1, "__metatable");
  return 1;  /* returns either __metatable field (if present) or metatable */
}


static int luaB_setmetatable (lua_State *L) {
  int t = lua_type(L, 2);
  luaL_checktype(L, 1, LUA_TTABLE);
  luaL_argexpected(L, t == LUA_TNIL || t == LUA_TTABLE, 2, "nil or table");
  if (luaL_getmetafield(L, 1, "__metatable") != LUA_TNIL)
    return luaL_error(L, "cannot change a protected metatable");
  lua_settop(L, 2);
  lua_setmetatable(L, 1);
  return 1;
}
```

### Lua string库

以下为`lstrlib.c`,

```cpp
static int str_sub (lua_State *L) {
  size_t l;
  const char *s = luaL_checklstring(L, 1, &l);
  size_t start = posrelatI(luaL_checkinteger(L, 2), l);
  size_t end = getendpos(L, 3, -1, l);
  if (start <= end)
    lua_pushlstring(L, s + start - 1, (end - start) + 1);
  else lua_pushliteral(L, "");
  return 1;
}


static int str_reverse (lua_State *L) {
  size_t l, i;
  luaL_Buffer b;
  const char *s = luaL_checklstring(L, 1, &l);
  char *p = luaL_buffinitsize(L, &b, l);
  for (i = 0; i < l; i++)
    p[i] = s[l - i - 1];
  luaL_pushresultsize(&b, l);
  return 1;
}


static int str_lower (lua_State *L) {
  size_t l;
  size_t i;
  luaL_Buffer b;
  const char *s = luaL_checklstring(L, 1, &l);
  char *p = luaL_buffinitsize(L, &b, l);
  for (i=0; i<l; i++)
    p[i] = tolower(uchar(s[i]));
  luaL_pushresultsize(&b, l);
  return 1;
}


static int str_upper (lua_State *L) {
  size_t l;
  size_t i;
  luaL_Buffer b;
  const char *s = luaL_checklstring(L, 1, &l);
  char *p = luaL_buffinitsize(L, &b, l);
  for (i=0; i<l; i++)
    p[i] = toupper(uchar(s[i]));
  luaL_pushresultsize(&b, l);
  return 1;
}


static int str_rep (lua_State *L) {
  size_t l, lsep;
  const char *s = luaL_checklstring(L, 1, &l);
  lua_Integer n = luaL_checkinteger(L, 2);
  const char *sep = luaL_optlstring(L, 3, "", &lsep);
  if (n <= 0) lua_pushliteral(L, "");
  else if (l + lsep < l || l + lsep > MAXSIZE / n)  /* may overflow? */
    return luaL_error(L, "resulting string too large");
  else {
    size_t totallen = (size_t)n * l + (size_t)(n - 1) * lsep;
    luaL_Buffer b;
    char *p = luaL_buffinitsize(L, &b, totallen);
    while (n-- > 1) {  /* first n-1 copies (followed by separator) */
      memcpy(p, s, l * sizeof(char)); p += l;
      if (lsep > 0) {  /* empty 'memcpy' is not that cheap */
        memcpy(p, sep, lsep * sizeof(char));
        p += lsep;
      }
    }
    memcpy(p, s, l * sizeof(char));  /* last copy (not followed by separator) */
    luaL_pushresultsize(&b, totallen);
  }
  return 1;
}


static int str_byte (lua_State *L) {
  size_t l;
  const char *s = luaL_checklstring(L, 1, &l);
  lua_Integer pi = luaL_optinteger(L, 2, 1);
  size_t posi = posrelatI(pi, l);
  size_t pose = getendpos(L, 3, pi, l);
  int n, i;
  if (posi > pose) return 0;  /* empty interval; return no values */
  if (pose - posi >= (size_t)INT_MAX)  /* arithmetic overflow? */
    return luaL_error(L, "string slice too long");
  n = (int)(pose -  posi) + 1;
  luaL_checkstack(L, n, "string slice too long");
  for (i=0; i<n; i++)
    lua_pushinteger(L, uchar(s[posi+i-1]));
  return n;
}


static int str_char (lua_State *L) {
  int n = lua_gettop(L);  /* number of arguments */
  int i;
  luaL_Buffer b;
  char *p = luaL_buffinitsize(L, &b, n);
  for (i=1; i<=n; i++) {
    lua_Unsigned c = (lua_Unsigned)luaL_checkinteger(L, i);
    luaL_argcheck(L, c <= (lua_Unsigned)UCHAR_MAX, i, "value out of range");
    p[i - 1] = uchar(c);
  }
  luaL_pushresultsize(&b, n);
  return 1;
}
```

Lua字符串正则表达式匹配

```cpp
typedef struct MatchState {
  const char *src_init;  /* init of source string */
  const char *src_end;  /* end ('\0') of source string */
  const char *p_end;  /* end ('\0') of pattern */
  lua_State *L;
  int matchdepth;  /* control for recursive depth (to avoid C stack overflow) */
  unsigned char level;  /* total number of captures (finished or unfinished) */
  struct {
    const char *init;
    ptrdiff_t len;
  } capture[LUA_MAXCAPTURES];
} MatchState;


static const char *match (MatchState *ms, const char *s, const char *p) {
  if (ms->matchdepth-- == 0)
    luaL_error(ms->L, "pattern too complex");
  init: /* using goto's to optimize tail recursion */
  if (p != ms->p_end) {  /* end of pattern? */
    switch (*p) {
      case '(': {  /* start capture */
        if (*(p + 1) == ')')  /* position capture? */
          s = start_capture(ms, s, p + 2, CAP_POSITION);
        else
          s = start_capture(ms, s, p + 1, CAP_UNFINISHED);
        break;
      }
      case ')': {  /* end capture */
        s = end_capture(ms, s, p + 1);
        break;
      }
      case '$': {
        if ((p + 1) != ms->p_end)  /* is the '$' the last char in pattern? */
          goto dflt;  /* no; go to default */
        s = (s == ms->src_end) ? s : NULL;  /* check end of string */
        break;
      }
      case L_ESC: {  /* escaped sequences not in the format class[*+?-]? */
        switch (*(p + 1)) {
          case 'b': {  /* balanced string? */
            s = matchbalance(ms, s, p + 2);
            if (s != NULL) {
              p += 4; goto init;  /* return match(ms, s, p + 4); */
            }  /* else fail (s == NULL) */
            break;
          }
          case 'f': {  /* frontier? */
            const char *ep; char previous;
            p += 2;
            if (*p != '[')
              luaL_error(ms->L, "missing '[' after '%%f' in pattern");
            ep = classend(ms, p);  /* points to what is next */
            previous = (s == ms->src_init) ? '\0' : *(s - 1);
            if (!matchbracketclass(uchar(previous), p, ep - 1) &&
               matchbracketclass(uchar(*s), p, ep - 1)) {
              p = ep; goto init;  /* return match(ms, s, ep); */
            }
            s = NULL;  /* match failed */
            break;
          }
          case '0': case '1': case '2': case '3':
          case '4': case '5': case '6': case '7':
          case '8': case '9': {  /* capture results (%0-%9)? */
            s = match_capture(ms, s, uchar(*(p + 1)));
            if (s != NULL) {
              p += 2; goto init;  /* return match(ms, s, p + 2) */
            }
            break;
          }
          default: goto dflt;
        }
        break;
      }
      default: dflt: {  /* pattern class plus optional suffix */
        const char *ep = classend(ms, p);  /* points to optional suffix */
        /* does not match at least once? */
        if (!singlematch(ms, s, p, ep)) {
          if (*ep == '*' || *ep == '?' || *ep == '-') {  /* accept empty? */
            p = ep + 1; goto init;  /* return match(ms, s, ep + 1); */
          }
          else  /* '+' or no suffix */
            s = NULL;  /* fail */
        }
        else {  /* matched once */
          switch (*ep) {  /* handle optional suffix */
            case '?': {  /* optional */
              const char *res;
              if ((res = match(ms, s + 1, ep + 1)) != NULL)
                s = res;
              else {
                p = ep + 1; goto init;  /* else return match(ms, s, ep + 1); */
              }
              break;
            }
            case '+':  /* 1 or more repetitions */
              s++;  /* 1 match already done */
              /* FALLTHROUGH */
            case '*':  /* 0 or more repetitions */
              s = max_expand(ms, s, p, ep);
              break;
            case '-':  /* 0 or more repetitions (minimum) */
              s = min_expand(ms, s, p, ep);
              break;
            default:  /* no suffix */
              s++; p = ep; goto init;  /* return match(ms, s + 1, ep); */
          }
        }
        break;
      }
    }
  }
  ms->matchdepth++;
  return s;
}
```

字符串格式化函数

```cpp
/*
** Read and classify next option. 'size' is filled with option's size.
*/
static KOption getoption (Header *h, const char **fmt, int *size) {
  int opt = *((*fmt)++);
  *size = 0;  /* default */
  switch (opt) {
    case 'b': *size = sizeof(char); return Kint;
    case 'B': *size = sizeof(char); return Kuint;
    case 'h': *size = sizeof(short); return Kint;
    case 'H': *size = sizeof(short); return Kuint;
    case 'l': *size = sizeof(long); return Kint;
    case 'L': *size = sizeof(long); return Kuint;
    case 'j': *size = sizeof(lua_Integer); return Kint;
    case 'J': *size = sizeof(lua_Integer); return Kuint;
    case 'T': *size = sizeof(size_t); return Kuint;
    case 'f': *size = sizeof(float); return Kfloat;
    case 'd': *size = sizeof(double); return Kfloat;
    case 'n': *size = sizeof(lua_Number); return Kfloat;
    case 'i': *size = getnumlimit(h, fmt, sizeof(int)); return Kint;
    case 'I': *size = getnumlimit(h, fmt, sizeof(int)); return Kuint;
    case 's': *size = getnumlimit(h, fmt, sizeof(size_t)); return Kstring;
    case 'c':
      *size = getnum(fmt, -1);
      if (*size == -1)
        luaL_error(h->L, "missing size for format option 'c'");
      return Kchar;
    case 'z': return Kzstr;
    case 'x': *size = 1; return Kpadding;
    case 'X': return Kpaddalign;
    case ' ': break;
    case '<': h->islittle = 1; break;
    case '>': h->islittle = 0; break;
    case '=': h->islittle = nativeendian.little; break;
    case '!': h->maxalign = getnumlimit(h, fmt, MAXALIGN); break;
    default: luaL_error(h->L, "invalid format option '%c'", opt);
  }
  return Knop;
}
```

所有Lua字符串函数

```cpp
static const luaL_Reg strlib[] = {
  {"byte", str_byte},
  {"char", str_char},
  {"dump", str_dump},
  {"find", str_find},
  {"format", str_format},
  {"gmatch", gmatch},
  {"gsub", str_gsub},
  {"len", str_len},
  {"lower", str_lower},
  {"match", str_match},
  {"rep", str_rep},
  {"reverse", str_reverse},
  {"sub", str_sub},
  {"upper", str_upper},
  {"pack", str_pack},
  {"packsize", str_packsize},
  {"unpack", str_unpack},
  {NULL, NULL}
};
```

### Lua table库

以下为`ltable.c`和`ltable.h`

Lua表（又名数组，对象或哈希表）的实现。表将其元素分为两部分：数组部分和哈希部分。

非负整数键是要保留在数组部分中的所有候选项。 数组的实际大小是最大的“ n”，因此使用了介于1和n之间的插槽的一半以上。

Hash结合使用链式散布表和Brent的变体。这些表格的主要不变之处在于，如果元素不在其主要位置（即，其散列赋予其的“原始”位置），则碰撞元素将位于其自己的主要位置。因此，即使负载率达到100％，性能仍然保持良好。

Lua Table API

```cpp
LUAI_FUNC const TValue *luaH_getint (Table *t, lua_Integer key);
LUAI_FUNC void luaH_setint (lua_State *L, Table *t, lua_Integer key,
                                                    TValue *value);
LUAI_FUNC const TValue *luaH_getshortstr (Table *t, TString *key);
LUAI_FUNC const TValue *luaH_getstr (Table *t, TString *key);
LUAI_FUNC const TValue *luaH_get (Table *t, const TValue *key);
LUAI_FUNC TValue *luaH_newkey (lua_State *L, Table *t, const TValue *key);
LUAI_FUNC TValue *luaH_set (lua_State *L, Table *t, const TValue *key);
LUAI_FUNC Table *luaH_new (lua_State *L);
LUAI_FUNC void luaH_resize (lua_State *L, Table *t, unsigned int nasize,
                                                    unsigned int nhsize);
LUAI_FUNC void luaH_resizearray (lua_State *L, Table *t, unsigned int nasize);
LUAI_FUNC void luaH_free (lua_State *L, Table *t);
LUAI_FUNC int luaH_next (lua_State *L, Table *t, StkId key);
LUAI_FUNC lua_Unsigned luaH_getn (Table *t);
LUAI_FUNC unsigned int luaH_realasize (const Table *t);
```

```cpp
/*
** Returns the real size of the 'array' array
返回“数组”数组的实际大小
*/
LUAI_FUNC unsigned int luaH_realasize (const Table *t) {
  if (limitequalsasize(t))
    return t->alimit;  /* this is the size */
  else {
    unsigned int size = t->alimit;
    /* compute the smallest power of 2 not smaller than 'n' */
    size |= (size >> 1);
    size |= (size >> 2);
    size |= (size >> 4);
    size |= (size >> 8);
    size |= (size >> 16);
#if (UINT_MAX >> 30) > 3
    size |= (size >> 32);  /* unsigned int has more than 32 bits */
#endif
    size++;
    lua_assert(ispow2(size) && size/2 < t->alimit && t->alimit < size);
    return size;
  }
}
```

```cpp
/*
** Compute the optimal size for the array part of table 't'. 'nums' is a
** "count array" where 'nums[i]' is the number of integers in the table
** between 2^(i - 1) + 1 and 2^i. 'pna' enters with the total number of
** integer keys in the table and leaves with the number of keys that
** will go to the array part; return the optimal size.  (The condition
** 'twotoi > 0' in the for loop stops the loop if 'twotoi' overflows.)
计算表't'的数组部分的最佳大小。 “ nums”是“计数数组”，其中“ nums [i]”是表中2 ^（i-1）+1和2 ^ i之间的整数数。
“ pna”以表中整数键的总数输入，而留下将要到达数组部分的键数； 返回最佳尺寸。
（如果“ twotoi”溢出，则for循环中的条件“ twotoi> 0”将停止循环。）
*/
static unsigned int computesizes (unsigned int nums[], unsigned int *pna) {
  int i;
  unsigned int twotoi;  /* 2^i (candidate for optimal size) */
  unsigned int a = 0;  /* number of elements smaller than 2^i */
  unsigned int na = 0;  /* number of elements to go to array part */
  unsigned int optimal = 0;  /* optimal size for array part */
  /* loop while keys can fill more than half of total size */
  for (i = 0, twotoi = 1;
       twotoi > 0 && *pna > twotoi / 2;
       i++, twotoi *= 2) {
    a += nums[i];
    if (a > twotoi/2) {  /* more than half elements present? */
      optimal = twotoi;  /* optimal size (till now) */
      na = a;  /* all elements up to 'optimal' will go to array part */
    }
  }
  lua_assert((optimal == 0 || optimal / 2 < na) && na <= optimal);
  *pna = na;
  return optimal;
}
```

```cpp
/*
** Resize table 't' for the new given sizes. Both allocations (for
** the hash part and for the array part) can fail, which creates some
** subtleties. If the first allocation, for the hash part, fails, an
** error is raised and that is it. Otherwise, it copies the elements from
** the shrinking part of the array (if it is shrinking) into the new
** hash. Then it reallocates the array part.  If that fails, the table
** is in its original state; the function frees the new hash part and then
** raises the allocation error. Otherwise, it sets the new hash part
** into the table, initializes the new part of the array (if any) with
** nils and reinserts the elements of the old hash back into the new
** parts of the table.
为新的给定尺寸调整表't'的尺寸。 两种分配（用于散列部分和用于数组部分）都可能失败，这会导致som的细微差别。 
如果针对哈希部分的第一次分配失败，则会引发错误，仅此而已。 否则，它将元素从数组的收缩部分（如果正在收缩）
复制到新的哈希中。 然后，它重新分配数组部分。 如果失败，则表处于原始状态； 该函数释放新的哈希部分，
然后引发分配错误。 否则，它将新的哈希部分设置到表中，使用nil初始化数组的新部分（如果有），
然后将旧哈希的元素重新插入表的新部分。
*/
void luaH_resize (lua_State *L, Table *t, unsigned int newasize,
                                          unsigned int nhsize) {
  unsigned int i;
  Table newt;  /* to keep the new hash part */
  unsigned int oldasize = setlimittosize(t);
  TValue *newarray;
  /* create new hash part with appropriate size into 'newt' */
  setnodevector(L, &newt, nhsize);
  if (newasize < oldasize) {  /* will array shrink? */
    t->alimit = newasize;  /* pretend array has new size... */
    exchangehashpart(t, &newt);  /* and new hash */
    /* re-insert into the new hash the elements from vanishing slice */
    for (i = newasize; i < oldasize; i++) {
      if (!isempty(&t->array[i]))
        luaH_setint(L, t, i + 1, &t->array[i]);
    }
    t->alimit = oldasize;  /* restore current size... */
    exchangehashpart(t, &newt);  /* and hash (in case of errors) */
  }
  /* allocate new array */
  newarray = luaM_reallocvector(L, t->array, oldasize, newasize, TValue);
  if (unlikely(newarray == NULL && newasize > 0)) {  /* allocation failed? */
    freehash(L, &newt);  /* release new hash part */
    luaM_error(L);  /* raise error (with array unchanged) */
  }
  /* allocation ok; initialize new part of the array */
  exchangehashpart(t, &newt);  /* 't' has the new hash ('newt' has the old) */
  t->array = newarray;  /* set new array part */
  t->alimit = newasize;
  for (i = oldasize; i < newasize; i++)  /* clear new slice of the array */
     setempty(&t->array[i]);
  /* re-insert elements from old hash part into new parts */
  reinsert(L, &newt, t);  /* 'newt' now has the old hash */
  freehash(L, &newt);  /* free old hash part */
}
```

Lua新建表

```cpp
Table *luaH_new (lua_State *L) {
  GCObject *o = luaC_newobj(L, LUA_TTABLE, sizeof(Table));
  Table *t = gco2t(o);
  t->metatable = NULL;
  t->flags = cast_byte(~0);
  t->array = NULL;
  t->alimit = 0;
  setnodevector(L, t, 0);
  return t;
}
```

```cpp
/*
** inserts a new key into a hash table; first, check whether key's main
** position is free. If not, check whether colliding node is in its main
** position or not: if it is not, move colliding node to an empty place and
** put new key in its main position; otherwise (colliding node is in its main
** position), new key goes to an empty position.
将新密钥插入哈希表； 首先，检查钥匙的主要位置是否空闲。 
如果不是，则检查碰撞节点是否位于其主要位置：如果不是，请将碰撞节点移至空白处并将新钥匙置于其主要位置；
否则（冲突节点位于其主要位置），新密钥将移至空位置。
*/
TValue *luaH_newkey (lua_State *L, Table *t, const TValue *key) {
  Node *mp;
  TValue aux;
  if (unlikely(ttisnil(key)))
    luaG_runerror(L, "table index is nil");
  else if (ttisfloat(key)) {
    lua_Number f = fltvalue(key);
    lua_Integer k;
    if (luaV_flttointeger(f, &k, 0)) {  /* does key fit in an integer? */
      setivalue(&aux, k);
      key = &aux;  /* insert it as an integer */
    }
    else if (unlikely(luai_numisnan(f)))
      luaG_runerror(L, "table index is NaN");
  }
  mp = mainpositionTV(t, key);
  if (!isempty(gval(mp)) || isdummy(t)) {  /* main position is taken? */
    Node *othern;
    Node *f = getfreepos(t);  /* get a free place */
    if (f == NULL) {  /* cannot find a free place? */
      rehash(L, t, key);  /* grow table */
      /* whatever called 'newkey' takes care of TM cache */
      return luaH_set(L, t, key);  /* insert key into grown table */
    }
    lua_assert(!isdummy(t));
    othern = mainposition(t, keytt(mp), &keyval(mp));
    if (othern != mp) {  /* is colliding node out of its main position? */
      /* yes; move colliding node into free position */
      while (othern + gnext(othern) != mp)  /* find previous */
        othern += gnext(othern);
      gnext(othern) = cast_int(f - othern);  /* rechain to point to 'f' */
      *f = *mp;  /* copy colliding node into free pos. (mp->next also goes) */
      if (gnext(mp) != 0) {
        gnext(f) += cast_int(mp - f);  /* correct 'next' */
        gnext(mp) = 0;  /* now 'mp' is free */
      }
      setempty(gval(mp));
    }
    else {  /* colliding node is in its own main position */
      /* new node will go into free position */
      if (gnext(mp) != 0)
        gnext(f) = cast_int((mp + gnext(mp)) - f);  /* chain new position */
      else lua_assert(gnext(f) == 0);
      gnext(mp) = cast_int(f - mp);
      mp = f;
    }
  }
  setnodekey(L, mp, key);
  luaC_barrierback(L, obj2gco(t), key);
  lua_assert(isempty(gval(mp)));
  return gval(mp);
}
```

```cpp
/*
** main search function
主要搜索函数
*/
const TValue *luaH_get (Table *t, const TValue *key) {
  switch (ttypetag(key)) {
    case LUA_TSHRSTR: return luaH_getshortstr(t, tsvalue(key));
    case LUA_TNUMINT: return luaH_getint(t, ivalue(key));
    case LUA_TNIL: return &absentkey;
    case LUA_TNUMFLT: {
      lua_Integer k;
      if (luaV_flttointeger(fltvalue(key), &k, 0)) /* index is an integral? */
        return luaH_getint(t, k);  /* use specialized version */
      /* else... */
    }  /* FALLTHROUGH */
    default:
      return getgeneric(t, key);
  }
}
```

```cpp
/*
** Try to find a boundary in table 't'. (A 'boundary' is an integer index
** such that t[i] is present and t[i+1] is absent, or 0 if t[1] is absent
** and 'maxinteger' if t[maxinteger] is present.)
** (In the next explanation, we use Lua indices, that is, with base 1.
** The code itself uses base 0 when indexing the array part of the table.)
** The code starts with 'limit = t->alimit', a position in the array
** part that may be a boundary.
**
** (1) If 't[limit]' is empty, there must be a boundary before it.
** As a common case (e.g., after 't[#t]=nil'), check whether 'limit-1'
** is present. If so, it is a boundary. Otherwise, do a binary search
** between 0 and limit to find a boundary. In both cases, try to
** use this boundary as the new 'alimit', as a hint for the next call.
**
** (2) If 't[limit]' is not empty and the array has more elements
** after 'limit', try to find a boundary there. Again, try first
** the special case (which should be quite frequent) where 'limit+1'
** is empty, so that 'limit' is a boundary. Otherwise, check the
** last element of the array part. If it is empty, there must be a
** boundary between the old limit (present) and the last element
** (absent), which is found with a binary search. (This boundary always
** can be a new limit.)
**
** (3) The last case is when there are no elements in the array part
** (limit == 0) or its last element (the new limit) is present.
** In this case, must check the hash part. If there is no hash part
** or 'limit+1' is absent, 'limit' is a boundary.  Otherwise, call
** 'hash_search' to find a boundary in the hash part of the table.
** (In those cases, the boundary is not inside the array part, and
** therefore cannot be used as a new limit.)
尝试找到表t的边界，分为三种情况。
*/
lua_Unsigned luaH_getn (Table *t) {
  unsigned int limit = t->alimit;
  if (limit > 0 && isempty(&t->array[limit - 1])) {  /* (1)? */
    /* there must be a boundary before 'limit' */
    if (limit >= 2 && !isempty(&t->array[limit - 2])) {
      /* 'limit - 1' is a boundary; can it be a new limit? */
      if (ispow2realasize(t) && !ispow2(limit - 1)) {
        t->alimit = limit - 1;
        setnorealasize(t);  /* now 'alimit' is not the real size */
      }
      return limit - 1;
    }
    else {  /* must search for a boundary in [0, limit] */
      unsigned int boundary = binsearch(t->array, 0, limit);
      /* can this boundary represent the real size of the array? */
      if (ispow2realasize(t) && boundary > luaH_realasize(t) / 2) {
        t->alimit = boundary;  /* use it as the new limit */
        setnorealasize(t);
      }
      return boundary;
    }
  }
  /* 'limit' is zero or present in table */
  if (!limitequalsasize(t)) {  /* (2)? */
    /* 'limit' > 0 and array has more elements after 'limit' */
    if (isempty(&t->array[limit]))  /* 'limit + 1' is empty? */
      return limit;  /* this is the boundary */
    /* else, try last element in the array */
    limit = luaH_realasize(t);
    if (isempty(&t->array[limit - 1])) {  /* empty? */
      /* there must be a boundary in the array after old limit,
         and it must be a valid new limit */
      unsigned int boundary = binsearch(t->array, t->alimit, limit);
      t->alimit = boundary;
      return boundary;
    }
    /* else, new limit is present in the table; check the hash part */
  }
  /* (3) 'limit' is the last element and either is zero or present in table */
  lua_assert(limit == luaH_realasize(t) &&
             (limit == 0 || !isempty(&t->array[limit - 1])));
  if (isdummy(t) || isempty(luaH_getint(t, cast(lua_Integer, limit + 1))))
    return limit;  /* 'limit + 1' is absent */
  else  /* 'limit + 1' is also present */
    return hash_search(t, limit);
}
```

### Lua 数学库

以下为`lmathlib.c`,包括了基本的数学的运算和随机数库。

```cpp
static const luaL_Reg mathlib[] = {
  {"abs",   math_abs},
  {"acos",  math_acos},
  {"asin",  math_asin},
  {"atan",  math_atan},
  {"ceil",  math_ceil},
  {"cos",   math_cos},
  {"deg",   math_deg},
  {"exp",   math_exp},
  {"tointeger", math_toint},
  {"floor", math_floor},
  {"fmod",   math_fmod},
  {"ult",   math_ult},
  {"log",   math_log},
  {"max",   math_max},
  {"min",   math_min},
  {"modf",   math_modf},
  {"rad",   math_rad},
  {"sin",   math_sin},
  {"sqrt",  math_sqrt},
  {"tan",   math_tan},
  {"type", math_type},
#if defined(LUA_COMPAT_MATHLIB)
  {"atan2", math_atan},
  {"cosh",   math_cosh},
  {"sinh",   math_sinh},
  {"tanh",   math_tanh},
  {"pow",   math_pow},
  {"frexp", math_frexp},
  {"ldexp", math_ldexp},
  {"log10", math_log10},
#endif
  /* placeholders */
  {"random", NULL},
  {"randomseed", NULL},
  {"pi", NULL},
  {"huge", NULL},
  {"maxinteger", NULL},
  {"mininteger", NULL},
  {NULL, NULL}
};
```

Lua 数学函数示例

```cpp
/*
** next function does not use 'modf', avoiding problems with 'double*'
** (which is not compatible with 'float*') when lua_Number is not
** 'double'.
*/
static int math_modf (lua_State *L) {
  if (lua_isinteger(L ,1)) {
    lua_settop(L, 1);  /* number is its own integer part */
    lua_pushnumber(L, 0);  /* no fractional part */
  }
  else {
    lua_Number n = luaL_checknumber(L, 1);
    /* integer part (rounds toward zero) */
    lua_Number ip = (n < 0) ? l_mathop(ceil)(n) : l_mathop(floor)(n);
    pushnumint(L, ip);
    /* fractional part (test needed for inf/-inf) */
    lua_pushnumber(L, (n == ip) ? l_mathop(0.0) : (n - ip));
  }
  return 2;
}


static int math_sqrt (lua_State *L) {
  lua_pushnumber(L, l_mathop(sqrt)(luaL_checknumber(L, 1)));
  return 1;
}
```

### Lua 协程库

以下为`lcorolib.c`和`lcorolib.h`

Lua 协程库函数

```cpp
static const luaL_Reg co_funcs[] = {
  {"create", luaB_cocreate},
  {"resume", luaB_coresume},
  {"running", luaB_corunning},
  {"status", luaB_costatus},
  {"wrap", luaB_cowrap},
  {"yield", luaB_yield},
  {"isyieldable", luaB_yieldable},
  {"close", luaB_close},
  {NULL, NULL}
};
```

```cpp
static int luaB_cocreate (lua_State *L) {
  lua_State *NL;
  luaL_checktype(L, 1, LUA_TFUNCTION);
  NL = lua_newthread(L);
  lua_pushvalue(L, 1);  /* move function to top */
  lua_xmove(L, NL, 1);  /* move function from L to NL */
  return 1;
}

LUA_API lua_State *lua_newthread (lua_State *L) {
  global_State *g = G(L);
  lua_State *L1;
  lua_lock(L);
  luaC_checkGC(L);
  /* create new thread */
  L1 = &cast(LX *, luaM_newobject(L, LUA_TTHREAD, sizeof(LX)))->l;
  L1->marked = luaC_white(g);
  L1->tt = LUA_TTHREAD;
  /* link it on list 'allgc' */
  L1->next = g->allgc;
  g->allgc = obj2gco(L1);
  /* anchor it on L stack */
  setthvalue2s(L, L->top, L1);
  api_incr_top(L);
  preinit_thread(L1, g);
  L1->nCcalls = getCcalls(L);
  L1->hookmask = L->hookmask;
  L1->basehookcount = L->basehookcount;
  L1->hook = L->hook;
  resethookcount(L1);
  /* initialize L1 extra space */
  memcpy(lua_getextraspace(L1), lua_getextraspace(g->mainthread),
         LUA_EXTRASPACE);
  luai_userstatethread(L, L1);
  stack_init(L1, L);  /* init stack */
  lua_unlock(L);
  return L1;
}
```

### 操作系统库

以下为`loslib.c`

```cpp
static const luaL_Reg syslib[] = {
  {"clock",     os_clock},
  {"date",      os_date},
  {"difftime",  os_difftime},
  {"execute",   os_execute},
  {"exit",      os_exit},
  {"getenv",    os_getenv},
  {"remove",    os_remove},
  {"rename",    os_rename},
  {"setlocale", os_setlocale},
  {"time",      os_time},
  {"tmpname",   os_tmpname},
  {NULL, NULL}
};
```

Lua os库示例

```cpp
static int os_time (lua_State *L) {
  time_t t;
  if (lua_isnoneornil(L, 1))  /* called without args? */
    t = time(NULL);  /* get current time */
  else {
    struct tm ts;
    luaL_checktype(L, 1, LUA_TTABLE);
    lua_settop(L, 1);  /* make sure table is at the top */
    ts.tm_year = getfield(L, "year", -1, 1900);
    ts.tm_mon = getfield(L, "month", -1, 1);
    ts.tm_mday = getfield(L, "day", -1, 0);
    ts.tm_hour = getfield(L, "hour", 12, 0);
    ts.tm_min = getfield(L, "min", 0, 0);
    ts.tm_sec = getfield(L, "sec", 0, 0);
    ts.tm_isdst = getboolfield(L, "isdst");
    t = mktime(&ts);
    setallfields(L, &ts);  /* update fields with normalized values */
  }
  if (t != (time_t)(l_timet)t || t == (time_t)(-1))
    return luaL_error(L,
                  "time result cannot be represented in this installation");
  l_pushtime(L, t);
  return 1;
}


static int os_difftime (lua_State *L) {
  time_t t1 = l_checktime(L, 1);
  time_t t2 = l_checktime(L, 2);
  lua_pushnumber(L, (lua_Number)difftime(t1, t2));
  return 1;
}

/* }====================================================== */


static int os_setlocale (lua_State *L) {
  static const int cat[] = {LC_ALL, LC_COLLATE, LC_CTYPE, LC_MONETARY,
                      LC_NUMERIC, LC_TIME};
  static const char *const catnames[] = {"all", "collate", "ctype", "monetary",
     "numeric", "time", NULL};
  const char *l = luaL_optstring(L, 1, NULL);
  int op = luaL_checkoption(L, 2, "all", catnames);
  lua_pushstring(L, setlocale(cat[op], l));
  return 1;
}
```

### 标准输入输出I/O库

以下为`liolib.c`

```cpp
/*
** functions for 'io' library
*/
static const luaL_Reg iolib[] = {
  {"close", io_close},
  {"flush", io_flush},
  {"input", io_input},
  {"lines", io_lines},
  {"open", io_open},
  {"output", io_output},
  {"popen", io_popen},
  {"read", io_read},
  {"tmpfile", io_tmpfile},
  {"type", io_type},
  {"write", io_write},
  {NULL, NULL}
};


/*
** methods for file handles
*/
static const luaL_Reg meth[] = {
  {"read", f_read},
  {"write", f_write},
  {"lines", f_lines},
  {"flush", f_flush},
  {"seek", f_seek},
  {"close", f_close},
  {"setvbuf", f_setvbuf},
  {NULL, NULL}
};
```

```cpp
typedef struct luaL_Stream {
  FILE *f;  /* stream (NULL for incompletely created streams) */
  lua_CFunction closef;  /* to close stream (NULL for closed streams) */
} luaL_Stream;
```

Lua io库函数示例

```cpp
static int io_type (lua_State *L) {
  LStream *p;
  luaL_checkany(L, 1);
  p = (LStream *)luaL_testudata(L, 1, LUA_FILEHANDLE);
  if (p == NULL)
    luaL_pushfail(L);  /* not a file */
  else if (isclosed(p))
    lua_pushliteral(L, "closed file");
  else
    lua_pushliteral(L, "file");
  return 1;
}

static int f_close (lua_State *L) {
  tofile(L);  /* make sure argument is an open stream */
  return aux_close(L);
}


static int io_close (lua_State *L) {
  if (lua_isnone(L, 1))  /* no argument? */
    lua_getfield(L, LUA_REGISTRYINDEX, IO_OUTPUT);  /* use standard output */
  return f_close(L);
}

/*
** function to close regular files
*/
static int io_fclose (lua_State *L) {
  LStream *p = tolstream(L);
  int res = fclose(p->f);
  return luaL_fileresult(L, (res == 0), NULL);
}

static int io_open (lua_State *L) {
  const char *filename = luaL_checkstring(L, 1);
  const char *mode = luaL_optstring(L, 2, "r");
  LStream *p = newfile(L);
  const char *md = mode;  /* to traverse/check mode */
  luaL_argcheck(L, l_checkmode(md), 2, "invalid mode");
  p->f = fopen(filename, mode);
  return (p->f == NULL) ? luaL_fileresult(L, 0, filename) : 1;
}

static int io_tmpfile (lua_State *L) {
  LStream *p = newfile(L);
  p->f = tmpfile();
  return (p->f == NULL) ? luaL_fileresult(L, 0, NULL) : 1;
}

static int g_iofile (lua_State *L, const char *f, const char *mode) {
  if (!lua_isnoneornil(L, 1)) {
    const char *filename = lua_tostring(L, 1);
    if (filename)
      opencheck(L, filename, mode);
    else {
      tofile(L);  /* check that it's a valid file handle */
      lua_pushvalue(L, 1);
    }
    lua_setfield(L, LUA_REGISTRYINDEX, f);
  }
  /* return current value */
  lua_getfield(L, LUA_REGISTRYINDEX, f);
  return 1;
}


static int io_input (lua_State *L) {
  return g_iofile(L, IO_INPUT, "r");
}


static int io_output (lua_State *L) {
  return g_iofile(L, IO_OUTPUT, "w");
}

/*
** Return an iteration function for 'io.lines'. If file has to be
** closed, also returns the file itself as a second result (to be
** closed as the state at the exit of a generic for).
返回“ io.lines”的迭代函数。 如果必须关闭文件，
则还返回文件本身作为第二个结果（以作为通用出口的状态关闭）。
*/
static int io_lines (lua_State *L) {
  int toclose;
  if (lua_isnone(L, 1)) lua_pushnil(L);  /* at least one argument */
  if (lua_isnil(L, 1)) {  /* no file name? */
    lua_getfield(L, LUA_REGISTRYINDEX, IO_INPUT);  /* get default input */
    lua_replace(L, 1);  /* put it at index 1 */
    tofile(L);  /* check that it's a valid file handle */
    toclose = 0;  /* do not close it after iteration */
  }
  else {  /* open a new file */
    const char *filename = luaL_checkstring(L, 1);
    opencheck(L, filename, "r");
    lua_replace(L, 1);  /* put file at index 1 */
    toclose = 1;  /* close it after iteration */
  }
  aux_lines(L, toclose);  /* push iteration function */
  if (toclose) {
    lua_pushnil(L);  /* state */
    lua_pushnil(L);  /* control */
    lua_pushvalue(L, 1);  /* file is the to-be-closed variable (4th result) */
    return 4;
  }
  else
    return 1;
}

static int read_line (lua_State *L, FILE *f, int chop) {
  luaL_Buffer b;
  int c;
  luaL_buffinit(L, &b);
  do {  /* may need to read several chunks to get whole line */
    char *buff = luaL_prepbuffer(&b);  /* preallocate buffer space */
    int i = 0;
    l_lockfile(f);  /* no memory errors can happen inside the lock */
    while (i < LUAL_BUFFERSIZE && (c = l_getc(f)) != EOF && c != '\n')
      buff[i++] = c;  /* read up to end of line or buffer limit */
    l_unlockfile(f);
    luaL_addsize(&b, i);
  } while (c != EOF && c != '\n');  /* repeat until end of line */
  if (!chop && c == '\n')  /* want a newline and have one? */
    luaL_addchar(&b, c);  /* add ending newline to result */
  luaL_pushresult(&b);  /* close buffer */
  /* return ok if read something (either a newline or something else) */
  return (c == '\n' || lua_rawlen(L, -1) > 0);
}


static void read_all (lua_State *L, FILE *f) {
  size_t nr;
  luaL_Buffer b;
  luaL_buffinit(L, &b);
  do {  /* read file in chunks of LUAL_BUFFERSIZE bytes */
    char *p = luaL_prepbuffer(&b);
    nr = fread(p, sizeof(char), LUAL_BUFFERSIZE, f);
    luaL_addsize(&b, nr);
  } while (nr == LUAL_BUFFERSIZE);
  luaL_pushresult(&b);  /* close buffer */
}


static int read_chars (lua_State *L, FILE *f, size_t n) {
  size_t nr;  /* number of chars actually read */
  char *p;
  luaL_Buffer b;
  luaL_buffinit(L, &b);
  p = luaL_prepbuffsize(&b, n);  /* prepare buffer to read whole block */
  nr = fread(p, sizeof(char), n, f);  /* try to read 'n' chars */
  luaL_addsize(&b, nr);
  luaL_pushresult(&b);  /* close buffer */
  return (nr > 0);  /* true iff read something */
}

/*
** Iteration function for 'lines'.
*/
static int io_readline (lua_State *L) {
  LStream *p = (LStream *)lua_touserdata(L, lua_upvalueindex(1));
  int i;
  int n = (int)lua_tointeger(L, lua_upvalueindex(2));
  if (isclosed(p))  /* file is already closed? */
    return luaL_error(L, "file is already closed");
  lua_settop(L , 1);
  luaL_checkstack(L, n, "too many arguments");
  for (i = 1; i <= n; i++)  /* push arguments to 'g_read' */
    lua_pushvalue(L, lua_upvalueindex(3 + i));
  n = g_read(L, p->f, 2);  /* 'n' is number of results */
  lua_assert(n > 0);  /* should return at least a nil */
  if (lua_toboolean(L, -n))  /* read at least one value? */
    return n;  /* return them */
  else {  /* first result is false: EOF or error */
    if (n > 1) {  /* is there error information? */
      /* 2nd result is error message */
      return luaL_error(L, "%s", lua_tostring(L, -n + 1));
    }
    if (lua_toboolean(L, lua_upvalueindex(3))) {  /* generator created file? */
      lua_settop(L, 0);  /* clear stack */
      lua_pushvalue(L, lua_upvalueindex(1));  /* push file at index 1 */
      aux_close(L);  /* close it */
    }
    return 0;
  }
}
```

### Lua utf8库

以下为`lutf8lib.c`

```cpp
static const luaL_Reg funcs[] = {
  {"offset", byteoffset},
  {"codepoint", codepoint},
  {"char", utfchar},
  {"len", utflen},
  {"codes", iter_codes},
  /* placeholders */
  {"charpattern", NULL},
  {NULL, NULL}
};
```

```cpp
/* pattern to match a single UTF-8 character */
#define UTF8PATT	"[\0-\x7F\xC2-\xFD][\x80-\xBF]*"
```

```cpp
/*
** Decode one UTF-8 sequence, returning NULL if byte sequence is
** invalid.  The array 'limits' stores the minimum value for each
** sequence length, to check for overlong representations. Its first
** entry forces an error for non-ascii bytes with no continuation
** bytes (count == 0).
*/
static const char *utf8_decode (const char *s, utfint *val, int strict) {
  static const utfint limits[] =
        {~(utfint)0, 0x80, 0x800, 0x10000u, 0x200000u, 0x4000000u};
  unsigned int c = (unsigned char)s[0];
  utfint res = 0;  /* final result */
  if (c < 0x80)  /* ascii? */
    res = c;
  else {
    int count = 0;  /* to count number of continuation bytes */
    for (; c & 0x40; c <<= 1) {  /* while it needs continuation bytes... */
      unsigned int cc = (unsigned char)s[++count];  /* read next byte */
      if ((cc & 0xC0) != 0x80)  /* not a continuation byte? */
        return NULL;  /* invalid byte sequence */
      res = (res << 6) | (cc & 0x3F);  /* add lower 6 bits from cont. byte */
    }
    res |= ((utfint)(c & 0x7F) << (count * 5));  /* add first byte */
    if (count > 5 || res > MAXUTF || res < limits[count])
      return NULL;  /* invalid byte sequence */
    s += count;  /* skip continuation bytes read */
  }
  if (strict) {
    /* check for invalid code points; too large or surrogates */
    if (res > MAXUNICODE || (0xD800u <= res && res <= 0xDFFFu))
      return NULL;
  }
  if (val) *val = res;
  return s + 1;  /* +1 to include first byte */
}

/*
** utfchar(n1, n2, ...)  -> char(n1)..char(n2)...
*/
static int utfchar (lua_State *L) {
  int n = lua_gettop(L);  /* number of arguments */
  if (n == 1)  /* optimize common case of single char */
    pushutfchar(L, 1);
  else {
    int i;
    luaL_Buffer b;
    luaL_buffinit(L, &b);
    for (i = 1; i <= n; i++) {
      pushutfchar(L, i);
      luaL_addvalue(&b);
    }
    luaL_pushresult(&b);
  }
  return 1;
}

/*
** utf8len(s [, i [, j [, lax]]]) --> number of characters that
** start in the range [i,j], or nil + current position if 's' is not
** well formed in that interval
*/
static int utflen (lua_State *L) {
  lua_Integer n = 0;  /* counter for the number of characters */
  size_t len;  /* string length in bytes */
  const char *s = luaL_checklstring(L, 1, &len);
  lua_Integer posi = u_posrelat(luaL_optinteger(L, 2, 1), len);
  lua_Integer posj = u_posrelat(luaL_optinteger(L, 3, -1), len);
  int lax = lua_toboolean(L, 4);
  luaL_argcheck(L, 1 <= posi && --posi <= (lua_Integer)len, 2,
                   "initial position out of string");
  luaL_argcheck(L, --posj < (lua_Integer)len, 3,
                   "final position out of string");
  while (posi <= posj) {
    const char *s1 = utf8_decode(s + posi, NULL, !lax);
    if (s1 == NULL) {  /* conversion error? */
      luaL_pushfail(L);  /* return fail ... */
      lua_pushinteger(L, posi + 1);  /* ... and current position */
      return 2;
    }
    posi = s1 - s;
    n++;
  }
  lua_pushinteger(L, n);
  return 1;
}
```

### Lua 动态库加载器库

以下为`loadlib.c`

此模块包含具有dlfcn的Unix系统的loadlib实现，Windows的实现以及其他系统的存根。

```cpp
/*
** LUA_CSUBSEP is the character that replaces dots in submodule names
** when searching for a C loader.
** LUA_LSUBSEP is the character that replaces dots in submodule names
** when searching for a Lua loader.
** LUA_CSUBSEP是在搜索C加载程序时替换子模块名称中点的字符。
** LUA_LSUBSEP是在搜索Lua加载程序时替换子模块名称中点的字符。
*/
#if !defined(LUA_CSUBSEP)
#define LUA_CSUBSEP		LUA_DIRSEP
#endif

#if !defined(LUA_LSUBSEP)
#define LUA_LSUBSEP		LUA_DIRSEP
#endif
```

```cpp
#if defined(LUA_USE_DLOPEN)	/* { */
/*
** {========================================================================
** This is an implementation of loadlib based on the dlfcn interface.
** The dlfcn interface is available in Linux, SunOS, Solaris, IRIX, FreeBSD,
** NetBSD, AIX 4.2, HPUX 11, and  probably most other Unix flavors, at least
** as an emulation layer on top of native functions.
**这是基于dlfcn接口的loadlib的实现。
** dlfcn界面在Linux，SunOS，Solaris，IRIX，FreeBSD，
**至少NetBSD，AIX 4.2，HPUX 11和大多数其他Unix版本
**作为本机功能之上的仿真层。
** =========================================================================
*/

#include <dlfcn.h>

/*
** Macro to convert pointer-to-void* to pointer-to-function. This cast
** is undefined according to ISO C, but POSIX assumes that it works.
** (The '__extension__' in gnu compilers is only to avoid warnings.)
*/
#if defined(__GNUC__)
#define cast_func(p) (__extension__ (lua_CFunction)(p))
#else
#define cast_func(p) ((lua_CFunction)(p))
#endif


static void lsys_unloadlib (void *lib) {
  dlclose(lib);
}


static void *lsys_load (lua_State *L, const char *path, int seeglb) {
  void *lib = dlopen(path, RTLD_NOW | (seeglb ? RTLD_GLOBAL : RTLD_LOCAL));
  if (lib == NULL) lua_pushstring(L, dlerror());
  return lib;
}


static lua_CFunction lsys_sym (lua_State *L, void *lib, const char *sym) {
  lua_CFunction f = cast_func(dlsym(lib, sym));
  if (f == NULL) lua_pushstring(L, dlerror());
  return f;
}

/* }====================================================== */



#elif defined(LUA_DL_DLL)	/* }{ */
/*
** {======================================================================
** This is an implementation of loadlib for Windows using native functions.
这是使用本机函数的Windows的loadlib的实现。
** =======================================================================
*/

#include <windows.h>


/*
** optional flags for LoadLibraryEx
*/
#if !defined(LUA_LLE_FLAGS)
#define LUA_LLE_FLAGS	0
#endif


#undef setprogdir


/*
** Replace in the path (on the top of the stack) any occurrence
** of LUA_EXEC_DIR with the executable's path.
*/
static void setprogdir (lua_State *L) {
  char buff[MAX_PATH + 1];
  char *lb;
  DWORD nsize = sizeof(buff)/sizeof(char);
  DWORD n = GetModuleFileNameA(NULL, buff, nsize);  /* get exec. name */
  if (n == 0 || n == nsize || (lb = strrchr(buff, '\\')) == NULL)
    luaL_error(L, "unable to get ModuleFileName");
  else {
    *lb = '\0';  /* cut name on the last '\\' to get the path */
    luaL_gsub(L, lua_tostring(L, -1), LUA_EXEC_DIR, buff);
    lua_remove(L, -2);  /* remove original string */
  }
}




static void pusherror (lua_State *L) {
  int error = GetLastError();
  char buffer[128];
  if (FormatMessageA(FORMAT_MESSAGE_IGNORE_INSERTS | FORMAT_MESSAGE_FROM_SYSTEM,
      NULL, error, 0, buffer, sizeof(buffer)/sizeof(char), NULL))
    lua_pushstring(L, buffer);
  else
    lua_pushfstring(L, "system error %d\n", error);
}

static void lsys_unloadlib (void *lib) {
  FreeLibrary((HMODULE)lib);
}


static void *lsys_load (lua_State *L, const char *path, int seeglb) {
  HMODULE lib = LoadLibraryExA(path, NULL, LUA_LLE_FLAGS);
  (void)(seeglb);  /* not used: symbols are 'global' by default */
  if (lib == NULL) pusherror(L);
  return lib;
}


static lua_CFunction lsys_sym (lua_State *L, void *lib, const char *sym) {
  lua_CFunction f = (lua_CFunction)GetProcAddress((HMODULE)lib, sym);
  if (f == NULL) pusherror(L);
  return f;
}

/* }====================================================== */


#else				/* }{ */
/*
** {======================================================
** Fallback for other systems
** =======================================================
*/

#undef LIB_FAIL
#define LIB_FAIL	"absent"


#define DLMSG	"dynamic libraries not enabled; check your Lua installation"


static void lsys_unloadlib (void *lib) {
  (void)(lib);  /* not used */
}


static void *lsys_load (lua_State *L, const char *path, int seeglb) {
  (void)(path); (void)(seeglb);  /* not used */
  lua_pushliteral(L, DLMSG);
  return NULL;
}


static lua_CFunction lsys_sym (lua_State *L, void *lib, const char *sym) {
  (void)(lib); (void)(sym);  /* not used */
  lua_pushliteral(L, DLMSG);
  return NULL;
}

/* }====================================================== */
#endif				/* } */

```

```cpp
LUAMOD_API int luaopen_package (lua_State *L) {
  createclibstable(L);
  luaL_newlib(L, pk_funcs);  /* create 'package' table */
  createsearcherstable(L);
  /* set paths */
  setpath(L, "path", LUA_PATH_VAR, LUA_PATH_DEFAULT);
  setpath(L, "cpath", LUA_CPATH_VAR, LUA_CPATH_DEFAULT);
  /* store config information */
  lua_pushliteral(L, LUA_DIRSEP "\n" LUA_PATH_SEP "\n" LUA_PATH_MARK "\n"
                     LUA_EXEC_DIR "\n" LUA_IGMARK "\n");
  lua_setfield(L, -2, "config");
  /* set field 'loaded' */
  luaL_getsubtable(L, LUA_REGISTRYINDEX, LUA_LOADED_TABLE);
  lua_setfield(L, -2, "loaded");
  /* set field 'preload' */
  luaL_getsubtable(L, LUA_REGISTRYINDEX, LUA_PRELOAD_TABLE);
  lua_setfield(L, -2, "preload");
  lua_pushglobaltable(L);
  lua_pushvalue(L, -2);  /* set 'package' as upvalue for next lib */
  luaL_setfuncs(L, ll_funcs, 1);  /* open lib into global table */
  lua_pop(L, 1);  /* pop global table */
  return 1;  /* return 'package' table */
}
```

## Lua 代码生成器

以下为`lcode.c`和`lcode.h`

将语法解析器生成的AST转换成虚拟机可以调用运行的代码。

```cpp
/*
** grep "ORDER OPR" if you change these enums  (ORDER OP)
grep“ ORDER OPR”，如果您更改了这些枚举（ORDER OP）
*/
typedef enum BinOpr {
  /* arithmetic operators */
  OPR_ADD, OPR_SUB, OPR_MUL, OPR_MOD, OPR_POW,
  OPR_DIV, OPR_IDIV,
  /* bitwise operators */
  OPR_BAND, OPR_BOR, OPR_BXOR,
  OPR_SHL, OPR_SHR,
  /* string operator */
  OPR_CONCAT,
  /* comparison operators */
  OPR_EQ, OPR_LT, OPR_LE,
  OPR_NE, OPR_GT, OPR_GE,
  /* logical operators */
  OPR_AND, OPR_OR,
  OPR_NOBINOPR
} BinOpr;
```

```cpp
LUAI_FUNC int luaK_code (FuncState *fs, Instruction i);
LUAI_FUNC int luaK_codeABx (FuncState *fs, OpCode o, int A, unsigned int Bx);
LUAI_FUNC int luaK_codeAsBx (FuncState *fs, OpCode o, int A, int Bx);
LUAI_FUNC int luaK_codeABCk (FuncState *fs, OpCode o, int A,
                                            int B, int C, int k);
LUAI_FUNC int luaK_isKint (expdesc *e);
LUAI_FUNC int luaK_exp2const (FuncState *fs, const expdesc *e, TValue *v);
LUAI_FUNC void luaK_fixline (FuncState *fs, int line);
LUAI_FUNC void luaK_nil (FuncState *fs, int from, int n);
LUAI_FUNC void luaK_reserveregs (FuncState *fs, int n);
LUAI_FUNC void luaK_checkstack (FuncState *fs, int n);
LUAI_FUNC void luaK_int (FuncState *fs, int reg, lua_Integer n);
LUAI_FUNC void luaK_dischargevars (FuncState *fs, expdesc *e);
LUAI_FUNC int luaK_exp2anyreg (FuncState *fs, expdesc *e);
LUAI_FUNC void luaK_exp2anyregup (FuncState *fs, expdesc *e);
LUAI_FUNC void luaK_exp2nextreg (FuncState *fs, expdesc *e);
LUAI_FUNC void luaK_exp2val (FuncState *fs, expdesc *e);
LUAI_FUNC int luaK_exp2RK (FuncState *fs, expdesc *e);
LUAI_FUNC void luaK_self (FuncState *fs, expdesc *e, expdesc *key);
LUAI_FUNC void luaK_indexed (FuncState *fs, expdesc *t, expdesc *k);
LUAI_FUNC void luaK_goiftrue (FuncState *fs, expdesc *e);
LUAI_FUNC void luaK_goiffalse (FuncState *fs, expdesc *e);
LUAI_FUNC void luaK_storevar (FuncState *fs, expdesc *var, expdesc *e);
LUAI_FUNC void luaK_setreturns (FuncState *fs, expdesc *e, int nresults);
LUAI_FUNC void luaK_setoneret (FuncState *fs, expdesc *e);
LUAI_FUNC int luaK_jump (FuncState *fs);
LUAI_FUNC void luaK_ret (FuncState *fs, int first, int nret);
LUAI_FUNC void luaK_patchlist (FuncState *fs, int list, int target);
LUAI_FUNC void luaK_patchtohere (FuncState *fs, int list);
LUAI_FUNC void luaK_concat (FuncState *fs, int *l1, int l2);
LUAI_FUNC int luaK_getlabel (FuncState *fs);
LUAI_FUNC void luaK_prefix (FuncState *fs, UnOpr op, expdesc *v, int line);
LUAI_FUNC void luaK_infix (FuncState *fs, BinOpr op, expdesc *v);
LUAI_FUNC void luaK_posfix (FuncState *fs, BinOpr op, expdesc *v1,
                            expdesc *v2, int line);
LUAI_FUNC void luaK_settablesize (FuncState *fs, int pc,
                                                 int ra, int rb, int rc);
LUAI_FUNC void luaK_setlist (FuncState *fs, int base, int nelems, int tostore);
LUAI_FUNC void luaK_finish (FuncState *fs);
LUAI_FUNC l_noret luaK_semerror (LexState *ls, const char *msg);
```

```cpp
/* Maximum number of registers in a Lua function (must fit in 8 bits) 
Lua函数中的最大寄存器数（必须适合8位） */
#define MAXREGS		255
```

Lua 代码生成器部分相关API

```cpp
/*
** If expression is a constant, fills 'v' with its value
** and returns 1. Otherwise, returns 0.
如果expression是常量，则用其值填充'v'并返回1。否则，返回0。
*/
int luaK_exp2const (FuncState *fs, const expdesc *e, TValue *v) {
  if (hasjumps(e))
    return 0;  /* not a constant */
  switch (e->k) {
    case VFALSE: case VTRUE:
      setbvalue(v, e->k == VTRUE);
      return 1;
    case VNIL:
      setnilvalue(v);
      return 1;
    case VKSTR: {
      setsvalue(fs->ls->L, v, e->u.strval);
      return 1;
    }
    case VCONST: {
      setobj(fs->ls->L, v, const2val(fs, e));
      return 1;
    }
    default: return tonumeral(e, v);
  }
}

/*
** Create a OP_LOADNIL instruction, but try to optimize: if the previous
** instruction is also OP_LOADNIL and ranges are compatible, adjust
** range of previous instruction instead of emitting a new one. (For
** instance, 'local a; local b' will generate a single opcode.)
创建一个OP_LOADNIL指令，但要进行优化：如果前一条指令也是OP_LOADNIL并且范围兼容，
请调整前一条指令的范围，而不发出新的指令。 （例如，“ local a; local b”将生成一个操作码。）
*/
void luaK_nil (FuncState *fs, int from, int n) {
  int l = from + n - 1;  /* last register to set nil */
  Instruction *previous = previousinstruction(fs);
  if (GET_OPCODE(*previous) == OP_LOADNIL) {  /* previous is LOADNIL? */
    int pfrom = GETARG_A(*previous);  /* get previous range */
    int pl = pfrom + GETARG_B(*previous);
    if ((pfrom <= from && from <= pl + 1) ||
        (from <= pfrom && pfrom <= l + 1)) {  /* can connect both? */
      if (pfrom < from) from = pfrom;  /* from = min(from, pfrom) */
      if (pl > l) l = pl;  /* l = max(l, pl) */
      SETARG_A(*previous, from);
      SETARG_B(*previous, l - from);
      return;
    }  /* else go through 否则经历 */
  }
  luaK_codeABC(fs, OP_LOADNIL, from, n - 1, 0);  /* else no optimization */
}

/*
** Gets the destination address of a jump instruction. Used to traverse
** a list of jumps.
获取跳转指令的目标地址。 用于遍历跳转列表。
*/
static int getjump (FuncState *fs, int pc) {
  int offset = GETARG_sJ(fs->f->code[pc]);
  if (offset == NO_JUMP)  /* point to itself represents end of list 
  指向自身表示列表的结尾 */
    return NO_JUMP;  /* end of list */
  else
    return (pc+1)+offset;  /* turn offset into absolute position */
}

/*
** Fix jump instruction at position 'pc' to jump to 'dest'.
** (Jump addresses are relative in Lua)
将跳转指令固定在位置“pc”以跳转到“目标”。 （跳转地址在Lua中是相对的）
*/
static void fixjump (FuncState *fs, int pc, int dest) {
  Instruction *jmp = &fs->f->code[pc];
  int offset = dest - (pc + 1);
  lua_assert(dest != NO_JUMP);
  if (!(-OFFSET_sJ <= offset && offset <= MAXARG_sJ - OFFSET_sJ))
    luaX_syntaxerror(fs->ls, "control structure too long");
  lua_assert(GET_OPCODE(*jmp) == OP_JMP);
  SETARG_sJ(*jmp, offset);
}
```

```cpp
/*
** Emit instruction 'i', checking for array sizes and saving also its
** line information. Return 'i' position.
发出指令“i”，检查数组大小并保存其行信息。返回“i”位置。
*/
int luaK_code (FuncState *fs, Instruction i) {
  Proto *f = fs->f;
  /* put new instruction in code array */
  luaM_growvector(fs->ls->L, f->code, fs->pc, f->sizecode, Instruction,
                  MAX_INT, "opcodes");
  f->code[fs->pc++] = i;
  savelineinfo(fs, f, fs->ls->lastline);
  return fs->pc - 1;  /* index of new instruction */
}


/*
** Format and emit an 'iABC' instruction. (Assertions check consistency
** of parameters versus opcode.)
格式化并发出“ iABC”指令。 （断言检查参数与操作码的一致性。）
*/
int luaK_codeABCk (FuncState *fs, OpCode o, int a, int b, int c, int k) {
  lua_assert(getOpMode(o) == iABC);
  lua_assert(a <= MAXARG_A && b <= MAXARG_B &&
             c <= MAXARG_C && (k & ~1) == 0);
  return luaK_code(fs, CREATE_ABCk(o, a, b, c, k));
}


/*
** Format and emit an 'iABx' instruction.
格式化并发出“iABx”指令。
*/
int luaK_codeABx (FuncState *fs, OpCode o, int a, unsigned int bc) {
  lua_assert(getOpMode(o) == iABx);
  lua_assert(a <= MAXARG_A && bc <= MAXARG_Bx);
  return luaK_code(fs, CREATE_ABx(o, a, bc));
}


/*
** Format and emit an 'iAsBx' instruction.
格式化并发出“iAsBx”指令。
*/
int luaK_codeAsBx (FuncState *fs, OpCode o, int a, int bc) {
  unsigned int b = bc + OFFSET_sBx;
  lua_assert(getOpMode(o) == iAsBx);
  lua_assert(a <= MAXARG_A && b <= MAXARG_Bx);
  return luaK_code(fs, CREATE_ABx(o, a, b));
}


/*
** Format and emit an 'isJ' instruction.
格式化并发出“isJ”指令。
*/
static int codesJ (FuncState *fs, OpCode o, int sj, int k) {
  unsigned int j = sj + OFFSET_sJ;
  lua_assert(getOpMode(o) == isJ);
  lua_assert(j <= MAXARG_sJ && (k & ~1) == 0);
  return luaK_code(fs, CREATE_sJ(o, j, k));
}


/*
** Emit an "extra argument" instruction (format 'iAx')
发出“额外参数”指令（格式为“ iAx”）
*/
static int codeextraarg (FuncState *fs, int a) {
  lua_assert(a <= MAXARG_Ax);
  return luaK_code(fs, CREATE_Ax(OP_EXTRAARG, a));
}

/*
** Emit a "load constant" instruction, using either 'OP_LOADK'
** (if constant index 'k' fits in 18 bits) or an 'OP_LOADKX'
** instruction with "extra argument".
使用 “OP_LOADK”（如果常数索引“k”适合18位）或
带有“额外参数”的“OP_LOADKX”指令发出“加载常数”指令。
*/
static int luaK_codek (FuncState *fs, int reg, int k) {
  if (k <= MAXARG_Bx)
    return luaK_codeABx(fs, OP_LOADK, reg, k);
  else {
    int p = luaK_codeABx(fs, OP_LOADKX, reg, 0);
    codeextraarg(fs, k);
    return p;
  }
}
```

```cpp
/*
** Add an integer to list of constants and return its index.
** Integers use userdata as keys to avoid collision with floats with
** same value; conversion to 'void*' is used only for hashing, so there
** are no "precision" problems.
在常量列表中添加一个整数并返回其索引。
整数使用userdata作为键，以避免与具有相同值的float发生冲突；
转换为 “void *” 仅用于哈希，因此不存在“精度”问题。
*/
static int luaK_intK (FuncState *fs, lua_Integer n) {
  TValue k, o;
  setpvalue(&k, cast_voidp(cast_sizet(n)));
  setivalue(&o, n);
  return addk(fs, &k, &o);
}

/*
** Add a float to list of constants and return its index.
在常量列表中添加一个float并返回其索引。
*/
static int luaK_numberK (FuncState *fs, lua_Number r) {
  TValue o;
  setfltvalue(&o, r);
  return addk(fs, &o, &o);  /* use number itself as key */
}


/*
** Add a boolean to list of constants and return its index.
向常量列表添加一个布尔值并返回其索引。
*/
static int boolK (FuncState *fs, int b) {
  TValue o;
  setbvalue(&o, b);
  return addk(fs, &o, &o);  /* use boolean itself as key */
}


/*
** Add nil to list of constants and return its index.
在常量列表中添加nil并返回其索引。
*/
static int nilK (FuncState *fs) {
  TValue k, v;
  setnilvalue(&v);
  /* cannot use nil as key; instead use table itself to represent nil */
  sethvalue(fs->ls->L, &k, fs->ls->h);
  return addk(fs, &k, &v);
}


/*
** Check whether 'i' can be stored in an 'sC' operand. Equivalent to
** (0 <= int2sC(i) && int2sC(i) <= MAXARG_C) but without risk of
** overflows in the hidden addition inside 'int2sC'.
检查“i”是否可以存储在“sC”操作数中。 
等效于（0 <= int2sC（i）&& int2sC（i）<= MAXARG_C），
但在'int2sC'内部隐藏的加法运算中没有溢出的风险。
*/
static int fitsC (lua_Integer i) {
  return (l_castS2U(i) + OFFSET_sC <= cast_uint(MAXARG_C));
}


/*
** Check whether 'i' can be stored in an 'sBx' operand.
检查 “i” 是否可以存储在“ sBx”操作数中。
*/
static int fitsBx (lua_Integer i) {
  return (-OFFSET_sBx <= i && i <= MAXARG_Bx - OFFSET_sBx);
}
```

```cpp
void luaK_int (FuncState *fs, int reg, lua_Integer i) {
  if (fitsBx(i))
    luaK_codeAsBx(fs, OP_LOADI, reg, cast_int(i));
  else
    luaK_codek(fs, reg, luaK_intK(fs, i));
}


static void luaK_float (FuncState *fs, int reg, lua_Number f) {
  lua_Integer fi;
  if (luaV_flttointeger(f, &fi, 0) && fitsBx(fi))
    luaK_codeAsBx(fs, OP_LOADF, reg, cast_int(fi));
  else
    luaK_codek(fs, reg, luaK_numberK(fs, f));
}


/*
** Convert a constant in 'v' into an expression description 'e'
将 “v” 中的常量转换为表达式描述 “e”
*/
static void const2exp (TValue *v, expdesc *e) {
  switch (ttypetag(v)) {
    case LUA_TNUMINT:
      e->k = VKINT; e->u.ival = ivalue(v);
      break;
    case LUA_TNUMFLT:
      e->k = VKFLT; e->u.nval = fltvalue(v);
      break;
    case LUA_TBOOLEAN:
      e->k = bvalue(v) ? VTRUE : VFALSE;
      break;
    case LUA_TNIL:
      e->k = VNIL;
      break;
    case LUA_TSHRSTR:  case LUA_TLNGSTR:
      e->k = VKSTR; e->u.strval = tsvalue(v);
      break;
    default: lua_assert(0);
  }
}


/*
** Fix an expression to return the number of results 'nresults'.
** Either 'e' is a multi-ret expression (function call or vararg)
** or 'nresults' is LUA_MULTRET (as any expression can satisfy that).
修复表达式以返回结果 “nresults”的数量。 
“e”是多列表达式（函数调用或vararg），或者“nresults”是LUA_MULTRET（因为任何表达式都可以满足）。
*/
void luaK_setreturns (FuncState *fs, expdesc *e, int nresults) {
  Instruction *pc = &getinstruction(fs, e);
  if (e->k == VCALL)  /* expression is an open function call? */
    SETARG_C(*pc, nresults + 1);
  else if (e->k == VVARARG) {
    SETARG_C(*pc, nresults + 1);
    SETARG_A(*pc, fs->freereg);
    luaK_reserveregs(fs, 1);
  }
  else lua_assert(nresults == LUA_MULTRET);
}
```

```cpp
/*
** Ensure that expression 'e' is not a variable (nor a constant).
** (Expression still may have jump lists.)
确保表达式“e”不是变量（也不是常量）。 
（表达式仍可能具有跳转列表。）
*/
void luaK_dischargevars (FuncState *fs, expdesc *e) {
  switch (e->k) {
    case VCONST: {
      const2exp(const2val(fs, e), e);
      break;
    }
    case VLOCAL: {  /* already in a register */
      e->u.info = e->u.var.sidx;
      e->k = VNONRELOC;  /* becomes a non-relocatable value */
      break;
    }
    case VUPVAL: {  /* move value to some (pending) register */
      e->u.info = luaK_codeABC(fs, OP_GETUPVAL, 0, e->u.info, 0);
      e->k = VRELOC;
      break;
    }
    case VINDEXUP: {
      e->u.info = luaK_codeABC(fs, OP_GETTABUP, 0, e->u.ind.t, e->u.ind.idx);
      e->k = VRELOC;
      break;
    }
    case VINDEXI: {
      freereg(fs, e->u.ind.t);
      e->u.info = luaK_codeABC(fs, OP_GETI, 0, e->u.ind.t, e->u.ind.idx);
      e->k = VRELOC;
      break;
    }
    case VINDEXSTR: {
      freereg(fs, e->u.ind.t);
      e->u.info = luaK_codeABC(fs, OP_GETFIELD, 0, e->u.ind.t, e->u.ind.idx);
      e->k = VRELOC;
      break;
    }
    case VINDEXED: {
      freeregs(fs, e->u.ind.t, e->u.ind.idx);
      e->u.info = luaK_codeABC(fs, OP_GETTABLE, 0, e->u.ind.t, e->u.ind.idx);
      e->k = VRELOC;
      break;
    }
    case VVARARG: case VCALL: {
      luaK_setoneret(fs, e);
      break;
    }
    default: break;  /* there is one value available (somewhere) */
  }
}


/*
** Ensures expression value is in register 'reg' (and therefore
** 'e' will become a non-relocatable expression).
** (Expression still may have jump lists.)
确保表达式值在寄存器“reg”中（因此，
“e”将成为不可重定位的表达式）。 （表达式仍可能具有跳转列表。）
*/
static void discharge2reg (FuncState *fs, expdesc *e, int reg) {
  luaK_dischargevars(fs, e);
  switch (e->k) {
    case VNIL: {
      luaK_nil(fs, reg, 1);
      break;
    }
    case VFALSE: case VTRUE: {
      luaK_codeABC(fs, OP_LOADBOOL, reg, e->k == VTRUE, 0);
      break;
    }
    case VKSTR: {
      str2K(fs, e);
    }  /* FALLTHROUGH */
    case VK: {
      luaK_codek(fs, reg, e->u.info);
      break;
    }
    case VKFLT: {
      luaK_float(fs, reg, e->u.nval);
      break;
    }
    case VKINT: {
      luaK_int(fs, reg, e->u.ival);
      break;
    }
    case VRELOC: {
      Instruction *pc = &getinstruction(fs, e);
      SETARG_A(*pc, reg);  /* instruction will put result in 'reg' */
      break;
    }
    case VNONRELOC: {
      if (reg != e->u.info)
        luaK_codeABC(fs, OP_MOVE, reg, e->u.info, 0);
      break;
    }
    default: {
      lua_assert(e->k == VJMP);
      return;  /* nothing to do... */
    }
  }
  e->u.info = reg;
  e->k = VNONRELOC;
}


/*
** Ensures expression value is in any register.
** (Expression still may have jump lists.)
确保表达式值在任何寄存器中（表达式仍可能具有跳转列表。）
*/
static void discharge2anyreg (FuncState *fs, expdesc *e) {
  if (e->k != VNONRELOC) {  /* no fixed register yet? */
    luaK_reserveregs(fs, 1);  /* get a register */
    discharge2reg(fs, e, fs->freereg-1);  /* put value there */
  }
}


static int code_loadbool (FuncState *fs, int A, int b, int jump) {
  luaK_getlabel(fs);  /* those instructions may be jump targets */
  return luaK_codeABC(fs, OP_LOADBOOL, A, b, jump);
}
```

```cpp
/*
** Emit code to go through if 'e' is false, jump otherwise.
如果“e”为假，则发送代码以通过，否则跳转。
*/
void luaK_goiffalse (FuncState *fs, expdesc *e) {
  int pc;  /* pc of new jump */
  luaK_dischargevars(fs, e);
  switch (e->k) {
    case VJMP: {
      pc = e->u.info;  /* already jump if true */
      break;
    }
    case VNIL: case VFALSE: {
      pc = NO_JUMP;  /* always false; do nothing */
      break;
    }
    default: {
      pc = jumponcond(fs, e, 1);  /* jump if true */
      break;
    }
  }
  luaK_concat(fs, &e->t, pc);  /* insert new jump in 't' list */
  luaK_patchtohere(fs, e->f);  /* false list jumps to here (to go through) */
  e->f = NO_JUMP;
}


/*
** Code 'not e', doing constant folding.
代码“not e”，不断折叠。
*/
static void codenot (FuncState *fs, expdesc *e) {
  switch (e->k) {
    case VNIL: case VFALSE: {
      e->k = VTRUE;  /* true == not nil == not false */
      break;
    }
    case VK: case VKFLT: case VKINT: case VKSTR: case VTRUE: {
      e->k = VFALSE;  /* false == not "x" == not 0.5 == not 1 == not true */
      break;
    }
    case VJMP: {
      negatecondition(fs, e);
      break;
    }
    case VRELOC:
    case VNONRELOC: {
      discharge2anyreg(fs, e);
      freeexp(fs, e);
      e->u.info = luaK_codeABC(fs, OP_NOT, 0, e->u.info, 0);
      e->k = VRELOC;
      break;
    }
    default: lua_assert(0);  /* cannot happen */
  }
  /* interchange true and false lists */
  { int temp = e->f; e->f = e->t; e->t = temp; }
  removevalues(fs, e->f);  /* values are useless when negated */
  removevalues(fs, e->t);
}


/*
** Check whether expression 'e' is a small literal string
检查表达式 “e” 是否为小文字字符串
*/
static int isKstr (FuncState *fs, expdesc *e) {
  return (e->k == VK && !hasjumps(e) && e->u.info <= MAXARG_B &&
          ttisshrstring(&fs->f->k[e->u.info]));
}

/*
** Check whether expression 'e' is a literal integer.
检查表达式 “e” 是否为文字整数。
*/
int luaK_isKint (expdesc *e) {
  return (e->k == VKINT && !hasjumps(e));
}


/*
** Check whether expression 'e' is a literal integer in
** proper range to fit in register C
检查表达式“e”是否为适当范围内的文字整数以适合寄存器C
*/
static int isCint (expdesc *e) {
  return luaK_isKint(e) && (l_castS2U(e->u.ival) <= l_castS2U(MAXARG_C));
}


/*
** Check whether expression 'e' is a literal integer in
** proper range to fit in register sC
检查表达式“e”是否为适当范围内的文字整数以适合寄存器sC
*/
static int isSCint (expdesc *e) {
  return luaK_isKint(e) && fitsC(e->u.ival);
}


/*
** Check whether expression 'e' is a literal integer or float in
** proper range to fit in a register (sB or sC).
检查表达式“e”是文字整数还是浮点数在适当范围内以适合寄存器（sB或sC）。
*/
static int isSCnumber (expdesc *e, int *pi, int *isfloat) {
  lua_Integer i;
  if (e->k == VKINT)
    i = e->u.ival;
  else if (e->k == VKFLT && luaV_flttointeger(e->u.nval, &i, 0))
    *isfloat = 1;
  else
    return 0;  /* not a number */
  if (!hasjumps(e) && fitsC(i)) {
    *pi = int2sC(cast_int(i));
    return 1;
  }
  else
    return 0;
}
```

```cpp
/*
** Create expression 't[k]'. 't' must have its final result already in a
** register or upvalue. Upvalues can only be indexed by literal strings.
** Keys can be literal strings in the constant table or arbitrary
** values in registers.
创建表达式 “t[k]”。 “t” 必须已将其最终结果存储在寄存器或高值中。
升值只能由文字字符串索引。 键可以是常量表中的文字字符串，也可以是寄存器中的任意值。
*/
void luaK_indexed (FuncState *fs, expdesc *t, expdesc *k) {
  if (k->k == VKSTR)
    str2K(fs, k);
  lua_assert(!hasjumps(t) &&
             (t->k == VLOCAL || t->k == VNONRELOC || t->k == VUPVAL));
  if (t->k == VUPVAL && !isKstr(fs, k))  /* upvalue indexed by non 'Kstr'? */
    luaK_exp2anyreg(fs, t);  /* put it in a register */
  if (t->k == VUPVAL) {
    t->u.ind.t = t->u.info;  /* upvalue index */
    t->u.ind.idx = k->u.info;  /* literal string */
    t->k = VINDEXUP;
  }
  else {
    /* register index of the table */
    t->u.ind.t = (t->k == VLOCAL) ? t->u.var.sidx: t->u.info;
    if (isKstr(fs, k)) {
      t->u.ind.idx = k->u.info;  /* literal string */
      t->k = VINDEXSTR;
    }
    else if (isCint(k)) {
      t->u.ind.idx = cast_int(k->u.ival);  /* int. constant in proper range */
      t->k = VINDEXI;
    }
    else {
      t->u.ind.idx = luaK_exp2anyreg(fs, k);  /* register */
      t->k = VINDEXED;
    }
  }
}


/*
** Return false if folding can raise an error.
** Bitwise operations need operands convertible to integers; division
** operations cannot have 0 as divisor.
如果折叠会引发错误，则返回false。 按位运算需要可转换为整数的操作数； 除法运算的除数不能为0。
*/
static int validop (int op, TValue *v1, TValue *v2) {
  switch (op) {
    case LUA_OPBAND: case LUA_OPBOR: case LUA_OPBXOR:
    case LUA_OPSHL: case LUA_OPSHR: case LUA_OPBNOT: {  /* conversion errors */
      lua_Integer i;
      return (tointegerns(v1, &i) && tointegerns(v2, &i));
    }
    case LUA_OPDIV: case LUA_OPIDIV: case LUA_OPMOD:  /* division by 0 */
      return (nvalue(v2) != 0);
    default: return 1;  /* everything else is valid */
  }
}


/*
** Try to "constant-fold" an operation; return 1 iff successful.
** (In this case, 'e1' has the final result.)
尝试“恒定折叠”操作； 成功时返回1。（在这种情况下，“ e1”具有最终结果。）
*/
static int constfolding (FuncState *fs, int op, expdesc *e1,
                                        const expdesc *e2) {
  TValue v1, v2, res;
  if (!tonumeral(e1, &v1) || !tonumeral(e2, &v2) || !validop(op, &v1, &v2))
    return 0;  /* non-numeric operands or not safe to fold */
  luaO_rawarith(fs->ls->L, op, &v1, &v2, &res);  /* does operation */
  if (ttisinteger(&res)) {
    e1->k = VKINT;
    e1->u.ival = ivalue(&res);
  }
  else {  /* folds neither NaN nor 0.0 (to avoid problems with -0.0) */
    lua_Number n = fltvalue(&res);
    if (luai_numisnan(n) || n == 0)
      return 0;
    e1->k = VKFLT;
    e1->u.nval = n;
  }
  return 1;
}


/*
** Emit code for unary expressions that "produce values"
** (everything but 'not').
** Expression to produce final result will be encoded in 'e'.
为 “产生值”（所有但不是）的一元表达式发出代码。 产生最终结果的表达式将被编码为 “e”。
*/
static void codeunexpval (FuncState *fs, OpCode op, expdesc *e, int line) {
  int r = luaK_exp2anyreg(fs, e);  /* opcodes operate only on registers */
  freeexp(fs, e);
  e->u.info = luaK_codeABC(fs, op, 0, r, 0);  /* generate opcode */
  e->k = VRELOC;  /* all those operations are relocatable */
  luaK_fixline(fs, line);
}


/*
** Emit code for binary expressions that "produce values"
** (everything but logical operators 'and'/'or' and comparison
** operators).
** Expression to produce final result will be encoded in 'e1'.
为“产生值”的二进制表达式发出代码（除逻辑运算符“and”/“or”和比较运算符外的所有内容）。 
产生最终结果的表达式将被编码为“e1”。
*/
static void finishbinexpval (FuncState *fs, expdesc *e1, expdesc *e2,
                             OpCode op, int v2, int flip, int line,
                             OpCode mmop, TMS event) {
  int v1 = luaK_exp2anyreg(fs, e1);
  int pc = luaK_codeABCk(fs, op, 0, v1, v2, 0);
  freeexps(fs, e1, e2);
  e1->u.info = pc;
  e1->k = VRELOC;  /* all those operations are relocatable */
  luaK_fixline(fs, line);
  luaK_codeABCk(fs, mmop, v1, v2, event, flip);  /* to call metamethod */
  luaK_fixline(fs, line);
}


/*
** Emit code for binary expressions that "produce values" over
** two registers.
为在两个寄存器中“产生值”的二进制表达式发出代码。
*/
static void codebinexpval (FuncState *fs, OpCode op,
                           expdesc *e1, expdesc *e2, int line) {
  int v2 = luaK_exp2anyreg(fs, e2);  /* both operands are in registers */
  lua_assert(OP_ADD <= op && op <= OP_SHR);
  finishbinexpval(fs, e1, e2, op, v2, 0, line, OP_MMBIN,
                  cast(TMS, (op - OP_ADD) + TM_ADD));
}


/*
** Code binary operators with immediate operands.
使用立即数对二进制运算符进行编码。
*/
static void codebini (FuncState *fs, OpCode op,
                       expdesc *e1, expdesc *e2, int flip, int line,
                       TMS event) {
  int v2 = int2sC(cast_int(e2->u.ival));  /* immediate operand */
  lua_assert(e2->k == VKINT);
  finishbinexpval(fs, e1, e2, op, v2, flip, line, OP_MMBINI, event);
}


/* Try to code a binary operator negating its second operand.
** For the metamethod, 2nd operand must keep its original value.
尝试编写一个二进制运算符，使其第二个操作数取反。 对于元方法，第二个操作数必须保留其原始值。
*/
static int finishbinexpneg (FuncState *fs, expdesc *e1, expdesc *e2,
                             OpCode op, int line, TMS event) {
  if (!luaK_isKint(e2))
    return 0;  /* not an integer constant */
  else {
    lua_Integer i2 = e2->u.ival;
    if (!(fitsC(i2) && fitsC(-i2)))
      return 0;  /* not in the proper range */
    else {  /* operating a small integer constant */
      int v2 = cast_int(i2);
      finishbinexpval(fs, e1, e2, op, int2sC(-v2), 0, line, OP_MMBINI, event);
      /* correct metamethod argument */
      SETARG_B(fs->f->code[fs->pc - 1], int2sC(v2));
      return 1;  /* successfully coded */
    }
  }
}
```

```cpp
/*
** Code arithmetic operators ('+', '-', ...). If second operand is a
** constant in the proper range, use variant opcodes with K operands.
代码算术运算符（'+'，'-'，...）。 如果第二个操作数是在适当范围内的常数，
则使用带有K个操作数的变体操作码。
*/
static void codearith (FuncState *fs, BinOpr opr,
                       expdesc *e1, expdesc *e2, int flip, int line) {
  TMS event = cast(TMS, opr + TM_ADD);
  if (tonumeral(e2, NULL) && luaK_exp2K(fs, e2)) {  /* K operand? */
    int v2 = e2->u.info;  /* K index */
    OpCode op = cast(OpCode, opr + OP_ADDK);
    finishbinexpval(fs, e1, e2, op, v2, flip, line, OP_MMBINK, event);
  }
  else {  /* 'e2' is neither an immediate nor a K operand */
    OpCode op = cast(OpCode, opr + OP_ADD);
    if (flip)
      swapexps(e1, e2);  /* back to original order */
    codebinexpval(fs, op, e1, e2, line);  /* use standard operators */
  }
}


/*
** Code commutative operators ('+', '*'). If first operand is a
** numeric constant, change order of operands to try to use an
** immediate or K operator.
代码交换运算符（“ +”，“ *”）。 如果第一个操作数是数字常量，
请更改操作数的顺序以尝试使用立即数或K运算符。
*/
static void codecommutative (FuncState *fs, BinOpr op,
                             expdesc *e1, expdesc *e2, int line) {
  int flip = 0;
  if (tonumeral(e1, NULL)) {  /* is first operand a numeric constant? */
    swapexps(e1, e2);  /* change order */
    flip = 1;
  }
  if (op == OPR_ADD && isSCint(e2))  /* immediate operand? */
    codebini(fs, cast(OpCode, OP_ADDI), e1, e2, flip, line, TM_ADD);
  else
    codearith(fs, op, e1, e2, flip, line);
}


/*
** Code bitwise operations; they are all associative, so the function
** tries to put an integer constant as the 2nd operand (a K operand).
代码按位运算；它们都是关联的，因此该函数尝试将整数常量作为第二个操作数（K个操作数）。
*/
static void codebitwise (FuncState *fs, BinOpr opr,
                         expdesc *e1, expdesc *e2, int line) {
  int flip = 0;
  int v2;
  OpCode op;
  if (e1->k == VKINT && luaK_exp2RK(fs, e1)) {
    swapexps(e1, e2);  /* 'e2' will be the constant operand */
    flip = 1;
  }
  else if (!(e2->k == VKINT && luaK_exp2RK(fs, e2))) {  /* no constants? */
    op = cast(OpCode, opr + OP_ADD);
    codebinexpval(fs, op, e1, e2, line);  /* all-register opcodes */
    return;
  }
  v2 = e2->u.info;  /* index in K array */
  op = cast(OpCode, opr + OP_ADDK);
  lua_assert(ttisinteger(&fs->f->k[v2]));
  finishbinexpval(fs, e1, e2, op, v2, flip, line, OP_MMBINK,
                  cast(TMS, opr + TM_ADD));
}


/*
** Emit code for order comparisons. When using an immediate operand,
** 'isfloat' tells whether the original value was a float.
发出代码以进行订单比较。使用立即数操作数时，“isfloat” 会告知原始值是否为浮点型。
*/
static void codeorder (FuncState *fs, OpCode op, expdesc *e1, expdesc *e2) {
  int r1, r2;
  int im;
  int isfloat = 0;
  if (isSCnumber(e2, &im, &isfloat)) {
    /* use immediate operand */
    r1 = luaK_exp2anyreg(fs, e1);
    r2 = im;
    op = cast(OpCode, (op - OP_LT) + OP_LTI);
  }
  else if (isSCnumber(e1, &im, &isfloat)) {
    /* transform (A < B) to (B > A) and (A <= B) to (B >= A) */
    r1 = luaK_exp2anyreg(fs, e2);
    r2 = im;
    op = (op == OP_LT) ? OP_GTI : OP_GEI;
  }
  else {  /* regular case, compare two registers */
    r1 = luaK_exp2anyreg(fs, e1);
    r2 = luaK_exp2anyreg(fs, e2);
  }
  freeexps(fs, e1, e2);
  e1->u.info = condjump(fs, op, r1, r2, isfloat, 1);
  e1->k = VJMP;
}


/*
** Emit code for equality comparisons ('==', '~=').
** 'e1' was already put as RK by 'luaK_infix'.
发出用于相等性比较的代码（'=='，'〜='）。
'luaK_infix'已将'e1'作为RK放置。
*/
static void codeeq (FuncState *fs, BinOpr opr, expdesc *e1, expdesc *e2) {
  int r1, r2;
  int im;
  int isfloat = 0;  /* not needed here, but kept for symmetry */
  OpCode op;
  if (e1->k != VNONRELOC) {
    lua_assert(e1->k == VK || e1->k == VKINT || e1->k == VKFLT);
    swapexps(e1, e2);
  }
  r1 = luaK_exp2anyreg(fs, e1);  /* 1nd expression must be in register */
  if (isSCnumber(e2, &im, &isfloat)) {
    op = OP_EQI;
    r2 = im;  /* immediate operand */
  }
  else if (luaK_exp2RK(fs, e2)) {  /* 1st expression is constant? */
    op = OP_EQK;
    r2 = e2->u.info;  /* constant index */
  }
  else {
    op = OP_EQ;  /* will compare two registers */
    r2 = luaK_exp2anyreg(fs, e2);
  }
  freeexps(fs, e1, e2);
  e1->u.info = condjump(fs, op, r1, r2, isfloat, (opr == OPR_EQ));
  e1->k = VJMP;
}
```

```cpp
/*
** Finalize code for binary operation, after reading 2nd operand.
在读取第二个操作数之后，完成二进制操作的代码。
*/
void luaK_posfix (FuncState *fs, BinOpr opr,
                  expdesc *e1, expdesc *e2, int line) {
  luaK_dischargevars(fs, e2);
  if (foldbinop(opr) && constfolding(fs, opr + LUA_OPADD, e1, e2))
    return;  /* done by folding */
  switch (opr) {
    case OPR_AND: {
      lua_assert(e1->t == NO_JUMP);  /* list closed by 'luaK_infix' */
      luaK_concat(fs, &e2->f, e1->f);
      *e1 = *e2;
      break;
    }
    case OPR_OR: {
      lua_assert(e1->f == NO_JUMP);  /* list closed by 'luaK_infix' */
      luaK_concat(fs, &e2->t, e1->t);
      *e1 = *e2;
      break;
    }
    case OPR_CONCAT: {  /* e1 .. e2 */
      luaK_exp2nextreg(fs, e2);
      codeconcat(fs, e1, e2, line);
      break;
    }
    case OPR_ADD: case OPR_MUL: {
      codecommutative(fs, opr, e1, e2, line);
      break;
    }
    case OPR_SUB: {
      if (finishbinexpneg(fs, e1, e2, OP_ADDI, line, TM_SUB))
        break; /* coded as (r1 + -I) */
      /* ELSE */
    }  /* FALLTHROUGH */
    case OPR_DIV: case OPR_IDIV: case OPR_MOD: case OPR_POW: {
      codearith(fs, opr, e1, e2, 0, line);
      break;
    }
    case OPR_BAND: case OPR_BOR: case OPR_BXOR: {
      codebitwise(fs, opr, e1, e2, line);
      break;
    }
    case OPR_SHL: {
      if (isSCint(e1)) {
        swapexps(e1, e2);
        codebini(fs, OP_SHLI, e1, e2, 1, line, TM_SHL);  /* I << r2 */
      }
      else if (finishbinexpneg(fs, e1, e2, OP_SHRI, line, TM_SHL)) {
        /* coded as (r1 >> -I) */;
      }
      else  /* regular case (two registers) */
       codebinexpval(fs, OP_SHL, e1, e2, line);
      break;
    }
    case OPR_SHR: {
      if (isSCint(e2))
        codebini(fs, OP_SHRI, e1, e2, 0, line, TM_SHR);  /* r1 >> I */
      else  /* regular case (two registers) */
        codebinexpval(fs, OP_SHR, e1, e2, line);
      break;
    }
    case OPR_EQ: case OPR_NE: {
      codeeq(fs, opr, e1, e2);
      break;
    }
    case OPR_LT: case OPR_LE: {
      OpCode op = cast(OpCode, (opr - OPR_EQ) + OP_EQ);
      codeorder(fs, op, e1, e2);
      break;
    }
    case OPR_GT: case OPR_GE: {
      /* '(a > b)' <=> '(b < a)';  '(a >= b)' <=> '(b <= a)' */
      OpCode op = cast(OpCode, (opr - OPR_NE) + OP_EQ);
      swapexps(e1, e2);
      codeorder(fs, op, e1, e2);
      break;
    }
    default: lua_assert(0);
  }
}
```

```cpp
/*
** Do a final pass over the code of a function, doing small peephole
** optimizations and adjustments.
对函数代码进行最后遍历，进行一些小的窥孔优化和调整。
*/
void luaK_finish (FuncState *fs) {
  int i;
  Proto *p = fs->f;
  for (i = 0; i < fs->pc; i++) {
    Instruction *pc = &p->code[i];
    lua_assert(i == 0 || isOT(*(pc - 1)) == isIT(*pc));
    switch (GET_OPCODE(*pc)) {
      case OP_RETURN0: case OP_RETURN1: {
        if (!(fs->needclose || p->is_vararg))
          break;  /* no extra work */
        /* else use OP_RETURN to do the extra work */
        SET_OPCODE(*pc, OP_RETURN);
      }  /* FALLTHROUGH */
      case OP_RETURN: case OP_TAILCALL: {
        if (fs->needclose)
          SETARG_k(*pc, 1);  /* signal that it needs to close */
        if (p->is_vararg)
          SETARG_C(*pc, p->numparams + 1);  /* signal that it is vararg */
        break;
      }
      case OP_JMP: {
        int target = finaltarget(p->code, i);
        fixjump(fs, i, target);
        break;
      }
      default: break;
    }
  }
}
```

## Lua C类型函数

以下为`lctype.c`和`lctype.h`

```cpp
/*
** add 1 to char to allow index -1 (EOZ)
*/
#define testprop(c,p)	(luai_ctype_[(c)+1] & (p))

/*
** 'lalpha' (Lua alphabetic) and 'lalnum' (Lua alphanumeric) both include '_'
*/
#define lislalpha(c)	testprop(c, MASK(ALPHABIT))
#define lislalnum(c)	testprop(c, (MASK(ALPHABIT) | MASK(DIGITBIT)))
#define lisdigit(c)	testprop(c, MASK(DIGITBIT))
#define lisspace(c)	testprop(c, MASK(SPACEBIT))
#define lisprint(c)	testprop(c, MASK(PRINTBIT))
#define lisxdigit(c)	testprop(c, MASK(XDIGITBIT))

/*
** this 'ltolower' only works for alphabetic characters
这个“函数”仅适用于字母字符
*/
#define ltolower(c)	((c) | ('A' ^ 'a'))


/* two more entries for 0 and -1 (EOZ) */
LUAI_DDEC(const lu_byte luai_ctype_[UCHAR_MAX + 2];)
```

## Lua 调试 (Debug)

以下为`ldblib.c`, `ldebug.c`和`ldebug.h`

```cpp
struct lua_Debug {
  int event;
  const char *name;	/* (n) */
  const char *namewhat;	/* (n) 'global', 'local', 'field', 'method' */
  const char *what;	/* (S) 'Lua', 'C', 'main', 'tail' */
  const char *source;	/* (S) */
  size_t srclen;	/* (S) */
  int currentline;	/* (l) */
  int linedefined;	/* (S) */
  int lastlinedefined;	/* (S) */
  unsigned char nups;	/* (u) number of upvalues */
  unsigned char nparams;/* (u) number of parameters */
  char isvararg;        /* (u) */
  char istailcall;	/* (t) */
  unsigned short ftransfer;   /* (r) index of first value transferred */
  unsigned short ntransfer;   /* (r) number of transferred values */
  char short_src[LUA_IDSIZE]; /* (S) */
  /* private part */
  struct CallInfo *i_ci;  /* active function */
};
```

```cpp
#define ABSLINEINFO	(-0x80)

LUAI_FUNC int luaG_getfuncline (const Proto *f, int pc);
LUAI_FUNC const char *luaG_findlocal (lua_State *L, CallInfo *ci, int n,
                                                    StkId *pos);
LUAI_FUNC l_noret luaG_typeerror (lua_State *L, const TValue *o,
                                                const char *opname);
LUAI_FUNC l_noret luaG_forerror (lua_State *L, const TValue *o,
                                               const char *what);
LUAI_FUNC l_noret luaG_concaterror (lua_State *L, const TValue *p1,
                                                  const TValue *p2);
LUAI_FUNC l_noret luaG_opinterror (lua_State *L, const TValue *p1,
                                                 const TValue *p2,
                                                 const char *msg);
LUAI_FUNC l_noret luaG_tointerror (lua_State *L, const TValue *p1,
                                                 const TValue *p2);
LUAI_FUNC l_noret luaG_ordererror (lua_State *L, const TValue *p1,
                                                 const TValue *p2);
LUAI_FUNC l_noret luaG_runerror (lua_State *L, const char *fmt, ...);
LUAI_FUNC const char *luaG_addinfo (lua_State *L, const char *msg,
                                                  TString *src, int line);
LUAI_FUNC l_noret luaG_errormsg (lua_State *L);
LUAI_FUNC int luaG_traceexec (lua_State *L, const Instruction *pc);
```

```cpp
/*
** Get the line corresponding to instruction 'pc' in function 'f';
** first gets a base line and from there does the increments until
** the desired instruction.
在函数“f”中获取与指令“pc”相对应的行； 首先获取基线，然后从那里开始递增，直到所需的指令为止。
*/
int luaG_getfuncline (const Proto *f, int pc) {
  if (f->lineinfo == NULL)  /* no debug information? */
    return -1;
  else {
    int basepc;
    int baseline = getbaseline(f, pc, &basepc);
    while (basepc++ < pc) {  /* walk until given instruction */
      lua_assert(f->lineinfo[basepc] != ABSLINEINFO);
      baseline += f->lineinfo[basepc];  /* correct line */
    }
    return baseline;
  }
}

const char *luaG_findlocal (lua_State *L, CallInfo *ci, int n, StkId *pos) {
  StkId base = ci->func + 1;
  const char *name = NULL;
  if (isLua(ci)) {
    if (n < 0)  /* access to vararg values? */
      return findvararg(ci, -n, pos);
    else
      name = luaF_getlocalname(ci_func(ci)->p, n, currentpc(ci));
  }
  if (name == NULL) {  /* no 'standard' name? */
    StkId limit = (ci == L->ci) ? L->top : ci->next->func;
    if (limit - base >= n && n > 0) {  /* is 'n' inside 'ci' stack? */
      /* generic name for any valid slot */
      name = isLua(ci) ? "(temporary)" : "(C temporary)";
    }
    else
      return NULL;  /* no name */
  }
  if (pos)
    *pos = base + (n - 1);
  return name;
}
```

```cpp
l_noret luaG_runerror (lua_State *L, const char *fmt, ...) {
  CallInfo *ci = L->ci;
  const char *msg;
  va_list argp;
  luaC_checkGC(L);  /* error message uses memory */
  va_start(argp, fmt);
  msg = luaO_pushvfstring(L, fmt, argp);  /* format message */
  va_end(argp);
  if (isLua(ci))  /* if Lua function, add source:line information */
    luaG_addinfo(L, msg, ci_func(ci)->p->source, currentline(ci));
  luaG_errormsg(L);
}
```
