// Generated from /home/vepo/source/maestro/stream-lang/src/main/antlr4/dev/vepo/maestro/lang/Stream.g4 by ANTLR 4.13.1
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class StreamParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		FROM=1, TO=2, WHERE=3, UNIQUE=4, BY=5, AND=6, OR=7, NOT=8, IN=9, BETWEEN=10, 
		IS=11, NULL=12, LIKE=13, EQ=14, NEQ=15, LT=16, LTE=17, GT=18, GTE=19, 
		LPAREN=20, RPAREN=21, COMMA=22, BOOLEAN=23, STRING=24, NUMBER=25, IDENTIFIER=26, 
		WS=27;
	public static final int
		RULE_streamQueries = 0, RULE_query = 1, RULE_from = 2, RULE_where = 3, 
		RULE_unique = 4, RULE_to = 5, RULE_sourceTopics = 6, RULE_sinkTopics = 7, 
		RULE_fieldList = 8, RULE_topicName = 9, RULE_expression = 10, RULE_predicate = 11, 
		RULE_comparator = 12, RULE_value = 13, RULE_valueList = 14, RULE_literal = 15;
	private static String[] makeRuleNames() {
		return new String[] {
			"streamQueries", "query", "from", "where", "unique", "to", "sourceTopics", 
			"sinkTopics", "fieldList", "topicName", "expression", "predicate", "comparator", 
			"value", "valueList", "literal"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, "'='", null, "'<'", "'<='", "'>'", "'>='", "'('", "')'", 
			"','"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "FROM", "TO", "WHERE", "UNIQUE", "BY", "AND", "OR", "NOT", "IN", 
			"BETWEEN", "IS", "NULL", "LIKE", "EQ", "NEQ", "LT", "LTE", "GT", "GTE", 
			"LPAREN", "RPAREN", "COMMA", "BOOLEAN", "STRING", "NUMBER", "IDENTIFIER", 
			"WS"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
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
	public String getGrammarFileName() { return "Stream.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public StreamParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StreamQueriesContext extends ParserRuleContext {
		public TerminalNode EOF() { return getToken(StreamParser.EOF, 0); }
		public List<QueryContext> query() {
			return getRuleContexts(QueryContext.class);
		}
		public QueryContext query(int i) {
			return getRuleContext(QueryContext.class,i);
		}
		public StreamQueriesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_streamQueries; }
	}

	public final StreamQueriesContext streamQueries() throws RecognitionException {
		StreamQueriesContext _localctx = new StreamQueriesContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_streamQueries);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(33); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(32);
				query();
				}
				}
				setState(35); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==FROM );
			setState(37);
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

	@SuppressWarnings("CheckReturnValue")
	public static class QueryContext extends ParserRuleContext {
		public FromContext from() {
			return getRuleContext(FromContext.class,0);
		}
		public ToContext to() {
			return getRuleContext(ToContext.class,0);
		}
		public QueryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_query; }
	}

	public final QueryContext query() throws RecognitionException {
		QueryContext _localctx = new QueryContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_query);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(39);
			from();
			setState(40);
			to();
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

	@SuppressWarnings("CheckReturnValue")
	public static class FromContext extends ParserRuleContext {
		public TerminalNode FROM() { return getToken(StreamParser.FROM, 0); }
		public SourceTopicsContext sourceTopics() {
			return getRuleContext(SourceTopicsContext.class,0);
		}
		public WhereContext where() {
			return getRuleContext(WhereContext.class,0);
		}
		public UniqueContext unique() {
			return getRuleContext(UniqueContext.class,0);
		}
		public FromContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_from; }
	}

	public final FromContext from() throws RecognitionException {
		FromContext _localctx = new FromContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_from);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(42);
			match(FROM);
			setState(43);
			sourceTopics();
			setState(50);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
			case 1:
				{
				setState(45);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==WHERE) {
					{
					setState(44);
					where();
					}
				}

				}
				break;
			case 2:
				{
				setState(48);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==UNIQUE) {
					{
					setState(47);
					unique();
					}
				}

				}
				break;
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

	@SuppressWarnings("CheckReturnValue")
	public static class WhereContext extends ParserRuleContext {
		public TerminalNode WHERE() { return getToken(StreamParser.WHERE, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public WhereContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_where; }
	}

	public final WhereContext where() throws RecognitionException {
		WhereContext _localctx = new WhereContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_where);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(52);
			match(WHERE);
			setState(53);
			expression(0);
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

	@SuppressWarnings("CheckReturnValue")
	public static class UniqueContext extends ParserRuleContext {
		public TerminalNode UNIQUE() { return getToken(StreamParser.UNIQUE, 0); }
		public TerminalNode BY() { return getToken(StreamParser.BY, 0); }
		public FieldListContext fieldList() {
			return getRuleContext(FieldListContext.class,0);
		}
		public UniqueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unique; }
	}

	public final UniqueContext unique() throws RecognitionException {
		UniqueContext _localctx = new UniqueContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_unique);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(55);
			match(UNIQUE);
			setState(56);
			match(BY);
			setState(57);
			fieldList();
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

	@SuppressWarnings("CheckReturnValue")
	public static class ToContext extends ParserRuleContext {
		public TerminalNode TO() { return getToken(StreamParser.TO, 0); }
		public SinkTopicsContext sinkTopics() {
			return getRuleContext(SinkTopicsContext.class,0);
		}
		public ToContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_to; }
	}

	public final ToContext to() throws RecognitionException {
		ToContext _localctx = new ToContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_to);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(59);
			match(TO);
			setState(60);
			sinkTopics();
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

	@SuppressWarnings("CheckReturnValue")
	public static class SourceTopicsContext extends ParserRuleContext {
		public List<TopicNameContext> topicName() {
			return getRuleContexts(TopicNameContext.class);
		}
		public TopicNameContext topicName(int i) {
			return getRuleContext(TopicNameContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(StreamParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(StreamParser.COMMA, i);
		}
		public SourceTopicsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_sourceTopics; }
	}

	public final SourceTopicsContext sourceTopics() throws RecognitionException {
		SourceTopicsContext _localctx = new SourceTopicsContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_sourceTopics);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(62);
			topicName();
			setState(67);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(63);
				match(COMMA);
				setState(64);
				topicName();
				}
				}
				setState(69);
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

	@SuppressWarnings("CheckReturnValue")
	public static class SinkTopicsContext extends ParserRuleContext {
		public List<TopicNameContext> topicName() {
			return getRuleContexts(TopicNameContext.class);
		}
		public TopicNameContext topicName(int i) {
			return getRuleContext(TopicNameContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(StreamParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(StreamParser.COMMA, i);
		}
		public SinkTopicsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_sinkTopics; }
	}

	public final SinkTopicsContext sinkTopics() throws RecognitionException {
		SinkTopicsContext _localctx = new SinkTopicsContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_sinkTopics);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(70);
			topicName();
			setState(75);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(71);
				match(COMMA);
				setState(72);
				topicName();
				}
				}
				setState(77);
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

	@SuppressWarnings("CheckReturnValue")
	public static class FieldListContext extends ParserRuleContext {
		public List<TerminalNode> IDENTIFIER() { return getTokens(StreamParser.IDENTIFIER); }
		public TerminalNode IDENTIFIER(int i) {
			return getToken(StreamParser.IDENTIFIER, i);
		}
		public List<TerminalNode> COMMA() { return getTokens(StreamParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(StreamParser.COMMA, i);
		}
		public FieldListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fieldList; }
	}

	public final FieldListContext fieldList() throws RecognitionException {
		FieldListContext _localctx = new FieldListContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_fieldList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(78);
			match(IDENTIFIER);
			setState(83);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(79);
				match(COMMA);
				setState(80);
				match(IDENTIFIER);
				}
				}
				setState(85);
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

	@SuppressWarnings("CheckReturnValue")
	public static class TopicNameContext extends ParserRuleContext {
		public TerminalNode IDENTIFIER() { return getToken(StreamParser.IDENTIFIER, 0); }
		public TopicNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_topicName; }
	}

	public final TopicNameContext topicName() throws RecognitionException {
		TopicNameContext _localctx = new TopicNameContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_topicName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(86);
			match(IDENTIFIER);
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

	@SuppressWarnings("CheckReturnValue")
	public static class ExpressionContext extends ParserRuleContext {
		public ExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression; }
	 
		public ExpressionContext() { }
		public void copyFrom(ExpressionContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class PredicateExprContext extends ExpressionContext {
		public PredicateContext predicate() {
			return getRuleContext(PredicateContext.class,0);
		}
		public PredicateExprContext(ExpressionContext ctx) { copyFrom(ctx); }
	}
	@SuppressWarnings("CheckReturnValue")
	public static class AndExpressionContext extends ExpressionContext {
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public TerminalNode AND() { return getToken(StreamParser.AND, 0); }
		public AndExpressionContext(ExpressionContext ctx) { copyFrom(ctx); }
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ParenExpressionContext extends ExpressionContext {
		public TerminalNode LPAREN() { return getToken(StreamParser.LPAREN, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(StreamParser.RPAREN, 0); }
		public ParenExpressionContext(ExpressionContext ctx) { copyFrom(ctx); }
	}
	@SuppressWarnings("CheckReturnValue")
	public static class NotExpressionContext extends ExpressionContext {
		public TerminalNode NOT() { return getToken(StreamParser.NOT, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public NotExpressionContext(ExpressionContext ctx) { copyFrom(ctx); }
	}
	@SuppressWarnings("CheckReturnValue")
	public static class OrExpressionContext extends ExpressionContext {
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public TerminalNode OR() { return getToken(StreamParser.OR, 0); }
		public OrExpressionContext(ExpressionContext ctx) { copyFrom(ctx); }
	}

	public final ExpressionContext expression() throws RecognitionException {
		return expression(0);
	}

	private ExpressionContext expression(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		ExpressionContext _localctx = new ExpressionContext(_ctx, _parentState);
		ExpressionContext _prevctx = _localctx;
		int _startState = 20;
		enterRecursionRule(_localctx, 20, RULE_expression, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(96);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LPAREN:
				{
				_localctx = new ParenExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(89);
				match(LPAREN);
				setState(90);
				expression(0);
				setState(91);
				match(RPAREN);
				}
				break;
			case NOT:
				{
				_localctx = new NotExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(93);
				match(NOT);
				setState(94);
				expression(4);
				}
				break;
			case IDENTIFIER:
				{
				_localctx = new PredicateExprContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(95);
				predicate();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(106);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,9,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(104);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,8,_ctx) ) {
					case 1:
						{
						_localctx = new AndExpressionContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(98);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(99);
						match(AND);
						setState(100);
						expression(4);
						}
						break;
					case 2:
						{
						_localctx = new OrExpressionContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(101);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(102);
						match(OR);
						setState(103);
						expression(3);
						}
						break;
					}
					} 
				}
				setState(108);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,9,_ctx);
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

	@SuppressWarnings("CheckReturnValue")
	public static class PredicateContext extends ParserRuleContext {
		public PredicateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_predicate; }
	 
		public PredicateContext() { }
		public void copyFrom(PredicateContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class BetweenPredicateContext extends PredicateContext {
		public TerminalNode IDENTIFIER() { return getToken(StreamParser.IDENTIFIER, 0); }
		public TerminalNode BETWEEN() { return getToken(StreamParser.BETWEEN, 0); }
		public List<ValueContext> value() {
			return getRuleContexts(ValueContext.class);
		}
		public ValueContext value(int i) {
			return getRuleContext(ValueContext.class,i);
		}
		public TerminalNode AND() { return getToken(StreamParser.AND, 0); }
		public BetweenPredicateContext(PredicateContext ctx) { copyFrom(ctx); }
	}
	@SuppressWarnings("CheckReturnValue")
	public static class IsNullPredicateContext extends PredicateContext {
		public TerminalNode IDENTIFIER() { return getToken(StreamParser.IDENTIFIER, 0); }
		public TerminalNode IS() { return getToken(StreamParser.IS, 0); }
		public TerminalNode NULL() { return getToken(StreamParser.NULL, 0); }
		public IsNullPredicateContext(PredicateContext ctx) { copyFrom(ctx); }
	}
	@SuppressWarnings("CheckReturnValue")
	public static class LikePredicateContext extends PredicateContext {
		public TerminalNode IDENTIFIER() { return getToken(StreamParser.IDENTIFIER, 0); }
		public TerminalNode LIKE() { return getToken(StreamParser.LIKE, 0); }
		public TerminalNode STRING() { return getToken(StreamParser.STRING, 0); }
		public LikePredicateContext(PredicateContext ctx) { copyFrom(ctx); }
	}
	@SuppressWarnings("CheckReturnValue")
	public static class InPredicateContext extends PredicateContext {
		public TerminalNode IDENTIFIER() { return getToken(StreamParser.IDENTIFIER, 0); }
		public TerminalNode IN() { return getToken(StreamParser.IN, 0); }
		public ValueListContext valueList() {
			return getRuleContext(ValueListContext.class,0);
		}
		public InPredicateContext(PredicateContext ctx) { copyFrom(ctx); }
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ComparisonPredicateContext extends PredicateContext {
		public TerminalNode IDENTIFIER() { return getToken(StreamParser.IDENTIFIER, 0); }
		public ComparatorContext comparator() {
			return getRuleContext(ComparatorContext.class,0);
		}
		public ValueContext value() {
			return getRuleContext(ValueContext.class,0);
		}
		public ComparisonPredicateContext(PredicateContext ctx) { copyFrom(ctx); }
	}
	@SuppressWarnings("CheckReturnValue")
	public static class IsNotNullPredicateContext extends PredicateContext {
		public TerminalNode IDENTIFIER() { return getToken(StreamParser.IDENTIFIER, 0); }
		public TerminalNode IS() { return getToken(StreamParser.IS, 0); }
		public TerminalNode NOT() { return getToken(StreamParser.NOT, 0); }
		public TerminalNode NULL() { return getToken(StreamParser.NULL, 0); }
		public IsNotNullPredicateContext(PredicateContext ctx) { copyFrom(ctx); }
	}

	public final PredicateContext predicate() throws RecognitionException {
		PredicateContext _localctx = new PredicateContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_predicate);
		try {
			setState(132);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,10,_ctx) ) {
			case 1:
				_localctx = new ComparisonPredicateContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(109);
				match(IDENTIFIER);
				setState(110);
				comparator();
				setState(111);
				value();
				}
				break;
			case 2:
				_localctx = new InPredicateContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(113);
				match(IDENTIFIER);
				setState(114);
				match(IN);
				setState(115);
				valueList();
				}
				break;
			case 3:
				_localctx = new BetweenPredicateContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(116);
				match(IDENTIFIER);
				setState(117);
				match(BETWEEN);
				setState(118);
				value();
				setState(119);
				match(AND);
				setState(120);
				value();
				}
				break;
			case 4:
				_localctx = new IsNullPredicateContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(122);
				match(IDENTIFIER);
				setState(123);
				match(IS);
				setState(124);
				match(NULL);
				}
				break;
			case 5:
				_localctx = new IsNotNullPredicateContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(125);
				match(IDENTIFIER);
				setState(126);
				match(IS);
				setState(127);
				match(NOT);
				setState(128);
				match(NULL);
				}
				break;
			case 6:
				_localctx = new LikePredicateContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(129);
				match(IDENTIFIER);
				setState(130);
				match(LIKE);
				setState(131);
				match(STRING);
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

	@SuppressWarnings("CheckReturnValue")
	public static class ComparatorContext extends ParserRuleContext {
		public TerminalNode EQ() { return getToken(StreamParser.EQ, 0); }
		public TerminalNode NEQ() { return getToken(StreamParser.NEQ, 0); }
		public TerminalNode LT() { return getToken(StreamParser.LT, 0); }
		public TerminalNode LTE() { return getToken(StreamParser.LTE, 0); }
		public TerminalNode GT() { return getToken(StreamParser.GT, 0); }
		public TerminalNode GTE() { return getToken(StreamParser.GTE, 0); }
		public ComparatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_comparator; }
	}

	public final ComparatorContext comparator() throws RecognitionException {
		ComparatorContext _localctx = new ComparatorContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_comparator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(134);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 1032192L) != 0)) ) {
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

	@SuppressWarnings("CheckReturnValue")
	public static class ValueContext extends ParserRuleContext {
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public TerminalNode IDENTIFIER() { return getToken(StreamParser.IDENTIFIER, 0); }
		public ValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_value; }
	}

	public final ValueContext value() throws RecognitionException {
		ValueContext _localctx = new ValueContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_value);
		try {
			setState(138);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case NULL:
			case BOOLEAN:
			case STRING:
			case NUMBER:
				enterOuterAlt(_localctx, 1);
				{
				setState(136);
				literal();
				}
				break;
			case IDENTIFIER:
				enterOuterAlt(_localctx, 2);
				{
				setState(137);
				match(IDENTIFIER);
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

	@SuppressWarnings("CheckReturnValue")
	public static class ValueListContext extends ParserRuleContext {
		public TerminalNode LPAREN() { return getToken(StreamParser.LPAREN, 0); }
		public List<LiteralContext> literal() {
			return getRuleContexts(LiteralContext.class);
		}
		public LiteralContext literal(int i) {
			return getRuleContext(LiteralContext.class,i);
		}
		public TerminalNode RPAREN() { return getToken(StreamParser.RPAREN, 0); }
		public List<TerminalNode> COMMA() { return getTokens(StreamParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(StreamParser.COMMA, i);
		}
		public ValueListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_valueList; }
	}

	public final ValueListContext valueList() throws RecognitionException {
		ValueListContext _localctx = new ValueListContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_valueList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(140);
			match(LPAREN);
			setState(141);
			literal();
			setState(146);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(142);
				match(COMMA);
				setState(143);
				literal();
				}
				}
				setState(148);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(149);
			match(RPAREN);
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

	@SuppressWarnings("CheckReturnValue")
	public static class LiteralContext extends ParserRuleContext {
		public TerminalNode STRING() { return getToken(StreamParser.STRING, 0); }
		public TerminalNode NUMBER() { return getToken(StreamParser.NUMBER, 0); }
		public TerminalNode BOOLEAN() { return getToken(StreamParser.BOOLEAN, 0); }
		public TerminalNode NULL() { return getToken(StreamParser.NULL, 0); }
		public LiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_literal; }
	}

	public final LiteralContext literal() throws RecognitionException {
		LiteralContext _localctx = new LiteralContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(151);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 58724352L) != 0)) ) {
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
		case 10:
			return expression_sempred((ExpressionContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean expression_sempred(ExpressionContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 3);
		case 1:
			return precpred(_ctx, 2);
		}
		return true;
	}

	public static final String _serializedATN =
		"\u0004\u0001\u001b\u009a\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001"+
		"\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004"+
		"\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007"+
		"\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b"+
		"\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007"+
		"\u000f\u0001\u0000\u0004\u0000\"\b\u0000\u000b\u0000\f\u0000#\u0001\u0000"+
		"\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0003\u0002.\b\u0002\u0001\u0002\u0003\u00021\b\u0002\u0003"+
		"\u00023\b\u0002\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0004\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0005\u0001\u0005\u0001\u0005\u0001"+
		"\u0006\u0001\u0006\u0001\u0006\u0005\u0006B\b\u0006\n\u0006\f\u0006E\t"+
		"\u0006\u0001\u0007\u0001\u0007\u0001\u0007\u0005\u0007J\b\u0007\n\u0007"+
		"\f\u0007M\t\u0007\u0001\b\u0001\b\u0001\b\u0005\bR\b\b\n\b\f\bU\t\b\u0001"+
		"\t\u0001\t\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001"+
		"\n\u0003\na\b\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0005"+
		"\ni\b\n\n\n\f\nl\t\n\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001"+
		"\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001"+
		"\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001"+
		"\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001"+
		"\u000b\u0003\u000b\u0085\b\u000b\u0001\f\u0001\f\u0001\r\u0001\r\u0003"+
		"\r\u008b\b\r\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0005\u000e"+
		"\u0091\b\u000e\n\u000e\f\u000e\u0094\t\u000e\u0001\u000e\u0001\u000e\u0001"+
		"\u000f\u0001\u000f\u0001\u000f\u0000\u0001\u0014\u0010\u0000\u0002\u0004"+
		"\u0006\b\n\f\u000e\u0010\u0012\u0014\u0016\u0018\u001a\u001c\u001e\u0000"+
		"\u0002\u0001\u0000\u000e\u0013\u0002\u0000\f\f\u0017\u0019\u009b\u0000"+
		"!\u0001\u0000\u0000\u0000\u0002\'\u0001\u0000\u0000\u0000\u0004*\u0001"+
		"\u0000\u0000\u0000\u00064\u0001\u0000\u0000\u0000\b7\u0001\u0000\u0000"+
		"\u0000\n;\u0001\u0000\u0000\u0000\f>\u0001\u0000\u0000\u0000\u000eF\u0001"+
		"\u0000\u0000\u0000\u0010N\u0001\u0000\u0000\u0000\u0012V\u0001\u0000\u0000"+
		"\u0000\u0014`\u0001\u0000\u0000\u0000\u0016\u0084\u0001\u0000\u0000\u0000"+
		"\u0018\u0086\u0001\u0000\u0000\u0000\u001a\u008a\u0001\u0000\u0000\u0000"+
		"\u001c\u008c\u0001\u0000\u0000\u0000\u001e\u0097\u0001\u0000\u0000\u0000"+
		" \"\u0003\u0002\u0001\u0000! \u0001\u0000\u0000\u0000\"#\u0001\u0000\u0000"+
		"\u0000#!\u0001\u0000\u0000\u0000#$\u0001\u0000\u0000\u0000$%\u0001\u0000"+
		"\u0000\u0000%&\u0005\u0000\u0000\u0001&\u0001\u0001\u0000\u0000\u0000"+
		"\'(\u0003\u0004\u0002\u0000()\u0003\n\u0005\u0000)\u0003\u0001\u0000\u0000"+
		"\u0000*+\u0005\u0001\u0000\u0000+2\u0003\f\u0006\u0000,.\u0003\u0006\u0003"+
		"\u0000-,\u0001\u0000\u0000\u0000-.\u0001\u0000\u0000\u0000.3\u0001\u0000"+
		"\u0000\u0000/1\u0003\b\u0004\u00000/\u0001\u0000\u0000\u000001\u0001\u0000"+
		"\u0000\u000013\u0001\u0000\u0000\u00002-\u0001\u0000\u0000\u000020\u0001"+
		"\u0000\u0000\u00003\u0005\u0001\u0000\u0000\u000045\u0005\u0003\u0000"+
		"\u000056\u0003\u0014\n\u00006\u0007\u0001\u0000\u0000\u000078\u0005\u0004"+
		"\u0000\u000089\u0005\u0005\u0000\u00009:\u0003\u0010\b\u0000:\t\u0001"+
		"\u0000\u0000\u0000;<\u0005\u0002\u0000\u0000<=\u0003\u000e\u0007\u0000"+
		"=\u000b\u0001\u0000\u0000\u0000>C\u0003\u0012\t\u0000?@\u0005\u0016\u0000"+
		"\u0000@B\u0003\u0012\t\u0000A?\u0001\u0000\u0000\u0000BE\u0001\u0000\u0000"+
		"\u0000CA\u0001\u0000\u0000\u0000CD\u0001\u0000\u0000\u0000D\r\u0001\u0000"+
		"\u0000\u0000EC\u0001\u0000\u0000\u0000FK\u0003\u0012\t\u0000GH\u0005\u0016"+
		"\u0000\u0000HJ\u0003\u0012\t\u0000IG\u0001\u0000\u0000\u0000JM\u0001\u0000"+
		"\u0000\u0000KI\u0001\u0000\u0000\u0000KL\u0001\u0000\u0000\u0000L\u000f"+
		"\u0001\u0000\u0000\u0000MK\u0001\u0000\u0000\u0000NS\u0005\u001a\u0000"+
		"\u0000OP\u0005\u0016\u0000\u0000PR\u0005\u001a\u0000\u0000QO\u0001\u0000"+
		"\u0000\u0000RU\u0001\u0000\u0000\u0000SQ\u0001\u0000\u0000\u0000ST\u0001"+
		"\u0000\u0000\u0000T\u0011\u0001\u0000\u0000\u0000US\u0001\u0000\u0000"+
		"\u0000VW\u0005\u001a\u0000\u0000W\u0013\u0001\u0000\u0000\u0000XY\u0006"+
		"\n\uffff\uffff\u0000YZ\u0005\u0014\u0000\u0000Z[\u0003\u0014\n\u0000["+
		"\\\u0005\u0015\u0000\u0000\\a\u0001\u0000\u0000\u0000]^\u0005\b\u0000"+
		"\u0000^a\u0003\u0014\n\u0004_a\u0003\u0016\u000b\u0000`X\u0001\u0000\u0000"+
		"\u0000`]\u0001\u0000\u0000\u0000`_\u0001\u0000\u0000\u0000aj\u0001\u0000"+
		"\u0000\u0000bc\n\u0003\u0000\u0000cd\u0005\u0006\u0000\u0000di\u0003\u0014"+
		"\n\u0004ef\n\u0002\u0000\u0000fg\u0005\u0007\u0000\u0000gi\u0003\u0014"+
		"\n\u0003hb\u0001\u0000\u0000\u0000he\u0001\u0000\u0000\u0000il\u0001\u0000"+
		"\u0000\u0000jh\u0001\u0000\u0000\u0000jk\u0001\u0000\u0000\u0000k\u0015"+
		"\u0001\u0000\u0000\u0000lj\u0001\u0000\u0000\u0000mn\u0005\u001a\u0000"+
		"\u0000no\u0003\u0018\f\u0000op\u0003\u001a\r\u0000p\u0085\u0001\u0000"+
		"\u0000\u0000qr\u0005\u001a\u0000\u0000rs\u0005\t\u0000\u0000s\u0085\u0003"+
		"\u001c\u000e\u0000tu\u0005\u001a\u0000\u0000uv\u0005\n\u0000\u0000vw\u0003"+
		"\u001a\r\u0000wx\u0005\u0006\u0000\u0000xy\u0003\u001a\r\u0000y\u0085"+
		"\u0001\u0000\u0000\u0000z{\u0005\u001a\u0000\u0000{|\u0005\u000b\u0000"+
		"\u0000|\u0085\u0005\f\u0000\u0000}~\u0005\u001a\u0000\u0000~\u007f\u0005"+
		"\u000b\u0000\u0000\u007f\u0080\u0005\b\u0000\u0000\u0080\u0085\u0005\f"+
		"\u0000\u0000\u0081\u0082\u0005\u001a\u0000\u0000\u0082\u0083\u0005\r\u0000"+
		"\u0000\u0083\u0085\u0005\u0018\u0000\u0000\u0084m\u0001\u0000\u0000\u0000"+
		"\u0084q\u0001\u0000\u0000\u0000\u0084t\u0001\u0000\u0000\u0000\u0084z"+
		"\u0001\u0000\u0000\u0000\u0084}\u0001\u0000\u0000\u0000\u0084\u0081\u0001"+
		"\u0000\u0000\u0000\u0085\u0017\u0001\u0000\u0000\u0000\u0086\u0087\u0007"+
		"\u0000\u0000\u0000\u0087\u0019\u0001\u0000\u0000\u0000\u0088\u008b\u0003"+
		"\u001e\u000f\u0000\u0089\u008b\u0005\u001a\u0000\u0000\u008a\u0088\u0001"+
		"\u0000\u0000\u0000\u008a\u0089\u0001\u0000\u0000\u0000\u008b\u001b\u0001"+
		"\u0000\u0000\u0000\u008c\u008d\u0005\u0014\u0000\u0000\u008d\u0092\u0003"+
		"\u001e\u000f\u0000\u008e\u008f\u0005\u0016\u0000\u0000\u008f\u0091\u0003"+
		"\u001e\u000f\u0000\u0090\u008e\u0001\u0000\u0000\u0000\u0091\u0094\u0001"+
		"\u0000\u0000\u0000\u0092\u0090\u0001\u0000\u0000\u0000\u0092\u0093\u0001"+
		"\u0000\u0000\u0000\u0093\u0095\u0001\u0000\u0000\u0000\u0094\u0092\u0001"+
		"\u0000\u0000\u0000\u0095\u0096\u0005\u0015\u0000\u0000\u0096\u001d\u0001"+
		"\u0000\u0000\u0000\u0097\u0098\u0007\u0001\u0000\u0000\u0098\u001f\u0001"+
		"\u0000\u0000\u0000\r#-02CKS`hj\u0084\u008a\u0092";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}