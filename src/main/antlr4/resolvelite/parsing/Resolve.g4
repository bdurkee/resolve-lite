grammar Resolve;

module
    :   'Precis' name=Identifier ';'
        moduleBlock
        'end' ';'
    ;

moduleBlock
    :   definitionDecl
    ;

definitionDecl
    :   'Definition' name=Identifier (definitionParameterList)? ':'
        mathTypeExp ';'
    ;

definitionParameterList
    :   '(' ')'
    ;

variableDecl
    :   name=Identifier ':' mathTypeExp
    ;

mathTypeExp
    :   mathExp
    ;

mathExp
    :   primary                                     #primaryExp
    |   op=('+'|'-'|'~'|'not') mathExp              #unaryExp
    |   mathExp op=('*'|'/') mathExp                #infixExp
    |   mathExp op=('+'|'-') mathExp                #infixExp
    |   mathExp op=('..'|'->') mathExp              #infixExp
    |   mathExp op=('is_in'|'is_not_in') mathExp    #infixExp
    |   mathExp op=('<='|'>='|'>'|'<') mathExp      #infixExp
    |   mathExp op=('='|'/=') mathExp               #infixExp
    |   mathExp op='implies' mathExp                #infixExp
    |   mathExp op=('and'|'or') mathExp             #infixExp
    |   mathExp (':') mathExp                       #typeAssertionExp
    ;

primary
    :   literalExp
    |   functionExp
    |   variableExp
    ;

literalExp
    :   BooleanLiteral      #booleanExp
    |   IntegerLiteral      #integerExp
    ;

functionExp
    :   name=Identifier '(' mathExp (',' mathExp)* ')'
    ;

variableExp
    :   (qualifier=Identifier '::')? name=Identifier
    ;

// literal rules and fragments

BooleanLiteral
    :   'true'
    |   'false'
    |   'B'
    ;

IntegerLiteral
    :   DecimalIntegerLiteral
    ;

fragment
StringCharacters
    :   StringCharacter+
    ;

fragment
StringCharacter
    :   ~["\\]
    ;

fragment
DecimalIntegerLiteral
    :   '0'
    |   NonZeroDigit (Digits)?
    ;

fragment
Digits
    :   Digit (Digit)*
    ;

fragment
Digit
    :   '0'
    |   NonZeroDigit
    ;

fragment
NonZeroDigit
    :   [1-9]
    ;

fragment
SingleCharacter
    :   ~['\\]
    ;

// whitespace, identifier rules, and comments

Identifier
    :   Letter LetterOrDigit*
    ;

Letter
    :   [a-zA-Z$_]
    ;

LetterOrDigit
    :   [a-zA-Z0-9$_]
    ;

SPACE
    :  [ \t\r\n\u000C]+ -> skip
    ;

COMMENT
    :   '(*' .*? '*)' -> skip
    ;

LINE_COMMENT
    :   '--' ~[\r\n]* -> skip
    ;