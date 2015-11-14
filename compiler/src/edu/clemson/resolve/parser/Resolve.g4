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
    |   conceptModule
   // |   conceptImplModule
    |   facilityModule
    |   extensionImplModule
    |   extensionModule
    ;

conceptModule
    :   'Concept' name=ID ('<' genericType (',' genericType)* '>')?
        (specModuleParameterList)? ';'?
        (usesList)?
        (requiresClause)?
        (conceptBlock)
        'end' closename=ID ';'? EOF
    ;

conceptBlock
    :   ( typeModelDecl
        | operationDecl
        | mathDefinitionDecl
        | specModuleInit
        | constraintClause
        )*
    ;

// extension module

extensionModule
    :   'Extension' name=ID (specModuleParameterList)?
        'for' concept=ID ';'?
        (usesList)?
        (requiresClause)?
        (extensionBlock)
        'end' closename=ID ';'? EOF
    ;

extensionBlock
    :   ( operationDecl
        | typeModelDecl
        | mathDefinitionDecl
        )*
    ;

// implementation modules

/*conceptImplModule
    :   'Implementation' name=ID (implModuleParameterList)?
        'for' concept=ID ';'
        (usesList)?
        (requiresClause)?
        (implBlock)
        'end' closename=ID ';' EOF
    ;
*/

extensionImplModule
   :   'Implementation' name=ID (implModuleParameterList)?
       'for' enhancement=ID 'of' concept=ID ';'?
       (usesList)?
       (requiresClause)?
       (implBlock)
       'end' closename=ID ';'? EOF
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
    :   'Facility' name=ID ';'?
        (usesList)?
        (requiresClause)?
        (facilityBlock)
        'end' closename=ID ';'? EOF
    ;

facilityBlock
    :   ( mathDefinitionDecl
        | operationProcedureDecl
        | facilityDecl
        | typeRepresentationDecl
        )*
    ;

precisModule
    :   'Precis' name=ID ';'?
        (usesList)?
        precisBlock
        'end' closename=ID ';'? EOF
    ;

precisBlock
    :   ( mathDefinitionDecl
        | mathAssertionDecl
        )*
    ;

// uses, imports

usesList
    :   'uses' ID (',' ID)* ';'?
    ;

// parameter and parameter-list related rules

operationParameterList
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
    :   parameterMode ID (',' ID)* ':' type
    ;

parameterMode
    :   ( 'alters'
        | 'updates'
        | 'clears'
        | 'restores'
        | 'preserves'
        | 'replaces'
        | 'evaluates' )
    ;

variableDeclGroup
    :   'Var' ID (',' ID)* ':' type ';'?
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
    :   left=progVarExp ':=' right=progExp ';'?
    ;

swapStmt
    :   left=progVarExp ':=:' right=progVarExp ';'?
    ;

callStmt
    :   progExp ';'?
    ;

whileStmt
    :   'While' progExp
        ('maintaining' mathExp ';'?)?
        ('decreasing' mathExp ';'?)? 'do'
        (stmt)*
        'end' ';'?
    ;

ifStmt
    :   'If' progExp 'then' stmt* (elsePart)? 'end' ';'?
    ;

elsePart
    :   'else' stmt*
    ;

// type and record related rules

type
    :   (qualifier=ID '::')? name=ID
    ;

genericType
    :   ID
    ;

record
    :   'Record' (recordVariableDeclGroup)+ 'end'
    ;

recordVariableDeclGroup
    :   ID (',' ID)* ':' type ';'?
    ;

typeModelDecl
    :   'Type' 'family' name=ID 'is' 'modeled' 'by' mathTypeExp ';'?
        'exemplar' exemplar=ID ';'?
        (constraintClause)?
        (typeModelInit)?
    ;


typeRepresentationDecl
    :   'Type' name=ID '=' (type|record) ';'?
        (conventionClause)?
        (correspondenceClause)?
        (typeImplInit)?
    ;

// type initialization rules

