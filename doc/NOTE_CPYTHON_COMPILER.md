
# Python 编译器 CPython 源码笔记

## CPython 主函数和命令行程序

包含了**CPython配置文件**和**CPython辅助函数**的大多数功能，定义了CPython的大、小和发布版本号，发布版本数字，发布字符串等。主要位于`Modules/main.c`中。

两种运行Python编译器的方式，wchar模式和bytes模式

```cpp
#ifdef MS_WINDOWS
int
wmain(int argc, wchar_t **argv)
{
    return Py_Main(argc, argv);
}
#else
int
main(int argc, char **argv)
{
    return Py_BytesMain(argc, argv);
}
#endif
```

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

```cpp
/* Set up tokenizer for string */
/*为字符串设置标记器*/
struct tok_state *
PyTokenizer_FromString(const char *str, int exec_input)
{
    struct tok_state *tok = tok_new();
    char *decoded;

    if (tok == NULL)
        return NULL;
    decoded = decode_str(str, exec_input, tok);
    if (decoded == NULL) {
        PyTokenizer_Free(tok);
        return NULL;
    }

    tok->buf = tok->cur = tok->inp = decoded;
    tok->end = decoded;
    return tok;
}

struct tok_state *
PyTokenizer_FromUTF8(const char *str, int exec_input)
{
    struct tok_state *tok = tok_new();
    char *translated;
    if (tok == NULL)
        return NULL;
    tok->input = translated = translate_newlines(str, exec_input, tok);
    if (translated == NULL) {
        PyTokenizer_Free(tok);
        return NULL;
    }
    tok->decoding_state = STATE_RAW;
    tok->read_coding_spec = 1;
    tok->enc = NULL;
    tok->str = translated;
    tok->encoding = (char *)PyMem_MALLOC(6);
    if (!tok->encoding) {
        PyTokenizer_Free(tok);
        return NULL;
    }
    strcpy(tok->encoding, "utf-8");

    tok->buf = tok->cur = tok->inp = translated;
    tok->end = translated;
    return tok;
}

/* Set up tokenizer for file */

struct tok_state *
PyTokenizer_FromFile(FILE *fp, const char* enc,
                     const char *ps1, const char *ps2)
{
    struct tok_state *tok = tok_new();
    if (tok == NULL)
        return NULL;
    if ((tok->buf = (char *)PyMem_MALLOC(BUFSIZ)) == NULL) {
        PyTokenizer_Free(tok);
        return NULL;
    }
    tok->cur = tok->inp = tok->buf;
    tok->end = tok->buf + BUFSIZ;
    tok->fp = fp;
    tok->prompt = ps1;
    tok->nextprompt = ps2;
    if (enc != NULL) {
        /* Must copy encoding declaration since it
           gets copied into the parse tree. */
        tok->encoding = PyMem_MALLOC(strlen(enc)+1);
        if (!tok->encoding) {
            PyTokenizer_Free(tok);
            return NULL;
        }
        strcpy(tok->encoding, enc);
        tok->decoding_state = STATE_NORMAL;
    }
    return tok;
}


/* Free a tok_state structure */

void
PyTokenizer_Free(struct tok_state *tok)
{
    if (tok->encoding != NULL)
        PyMem_FREE(tok->encoding);
    Py_XDECREF(tok->decoding_readline);
    Py_XDECREF(tok->decoding_buffer);
    Py_XDECREF(tok->filename);
    if (tok->fp != NULL && tok->buf != NULL)
        PyMem_FREE(tok->buf);
    if (tok->input)
        PyMem_FREE(tok->input);
    PyMem_FREE(tok);
}
```

```cpp
/* Get next char, updating state; error code goes into tok->done */
static int
tok_nextc(struct tok_state *tok)
{
    for (;;) {
        if (tok->cur != tok->inp) {
            return Py_CHARMASK(*tok->cur++); /* Fast path */
        }
        if (tok->done != E_OK)
            return EOF;
        if (tok->fp == NULL) {
            char *end = strchr(tok->inp, '\n');
            if (end != NULL)
                end++;
            else {
                end = strchr(tok->inp, '\0');
                if (end == tok->inp) {
                    tok->done = E_EOF;
                    return EOF;
                }
            }
            if (tok->start == NULL)
                tok->buf = tok->cur;
            tok->line_start = tok->cur;
            tok->lineno++;
            tok->inp = end;
            return Py_CHARMASK(*tok->cur++);
        }
        if (tok->prompt != NULL) {
            char *newtok = PyOS_Readline(stdin, stdout, tok->prompt);
            if (newtok != NULL) {
                char *translated = translate_newlines(newtok, 0, tok);
                PyMem_FREE(newtok);
                if (translated == NULL)
                    return EOF;
                newtok = translated;
            }
            if (tok->encoding && newtok && *newtok) {
                /* Recode to UTF-8 */
                Py_ssize_t buflen;
                const char* buf;
                PyObject *u = translate_into_utf8(newtok, tok->encoding);
                PyMem_FREE(newtok);
                if (!u) {
                    tok->done = E_DECODE;
                    return EOF;
                }
                buflen = PyBytes_GET_SIZE(u);
                buf = PyBytes_AS_STRING(u);
                newtok = PyMem_MALLOC(buflen+1);
                if (newtok == NULL) {
                    Py_DECREF(u);
                    tok->done = E_NOMEM;
                    return EOF;
                }
                strcpy(newtok, buf);
                Py_DECREF(u);
            }
            if (tok->nextprompt != NULL)
                tok->prompt = tok->nextprompt;
            if (newtok == NULL)
                tok->done = E_INTR;
            else if (*newtok == '\0') {
                PyMem_FREE(newtok);
                tok->done = E_EOF;
            }
            else if (tok->start != NULL) {
                size_t start = tok->start - tok->buf;
                size_t oldlen = tok->cur - tok->buf;
                size_t newlen = oldlen + strlen(newtok);
                Py_ssize_t cur_multi_line_start = tok->multi_line_start - tok->buf;
                char *buf = tok->buf;
                buf = (char *)PyMem_REALLOC(buf, newlen+1);
                tok->lineno++;
                if (buf == NULL) {
                    PyMem_FREE(tok->buf);
                    tok->buf = NULL;
                    PyMem_FREE(newtok);
                    tok->done = E_NOMEM;
                    return EOF;
                }
                tok->buf = buf;
                tok->cur = tok->buf + oldlen;
                tok->multi_line_start = tok->buf + cur_multi_line_start;
                tok->line_start = tok->cur;
                strcpy(tok->buf + oldlen, newtok);
                PyMem_FREE(newtok);
                tok->inp = tok->buf + newlen;
                tok->end = tok->inp + 1;
                tok->start = tok->buf + start;
            }
            else {
                tok->lineno++;
                if (tok->buf != NULL)
                    PyMem_FREE(tok->buf);
                tok->buf = newtok;
                tok->cur = tok->buf;
                tok->line_start = tok->buf;
                tok->inp = strchr(tok->buf, '\0');
                tok->end = tok->inp + 1;
            }
        }
        else {
            int done = 0;
            Py_ssize_t cur = 0;
            char *pt;
            if (tok->start == NULL) {
                if (tok->buf == NULL) {
                    tok->buf = (char *)
                        PyMem_MALLOC(BUFSIZ);
                    if (tok->buf == NULL) {
                        tok->done = E_NOMEM;
                        return EOF;
                    }
                    tok->end = tok->buf + BUFSIZ;
                }
                if (decoding_fgets(tok->buf, (int)(tok->end - tok->buf),
                          tok) == NULL) {
                    if (!tok->decoding_erred)
                        tok->done = E_EOF;
                    done = 1;
                }
                else {
                    tok->done = E_OK;
                    tok->inp = strchr(tok->buf, '\0');
                    done = tok->inp == tok->buf || tok->inp[-1] == '\n';
                }
            }
            else {
                cur = tok->cur - tok->buf;
                if (decoding_feof(tok)) {
                    tok->done = E_EOF;
                    done = 1;
                }
                else
                    tok->done = E_OK;
            }
            tok->lineno++;
            /* Read until '\n' or EOF */
            while (!done) {
                Py_ssize_t curstart = tok->start == NULL ? -1 :
                          tok->start - tok->buf;
                Py_ssize_t cur_multi_line_start = tok->multi_line_start - tok->buf;
                Py_ssize_t curvalid = tok->inp - tok->buf;
                Py_ssize_t newsize = curvalid + BUFSIZ;
                char *newbuf = tok->buf;
                newbuf = (char *)PyMem_REALLOC(newbuf,
                                               newsize);
                if (newbuf == NULL) {
                    tok->done = E_NOMEM;
                    tok->cur = tok->inp;
                    return EOF;
                }
                tok->buf = newbuf;
                tok->cur = tok->buf + cur;
                tok->multi_line_start = tok->buf + cur_multi_line_start;
                tok->line_start = tok->cur;
                tok->inp = tok->buf + curvalid;
                tok->end = tok->buf + newsize;
                tok->start = curstart < 0 ? NULL :
                         tok->buf + curstart;
                if (decoding_fgets(tok->inp,
                               (int)(tok->end - tok->inp),
                               tok) == NULL) {
                    /* Break out early on decoding
                       errors, as tok->buf will be NULL
                     */
                    if (tok->decoding_erred)
                        return EOF;
                    /* Last line does not end in \n,
                       fake one */
                    if (tok->inp[-1] != '\n')
                        strcpy(tok->inp, "\n");
                }
                tok->inp = strchr(tok->inp, '\0');
                done = tok->inp[-1] == '\n';
            }
            if (tok->buf != NULL) {
                tok->cur = tok->buf + cur;
                tok->line_start = tok->cur;
                /* replace "\r\n" with "\n" */
                /* For Mac leave the \r, giving a syntax error */
                pt = tok->inp - 2;
                if (pt >= tok->buf && *pt == '\r') {
                    *pt++ = '\n';
                    *pt = '\0';
                    tok->inp = pt;
                }
            }
        }
        if (tok->done != E_OK) {
            if (tok->prompt != NULL)
                PySys_WriteStderr("\n");
            tok->cur = tok->inp;
            return EOF;
        }
    }
    /*NOTREACHED*/
}


/* Back-up one character */

static void
tok_backup(struct tok_state *tok, int c)
{
    if (c != EOF) {
        if (--tok->cur < tok->buf) {
            Py_FatalError("tokenizer beginning of buffer");
        }
        if (*tok->cur != c) {
            *tok->cur = c;
        }
    }
}
```

