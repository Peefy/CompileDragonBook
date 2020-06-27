
# Python 编译器 CPython 源码笔记

## CPython 主函数和命令行程序

包含了**CPython配置文件**和**CPython辅助函数**的大多数功能，定义了CPython的大、小和发布版本号，发布版本数字，发布字符串等。主要位于`Modules/main.c`中。

两种运行Python编译器的方式，wchar模式和bytes模式

```cpp
int
Py_Main(int argc, wchar_t **argv)
{
    _PyArgv args = {
        .argc = argc,
        .use_bytes_argv = 0,
        .bytes_argv = NULL,
        .wchar_argv = argv};
    return pymain_main(&args);
}


int
Py_BytesMain(int argc, char **argv)
{
    _PyArgv args = {
        .argc = argc,
        .use_bytes_argv = 1,
        .bytes_argv = argv,
        .wchar_argv = NULL};
    return pymain_main(&args);
}
```

## CPython Parser

### CPython 词法分析器

`Parser/token.c`,`Parser/tokenizer.c`

```cpp
#define ENDMARKER       0
#define NAME            1
#define NUMBER          2
#define STRING          3
#define NEWLINE         4
#define INDENT          5
#define DEDENT          6
#define LPAR            7
#define RPAR            8
#define LSQB            9
#define RSQB            10
#define COLON           11
#define COMMA           12
#define SEMI            13
#define PLUS            14
#define MINUS           15
#define STAR            16
#define SLASH           17
#define VBAR            18
#define AMPER           19
#define LESS            20
#define GREATER         21
#define EQUAL           22
#define DOT             23
#define PERCENT         24
#define LBRACE          25
#define RBRACE          26
#define EQEQUAL         27
#define NOTEQUAL        28
#define LESSEQUAL       29
#define GREATEREQUAL    30
#define TILDE           31
#define CIRCUMFLEX      32
#define LEFTSHIFT       33
#define RIGHTSHIFT      34
#define DOUBLESTAR      35
#define PLUSEQUAL       36
#define MINEQUAL        37
#define STAREQUAL       38
#define SLASHEQUAL      39
#define PERCENTEQUAL    40
#define AMPEREQUAL      41
#define VBAREQUAL       42
#define CIRCUMFLEXEQUAL 43
#define LEFTSHIFTEQUAL  44
#define RIGHTSHIFTEQUAL 45
#define DOUBLESTAREQUAL 46
#define DOUBLESLASH     47
#define DOUBLESLASHEQUAL 48
#define AT              49
#define ATEQUAL         50
#define RARROW          51
#define ELLIPSIS        52
#define COLONEQUAL      53
#define OP              54
#define AWAIT           55
#define ASYNC           56
#define TYPE_IGNORE     57
#define TYPE_COMMENT    58
#define ERRORTOKEN      59
#define N_TOKENS        63
#define NT_OFFSET       256

/* Special definitions for cooperation with parser */

#define ISTERMINAL(x)           ((x) < NT_OFFSET)
#define ISNONTERMINAL(x)        ((x) >= NT_OFFSET)
#define ISEOF(x)                ((x) == ENDMARKER)
#define ISWHITESPACE(x)         ((x) == ENDMARKER || \
                                 (x) == NEWLINE   || \
                                 (x) == INDENT    || \
                                 (x) == DEDENT)

int
PyToken_OneChar(int c1)
{
    switch (c1) {
    case '%': return PERCENT;
    case '&': return AMPER;
    case '(': return LPAR;
    case ')': return RPAR;
    case '*': return STAR;
    case '+': return PLUS;
    case ',': return COMMA;
    case '-': return MINUS;
    case '.': return DOT;
    case '/': return SLASH;
    case ':': return COLON;
    case ';': return SEMI;
    case '<': return LESS;
    case '=': return EQUAL;
    case '>': return GREATER;
    case '@': return AT;
    case '[': return LSQB;
    case ']': return RSQB;
    case '^': return CIRCUMFLEX;
    case '{': return LBRACE;
    case '|': return VBAR;
    case '}': return RBRACE;
    case '~': return TILDE;
    }
    return OP;
}

int
PyToken_TwoChars(int c1, int c2)
{
    switch (c1) {
    case '!':
        switch (c2) {
        case '=': return NOTEQUAL;
        }
        break;
    case '%':
        switch (c2) {
        case '=': return PERCENTEQUAL;
        }
        break;
    case '&':
        switch (c2) {
        case '=': return AMPEREQUAL;
        }
        break;
    case '*':
        switch (c2) {
        case '*': return DOUBLESTAR;
        case '=': return STAREQUAL;
        }
        break;
    case '+':
        switch (c2) {
        case '=': return PLUSEQUAL;
        }
        break;
    case '-':
        switch (c2) {
        case '=': return MINEQUAL;
        case '>': return RARROW;
        }
        break;
    case '/':
        switch (c2) {
        case '/': return DOUBLESLASH;
        case '=': return SLASHEQUAL;
        }
        break;
    case ':':
        switch (c2) {
        case '=': return COLONEQUAL;
        }
        break;
    case '<':
        switch (c2) {
        case '<': return LEFTSHIFT;
        case '=': return LESSEQUAL;
        case '>': return NOTEQUAL;
        }
        break;
    case '=':
        switch (c2) {
        case '=': return EQEQUAL;
        }
        break;
    case '>':
        switch (c2) {
        case '=': return GREATEREQUAL;
        case '>': return RIGHTSHIFT;
        }
        break;
    case '@':
        switch (c2) {
        case '=': return ATEQUAL;
        }
        break;
    case '^':
        switch (c2) {
        case '=': return CIRCUMFLEXEQUAL;
        }
        break;
    case '|':
        switch (c2) {
        case '=': return VBAREQUAL;
        }
        break;
    }
    return OP;
}

int
PyToken_ThreeChars(int c1, int c2, int c3)
{
    switch (c1) {
    case '*':
        switch (c2) {
        case '*':
            switch (c3) {
            case '=': return DOUBLESTAREQUAL;
            }
            break;
        }
        break;
    case '.':
        switch (c2) {
        case '.':
            switch (c3) {
            case '.': return ELLIPSIS;
            }
            break;
        }
        break;
    case '/':
        switch (c2) {
        case '/':
            switch (c3) {
            case '=': return DOUBLESLASHEQUAL;
            }
            break;
        }
        break;
    case '<':
        switch (c2) {
        case '<':
            switch (c3) {
            case '=': return LEFTSHIFTEQUAL;
            }
            break;
        }
        break;
    case '>':
        switch (c2) {
        case '>':
            switch (c3) {
            case '=': return RIGHTSHIFTEQUAL;
            }
            break;
        }
        break;
    }
    return OP;
}
```