specModuleInit
    :   'Facility_Init' (requiresClause)? (ensuresClause)?
    ;

typeModelInit
    :   'initialization' (ensuresClause)?
    ;

typeImplInit
    :   'initialization' (ensuresClause)?
        (variableDeclGroup)* (stmt)*
        'end' ';'?
    ;

// math constructs

mathAssertionDecl
    :   ('Corollary'|'Theorem') name=ID ':' mathAssertionExp ';'
    ;

//The '(COMMA ID)?' is reserved for the variable we're inducting over
//in the context of an inductive defn
mathDefinitionSig
    :   name=mathSymbolName ('('
            mathVariableDeclGroup (',' mathVariableDeclGroup)* ')')?
            ':' mathTypeExp
    ;

mathSymbolName
    :   ID
    |   ('+'|'-'|'*'|'\\'|'...'|'..'|'|'|'||'|'<'|'>'|'<='|'>='|'o'|'*'|INT)
    |   '|' '...' '|'
    |   '<' '...' '>'
    |   '||' '...' '||'
    ;

mathDefinitionDecl
    :   'Definition' mathDefinitionSig ('is' mathAssertionExp)? ';'?
    ;

mathVariableDeclGroup
    :   ID (',' ID)* ':' mathTypeExp
    ;

mathVariableDecl
    :   ID ':' mathTypeExp
    ;

// facilitydecls, enhancements, etc

facilityDecl
    :   'Facility' name=ID 'is' spec=ID ('<' type (',' type)* '>')?
        (specArgs=moduleArgumentList)? (externally='externally')? 'implemented'
        'by' impl=ID (implArgs=moduleArgumentList)? (enhancementPairDecl)* ';'?
    ;

enhancementPairDecl
    :   'extended' 'by' spec=ID ('<' type (',' type)* '>')?
        (specArgs=moduleArgumentList)?
        (externally='externally')? 'implemented' 'by' impl=ID
        (implArgs=moduleArgumentList)?
    ;

moduleArgumentList
    :   '(' moduleArgument (',' moduleArgument)* ')'
    ;

moduleArgument
    :   progExp
    ;

// functions

operationDecl
    :   'Operation' name=ID operationParameterList (':' type)? ';'?
        (requiresClause)? (ensuresClause)?
    ;

operationProcedureDecl
    :   'Operation'
        name=ID operationParameterList (':' type)? ';'?
        (requiresClause)?
        (ensuresClause)?
        (recursive='Recursive')? 'Procedure'
        (variableDeclGroup)*
        (stmt)*
        'end' closename=ID ';'?
    ;

procedureDecl
    :   (recursive='Recursive')? 'Procedure' name=ID operationParameterList
        (':' type)? ';'?
        (variableDeclGroup)*
        (stmt)*
        'end' closename=ID ';'?
    ;

// mathematical clauses

affectsClause
    :   'affects' parameterMode affectsItem (',' affectsItem)*
    ;

affectsItem
    :   parameterMode (qualifier=ID '::')? name=ID
    ;

requiresClause
    :   'requires' mathAssertionExp (entailsClause)? ';'?
    ;

ensuresClause
    :   'ensures' mathAssertionExp ';'?
    ;

constraintClause
    :   'constraint' mathAssertionExp ';'?
    ;

conventionClause
    :   'convention' mathAssertionExp (entailsClause)? ';'?
    ;

correspondenceClause
    :   'correspondence' mathAssertionExp ';'?
    ;

