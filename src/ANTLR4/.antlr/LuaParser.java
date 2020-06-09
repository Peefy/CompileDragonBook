// Generated from /Volumes/MSD64G/Developer/CodeLanuage/C++/CompileDragonBook.Cpp/src/ANTLR4/Lua.g4 by ANTLR 4.7.1
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class LuaParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.7.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, T__14=15, T__15=16, T__16=17, 
		T__17=18, T__18=19, T__19=20, T__20=21, T__21=22, T__22=23, T__23=24, 
		T__24=25, T__25=26, T__26=27, T__27=28, T__28=29, T__29=30, T__30=31, 
		T__31=32, T__32=33, T__33=34, T__34=35, T__35=36, T__36=37, T__37=38, 
		T__38=39, T__39=40, T__40=41, T__41=42, T__42=43, T__43=44, T__44=45, 
		T__45=46, T__46=47, T__47=48, T__48=49, T__49=50, T__50=51, T__51=52, 
		T__52=53, T__53=54, T__54=55, NAME=56, NORMALSTRING=57, CHARSTRING=58, 
		LONGSTRING=59, INT=60, HEX=61, FLOAT=62, HEX_FLOAT=63, COMMENT=64, LINE_COMMENT=65, 
		WS=66, SHEBANG=67;
	public static final int
		RULE_chunk = 0, RULE_block = 1, RULE_stat = 2, RULE_retstat = 3, RULE_label = 4, 
		RULE_funcname = 5, RULE_varlist = 6, RULE_namelist = 7, RULE_explist = 8, 
		RULE_exp = 9, RULE_prefixexp = 10, RULE_functioncall = 11, RULE_varOrExp = 12, 
		RULE_var = 13, RULE_varSuffix = 14, RULE_nameAndArgs = 15, RULE_args = 16, 
		RULE_functiondef = 17, RULE_funcbody = 18, RULE_parlist = 19, RULE_tableconstructor = 20, 
		RULE_fieldlist = 21, RULE_field = 22, RULE_fieldsep = 23, RULE_operatorOr = 24, 
		RULE_operatorAnd = 25, RULE_operatorComparison = 26, RULE_operatorStrcat = 27, 
		RULE_operatorAddSub = 28, RULE_operatorMulDivMod = 29, RULE_operatorBitwise = 30, 
		RULE_operatorUnary = 31, RULE_operatorPower = 32, RULE_number = 33, RULE_string = 34;
	public static final String[] ruleNames = {
		"chunk", "block", "stat", "retstat", "label", "funcname", "varlist", "namelist", 
		"explist", "exp", "prefixexp", "functioncall", "varOrExp", "var", "varSuffix", 
		"nameAndArgs", "args", "functiondef", "funcbody", "parlist", "tableconstructor", 
		"fieldlist", "field", "fieldsep", "operatorOr", "operatorAnd", "operatorComparison", 
		"operatorStrcat", "operatorAddSub", "operatorMulDivMod", "operatorBitwise", 
		"operatorUnary", "operatorPower", "number", "string"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "';'", "'='", "'break'", "'goto'", "'do'", "'end'", "'while'", "'repeat'", 
		"'until'", "'if'", "'then'", "'elseif'", "'else'", "'for'", "','", "'in'", 
		"'function'", "'local'", "'return'", "'::'", "'.'", "':'", "'nil'", "'false'", 
		"'true'", "'...'", "'('", "')'", "'['", "']'", "'{'", "'}'", "'or'", "'and'", 
		"'<'", "'>'", "'<='", "'>='", "'~='", "'=='", "'..'", "'+'", "'-'", "'*'", 
		"'/'", "'%'", "'//'", "'&'", "'|'", "'~'", "'<<'", "'>>'", "'not'", "'#'", 
		"'^'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, "NAME", "NORMALSTRING", 
		"CHARSTRING", "LONGSTRING", "INT", "HEX", "FLOAT", "HEX_FLOAT", "COMMENT", 
		"LINE_COMMENT", "WS", "SHEBANG"
	};
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "Lua.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public LuaParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class ChunkContext extends ParserRuleContext {
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public TerminalNode EOF() { return getToken(LuaParser.EOF, 0); }
		public ChunkContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_chunk; }
	}

	public final ChunkContext chunk() throws RecognitionException {
		ChunkContext _localctx = new ChunkContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_chunk);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(70);
			block();
			setState(71);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class BlockContext extends ParserRuleContext {
		public List<StatContext> stat() {
			return getRuleContexts(StatContext.class);
		}
		public StatContext stat(int i) {
			return getRuleContext(StatContext.class,i);
		}
		public RetstatContext retstat() {
			return getRuleContext(RetstatContext.class,0);
		}
		public BlockContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_block; }
	}

	public final BlockContext block() throws RecognitionException {
		BlockContext _localctx = new BlockContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_block);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(76);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__2) | (1L << T__3) | (1L << T__4) | (1L << T__6) | (1L << T__7) | (1L << T__9) | (1L << T__13) | (1L << T__16) | (1L << T__17) | (1L << T__19) | (1L << T__26) | (1L << NAME))) != 0)) {
				{
				{
				setState(73);
				stat();
				}
				}
				setState(78);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(80);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__18) {
				{
				setState(79);
				retstat();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class StatContext extends ParserRuleContext {
		public VarlistContext varlist() {
			return getRuleContext(VarlistContext.class,0);
		}
		public ExplistContext explist() {
			return getRuleContext(ExplistContext.class,0);
		}
		public FunctioncallContext functioncall() {
			return getRuleContext(FunctioncallContext.class,0);
		}
		public LabelContext label() {
			return getRuleContext(LabelContext.class,0);
		}
		public TerminalNode NAME() { return getToken(LuaParser.NAME, 0); }
		public List<BlockContext> block() {
			return getRuleContexts(BlockContext.class);
		}
		public BlockContext block(int i) {
			return getRuleContext(BlockContext.class,i);
		}
		public List<ExpContext> exp() {
			return getRuleContexts(ExpContext.class);
		}
		public ExpContext exp(int i) {
			return getRuleContext(ExpContext.class,i);
		}
		public NamelistContext namelist() {
			return getRuleContext(NamelistContext.class,0);
		}
		public FuncnameContext funcname() {
			return getRuleContext(FuncnameContext.class,0);
		}
		public FuncbodyContext funcbody() {
			return getRuleContext(FuncbodyContext.class,0);
		}
		public StatContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_stat; }
	}

	public final StatContext stat() throws RecognitionException {
		StatContext _localctx = new StatContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_stat);
		int _la;
		try {
			setState(163);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,6,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(82);
				match(T__0);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(83);
				varlist();
				setState(84);
				match(T__1);
				setState(85);
				explist();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(87);
				functioncall();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(88);
				label();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(89);
				match(T__2);
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(90);
				match(T__3);
				setState(91);
				match(NAME);
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(92);
				match(T__4);
				setState(93);
				block();
				setState(94);
				match(T__5);
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(96);
				match(T__6);
				setState(97);
				exp(0);
				setState(98);
				match(T__4);
				setState(99);
				block();
				setState(100);
				match(T__5);
				}
				break;
			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(102);
				match(T__7);
				setState(103);
				block();
				setState(104);
				match(T__8);
				setState(105);
				exp(0);
				}
				break;
			case 10:
				enterOuterAlt(_localctx, 10);
				{
				setState(107);
				match(T__9);
				setState(108);
				exp(0);
				setState(109);
				match(T__10);
				setState(110);
				block();
				setState(118);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__11) {
					{
					{
					setState(111);
					match(T__11);
					setState(112);
					exp(0);
					setState(113);
					match(T__10);
					setState(114);
					block();
					}
					}
					setState(120);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(123);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__12) {
					{
					setState(121);
					match(T__12);
					setState(122);
					block();
					}
				}

				setState(125);
				match(T__5);
				}
				break;
			case 11:
				enterOuterAlt(_localctx, 11);
				{
				setState(127);
				match(T__13);
				setState(128);
				match(NAME);
				setState(129);
				match(T__1);
				setState(130);
				exp(0);
				setState(131);
				match(T__14);
				setState(132);
				exp(0);
				setState(135);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__14) {
					{
					setState(133);
					match(T__14);
					setState(134);
					exp(0);
					}
				}

				setState(137);
				match(T__4);
				setState(138);
				block();
				setState(139);
				match(T__5);
				}
				break;
			case 12:
				enterOuterAlt(_localctx, 12);
				{
				setState(141);
				match(T__13);
				setState(142);
				namelist();
				setState(143);
				match(T__15);
				setState(144);
				explist();
				setState(145);
				match(T__4);
				setState(146);
				block();
				setState(147);
				match(T__5);
				}
				break;
			case 13:
				enterOuterAlt(_localctx, 13);
				{
				setState(149);
				match(T__16);
				setState(150);
				funcname();
				setState(151);
				funcbody();
				}
				break;
			case 14:
				enterOuterAlt(_localctx, 14);
				{
				setState(153);
				match(T__17);
				setState(154);
				match(T__16);
				setState(155);
				match(NAME);
				setState(156);
				funcbody();
				}
				break;
			case 15:
				enterOuterAlt(_localctx, 15);
				{
				setState(157);
				match(T__17);
				setState(158);
				namelist();
				setState(161);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__1) {
					{
					setState(159);
					match(T__1);
					setState(160);
					explist();
					}
				}

				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class RetstatContext extends ParserRuleContext {
		public ExplistContext explist() {
			return getRuleContext(ExplistContext.class,0);
		}
		public RetstatContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_retstat; }
	}

	public final RetstatContext retstat() throws RecognitionException {
		RetstatContext _localctx = new RetstatContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_retstat);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(165);
			match(T__18);
			setState(167);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__16) | (1L << T__22) | (1L << T__23) | (1L << T__24) | (1L << T__25) | (1L << T__26) | (1L << T__30) | (1L << T__42) | (1L << T__49) | (1L << T__52) | (1L << T__53) | (1L << NAME) | (1L << NORMALSTRING) | (1L << CHARSTRING) | (1L << LONGSTRING) | (1L << INT) | (1L << HEX) | (1L << FLOAT) | (1L << HEX_FLOAT))) != 0)) {
				{
				setState(166);
				explist();
				}
			}

			setState(170);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__0) {
				{
				setState(169);
				match(T__0);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class LabelContext extends ParserRuleContext {
		public TerminalNode NAME() { return getToken(LuaParser.NAME, 0); }
		public LabelContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_label; }
	}

	public final LabelContext label() throws RecognitionException {
		LabelContext _localctx = new LabelContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_label);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(172);
			match(T__19);
			setState(173);
			match(NAME);
			setState(174);
			match(T__19);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FuncnameContext extends ParserRuleContext {
		public List<TerminalNode> NAME() { return getTokens(LuaParser.NAME); }
		public TerminalNode NAME(int i) {
			return getToken(LuaParser.NAME, i);
		}
		public FuncnameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_funcname; }
	}

	public final FuncnameContext funcname() throws RecognitionException {
		FuncnameContext _localctx = new FuncnameContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_funcname);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(176);
			match(NAME);
			setState(181);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__20) {
				{
				{
				setState(177);
				match(T__20);
				setState(178);
				match(NAME);
				}
				}
				setState(183);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(186);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__21) {
				{
				setState(184);
				match(T__21);
				setState(185);
				match(NAME);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class VarlistContext extends ParserRuleContext {
		public List<VarContext> var() {
			return getRuleContexts(VarContext.class);
		}
		public VarContext var(int i) {
			return getRuleContext(VarContext.class,i);
		}
		public VarlistContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_varlist; }
	}

	public final VarlistContext varlist() throws RecognitionException {
		VarlistContext _localctx = new VarlistContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_varlist);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(188);
			var();
			setState(193);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__14) {
				{
				{
				setState(189);
				match(T__14);
				setState(190);
				var();
				}
				}
				setState(195);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NamelistContext extends ParserRuleContext {
		public List<TerminalNode> NAME() { return getTokens(LuaParser.NAME); }
		public TerminalNode NAME(int i) {
			return getToken(LuaParser.NAME, i);
		}
		public NamelistContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_namelist; }
	}

	public final NamelistContext namelist() throws RecognitionException {
		NamelistContext _localctx = new NamelistContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_namelist);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(196);
			match(NAME);
			setState(201);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,12,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(197);
					match(T__14);
					setState(198);
					match(NAME);
					}
					} 
				}
				setState(203);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,12,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ExplistContext extends ParserRuleContext {
		public List<ExpContext> exp() {
			return getRuleContexts(ExpContext.class);
		}
		public ExpContext exp(int i) {
			return getRuleContext(ExpContext.class,i);
		}
		public ExplistContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_explist; }
	}

	public final ExplistContext explist() throws RecognitionException {
		ExplistContext _localctx = new ExplistContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_explist);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(204);
			exp(0);
			setState(209);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__14) {
				{
				{
				setState(205);
				match(T__14);
				setState(206);
				exp(0);
				}
				}
				setState(211);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ExpContext extends ParserRuleContext {
		public NumberContext number() {
			return getRuleContext(NumberContext.class,0);
		}
		public StringContext string() {
			return getRuleContext(StringContext.class,0);
		}
		public FunctiondefContext functiondef() {
			return getRuleContext(FunctiondefContext.class,0);
		}
		public PrefixexpContext prefixexp() {
			return getRuleContext(PrefixexpContext.class,0);
		}
		public TableconstructorContext tableconstructor() {
			return getRuleContext(TableconstructorContext.class,0);
		}
		public OperatorUnaryContext operatorUnary() {
			return getRuleContext(OperatorUnaryContext.class,0);
		}
		public List<ExpContext> exp() {
			return getRuleContexts(ExpContext.class);
		}
		public ExpContext exp(int i) {
			return getRuleContext(ExpContext.class,i);
		}
		public OperatorPowerContext operatorPower() {
			return getRuleContext(OperatorPowerContext.class,0);
		}
		public OperatorMulDivModContext operatorMulDivMod() {
			return getRuleContext(OperatorMulDivModContext.class,0);
		}
		public OperatorAddSubContext operatorAddSub() {
			return getRuleContext(OperatorAddSubContext.class,0);
		}
		public OperatorStrcatContext operatorStrcat() {
			return getRuleContext(OperatorStrcatContext.class,0);
		}
		public OperatorComparisonContext operatorComparison() {
			return getRuleContext(OperatorComparisonContext.class,0);
		}
		public OperatorAndContext operatorAnd() {
			return getRuleContext(OperatorAndContext.class,0);
		}
		public OperatorOrContext operatorOr() {
			return getRuleContext(OperatorOrContext.class,0);
		}
		public OperatorBitwiseContext operatorBitwise() {
			return getRuleContext(OperatorBitwiseContext.class,0);
		}
		public ExpContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_exp; }
	}

	public final ExpContext exp() throws RecognitionException {
		return exp(0);
	}

	private ExpContext exp(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		ExpContext _localctx = new ExpContext(_ctx, _parentState);
		ExpContext _prevctx = _localctx;
		int _startState = 18;
		enterRecursionRule(_localctx, 18, RULE_exp, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(225);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__22:
				{
				setState(213);
				match(T__22);
				}
				break;
			case T__23:
				{
				setState(214);
				match(T__23);
				}
				break;
			case T__24:
				{
				setState(215);
				match(T__24);
				}
				break;
			case INT:
			case HEX:
			case FLOAT:
			case HEX_FLOAT:
				{
				setState(216);
				number();
				}
				break;
			case NORMALSTRING:
			case CHARSTRING:
			case LONGSTRING:
				{
				setState(217);
				string();
				}
				break;
			case T__25:
				{
				setState(218);
				match(T__25);
				}
				break;
			case T__16:
				{
				setState(219);
				functiondef();
				}
				break;
			case T__26:
			case NAME:
				{
				setState(220);
				prefixexp();
				}
				break;
			case T__30:
				{
				setState(221);
				tableconstructor();
				}
				break;
			case T__42:
			case T__49:
			case T__52:
			case T__53:
				{
				setState(222);
				operatorUnary();
				setState(223);
				exp(8);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(261);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,16,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(259);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,15,_ctx) ) {
					case 1:
						{
						_localctx = new ExpContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_exp);
						setState(227);
						if (!(precpred(_ctx, 9))) throw new FailedPredicateException(this, "precpred(_ctx, 9)");
						setState(228);
						operatorPower();
						setState(229);
						exp(9);
						}
						break;
					case 2:
						{
						_localctx = new ExpContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_exp);
						setState(231);
						if (!(precpred(_ctx, 7))) throw new FailedPredicateException(this, "precpred(_ctx, 7)");
						setState(232);
						operatorMulDivMod();
						setState(233);
						exp(8);
						}
						break;
					case 3:
						{
						_localctx = new ExpContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_exp);
						setState(235);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(236);
						operatorAddSub();
						setState(237);
						exp(7);
						}
						break;
					case 4:
						{
						_localctx = new ExpContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_exp);
						setState(239);
						if (!(precpred(_ctx, 5))) throw new FailedPredicateException(this, "precpred(_ctx, 5)");
						setState(240);
						operatorStrcat();
						setState(241);
						exp(5);
						}
						break;
					case 5:
						{
						_localctx = new ExpContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_exp);
						setState(243);
						if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
						setState(244);
						operatorComparison();
						setState(245);
						exp(5);
						}
						break;
					case 6:
						{
						_localctx = new ExpContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_exp);
						setState(247);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(248);
						operatorAnd();
						setState(249);
						exp(4);
						}
						break;
					case 7:
						{
						_localctx = new ExpContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_exp);
						setState(251);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(252);
						operatorOr();
						setState(253);
						exp(3);
						}
						break;
					case 8:
						{
						_localctx = new ExpContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_exp);
						setState(255);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(256);
						operatorBitwise();
						setState(257);
						exp(2);
						}
						break;
					}
					} 
				}
				setState(263);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,16,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class PrefixexpContext extends ParserRuleContext {
		public VarOrExpContext varOrExp() {
			return getRuleContext(VarOrExpContext.class,0);
		}
		public List<NameAndArgsContext> nameAndArgs() {
			return getRuleContexts(NameAndArgsContext.class);
		}
		public NameAndArgsContext nameAndArgs(int i) {
			return getRuleContext(NameAndArgsContext.class,i);
		}
		public PrefixexpContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_prefixexp; }
	}

	public final PrefixexpContext prefixexp() throws RecognitionException {
		PrefixexpContext _localctx = new PrefixexpContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_prefixexp);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(264);
			varOrExp();
			setState(268);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,17,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(265);
					nameAndArgs();
					}
					} 
				}
				setState(270);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,17,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FunctioncallContext extends ParserRuleContext {
		public VarOrExpContext varOrExp() {
			return getRuleContext(VarOrExpContext.class,0);
		}
		public List<NameAndArgsContext> nameAndArgs() {
			return getRuleContexts(NameAndArgsContext.class);
		}
		public NameAndArgsContext nameAndArgs(int i) {
			return getRuleContext(NameAndArgsContext.class,i);
		}
		public FunctioncallContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functioncall; }
	}

	public final FunctioncallContext functioncall() throws RecognitionException {
		FunctioncallContext _localctx = new FunctioncallContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_functioncall);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(271);
			varOrExp();
			setState(273); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(272);
					nameAndArgs();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(275); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,18,_ctx);
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class VarOrExpContext extends ParserRuleContext {
		public VarContext var() {
			return getRuleContext(VarContext.class,0);
		}
		public ExpContext exp() {
			return getRuleContext(ExpContext.class,0);
		}
		public VarOrExpContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_varOrExp; }
	}

	public final VarOrExpContext varOrExp() throws RecognitionException {
		VarOrExpContext _localctx = new VarOrExpContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_varOrExp);
		try {
			setState(282);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,19,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(277);
				var();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(278);
				match(T__26);
				setState(279);
				exp(0);
				setState(280);
				match(T__27);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class VarContext extends ParserRuleContext {
		public TerminalNode NAME() { return getToken(LuaParser.NAME, 0); }
		public ExpContext exp() {
			return getRuleContext(ExpContext.class,0);
		}
		public List<VarSuffixContext> varSuffix() {
			return getRuleContexts(VarSuffixContext.class);
		}
		public VarSuffixContext varSuffix(int i) {
			return getRuleContext(VarSuffixContext.class,i);
		}
		public VarContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_var; }
	}

	public final VarContext var() throws RecognitionException {
		VarContext _localctx = new VarContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_var);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(290);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case NAME:
				{
				setState(284);
				match(NAME);
				}
				break;
			case T__26:
				{
				setState(285);
				match(T__26);
				setState(286);
				exp(0);
				setState(287);
				match(T__27);
				setState(288);
				varSuffix();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(295);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,21,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(292);
					varSuffix();
					}
					} 
				}
				setState(297);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,21,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class VarSuffixContext extends ParserRuleContext {
		public ExpContext exp() {
			return getRuleContext(ExpContext.class,0);
		}
		public TerminalNode NAME() { return getToken(LuaParser.NAME, 0); }
		public List<NameAndArgsContext> nameAndArgs() {
			return getRuleContexts(NameAndArgsContext.class);
		}
		public NameAndArgsContext nameAndArgs(int i) {
			return getRuleContext(NameAndArgsContext.class,i);
		}
		public VarSuffixContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_varSuffix; }
	}

	public final VarSuffixContext varSuffix() throws RecognitionException {
		VarSuffixContext _localctx = new VarSuffixContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_varSuffix);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(301);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__21) | (1L << T__26) | (1L << T__30) | (1L << NORMALSTRING) | (1L << CHARSTRING) | (1L << LONGSTRING))) != 0)) {
				{
				{
				setState(298);
				nameAndArgs();
				}
				}
				setState(303);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(310);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__28:
				{
				setState(304);
				match(T__28);
				setState(305);
				exp(0);
				setState(306);
				match(T__29);
				}
				break;
			case T__20:
				{
				setState(308);
				match(T__20);
				setState(309);
				match(NAME);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NameAndArgsContext extends ParserRuleContext {
		public ArgsContext args() {
			return getRuleContext(ArgsContext.class,0);
		}
		public TerminalNode NAME() { return getToken(LuaParser.NAME, 0); }
		public NameAndArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nameAndArgs; }
	}

	public final NameAndArgsContext nameAndArgs() throws RecognitionException {
		NameAndArgsContext _localctx = new NameAndArgsContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_nameAndArgs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(314);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__21) {
				{
				setState(312);
				match(T__21);
				setState(313);
				match(NAME);
				}
			}

			setState(316);
			args();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ArgsContext extends ParserRuleContext {
		public ExplistContext explist() {
			return getRuleContext(ExplistContext.class,0);
		}
		public TableconstructorContext tableconstructor() {
			return getRuleContext(TableconstructorContext.class,0);
		}
		public StringContext string() {
			return getRuleContext(StringContext.class,0);
		}
		public ArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_args; }
	}

	public final ArgsContext args() throws RecognitionException {
		ArgsContext _localctx = new ArgsContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_args);
		int _la;
		try {
			setState(325);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__26:
				enterOuterAlt(_localctx, 1);
				{
				setState(318);
				match(T__26);
				setState(320);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__16) | (1L << T__22) | (1L << T__23) | (1L << T__24) | (1L << T__25) | (1L << T__26) | (1L << T__30) | (1L << T__42) | (1L << T__49) | (1L << T__52) | (1L << T__53) | (1L << NAME) | (1L << NORMALSTRING) | (1L << CHARSTRING) | (1L << LONGSTRING) | (1L << INT) | (1L << HEX) | (1L << FLOAT) | (1L << HEX_FLOAT))) != 0)) {
					{
					setState(319);
					explist();
					}
				}

				setState(322);
				match(T__27);
				}
				break;
			case T__30:
				enterOuterAlt(_localctx, 2);
				{
				setState(323);
				tableconstructor();
				}
				break;
			case NORMALSTRING:
			case CHARSTRING:
			case LONGSTRING:
				enterOuterAlt(_localctx, 3);
				{
				setState(324);
				string();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FunctiondefContext extends ParserRuleContext {
		public FuncbodyContext funcbody() {
			return getRuleContext(FuncbodyContext.class,0);
		}
		public FunctiondefContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functiondef; }
	}

	public final FunctiondefContext functiondef() throws RecognitionException {
		FunctiondefContext _localctx = new FunctiondefContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_functiondef);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(327);
			match(T__16);
			setState(328);
			funcbody();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FuncbodyContext extends ParserRuleContext {
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public ParlistContext parlist() {
			return getRuleContext(ParlistContext.class,0);
		}
		public FuncbodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_funcbody; }
	}

	public final FuncbodyContext funcbody() throws RecognitionException {
		FuncbodyContext _localctx = new FuncbodyContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_funcbody);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(330);
			match(T__26);
			setState(332);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__25 || _la==NAME) {
				{
				setState(331);
				parlist();
				}
			}

			setState(334);
			match(T__27);
			setState(335);
			block();
			setState(336);
			match(T__5);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ParlistContext extends ParserRuleContext {
		public NamelistContext namelist() {
			return getRuleContext(NamelistContext.class,0);
		}
		public ParlistContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_parlist; }
	}

	public final ParlistContext parlist() throws RecognitionException {
		ParlistContext _localctx = new ParlistContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_parlist);
		int _la;
		try {
			setState(344);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case NAME:
				enterOuterAlt(_localctx, 1);
				{
				setState(338);
				namelist();
				setState(341);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__14) {
					{
					setState(339);
					match(T__14);
					setState(340);
					match(T__25);
					}
				}

				}
				break;
			case T__25:
				enterOuterAlt(_localctx, 2);
				{
				setState(343);
				match(T__25);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TableconstructorContext extends ParserRuleContext {
		public FieldlistContext fieldlist() {
			return getRuleContext(FieldlistContext.class,0);
		}
		public TableconstructorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tableconstructor; }
	}

	public final TableconstructorContext tableconstructor() throws RecognitionException {
		TableconstructorContext _localctx = new TableconstructorContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_tableconstructor);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(346);
			match(T__30);
			setState(348);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__16) | (1L << T__22) | (1L << T__23) | (1L << T__24) | (1L << T__25) | (1L << T__26) | (1L << T__28) | (1L << T__30) | (1L << T__42) | (1L << T__49) | (1L << T__52) | (1L << T__53) | (1L << NAME) | (1L << NORMALSTRING) | (1L << CHARSTRING) | (1L << LONGSTRING) | (1L << INT) | (1L << HEX) | (1L << FLOAT) | (1L << HEX_FLOAT))) != 0)) {
				{
				setState(347);
				fieldlist();
				}
			}

			setState(350);
			match(T__31);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FieldlistContext extends ParserRuleContext {
		public List<FieldContext> field() {
			return getRuleContexts(FieldContext.class);
		}
		public FieldContext field(int i) {
			return getRuleContext(FieldContext.class,i);
		}
		public List<FieldsepContext> fieldsep() {
			return getRuleContexts(FieldsepContext.class);
		}
		public FieldsepContext fieldsep(int i) {
			return getRuleContext(FieldsepContext.class,i);
		}
		public FieldlistContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fieldlist; }
	}

	public final FieldlistContext fieldlist() throws RecognitionException {
		FieldlistContext _localctx = new FieldlistContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_fieldlist);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(352);
			field();
			setState(358);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,31,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(353);
					fieldsep();
					setState(354);
					field();
					}
					} 
				}
				setState(360);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,31,_ctx);
			}
			setState(362);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__0 || _la==T__14) {
				{
				setState(361);
				fieldsep();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FieldContext extends ParserRuleContext {
		public List<ExpContext> exp() {
			return getRuleContexts(ExpContext.class);
		}
		public ExpContext exp(int i) {
			return getRuleContext(ExpContext.class,i);
		}
		public TerminalNode NAME() { return getToken(LuaParser.NAME, 0); }
		public FieldContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_field; }
	}

	public final FieldContext field() throws RecognitionException {
		FieldContext _localctx = new FieldContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_field);
		try {
			setState(374);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,33,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(364);
				match(T__28);
				setState(365);
				exp(0);
				setState(366);
				match(T__29);
				setState(367);
				match(T__1);
				setState(368);
				exp(0);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(370);
				match(NAME);
				setState(371);
				match(T__1);
				setState(372);
				exp(0);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(373);
				exp(0);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FieldsepContext extends ParserRuleContext {
		public FieldsepContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fieldsep; }
	}

	public final FieldsepContext fieldsep() throws RecognitionException {
		FieldsepContext _localctx = new FieldsepContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_fieldsep);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(376);
			_la = _input.LA(1);
			if ( !(_la==T__0 || _la==T__14) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class OperatorOrContext extends ParserRuleContext {
		public OperatorOrContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_operatorOr; }
	}

	public final OperatorOrContext operatorOr() throws RecognitionException {
		OperatorOrContext _localctx = new OperatorOrContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_operatorOr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(378);
			match(T__32);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class OperatorAndContext extends ParserRuleContext {
		public OperatorAndContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_operatorAnd; }
	}

	public final OperatorAndContext operatorAnd() throws RecognitionException {
		OperatorAndContext _localctx = new OperatorAndContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_operatorAnd);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(380);
			match(T__33);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class OperatorComparisonContext extends ParserRuleContext {
		public OperatorComparisonContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_operatorComparison; }
	}

	public final OperatorComparisonContext operatorComparison() throws RecognitionException {
		OperatorComparisonContext _localctx = new OperatorComparisonContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_operatorComparison);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(382);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__34) | (1L << T__35) | (1L << T__36) | (1L << T__37) | (1L << T__38) | (1L << T__39))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class OperatorStrcatContext extends ParserRuleContext {
		public OperatorStrcatContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_operatorStrcat; }
	}

	public final OperatorStrcatContext operatorStrcat() throws RecognitionException {
		OperatorStrcatContext _localctx = new OperatorStrcatContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_operatorStrcat);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(384);
			match(T__40);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class OperatorAddSubContext extends ParserRuleContext {
		public OperatorAddSubContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_operatorAddSub; }
	}

	public final OperatorAddSubContext operatorAddSub() throws RecognitionException {
		OperatorAddSubContext _localctx = new OperatorAddSubContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_operatorAddSub);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(386);
			_la = _input.LA(1);
			if ( !(_la==T__41 || _la==T__42) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class OperatorMulDivModContext extends ParserRuleContext {
		public OperatorMulDivModContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_operatorMulDivMod; }
	}

	public final OperatorMulDivModContext operatorMulDivMod() throws RecognitionException {
		OperatorMulDivModContext _localctx = new OperatorMulDivModContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_operatorMulDivMod);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(388);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__43) | (1L << T__44) | (1L << T__45) | (1L << T__46))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class OperatorBitwiseContext extends ParserRuleContext {
		public OperatorBitwiseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_operatorBitwise; }
	}

	public final OperatorBitwiseContext operatorBitwise() throws RecognitionException {
		OperatorBitwiseContext _localctx = new OperatorBitwiseContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_operatorBitwise);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(390);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__47) | (1L << T__48) | (1L << T__49) | (1L << T__50) | (1L << T__51))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class OperatorUnaryContext extends ParserRuleContext {
		public OperatorUnaryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_operatorUnary; }
	}

	public final OperatorUnaryContext operatorUnary() throws RecognitionException {
		OperatorUnaryContext _localctx = new OperatorUnaryContext(_ctx, getState());
		enterRule(_localctx, 62, RULE_operatorUnary);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(392);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__42) | (1L << T__49) | (1L << T__52) | (1L << T__53))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class OperatorPowerContext extends ParserRuleContext {
		public OperatorPowerContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_operatorPower; }
	}

	public final OperatorPowerContext operatorPower() throws RecognitionException {
		OperatorPowerContext _localctx = new OperatorPowerContext(_ctx, getState());
		enterRule(_localctx, 64, RULE_operatorPower);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(394);
			match(T__54);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NumberContext extends ParserRuleContext {
		public TerminalNode INT() { return getToken(LuaParser.INT, 0); }
		public TerminalNode HEX() { return getToken(LuaParser.HEX, 0); }
		public TerminalNode FLOAT() { return getToken(LuaParser.FLOAT, 0); }
		public TerminalNode HEX_FLOAT() { return getToken(LuaParser.HEX_FLOAT, 0); }
		public NumberContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_number; }
	}

	public final NumberContext number() throws RecognitionException {
		NumberContext _localctx = new NumberContext(_ctx, getState());
		enterRule(_localctx, 66, RULE_number);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(396);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << INT) | (1L << HEX) | (1L << FLOAT) | (1L << HEX_FLOAT))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class StringContext extends ParserRuleContext {
		public TerminalNode NORMALSTRING() { return getToken(LuaParser.NORMALSTRING, 0); }
		public TerminalNode CHARSTRING() { return getToken(LuaParser.CHARSTRING, 0); }
		public TerminalNode LONGSTRING() { return getToken(LuaParser.LONGSTRING, 0); }
		public StringContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_string; }
	}

	public final StringContext string() throws RecognitionException {
		StringContext _localctx = new StringContext(_ctx, getState());
		enterRule(_localctx, 68, RULE_string);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(398);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << NORMALSTRING) | (1L << CHARSTRING) | (1L << LONGSTRING))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 9:
			return exp_sempred((ExpContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean exp_sempred(ExpContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 9);
		case 1:
			return precpred(_ctx, 7);
		case 2:
			return precpred(_ctx, 6);
		case 3:
			return precpred(_ctx, 5);
		case 4:
			return precpred(_ctx, 4);
		case 5:
			return precpred(_ctx, 3);
		case 6:
			return precpred(_ctx, 2);
		case 7:
			return precpred(_ctx, 1);
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3E\u0193\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\3\2\3\2\3\2\3\3\7\3M\n\3\f\3\16\3P\13\3\3\3\5"+
		"\3S\n\3\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3"+
		"\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4"+
		"\3\4\7\4w\n\4\f\4\16\4z\13\4\3\4\3\4\5\4~\n\4\3\4\3\4\3\4\3\4\3\4\3\4"+
		"\3\4\3\4\3\4\3\4\5\4\u008a\n\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4"+
		"\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\5\4\u00a4\n\4"+
		"\5\4\u00a6\n\4\3\5\3\5\5\5\u00aa\n\5\3\5\5\5\u00ad\n\5\3\6\3\6\3\6\3\6"+
		"\3\7\3\7\3\7\7\7\u00b6\n\7\f\7\16\7\u00b9\13\7\3\7\3\7\5\7\u00bd\n\7\3"+
		"\b\3\b\3\b\7\b\u00c2\n\b\f\b\16\b\u00c5\13\b\3\t\3\t\3\t\7\t\u00ca\n\t"+
		"\f\t\16\t\u00cd\13\t\3\n\3\n\3\n\7\n\u00d2\n\n\f\n\16\n\u00d5\13\n\3\13"+
		"\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\5\13\u00e4"+
		"\n\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13"+
		"\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13"+
		"\3\13\3\13\3\13\3\13\3\13\7\13\u0106\n\13\f\13\16\13\u0109\13\13\3\f\3"+
		"\f\7\f\u010d\n\f\f\f\16\f\u0110\13\f\3\r\3\r\6\r\u0114\n\r\r\r\16\r\u0115"+
		"\3\16\3\16\3\16\3\16\3\16\5\16\u011d\n\16\3\17\3\17\3\17\3\17\3\17\3\17"+
		"\5\17\u0125\n\17\3\17\7\17\u0128\n\17\f\17\16\17\u012b\13\17\3\20\7\20"+
		"\u012e\n\20\f\20\16\20\u0131\13\20\3\20\3\20\3\20\3\20\3\20\3\20\5\20"+
		"\u0139\n\20\3\21\3\21\5\21\u013d\n\21\3\21\3\21\3\22\3\22\5\22\u0143\n"+
		"\22\3\22\3\22\3\22\5\22\u0148\n\22\3\23\3\23\3\23\3\24\3\24\5\24\u014f"+
		"\n\24\3\24\3\24\3\24\3\24\3\25\3\25\3\25\5\25\u0158\n\25\3\25\5\25\u015b"+
		"\n\25\3\26\3\26\5\26\u015f\n\26\3\26\3\26\3\27\3\27\3\27\3\27\7\27\u0167"+
		"\n\27\f\27\16\27\u016a\13\27\3\27\5\27\u016d\n\27\3\30\3\30\3\30\3\30"+
		"\3\30\3\30\3\30\3\30\3\30\3\30\5\30\u0179\n\30\3\31\3\31\3\32\3\32\3\33"+
		"\3\33\3\34\3\34\3\35\3\35\3\36\3\36\3\37\3\37\3 \3 \3!\3!\3\"\3\"\3#\3"+
		"#\3$\3$\3$\2\3\24%\2\4\6\b\n\f\16\20\22\24\26\30\32\34\36 \"$&(*,.\60"+
		"\62\64\668:<>@BDF\2\n\4\2\3\3\21\21\3\2%*\3\2,-\3\2.\61\3\2\62\66\5\2"+
		"--\64\64\678\3\2>A\3\2;=\2\u01ae\2H\3\2\2\2\4N\3\2\2\2\6\u00a5\3\2\2\2"+
		"\b\u00a7\3\2\2\2\n\u00ae\3\2\2\2\f\u00b2\3\2\2\2\16\u00be\3\2\2\2\20\u00c6"+
		"\3\2\2\2\22\u00ce\3\2\2\2\24\u00e3\3\2\2\2\26\u010a\3\2\2\2\30\u0111\3"+
		"\2\2\2\32\u011c\3\2\2\2\34\u0124\3\2\2\2\36\u012f\3\2\2\2 \u013c\3\2\2"+
		"\2\"\u0147\3\2\2\2$\u0149\3\2\2\2&\u014c\3\2\2\2(\u015a\3\2\2\2*\u015c"+
		"\3\2\2\2,\u0162\3\2\2\2.\u0178\3\2\2\2\60\u017a\3\2\2\2\62\u017c\3\2\2"+
		"\2\64\u017e\3\2\2\2\66\u0180\3\2\2\28\u0182\3\2\2\2:\u0184\3\2\2\2<\u0186"+
		"\3\2\2\2>\u0188\3\2\2\2@\u018a\3\2\2\2B\u018c\3\2\2\2D\u018e\3\2\2\2F"+
		"\u0190\3\2\2\2HI\5\4\3\2IJ\7\2\2\3J\3\3\2\2\2KM\5\6\4\2LK\3\2\2\2MP\3"+
		"\2\2\2NL\3\2\2\2NO\3\2\2\2OR\3\2\2\2PN\3\2\2\2QS\5\b\5\2RQ\3\2\2\2RS\3"+
		"\2\2\2S\5\3\2\2\2T\u00a6\7\3\2\2UV\5\16\b\2VW\7\4\2\2WX\5\22\n\2X\u00a6"+
		"\3\2\2\2Y\u00a6\5\30\r\2Z\u00a6\5\n\6\2[\u00a6\7\5\2\2\\]\7\6\2\2]\u00a6"+
		"\7:\2\2^_\7\7\2\2_`\5\4\3\2`a\7\b\2\2a\u00a6\3\2\2\2bc\7\t\2\2cd\5\24"+
		"\13\2de\7\7\2\2ef\5\4\3\2fg\7\b\2\2g\u00a6\3\2\2\2hi\7\n\2\2ij\5\4\3\2"+
		"jk\7\13\2\2kl\5\24\13\2l\u00a6\3\2\2\2mn\7\f\2\2no\5\24\13\2op\7\r\2\2"+
		"px\5\4\3\2qr\7\16\2\2rs\5\24\13\2st\7\r\2\2tu\5\4\3\2uw\3\2\2\2vq\3\2"+
		"\2\2wz\3\2\2\2xv\3\2\2\2xy\3\2\2\2y}\3\2\2\2zx\3\2\2\2{|\7\17\2\2|~\5"+
		"\4\3\2}{\3\2\2\2}~\3\2\2\2~\177\3\2\2\2\177\u0080\7\b\2\2\u0080\u00a6"+
		"\3\2\2\2\u0081\u0082\7\20\2\2\u0082\u0083\7:\2\2\u0083\u0084\7\4\2\2\u0084"+
		"\u0085\5\24\13\2\u0085\u0086\7\21\2\2\u0086\u0089\5\24\13\2\u0087\u0088"+
		"\7\21\2\2\u0088\u008a\5\24\13\2\u0089\u0087\3\2\2\2\u0089\u008a\3\2\2"+
		"\2\u008a\u008b\3\2\2\2\u008b\u008c\7\7\2\2\u008c\u008d\5\4\3\2\u008d\u008e"+
		"\7\b\2\2\u008e\u00a6\3\2\2\2\u008f\u0090\7\20\2\2\u0090\u0091\5\20\t\2"+
		"\u0091\u0092\7\22\2\2\u0092\u0093\5\22\n\2\u0093\u0094\7\7\2\2\u0094\u0095"+
		"\5\4\3\2\u0095\u0096\7\b\2\2\u0096\u00a6\3\2\2\2\u0097\u0098\7\23\2\2"+
		"\u0098\u0099\5\f\7\2\u0099\u009a\5&\24\2\u009a\u00a6\3\2\2\2\u009b\u009c"+
		"\7\24\2\2\u009c\u009d\7\23\2\2\u009d\u009e\7:\2\2\u009e\u00a6\5&\24\2"+
		"\u009f\u00a0\7\24\2\2\u00a0\u00a3\5\20\t\2\u00a1\u00a2\7\4\2\2\u00a2\u00a4"+
		"\5\22\n\2\u00a3\u00a1\3\2\2\2\u00a3\u00a4\3\2\2\2\u00a4\u00a6\3\2\2\2"+
		"\u00a5T\3\2\2\2\u00a5U\3\2\2\2\u00a5Y\3\2\2\2\u00a5Z\3\2\2\2\u00a5[\3"+
		"\2\2\2\u00a5\\\3\2\2\2\u00a5^\3\2\2\2\u00a5b\3\2\2\2\u00a5h\3\2\2\2\u00a5"+
		"m\3\2\2\2\u00a5\u0081\3\2\2\2\u00a5\u008f\3\2\2\2\u00a5\u0097\3\2\2\2"+
		"\u00a5\u009b\3\2\2\2\u00a5\u009f\3\2\2\2\u00a6\7\3\2\2\2\u00a7\u00a9\7"+
		"\25\2\2\u00a8\u00aa\5\22\n\2\u00a9\u00a8\3\2\2\2\u00a9\u00aa\3\2\2\2\u00aa"+
		"\u00ac\3\2\2\2\u00ab\u00ad\7\3\2\2\u00ac\u00ab\3\2\2\2\u00ac\u00ad\3\2"+
		"\2\2\u00ad\t\3\2\2\2\u00ae\u00af\7\26\2\2\u00af\u00b0\7:\2\2\u00b0\u00b1"+
		"\7\26\2\2\u00b1\13\3\2\2\2\u00b2\u00b7\7:\2\2\u00b3\u00b4\7\27\2\2\u00b4"+
		"\u00b6\7:\2\2\u00b5\u00b3\3\2\2\2\u00b6\u00b9\3\2\2\2\u00b7\u00b5\3\2"+
		"\2\2\u00b7\u00b8\3\2\2\2\u00b8\u00bc\3\2\2\2\u00b9\u00b7\3\2\2\2\u00ba"+
		"\u00bb\7\30\2\2\u00bb\u00bd\7:\2\2\u00bc\u00ba\3\2\2\2\u00bc\u00bd\3\2"+
		"\2\2\u00bd\r\3\2\2\2\u00be\u00c3\5\34\17\2\u00bf\u00c0\7\21\2\2\u00c0"+
		"\u00c2\5\34\17\2\u00c1\u00bf\3\2\2\2\u00c2\u00c5\3\2\2\2\u00c3\u00c1\3"+
		"\2\2\2\u00c3\u00c4\3\2\2\2\u00c4\17\3\2\2\2\u00c5\u00c3\3\2\2\2\u00c6"+
		"\u00cb\7:\2\2\u00c7\u00c8\7\21\2\2\u00c8\u00ca\7:\2\2\u00c9\u00c7\3\2"+
		"\2\2\u00ca\u00cd\3\2\2\2\u00cb\u00c9\3\2\2\2\u00cb\u00cc\3\2\2\2\u00cc"+
		"\21\3\2\2\2\u00cd\u00cb\3\2\2\2\u00ce\u00d3\5\24\13\2\u00cf\u00d0\7\21"+
		"\2\2\u00d0\u00d2\5\24\13\2\u00d1\u00cf\3\2\2\2\u00d2\u00d5\3\2\2\2\u00d3"+
		"\u00d1\3\2\2\2\u00d3\u00d4\3\2\2\2\u00d4\23\3\2\2\2\u00d5\u00d3\3\2\2"+
		"\2\u00d6\u00d7\b\13\1\2\u00d7\u00e4\7\31\2\2\u00d8\u00e4\7\32\2\2\u00d9"+
		"\u00e4\7\33\2\2\u00da\u00e4\5D#\2\u00db\u00e4\5F$\2\u00dc\u00e4\7\34\2"+
		"\2\u00dd\u00e4\5$\23\2\u00de\u00e4\5\26\f\2\u00df\u00e4\5*\26\2\u00e0"+
		"\u00e1\5@!\2\u00e1\u00e2\5\24\13\n\u00e2\u00e4\3\2\2\2\u00e3\u00d6\3\2"+
		"\2\2\u00e3\u00d8\3\2\2\2\u00e3\u00d9\3\2\2\2\u00e3\u00da\3\2\2\2\u00e3"+
		"\u00db\3\2\2\2\u00e3\u00dc\3\2\2\2\u00e3\u00dd\3\2\2\2\u00e3\u00de\3\2"+
		"\2\2\u00e3\u00df\3\2\2\2\u00e3\u00e0\3\2\2\2\u00e4\u0107\3\2\2\2\u00e5"+
		"\u00e6\f\13\2\2\u00e6\u00e7\5B\"\2\u00e7\u00e8\5\24\13\13\u00e8\u0106"+
		"\3\2\2\2\u00e9\u00ea\f\t\2\2\u00ea\u00eb\5<\37\2\u00eb\u00ec\5\24\13\n"+
		"\u00ec\u0106\3\2\2\2\u00ed\u00ee\f\b\2\2\u00ee\u00ef\5:\36\2\u00ef\u00f0"+
		"\5\24\13\t\u00f0\u0106\3\2\2\2\u00f1\u00f2\f\7\2\2\u00f2\u00f3\58\35\2"+
		"\u00f3\u00f4\5\24\13\7\u00f4\u0106\3\2\2\2\u00f5\u00f6\f\6\2\2\u00f6\u00f7"+
		"\5\66\34\2\u00f7\u00f8\5\24\13\7\u00f8\u0106\3\2\2\2\u00f9\u00fa\f\5\2"+
		"\2\u00fa\u00fb\5\64\33\2\u00fb\u00fc\5\24\13\6\u00fc\u0106\3\2\2\2\u00fd"+
		"\u00fe\f\4\2\2\u00fe\u00ff\5\62\32\2\u00ff\u0100\5\24\13\5\u0100\u0106"+
		"\3\2\2\2\u0101\u0102\f\3\2\2\u0102\u0103\5> \2\u0103\u0104\5\24\13\4\u0104"+
		"\u0106\3\2\2\2\u0105\u00e5\3\2\2\2\u0105\u00e9\3\2\2\2\u0105\u00ed\3\2"+
		"\2\2\u0105\u00f1\3\2\2\2\u0105\u00f5\3\2\2\2\u0105\u00f9\3\2\2\2\u0105"+
		"\u00fd\3\2\2\2\u0105\u0101\3\2\2\2\u0106\u0109\3\2\2\2\u0107\u0105\3\2"+
		"\2\2\u0107\u0108\3\2\2\2\u0108\25\3\2\2\2\u0109\u0107\3\2\2\2\u010a\u010e"+
		"\5\32\16\2\u010b\u010d\5 \21\2\u010c\u010b\3\2\2\2\u010d\u0110\3\2\2\2"+
		"\u010e\u010c\3\2\2\2\u010e\u010f\3\2\2\2\u010f\27\3\2\2\2\u0110\u010e"+
		"\3\2\2\2\u0111\u0113\5\32\16\2\u0112\u0114\5 \21\2\u0113\u0112\3\2\2\2"+
		"\u0114\u0115\3\2\2\2\u0115\u0113\3\2\2\2\u0115\u0116\3\2\2\2\u0116\31"+
		"\3\2\2\2\u0117\u011d\5\34\17\2\u0118\u0119\7\35\2\2\u0119\u011a\5\24\13"+
		"\2\u011a\u011b\7\36\2\2\u011b\u011d\3\2\2\2\u011c\u0117\3\2\2\2\u011c"+
		"\u0118\3\2\2\2\u011d\33\3\2\2\2\u011e\u0125\7:\2\2\u011f\u0120\7\35\2"+
		"\2\u0120\u0121\5\24\13\2\u0121\u0122\7\36\2\2\u0122\u0123\5\36\20\2\u0123"+
		"\u0125\3\2\2\2\u0124\u011e\3\2\2\2\u0124\u011f\3\2\2\2\u0125\u0129\3\2"+
		"\2\2\u0126\u0128\5\36\20\2\u0127\u0126\3\2\2\2\u0128\u012b\3\2\2\2\u0129"+
		"\u0127\3\2\2\2\u0129\u012a\3\2\2\2\u012a\35\3\2\2\2\u012b\u0129\3\2\2"+
		"\2\u012c\u012e\5 \21\2\u012d\u012c\3\2\2\2\u012e\u0131\3\2\2\2\u012f\u012d"+
		"\3\2\2\2\u012f\u0130\3\2\2\2\u0130\u0138\3\2\2\2\u0131\u012f\3\2\2\2\u0132"+
		"\u0133\7\37\2\2\u0133\u0134\5\24\13\2\u0134\u0135\7 \2\2\u0135\u0139\3"+
		"\2\2\2\u0136\u0137\7\27\2\2\u0137\u0139\7:\2\2\u0138\u0132\3\2\2\2\u0138"+
		"\u0136\3\2\2\2\u0139\37\3\2\2\2\u013a\u013b\7\30\2\2\u013b\u013d\7:\2"+
		"\2\u013c\u013a\3\2\2\2\u013c\u013d\3\2\2\2\u013d\u013e\3\2\2\2\u013e\u013f"+
		"\5\"\22\2\u013f!\3\2\2\2\u0140\u0142\7\35\2\2\u0141\u0143\5\22\n\2\u0142"+
		"\u0141\3\2\2\2\u0142\u0143\3\2\2\2\u0143\u0144\3\2\2\2\u0144\u0148\7\36"+
		"\2\2\u0145\u0148\5*\26\2\u0146\u0148\5F$\2\u0147\u0140\3\2\2\2\u0147\u0145"+
		"\3\2\2\2\u0147\u0146\3\2\2\2\u0148#\3\2\2\2\u0149\u014a\7\23\2\2\u014a"+
		"\u014b\5&\24\2\u014b%\3\2\2\2\u014c\u014e\7\35\2\2\u014d\u014f\5(\25\2"+
		"\u014e\u014d\3\2\2\2\u014e\u014f\3\2\2\2\u014f\u0150\3\2\2\2\u0150\u0151"+
		"\7\36\2\2\u0151\u0152\5\4\3\2\u0152\u0153\7\b\2\2\u0153\'\3\2\2\2\u0154"+
		"\u0157\5\20\t\2\u0155\u0156\7\21\2\2\u0156\u0158\7\34\2\2\u0157\u0155"+
		"\3\2\2\2\u0157\u0158\3\2\2\2\u0158\u015b\3\2\2\2\u0159\u015b\7\34\2\2"+
		"\u015a\u0154\3\2\2\2\u015a\u0159\3\2\2\2\u015b)\3\2\2\2\u015c\u015e\7"+
		"!\2\2\u015d\u015f\5,\27\2\u015e\u015d\3\2\2\2\u015e\u015f\3\2\2\2\u015f"+
		"\u0160\3\2\2\2\u0160\u0161\7\"\2\2\u0161+\3\2\2\2\u0162\u0168\5.\30\2"+
		"\u0163\u0164\5\60\31\2\u0164\u0165\5.\30\2\u0165\u0167\3\2\2\2\u0166\u0163"+
		"\3\2\2\2\u0167\u016a\3\2\2\2\u0168\u0166\3\2\2\2\u0168\u0169\3\2\2\2\u0169"+
		"\u016c\3\2\2\2\u016a\u0168\3\2\2\2\u016b\u016d\5\60\31\2\u016c\u016b\3"+
		"\2\2\2\u016c\u016d\3\2\2\2\u016d-\3\2\2\2\u016e\u016f\7\37\2\2\u016f\u0170"+
		"\5\24\13\2\u0170\u0171\7 \2\2\u0171\u0172\7\4\2\2\u0172\u0173\5\24\13"+
		"\2\u0173\u0179\3\2\2\2\u0174\u0175\7:\2\2\u0175\u0176\7\4\2\2\u0176\u0179"+
		"\5\24\13\2\u0177\u0179\5\24\13\2\u0178\u016e\3\2\2\2\u0178\u0174\3\2\2"+
		"\2\u0178\u0177\3\2\2\2\u0179/\3\2\2\2\u017a\u017b\t\2\2\2\u017b\61\3\2"+
		"\2\2\u017c\u017d\7#\2\2\u017d\63\3\2\2\2\u017e\u017f\7$\2\2\u017f\65\3"+
		"\2\2\2\u0180\u0181\t\3\2\2\u0181\67\3\2\2\2\u0182\u0183\7+\2\2\u01839"+
		"\3\2\2\2\u0184\u0185\t\4\2\2\u0185;\3\2\2\2\u0186\u0187\t\5\2\2\u0187"+
		"=\3\2\2\2\u0188\u0189\t\6\2\2\u0189?\3\2\2\2\u018a\u018b\t\7\2\2\u018b"+
		"A\3\2\2\2\u018c\u018d\79\2\2\u018dC\3\2\2\2\u018e\u018f\t\b\2\2\u018f"+
		"E\3\2\2\2\u0190\u0191\t\t\2\2\u0191G\3\2\2\2$NRx}\u0089\u00a3\u00a5\u00a9"+
		"\u00ac\u00b7\u00bc\u00c3\u00cb\u00d3\u00e3\u0105\u0107\u010e\u0115\u011c"+
		"\u0124\u0129\u012f\u0138\u013c\u0142\u0147\u014e\u0157\u015a\u015e\u0168"+
		"\u016c\u0178";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}