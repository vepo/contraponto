grammar Markdown;

document: block* EOF;

block: heading | paragraph;

paragraph: inline (paragraphBreak inline)* NEWLINE;

paragraphBreak: (linebreak NEWLINE) | NEWLINE;
linebreak: SPACE SPACE+ NEWLINE;
heading:
	H1 inline+ NEWLINE
	| H2 inline+ NEWLINE
	| H3 inline+ NEWLINE
	| H4 inline+ NEWLINE
	| H5 inline+ NEWLINE
	| H6 inline+ NEWLINE;

H6: '######' ' '+;
H5: '#####' ' '+;
H4: '####' ' '+;
H3: '###' ' '+;
H2: '##' ' '+;
H1: '#' ' '+;

inline: (inlineContent)+ (space inlineContent)*;
inlineContent: inlineCode | bold | italic | text;
inlineCode: '`' text+ '`';
formattedBold: (italicPlain | text)+ (space ( italicPlain | text))*;
formattedItalic: (boldPlain | text)+ (space ( boldPlain | text))*;
bold: ('**' formattedBold+ '**') | ('__' formattedBold+ '__');
italic: ('*' formattedItalic+ '*') | ('_' formattedItalic+ '_');
boldPlain: ('**' (text (space text)*)+ '**')
	| ('__' (text (space text)*)+ '__');
italicPlain: ('*' (text (space text)*)+ '*')
	| ('_' (text (space text)*)+ '_');
text: (NON_NEWLINE+)
	| (NON_NEWLINE (SPACE | NON_NEWLINE)+ NON_NEWLINE);
space: SPACE;
newline: NEWLINE;
NON_NEWLINE: ~('\r' | '\n' | '#' | '*' | '_' | ' ');
SPACE: ' ';
NEWLINE: '\r'? '\n';