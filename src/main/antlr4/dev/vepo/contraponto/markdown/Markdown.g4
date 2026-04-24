grammar Markdown;

document: block* EOF;

block: heading | paragraph;

paragraph: (inline (linebreak | newline))+ inline (
		linebreak
		| NEWLINE
	)
	| inline (linebreak | NEWLINE);
linebreak: '  ' NEWLINE;
heading:
	H1 inline NEWLINE
	| H2 inline NEWLINE
	| H3 inline NEWLINE
	| H4 inline NEWLINE
	| H5 inline NEWLINE
	| H6 inline NEWLINE;

H6: '######' ' '+;
H5: '#####' ' '+;
H4: '####' ' '+;
H3: '###' ' '+;
H2: '##' ' '+;
H1: '#' ' '+;

inline: (inlineCode | bold | italic | text)+;
inlineCode: '`' text+ '`';
italic: ('*' italicText+ '*') | ('_' italicText+ '_');
italicText: text+ | bold;
bold: ('**' text+ '**') | ('__' text+ '__');
boldtext: text+ | italic;
text: NON_NEWLINE+ | SPACE NON_NEWLINE+ | NON_NEWLINE+ SPACE | SPACE NON_NEWLINE+ SPACE;
newline: NEWLINE;
SPACE: ' ';
NON_NEWLINE: ~('\r' | '\n' | '#' | '*' | '_'| ' ');
NEWLINE: '\r'? '\n';