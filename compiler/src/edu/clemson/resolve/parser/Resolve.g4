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
    |   precisExtensionModuleDecl
    |   facilityModuleDecl
    |   conceptModuleDecl
    |   conceptImplModuleDecl
    ;

// precis

precisModuleDecl
    :   'Precis' name=ID ';'
        (usesList)?
        precisBlock
        'end' closename=ID ';' EOF
    ;

// precis extensions

precisExtensionModuleDecl
    :   'Precis' 'Extension' name=ID 'for' precis=ID
        ('extended_by' precisExt=ID)? ';'
        precisBlock
        'end' closename=ID ';'
    ;

precisBlock
    :   ( mathStandardDefinitionDecl
        | mathCategoricalDefinitionDecl
        | mathInductiveDefinitionDecl
        | mathTheoremDecl
        )*
    ;

// concepts

conceptModuleDecl
    :   'Concept' name=ID (specModuleParameterList)? ';'
        (usesList)?
        (requiresClause)?
        conceptBlock
        'end' closename=ID ';'
    ;

conceptBlock
    :   ( mathStandardDefinitionDecl
        | typeModelDecl
        | operationDecl
        )*
    ;

// concept impls

conceptImplModuleDecl
    :   'Implementation' name=ID implModuleParameterList?
        'for' concept=ID ';'
        (usesList)?
        implBlock
        'end' closename=ID ';'
    ;

implBlock
    :   ( operationProcedureDecl
        | procedureDecl
        | typeRepresentationDecl
        | facilityDecl
        )*
    ;

// facilities

facilityModuleDecl
    :   'Facility' name=ID ';'
         (usesList)?
         (requiresClause)?
         facilityBlock
        'end' closename=ID ';'
    ;

facilityBlock
    :   ( facilityDecl )*
    ;

// uses, imports

usesList
    :   'uses' ID (',' ID)* ';'
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
    |   mathStandardDefinitionDecl
    |   genericTypeParameterDecl
    ;

implModuleParameterDecl
    :   parameterDeclGroup
   // |   operationDecl
    ;

parameterDeclGroup
    :   parameterMode ID (',' ID)* ':' type
    ;

genericTypeParameterDecl
    :   'type' name=ID
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


// type and record related rules

type
    :   (qualifier=ID '::')? name=ID                #namedType
    |    'Record' (recordVariableDeclGroup)* 'end'  #recordType
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
    :   'Type' name=ID '=' type ';'?
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
        (variableDeclGroup)* //(stmt)*
        'end' ';'?
    ;

// math constructs

mathTheoremDecl
    :   ('Corollary'|'Theorem') name=ID ':' mathAssertionExp ';'
    ;

mathDefinitionSig
    :   mathPrefixDefinitionSig
    |   mathInfixDefinitionSig
    |   mathOutfixDefinitionSig
    ;

mathPrefixDefinitionSig
    :   name=mathSymbolName ('('
                mathVariableDeclGroup (',' mathVariableDeclGroup)* ')')?
                ':' mathTypeExp
    ;

mathInfixDefinitionSig
    :   '(' mathVariableDecl ')' name=mathSymbolName
        '(' mathVariableDecl ')' ':' mathTypeExp
    ;

mathOutfixDefinitionSig
    :   leftSym=mathSymbolName mathVariableDecl
        rightSym=mathSymbolName ':' mathTypeExp
    ;

mathSymbolName
    :   ID
    |   ('+'|'-'|'...'|'/'|'\\'|'|'|'||'|'<'|'>'|'o'|'*'|'>='|'<='|INT|'not')
    ;

mathCategoricalDefinitionDecl
    :   'Categorical' 'Definition' 'for'
        mathPrefixDefinitionSig (',' mathPrefixDefinitionSig)+
        'is' mathAssertionExp ';'
    ;

mathStandardDefinitionDecl
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

// facilitydecls, enhancements, etc

facilityDecl
    :   'Facility' name=ID 'is' spec=ID (specArgs=moduleArgumentList)?
        (externally='externally')? 'implemented' 'by' impl=ID
        (implArgs=moduleArgumentList)? /*(enhancementPairDecl)*/ ';'?
    ;

