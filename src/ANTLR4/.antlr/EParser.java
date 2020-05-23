// Generated from /Volumes/MSD64G/Developer/CodeLanuage/C++/CompileDragonBook.Cpp/src/ANTLR4/E.g4 by ANTLR 4.7.1
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class EParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.7.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, VAR=8, INT=9, 
		STRING=10, WS=11;
	public static final int
		RULE_program = 0, RULE_statement = 1, RULE_expression = 2, RULE_multExpr = 3, 
		RULE_atom = 4;
	public static final String[] ruleNames = {
		"program", "statement", "expression", "multExpr", "atom"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'='", "';'", "'+'", "'-'", "'*'", "'('", "')'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, null, null, null, null, null, "VAR", "INT", "STRING", 
		"WS"
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
	public String getGrammarFileName() { return "E.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public EParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class ProgramContext extends ParserRuleContext {
		public List<StatementContext> statement() {
			return getRuleContexts(StatementContext.class);
		}
		public StatementContext statement(int i) {
			return getRuleContext(StatementContext.class,i);
		}
		public ProgramContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_program; }
	}

	public final ProgramContext program() throws RecognitionException {
		ProgramContext _localctx = new ProgramContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_program);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(11); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(10);
				statement();
				}
				}
				setState(13); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__5) | (1L << VAR) | (1L << INT) | (1L << STRING))) != 0) );
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

	public static class StatementContext extends ParserRuleContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode VAR() { return getToken(EParser.VAR, 0); }
		public StatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_statement; }
	}

	public final StatementContext statement() throws RecognitionException {
		StatementContext _localctx = new StatementContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_statement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(19);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__5:
			case INT:
			case STRING:
				{
				setState(15);
				expression();
				}
				break;
			case VAR:
				{
				setState(16);
				match(VAR);
				setState(17);
				match(T__0);
				setState(18);
				expression();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(21);
			match(T__1);
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

	public static class ExpressionContext extends ParserRuleContext {
		public List<MultExprContext> multExpr() {
			return getRuleContexts(MultExprContext.class);
		}
		public MultExprContext multExpr(int i) {
			return getRuleContext(MultExprContext.class,i);
		}
		public TerminalNode STRING() { return getToken(EParser.STRING, 0); }
		public ExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression; }
	}

	public final ExpressionContext expression() throws RecognitionException {
		ExpressionContext _localctx = new ExpressionContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_expression);
		int _la;
		try {
			setState(32);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__5:
			case INT:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(23);
				multExpr();
				setState(28);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__2 || _la==T__3) {
					{
					{
					setState(24);
					_la = _input.LA(1);
					if ( !(_la==T__2 || _la==T__3) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(25);
					multExpr();
					}
					}
					setState(30);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				}
				break;
			case STRING:
				enterOuterAlt(_localctx, 2);
				{
				setState(31);
				match(STRING);
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

	public static class MultExprContext extends ParserRuleContext {
		public List<AtomContext> atom() {
			return getRuleContexts(AtomContext.class);
		}
		public AtomContext atom(int i) {
			return getRuleContext(AtomContext.class,i);
		}
		public MultExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_multExpr; }
	}

	public final MultExprContext multExpr() throws RecognitionException {
		MultExprContext _localctx = new MultExprContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_multExpr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(34);
			atom();
			setState(39);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__4) {
				{
				{
				setState(35);
				match(T__4);
				setState(36);
				atom();
				}
				}
				setState(41);
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

	public static class AtomContext extends ParserRuleContext {
		public TerminalNode INT() { return getToken(EParser.INT, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public AtomContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_atom; }
	}

	public final AtomContext atom() throws RecognitionException {
		AtomContext _localctx = new AtomContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_atom);
		try {
			setState(47);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case INT:
				enterOuterAlt(_localctx, 1);
				{
				setState(42);
				match(INT);
				}
				break;
			case T__5:
				enterOuterAlt(_localctx, 2);
				{
				setState(43);
				match(T__5);
				setState(44);
				expression();
				setState(45);
				match(T__6);
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

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\r\64\4\2\t\2\4\3"+
		"\t\3\4\4\t\4\4\5\t\5\4\6\t\6\3\2\6\2\16\n\2\r\2\16\2\17\3\3\3\3\3\3\3"+
		"\3\5\3\26\n\3\3\3\3\3\3\4\3\4\3\4\7\4\35\n\4\f\4\16\4 \13\4\3\4\5\4#\n"+
		"\4\3\5\3\5\3\5\7\5(\n\5\f\5\16\5+\13\5\3\6\3\6\3\6\3\6\3\6\5\6\62\n\6"+
		"\3\6\2\2\7\2\4\6\b\n\2\3\3\2\5\6\2\64\2\r\3\2\2\2\4\25\3\2\2\2\6\"\3\2"+
		"\2\2\b$\3\2\2\2\n\61\3\2\2\2\f\16\5\4\3\2\r\f\3\2\2\2\16\17\3\2\2\2\17"+
		"\r\3\2\2\2\17\20\3\2\2\2\20\3\3\2\2\2\21\26\5\6\4\2\22\23\7\n\2\2\23\24"+
		"\7\3\2\2\24\26\5\6\4\2\25\21\3\2\2\2\25\22\3\2\2\2\26\27\3\2\2\2\27\30"+
		"\7\4\2\2\30\5\3\2\2\2\31\36\5\b\5\2\32\33\t\2\2\2\33\35\5\b\5\2\34\32"+
		"\3\2\2\2\35 \3\2\2\2\36\34\3\2\2\2\36\37\3\2\2\2\37#\3\2\2\2 \36\3\2\2"+
		"\2!#\7\f\2\2\"\31\3\2\2\2\"!\3\2\2\2#\7\3\2\2\2$)\5\n\6\2%&\7\7\2\2&("+
		"\5\n\6\2\'%\3\2\2\2(+\3\2\2\2)\'\3\2\2\2)*\3\2\2\2*\t\3\2\2\2+)\3\2\2"+
		"\2,\62\7\13\2\2-.\7\b\2\2./\5\6\4\2/\60\7\t\2\2\60\62\3\2\2\2\61,\3\2"+
		"\2\2\61-\3\2\2\2\62\13\3\2\2\2\b\17\25\36\")\61";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}