```cpp
/* Get next token, after space stripping etc. 
在剥离空格等之后获取下一个词法记号。 */
static int
tok_get(struct tok_state *tok, const char **p_start, const char **p_end)
{
    int c;
    int blankline, nonascii;

    *p_start = *p_end = NULL;
  nextline:
    tok->start = NULL;
    blankline = 0;

    /* Get indentation level 
    获取缩进级别 */
    if (tok->atbol) {
        int col = 0;
        int altcol = 0;
        tok->atbol = 0;
        for (;;) {
            c = tok_nextc(tok);
            if (c == ' ') {
                col++, altcol++;
            }
            else if (c == '\t') {
                col = (col / tok->tabsize + 1) * tok->tabsize;
                altcol = (altcol / ALTTABSIZE + 1) * ALTTABSIZE;
            }
            else if (c == '\014')  {/* Control-L (formfeed) */
                col = altcol = 0; /* For Emacs users */
            }
            else {
                break;
            }
        }
        tok_backup(tok, c);
        if (c == '#' || c == '\n' || c == '\\') {
            /* Lines with only whitespace and/or comments
               and/or a line continuation character
               shouldn't affect the indentation and are
               not passed to the parser as NEWLINE tokens,
               except *totally* empty lines in interactive
               mode, which signal the end of a command group. */
            /*仅包含空格和/或注释的行
                和/或换行符
                不应该影响缩进，并且
                没有作为NEWLINE词法标记传递给解析器，
                交互式中“完全”空行除外
                模式，表示命令组已结束。 */
            if (col == 0 && c == '\n' && tok->prompt != NULL) {
                blankline = 0; /* Let it through 空行 */
            }
            else if (tok->prompt != NULL && tok->lineno == 1) {
                /* In interactive mode, if the first line contains
                   only spaces and/or a comment, let it through. */
                /*在交互模式下，如果第一行包含
                    仅空格和/或注释，让它通过。 */
                blankline = 0;
                col = altcol = 0;
            }
            else {
                blankline = 1; /* Ignore completely 完全忽略 */
            }
            /* We can't jump back right here since we still
               may need to skip to the end of a comment */
            /*因为我们仍然无法在此处跳回
                可能需要跳到评论结尾*/
        }
        if (!blankline && tok->level == 0) {
            if (col == tok->indstack[tok->indent]) {
                /* No change 没有改变 */
                if (altcol != tok->altindstack[tok->indent]) {
                    return indenterror(tok);
                }
            }
            else if (col > tok->indstack[tok->indent]) {
                /* Indent -- always one 缩进-总是一个 */
                if (tok->indent+1 >= MAXINDENT) {
                    tok->done = E_TOODEEP;
                    tok->cur = tok->inp;
                    return ERRORTOKEN;
                }
                if (altcol <= tok->altindstack[tok->indent]) {
                    return indenterror(tok);
                }
                tok->pendin++;
                tok->indstack[++tok->indent] = col;
                tok->altindstack[tok->indent] = altcol;
            }
            else /* col < tok->indstack[tok->indent] */ {
                /* Dedent -- any number, must be consistent 任何数字，必须一致 */
                while (tok->indent > 0 &&
                    col < tok->indstack[tok->indent]) {
                    tok->pendin--;
                    tok->indent--;
                }
                if (col != tok->indstack[tok->indent]) {
                    tok->done = E_DEDENT;
                    tok->cur = tok->inp;
                    return ERRORTOKEN;
                }
                if (altcol != tok->altindstack[tok->indent]) {
                    return indenterror(tok);
                }
            }
        }
    }

    tok->start = tok->cur;

    /* Return pending indents/dedents 返回待定缩进/缩进 */
    if (tok->pendin != 0) {
        if (tok->pendin < 0) {
            tok->pendin++;
            return DEDENT;
        }
        else {
            tok->pendin--;
            return INDENT;
        }
    }

    /* Peek ahead at the next character 提前看下一个字符 */
    c = tok_nextc(tok);
    tok_backup(tok, c);
    /* Check if we are closing an async function 
    检查我们是否正在关闭异步功能 */
    if (tok->async_def
        && !blankline
        /* Due to some implementation artifacts of type comments,
         * a TYPE_COMMENT at the start of a function won't set an
         * indentation level and it will produce a NEWLINE after it.
         * To avoid spuriously ending an async function due to this,
         * wait until we have some non-newline char in front of us. */
        /*由于类型注释的一些实现工件，
          *函数开头的TYPE_COMMENT不会设置
          *缩进级别，它将在其后产生一个NEWLINE。
          *为避免因此错误终止异步功能，
          *等到我们面前出现一些非换行符。 */
        && c != '\n'
        && tok->level == 0
        /* There was a NEWLINE after ASYNC DEF,
           so we're past the signature. */
        /* ASYNC DEF之后有一个NEWLINE，
            所以我们过去了签名。 */
        && tok->async_def_nl
        /* Current indentation level is less than where
           the async function was defined */
        /*当前缩进级别小于
            异步功能已定义*/
        && tok->async_def_indent >= tok->indent)
    {
        tok->async_def = 0;
        tok->async_def_indent = 0;
        tok->async_def_nl = 0;
    }

 again:
    tok->start = NULL;
    /* Skip spaces 跳过空白 */
    do {
        c = tok_nextc(tok);
    } while (c == ' ' || c == '\t' || c == '\014');

    /* Set start of current token 设置当前token的开始 */
    tok->start = tok->cur - 1;

    /* Skip comment, unless it's a type comment 忽略注释，除非它是一个类型注释 */
    if (c == '#') {
        const char *prefix, *p, *type_start;

        while (c != EOF && c != '\n') {
            c = tok_nextc(tok);
        }

        if (tok->type_comments) {
            p = tok->start;
            prefix = type_comment_prefix;
            while (*prefix && p < tok->cur) {
                if (*prefix == ' ') {
                    while (*p == ' ' || *p == '\t') {
                        p++;
                    }
                } else if (*prefix == *p) {
                    p++;
                } else {
                    break;
                }

                prefix++;
            }

            /* This is a type comment if we matched all of type_comment_prefix. 
            如果我们匹配所有type_comment_prefix，则这是一个类型注释。 */
            if (!*prefix) {
                int is_type_ignore = 1;
                const char *ignore_end = p + 6;
                tok_backup(tok, c);  /* don't eat the newline or EOF 
                不要吃换行符或EOF */

                type_start = p;

                /* A TYPE_IGNORE is "type: ignore" followed by the end of the token
                 * or anything ASCII and non-alphanumeric. 
                 * TYPE_IGNORE是“类型：忽略”，后跟词法标记的结尾
                * 或任何ASCII和非字母数字的内容。 */
                is_type_ignore = (
                    tok->cur >= ignore_end && memcmp(p, "ignore", 6) == 0
                    && !(tok->cur > ignore_end
                         && ((unsigned char)ignore_end[0] >= 128 || Py_ISALNUM(ignore_end[0]))));

                if (is_type_ignore) {
                    *p_start = ignore_end;
                    *p_end = tok->cur;

                    /* If this type ignore is the only thing on the line, 
                    consume the newline also. 
                    如果此类型ignore是行上唯一的内容，则也使用换行符。 */
                    if (blankline) {
                        tok_nextc(tok);
                        tok->atbol = 1;
                    }
                    return TYPE_IGNORE;
                } else {
                    *p_start = type_start;  /* after type_comment_prefix 
                    在type_comment_prefix之后 */
                    *p_end = tok->cur;
                    return TYPE_COMMENT;
                }
            }
        }
    }

    /* Check for EOF and errors now 检查EOF字符 */
    if (c == EOF) {
        return tok->done == E_EOF ? ENDMARKER : ERRORTOKEN;
    }

    /* Identifier (most frequent token!) 
    标识符（最常使用的词法标记！）*/
    nonascii = 0;
    if (is_potential_identifier_start(c)) {
        /* Process the various legal combinations of b"", r"", u"", and f"". 
        处理b“”，r“”，u“”和f“”的各种字符串合法组合。 */
        int saw_b = 0, saw_r = 0, saw_u = 0, saw_f = 0;
        while (1) {
            if (!(saw_b || saw_u || saw_f) && (c == 'b' || c == 'B'))
                saw_b = 1;
            /* Since this is a backwards compatibility support literal we don't
               want to support it in arbitrary order like byte literals. */
            /*由于这是一个向后兼容支持文字，因此我们不希望像字节文字那样以任意顺序支持它。 */
            else if (!(saw_b || saw_u || saw_r || saw_f)
                     && (c == 'u'|| c == 'U')) {
                saw_u = 1;
            }
            /* ur"" and ru"" are not supported ur 和 ru不支持 */
            else if (!(saw_r || saw_u) && (c == 'r' || c == 'R')) {
                saw_r = 1;
            }
            else if (!(saw_f || saw_b || saw_u) && (c == 'f' || c == 'F')) {
                saw_f = 1;
            }
            else {
                break;
            }
            c = tok_nextc(tok);
            if (c == '"' || c == '\'') {
                goto letter_quote;
            }
        }
        while (is_potential_identifier_char(c)) {
            if (c >= 128) {
                nonascii = 1;
            }
            c = tok_nextc(tok);
        }
        tok_backup(tok, c);
        if (nonascii && !verify_identifier(tok)) {
            return ERRORTOKEN;
        }

        *p_start = tok->start;
        *p_end = tok->cur;

        /* async/await parsing block. */
        if (tok->cur - tok->start == 5 && tok->start[0] == 'a') {
            /* May be an 'async' or 'await' token.  For Python 3.7 or
               later we recognize them unconditionally.  For Python
               3.5 or 3.6 we recognize 'async' in front of 'def', and
               either one inside of 'async def'.  (Technically we
               shouldn't recognize these at all for 3.4 or earlier,
               but there's no *valid* Python 3.4 code that would be
               rejected, and async functions will be rejected in a
               later phase.) */
        /* 可能是“异步”或“等待”词法标记。 对于Python 3.7或
                后来我们无条件地认出了他们。 对于Python
                3.5或3.6我们在'def'前面识别了'async'，并且
                “异步定义”中的任意一个。 （从技术上讲，
                在3.4或更早的版本中根本不应该识别这些，
                但是没有*有效*的Python 3.4代码
                被拒绝，异步功能将在
                后期。）*/
            if (!tok->async_hacks || tok->async_def) {
                /* Always recognize the keywords. 始终识别关键字。 */
                if (memcmp(tok->start, "async", 5) == 0) {
                    return ASYNC;
                }
                if (memcmp(tok->start, "await", 5) == 0) {
                    return AWAIT;
                }
            }
            else if (memcmp(tok->start, "async", 5) == 0) {
                /* The current token is 'async'.
                   Look ahead one token to see if that is 'def'. 
                   当前词法标记为“异步”。 提前查看一个词法标记，看看是否为“def”。 */
                struct tok_state ahead_tok;
                const char *ahead_tok_start = NULL;
                const char *ahead_tok_end = NULL;
                int ahead_tok_kind;

                memcpy(&ahead_tok, tok, sizeof(ahead_tok));
                ahead_tok_kind = tok_get(&ahead_tok, &ahead_tok_start,
                                         &ahead_tok_end);

                if (ahead_tok_kind == NAME
                    && ahead_tok.cur - ahead_tok.start == 3
                    && memcmp(ahead_tok.start, "def", 3) == 0)
                {
                    /* The next token is going to be 'def', so instead of
                       returning a plain NAME token, return ASYNC. 
                    下一个标记将是’def‘，因此，不返回简单的NAME标记，而是返回ASYNC。 */
                    tok->async_def_indent = tok->indent;
                    tok->async_def = 1;
                    return ASYNC;
                }
            }
        }

        return NAME;
    }

    /* Newline 换行符 */
    if (c == '\n') {
        tok->atbol = 1;
        if (blankline || tok->level > 0) {
            goto nextline;
        }
        *p_start = tok->start;
        *p_end = tok->cur - 1; /* Leave '\n' out of the string 将'\n'排除在字符串外 */
        tok->cont_line = 0;
        if (tok->async_def) {
            /* We're somewhere inside an 'async def' function, and
               we've encountered a NEWLINE after its signature. 
               我们在“异步定义”函数内部，在签名后遇到了NEWLINE。 */
            tok->async_def_nl = 1;
        }
        return NEWLINE;
    }

    /* Period or number starting with period? 句号还是数字以句号开头？ */
    if (c == '.') {
        c = tok_nextc(tok);
        if (isdigit(c)) {
            goto fraction;
        } else if (c == '.') {
            c = tok_nextc(tok);
            if (c == '.') {
                *p_start = tok->start;
                *p_end = tok->cur;
                return ELLIPSIS;
            }
            else {
                tok_backup(tok, c);
            }
            tok_backup(tok, '.');
        }
        else {
            tok_backup(tok, c);
        }
        *p_start = tok->start;
        *p_end = tok->cur;
        return DOT;
    }

    /* Number 数字 */
    if (isdigit(c)) {
        if (c == '0') {
            /* Hex, octal or binary -- maybe. 如果是以0开头，那么有可能是一个十六进制，八进制和二进制数字 */
            c = tok_nextc(tok);
            if (c == 'x' || c == 'X') {
                /* Hex */
                c = tok_nextc(tok);
                do {
                    if (c == '_') {  /* 忽略数字中的下划线 */
                        c = tok_nextc(tok);
                    }
                    if (!isxdigit(c)) {
                        tok_backup(tok, c);
                        return syntaxerror(tok, "invalid hexadecimal literal");
                    }
                    do {
                        c = tok_nextc(tok);
                    } while (isxdigit(c));
                } while (c == '_');
            }
            else if (c == 'o' || c == 'O') {
                /* Octal 八进制 */
                c = tok_nextc(tok);
                do {
                    if (c == '_') {
                        c = tok_nextc(tok);
                    }
                    if (c < '0' || c >= '8') {
                        tok_backup(tok, c);
                        if (isdigit(c)) {
                            return syntaxerror(tok,
                                    "invalid digit '%c' in octal literal", c);
                        }
                        else {
                            return syntaxerror(tok, "invalid octal literal");
                        }
                    }
                    do {
                        c = tok_nextc(tok);
                    } while ('0' <= c && c < '8');
                } while (c == '_');
                if (isdigit(c)) {
                    return syntaxerror(tok,
                            "invalid digit '%c' in octal literal", c);
                }
            }
            else if (c == 'b' || c == 'B') {
                /* Binary 二进制数字 */
                c = tok_nextc(tok);
                do {
                    if (c == '_') {
                        c = tok_nextc(tok);
                    }
                    if (c != '0' && c != '1') {
                        tok_backup(tok, c);
                        if (isdigit(c)) {
                            return syntaxerror(tok,
                                    "invalid digit '%c' in binary literal", c);
                        }
                        else {
                            return syntaxerror(tok, "invalid binary literal");
                        }
                    }
                    do {
                        c = tok_nextc(tok);
                    } while (c == '0' || c == '1');
                } while (c == '_');
                if (isdigit(c)) {
                    return syntaxerror(tok,
                            "invalid digit '%c' in binary literal", c);
                }
            }
            else {
                int nonzero = 0;
                /* maybe old-style octal; c is first char of it 也许是老式的八进制； c是它的第一个字符 */
                /* in any case, allow '0' as a literal 在任何情况下，均允许使用“ 0”作为字面量 */
                while (1) {
                    if (c == '_') {
                        c = tok_nextc(tok);
                        if (!isdigit(c)) {
                            tok_backup(tok, c);
                            return syntaxerror(tok, "invalid decimal literal");
                        }
                    }
                    if (c != '0') {
                        break;
                    }
                    c = tok_nextc(tok);
                }
                if (isdigit(c)) {
                    nonzero = 1;
                    c = tok_decimal_tail(tok);
                    if (c == 0) {
                        return ERRORTOKEN;
                    }
                }
                if (c == '.') {
                    c = tok_nextc(tok);
                    goto fraction;  /* 分数 */
                }
                else if (c == 'e' || c == 'E') {
                    goto exponent;  /* 指数 */
                }
                else if (c == 'j' || c == 'J') {
                    goto imaginary; /* 复数 */
                }
                else if (nonzero) {
                    /* Old-style octal: now disallowed. 旧式八进制：现在禁止使用。 */
                    tok_backup(tok, c);
                    return syntaxerror(tok,
                                       "leading zeros in decimal integer "
                                       "literals are not permitted; "
                                       "use an 0o prefix for octal integers");
                }
            }
        }
        else {
            /* Decimal 小数 */
            c = tok_decimal_tail(tok);
            if (c == 0) {
                return ERRORTOKEN;
            }
            {
                /* Accept floating point numbers. 接收作为浮点数的小数点 */
                if (c == '.') {
                    c = tok_nextc(tok);
        fraction:
                    /* Fraction 分数 */
                    if (isdigit(c)) {
                        c = tok_decimal_tail(tok);
                        if (c == 0) {
                            return ERRORTOKEN;
                        }
                    }
                }
                if (c == 'e' || c == 'E') {
                    int e;
                  exponent:
                    e = c;
                    /* Exponent part 指数部分 */
                    c = tok_nextc(tok);
                    if (c == '+' || c == '-') {
                        c = tok_nextc(tok);
                        if (!isdigit(c)) {
                            tok_backup(tok, c);
                            return syntaxerror(tok, "invalid decimal literal");
                        }
                    } else if (!isdigit(c)) {
                        tok_backup(tok, c);
                        tok_backup(tok, e);
                        *p_start = tok->start;
                        *p_end = tok->cur;
                        return NUMBER;
                    }
                    c = tok_decimal_tail(tok);
                    if (c == 0) {
                        return ERRORTOKEN;
                    }
                }
                if (c == 'j' || c == 'J') {
                    /* Imaginary part 复数的虚数部分 */
        imaginary:
                    c = tok_nextc(tok);
                }
            }
        }
        tok_backup(tok, c);
        *p_start = tok->start;
        *p_end = tok->cur;
        return NUMBER;
    }
    /* 字符串的引号 */
  letter_quote:
    /* String 字符串  */
    if (c == '\'' || c == '"') {
        int quote = c;
        int quote_size = 1;             /* 1 or 3 3个引号表示Python的多行字符串*/
        int end_quote_size = 0;

        /* Nodes of type STRING, especially multi line strings
           must be handled differently in order to get both
           the starting line number and the column offset right.
           (cf. issue 16806) 类型为STRING的节点，尤其是多行字符串
            必须以不同的方式处理，以使两者
            起始行号和右列偏移量。
            （请参阅问题16806） */
        tok->first_lineno = tok->lineno;
        tok->multi_line_start = tok->line_start;

        /* Find the quote size and start of string 找出字符串的组成是几个引号包围的 */
        c = tok_nextc(tok);
        if (c == quote) {
            c = tok_nextc(tok);
            if (c == quote) {
                quote_size = 3;
            }
            else {
                end_quote_size = 1;     /* empty string found 空字符串 */
            }
        }
        if (c != quote) {
            tok_backup(tok, c);
        }

        /* Get rest of string  获取字符串的其他的部分 */
        while (end_quote_size != quote_size) {
            c = tok_nextc(tok);
            if (c == EOF) {
                if (quote_size == 3) {
                    tok->done = E_EOFS;
                }
                else {
                    tok->done = E_EOLS;
                }
                tok->cur = tok->inp;
                return ERRORTOKEN;
            }
            if (quote_size == 1 && c == '\n') {
                tok->done = E_EOLS;
                tok->cur = tok->inp;
                return ERRORTOKEN;
            }
            if (c == quote) {
                end_quote_size += 1;
            }
            else {
                end_quote_size = 0;
                if (c == '\\') {
                    tok_nextc(tok);  /* skip escaped char 跳过构成转义字符的\字符 */
                }
            }
        }

        *p_start = tok->start;
        *p_end = tok->cur;
        return STRING;
    }

    /* Line continuation 续行 */
    if (c == '\\') {
        c = tok_nextc(tok);
        if (c != '\n') {
            tok->done = E_LINECONT;
            tok->cur = tok->inp;
            return ERRORTOKEN;
        }
        c = tok_nextc(tok);
        if (c == EOF) {
            tok->done = E_EOF;
            tok->cur = tok->inp;
            return ERRORTOKEN;
        } else {
            tok_backup(tok, c);
        }
        tok->cont_line = 1;
        goto again; /* Read next line 读取下一行 */
    }

    /* Check for two-character token 检查两个字符的记号 */
    {
        int c2 = tok_nextc(tok);
        int token = PyToken_TwoChars(c, c2);
        if (token != OP) {
            int c3 = tok_nextc(tok);
            int token3 = PyToken_ThreeChars(c, c2, c3);
            if (token3 != OP) {
                token = token3;
            }
            else {
                tok_backup(tok, c3);
            }
            *p_start = tok->start;
            *p_end = tok->cur;
            return token;
        }
        tok_backup(tok, c2);
    }

    /* Keep track of parentheses nesting level 
    跟踪括号嵌套级别 */
    switch (c) {
    case '(':
    case '[':
    case '{':
        if (tok->level >= MAXLEVEL) {
            return syntaxerror(tok, "too many nested parentheses");
        }
        tok->parenstack[tok->level] = c;
        tok->parenlinenostack[tok->level] = tok->lineno;
        tok->level++;
        break;
    case ')':
    case ']':
    case '}':
        if (!tok->level) {
            return syntaxerror(tok, "unmatched '%c'", c);
        }
        tok->level--;
        int opening = tok->parenstack[tok->level];
        if (!((opening == '(' && c == ')') ||
              (opening == '[' && c == ']') ||
              (opening == '{' && c == '}')))
        {
            if (tok->parenlinenostack[tok->level] != tok->lineno) {
                return syntaxerror(tok,
                        "closing parenthesis '%c' does not match "
                        "opening parenthesis '%c' on line %d",
                        c, opening, tok->parenlinenostack[tok->level]);
            }
            else {
                return syntaxerror(tok,
                        "closing parenthesis '%c' does not match "
                        "opening parenthesis '%c'",
                        c, opening);
            }
        }
        break;
    }

    /* Punctuation character 标点符号 */
    *p_start = tok->start;
    *p_end = tok->cur;
    return PyToken_OneChar(c);
}

int
PyTokenizer_Get(struct tok_state *tok, const char **p_start, const char **p_end)
{
    int result = tok_get(tok, p_start, p_end);
    if (tok->decoding_erred) {
        result = ERRORTOKEN;
        tok->done = E_DECODE;
    }
    return result;
}

/* Get the encoding of a Python file. Check for the coding cookie and check if
   the file starts with a BOM.

   PyTokenizer_FindEncodingFilename() returns NULL when it can't find the
   encoding in the first or second line of the file (in which case the encoding
   should be assumed to be UTF-8).

   The char* returned is malloc'ed via PyMem_MALLOC() and thus must be freed
   by the caller. */
/* 获取Python文件的编码。 检查编码cookie，并检查文件是否以BOM表开头。 
当PyTokenizer_FindEncodingFilename（）在文件的第一行或第二行中找不到编码时，
则返回NULL（在这种情况下，应假定编码为UTF-8）。 
返回的char *是通过PyMem_MALLOC（）分配的，因此必须由调用方释放。 */
char *
PyTokenizer_FindEncodingFilename(int fd, PyObject *filename)
{
    struct tok_state *tok;
    FILE *fp;
    const char *p_start = NULL;
    const char *p_end = NULL;
    char *encoding = NULL;

    fd = _Py_dup(fd);
    if (fd < 0) {
        return NULL;
    }

    fp = fdopen(fd, "r");
    if (fp == NULL) {
        return NULL;
    }
    tok = PyTokenizer_FromFile(fp, NULL, NULL, NULL);
    if (tok == NULL) {
        fclose(fp);
        return NULL;
    }
    if (filename != NULL) {
        Py_INCREF(filename);
        tok->filename = filename;
    }
    else {
        tok->filename = PyUnicode_FromString("<string>");
        if (tok->filename == NULL) {
            fclose(fp);
            PyTokenizer_Free(tok);
            return encoding;
        }
    }
    while (tok->lineno < 2 && tok->done == E_OK) {
        PyTokenizer_Get(tok, &p_start, &p_end);
    }
    fclose(fp);
    if (tok->encoding) {
        encoding = (char *)PyMem_MALLOC(strlen(tok->encoding) + 1);
        if (encoding)
            strcpy(encoding, tok->encoding);
    }
    PyTokenizer_Free(tok);
    return encoding;
}

char *
PyTokenizer_FindEncoding(int fd)
{
    return PyTokenizer_FindEncodingFilename(fd, NULL);
}

#ifdef Py_DEBUG

void
tok_dump(int type, char *start, char *end)
{
    printf("%s", _PyParser_TokenNames[type]);
    if (type == NAME || type == NUMBER || type == STRING || type == OP)
        printf("(%.*s)", (int)(end - start), start);
}

#endif
```

