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
parser grammar Resolve;

options {
	tokenVocab=ResolveLexer;
}

module
    :   precisModule
    |   conceptModule
    ;

usesList
    :   USES ID (COMMA ID)* SEMI
    ;

conceptModule
    :   CONCEPT name=ID SEMI
        (usesList)?
        END closename=ID SEMI EOF
    ;

precisModule
    :   PRECIS name=ID SEMI
        (usesList)?
        (precisBlock)?
        END closename=ID SEMI EOF
    ;

precisBlock
    :   ( mathDefinitionDecl
        | mathCategoricalDefinitionDecl
        | mathInductiveDefinitionDecl
        )+
    ;

//The '(COMMA ID)?' is reserved for the variable we're inducting over
//for instances in which the sig is used in the context of an inductive defn
mathDefinitionSig
    :   name=mathSymbol (LPAREN
            mathVariableDeclGroup (COMMA mathVariableDeclGroup)*
            (COMMA inductionVar=ID)? RPAREN)? COLON mathTypeExp
    ;

mathCategoricalDefinitionDecl
    :   CATEGORICAL DEFINITION FOR
        mathDefinitionSig (COMMA mathDefinitionSig)+
        IS mathAssertionExp SEMI
    ;

mathDefinitionDecl
    :   (IMPLICIT)? DEFINITION mathDefinitionSig
        (IS mathAssertionExp)? SEMI
    ;

mathInductiveDefinitionDecl
    :   INDUCTIVE DEFINITION ON mathVariableDecl OF mathDefinitionSig (IS
        BASE_CASE mathAssertionExp SEMI
        INDUCT_CASE mathAssertionExp)? SEMI
    ;

mathSymbol
    :   (PLUS|MINUS|DIVIDE|MULT|BOOL|INT)
    |   ID
    ;

mathVariableDeclGroup
    :   ID (COMMA ID)* COLON mathTypeExp
    ;

mathVariableDecl
    :   ID COLON mathTypeExp
    ;

facilityDecl
    :   FACILITY name=ID IS spec=ID
        (specArgs=moduleArgumentList)? (externally=EXTERNALLY)? IMPLEMENTED
        BY impl=ID (implArgs=moduleArgumentList)? SEMI
    ;

moduleArgumentList
    :   LPAREN moduleArgument (COMMA moduleArgument)* RPAREN
    ;

//Todo: Placeholder. I don't want to add the whole prog exp tree right now.
//I want to focus on the math.
moduleArgument
    :   ID
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
    :   q=(FORALL|EXISTS) mathVariableDeclGroup COMMA mathAssertionExp
    ;

mathExp
    :   mathPrimaryExp                                  #mathPrimeExp
    |   op=(PLUS|MINUS|NOT) mathExp                     #mathUnaryExp
    |   mathExp op=(MULT|DIVIDE|TILDE) mathExp          #mathInfixExp
    |   mathExp op=(PLUS|MINUS) mathExp                 #mathInfixExp
    |   mathExp op=(RANGE|RARROW) mathExp               #mathInfixExp
    |   mathExp op=(LTE|GTE|GT|LT) mathExp              #mathInfixExp
    |   mathExp op=(EQUALS|NEQUALS) mathExp             #mathInfixExp
    |   mathExp op=IMPLIES mathExp                      #mathInfixExp
    |   mathExp op=(AND|OR) mathExp                     #mathInfixExp
    |   mathExp op=COLON mathExp                        #mathTypeAssertionExp
    |   LPAREN mathAssertionExp RPAREN                  #mathNestedExp
    ;

mathPrimaryExp
    :   mathLiteralExp
    |   mathFunctionApplicationExp
    |   mathSegmentsExp
    |   mathOutfixExp
    |   mathSetExp
    |   mathTupleExp
 //   |   mathAlternativeExp
 //   |   mathLambdaExp
    ;

mathLiteralExp
    :   BOOL        #mathBooleanExp
    |   INT         #mathIntegerExp
    ;

mathFunctionApplicationExp
    :   (AT)? name=ID (LPAREN mathExp (COMMA mathExp)* RPAREN)+ #mathFunctionExp
    |   (AT)? (qualifier=ID COLONCOLON)? name=ID #mathVariableExp
    ;

mathOutfixExp
    :   lop=LT mathExp rop=GT
    |   lop=BAR mathExp rop=BAR
    |   lop=DBL_BAR mathExp rop=DBL_BAR
    ;

mathSetExp
    :   LBRACE mathVariableDecl BAR mathAssertionExp RBRACE  #mathSetBuilderExp//Todo
    |   LBRACE (mathExp (COMMA mathExp)*)? RBRACE         #mathSetCollectionExp
    ;

/*mathLambdaExp
    :   'lambda' definitionParameterList '.' '(' mathExp ')'
    ;

mathAlternativeExp
    :   '{{' (mathAlternativeItemExp)+ '}}'
    ;

mathAlternativeItemExp
    :   result=mathExp ('if' condition=mathExp ';' | 'otherwise' ';')
    ;*/

mathTupleExp
    :   LPAREN mathExp (COMMA mathExp)+ RPAREN
    ;

mathSegmentsExp
    :   mathFunctionApplicationExp (DOT mathFunctionApplicationExp)+
    ;