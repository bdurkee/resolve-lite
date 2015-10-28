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

module
    :   precisModule
/*    |   conceptModule
    |   conceptImplModule
    |   facilityModule
    |   enhancementImplModule
    |   enhancementModule*/
    ;

/*conceptModule
    :   CONCEPT name=ID (LT genericType (COMMA genericType)* GT)?
        (specModuleParameterList)? SEMI
        (usesList)?
        (requiresClause)?
        (conceptBlock)
        END closename=ID SEMI EOF
    ;

conceptBlock
    :   ( typeModelDecl
        | operationDecl
        | mathDefinitionDecl
        | mathDefinesDefinitionDecl
        | mathStateVariableDeclGroup
        | specModuleInit
        | constraintClause
        )*
    ;

mathStateVariableDeclGroup
    :   VAR mathVariableDeclGroup SEMI
    ;

// enhancement module

enhancementModule
    :   EXTENSION name=ID (specModuleParameterList)?
        FOR concept=ID SEMI
        (usesList)?
        (requiresClause)?
        (enhancementBlock)
        END closename=ID SEMI EOF
    ;

enhancementBlock
    :   ( operationDecl
        | typeModelDecl
        | mathDefinitionDecl
        )*
    ;

// implementation modules

conceptImplModule
    :   IMPLEMENTATION name=ID (implModuleParameterList)?
        FOR concept=ID SEMI
        (usesList)?
        (requiresClause)?
        (implBlock)
        END closename=ID SEMI EOF
    ;

enhancementImplModule
   :   IMPLEMENTATION name=ID (implModuleParameterList)?
       FOR enhancement=ID OF concept=ID SEMI
       (usesList)?
       (requiresClause)?
       (implBlock)
       END closename=ID SEMI EOF
   ;

implBlock
    :   ( typeRepresentationDecl
        | operationProcedureDecl
        | procedureDecl
        | facilityDecl
        )*
    ;

// facility modules

facilityModule
    :   FACILITY name=ID SEMI
        (usesList)?
        (requiresClause)?
        (facilityBlock)
        END closename=ID SEMI EOF
    ;

facilityBlock
    :   ( mathDefinitionDecl
        | operationProcedureDecl
        | facilityDecl
        | typeRepresentationDecl
        )*
    ;*/

precisModule
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

// parameter and parameter-list related rules

/*operationParameterList
    :   '(' (parameterDeclGroup (';' parameterDeclGroup)*)?  ')'
    ;

specModuleParameterList
    :   '(' specModuleParameterDecl (';' specModuleParameterDecl)* ')'
    ;

implModuleParameterList
    :   '(' implModuleParameterDecl (';' implModuleParameterDecl)* ')'
    ;

specModuleParameterDecl
    :   parameterDeclGroup
    |   mathDefinitionDecl
    ;

implModuleParameterDecl
    :   parameterDeclGroup
    |   operationDecl
    ;

parameterDeclGroup
    :   parameterMode ID (COMMA ID)* COLON type
    ;

parameterMode
    :   ( ALTERS
        | UPDATES
        | CLEARS
        | RESTORES
        | PRESERVES
        | REPLACES
        | EVALUATES )
    ;

variableDeclGroup
    :   VAR ID (COMMA ID)* COLON type SEMI
    ;

// statements

stmt
    :   assignStmt
    |   swapStmt
    |   callStmt
    |   whileStmt
    |   ifStmt
    ;

assignStmt
    :   left=progVarExp ASSIGN right=progExp SEMI
    ;

swapStmt
    :   left=progVarExp SWAP right=progVarExp SEMI
    ;

//semantically restrict things like 1++ (<literal>++/--, etc)
callStmt
    :   progExp SEMI
    ;

whileStmt
    :   WHILE progExp
        (MAINTAINING mathExp SEMI)?
        (DECREASING mathExp SEMI)? DO
        (stmt)*
        END SEMI
    ;

ifStmt
    :   IF progExp THEN stmt* (elsePart)? END SEMI
    ;

elsePart
    :   ELSE stmt*
    ;

// type and record related rules

type
    :   (qualifier=ID COLONCOLON)? name=ID
    ;

genericType
    :   ID
    ;

record
    :   RECORD (recordVariableDeclGroup)+ END
    ;

recordVariableDeclGroup
    :   ID (COMMA ID)* COLON type SEMI
    ;

typeModelDecl
    :   TYPE FAMILY name=ID IS MODELED BY mathTypeExp SEMI
        EXEMPLAR exemplar=ID SEMI
        (constraintClause)?
        (typeModelInit)?
    ;

typeRepresentationDecl
    :   TYPE name=ID EQUALS (type|record) SEMI
        (conventionClause)?
        (correspondenceClause)?
        (typeImplInit)?
    ;

// type initialization rules

specModuleInit
    :   FACILITY_INIT
        (affectsClause)? (requiresClause)? (ensuresClause)?
    ;

typeModelInit
    :   INIT (ensuresClause)?
    ;

typeImplInit
    :   INIT (ensuresClause)?
        (variableDeclGroup)* (stmt)*
        END SEMI
    ;
*/

