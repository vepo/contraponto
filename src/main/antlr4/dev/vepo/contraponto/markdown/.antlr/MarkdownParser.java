// Generated from /home/vepo/source/contraponto/src/main/antlr4/dev/vepo/contraponto/markdown/Markdown.g4 by ANTLR 4.13.1
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class MarkdownParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, H6=7, H5=8, H4=9, H3=10, 
		H2=11, H1=12, SPACE=13, NON_NEWLINE=14, NEWLINE=15;
	public static final int
		RULE_document = 0, RULE_block = 1, RULE_paragraph = 2, RULE_linebreak = 3, 
		RULE_heading = 4, RULE_inline = 5, RULE_inlineCode = 6, RULE_italic = 7, 
		RULE_italicText = 8, RULE_bold = 9, RULE_boldtext = 10, RULE_text = 11, 
		RULE_newline = 12;
	private static String[] makeRuleNames() {
		return new String[] {
			"document", "block", "paragraph", "linebreak", "heading", "inline", "inlineCode", 
			"italic", "italicText", "bold", "boldtext", "text", "newline"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'  '", "'`'", "'*'", "'_'", "'**'", "'__'", null, null, null, 
			null, null, null, "' '"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, "H6", "H5", "H4", "H3", "H2", 
			"H1", "SPACE", "NON_NEWLINE", "NEWLINE"
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
	public String getGrammarFileName() { return "Markdown.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public MarkdownParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DocumentContext extends ParserRuleContext {
		public TerminalNode EOF() { return getToken(MarkdownParser.EOF, 0); }
		public List<BlockContext> block() {
			return getRuleContexts(BlockContext.class);
		}
		public BlockContext block(int i) {
			return getRuleContext(BlockContext.class,i);
		}
		public DocumentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_document; }
	}

	public final DocumentContext document() throws RecognitionException {
		DocumentContext _localctx = new DocumentContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_document);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(29);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 32764L) != 0)) {
				{
				{
				setState(26);
				block();
				}
				}
				setState(31);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(32);
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
	public static class BlockContext extends ParserRuleContext {
		public HeadingContext heading() {
			return getRuleContext(HeadingContext.class,0);
		}
		public ParagraphContext paragraph() {
			return getRuleContext(ParagraphContext.class,0);
		}
		public BlockContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_block; }
	}

	public final BlockContext block() throws RecognitionException {
		BlockContext _localctx = new BlockContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_block);
		try {
			setState(36);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case H6:
			case H5:
			case H4:
			case H3:
			case H2:
			case H1:
				enterOuterAlt(_localctx, 1);
				{
				setState(34);
				heading();
				}
				break;
			case T__1:
			case T__2:
			case T__3:
			case T__4:
			case T__5:
			case SPACE:
			case NON_NEWLINE:
				enterOuterAlt(_localctx, 2);
				{
				setState(35);
				paragraph();
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
	public static class ParagraphContext extends ParserRuleContext {
		public List<InlineContext> inline() {
			return getRuleContexts(InlineContext.class);
		}
		public InlineContext inline(int i) {
			return getRuleContext(InlineContext.class,i);
		}
		public List<LinebreakContext> linebreak() {
			return getRuleContexts(LinebreakContext.class);
		}
		public LinebreakContext linebreak(int i) {
			return getRuleContext(LinebreakContext.class,i);
		}
		public TerminalNode NEWLINE() { return getToken(MarkdownParser.NEWLINE, 0); }
		public List<NewlineContext> newline() {
			return getRuleContexts(NewlineContext.class);
		}
		public NewlineContext newline(int i) {
			return getRuleContext(NewlineContext.class,i);
		}
		public ParagraphContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_paragraph; }
	}

	public final ParagraphContext paragraph() throws RecognitionException {
		ParagraphContext _localctx = new ParagraphContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_paragraph);
		try {
			int _alt;
			setState(57);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,6,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(43); 
				_errHandler.sync(this);
				_alt = 1;
				do {
					switch (_alt) {
					case 1:
						{
						{
						setState(38);
						inline();
						setState(41);
						_errHandler.sync(this);
						switch (_input.LA(1)) {
						case T__0:
							{
							setState(39);
							linebreak();
							}
							break;
						case NEWLINE:
							{
							setState(40);
							newline();
							}
							break;
						default:
							throw new NoViableAltException(this);
						}
						}
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					setState(45); 
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,3,_ctx);
				} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
				setState(47);
				inline();
				setState(50);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case T__0:
					{
					setState(48);
					linebreak();
					}
					break;
				case NEWLINE:
					{
					setState(49);
					match(NEWLINE);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(52);
				inline();
				setState(55);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case T__0:
					{
					setState(53);
					linebreak();
					}
					break;
				case NEWLINE:
					{
					setState(54);
					match(NEWLINE);
					}
					break;
				default:
					throw new NoViableAltException(this);
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

	@SuppressWarnings("CheckReturnValue")
	public static class LinebreakContext extends ParserRuleContext {
		public TerminalNode NEWLINE() { return getToken(MarkdownParser.NEWLINE, 0); }
		public LinebreakContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_linebreak; }
	}

	public final LinebreakContext linebreak() throws RecognitionException {
		LinebreakContext _localctx = new LinebreakContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_linebreak);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(59);
			match(T__0);
			setState(60);
			match(NEWLINE);
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
	public static class HeadingContext extends ParserRuleContext {
		public TerminalNode H1() { return getToken(MarkdownParser.H1, 0); }
		public InlineContext inline() {
			return getRuleContext(InlineContext.class,0);
		}
		public TerminalNode NEWLINE() { return getToken(MarkdownParser.NEWLINE, 0); }
		public TerminalNode H2() { return getToken(MarkdownParser.H2, 0); }
		public TerminalNode H3() { return getToken(MarkdownParser.H3, 0); }
		public TerminalNode H4() { return getToken(MarkdownParser.H4, 0); }
		public TerminalNode H5() { return getToken(MarkdownParser.H5, 0); }
		public TerminalNode H6() { return getToken(MarkdownParser.H6, 0); }
		public HeadingContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_heading; }
	}

	public final HeadingContext heading() throws RecognitionException {
		HeadingContext _localctx = new HeadingContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_heading);
		try {
			setState(86);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case H1:
				enterOuterAlt(_localctx, 1);
				{
				setState(62);
				match(H1);
				setState(63);
				inline();
				setState(64);
				match(NEWLINE);
				}
				break;
			case H2:
				enterOuterAlt(_localctx, 2);
				{
				setState(66);
				match(H2);
				setState(67);
				inline();
				setState(68);
				match(NEWLINE);
				}
				break;
			case H3:
				enterOuterAlt(_localctx, 3);
				{
				setState(70);
				match(H3);
				setState(71);
				inline();
				setState(72);
				match(NEWLINE);
				}
				break;
			case H4:
				enterOuterAlt(_localctx, 4);
				{
				setState(74);
				match(H4);
				setState(75);
				inline();
				setState(76);
				match(NEWLINE);
				}
				break;
			case H5:
				enterOuterAlt(_localctx, 5);
				{
				setState(78);
				match(H5);
				setState(79);
				inline();
				setState(80);
				match(NEWLINE);
				}
				break;
			case H6:
				enterOuterAlt(_localctx, 6);
				{
				setState(82);
				match(H6);
				setState(83);
				inline();
				setState(84);
				match(NEWLINE);
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
	public static class InlineContext extends ParserRuleContext {
		public List<InlineCodeContext> inlineCode() {
			return getRuleContexts(InlineCodeContext.class);
		}
		public InlineCodeContext inlineCode(int i) {
			return getRuleContext(InlineCodeContext.class,i);
		}
		public List<BoldContext> bold() {
			return getRuleContexts(BoldContext.class);
		}
		public BoldContext bold(int i) {
			return getRuleContext(BoldContext.class,i);
		}
		public List<ItalicContext> italic() {
			return getRuleContexts(ItalicContext.class);
		}
		public ItalicContext italic(int i) {
			return getRuleContext(ItalicContext.class,i);
		}
		public List<TextContext> text() {
			return getRuleContexts(TextContext.class);
		}
		public TextContext text(int i) {
			return getRuleContext(TextContext.class,i);
		}
		public InlineContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inline; }
	}

	public final InlineContext inline() throws RecognitionException {
		InlineContext _localctx = new InlineContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_inline);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(92); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				setState(92);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case T__1:
					{
					setState(88);
					inlineCode();
					}
					break;
				case T__4:
				case T__5:
					{
					setState(89);
					bold();
					}
					break;
				case T__2:
				case T__3:
					{
					setState(90);
					italic();
					}
					break;
				case SPACE:
				case NON_NEWLINE:
					{
					setState(91);
					text();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				setState(94); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & 24700L) != 0) );
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
	public static class InlineCodeContext extends ParserRuleContext {
		public List<TextContext> text() {
			return getRuleContexts(TextContext.class);
		}
		public TextContext text(int i) {
			return getRuleContext(TextContext.class,i);
		}
		public InlineCodeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inlineCode; }
	}

	public final InlineCodeContext inlineCode() throws RecognitionException {
		InlineCodeContext _localctx = new InlineCodeContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_inlineCode);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(96);
			match(T__1);
			setState(98); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(97);
				text();
				}
				}
				setState(100); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==SPACE || _la==NON_NEWLINE );
			setState(102);
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

	@SuppressWarnings("CheckReturnValue")
	public static class ItalicContext extends ParserRuleContext {
		public List<ItalicTextContext> italicText() {
			return getRuleContexts(ItalicTextContext.class);
		}
		public ItalicTextContext italicText(int i) {
			return getRuleContext(ItalicTextContext.class,i);
		}
		public ItalicContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_italic; }
	}

	public final ItalicContext italic() throws RecognitionException {
		ItalicContext _localctx = new ItalicContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_italic);
		int _la;
		try {
			setState(120);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__2:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(104);
				match(T__2);
				setState(106); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(105);
					italicText();
					}
					}
					setState(108); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & 24672L) != 0) );
				setState(110);
				match(T__2);
				}
				}
				break;
			case T__3:
				enterOuterAlt(_localctx, 2);
				{
				{
				setState(112);
				match(T__3);
				setState(114); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(113);
					italicText();
					}
					}
					setState(116); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & 24672L) != 0) );
				setState(118);
				match(T__3);
				}
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
	public static class ItalicTextContext extends ParserRuleContext {
		public List<TextContext> text() {
			return getRuleContexts(TextContext.class);
		}
		public TextContext text(int i) {
			return getRuleContext(TextContext.class,i);
		}
		public BoldContext bold() {
			return getRuleContext(BoldContext.class,0);
		}
		public ItalicTextContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_italicText; }
	}

	public final ItalicTextContext italicText() throws RecognitionException {
		ItalicTextContext _localctx = new ItalicTextContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_italicText);
		try {
			int _alt;
			setState(128);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case SPACE:
			case NON_NEWLINE:
				enterOuterAlt(_localctx, 1);
				{
				setState(123); 
				_errHandler.sync(this);
				_alt = 1;
				do {
					switch (_alt) {
					case 1:
						{
						{
						setState(122);
						text();
						}
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					setState(125); 
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,14,_ctx);
				} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
				}
				break;
			case T__4:
			case T__5:
				enterOuterAlt(_localctx, 2);
				{
				setState(127);
				bold();
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
	public static class BoldContext extends ParserRuleContext {
		public List<TextContext> text() {
			return getRuleContexts(TextContext.class);
		}
		public TextContext text(int i) {
			return getRuleContext(TextContext.class,i);
		}
		public BoldContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_bold; }
	}

	public final BoldContext bold() throws RecognitionException {
		BoldContext _localctx = new BoldContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_bold);
		int _la;
		try {
			setState(146);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__4:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(130);
				match(T__4);
				setState(132); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(131);
					text();
					}
					}
					setState(134); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==SPACE || _la==NON_NEWLINE );
				setState(136);
				match(T__4);
				}
				}
				break;
			case T__5:
				enterOuterAlt(_localctx, 2);
				{
				{
				setState(138);
				match(T__5);
				setState(140); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(139);
					text();
					}
					}
					setState(142); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==SPACE || _la==NON_NEWLINE );
				setState(144);
				match(T__5);
				}
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
	public static class BoldtextContext extends ParserRuleContext {
		public List<TextContext> text() {
			return getRuleContexts(TextContext.class);
		}
		public TextContext text(int i) {
			return getRuleContext(TextContext.class,i);
		}
		public ItalicContext italic() {
			return getRuleContext(ItalicContext.class,0);
		}
		public BoldtextContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_boldtext; }
	}

	public final BoldtextContext boldtext() throws RecognitionException {
		BoldtextContext _localctx = new BoldtextContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_boldtext);
		int _la;
		try {
			setState(154);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case SPACE:
			case NON_NEWLINE:
				enterOuterAlt(_localctx, 1);
				{
				setState(149); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(148);
					text();
					}
					}
					setState(151); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==SPACE || _la==NON_NEWLINE );
				}
				break;
			case T__2:
			case T__3:
				enterOuterAlt(_localctx, 2);
				{
				setState(153);
				italic();
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
	public static class TextContext extends ParserRuleContext {
		public List<TerminalNode> NON_NEWLINE() { return getTokens(MarkdownParser.NON_NEWLINE); }
		public TerminalNode NON_NEWLINE(int i) {
			return getToken(MarkdownParser.NON_NEWLINE, i);
		}
		public List<TerminalNode> SPACE() { return getTokens(MarkdownParser.SPACE); }
		public TerminalNode SPACE(int i) {
			return getToken(MarkdownParser.SPACE, i);
		}
		public TextContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_text; }
	}

	public final TextContext text() throws RecognitionException {
		TextContext _localctx = new TextContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_text);
		int _la;
		try {
			int _alt;
			setState(180);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,25,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(157); 
				_errHandler.sync(this);
				_alt = 1;
				do {
					switch (_alt) {
					case 1:
						{
						{
						setState(156);
						match(NON_NEWLINE);
						}
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					setState(159); 
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,21,_ctx);
				} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(161);
				match(SPACE);
				setState(163); 
				_errHandler.sync(this);
				_alt = 1;
				do {
					switch (_alt) {
					case 1:
						{
						{
						setState(162);
						match(NON_NEWLINE);
						}
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					setState(165); 
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,22,_ctx);
				} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(168); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(167);
					match(NON_NEWLINE);
					}
					}
					setState(170); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==NON_NEWLINE );
				setState(172);
				match(SPACE);
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(173);
				match(SPACE);
				setState(175); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(174);
					match(NON_NEWLINE);
					}
					}
					setState(177); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==NON_NEWLINE );
				setState(179);
				match(SPACE);
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
	public static class NewlineContext extends ParserRuleContext {
		public TerminalNode NEWLINE() { return getToken(MarkdownParser.NEWLINE, 0); }
		public NewlineContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_newline; }
	}

	public final NewlineContext newline() throws RecognitionException {
		NewlineContext _localctx = new NewlineContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_newline);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(182);
			match(NEWLINE);
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
		"\u0004\u0001\u000f\u00b9\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001"+
		"\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004"+
		"\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007"+
		"\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b"+
		"\u0002\f\u0007\f\u0001\u0000\u0005\u0000\u001c\b\u0000\n\u0000\f\u0000"+
		"\u001f\t\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0003\u0001"+
		"%\b\u0001\u0001\u0002\u0001\u0002\u0001\u0002\u0003\u0002*\b\u0002\u0004"+
		"\u0002,\b\u0002\u000b\u0002\f\u0002-\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0003\u00023\b\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0003\u0002"+
		"8\b\u0002\u0003\u0002:\b\u0002\u0001\u0003\u0001\u0003\u0001\u0003\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0003"+
		"\u0004W\b\u0004\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0004"+
		"\u0005]\b\u0005\u000b\u0005\f\u0005^\u0001\u0006\u0001\u0006\u0004\u0006"+
		"c\b\u0006\u000b\u0006\f\u0006d\u0001\u0006\u0001\u0006\u0001\u0007\u0001"+
		"\u0007\u0004\u0007k\b\u0007\u000b\u0007\f\u0007l\u0001\u0007\u0001\u0007"+
		"\u0001\u0007\u0001\u0007\u0004\u0007s\b\u0007\u000b\u0007\f\u0007t\u0001"+
		"\u0007\u0001\u0007\u0003\u0007y\b\u0007\u0001\b\u0004\b|\b\b\u000b\b\f"+
		"\b}\u0001\b\u0003\b\u0081\b\b\u0001\t\u0001\t\u0004\t\u0085\b\t\u000b"+
		"\t\f\t\u0086\u0001\t\u0001\t\u0001\t\u0001\t\u0004\t\u008d\b\t\u000b\t"+
		"\f\t\u008e\u0001\t\u0001\t\u0003\t\u0093\b\t\u0001\n\u0004\n\u0096\b\n"+
		"\u000b\n\f\n\u0097\u0001\n\u0003\n\u009b\b\n\u0001\u000b\u0004\u000b\u009e"+
		"\b\u000b\u000b\u000b\f\u000b\u009f\u0001\u000b\u0001\u000b\u0004\u000b"+
		"\u00a4\b\u000b\u000b\u000b\f\u000b\u00a5\u0001\u000b\u0004\u000b\u00a9"+
		"\b\u000b\u000b\u000b\f\u000b\u00aa\u0001\u000b\u0001\u000b\u0001\u000b"+
		"\u0004\u000b\u00b0\b\u000b\u000b\u000b\f\u000b\u00b1\u0001\u000b\u0003"+
		"\u000b\u00b5\b\u000b\u0001\f\u0001\f\u0001\f\u0000\u0000\r\u0000\u0002"+
		"\u0004\u0006\b\n\f\u000e\u0010\u0012\u0014\u0016\u0018\u0000\u0000\u00cd"+
		"\u0000\u001d\u0001\u0000\u0000\u0000\u0002$\u0001\u0000\u0000\u0000\u0004"+
		"9\u0001\u0000\u0000\u0000\u0006;\u0001\u0000\u0000\u0000\bV\u0001\u0000"+
		"\u0000\u0000\n\\\u0001\u0000\u0000\u0000\f`\u0001\u0000\u0000\u0000\u000e"+
		"x\u0001\u0000\u0000\u0000\u0010\u0080\u0001\u0000\u0000\u0000\u0012\u0092"+
		"\u0001\u0000\u0000\u0000\u0014\u009a\u0001\u0000\u0000\u0000\u0016\u00b4"+
		"\u0001\u0000\u0000\u0000\u0018\u00b6\u0001\u0000\u0000\u0000\u001a\u001c"+
		"\u0003\u0002\u0001\u0000\u001b\u001a\u0001\u0000\u0000\u0000\u001c\u001f"+
		"\u0001\u0000\u0000\u0000\u001d\u001b\u0001\u0000\u0000\u0000\u001d\u001e"+
		"\u0001\u0000\u0000\u0000\u001e \u0001\u0000\u0000\u0000\u001f\u001d\u0001"+
		"\u0000\u0000\u0000 !\u0005\u0000\u0000\u0001!\u0001\u0001\u0000\u0000"+
		"\u0000\"%\u0003\b\u0004\u0000#%\u0003\u0004\u0002\u0000$\"\u0001\u0000"+
		"\u0000\u0000$#\u0001\u0000\u0000\u0000%\u0003\u0001\u0000\u0000\u0000"+
		"&)\u0003\n\u0005\u0000\'*\u0003\u0006\u0003\u0000(*\u0003\u0018\f\u0000"+
		")\'\u0001\u0000\u0000\u0000)(\u0001\u0000\u0000\u0000*,\u0001\u0000\u0000"+
		"\u0000+&\u0001\u0000\u0000\u0000,-\u0001\u0000\u0000\u0000-+\u0001\u0000"+
		"\u0000\u0000-.\u0001\u0000\u0000\u0000./\u0001\u0000\u0000\u0000/2\u0003"+
		"\n\u0005\u000003\u0003\u0006\u0003\u000013\u0005\u000f\u0000\u000020\u0001"+
		"\u0000\u0000\u000021\u0001\u0000\u0000\u00003:\u0001\u0000\u0000\u0000"+
		"47\u0003\n\u0005\u000058\u0003\u0006\u0003\u000068\u0005\u000f\u0000\u0000"+
		"75\u0001\u0000\u0000\u000076\u0001\u0000\u0000\u00008:\u0001\u0000\u0000"+
		"\u00009+\u0001\u0000\u0000\u000094\u0001\u0000\u0000\u0000:\u0005\u0001"+
		"\u0000\u0000\u0000;<\u0005\u0001\u0000\u0000<=\u0005\u000f\u0000\u0000"+
		"=\u0007\u0001\u0000\u0000\u0000>?\u0005\f\u0000\u0000?@\u0003\n\u0005"+
		"\u0000@A\u0005\u000f\u0000\u0000AW\u0001\u0000\u0000\u0000BC\u0005\u000b"+
		"\u0000\u0000CD\u0003\n\u0005\u0000DE\u0005\u000f\u0000\u0000EW\u0001\u0000"+
		"\u0000\u0000FG\u0005\n\u0000\u0000GH\u0003\n\u0005\u0000HI\u0005\u000f"+
		"\u0000\u0000IW\u0001\u0000\u0000\u0000JK\u0005\t\u0000\u0000KL\u0003\n"+
		"\u0005\u0000LM\u0005\u000f\u0000\u0000MW\u0001\u0000\u0000\u0000NO\u0005"+
		"\b\u0000\u0000OP\u0003\n\u0005\u0000PQ\u0005\u000f\u0000\u0000QW\u0001"+
		"\u0000\u0000\u0000RS\u0005\u0007\u0000\u0000ST\u0003\n\u0005\u0000TU\u0005"+
		"\u000f\u0000\u0000UW\u0001\u0000\u0000\u0000V>\u0001\u0000\u0000\u0000"+
		"VB\u0001\u0000\u0000\u0000VF\u0001\u0000\u0000\u0000VJ\u0001\u0000\u0000"+
		"\u0000VN\u0001\u0000\u0000\u0000VR\u0001\u0000\u0000\u0000W\t\u0001\u0000"+
		"\u0000\u0000X]\u0003\f\u0006\u0000Y]\u0003\u0012\t\u0000Z]\u0003\u000e"+
		"\u0007\u0000[]\u0003\u0016\u000b\u0000\\X\u0001\u0000\u0000\u0000\\Y\u0001"+
		"\u0000\u0000\u0000\\Z\u0001\u0000\u0000\u0000\\[\u0001\u0000\u0000\u0000"+
		"]^\u0001\u0000\u0000\u0000^\\\u0001\u0000\u0000\u0000^_\u0001\u0000\u0000"+
		"\u0000_\u000b\u0001\u0000\u0000\u0000`b\u0005\u0002\u0000\u0000ac\u0003"+
		"\u0016\u000b\u0000ba\u0001\u0000\u0000\u0000cd\u0001\u0000\u0000\u0000"+
		"db\u0001\u0000\u0000\u0000de\u0001\u0000\u0000\u0000ef\u0001\u0000\u0000"+
		"\u0000fg\u0005\u0002\u0000\u0000g\r\u0001\u0000\u0000\u0000hj\u0005\u0003"+
		"\u0000\u0000ik\u0003\u0010\b\u0000ji\u0001\u0000\u0000\u0000kl\u0001\u0000"+
		"\u0000\u0000lj\u0001\u0000\u0000\u0000lm\u0001\u0000\u0000\u0000mn\u0001"+
		"\u0000\u0000\u0000no\u0005\u0003\u0000\u0000oy\u0001\u0000\u0000\u0000"+
		"pr\u0005\u0004\u0000\u0000qs\u0003\u0010\b\u0000rq\u0001\u0000\u0000\u0000"+
		"st\u0001\u0000\u0000\u0000tr\u0001\u0000\u0000\u0000tu\u0001\u0000\u0000"+
		"\u0000uv\u0001\u0000\u0000\u0000vw\u0005\u0004\u0000\u0000wy\u0001\u0000"+
		"\u0000\u0000xh\u0001\u0000\u0000\u0000xp\u0001\u0000\u0000\u0000y\u000f"+
		"\u0001\u0000\u0000\u0000z|\u0003\u0016\u000b\u0000{z\u0001\u0000\u0000"+
		"\u0000|}\u0001\u0000\u0000\u0000}{\u0001\u0000\u0000\u0000}~\u0001\u0000"+
		"\u0000\u0000~\u0081\u0001\u0000\u0000\u0000\u007f\u0081\u0003\u0012\t"+
		"\u0000\u0080{\u0001\u0000\u0000\u0000\u0080\u007f\u0001\u0000\u0000\u0000"+
		"\u0081\u0011\u0001\u0000\u0000\u0000\u0082\u0084\u0005\u0005\u0000\u0000"+
		"\u0083\u0085\u0003\u0016\u000b\u0000\u0084\u0083\u0001\u0000\u0000\u0000"+
		"\u0085\u0086\u0001\u0000\u0000\u0000\u0086\u0084\u0001\u0000\u0000\u0000"+
		"\u0086\u0087\u0001\u0000\u0000\u0000\u0087\u0088\u0001\u0000\u0000\u0000"+
		"\u0088\u0089\u0005\u0005\u0000\u0000\u0089\u0093\u0001\u0000\u0000\u0000"+
		"\u008a\u008c\u0005\u0006\u0000\u0000\u008b\u008d\u0003\u0016\u000b\u0000"+
		"\u008c\u008b\u0001\u0000\u0000\u0000\u008d\u008e\u0001\u0000\u0000\u0000"+
		"\u008e\u008c\u0001\u0000\u0000\u0000\u008e\u008f\u0001\u0000\u0000\u0000"+
		"\u008f\u0090\u0001\u0000\u0000\u0000\u0090\u0091\u0005\u0006\u0000\u0000"+
		"\u0091\u0093\u0001\u0000\u0000\u0000\u0092\u0082\u0001\u0000\u0000\u0000"+
		"\u0092\u008a\u0001\u0000\u0000\u0000\u0093\u0013\u0001\u0000\u0000\u0000"+
		"\u0094\u0096\u0003\u0016\u000b\u0000\u0095\u0094\u0001\u0000\u0000\u0000"+
		"\u0096\u0097\u0001\u0000\u0000\u0000\u0097\u0095\u0001\u0000\u0000\u0000"+
		"\u0097\u0098\u0001\u0000\u0000\u0000\u0098\u009b\u0001\u0000\u0000\u0000"+
		"\u0099\u009b\u0003\u000e\u0007\u0000\u009a\u0095\u0001\u0000\u0000\u0000"+
		"\u009a\u0099\u0001\u0000\u0000\u0000\u009b\u0015\u0001\u0000\u0000\u0000"+
		"\u009c\u009e\u0005\u000e\u0000\u0000\u009d\u009c\u0001\u0000\u0000\u0000"+
		"\u009e\u009f\u0001\u0000\u0000\u0000\u009f\u009d\u0001\u0000\u0000\u0000"+
		"\u009f\u00a0\u0001\u0000\u0000\u0000\u00a0\u00b5\u0001\u0000\u0000\u0000"+
		"\u00a1\u00a3\u0005\r\u0000\u0000\u00a2\u00a4\u0005\u000e\u0000\u0000\u00a3"+
		"\u00a2\u0001\u0000\u0000\u0000\u00a4\u00a5\u0001\u0000\u0000\u0000\u00a5"+
		"\u00a3\u0001\u0000\u0000\u0000\u00a5\u00a6\u0001\u0000\u0000\u0000\u00a6"+
		"\u00b5\u0001\u0000\u0000\u0000\u00a7\u00a9\u0005\u000e\u0000\u0000\u00a8"+
		"\u00a7\u0001\u0000\u0000\u0000\u00a9\u00aa\u0001\u0000\u0000\u0000\u00aa"+
		"\u00a8\u0001\u0000\u0000\u0000\u00aa\u00ab\u0001\u0000\u0000\u0000\u00ab"+
		"\u00ac\u0001\u0000\u0000\u0000\u00ac\u00b5\u0005\r\u0000\u0000\u00ad\u00af"+
		"\u0005\r\u0000\u0000\u00ae\u00b0\u0005\u000e\u0000\u0000\u00af\u00ae\u0001"+
		"\u0000\u0000\u0000\u00b0\u00b1\u0001\u0000\u0000\u0000\u00b1\u00af\u0001"+
		"\u0000\u0000\u0000\u00b1\u00b2\u0001\u0000\u0000\u0000\u00b2\u00b3\u0001"+
		"\u0000\u0000\u0000\u00b3\u00b5\u0005\r\u0000\u0000\u00b4\u009d\u0001\u0000"+
		"\u0000\u0000\u00b4\u00a1\u0001\u0000\u0000\u0000\u00b4\u00a8\u0001\u0000"+
		"\u0000\u0000\u00b4\u00ad\u0001\u0000\u0000\u0000\u00b5\u0017\u0001\u0000"+
		"\u0000\u0000\u00b6\u00b7\u0005\u000f\u0000\u0000\u00b7\u0019\u0001\u0000"+
		"\u0000\u0000\u001a\u001d$)-279V\\^dltx}\u0080\u0086\u008e\u0092\u0097"+
		"\u009a\u009f\u00a5\u00aa\u00b1\u00b4";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}