moduleArgumentList
    :   '(' moduleArgument (',' moduleArgument)* ')'
    ;

moduleArgument
    :   progExp
    ;

// functions

operationDecl
    :   'Operation' name=ID operationParameterList (':' type)? ';'
        (requiresClause)? (ensuresClause)?
    ;

operationProcedureDecl
    :   'Operation' name=ID operationParameterList (':' type)? ';'
        (requiresClause)?
        (ensuresClause)?
        (recursive='Recursive')? 'Procedure'
        (variableDeclGroup)*
        //(stmt)*
        'end' closename=ID ';'
    ;

procedureDecl
    :   (recursive='Recursive')? 'Procedure' name=ID operationParameterList
        (':' type)? ';'
        (variableDeclGroup)*
        //(stmt)*
        'end' closename=ID ';'
    ;

// mathematical clauses

requiresClause
    :   'requires' mathAssertionExp (entailsClause)? ';'
    ;

ensuresClause
    :   'ensures' mathAssertionExp ';'
    ;

constraintClause
    :   ('Constraints'|'constraints') mathAssertionExp ';'
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
    :   functionExp=mathExp '(' mathExp (',' mathExp)* ')'      #mathPrefixApplyExp
    |   lhs=mathExp op='.' rhs=mathExp                          #mathSelectorExp
    |   mathExp op=('*'|'/'|'~') mathExp                        #mathInfixApplyExp
    |   mathExp op=('+'|'-') mathExp                            #mathInfixApplyExp
    |   mathExp op=('..'|'->') mathExp                          #mathInfixApplyExp
    |   mathExp op=('o'|'union'|'intersect') mathExp            #mathInfixApplyExp
    |   mathExp op=('is_in'|'is_not_in') mathExp                #mathInfixApplyExp
    |   mathExp op=('<='|'>='|'>'|'<') mathExp                  #mathInfixApplyExp
    |   mathExp op=('='|'/=') mathExp                           #mathInfixApplyExp
    |   mathExp op=('implies'|'iff') mathExp                    #mathInfixApplyExp
    |   mathExp op=('and'|'or') mathExp                         #mathInfixApplyExp
    |   mathExp op=':' mathTypeExp                              #mathTypeAssertionExp
    |   '(' mathAssertionExp ')'                                #mathNestedExp
    |   mathPrimaryExp                                          #mathPrimeExp
    ;

mathPrimaryExp
    :   mathLiteralExp
    |   mathCrossTypeExp
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

mathCrossTypeExp
    :   'Cart_Prod' (mathVariableDeclGroup ';')+ 'end'
    ;

mathSymbolExp
    :   (incoming='@')? (qualifier=ID '::')? name=mathSymbolName
    ;

mathOutfixApplyExp
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

// program expressions

//Todo: I think precedence, and the ordering of these alternatives is nearly there -- if not already.
//we could really use some unit tests to perhaps check precendence so that in the future when
//someone comes in and mucks with the grammar, our tests will indicate that precedence is right or wrong.
progExp
    :   progPrimary                                     #progPrimaryExp
    |   '(' progExp ')'                                 #progNestedExp
    |   lhs=progExp op='.' rhs=progExp                  #progSelectorExp
    |   op=('-'|'not') progExp                          #progUnaryExp
    |   progExp op='%' progExp                          #progInfixExp
    |   progExp op=('*'|'/') progExp                    #progInfixExp
    |   progExp op=('+'|'-') progExp                    #progInfixExp
    |   progExp op=('<='|'>='|'<'|'>') progExp          #progInfixExp
    |   progExp op=('='|'/=') progExp                   #progInfixExp
    |   progExp op=('and'|'or') progExp                 #progInfixExp
    ;

progPrimary
    :   progLiteralExp
    |   progParamExp
    |   progNamedExp
    ;

progParamExp
    :   (qualifier=ID '::')? name=ID '(' (progExp (',' progExp)*)? ')'
    ;

progNamedExp
    :   (qualifier=ID '::')? name=ID
    ;

progLiteralExp
    :   ('true'|'false')    #progBooleanLiteralExp
    |   INT                 #progIntegerLiteralExp
    |   CHAR                #progCharacterLiteralExp
    |   STRING              #progStringLiteralExp
    ;

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