// math constructs

mathTheoremDecl
    :   ('Corollary'|'Theorem') name=ID ':' mathAssertionExp ';'
    ;

//The '(COMMA ID)?' is reserved for the variable we're inducting over
//in the context of an inductive defn
mathDefinitionSig
    :   name=mathSymbol ('('
            mathDefinitionParameter (',' mathDefinitionParameter)* ')')?
            ':' mathTypeExp
    ;

//Todo: Clean this up for god's sake.
mathSymbol
    :   ID
    |   ('+'|'-'|'*'|'\\'|'...'|'..'|'|'|'||'|'<'|'<='|'o'|'*'|'>='|'<='|INT)
    |   '|' '...' '|'
    |   '<' '...' '>'
    |   '||' '...' '||'
    ;

mathDefinitionParameter
    :   mathVariableDeclGroup
    |   ID
    ;

mathCategoricalDefinitionDecl
    :   'Categorical' 'Definition' 'for'
        mathDefinitionSig (',' mathDefinitionSig)+
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
    :   'Inductive' 'Definition' 'on' mathVariableDecl 'of' mathDefinitionSig 'is'
        '(i.)' mathAssertionExp ';'
        '(ii.)' mathAssertionExp ';'
    ;

mathVariableDeclGroup
    :   ID (',' ID)* ':' mathTypeExp
    ;

mathVariableDecl
    :   ID ':' mathTypeExp
    ;

// facilitydecls, enhancements, etc

/*facilityDecl
    :   'Facility' name=ID 'is' spec=ID ('<' type (COMMA type)* '>')?
        (specArgs=moduleArgumentList)? (externally=EXTERNALLY)? IMPLEMENTED
        BY impl=ID (implArgs=moduleArgumentList)? (enhancementPairDecl)* SEMI
    ;

enhancementPairDecl
    :   EXTENDED BY spec=ID (LT type (COMMA type)* GT)?
        (specArgs=moduleArgumentList)?
        (externally=EXTERNALLY)? IMPLEMENTED BY impl=ID
        (implArgs=moduleArgumentList)?
    ;

moduleArgumentList
    :   LPAREN moduleArgument (COMMA moduleArgument)* RPAREN
    ;

moduleArgument
    :   progExp
    ;

// functions

operationDecl
    :   OPERATION name=ID operationParameterList (COLON type)? SEMI
        (requiresClause)? (ensuresClause)?
    ;

operationProcedureDecl
    :   OPERATION
        name=ID operationParameterList (COLON type)? SEMI
        (requiresClause)?
        (ensuresClause)?
        (recursive=RECURSIVE)? PROCEDURE
        (variableDeclGroup)*
        (stmt)*
        END closename=ID SEMI
    ;

procedureDecl
    :   (recursive=RECURSIVE)? PROCEDURE name=ID operationParameterList
        (COLON type)? SEMI
        (variableDeclGroup)*
        (stmt)*
        END closename=ID SEMI
    ;

// mathematical clauses

affectsClause
    :   'affects' parameterMode affectsItem (COMMA affectsItem)*
    ;

affectsItem
    :   parameterMode (qualifier=ID COLONCOLON)? name=ID
    ;
*/
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
    :   q=('Forall'|'Exists') mathVariableDeclGroup ',' mathAssertionExp
    ;

