/*
 * [The "BSD license"]
 * Copyright (c) 2015 Clemson University
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. The name of the author may not be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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