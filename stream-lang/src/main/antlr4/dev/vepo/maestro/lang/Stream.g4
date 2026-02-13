grammar Stream;

// Parser Rules
streamQueries: query+ EOF;

query: from to;

from: FROM sourceTopics (where? | unique?);

where: WHERE expression;

unique: UNIQUE BY fieldList;

to: TO sinkTopics;

sourceTopics: topicName (',' topicName)*;
sinkTopics: topicName (',' topicName)*;
fieldList: IDENTIFIER (',' IDENTIFIER)*;

topicName: IDENTIFIER;

expression:
	LPAREN expression RPAREN	# ParenExpression
	| NOT expression			# NotExpression
	| expression AND expression	# AndExpression
	| expression OR expression	# OrExpression
	| predicate					# PredicateExpr;

predicate:
	IDENTIFIER comparator value				# ComparisonPredicate
	| IDENTIFIER IN valueList				# InPredicate
	| IDENTIFIER BETWEEN value AND value	# BetweenPredicate
	| IDENTIFIER IS NULL					# IsNullPredicate
	| IDENTIFIER IS NOT NULL				# IsNotNullPredicate
	| IDENTIFIER LIKE STRING				# LikePredicate;

comparator: EQ | NEQ | LT | LTE | GT | GTE;

value: literal | IDENTIFIER;

valueList: LPAREN literal (',' literal)* RPAREN;

literal: STRING | NUMBER | BOOLEAN | NULL;

// Keywords (case-insensitive)
FROM: [Ff][Rr][Oo][Mm];
TO: [Tt][Oo];
WHERE: [Ww][Hh][Ee][Rr][Ee];
UNIQUE: [Uu][Nn][Ii][Qq][Uu][Ee];
BY: [Bb][Yy];
AND: [Aa][Nn][Dd];
OR: [Oo][Rr];
NOT: [Nn][Oo][Tt];
IN: [Ii][Nn];
BETWEEN: [Bb][Ee][Tt][Ww][Ee][Ee][Nn];
IS: [Ii][Ss];
NULL: [Nn][Uu][Ll][Ll];
LIKE: [Ll][Ii][Kk][Ee];

// Operators
EQ: '=';
NEQ: '!=' | '<>';
LT: '<';
LTE: '<=';
GT: '>';
GTE: '>=';

// Punctuation
LPAREN: '(';
RPAREN: ')';
COMMA: ',';

// Literals
BOOLEAN: [Tt][Rr][Uu][Ee] | [Ff][Aa][Ll][Ss][Ee];

STRING: DQUOTE (ESC | ~["\\])* DQUOTE;

NUMBER: '-'? INT '.' [0-9]+ EXP? | '-'? INT EXP | '-'? INT;

IDENTIFIER: [A-Za-z] [._\-A-Za-z0-9]*;

// Fragments
fragment DQUOTE: '"';
fragment ESC: '\\' (["\\/bfnrt] | UNICODE);
fragment UNICODE: 'u' HEX HEX HEX HEX;
fragment HEX: [0-9a-fA-F];
fragment INT: '0' | [1-9] [0-9]*;
fragment EXP: [Ee] [+\-]? INT;

// Skip whitespace
WS: [ \t\r\n]+ -> skip;