词法分析器状态和API

```cpp
#define MAXINDENT 100   /* Max indentation level 最大缩进级别 */
#define MAXLEVEL 200    /* Max parentheses level 最大括号级别 */

enum decoding_state {
    STATE_INIT,
    STATE_RAW,
    STATE_NORMAL        /* have a codec associated with input 具有与输入关联的编解码器 */
};

/* Tokenizer state 词法分析器状态 */
struct tok_state {
    /* Input state; buf <= cur <= inp <= end 输入状态 */
    /* NB an entire line is held in the buffer 整行都保存在缓冲区中 */
    char *buf;          /* Input buffer, or NULL; malloc'ed if fp != NULL 输入缓冲  */
    char *cur;          /* Next character in buffer 缓冲区中下一个字符 */
    char *inp;          /* End of data in buffer 缓冲区中的数据结束 */
    const char *end;    /* End of input buffer if buf != NULL 当buf不为空时输入缓冲的结尾 */
    const char *start;  /* Start of current token if not NULL 当前记号的开始 */
    int done;           /* E_OK normally, E_EOF at EOF, otherwise error code 是否完成解析 */
    /* NB If done != E_OK, cur must be == inp!!! */
    FILE *fp;           /* Rest of input; NULL if tokenizing a string 其余输入； 如果标记字符串则为NULL */
    int tabsize;        /* Tab spacing 制表符的宽度 */
    int indent;         /* Current indentation index 当前缩进的索引 */
    int indstack[MAXINDENT];            /* Stack of indents 缩进的栈 */
    int atbol;          /* Nonzero if at begin of new line 非零（如果在新行的开头） */
    int pendin;         /* Pending indents (if > 0) or dedents (if < 0) 待定缩进（如果> 0）或缩进（如果<0） */
    const char *prompt, *nextprompt;          /* For interactive prompting 交互提示 */
    int lineno;         /* Current line number 当前行号 */
    int first_lineno;   /* First line of a single line or multi line string
                           expression (cf. issue 16806) 单行或多行字符串表达式的第一行（请参阅问题16806） */
    int level;          /* () [] {} Parentheses nesting level 括号嵌套级别 */
            /* Used to allow free continuations inside them 用于允许它们内部自由延续 */
    char parenstack[MAXLEVEL];  /* 嵌套的栈 */
    int parenlinenostack[MAXLEVEL];
    PyObject *filename;
    /* Stuff for checking on different tab sizes 
    用于检查不同制表符大小的材料 */
    int altindstack[MAXINDENT];         /* Stack of alternate indents 备用缩进的栈 */
    /* Stuff for PEP 0263 */
    enum decoding_state decoding_state;
    int decoding_erred;         /* whether erred in decoding 是否在解码中出错 */
    int read_coding_spec;       /* whether 'coding:...' has been read  是否已读取“编码：...” */
    char *encoding;         /* Source encoding. 源编码 */
    int cont_line;          /* whether we are in a continuation line. 是否在延续行中 */
    const char* line_start;     /* pointer to start of current line 指向当前行开始的指 */
    const char* multi_line_start; /* pointer to start of first line of
                                     a single line or multi line string
                                     指向单行或多行字符串的第一行开始的指针
                                     expression (cf. issue 16806) */
    PyObject *decoding_readline; /* open(...).readline */
    PyObject *decoding_buffer;  /* 解码缓冲区 */
    const char* enc;        /* Encoding for the current str. 当前字符串的编码 */
    char* str;
    char* input;       /* Tokenizer's newline translated copy of the string. 
                        词法分析器的换行符转换后的字符串副本。 */

    int type_comments;      /* Whether to look for type comments 是否查找类型注释 */

    /* async/await related fields (still needed depending on feature_version) 
    async/await相关字段（取决于feature_version仍需要） */
    int async_hacks;     /* =1 if async/await aren't always keywords = 1，如果异步/等待并非总是关键字 */
    int async_def;        /* =1 if tokens are inside an 'async def' body. = 1，如果令牌位于“异步定义”正文中 */
    int async_def_indent; /* Indentation level of the outermost 'async def'. *最外面的“异步定义”的缩进级别 */
    int async_def_nl;     /* =1 if the outermost 'async def' had at least one
                             NEWLINE token after it. 
                             = 1，如果最外面的“异步定义”至少有一个
                              之后是NEWLINE令牌 */
};

extern struct tok_state *PyTokenizer_FromString(const char *, int);
extern struct tok_state *PyTokenizer_FromUTF8(const char *, int);
extern struct tok_state *PyTokenizer_FromFile(FILE *, const char*,
                                              const char *, const char *);
extern void PyTokenizer_Free(struct tok_state *);
extern int PyTokenizer_Get(struct tok_state *, const char **, const char **);
```