### CPython 语法分析器

Python的语法分析器是pegen生成的

```cpp
// @generated by pegen.py from ./Grammar/python.gram
#include "pegen.h"
```

```cpp
typedef struct _memo {
    int type;
    void *node;
    int mark;
    struct _memo *next;
} Memo;

typedef struct {
    int type;
    PyObject *bytes;
    int lineno, col_offset, end_lineno, end_col_offset;
    Memo *memo;
} Token;

typedef struct {
    char *str;
    int type;
} KeywordToken;


typedef struct {
    struct {
        int lineno;
        char *comment;  // The " <tag>" in "# type: ignore <tag>"
    } *items;
    size_t size;
    size_t num_items;
} growable_comment_array;

typedef struct {
    struct tok_state *tok;
    Token **tokens;
    int mark;
    int fill, size;
    PyArena *arena;
    KeywordToken **keywords;
    int n_keyword_lists;
    int start_rule;
    int *errcode;
    int parsing_started;
    PyObject* normalize;
    int starting_lineno;
    int starting_col_offset;
    int error_indicator;
    int flags;
    int feature_version;
    growable_comment_array type_ignore_comments;
    Token *known_err_token;
    int level;
} Parser;

typedef struct {
    cmpop_ty cmpop;
    expr_ty expr;
} CmpopExprPair;

typedef struct {
    expr_ty key;
    expr_ty value;
} KeyValuePair;

typedef struct {
    arg_ty arg;
    expr_ty value;
} NameDefaultPair;

typedef struct {
    asdl_seq *plain_names;
    asdl_seq *names_with_defaults; // asdl_seq* of NameDefaultsPair's
} SlashWithDefault;

typedef struct {
    arg_ty vararg;
    asdl_seq *kwonlyargs; // asdl_seq* of NameDefaultsPair's
    arg_ty kwarg;
} StarEtc;

typedef struct {
    operator_ty kind;
} AugOperator;

typedef struct {
    void *element;
    int is_keyword;
} KeywordOrStarred;
```

