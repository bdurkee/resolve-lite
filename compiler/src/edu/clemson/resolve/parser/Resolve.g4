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
 * documentation and/or other nterials provided with the distribution.
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

moduleDecl
    :   precisModuleDecl
    ;

precisModuleDecl
    :   'Precis' name=ID ';'
        (usesList)?
        precisBlock
        'end' closename=ID ';' EOF
    ;

precisBlock
    :   ( mathDefinitionDecl
        | mathCategoricalDefinitionDecl
        | mathInductiveDefinitionDecl
        | mathTheoremDecl
        )*
    ;

// uses, imports

usesList
    :   'uses' ID (',' ID)* ';'
    ;

// math constructs

mathTheoremDecl
    :   ('Corollary'|'Theorem') name=ID ':' mathAssertionExp ';'
    ;

mathDefinitionSig
    :   mathPrefixDefinitionSig
    |   mathInfixDefinitionSig
    ;

mathInfixDefinitionSig
    :   '(' mathVariableDecl ')' name=mathSymbolName
        '(' mathVariableDecl ')' ':' mathTypeExp
    ;

mathPrefixDefinitionSig
    :   name=mathSymbolName ('('
                mathVariableDeclGroup (',' mathVariableDeclGroup)* ')')?
                ':' mathTypeExp
    ;

mathSymbolName
    :   ID
    |   ('+'|'-'|'...'|'/'|'\\'|'|'|'||'|'<'|'>'|'o'|'*'|'>='|'<='|INT|'not')
    |   '|' '...' '|'
    |   '<' '...' '>'
    |   '||' '...' '||'
    |   '\\' '...' '//'
    ;

mathCategoricalDefinitionDecl
    :   'Categorical' 'Definition' 'for'
        mathPrefixDefinitionSig (',' mathPrefixDefinitionSig)+
        'is' mathAssertionExp ';'
    ;

mathDefinesDefinitionDecl
    :   'Defines' ID (',' ID)* ':' mathTypeExp ';'
    ;

mathDefinitionDecl
    :   ('Implicit')? 'Definition' mathDefinitionSig
        ('is' mathAssertionExp)? ';'
    ;

mathInductiveDefinitionDecl
    :   'Inductive' 'Definition' mathDefinitionSig 'is'
        '(i.)' mathAssertionExp ';'
        '(ii.)' mathAssertionExp ';'
    ;

mathVariableDeclGroup
    :   ID (',' ID)* ':' mathTypeExp
    ;

mathVariableDecl
    :   ID ':' mathTypeExp
    ;

// mathematical clauses

requiresClause
    :   'requires' mathAssertionExp (entailsClause)? ';'
    ;

ensuresClause
    :   'ensures' mathAssertionExp ';'
    ;

constraintClause
    :   'constraint' mathAssertionExp ';'
    ;

conventionClause
    :   'convention' mathAssertionExp (entailsClause)? ';'
    ;

correspondenceClause
    :   'correspondence' mathAssertionExp ';'
    ;

entailsClause
    :   'which_entails' mathExp (',' mathExp)* ':' mathTypeExp
    ;

// mathematical expressions

mathTypeExp
    :   mathExp
    ;

mathAssertionExp
    :   mathExp
    |   mathQuantifiedExp
    ;

mathQuantifiedExp
    :   q=(FORALL|EXISTS) mathVariableDeclGroup ',' mathAssertionExp
    ;

mathExp
    :   functionExp=mathExp '(' mathExp (',' mathExp)* ')'  #mathPrefixApplyExp
    |   mathExp op=('*'|'/'|'~') mathExp                    #mathInfixApplyExp
    |   mathExp op=('+'|'-') mathExp                        #mathInfixApplyExp
    |   mathExp op=('..'|'->') mathExp                      #mathInfixApplyExp
    |   mathExp op=('o'|'union'|'intersect') mathExp        #mathInfixApplyExp
    |   mathExp op=('is_in'|'is_not_in') mathExp            #mathInfixApplyExp
    |   mathExp op=('<='|'>='|'>'|'<') mathExp              #mathInfixApplyExp
    |   mathExp op=('='|'/=') mathExp                       #mathInfixApplyExp
    |   mathExp op='implies' mathExp                        #mathInfixApplyExp
    |   mathExp op=('and'|'or') mathExp                     #mathInfixApplyExp
    |   mathExp op=':' mathTypeExp                          #mathTypeAssertionExp
    |   '(' mathAssertionExp ')'                            #mathNestedExp
    |   mathPrimaryExp                                      #mathPrimeExp
    ;

mathPrimaryExp
    :   mathLiteralExp
    |   mathCrossTypeExp
    |   mathSymbolExp
    |   mathOutfixExp
    |   mathSetComprehensionExp
    |   mathSetExp
    |   mathLambdaExp
    |   mathAlternativeExp
    |   mathTupleExp
    |   mathSegmentsExp
    ;

mathLiteralExp
    :   (qualifier=ID '::')? ('true'|'false')       #mathBooleanLiteralExp
    |   (qualifier=ID '::')? num=INT                #mathIntegerLiteralExp
    ;

mathCrossTypeExp
    :   'Cart_Prod' (mathVariableDeclGroup ';')+ 'end'
    ;

mathSymbolExp
    :   (incoming='@')? (qualifier=ID '::')? name=mathSymbolName
    ;

mathOutfixExp
    :   lop='<' mathExp rop='>'
    |   lop='|' mathExp rop='|'
    |   lop='||' mathExp rop='||'
    ;

mathSetComprehensionExp
    :   '{' mathVariableDecl '|' mathAssertionExp '}'
    ;

mathSetExp
    :    '{' (mathExp (',' mathExp)*)? '}'
    ;

mathLambdaExp
    :   'lambda' '(' mathVariableDeclGroup
        (',' mathVariableDeclGroup)* ')' '.' '(' mathExp ')'
    ;

mathAlternativeExp
    :   '{{' (mathAlternativeItemExp)+ '}}'
    ;

mathAlternativeItemExp
    :   result=mathExp ('if' condition=mathExp ';' | 'otherwise' ';')
    ;

mathTupleExp
    :   '(' mathExp (',' mathExp)+ ')'
    ;

//Segments can end in an application but they can't contain one in the middle ..
//hopefully :) ...
mathSegmentsExp
    :   mathSymbolExp ('.' mathSymbolExp)+ ('(' mathExp (',' mathExp)* ')')?
    ;

FORALL : ('Forall') ;
EXISTS : ('Exists') ;

LINE_COMMENT : '//' .*? ('\n'|EOF)	-> channel(HIDDEN) ;
COMMENT      : '/*' .*? '*/'    	-> channel(HIDDEN) ;

ID  : [a-zA-Z_] [a-zA-Z0-9_]* ;
INT : [0-9]+ ;

CHAR: '\'' . '\'' ;
STRING :  '"' (ESC | ~["\\])* '"' ;
fragment ESC :   '\\' ["\bfnrt] ;
WS : [ \t\n\r]+ -> channel(HIDDEN) ;