```cpp
/* Create and initialize a new tok_state structure 
创建并初始化新的tok_state结构 */
static struct tok_state *
tok_new(void)
{
    struct tok_state *tok = (struct tok_state *)PyMem_MALLOC(
                                            sizeof(struct tok_state));
    if (tok == NULL)
        return NULL;
    tok->buf = tok->cur = tok->inp = NULL;
    tok->start = NULL;
    tok->end = NULL;
    tok->done = E_OK;
    tok->fp = NULL;
    tok->input = NULL;
    tok->tabsize = TABSIZE;
    tok->indent = 0;
    tok->indstack[0] = 0;

    tok->atbol = 1;
    tok->pendin = 0;
    tok->prompt = tok->nextprompt = NULL;
    tok->lineno = 0;
    tok->level = 0;
    tok->altindstack[0] = 0;
    tok->decoding_state = STATE_INIT;
    tok->decoding_erred = 0;
    tok->read_coding_spec = 0;
    tok->enc = NULL;
    tok->encoding = NULL;
    tok->cont_line = 0;
    tok->filename = NULL;
    tok->decoding_readline = NULL;
    tok->decoding_buffer = NULL;
    tok->type_comments = 0;

    tok->async_hacks = 0;
    tok->async_def = 0;
    tok->async_def_indent = 0;
    tok->async_def_nl = 0;

    return tok;
}
```

`PyMem_MALLOC`是Python内存分配的接口，