Python解析函数

```cpp
void *
_PyPegen_parse(Parser *p)
{
    // Initialize keywords
    p->keywords = reserved_keywords;
    p->n_keyword_lists = n_keyword_lists;

    // Run parser
    void *result = NULL;
    if (p->start_rule == Py_file_input) {
        result = file_rule(p);
    } else if (p->start_rule == Py_single_input) {
        result = interactive_rule(p);
    } else if (p->start_rule == Py_eval_input) {
        result = eval_rule(p);
    } else if (p->start_rule == Py_func_type_input) {
        result = func_type_rule(p);
    } else if (p->start_rule == Py_fstring_input) {
        result = fstring_rule(p);
    }

    return result;
}
```

Parser定义关键字序号

```cpp
static const int n_keyword_lists = 9;
static KeywordToken *reserved_keywords[] = {
    /* 0个字母构成的关键字集合 */
    NULL,
    /* 1个字母构成的关键字集合 */
    NULL,
    /* 2个字母构成的关键字集合 */
    (KeywordToken[]) {
        {"if", 510},
        {"in", 518},
        {"as", 520},
        {"is", 527},
        {"or", 531},
        {NULL, -1},
    },
    /* 3个字母构成的关键字集合 */
    (KeywordToken[]) {
        {"del", 503},
        {"try", 511},
        {"for", 517},
        {"def", 523},
        {"not", 526},
        {"and", 532},
        {NULL, -1},
    },
    /* 4个字母构成的关键字集合 */
    (KeywordToken[]) {
        {"pass", 502},
        {"from", 514},
        {"elif", 515},
        {"else", 516},
        {"with", 519},
        {"True", 528},
        {"None", 530},
        {NULL, -1},
    },
    /* 5个字母构成的关键字集合 */
    (KeywordToken[]) {
        {"raise", 501},
        {"yield", 504},
        {"break", 506},
        {"while", 512},
        {"class", 524},
        {"False", 529},
        {NULL, -1},
    },
    /* 6个字母构成的关键字集合 */
    (KeywordToken[]) {
        {"return", 500},
        {"assert", 505},
        {"global", 508},
        {"import", 513},
        {"except", 521},
        {"lambda", 525},
        {NULL, -1},
    },
    /* 7个字母构成的关键字集合 */
    (KeywordToken[]) {
        {"finally", 522},
        {NULL, -1},
    },
    /* 8个字母构成的关键字集合 */
    (KeywordToken[]) {
        {"continue", 507},
        {"nonlocal", 509},
        {NULL, -1},
    },
};
```