mathExp
    :   op='not' mathExp                                #mathUnaryExp
    |   mathExp op=('*'|'/'|'~') mathExp                #mathInfixExp
    |   mathExp op=('+'|'-'|'.-') mathExp               #mathInfixExp
    |   mathExp op=('..'|'->') mathExp                  #mathInfixExp
    |   mathExp op=('o'|'union'|'intersect') mathExp    #mathInfixExp
    |   mathExp op=('is_in'|'is_not_in') mathExp        #mathInfixExp
    |   mathExp op=('<='|'>='|'>'|'<') mathExp          #mathInfixExp
    |   mathExp op=('='|'/=') mathExp                   #mathInfixExp
    |   mathExp op='implies' mathExp                    #mathInfixExp
    |   mathExp op=('and'|'or') mathExp                 #mathInfixExp
    |   mathExp op=':' mathTypeExp                      #mathTypeAssertionExp
    |   mathExp '(' mathExp (',' mathExp)* ')'          #mathApplyExp
    |   mathExp ('.' mathExp)+                          #mathSegmentsExp
    |   '@' mathExp                                     #mathIncomingExp
    |   '(' mathAssertionExp ')'                        #mathNestedExp
    |   mathPrimaryExp                                  #mathPrimeExp
    ;

mathPrimaryExp
    :   mathLiteralExp
    |   mathSymbolExp
    |   mathCrossTypeExp
    //|   mathSetExp
    |   mathOutfixExp
    |   mathTupleExp
    |   mathAlternativeExp
    |   mathLambdaExp
    ;

mathSymbolExp
    :   (qualifier=ID '::')? name=ID
    ;

mathLiteralExp
    :   (qualifier=ID '::')? ('true'|'false')       #mathBooleanLiteralExp
    |   (qualifier=ID '::')? num=INT                #mathIntegerLiteralExp
    ;

mathCrossTypeExp
    :   'Cart_Prod' (mathVariableDeclGroup ';')+ 'end'
    ;

mathOutfixExp
    :   lop='<' mathExp rop='>'
    |   lop='|' mathExp rop='|'
    |   lop='||' mathExp rop='||'
    ;

/*mathSetExp
    :   LBRACE mathVariableDecl BAR mathAssertionExp RBRACE  #mathSetBuilderExp//Todo
    |   LBRACE (mathExp (COMMA mathExp)*)? RBRACE         #mathSetCollectionExp
    ;
*/
mathLambdaExp
    :   'lambda' '(' mathVariableDeclGroup
        (',' mathVariableDeclGroup)* ')' '.'  mathExp
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

// program expressions

//Todo: I think precedence, and the ordering of these alternatives is nearly there -- if not already.
//we could really use some unit tests to perhaps check precendence so that in the future when
//someone comes in and mucks with the grammar, our tests will indicate that precedence is right or wrong.
/*progExp
    :   progPrimary                                     #progPrimaryExp
    |   LPAREN progExp RPAREN                           #progNestedExp
    |   op=(MINUS|NOT) progExp                          #progUnaryExp
    |   progExp op=(PLUSPLUS|MINUSMINUS)                #progPostfixExp
    |   progExp op=MOD progExp                          #progInfixExp
    |   progExp op=(MULT|DIVIDE|PLUSPLUS) progExp       #progInfixExp
    |   progExp op=(PLUS|MINUS) progExp                 #progInfixExp
    |   progExp op=(LTE|GTE|LT|GT) progExp              #progInfixExp
    |   progExp op=(EQUALS|NEQUALS) progExp             #progInfixExp
    |   progExp op=AND progExp                          #progInfixExp
    |   progExp op=OR progExp                           #progInfixExp
    ;

progPrimary
    :   progLiteralExp
    |   progVarExp
    |   progParamExp
    ;

progVarExp
    :   progNamedExp
    |   progMemberExp
    ;

progParamExp
    :   (qualifier=ID COLONCOLON)? name=ID
        LPAREN (progExp (COMMA progExp)*)? RPAREN
    ;

progNamedExp
    :   (qualifier=ID COLONCOLON)? name=ID
    ;

progMemberExp
    :   (progParamExp|progNamedExp) (DOT ID)+
    ;

progLiteralExp
    :   (TRUE|FALSE)    #progBooleanLiteralExp
    |   INT             #progIntegerLiteralExp
    |   CHAR            #progCharacterLiteralExp
    |   STRING          #progStringLiteralExp
    ;
*/
LINE_COMMENT : '//' .*? ('\n'|EOF)	-> channel(HIDDEN) ;
COMMENT      : '/*' .*? '*/'    	-> channel(HIDDEN) ;

ID  : [a-zA-Z_] [a-zA-Z0-9_]* ;
INT : [0-9]+ ;

CHAR: '\'' . '\'' ;
STRING :  '"' (ESC | ~["\\])* '"' ;
fragment ESC :   '\\' ["\bfnrt] ;

WS : [ \t\n\r]+ -> channel(HIDDEN) ;