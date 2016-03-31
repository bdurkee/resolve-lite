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
    |   precisExtModuleDecl
    ;

// precis

precisModuleDecl
    :   'Precis' name=ID ';'
        (usesList)?
        precisBlock
        'end' closename=ID ';' EOF
    ;

// precis extensions

precisExtModuleDecl
    :   'Precis' 'Extension' name=ID 'for' precis=ID
        ('with' precisExt=ID)? ';'
        precisBlock
        'end' closename=ID ';'
    ;

precisBlock
    :   ( mathStandardDefnDecl
        | mathCategoricalDefnDecl
        | mathInductiveDefnDecl
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

mathDefnSig
    :   mathPrefixDefnSigs
    |   mathInfixDefnSig
    |   mathOutfixDefnSig
    |   mathPostfixDefnSig
    ;

mathPrefixDefnSig
    :   mathSymbolName (',' mathSymbolName)* ('('
                mathVarDeclGroup (',' mathVarDeclGroup)* ')')?
                ':' mathTypeExp
    ;

mathPrefixDefnSigs
    :   mathPrefixDefnSig
        (',' mathPrefixDefnSig)*
    ;

mathInfixDefnSig
    :   '(' mathVarDecl ')' name=mathSymbolName
        '(' mathVarDecl ')' ':' mathTypeExp
    ;

mathOutfixDefnSig
    :   leftSym=('|'|'||'|'<'|'⎝'|'⟨') mathVarDecl
        rightSym=('⟩'|'⎠'|'|'|'||'|'>') ':' mathTypeExp
    ;

mathPostfixDefnSig
    :   mathVarDecl lop='[' mathVarDecl rop=']' ':' mathTypeExp
    ;

mathSymbolName
    : ( ID |
      ('o'|'true'|'false'|INT|'+'|'-'|'*'|'/'|'>'|'≤'|
       '<'|'<='|'>='|'≥'|'not'|'⌐'|'≼'|'ϒ'|'∪₊'|'≤ᵤ'))
    ;

mathCategoricalDefnDecl
    :   'Categorical' 'Definition' 'for' mathPrefixDefnSigs
        'is' mathAssertionExp ';'
    ;

mathStandardDefnDecl
    :   ('Implicit')? 'Definition' mathDefnSig
        ('is' body=mathAssertionExp)? ';'
    ;

mathInductiveDefnDecl
    :   'Inductive' 'Definition' mathDefnSig 'is'
        '(i.)' mathAssertionExp ';'
        '(ii.)' mathAssertionExp ';'
    ;

mathVarDeclGroup
    :   ID (',' ID)* ':' mathTypeExp
    ;

mathVarDecl
    :   ID ':' mathTypeExp
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
    :   q=(FORALL|EXISTS|'∀'|'∃') mathVarDeclGroup ',' mathAssertionExp
    ;

//TODO: Add rest of alts
mathExp
    :   name=mathExp '(' mathExp (',' mathExp)*')'      #mathPrefixAppExp
    |   mathExp mathMultOpExp mathExp                   #mathInfixAppExp
    |   mathExp mathBooleanOpExp mathExp                #mathInfixAppExp
    |   <assoc=right> mathExp mathArrowOpExp mathExp    #mathInfixAppExp
    |   mathExp mathEqualityOpExp mathExp               #mathInfixAppExp
    |   mathExp mathImpliesOpExp mathExp                #mathInfixAppExp
    |   ID ':' mathExp                                  #mathTypeAssertionExp
    |   '(' mathExp ')'                                 #mathNestedExp
    |   mathPrimeExp                                    #mathPrimaryExp
    ;

/** Because operators are now first class citizens with expressions all of their
 *  own (as opposed to being simple strings embedded within the context of some application)
 *  we need these intermediate rules to convince antlr to create visitable, *annotatable*,
 *  rule contexts for these guys -- which greatly eases the creation (and subsequent typing) of an AST.
 *  No longer a need to pass special maps around from Token -> MathType, etc --
 *  now we just need to visit and annotate these names like any other node).
 */
mathMultOpExp : (qualifier=ID '::')? ('*'|'/'|'%') ;
mathAddOp : (qualifier=ID '::')? ('+'|'-'|'~');
mathJoiningOp : (qualifier=ID '::')? ('o'|'union'|'∪'|'∪₊'|'intersect'|'∩'|'∩₊');
mathArrowOpExp : (qualifier=ID '::')? ('->'|'⟶') ;
mathRelationalOpExp : (qualifier=ID '::')? ('<'|'>'|'<='|'≤'|'≤ᵤ'|'>='|'≥');
mathEqualityOpExp : (qualifier=ID '::')? ('='|'/='|'≠');
mathSetContainmentOpExp : (qualifier=ID '::')? ('is_in'|'is_not_in'|'∈'|'∉');
mathImpliesOpExp : (qualifier=ID '::')? ('implies');
mathBooleanOpExp : (qualifier=ID '::')? ('and'|'or'|'iff');

mathPrimeExp
    :   mathLiteralExp
    |   mathCartProdExp
    |   mathSymbolExp
    |   mathOutfixApplyExp
    |   mathSetComprehensionExp
    |   mathSetExp
    |   mathLambdaExp
    |   mathAlternativeExp
    ;

mathLiteralExp
    :   (qualifier=ID '::')? ('true'|'false')   #mathBooleanLiteralExp
    |   (qualifier=ID '::')? num=INT            #mathIntegerLiteralExp
    ;

mathCartProdExp
    :   'Cart_Prod' (mathVarDeclGroup ';')+ 'end'
    ;

mathSymbolExp
    :   (incoming='@')? (qualifier=ID '::')? name=mathSymbolName
    ;

mathOutfixApplyExp
    :   lop='|' mathExp rop='|'
    |   lop='||' mathExp rop='||'
    |   lop='<' mathExp rop='>'
    |   lop='⟨' mathExp rop='⟩'
    |   lop='[' mathExp rop=']'
    |   lop='⎝' mathExp rop='⎠'
    ;

mathSetComprehensionExp
    :   '{' mathVarDecl '|' mathAssertionExp '}'
    ;

mathSetExp
    :    '{' (mathExp (',' mathExp)*)? '}'
    ;

mathLambdaExp
    :   ('lambda'|'λ') '(' mathVarDeclGroup
        (',' mathVarDeclGroup)* ')' '.' '(' mathExp ')'
    ;

mathAlternativeExp
    :   '{{' (mathAlternativeItemExp)+ '}}'
    ;

mathAlternativeItemExp
    :   result=mathExp ('if' condition=mathExp ';' | 'otherwise' ';')
    ;

progExp:   'progExp';
facilityDecl:   'Facility' name=ID;

//prog ops here so I can use switch statements in the code
NOT : 'not' ;
EQUALS : '=' ;
NEQUALS : '/=' ;
LT : '<' ;
LTE : '<=' ;
GT : '>' ;
GTE : '>=' ;
MOD : '%' ;
TRUE : 'true' ;
FALSE : 'false' ;
AND : 'and' ;
OR : 'or' ;
PLUS : '+' ;
MINUS : '-' ;
MULT : '*' ;
DIV : '/' ;

FORALL : ('Forall'|'forall');
EXISTS : ('Exists'|'exists');
LINE_COMMENT : '//' .*? ('\n'|EOF)	-> channel(HIDDEN) ;
COMMENT      : '/*' .*? '*/'    	-> channel(HIDDEN) ;

ID  : [a-zA-Z_] [a-zA-Z0-9_]* ;
INT : [0-9]+ ;

CHAR: '\'' . '\'' ;
STRING :  '"' (ESC | ~["\\])* '"' ;
fragment ESC :   '\\' ["\bfnrt] ;
WS : [ \t\n\r]+ -> channel(HIDDEN) ;