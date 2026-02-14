```
-- Basic pipeline with multiple stages
FROM input_topic 
|> FILTER WHERE status = 'active'
|> PROJECT fields: user_id, name, email
|> TO output_topic

-- Aggregation pipeline
FROM clickstream
|> FILTER WHERE event_type = 'page_view'
|> WINDOW TUMBLING SIZE 5 MINUTES
|> GROUP BY user_id, page_url
|> AGGREGATE count(*) AS view_count, avg(time_on_page) AS avg_time
|> TO analytics_topic

-- Stream enrichment through joins
FROM orders
|> JOIN users ON orders.user_id = users.id LOOKUP TABLE 'users'
|> JOIN products ON orders.product_id = products.id STREAM 'product_updates'
|> PROJECT enriched_order: order_id, user_name, product_name, quantity, price
|> TO enriched_orders

-- Complex ETL pipeline
FROM raw_sensor_data
|> FILTER WHERE temperature IS NOT NULL AND status != 'error'
|> MAP 
   SET normalized_temp = (temperature - 32) * 5/9,
   timestamp = to_epoch_ms(timestamp)
|> WINDOW HOPPING SIZE 10 MINUTES ADVANCE BY 5 MINUTES
|> GROUP BY device_id, sensor_type
|> AGGREGATE 
   avg(normalized_temp) AS avg_temp,
   max(normalized_temp) AS max_temp,
   min(normalized_temp) AS min_temp,
   count(*) AS reading_count
|> FILTER WHERE reading_count > 10
|> TO device_analytics, alert_topic

-- Multi-branch pipeline
FROM user_events
|> BRANCH
   CASE WHEN event_type = 'purchase' 
        |> PROJECT purchase_data
        |> TO purchase_topic,
   CASE WHEN event_type = 'click'
        |> WINDOW TUMBLING SIZE 1 HOUR
        |> GROUP BY user_id
        |> AGGREGATE count(*) AS click_count
        |> TO click_analytics,
   DEFAULT
        |> TO other_events_topic

-- Stream-stream join with window
FROM ad_impressions
|> WINDOW TUMBLING SIZE 1 HOUR
|> JOIN ad_clicks ON impression_id = click_id WITHIN 30 MINUTES
|> PROJECT 
   ad_id, 
   impression_time, 
   click_time,
   time_to_click = click_time - impression_time
|> TO ad_performance

-- Pattern detection
FROM stock_ticks
|> PATTERN 
   DROP = price < stop_loss,
   REBOUND = price > take_profit WITHIN 5 MINUTES AFTER DROP
|> DETECT AS volatility_event
|> TO alerts

-- Sessionization
FROM website_activity
|> SESSIONIZE BY user_id 
   GAP 30 MINUTES 
   TIMEOUT 2 HOURS
|> AGGREGATE 
   count(*) AS actions_per_session,
   sum(page_views) AS total_views,
   first(action_time) AS session_start,
   last(action_time) AS session_end
|> TO sessions_topic
```

Sample Grammar

