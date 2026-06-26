grammar Stream;

stream: pipeline+ EOF;

pipeline: FROM topicName (PIPE stage)*;

stage
    : FILTER WHERE expression                          # FilterStage
    | PROJECT projectSpec                            # ProjectStage
    | MAP mapBody                                    # MapStage
    | WINDOW windowSpec                              # WindowStage
    | GROUP BY fieldList                             # GroupByStage
    | AGGREGATE aggFunctionList                      # AggregateStage
    | JOIN joinTarget ON expression joinSource       # JoinStage
    | TO destinationList                             # ToStage
    | BRANCH branchSpec                              # BranchStage
    | PATTERN patternSpec                            # PatternStage
    | SESSIONIZE sessionSpec                         # SessionizeStage
    ;

projectSpec
    : FIELDS COLON fieldList
    | IDENTIFIER COLON fieldList
    | projectField (COMMA projectField)*
    ;

projectField: fieldName | fieldName EQ expression;

mapBody: SET mapping (COMMA mapping)*;

mapping: fieldName EQ expression;

windowSpec
    : TUMBLING SIZE duration
    | HOPPING SIZE duration ADVANCE BY duration
    | SLIDING SIZE duration
    ;

joinTarget: IDENTIFIER;

joinSource
    : LOOKUP TABLE quotedTopic
    | STREAM_KW quotedTopic
    | WITHIN duration
    ;

branchSpec: branchCase (COMMA branchCase)*;

branchCase
    : CASE WHEN expression branchPipeline
    | DEFAULT branchPipeline
    ;

branchPipeline: (PIPE stage)*;

patternSpec: patternDef (COMMA patternDef)* DETECT AS IDENTIFIER;

patternDef: IDENTIFIER EQ patternExpression;

patternExpression
    : expression (WITHIN duration)? (AFTER IDENTIFIER)?
    ;

sessionSpec: BY fieldList GAP duration (TIMEOUT duration)?;

aggFunctionList: aggFunction (COMMA aggFunction)*;

aggFunction
    : COUNT LPAREN STAR RPAREN (AS IDENTIFIER)?
    | COUNT LPAREN fieldName RPAREN (AS IDENTIFIER)?
    | aggName LPAREN fieldName RPAREN (AS IDENTIFIER)?
    ;

aggName: SUM | AVG | MIN | MAX | FIRST | LAST;

destinationList: topicName (COMMA topicName)*;

topicName: IDENTIFIER | quotedTopic;

quotedTopic: STRING;

fieldList: fieldName (COMMA fieldName)*;

fieldName: IDENTIFIER (DOT IDENTIFIER)*;

expression
    : LPAREN expression RPAREN                    # ParenExpression
    | NOT expression                              # NotExpression
    | left = expression AND right = expression    # AndExpression
    | left = expression OR right = expression     # OrExpression
    | mathExpression                              # MathExpr
    | predicate                                   # PredicateExpr
    | functionCall                                # FunctionExpr
    | fieldName                                   # FieldRefExpr
    | literal                                     # LiteralExpr
    ;

mathExpression
    : MINUS mathExpression                        # UnaryMinus
    | left = mathExpression op = (STAR | DIV) right = mathExpression  # MulDiv
    | left = mathExpression op = (PLUS | MINUS) right = mathExpression # AddSub
    | atom                                        # MathAtom
    ;

atom
    : fieldName
    | literal
    | functionCall
    | LPAREN mathExpression RPAREN
    ;

functionCall: functionName LPAREN (expression (COMMA expression)*)? RPAREN;

functionName: IDENTIFIER;

predicate
    : fieldName comparisonOperator value          # ComparisonPredicate
    | fieldName IN valueList                      # InPredicate
    | fieldName BETWEEN value AND value           # BetweenPredicate
    | fieldName IS NULL                           # IsNullPredicate
    | fieldName IS NOT NULL                       # IsNotNullPredicate
    | fieldName LIKE STRING                       # LikePredicate
    | fieldName REGEX STRING                      # RegexPredicate
    ;

comparisonOperator: EQ | NEQ | LT | LTE | GT | GTE;

value: literal | fieldName;

valueList: LPAREN literal (COMMA literal)* RPAREN;

duration: NUMBER timeUnit;

timeUnit: MILLISECONDS | SECONDS | MINUTES | HOURS | HOUR | DAYS;

literal: STRING | NUMBER | BOOLEAN | NULL;