```cpp
/* Read a line of text from TOK into S, using the stream in TOK.
   Return NULL on failure, else S.

   On entry, tok->decoding_buffer will be one of:
     1) NULL: need to call tok->decoding_readline to get a new line
     2) PyUnicodeObject *: decoding_feof has called tok->decoding_readline and
       stored the result in tok->decoding_buffer
     3) PyByteArrayObject *: previous call to fp_readl did not have enough room
       (in the s buffer) to copy entire contents of the line read
       by tok->decoding_readline.  tok->decoding_buffer has the overflow.
       In this case, fp_readl is called in a loop (with an expanded buffer)
       until the buffer ends with a '\n' (or until the end of the file is
       reached): see tok_nextc and its calls to decoding_fgets.
*/

/*
使用TOK中的流将一行文本从TOK读入S。
    失败时返回NULL，否则返回S。

    进入时，tok-> decoding_buffer将是以下之一：
      1）NULL：需要调用tok-> decoding_readline以获取新行
      2）PyUnicodeObject *：decode_feof调用了tok-> decoding_readline和
        将结果存储在tok-> decoding_buffer中
      3）PyByteArrayObject *：先前对fp_readl的调用没有足够的空间
        （在s缓冲区中）复制读取的行的全部内容
        通过tok-> decoding_readline。 tok-> decoding_buffer溢出。
        在这种情况下，fp_readl在循环中调用（带有扩展的缓冲区）
        直到缓冲区以'\ n'结尾（或直到文件结尾为
        已到达）：请参见tok_nextc及其对encoding_fgets的调用。
*/

static char *
fp_readl(char *s, int size, struct tok_state *tok)
{
    PyObject* bufobj;
    const char *buf;
    Py_ssize_t buflen;

    /* Ask for one less byte so we can terminate it */
    assert(size > 0);
    size--;

    if (tok->decoding_buffer) {
        bufobj = tok->decoding_buffer;
        Py_INCREF(bufobj);
    }
    else
    {
        bufobj = _PyObject_CallNoArg(tok->decoding_readline);
        if (bufobj == NULL)
            goto error;
    }
    if (PyUnicode_CheckExact(bufobj))
    {
        buf = PyUnicode_AsUTF8AndSize(bufobj, &buflen);
        if (buf == NULL) {
            goto error;
        }
    }
    else
    {
        buf = PyByteArray_AsString(bufobj);
        if (buf == NULL) {
            goto error;
        }
        buflen = PyByteArray_GET_SIZE(bufobj);
    }

    Py_XDECREF(tok->decoding_buffer);
    if (buflen > size) {
        /* Too many chars, the rest goes into tok->decoding_buffer */
        tok->decoding_buffer = PyByteArray_FromStringAndSize(buf+size,
                                                         buflen-size);
        if (tok->decoding_buffer == NULL)
            goto error;
        buflen = size;
    }
    else
        tok->decoding_buffer = NULL;

    memcpy(s, buf, buflen);
    s[buflen] = '\0';
    if (buflen == 0) /* EOF */
        s = NULL;
    Py_DECREF(bufobj);
    return s;

error:
    Py_XDECREF(bufobj);
    return error_ret(tok);
}

/* Set the readline function for TOK to a StreamReader's
   readline function. The StreamReader is named ENC.

   This function is called from check_bom and check_coding_spec.

   ENC is usually identical to the future value of tok->encoding,
   except for the (currently unsupported) case of UTF-16.

   Return 1 on success, 0 on failure. */

/*
将TOK的readline函数设置为StreamReader的
    readline功能。 StreamReader的名称为ENC。

    从check_bom和check_coding_spec调用此函数。

    ENC通常与tok-> encoding的未来值相同，
    UTF-16（目前不支持）情况除外。

    成功返回1，失败返回0。
*/

static int
fp_setreadl(struct tok_state *tok, const char* enc)
{
    PyObject *readline, *io, *stream;
    _Py_IDENTIFIER(open);
    _Py_IDENTIFIER(readline);
    int fd;
    long pos;

    fd = fileno(tok->fp);
    /* Due to buffering the file offset for fd can be different from the file
     * position of tok->fp.  If tok->fp was opened in text mode on Windows,
     * its file position counts CRLF as one char and can't be directly mapped
     * to the file offset for fd.  Instead we step back one byte and read to
     * the end of line.*/
    /*由于缓冲，fd的文件偏移量可能与文件不同
      * tok-> fp的位置。 如果在Windows上以文本模式打开了tok-> fp，
      *它的文件位置将CRLF计为一个字符，无法直接映射
      *到fd的文件偏移量。 相反，我们退后一个字节读取
      *行尾*/
    pos = ftell(tok->fp);
    if (pos == -1 ||
        lseek(fd, (off_t)(pos > 0 ? pos - 1 : pos), SEEK_SET) == (off_t)-1) {
        PyErr_SetFromErrnoWithFilename(PyExc_OSError, NULL);
        return 0;
    }

    io = PyImport_ImportModuleNoBlock("io");
    if (io == NULL)
        return 0;

    stream = _PyObject_CallMethodId(io, &PyId_open, "isisOOO",
                    fd, "r", -1, enc, Py_None, Py_None, Py_False);
    Py_DECREF(io);
    if (stream == NULL)
        return 0;

    readline = _PyObject_GetAttrId(stream, &PyId_readline);
    Py_DECREF(stream);
    if (readline == NULL)
        return 0;
    Py_XSETREF(tok->decoding_readline, readline);

    if (pos > 0) {
        PyObject *bufobj = _PyObject_CallNoArg(readline);
        if (bufobj == NULL)
            return 0;
        Py_DECREF(bufobj);
    }

    return 1;
}
```

### CPython 语法分析器