```g4
grammar Stream;

// Parser Rules
stream: pipeline+ EOF;

pipeline: fromStage pipelineStage* toStage?;

fromStage: FROM topicName (AS alias)?;

pipelineStage
    : FILTER WHERE expression                                # FilterStage
    | PROJECT fieldList (AS alias)?                          # ProjectStage
    | MAP mappingList (AS alias)?                            # MapStage
    | WINDOW windowSpec (AS alias)?                          # WindowStage
    | GROUP BY fieldList (AS alias)?                         # GroupStage
    | AGGREGATE aggFunctionList (AS alias)?                  # AggregateStage
    | JOIN joinSpec (AS alias)?                              # JoinStage
    | BRANCH branchSpec                                      # BranchStage
    | PATTERN patternSpec (AS alias)?                        # PatternStage
    | SESSIONIZE sessionSpec (AS alias)?                     # SessionStage
    | UNION unionSpec                                        # UnionStage
    | EXPAND expandSpec                                      # ExpandStage
    ;

toStage: TO destinationList;

// Aliases and Identifiers
alias: IDENTIFIER;

topicName
    : IDENTIFIER
    | QUOTED_TOPIC
    ;

fieldName
    : IDENTIFIER
    ;

fieldList
    : fieldName (',' fieldName)*
    | STRING (',' STRING)*
    ;

destinationList: topicName (',' topicName)*;

// Window Specifications
windowSpec
    : TUMBLING SIZE timeUnit                     # TumblingWindow
    | HOPPING SIZE timeUnit ADVANCE BY timeUnit  # HoppingWindow
    | SLIDING SIZE timeUnit                       # SlidingWindow
    | SESSION GAP timeUnit                         # SessionWindow
    ;

timeUnit
    : NUMBER (MILLISECONDS | SECONDS | MINUTES | HOURS | DAYS)
    ;

// Join Specifications
joinSpec
    : streamJoin
    | tableJoin
    | streamStreamJoin
    ;

streamJoin: joinType? STREAM topicName ON joinCondition;
tableJoin: joinType? LOOKUP TABLE topicName ON joinCondition;
streamStreamJoin: joinType? STREAM topicName ON joinCondition WITHIN timeUnit;

joinType
    : INNER
    | LEFT
    | RIGHT
    | FULL
    ;

joinCondition: expression;

// Aggregation Functions
aggFunctionList: aggFunction (',' aggFunction)*;

aggFunction
    : COUNT '(' '*' ')' (AS alias)?                          # CountAll
    | COUNT '(' fieldName ')' (AS alias)?                    # CountField
    | SUM '(' fieldName ')' (AS alias)?                      # Sum
    | AVG '(' fieldName ')' (AS alias)?                      # Avg
    | MIN '(' fieldName ')' (AS alias)?                      # Min
    | MAX '(' fieldName ')' (AS alias)?                      # Max
    | FIRST '(' fieldName ')' (AS alias)?                    # First
    | LAST '(' fieldName ')' (AS alias)?                     # Last
    | COLLECT '(' fieldName ')' (AS alias)?                  # Collect
    | STDDEV '(' fieldName ')' (AS alias)?                   # StdDev
    | PERCENTILE '(' fieldName ',' NUMBER ')' (AS alias)?    # Percentile
    ;

// Mapping Operations
mappingList: mapping (',' mapping)*;

mapping
    : SET fieldName '=' expression
    | RENAME fieldName TO fieldName
    | DROP fieldName
    | CAST fieldName AS dataType
    ;

dataType
    : STRING_TYPE
    | INT_TYPE
    | LONG_TYPE
    | FLOAT_TYPE
    | DOUBLE_TYPE
    | BOOLEAN_TYPE
    | TIMESTAMP_TYPE
    | JSON_TYPE
    ;

// Branching
branchSpec: CASE branchCase+ (DEFAULT pipelineStage*)?;

branchCase: WHEN expression THEN pipelineStage*;

// Pattern Detection
patternSpec: patternDef+ DETECT AS alias;

patternDef: IDENTIFIER '=' patternExpression;

patternExpression
    : patternAtom
    | patternExpression FOLLOWED BY patternExpression
    | patternExpression OR patternExpression
    | patternExpression AND patternExpression
    | '(' patternExpression ')'
    ;

patternAtom
    : expression (WITHIN timeUnit)? (AFTER patternRef)?
    ;

patternRef: IDENTIFIER;

// Sessionization
sessionSpec: BY fieldList GAP timeUnit (TIMEOUT timeUnit)?;

// Union
unionSpec: UNION ALL? '(' pipeline (',' pipeline)* ')';

// Expand (for arrays/maps)
expandSpec: EXPAND fieldName AS alias (',' fieldName AS alias)*;

// Expressions (enhanced)
expression
    : LPAREN expression RPAREN                                # ParenExpression
    | NOT expression                                           # NotExpression
    | expression AND expression                                # AndExpression
    | expression OR expression                                 # OrExpression
    | predicate                                                 # PredicateExpr
    | mathExpression                                           # MathExpr
    | functionCall                                             # FunctionExpr
    | caseExpression                                           # CaseExpr
    ;

mathExpression
    : fieldName                                                 # FieldRef
    | literal                                                   # LiteralExpr
    | '-' mathExpression                                        # UnaryMinus
    | mathExpression '*' mathExpression                         # Multiply
    | mathExpression '/' mathExpression                         # Divide
    | mathExpression '+' mathExpression                         # Add
    | mathExpression '-' mathExpression                         # Subtract
    | '(' mathExpression ')'                                    # ParenMath
    ;

functionCall
    : functionName '(' (expression (',' expression)*)? ')'
    ;

functionName
    : IDENTIFIER
    | BUILTIN_FUNC
    ;

caseExpression
    : CASE WHEN expression THEN expression (WHEN expression THEN expression)* (ELSE expression)? END
    ;

predicate
    : fieldName comparator value                                # ComparisonPredicate
    | fieldName IN valueList                                    # InPredicate
    | fieldName BETWEEN value AND value                         # BetweenPredicate
    | fieldName IS NULL                                         # IsNullPredicate
    | fieldName IS NOT NULL                                     # IsNotNullPredicate
    | fieldName LIKE pattern                                    # LikePredicate
    | fieldName REGEXP pattern                                  # RegexPredicate
    | EXISTS '(' query ')'                                      # ExistsPredicate
    ;

comparator
    : EQ | NEQ | LT | LTE | GT | GTE
    ;

value
    : literal
    | fieldName
    | subquery
    ;

valueList
    : LPAREN literal (',' literal)* RPAREN
    ;

subquery: '(' query ')';

pattern: STRING;

literal
    : STRING
    | NUMBER
    | BOOLEAN
    | NULL
    | TIMESTAMP
    | ARRAY
    | MAP
    | JSON
    ;

// Complex literals
TIMESTAMP: [0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(Z|[+-][0-9]{2}:[0-9]{2})?;
ARRAY: '[' (literal (',' literal)*)? ']';
MAP: '{' (STRING ':' literal (',' STRING ':' literal)*)? '}';
JSON: '{' ~[}]* '}';

// Keywords (case-insensitive)
FROM        : [Ff][Rr][Oo][Mm];
TO          : [Tt][Oo];
WHERE       : [Ww][Hh][Ee][Rr][Ee];
UNIQUE      : [Uu][Nn][Ii][Qq][Uu][Ee];
BY          : [Bb][Yy];
FILTER      : [Ff][Ii][Ll][Tt][Ee][Rr];
PROJECT     : [Pp][Rr][Oo][Jj][Ee][Cc][Tt];
MAP         : [Mm][Aa][Pp];
WINDOW      : [Ww][Ii][Nn][Dd][Oo][Ww];
GROUP       : [Gg][Rr][Oo][Uu][Pp];
AGGREGATE   : [Aa][Gg][Gg][Rr][Ee][Gg][Aa][Tt][Ee];
JOIN        : [Jj][Oo][Ii][Nn];
BRANCH      : [Bb][Rr][Aa][Nn][Cc][Hh];
PATTERN     : [Pp][Aa][Tt][Tt][Ee][Rr][Nn];
SESSIONIZE  : [Ss][Ee][Ss][Ss][Ii][Oo][Nn][Ii][Zz][Ee];
UNION       : [Uu][Nn][Ii][Oo][Nn];
EXPAND      : [Ee][Xx][Pp][Aa][Nn][Dd];
TUMBLING    : [Tt][Uu][Mm][Bb][Ll][Ii][Nn][Gg];
HOPPING     : [Hh][Oo][Pp][Pp][Ii][Nn][Gg];
SLIDING     : [Ss][Ll][Ii][Ii][Dd][Ii][Nn][Gg];
SESSION     : [Ss][Ee][Ss][Ss][Ii][Oo][Nn];
SIZE        : [Ss][Ii][Zz][Ee];
ADVANCE     : [Aa][Dd][Vv][Aa][Nn][Cc][Ee];
GAP         : [Gg][Aa][Pp];
TIMEOUT     : [Tt][Ii][Mm][Ee][Oo][Uu][Tt];
LOOKUP      : [Ll][Oo][Oo][Kk][Uu][Pp];
TABLE       : [Tt][Aa][Bb][Ll][Ee];
STREAM      : [Ss][Tt][Rr][Ee][Aa][Mm];
WITHIN      : [Ww][Ii][Tt][Hh][Ii][Nn];
INNER       : [Ii][Nn][Nn][Ee][Rr];
LEFT        : [Ll][Ee][Ff][Tt];
RIGHT       : [Rr][Ii][Gg][Hh][Tt];
FULL        : [Ff][Uu][Ll][Ll];
ON          : [Oo][Nn];
AS          : [Aa][Ss];
SET         : [Ss][Ee][Tt];
RENAME      : [Rr][Ee][Nn][Aa][Mm][Ee];
DROP        : [Dd][Rr][Oo][Pp];
CAST        : [Cc][Aa][Ss][Tt];
CASE        : [Cc][Aa][Ss][Ee];
WHEN        : [Ww][Hh][Ee][Nn];
THEN        : [Tt][Hh][Ee][Nn];
ELSE        : [Ee][Ll][Ss][Ee];
END         : [Ee][Nn][Dd];
DETECT      : [Dd][Ee][Tt][Ee][Cc][Tt];
FOLLOWED    : [Ff][Oo][Ll][Ll][Oo][Ww][Ee][Dd];
AFTER       : [Aa][Ff][Tt][Ee][Rr];
ALL         : [Aa][Ll][Ll];
EXISTS      : [Ee][Xx][Ii][Ss][Tt][Ss];
REGEXP      : [Rr][Ee][Gg][Ee][Xx][Pp];
AND         : [Aa][Nn][Dd];
OR          : [Oo][Rr];
NOT         : [Nn][Oo][Tt];
IN          : [Ii][Nn];
BETWEEN     : [Bb][Ee][Tt][Ww][Ee][Ee][Nn];
IS          : [Ii][Ss];
NULL        : [Nn][Uu][Ll][Ll];
LIKE        : [Ll][Ii][Kk][Ee];

// Data Types
STRING_TYPE : [Ss][Tt][Rr][Ii][Nn][Gg];
INT_TYPE    : [Ii][Nn][Tt];
LONG_TYPE   : [Ll][Oo][Nn][Gg];
FLOAT_TYPE  : [Ff][Ll][Oo][Aa][Tt];
DOUBLE_TYPE : [Dd][Oo][Uu][Bb][Ll][Ee];
BOOLEAN_TYPE: [Bb][Oo][Oo][Ll][Ee][Aa][Nn];
TIMESTAMP_TYPE: [Tt][Ii][Mm][Ee][Ss][Tt][Aa][Mm][Pp];
JSON_TYPE   : [Jj][Ss][Oo][Nn];

// Time Units
MILLISECONDS: [Mm][Ss] | [Mm][Ii][Ll][Ll][Ii][Ss][Ee][Cc][Oo][Nn][Dd][Ss]?;
SECONDS     : [Ss] | [Ss][Ee][Cc][Oo][Nn][Dd][Ss]?;
MINUTES     : [Mm] | [Mm][Ii][Nn][Uu][Tt][Ee][Ss]?;
HOURS       : [Hh] | [Hh][Oo][Uu][Rr][Ss]?;
DAYS        : [Dd] | [Dd][Aa][Yy][Ss]?;

// Built-in Functions
BUILTIN_FUNC
    : [Cc][Oo][Aa][Ll][Ee][Ss][Cc][Ee]
    | [Gg][Rr][Ee][Aa][Tt][Ee][Ss][Tt]
    | [Ll][Ee][Aa][Ss][Tt]
    | [Ff][Ii][Rr][Ss][Tt]
    | [Dd][Aa][Tt][Ee][Dd][Ii][Ff][Ff]
    | [Ff][Rr][Oo][Mm][Uu][Nn][Ii][Xx][Tt][Ii][Mm][Ee]
    | [Tt][Oo][Uu][Nn][Ii][Xx][Tt][Ii][Mm][Ee]
    | [Uu][Rr][Ll][Dd][Ee][Cc][Oo][Dd][Ee]
    | [Uu][Rr][Ll][Ee][Nn][Cc][Oo][Dd][Ee]
    ;

// Operators
EQ          : '=';
NEQ         : '!=' | '<>';
LT          : '<';
LTE         : '<=';
GT          : '>';
GTE         : '>=';
PIPE        : '|>';
ARROW       : '->';

// Punctuation
LPAREN      : '(';
RPAREN      : ')';
LBRACK      : '[';
RBRACK      : ']';
LBRACE      : '{';
RBRACE      : '}';
COMMA       : ',';
COLON       : ':';
SEMICOLON   : ';';
DOT         : '.';

// Literals
BOOLEAN     : [Tt][Rr][Uu][Ee] | [Ff][Aa][Ll][Ss][Ee];
STRING      : DQUOTE (ESC | ~["\\])* DQUOTE | SQUOTE (ESC | ~['\\])* SQUOTE;
NUMBER      : '-'? INT '.' [0-9]+ EXP? | '-'? INT EXP | '-'? INT;

// Kafka Topic Names
IDENTIFIER  : [a-zA-Z0-9._-]+;
QUOTED_TOPIC
    : '`' ~[`]* '`'
    | '"' ~["]* '"'
    ;

// Fragments
fragment DQUOTE     : '"';
fragment SQUOTE     : '\'';
fragment ESC        : '\\' (["\\/bfnrt] | UNICODE);
fragment UNICODE    : 'u' HEX HEX HEX HEX;
fragment HEX        : [0-9a-fA-F];
fragment INT        : '0' | [1-9] [0-9]*;
fragment EXP        : [Ee] [+\-]? INT;

// Skip whitespace
WS          : [ \t\r\n]+ -> skip;
COMMENT     : '--' ~[\r\n]* -> skip;
``