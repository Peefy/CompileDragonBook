
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



## Lua 配置文件

```cpp
#define LUA_API		extern
```

## Lua 预编译

### 保存预编译的Lua块

## Lua 缓冲流

## Lua 内存管理

## Lua 对象

## Lua Table (hash)

## Lua 字符串和模式匹配

## Lua API

## Lua 限制 (Limit)

## Lua 词法分析器

## Lua 语法分析器

## Lua 垃圾回收 (GC)

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

### Lua 堆栈和调用结构

### Lua 虚拟机的操作码

## Lua 原型 (prototypes) 和闭包 (closures)

## Lua 字符串

### Lua 模式匹配

## Lua 文件IO

## Lua 函数库

### 建立Lua库的辅助功能

### Lua 基础库

### Lua 时间库

### Lua 数学库

### Lua 协程库

#### Lua 线程 

### 标准I/O（和系统）库

## Lua 代码生成器

## Lua utf8库

## Lua 标记方法

## Lua 动态库加载器

## Lua 调试 (Debug)