Parser文法序号宏定义

```cpp
#define file_type 1000
#define interactive_type 1001
#define eval_type 1002
#define func_type_type 1003
#define fstring_type 1004
#define type_expressions_type 1005
#define statements_type 1006
#define statement_type 1007
#define statement_newline_type 1008
#define simple_stmt_type 1009
#define small_stmt_type 1010
#define compound_stmt_type 1011
#define assignment_type 1012
#define augassign_type 1013
#define global_stmt_type 1014
#define nonlocal_stmt_type 1015
#define yield_stmt_type 1016
#define assert_stmt_type 1017
#define del_stmt_type 1018
#define import_stmt_type 1019
#define import_name_type 1020
#define import_from_type 1021
#define import_from_targets_type 1022
#define import_from_as_names_type 1023
#define import_from_as_name_type 1024
#define dotted_as_names_type 1025
#define dotted_as_name_type 1026
#define dotted_name_type 1027  // Left-recursive  左递归
#define if_stmt_type 1028
#define elif_stmt_type 1029
#define else_block_type 1030
#define while_stmt_type 1031
#define for_stmt_type 1032
#define with_stmt_type 1033
#define with_item_type 1034
#define try_stmt_type 1035
#define except_block_type 1036
#define finally_block_type 1037
#define return_stmt_type 1038
#define raise_stmt_type 1039
#define function_def_type 1040
#define function_def_raw_type 1041
#define func_type_comment_type 1042
#define params_type 1043
#define parameters_type 1044
#define slash_no_default_type 1045
#define slash_with_default_type 1046
#define star_etc_type 1047
#define kwds_type 1048
#define param_no_default_type 1049
#define param_with_default_type 1050
#define param_maybe_default_type 1051
#define param_type 1052
#define annotation_type 1053
#define default_type 1054
#define decorators_type 1055
#define class_def_type 1056
#define class_def_raw_type 1057
#define block_type 1058
#define expressions_list_type 1059
#define star_expressions_type 1060
#define star_expression_type 1061
#define star_named_expressions_type 1062
#define star_named_expression_type 1063
#define named_expression_type 1064
#define annotated_rhs_type 1065
#define expressions_type 1066
#define expression_type 1067
#define lambdef_type 1068
#define lambda_params_type 1069
#define lambda_parameters_type 1070
#define lambda_slash_no_default_type 1071
#define lambda_slash_with_default_type 1072
#define lambda_star_etc_type 1073
#define lambda_kwds_type 1074
#define lambda_param_no_default_type 1075
#define lambda_param_with_default_type 1076
#define lambda_param_maybe_default_type 1077
#define lambda_param_type 1078
#define disjunction_type 1079
#define conjunction_type 1080
#define inversion_type 1081
#define comparison_type 1082
#define compare_op_bitwise_or_pair_type 1083
#define eq_bitwise_or_type 1084
#define noteq_bitwise_or_type 1085
#define lte_bitwise_or_type 1086
#define lt_bitwise_or_type 1087
#define gte_bitwise_or_type 1088
#define gt_bitwise_or_type 1089
#define notin_bitwise_or_type 1090
#define in_bitwise_or_type 1091
#define isnot_bitwise_or_type 1092
#define is_bitwise_or_type 1093
#define bitwise_or_type 1094  // Left-recursive
#define bitwise_xor_type 1095  // Left-recursive
#define bitwise_and_type 1096  // Left-recursive
#define shift_expr_type 1097  // Left-recursive
#define sum_type 1098  // Left-recursive
#define term_type 1099  // Left-recursive
#define factor_type 1100
#define power_type 1101
#define await_primary_type 1102
#define primary_type 1103  // Left-recursive
#define slices_type 1104
#define slice_type 1105
#define atom_type 1106
#define strings_type 1107
#define list_type 1108
#define listcomp_type 1109
#define tuple_type 1110
#define group_type 1111
#define genexp_type 1112
#define set_type 1113
#define setcomp_type 1114
#define dict_type 1115
#define dictcomp_type 1116
#define double_starred_kvpairs_type 1117
#define double_starred_kvpair_type 1118
#define kvpair_type 1119
#define for_if_clauses_type 1120
#define for_if_clause_type 1121
#define yield_expr_type 1122
#define arguments_type 1123
#define args_type 1124
#define kwargs_type 1125
#define starred_expression_type 1126
#define kwarg_or_starred_type 1127
#define kwarg_or_double_starred_type 1128
#define star_targets_type 1129
#define star_targets_seq_type 1130
#define star_target_type 1131
#define star_atom_type 1132
#define single_target_type 1133
#define single_subscript_attribute_target_type 1134
#define del_targets_type 1135
#define del_target_type 1136
#define del_t_atom_type 1137
#define targets_type 1138
#define target_type 1139
#define t_primary_type 1140  // Left-recursive
#define t_lookahead_type 1141
#define t_atom_type 1142
#define incorrect_arguments_type 1143
#define invalid_kwarg_type 1144
#define invalid_named_expression_type 1145
#define invalid_assignment_type 1146
#define invalid_del_stmt_type 1147
#define invalid_block_type 1148
#define invalid_comprehension_type 1149
#define invalid_dict_comprehension_type 1150
#define invalid_parameters_type 1151
#define invalid_lambda_parameters_type 1152
#define invalid_star_etc_type 1153
#define invalid_lambda_star_etc_type 1154
#define invalid_double_type_comments_type 1155
#define invalid_with_item_type 1156
#define invalid_for_target_type 1157
#define invalid_group_type 1158
#define invalid_import_from_targets_type 1159
#define _loop0_1_type 1160
#define _loop0_2_type 1161
#define _loop0_4_type 1162
#define _gather_3_type 1163
#define _loop0_6_type 1164
#define _gather_5_type 1165
#define _loop0_8_type 1166
#define _gather_7_type 1167
#define _loop0_10_type 1168
#define _gather_9_type 1169
#define _loop1_11_type 1170
#define _loop0_13_type 1171
#define _gather_12_type 1172
#define _tmp_14_type 1173
#define _tmp_15_type 1174
#define _tmp_16_type 1175
#define _tmp_17_type 1176
#define _tmp_18_type 1177
#define _tmp_19_type 1178
#define _tmp_20_type 1179
#define _tmp_21_type 1180
#define _loop1_22_type 1181
#define _tmp_23_type 1182
#define _tmp_24_type 1183
#define _loop0_26_type 1184
#define _gather_25_type 1185
#define _loop0_28_type 1186
#define _gather_27_type 1187
#define _tmp_29_type 1188
#define _tmp_30_type 1189
#define _loop0_31_type 1190
#define _loop1_32_type 1191
#define _loop0_34_type 1192
#define _gather_33_type 1193
#define _tmp_35_type 1194
#define _loop0_37_type 1195
#define _gather_36_type 1196
#define _tmp_38_type 1197
#define _loop0_40_type 1198
#define _gather_39_type 1199
#define _loop0_42_type 1200
#define _gather_41_type 1201
#define _loop0_44_type 1202
#define _gather_43_type 1203
#define _loop0_46_type 1204
#define _gather_45_type 1205
#define _tmp_47_type 1206
#define _loop1_48_type 1207
#define _tmp_49_type 1208
#define _tmp_50_type 1209
#define _tmp_51_type 1210
#define _tmp_52_type 1211
#define _tmp_53_type 1212
#define _loop0_54_type 1213
#define _loop0_55_type 1214
#define _loop0_56_type 1215
#define _loop1_57_type 1216
#define _loop0_58_type 1217
#define _loop1_59_type 1218
#define _loop1_60_type 1219
#define _loop1_61_type 1220
#define _loop0_62_type 1221
#define _loop1_63_type 1222
#define _loop0_64_type 1223
#define _loop1_65_type 1224
#define _loop0_66_type 1225
#define _loop1_67_type 1226
#define _loop1_68_type 1227
#define _tmp_69_type 1228
#define _loop0_71_type 1229
#define _gather_70_type 1230
#define _loop1_72_type 1231
#define _loop0_74_type 1232
#define _gather_73_type 1233
#define _loop1_75_type 1234
#define _loop0_76_type 1235
#define _loop0_77_type 1236
#define _loop0_78_type 1237
#define _loop1_79_type 1238
#define _loop0_80_type 1239
#define _loop1_81_type 1240
#define _loop1_82_type 1241
#define _loop1_83_type 1242
#define _loop0_84_type 1243
#define _loop1_85_type 1244
#define _loop0_86_type 1245
#define _loop1_87_type 1246
#define _loop0_88_type 1247
#define _loop1_89_type 1248
#define _loop1_90_type 1249
#define _loop1_91_type 1250
#define _loop1_92_type 1251
#define _tmp_93_type 1252
#define _loop0_95_type 1253
#define _gather_94_type 1254
#define _tmp_96_type 1255
#define _tmp_97_type 1256
#define _tmp_98_type 1257
#define _tmp_99_type 1258
#define _loop1_100_type 1259
#define _tmp_101_type 1260
#define _tmp_102_type 1261
#define _loop0_104_type 1262
#define _gather_103_type 1263
#define _loop1_105_type 1264
#define _loop0_106_type 1265
#define _loop0_107_type 1266
#define _tmp_108_type 1267
#define _tmp_109_type 1268
#define _loop0_111_type 1269
#define _gather_110_type 1270
#define _loop0_113_type 1271
#define _gather_112_type 1272
#define _loop0_115_type 1273
#define _gather_114_type 1274
#define _loop0_117_type 1275
#define _gather_116_type 1276
#define _loop0_118_type 1277
#define _loop0_120_type 1278
#define _gather_119_type 1279
#define _tmp_121_type 1280
#define _loop0_123_type 1281
#define _gather_122_type 1282
#define _loop0_125_type 1283
#define _gather_124_type 1284
#define _tmp_126_type 1285
#define _loop0_127_type 1286
#define _tmp_128_type 1287
#define _loop0_129_type 1288
#define _loop0_130_type 1289
#define _tmp_131_type 1290
#define _tmp_132_type 1291
#define _loop0_133_type 1292
#define _tmp_134_type 1293
#define _loop0_135_type 1294
#define _tmp_136_type 1295
#define _tmp_137_type 1296
#define _tmp_138_type 1297
#define _tmp_139_type 1298
#define _tmp_140_type 1299
#define _tmp_141_type 1300
#define _tmp_142_type 1301
#define _tmp_143_type 1302
#define _tmp_144_type 1303
#define _tmp_145_type 1304
#define _tmp_146_type 1305
#define _tmp_147_type 1306
#define _tmp_148_type 1307
#define _tmp_149_type 1308
#define _tmp_150_type 1309
#define _tmp_151_type 1310
#define _loop1_152_type 1311
#define _loop1_153_type 1312
#define _tmp_154_type 1313
#define _tmp_155_type 1314
```

