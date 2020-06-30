
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
/* Get next token, after space stripping etc. */

static int
tok_get(struct tok_state *tok, const char **p_start, const char **p_end)
{
    int c;
    int blankline, nonascii;

    *p_start = *p_end = NULL;
  nextline:
    tok->start = NULL;
    blankline = 0;

    /* Get indentation level */
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
            if (col == 0 && c == '\n' && tok->prompt != NULL) {
                blankline = 0; /* Let it through */
            }
            else if (tok->prompt != NULL && tok->lineno == 1) {
                /* In interactive mode, if the first line contains
                   only spaces and/or a comment, let it through. */
                blankline = 0;
                col = altcol = 0;
            }
            else {
                blankline = 1; /* Ignore completely */
            }
            /* We can't jump back right here since we still
               may need to skip to the end of a comment */
        }
        if (!blankline && tok->level == 0) {
            if (col == tok->indstack[tok->indent]) {
                /* No change */
                if (altcol != tok->altindstack[tok->indent]) {
                    return indenterror(tok);
                }
            }
            else if (col > tok->indstack[tok->indent]) {
                /* Indent -- always one */
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
                /* Dedent -- any number, must be consistent */
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

    /* Return pending indents/dedents */
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

    /* Peek ahead at the next character */
    c = tok_nextc(tok);
    tok_backup(tok, c);
    /* Check if we are closing an async function */
    if (tok->async_def
        && !blankline
        /* Due to some implementation artifacts of type comments,
         * a TYPE_COMMENT at the start of a function won't set an
         * indentation level and it will produce a NEWLINE after it.
         * To avoid spuriously ending an async function due to this,
         * wait until we have some non-newline char in front of us. */
        && c != '\n'
        && tok->level == 0
        /* There was a NEWLINE after ASYNC DEF,
           so we're past the signature. */
        && tok->async_def_nl
        /* Current indentation level is less than where
           the async function was defined */
        && tok->async_def_indent >= tok->indent)
    {
        tok->async_def = 0;
        tok->async_def_indent = 0;
        tok->async_def_nl = 0;
    }

 again:
    tok->start = NULL;
    /* Skip spaces */
    do {
        c = tok_nextc(tok);
    } while (c == ' ' || c == '\t' || c == '\014');

    /* Set start of current token */
    tok->start = tok->cur - 1;

    /* Skip comment, unless it's a type comment */
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

            /* This is a type comment if we matched all of type_comment_prefix. */
            if (!*prefix) {
                int is_type_ignore = 1;
                const char *ignore_end = p + 6;
                tok_backup(tok, c);  /* don't eat the newline or EOF */

                type_start = p;

                /* A TYPE_IGNORE is "type: ignore" followed by the end of the token
                 * or anything ASCII and non-alphanumeric. */
                is_type_ignore = (
                    tok->cur >= ignore_end && memcmp(p, "ignore", 6) == 0
                    && !(tok->cur > ignore_end
                         && ((unsigned char)ignore_end[0] >= 128 || Py_ISALNUM(ignore_end[0]))));

                if (is_type_ignore) {
                    *p_start = ignore_end;
                    *p_end = tok->cur;

                    /* If this type ignore is the only thing on the line, consume the newline also. */
                    if (blankline) {
                        tok_nextc(tok);
                        tok->atbol = 1;
                    }
                    return TYPE_IGNORE;
                } else {
                    *p_start = type_start;  /* after type_comment_prefix */
                    *p_end = tok->cur;
                    return TYPE_COMMENT;
                }
            }
        }
    }

    /* Check for EOF and errors now */
    if (c == EOF) {
        return tok->done == E_EOF ? ENDMARKER : ERRORTOKEN;
    }

    /* Identifier (most frequent token!) */
    nonascii = 0;
    if (is_potential_identifier_start(c)) {
        /* Process the various legal combinations of b"", r"", u"", and f"". */
        int saw_b = 0, saw_r = 0, saw_u = 0, saw_f = 0;
        while (1) {
            if (!(saw_b || saw_u || saw_f) && (c == 'b' || c == 'B'))
                saw_b = 1;
            /* Since this is a backwards compatibility support literal we don't
               want to support it in arbitrary order like byte literals. */
            else if (!(saw_b || saw_u || saw_r || saw_f)
                     && (c == 'u'|| c == 'U')) {
                saw_u = 1;
            }
            /* ur"" and ru"" are not supported */
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
            if (!tok->async_hacks || tok->async_def) {
                /* Always recognize the keywords. */
                if (memcmp(tok->start, "async", 5) == 0) {
                    return ASYNC;
                }
                if (memcmp(tok->start, "await", 5) == 0) {
                    return AWAIT;
                }
            }
            else if (memcmp(tok->start, "async", 5) == 0) {
                /* The current token is 'async'.
                   Look ahead one token to see if that is 'def'. */

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
                       returning a plain NAME token, return ASYNC. */
                    tok->async_def_indent = tok->indent;
                    tok->async_def = 1;
                    return ASYNC;
                }
            }
        }

        return NAME;
    }

    /* Newline */
    if (c == '\n') {
        tok->atbol = 1;
        if (blankline || tok->level > 0) {
            goto nextline;
        }
        *p_start = tok->start;
        *p_end = tok->cur - 1; /* Leave '\n' out of the string */
        tok->cont_line = 0;
        if (tok->async_def) {
            /* We're somewhere inside an 'async def' function, and
               we've encountered a NEWLINE after its signature. */
            tok->async_def_nl = 1;
        }
        return NEWLINE;
    }

    /* Period or number starting with period? */
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

    /* Number */
    if (isdigit(c)) {
        if (c == '0') {
            /* Hex, octal or binary -- maybe. */
            c = tok_nextc(tok);
            if (c == 'x' || c == 'X') {
                /* Hex */
                c = tok_nextc(tok);
                do {
                    if (c == '_') {
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
                /* Octal */
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
                /* Binary */
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
                /* maybe old-style octal; c is first char of it */
                /* in any case, allow '0' as a literal */
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
                    goto fraction;
                }
                else if (c == 'e' || c == 'E') {
                    goto exponent;
                }
                else if (c == 'j' || c == 'J') {
                    goto imaginary;
                }
                else if (nonzero) {
                    /* Old-style octal: now disallowed. */
                    tok_backup(tok, c);
                    return syntaxerror(tok,
                                       "leading zeros in decimal integer "
                                       "literals are not permitted; "
                                       "use an 0o prefix for octal integers");
                }
            }
        }
        else {
            /* Decimal */
            c = tok_decimal_tail(tok);
            if (c == 0) {
                return ERRORTOKEN;
            }
            {
                /* Accept floating point numbers. */
                if (c == '.') {
                    c = tok_nextc(tok);
        fraction:
                    /* Fraction */
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
                    /* Exponent part */
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
                    /* Imaginary part */
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

  letter_quote:
    /* String */
    if (c == '\'' || c == '"') {
        int quote = c;
        int quote_size = 1;             /* 1 or 3 */
        int end_quote_size = 0;

        /* Nodes of type STRING, especially multi line strings
           must be handled differently in order to get both
           the starting line number and the column offset right.
           (cf. issue 16806) */
        tok->first_lineno = tok->lineno;
        tok->multi_line_start = tok->line_start;

        /* Find the quote size and start of string */
        c = tok_nextc(tok);
        if (c == quote) {
            c = tok_nextc(tok);
            if (c == quote) {
                quote_size = 3;
            }
            else {
                end_quote_size = 1;     /* empty string found */
            }
        }
        if (c != quote) {
            tok_backup(tok, c);
        }

        /* Get rest of string */
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
                    tok_nextc(tok);  /* skip escaped char */
                }
            }
        }

        *p_start = tok->start;
        *p_end = tok->cur;
        return STRING;
    }

    /* Line continuation */
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
        goto again; /* Read next line */
    }

    /* Check for two-character token */
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

    /* Keep track of parentheses nesting level */
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

    /* Punctuation character */
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
```

### CPython 语法分析器