entailsClause
    :   'which' 'entails' mathExp (',' mathExp)* ':' mathTypeExp
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
    :   op='not' mathExp                                    #mathUnaryApplyExp
    |   functionExp=mathExp '(' mathExp (',' mathExp)* ')'  #mathPrefixApplyExp
    |   mathExp op=('*'|'/'|'~') mathExp                    #mathInfixApplyExp
    |   mathExp op=('+'|'-'|'.-') mathExp                   #mathInfixApplyExp
    |   mathExp op=('..'|'->') mathExp                      #mathInfixApplyExp
    |   mathExp op=('o'|'union'|'intersect') mathExp        #mathInfixApplyExp
    |   mathExp op=('is_in'|'is_not_in') mathExp            #mathInfixApplyExp
    |   mathExp op=('<='|'>='|'>'|'<') mathExp              #mathInfixApplyExp
    |   mathExp op=('='|'/=') mathExp                       #mathInfixApplyExp
    |   mathExp op='implies' mathExp                        #mathInfixApplyExp
    |   mathExp op=('and'|'or') mathExp                     #mathInfixApplyExp
    |   '(' mathAssertionExp ')'                            #mathNestedExp
    |   mathPrimaryExp                                      #mathPrimeExp
    ;

mathPrimaryExp
    :   mathLiteralExp
    |   mathSymbolExp
    |   mathCrossTypeExp
    |   mathSetExp
    |   mathSetRestrictionExp
    |   mathOutfixApplyExp
    |   mathTupleExp
    |   mathAlternativeExp
    |   mathSegmentsExp
    |   mathLambdaExp
    ;

mathSegmentsExp
    :   mathSymbolExp ('.' mathSymbolExp)+ ('(' mathExp (',' mathExp)* ')')?
    ;

mathSymbolExp
    :   (incoming='@')? (qualifier=ID '::')? name=mathSymbolName
    ;

mathLiteralExp
    :   (qualifier=ID '::')? ('true'|'false')       #mathBooleanLiteralExp
    |   (qualifier=ID '::')? num=INT                #mathIntegerLiteralExp
    ;

mathCrossTypeExp
    :   'Cart_Prod' (mathVariableDeclGroup ';')+ 'end'
    ;

mathOutfixApplyExp
    :   lop='<' mathExp rop='>'
    |   lop='|' mathExp rop='|'
    |   lop='||' mathExp rop='||'
    ;

mathSetRestrictionExp
    :   '{' mathVariableDecl '|' mathAssertionExp '}'
    ;

mathSetExp
    :   '{' (mathExp (',' mathExp)*)? '}'
    ;

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
progExp
    :   progPrimary                                     #progPrimaryExp
    |   '(' progExp ')'                                 #progNestedExp
    |   op=('-'|'not') progExp                          #progUnaryExp
    |   progExp op=('++'|'--')                          #progPostfixExp
    |   progExp op='%' progExp                          #progInfixExp
    |   progExp op=('*'|'/'|'++') progExp               #progInfixExp
    |   progExp op=('+'|'-') progExp                    #progInfixExp
    |   progExp op=('<='|'>='|'<'|'>') progExp          #progInfixExp
    |   progExp op=('='|'/=') progExp                   #progInfixExp
    |   progExp op='and' progExp                        #progInfixExp
    |   progExp op='or' progExp                         #progInfixExp
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
    :   (qualifier=ID '::')? name=ID
        '(' (progExp (',' progExp)*)? ')'
    ;

progNamedExp
    :   (qualifier=ID '::')? name=ID
    ;

progMemberExp
    :   (progParamExp|progNamedExp) ('.' ID)+
    ;

progLiteralExp
    :   ('true'|'false')    #progBooleanLiteralExp
    |   INT                 #progIntegerLiteralExp
    |   CHAR                #progCharacterLiteralExp
    |   STRING              #progStringLiteralExp
    ;

FORALL : ('forall'|'Forall') ;
EXISTS : ('exists'|'Exists') ;

LINE_COMMENT : '//' .*? ('\n'|EOF)	-> channel(HIDDEN) ;
COMMENT      : '/*' .*? '*/'    	-> channel(HIDDEN) ;

ID  : [a-zA-Z_] [a-zA-Z0-9_]* ;
INT : [0-9]+ ;

CHAR: '\'' . '\'' ;
STRING :  '"' (ESC | ~["\\])* '"' ;
fragment ESC :   '\\' ["\bfnrt] ;

WS : [ \t\n\r]+ -> channel(HIDDEN) ;