// Keywords (case-insensitive)
FROM: [Ff][Rr][Oo][Mm];
TO: [Tt][Oo];
WHERE: [Ww][Hh][Ee][Rr][Ee];
FILTER: [Ff][Ii][Ll][Tt][Ee][Rr];
PROJECT: [Pp][Rr][Oo][Jj][Ee][Cc][Tt];
FIELDS: [Ff][Ii][Ll][Dd][Ss];
MAP: [Mm][Aa][Pp];
SET: [Ss][Ee][Tt];
WINDOW: [Ww][Ii][Nn][Dd][Oo][Ww];
GROUP: [Gg][Rr][Oo][Uu][Pp];
AGGREGATE: [Aa][Gg][Gg][Rr][Ee][Gg][Aa][Tt][Ee];
JOIN: [Jj][Oo][Ii][Nn];
BRANCH: [Bb][Rr][Aa][Nn][Cc][Hh];
PATTERN: [Pp][Aa][Tt][Tt][Ee][Rr][Nn];
SESSIONIZE: [Ss][Ee][Ss][Ss][Ii][Oo][Nn][Ii][Zz][Ee];
TUMBLING: [Tt][Uu][Mm][Bb][Ll][Ii][Nn][Gg];
HOPPING: [Hh][Oo][Pp][Pp][Ii][Nn][Gg];
SLIDING: [Ss][Ll][Ii][Dd][Ii][Nn][Gg];
SIZE: [Ss][Ii][Zz][Ee];
ADVANCE: [Aa][Dd][Vv][Aa][Nn][Cc][Ee];
BY: [Bb][Yy];
GAP: [Gg][Aa][Pp];
TIMEOUT: [Tt][Ii][Mm][Ee][Oo][Uu][Tt];
LOOKUP: [Ll][Oo][Oo][Kk][Uu][Pp];
TABLE: [Tt][Aa][Bb][Ll][Ee];
STREAM_KW: [Ss][Tt][Rr][Ee][Aa][Mm];
WITHIN: [Ww][Ii][Tt][Hh][Ii][Nn];
ON: [Oo][Nn];
AS: [Aa][Ss];
DETECT: [Dd][Ee][Tt][Ee][Cc][Tt];
AFTER: [Aa][Ff][Tt][Ee][Rr];
CASE: [Cc][Aa][Ss][Ee];
WHEN: [Ww][Hh][Ee][Nn];
DEFAULT: [Dd][Ee][Ff][Aa][Uu][Ll][Tt];
AND: [Aa][Nn][Dd];
OR: [Oo][Rr];
NOT: [Nn][Oo][Tt];
IN: [Ii][Nn];
BETWEEN: [Bb][Ee][Tt][Ww][Ee][Ee][Nn];
IS: [Ii][Ss];
NULL: [Nn][Uu][Ll][Ll];
LIKE: [Ll][Ii][Kk][Ee];
REGEX: [Rr][Ee][Gg][Ee][Xx];
SUM: [Ss][Uu][Mm];
AVG: [Aa][Vv][Gg];
MIN: [Mm][Ii][Nn];
MAX: [Mm][Aa][Xx];
FIRST: [Ff][Ii][Rr][Ss][Tt];
LAST: [Ll][Aa][Ss][Tt];
COUNT: [Cc][Oo][Uu][Nn][Tt];
MILLISECONDS: [Mm][Ss] | [Mm][Ii][Ll][Ll][Ii][Ss][Ee][Cc][Oo][Nn][Dd][Ss];
SECONDS: [Ss] | [Ss][Ee][Cc][Oo][Nn][Dd][Ss];
MINUTES: [Mm] | [Mm][Ii][Nn][Uu][Tt][Ee][Ss];
HOURS: [Hh][Oo][Uu][Rr][Ss];
HOUR: [Hh][Oo][Uu][Rr];
DAYS: [Dd] | [Dd][Aa][Yy][Ss];

// Operators
PIPE: '|>';
EQ: '=';
NEQ: '!=' | '<>';
LT: '<';
LTE: '<=';
GT: '>';
GTE: '>=';
PLUS: '+';
MINUS: '-';
STAR: '*';
DIV: '/';

// Punctuation
LPAREN: '(';
RPAREN: ')';
COMMA: ',';
COLON: ':';
DOT: '.';

// Literals
BOOLEAN: [Tt][Rr][Uu][Ee] | [Ff][Aa][Ll][Ee];
STRING: DQUOTE (ESC | ~["\\])* DQUOTE | SQUOTE (ESC | ~['\\])* SQUOTE;
NUMBER: '-'? INT '.' [0-9]+ EXP? | '-'? INT EXP | '-'? INT;

IDENTIFIER: [A-Za-z_] [A-Za-z0-9_]*;

fragment DQUOTE: '"';
fragment SQUOTE: '\'';
fragment ESC: '\\' (["'\\/bfnrt] | UNICODE);
fragment UNICODE: 'u' HEX HEX HEX HEX;
fragment HEX: [0-9a-fA-F];
fragment INT: '0' | [1-9] [0-9]*;
fragment EXP: [Ee] [+\-]? INT;

WS: [ \t\r\n]+ -> skip;
COMMENT: '--' ~[\r\n]* -> skip;
