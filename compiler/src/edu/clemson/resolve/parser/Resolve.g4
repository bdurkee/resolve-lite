grammar Resolve;

module
    :   precisModule
  //  |   conceptModule
    ;

// precis module

precisModule
    :   'Precis' name=Identifier ';'
       // (importList)?
       // (precisBlock)?
        'end' closename=Identifier ';' EOF
    ;

/*precisBlock
    :   ( mathDefinitionDecl
        | mathTypeTheoremDecl
        )+
    ;
*/
// uses, imports

importList
    :   'uses' Identifier (',' Identifier)* ';'
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

CharacterLiteral
    :   '\'' SingleCharacter '\''
    ;

StringLiteral
    :   '\"' StringCharacters? '\"'
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