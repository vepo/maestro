grammar Stream;

// Parser Rules
streamQueries: query (SEMICOLON query)* SEMICOLON? EOF;

query: FROM sourcePipeline TO sinkTopics;

sourcePipeline: sourceStage (PIPE processingStage)*;

sourceStage: sourceTopics (where? | unique?);

processingStage:
	projectStage
	| aggregateStage
	| windowStage
	| joinStage
	| flattenStage
	| filterStage
	| transformStage;

projectStage: PROJECT fieldList;

aggregateStage:
	AGGREGATE BY fieldList? aggregateFunction (
		',' aggregateFunction
	)*;

windowStage: WINDOW windowType windowSize (slideInterval)?;

joinStage:
	JOIN WITH sourceTopics ON joinCondition (windowType?);

flattenStage: FLATTEN fieldName;

filterStage: WHERE expression;

transformStage:
	TRANSFORM fieldName '=' expression (
		',' fieldName '=' expression
	)*;

windowType: TUMBLING | SLIDING | SESSION;

windowSize: duration;

slideInterval: EVERY duration;

duration: NUMBER timeUnit;

timeUnit: MILLISECONDS | SECONDS | MINUTES | HOURS | DAYS;

joinCondition: fieldName '=' fieldName;

aggregateFunction:
	IDENTIFIER '(' '*' ')' (AS IDENTIFIER)?
	| IDENTIFIER '(' fieldName ')' (AS IDENTIFIER)?;

sourceTopics: topicName (',' topicName)*;
sinkTopics: topicName (',' topicName)*;
fieldList: fieldName (',' fieldName)*;
fieldName: IDENTIFIER;

topicName: IDENTIFIER;
where: WHERE expression;
unique: UNIQUE BY fieldList;

expression:
	LPAREN expression RPAREN					# ParenExpression
	| NOT expression							# NotExpression
	| left = expression AND right = expression	# AndExpression
	| left = expression OR right = expression	# OrExpression
	| comparisonExpression						# ComparisonExpr;

comparisonExpression:
	left = atomExpression comparisonOperator right = atomExpression	# ComparisonOperatorExpression
	| atomExpression												# AtomExpr;

atomExpression:
	predicate		# PredicateExpr
	| functionCall	# FunctionExpr
	| fieldName		# FieldRefExpr
	| literal		# LiteralExpr;
comparisonOperator: EQ | NEQ | LT | LTE | GT | GTE;

functionCall:
	functionName '(' (expression (',' expression)*)? ')';

functionName: IDENTIFIER;

predicate:
	fieldName IN valueList				# InPredicate
	| fieldName BETWEEN value AND value	# BetweenPredicate
	| fieldName IS NULL					# IsNullPredicate
	| fieldName IS NOT NULL				# IsNotNullPredicate
	| fieldName LIKE STRING				# LikePredicate
	| fieldName REGEX STRING			# RegexPredicate;

value: literal | fieldName;

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
PROJECT: [Pp][Rr][Oo][Jj][Ee][Cc][Tt];
AGGREGATE: [Aa][Gg][Gg][Rr][Ee][Gg][Aa][Tt][Ee];
WINDOW: [Ww][Ii][Nn][Dd][Oo][Ww];
JOIN: [Jj][Oo][Ii][Nn];
WITH: [Ww][Ii][Tt][Hh];
ON: [Oo][Nn];
FLATTEN: [Ff][Ll][Aa][Tt][Tt][Ee][Nn];
TRANSFORM: [Tt][Rr][Aa][Nn][Ss][Ff][Oo][Rr][Mm];
TUMBLING: [Tt][Uu][Mm][Bb][Ll][Ii][Nn][Gg];
SLIDING: [Ss][Ll][Ii][Dd][Ii][Nn][Gg];
SESSION: [Ss][Ee][Ss][Ss][Ii][Oo][Nn];
EVERY: [Ee][Vv][Ee][Rr][Yy];
AS: [Aa][Ss];
REGEX: [Rr][Ee][Gg][Ee][Xx];
MILLISECONDS:
	[Mm][Ss]
	| [Mm][Ii][Ll][Ll][Ii][Ss][Ee][Cc][Oo][Nn][Dd][Ss];
SECONDS: [Ss] | [Ss][Ee][Cc][Oo][Nn][Dd][Ss];
MINUTES: [Mm] | [Mm][Ii][Nn][Uu][Tt][Ee][Ss];
HOURS: [Hh] | [Hh][Oo][Uu][Rr][Ss];
DAYS: [Dd] | [Dd][Aa][Yy][Ss];

// Operators
PIPE: '|>';
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
SEMICOLON: ';';

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