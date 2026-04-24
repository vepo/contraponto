// Generated from /home/vepo/source/contraponto/src/main/antlr4/dev/vepo/contraponto/markdown/Markdown.g4 by ANTLR 4.13.1
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class MarkdownLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.13.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, H6=7, H5=8, H4=9, H3=10, 
		H2=11, H1=12, SPACE=13, NON_NEWLINE=14, NEWLINE=15;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "H6", "H5", "H4", "H3", 
			"H2", "H1", "SPACE", "NON_NEWLINE", "NEWLINE"
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


	public MarkdownLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "Markdown.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\u0004\u0000\u000fo\u0006\uffff\uffff\u0002\u0000\u0007\u0000\u0002\u0001"+
		"\u0007\u0001\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004"+
		"\u0007\u0004\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007"+
		"\u0007\u0007\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b"+
		"\u0007\u000b\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0001"+
		"\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0002\u0001"+
		"\u0002\u0001\u0003\u0001\u0003\u0001\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0005\u0001\u0005\u0001\u0005\u0001\u0006\u0001\u0006\u0001\u0006\u0001"+
		"\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0004\u00067\b"+
		"\u0006\u000b\u0006\f\u00068\u0001\u0007\u0001\u0007\u0001\u0007\u0001"+
		"\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0004\u0007B\b\u0007\u000b"+
		"\u0007\f\u0007C\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0004"+
		"\bL\b\b\u000b\b\f\bM\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0004\tU"+
		"\b\t\u000b\t\f\tV\u0001\n\u0001\n\u0001\n\u0001\n\u0004\n]\b\n\u000b\n"+
		"\f\n^\u0001\u000b\u0001\u000b\u0004\u000bc\b\u000b\u000b\u000b\f\u000b"+
		"d\u0001\f\u0001\f\u0001\r\u0001\r\u0001\u000e\u0003\u000el\b\u000e\u0001"+
		"\u000e\u0001\u000e\u0000\u0000\u000f\u0001\u0001\u0003\u0002\u0005\u0003"+
		"\u0007\u0004\t\u0005\u000b\u0006\r\u0007\u000f\b\u0011\t\u0013\n\u0015"+
		"\u000b\u0017\f\u0019\r\u001b\u000e\u001d\u000f\u0001\u0000\u0001\u0006"+
		"\u0000\n\n\r\r  ##**__u\u0000\u0001\u0001\u0000\u0000\u0000\u0000\u0003"+
		"\u0001\u0000\u0000\u0000\u0000\u0005\u0001\u0000\u0000\u0000\u0000\u0007"+
		"\u0001\u0000\u0000\u0000\u0000\t\u0001\u0000\u0000\u0000\u0000\u000b\u0001"+
		"\u0000\u0000\u0000\u0000\r\u0001\u0000\u0000\u0000\u0000\u000f\u0001\u0000"+
		"\u0000\u0000\u0000\u0011\u0001\u0000\u0000\u0000\u0000\u0013\u0001\u0000"+
		"\u0000\u0000\u0000\u0015\u0001\u0000\u0000\u0000\u0000\u0017\u0001\u0000"+
		"\u0000\u0000\u0000\u0019\u0001\u0000\u0000\u0000\u0000\u001b\u0001\u0000"+
		"\u0000\u0000\u0000\u001d\u0001\u0000\u0000\u0000\u0001\u001f\u0001\u0000"+
		"\u0000\u0000\u0003\"\u0001\u0000\u0000\u0000\u0005$\u0001\u0000\u0000"+
		"\u0000\u0007&\u0001\u0000\u0000\u0000\t(\u0001\u0000\u0000\u0000\u000b"+
		"+\u0001\u0000\u0000\u0000\r.\u0001\u0000\u0000\u0000\u000f:\u0001\u0000"+
		"\u0000\u0000\u0011E\u0001\u0000\u0000\u0000\u0013O\u0001\u0000\u0000\u0000"+
		"\u0015X\u0001\u0000\u0000\u0000\u0017`\u0001\u0000\u0000\u0000\u0019f"+
		"\u0001\u0000\u0000\u0000\u001bh\u0001\u0000\u0000\u0000\u001dk\u0001\u0000"+
		"\u0000\u0000\u001f \u0005 \u0000\u0000 !\u0005 \u0000\u0000!\u0002\u0001"+
		"\u0000\u0000\u0000\"#\u0005`\u0000\u0000#\u0004\u0001\u0000\u0000\u0000"+
		"$%\u0005*\u0000\u0000%\u0006\u0001\u0000\u0000\u0000&\'\u0005_\u0000\u0000"+
		"\'\b\u0001\u0000\u0000\u0000()\u0005*\u0000\u0000)*\u0005*\u0000\u0000"+
		"*\n\u0001\u0000\u0000\u0000+,\u0005_\u0000\u0000,-\u0005_\u0000\u0000"+
		"-\f\u0001\u0000\u0000\u0000./\u0005#\u0000\u0000/0\u0005#\u0000\u0000"+
		"01\u0005#\u0000\u000012\u0005#\u0000\u000023\u0005#\u0000\u000034\u0005"+
		"#\u0000\u000046\u0001\u0000\u0000\u000057\u0005 \u0000\u000065\u0001\u0000"+
		"\u0000\u000078\u0001\u0000\u0000\u000086\u0001\u0000\u0000\u000089\u0001"+
		"\u0000\u0000\u00009\u000e\u0001\u0000\u0000\u0000:;\u0005#\u0000\u0000"+
		";<\u0005#\u0000\u0000<=\u0005#\u0000\u0000=>\u0005#\u0000\u0000>?\u0005"+
		"#\u0000\u0000?A\u0001\u0000\u0000\u0000@B\u0005 \u0000\u0000A@\u0001\u0000"+
		"\u0000\u0000BC\u0001\u0000\u0000\u0000CA\u0001\u0000\u0000\u0000CD\u0001"+
		"\u0000\u0000\u0000D\u0010\u0001\u0000\u0000\u0000EF\u0005#\u0000\u0000"+
		"FG\u0005#\u0000\u0000GH\u0005#\u0000\u0000HI\u0005#\u0000\u0000IK\u0001"+
		"\u0000\u0000\u0000JL\u0005 \u0000\u0000KJ\u0001\u0000\u0000\u0000LM\u0001"+
		"\u0000\u0000\u0000MK\u0001\u0000\u0000\u0000MN\u0001\u0000\u0000\u0000"+
		"N\u0012\u0001\u0000\u0000\u0000OP\u0005#\u0000\u0000PQ\u0005#\u0000\u0000"+
		"QR\u0005#\u0000\u0000RT\u0001\u0000\u0000\u0000SU\u0005 \u0000\u0000T"+
		"S\u0001\u0000\u0000\u0000UV\u0001\u0000\u0000\u0000VT\u0001\u0000\u0000"+
		"\u0000VW\u0001\u0000\u0000\u0000W\u0014\u0001\u0000\u0000\u0000XY\u0005"+
		"#\u0000\u0000YZ\u0005#\u0000\u0000Z\\\u0001\u0000\u0000\u0000[]\u0005"+
		" \u0000\u0000\\[\u0001\u0000\u0000\u0000]^\u0001\u0000\u0000\u0000^\\"+
		"\u0001\u0000\u0000\u0000^_\u0001\u0000\u0000\u0000_\u0016\u0001\u0000"+
		"\u0000\u0000`b\u0005#\u0000\u0000ac\u0005 \u0000\u0000ba\u0001\u0000\u0000"+
		"\u0000cd\u0001\u0000\u0000\u0000db\u0001\u0000\u0000\u0000de\u0001\u0000"+
		"\u0000\u0000e\u0018\u0001\u0000\u0000\u0000fg\u0005 \u0000\u0000g\u001a"+
		"\u0001\u0000\u0000\u0000hi\b\u0000\u0000\u0000i\u001c\u0001\u0000\u0000"+
		"\u0000jl\u0005\r\u0000\u0000kj\u0001\u0000\u0000\u0000kl\u0001\u0000\u0000"+
		"\u0000lm\u0001\u0000\u0000\u0000mn\u0005\n\u0000\u0000n\u001e\u0001\u0000"+
		"\u0000\u0000\b\u00008CMV^dk\u0000";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}