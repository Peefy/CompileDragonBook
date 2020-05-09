
# Lua 编译器源码笔记

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
  lu_byte genmajormul;  /* control for major generational collections 控制主要的世代收藏*/
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

可以延迟变量和表达式的代码生成，以允许优化； “ expdesc”结构描述了可能延迟的变量/表达式。 它具有其“主要”值的说明以及也可以产生其值的条件跳转列表（生成的由短路运算符“和” /“或”表示。

## Lua 预编译

以下为`lundump.c`和`lundump.h`

### 保存预编译的Lua块

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

## Lua 对象

以下为`lobject.c`和`lobject.h`

## Lua Table (hash)

以下为`ltable.c`和`ltable.h`

## Lua 字符串和模式匹配

以下为`lstring.c`和`lstring.h`

## Lua API

以下为`lapi.c`和`lapi.h`

对Lua线程状态`lua_State`的操作API，将不同的Lua类型压入栈，从栈中弹出不同的类型

## 建立Lua库的辅助功能

以下为`lauxlib.c`和`lauxlib.h`

在`lua_State`中找出索引等操作API，与**Lua API**类似。

## Lua 限制 (Limit)

以下为`llimit.c`和`llimit.h`

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

## Lua 虚拟机

以下为`lvm.c`和`lvm.h`

### Lua 堆栈和调用结构

以下为`ldo.c`和`ldo.h`

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

## Lua 字符串

以下为`lstrlib.c`和`lstring.h`

## Lua 函数库

### Lua 基本函数和基础库

以下为`lbaselib`和`lualib.h`

### Lua 时间库

以下为`ltm.c`和`ltm.h`

### Lua string库

以下为`lstring.c`,

### Lua table库

以下为`ltable.c`和`ltable.h`

### Lua 数学库

以下为`lmathlib.c`

### Lua 协程库

以下为`lcorolib.c`和`lcorolib.h`

#### Lua 线程 

### 操作系统库

以下为`loslib.c`

### 标准输入输出I/O库

以下为`liolib.c`

### Lua utf8库

以下为`lutf8lib.c`

## Lua 代码生成器

以下为`lcode.c`和`lcode.h`

## Lua C类型函数

以下为`lctype.c`和`lctype.h`

## Lua 动态库加载器

以下为`loadlib.c`

## Lua 调试 (Debug)

以下为`ldblib.c`, `ldebug.c`和`ldebug.h`