Parser文法API函数

```cpp
static mod_ty file_rule(Parser *p);
static mod_ty interactive_rule(Parser *p);
static mod_ty eval_rule(Parser *p);
static mod_ty func_type_rule(Parser *p);
static expr_ty fstring_rule(Parser *p);
static asdl_seq* type_expressions_rule(Parser *p);
static asdl_seq* statements_rule(Parser *p);
static asdl_seq* statement_rule(Parser *p);
static asdl_seq* statement_newline_rule(Parser *p);
static asdl_seq* simple_stmt_rule(Parser *p);
static stmt_ty small_stmt_rule(Parser *p);
static stmt_ty compound_stmt_rule(Parser *p);
static stmt_ty assignment_rule(Parser *p);
static AugOperator* augassign_rule(Parser *p);
static stmt_ty global_stmt_rule(Parser *p);
static stmt_ty nonlocal_stmt_rule(Parser *p);
static stmt_ty yield_stmt_rule(Parser *p);
static stmt_ty assert_stmt_rule(Parser *p);
static stmt_ty del_stmt_rule(Parser *p);
static stmt_ty import_stmt_rule(Parser *p);
static stmt_ty import_name_rule(Parser *p);
static stmt_ty import_from_rule(Parser *p);
static asdl_seq* import_from_targets_rule(Parser *p);
static asdl_seq* import_from_as_names_rule(Parser *p);
static alias_ty import_from_as_name_rule(Parser *p);
static asdl_seq* dotted_as_names_rule(Parser *p);
static alias_ty dotted_as_name_rule(Parser *p);
static expr_ty dotted_name_rule(Parser *p);
static stmt_ty if_stmt_rule(Parser *p);
static stmt_ty elif_stmt_rule(Parser *p);
static asdl_seq* else_block_rule(Parser *p);
static stmt_ty while_stmt_rule(Parser *p);
static stmt_ty for_stmt_rule(Parser *p);
static stmt_ty with_stmt_rule(Parser *p);
static withitem_ty with_item_rule(Parser *p);
static stmt_ty try_stmt_rule(Parser *p);
static excepthandler_ty except_block_rule(Parser *p);
static asdl_seq* finally_block_rule(Parser *p);
static stmt_ty return_stmt_rule(Parser *p);
static stmt_ty raise_stmt_rule(Parser *p);
static stmt_ty function_def_rule(Parser *p);
static stmt_ty function_def_raw_rule(Parser *p);
static Token* func_type_comment_rule(Parser *p);
static arguments_ty params_rule(Parser *p);
static arguments_ty parameters_rule(Parser *p);
static asdl_seq* slash_no_default_rule(Parser *p);
static SlashWithDefault* slash_with_default_rule(Parser *p);
static StarEtc* star_etc_rule(Parser *p);
static arg_ty kwds_rule(Parser *p);
static arg_ty param_no_default_rule(Parser *p);
static NameDefaultPair* param_with_default_rule(Parser *p);
static NameDefaultPair* param_maybe_default_rule(Parser *p);
static arg_ty param_rule(Parser *p);
static expr_ty annotation_rule(Parser *p);
static expr_ty default_rule(Parser *p);
static asdl_seq* decorators_rule(Parser *p);
static stmt_ty class_def_rule(Parser *p);
static stmt_ty class_def_raw_rule(Parser *p);
static asdl_seq* block_rule(Parser *p);
static asdl_seq* expressions_list_rule(Parser *p);
static expr_ty star_expressions_rule(Parser *p);
static expr_ty star_expression_rule(Parser *p);
static asdl_seq* star_named_expressions_rule(Parser *p);
static expr_ty star_named_expression_rule(Parser *p);
static expr_ty named_expression_rule(Parser *p);
static expr_ty annotated_rhs_rule(Parser *p);
static expr_ty expressions_rule(Parser *p);
static expr_ty expression_rule(Parser *p);
static expr_ty lambdef_rule(Parser *p);
static arguments_ty lambda_params_rule(Parser *p);
static arguments_ty lambda_parameters_rule(Parser *p);
static asdl_seq* lambda_slash_no_default_rule(Parser *p);
static SlashWithDefault* lambda_slash_with_default_rule(Parser *p);
static StarEtc* lambda_star_etc_rule(Parser *p);
static arg_ty lambda_kwds_rule(Parser *p);
static arg_ty lambda_param_no_default_rule(Parser *p);
static NameDefaultPair* lambda_param_with_default_rule(Parser *p);
static NameDefaultPair* lambda_param_maybe_default_rule(Parser *p);
static arg_ty lambda_param_rule(Parser *p);
static expr_ty disjunction_rule(Parser *p);
static expr_ty conjunction_rule(Parser *p);
static expr_ty inversion_rule(Parser *p);
static expr_ty comparison_rule(Parser *p);
static CmpopExprPair* compare_op_bitwise_or_pair_rule(Parser *p);
static CmpopExprPair* eq_bitwise_or_rule(Parser *p);
static CmpopExprPair* noteq_bitwise_or_rule(Parser *p);
static CmpopExprPair* lte_bitwise_or_rule(Parser *p);
static CmpopExprPair* lt_bitwise_or_rule(Parser *p);
static CmpopExprPair* gte_bitwise_or_rule(Parser *p);
static CmpopExprPair* gt_bitwise_or_rule(Parser *p);
static CmpopExprPair* notin_bitwise_or_rule(Parser *p);
static CmpopExprPair* in_bitwise_or_rule(Parser *p);
static CmpopExprPair* isnot_bitwise_or_rule(Parser *p);
static CmpopExprPair* is_bitwise_or_rule(Parser *p);
static expr_ty bitwise_or_rule(Parser *p);
static expr_ty bitwise_xor_rule(Parser *p);
static expr_ty bitwise_and_rule(Parser *p);
static expr_ty shift_expr_rule(Parser *p);
static expr_ty sum_rule(Parser *p);
static expr_ty term_rule(Parser *p);
static expr_ty factor_rule(Parser *p);
static expr_ty power_rule(Parser *p);
static expr_ty await_primary_rule(Parser *p);
static expr_ty primary_rule(Parser *p);
static expr_ty slices_rule(Parser *p);
static expr_ty slice_rule(Parser *p);
static expr_ty atom_rule(Parser *p);
static expr_ty strings_rule(Parser *p);
static expr_ty list_rule(Parser *p);
static expr_ty listcomp_rule(Parser *p);
static expr_ty tuple_rule(Parser *p);
static expr_ty group_rule(Parser *p);
static expr_ty genexp_rule(Parser *p);
static expr_ty set_rule(Parser *p);
static expr_ty setcomp_rule(Parser *p);
static expr_ty dict_rule(Parser *p);
static expr_ty dictcomp_rule(Parser *p);
static asdl_seq* double_starred_kvpairs_rule(Parser *p);
static KeyValuePair* double_starred_kvpair_rule(Parser *p);
static KeyValuePair* kvpair_rule(Parser *p);
static asdl_seq* for_if_clauses_rule(Parser *p);
static comprehension_ty for_if_clause_rule(Parser *p);
static expr_ty yield_expr_rule(Parser *p);
static expr_ty arguments_rule(Parser *p);
static expr_ty args_rule(Parser *p);
static asdl_seq* kwargs_rule(Parser *p);
static expr_ty starred_expression_rule(Parser *p);
static KeywordOrStarred* kwarg_or_starred_rule(Parser *p);
static KeywordOrStarred* kwarg_or_double_starred_rule(Parser *p);
static expr_ty star_targets_rule(Parser *p);
static asdl_seq* star_targets_seq_rule(Parser *p);
static expr_ty star_target_rule(Parser *p);
static expr_ty star_atom_rule(Parser *p);
static expr_ty single_target_rule(Parser *p);
static expr_ty single_subscript_attribute_target_rule(Parser *p);
static asdl_seq* del_targets_rule(Parser *p);
static expr_ty del_target_rule(Parser *p);
static expr_ty del_t_atom_rule(Parser *p);
static asdl_seq* targets_rule(Parser *p);
static expr_ty target_rule(Parser *p);
static expr_ty t_primary_rule(Parser *p);
static void *t_lookahead_rule(Parser *p);
static expr_ty t_atom_rule(Parser *p);
static void *incorrect_arguments_rule(Parser *p);
static void *invalid_kwarg_rule(Parser *p);
static void *invalid_named_expression_rule(Parser *p);
static void *invalid_assignment_rule(Parser *p);
static void *invalid_del_stmt_rule(Parser *p);
static void *invalid_block_rule(Parser *p);
static void *invalid_comprehension_rule(Parser *p);
static void *invalid_dict_comprehension_rule(Parser *p);
static void *invalid_parameters_rule(Parser *p);
static void *invalid_lambda_parameters_rule(Parser *p);
static void *invalid_star_etc_rule(Parser *p);
static void *invalid_lambda_star_etc_rule(Parser *p);
static void *invalid_double_type_comments_rule(Parser *p);
static void *invalid_with_item_rule(Parser *p);
static void *invalid_for_target_rule(Parser *p);
static void *invalid_group_rule(Parser *p);
static void *invalid_import_from_targets_rule(Parser *p);
static asdl_seq *_loop0_1_rule(Parser *p);
static asdl_seq *_loop0_2_rule(Parser *p);
static asdl_seq *_loop0_4_rule(Parser *p);
static asdl_seq *_gather_3_rule(Parser *p);
static asdl_seq *_loop0_6_rule(Parser *p);
static asdl_seq *_gather_5_rule(Parser *p);
static asdl_seq *_loop0_8_rule(Parser *p);
static asdl_seq *_gather_7_rule(Parser *p);
static asdl_seq *_loop0_10_rule(Parser *p);
static asdl_seq *_gather_9_rule(Parser *p);
static asdl_seq *_loop1_11_rule(Parser *p);
static asdl_seq *_loop0_13_rule(Parser *p);
static asdl_seq *_gather_12_rule(Parser *p);
static void *_tmp_14_rule(Parser *p);
static void *_tmp_15_rule(Parser *p);
static void *_tmp_16_rule(Parser *p);
static void *_tmp_17_rule(Parser *p);
static void *_tmp_18_rule(Parser *p);
static void *_tmp_19_rule(Parser *p);
static void *_tmp_20_rule(Parser *p);
static void *_tmp_21_rule(Parser *p);
static asdl_seq *_loop1_22_rule(Parser *p);
static void *_tmp_23_rule(Parser *p);
static void *_tmp_24_rule(Parser *p);
static asdl_seq *_loop0_26_rule(Parser *p);
static asdl_seq *_gather_25_rule(Parser *p);
static asdl_seq *_loop0_28_rule(Parser *p);
static asdl_seq *_gather_27_rule(Parser *p);
static void *_tmp_29_rule(Parser *p);
static void *_tmp_30_rule(Parser *p);
static asdl_seq *_loop0_31_rule(Parser *p);
static asdl_seq *_loop1_32_rule(Parser *p);
static asdl_seq *_loop0_34_rule(Parser *p);
static asdl_seq *_gather_33_rule(Parser *p);
static void *_tmp_35_rule(Parser *p);
static asdl_seq *_loop0_37_rule(Parser *p);
static asdl_seq *_gather_36_rule(Parser *p);
static void *_tmp_38_rule(Parser *p);
static asdl_seq *_loop0_40_rule(Parser *p);
static asdl_seq *_gather_39_rule(Parser *p);
static asdl_seq *_loop0_42_rule(Parser *p);
static asdl_seq *_gather_41_rule(Parser *p);
static asdl_seq *_loop0_44_rule(Parser *p);
static asdl_seq *_gather_43_rule(Parser *p);
static asdl_seq *_loop0_46_rule(Parser *p);
static asdl_seq *_gather_45_rule(Parser *p);
static void *_tmp_47_rule(Parser *p);
static asdl_seq *_loop1_48_rule(Parser *p);
static void *_tmp_49_rule(Parser *p);
static void *_tmp_50_rule(Parser *p);
static void *_tmp_51_rule(Parser *p);
static void *_tmp_52_rule(Parser *p);
static void *_tmp_53_rule(Parser *p);
static asdl_seq *_loop0_54_rule(Parser *p);
static asdl_seq *_loop0_55_rule(Parser *p);
static asdl_seq *_loop0_56_rule(Parser *p);
static asdl_seq *_loop1_57_rule(Parser *p);
static asdl_seq *_loop0_58_rule(Parser *p);
static asdl_seq *_loop1_59_rule(Parser *p);
static asdl_seq *_loop1_60_rule(Parser *p);
static asdl_seq *_loop1_61_rule(Parser *p);
static asdl_seq *_loop0_62_rule(Parser *p);
static asdl_seq *_loop1_63_rule(Parser *p);
static asdl_seq *_loop0_64_rule(Parser *p);
static asdl_seq *_loop1_65_rule(Parser *p);
static asdl_seq *_loop0_66_rule(Parser *p);
static asdl_seq *_loop1_67_rule(Parser *p);
static asdl_seq *_loop1_68_rule(Parser *p);
static void *_tmp_69_rule(Parser *p);
static asdl_seq *_loop0_71_rule(Parser *p);
static asdl_seq *_gather_70_rule(Parser *p);
static asdl_seq *_loop1_72_rule(Parser *p);
static asdl_seq *_loop0_74_rule(Parser *p);
static asdl_seq *_gather_73_rule(Parser *p);
static asdl_seq *_loop1_75_rule(Parser *p);
static asdl_seq *_loop0_76_rule(Parser *p);
static asdl_seq *_loop0_77_rule(Parser *p);
static asdl_seq *_loop0_78_rule(Parser *p);
static asdl_seq *_loop1_79_rule(Parser *p);
static asdl_seq *_loop0_80_rule(Parser *p);
static asdl_seq *_loop1_81_rule(Parser *p);
static asdl_seq *_loop1_82_rule(Parser *p);
static asdl_seq *_loop1_83_rule(Parser *p);
static asdl_seq *_loop0_84_rule(Parser *p);
static asdl_seq *_loop1_85_rule(Parser *p);
static asdl_seq *_loop0_86_rule(Parser *p);
static asdl_seq *_loop1_87_rule(Parser *p);
static asdl_seq *_loop0_88_rule(Parser *p);
static asdl_seq *_loop1_89_rule(Parser *p);
static asdl_seq *_loop1_90_rule(Parser *p);
static asdl_seq *_loop1_91_rule(Parser *p);
static asdl_seq *_loop1_92_rule(Parser *p);
static void *_tmp_93_rule(Parser *p);
static asdl_seq *_loop0_95_rule(Parser *p);
static asdl_seq *_gather_94_rule(Parser *p);
static void *_tmp_96_rule(Parser *p);
static void *_tmp_97_rule(Parser *p);
static void *_tmp_98_rule(Parser *p);
static void *_tmp_99_rule(Parser *p);
static asdl_seq *_loop1_100_rule(Parser *p);
static void *_tmp_101_rule(Parser *p);
static void *_tmp_102_rule(Parser *p);
static asdl_seq *_loop0_104_rule(Parser *p);
static asdl_seq *_gather_103_rule(Parser *p);
static asdl_seq *_loop1_105_rule(Parser *p);
static asdl_seq *_loop0_106_rule(Parser *p);
static asdl_seq *_loop0_107_rule(Parser *p);
static void *_tmp_108_rule(Parser *p);
static void *_tmp_109_rule(Parser *p);
static asdl_seq *_loop0_111_rule(Parser *p);
static asdl_seq *_gather_110_rule(Parser *p);
static asdl_seq *_loop0_113_rule(Parser *p);
static asdl_seq *_gather_112_rule(Parser *p);
static asdl_seq *_loop0_115_rule(Parser *p);
static asdl_seq *_gather_114_rule(Parser *p);
static asdl_seq *_loop0_117_rule(Parser *p);
static asdl_seq *_gather_116_rule(Parser *p);
static asdl_seq *_loop0_118_rule(Parser *p);
static asdl_seq *_loop0_120_rule(Parser *p);
static asdl_seq *_gather_119_rule(Parser *p);
static void *_tmp_121_rule(Parser *p);
static asdl_seq *_loop0_123_rule(Parser *p);
static asdl_seq *_gather_122_rule(Parser *p);
static asdl_seq *_loop0_125_rule(Parser *p);
static asdl_seq *_gather_124_rule(Parser *p);
static void *_tmp_126_rule(Parser *p);
static asdl_seq *_loop0_127_rule(Parser *p);
static void *_tmp_128_rule(Parser *p);
static asdl_seq *_loop0_129_rule(Parser *p);
static asdl_seq *_loop0_130_rule(Parser *p);
static void *_tmp_131_rule(Parser *p);
static void *_tmp_132_rule(Parser *p);
static asdl_seq *_loop0_133_rule(Parser *p);
static void *_tmp_134_rule(Parser *p);
static asdl_seq *_loop0_135_rule(Parser *p);
static void *_tmp_136_rule(Parser *p);
static void *_tmp_137_rule(Parser *p);
static void *_tmp_138_rule(Parser *p);
static void *_tmp_139_rule(Parser *p);
static void *_tmp_140_rule(Parser *p);
static void *_tmp_141_rule(Parser *p);
static void *_tmp_142_rule(Parser *p);
static void *_tmp_143_rule(Parser *p);
static void *_tmp_144_rule(Parser *p);
static void *_tmp_145_rule(Parser *p);
static void *_tmp_146_rule(Parser *p);
static void *_tmp_147_rule(Parser *p);
static void *_tmp_148_rule(Parser *p);
static void *_tmp_149_rule(Parser *p);
static void *_tmp_150_rule(Parser *p);
static void *_tmp_151_rule(Parser *p);
static asdl_seq *_loop1_152_rule(Parser *p);
static asdl_seq *_loop1_153_rule(Parser *p);
static void *_tmp_154_rule(Parser *p);
static void *_tmp_155_rule(Parser *p);
```
