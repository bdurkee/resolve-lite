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
    :   precisModule
    |   conceptModule
    |   enhancementModule
    |   enhancementImplModule
    |   facilityModule
    |   conceptImplModule
    ;

// precis module

precisModule
    :   'Precis' name=Identifier ';'
        (importList)?
        (precisItems)?
        'end' closename=Identifier ';' EOF
    ;

precisItems
    :   (precisItem)+
    ;

precisItem
    :   mathDefinitionDecl
    |   mathTypeTheoremDecl
    ;

// concept module

conceptModule
    :   'Concept' name=Identifier ('<' genericType (',' genericType)* '>')?
        (specModuleParameterList)? ';'
        (importList)?
        (requiresClause)?
        (conceptBlock)?
        'end' closename=Identifier ';' EOF
    ;

conceptBlock
    :   ( operationDecl
        | typeModelDecl
        | mathDefinitionDecl
        | constraintClause
        )+
    ;

// enhancement module

enhancementModule
    :   'Enhancement' name=Identifier (specModuleParameterList)?
        'for' concept=Identifier ';'
        (importList)?
        (requiresClause)?
        (enhancementBlock)?
        'end' closename=Identifier ';'
    ;

enhancementBlock
    :   ( operationDecl
        | typeModelDecl
        | mathDefinitionDecl
        )+
    ;

// facility module

facilityModule
    :   'Facility' name=Identifier ';'
        (importList)?
        (requiresClause)?
        (facilityBlock)?
        'end' closename=Identifier ';' EOF
    ;

facilityBlock
    :   ( facilityDecl
        | typeRepresentationDecl
        | operationProcedureDecl
        | mathDefinitionDecl
        )+
    ;

// implementation modules

conceptImplModule
    :   'Implementation' name=Identifier (implModuleParameterList)?
        'for' concept=Identifier ';'
        (importList)?
        (requiresClause)?
        (implBlock)?
        'end' closename=Identifier ';'
    ;

enhancementImplModule
    :   'Implementation' name=Identifier (specModuleParameterList)?
        'for' enhancement=Identifier 'of' concept=Identifier ';'
        (importList)?
        (requiresClause)?
        (implBlock)?
        'end' closename=Identifier ';'
    ;

implBlock
    :   ( typeRepresentationDecl
        | operationProcedureDecl
        | procedureDecl
        | facilityDecl
        )+
    ;

// uses, imports

importList
    :   'uses' Identifier (',' Identifier)* ';'
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

moduleParameterDecl
    :   parameterDeclGroup
    ;

specModuleParameterDecl
    :   parameterDeclGroup
    |   mathDefinitionDecl
    ;

implModuleParameterDecl
    :   parameterDeclGroup
    |   operationDecl
    ;

genericType
    :   Identifier
    ;

parameterDeclGroup
    :   parameterMode Identifier (',' Identifier)* ':' type
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
    :   'Var' Identifier (',' Identifier)* ':' type ';'
    ;

// statements

stmt
    :   assignStmt
    |   swapStmt
    |   callStmt
    |   whileStmt
    ;

assignStmt
    :   left=progExp ':=' right=progExp ';'
    ;

swapStmt
    :   left=progExp ':=:' right=progExp ';'
    ;

callStmt
    :   progParamExp ';'
    ;

whileStmt
    :   'While' progExp
        (changingClause)?
        (maintainingClause)?
        (decreasingClause)? 'do'
        (stmt)*
        'end' ';'
    ;

// type and record related rules

type
    :   (qualifier=Identifier '::')? name=Identifier
    ;

record
    :   'Record' (recordVariableDeclGroup)+ 'end'
    ;

recordVariableDeclGroup
    :   Identifier (',' Identifier)* ':' type ';'
    ;

typeModelDecl
    :   'Type' 'Family' name=Identifier 'is' 'modeled' 'by' mathTypeExp ';'
        'exemplar' exemplar=Identifier ';'
        (constraintClause)?
        (typeModelInit)?
        (typeModelFinal)?
    ;

typeRepresentationDecl
    :   'Type' name=Identifier '=' (record|type) ';'
        (conventionClause)?
        //(correspondenceClause)?
        (typeImplInit)?
        (typeImplFinal)?
    ;

// initialization, finalization rules

typeModelInit
    :   'initialization' (requiresClause)? (ensuresClause)?
    ;

typeModelFinal
    :   'finalization' (requiresClause)? (ensuresClause)?
    ;

typeImplInit
    :   'initialization' (requiresClause)? (ensuresClause)?
        (variableDeclGroup)* (stmt)*
    ;

typeImplFinal
    :   'finalization' (requiresClause)? (ensuresClause)?
        (variableDeclGroup)* (stmt)*
    ;

// functions

operationDecl
    :   ('Operation'|'Oper') name=Identifier operationParameterList
        (':' type)? ';' (requiresClause)? (ensuresClause)?
    ;

operationProcedureDecl
    :   (recursive='Recursive')? ('Operation'|'Oper')
        name=Identifier operationParameterList (':' type)? ';'
        (requiresClause)?
        (ensuresClause)?
        'Procedure'
        (variableDeclGroup)*
        (stmt)*
        'end' closename=Identifier ';'
    ;

procedureDecl
    :   (recursive='Recursive')? ('Procedure'|'Proc') name=Identifier
        operationParameterList (':' type)? ';'
        (variableDeclGroup)*
        (stmt)*
        'end' closename=Identifier ';'
    ;

// facility decls

facilityDecl
    :   'Facility' name=Identifier 'is' spec=Identifier
        ('<' type (',' type)* '>')?
        (specArgs=moduleArgumentList)? (externally='externally')? 'implemented'
        'by' impl=Identifier (implArgs=moduleArgumentList)?
        (enhancementPairDecl)* ';'
    ;

enhancementPairDecl
    :   'enhanced' 'by' spec=Identifier (specArgs=moduleArgumentList)?
        (externally='externally')? 'implemented' 'by' impl=Identifier
        (implArgs=moduleArgumentList)?
    ;

moduleArgumentList
    :   '(' moduleArgument (',' moduleArgument)* ')'
    ;

moduleArgument
    :   progExp
    ;

// variable declarations

mathVariableDeclGroup
    :   Identifier (',' Identifier)* ':' mathTypeExp
    ;

mathVariableDecl
    :   name=Identifier ':' mathTypeExp
    ;

// mathematical theorems, corollaries, etc

mathTheoremDecl
    :   ('Theorem'|'Lemma'|'Corollary') name=Identifier
        ':' mathAssertionExp ';'
    ;

// mathematical type theorem

mathTypeTheoremDecl
    :   'Type' 'Theorem' name=Identifier ':'
        (mathTypeTheoremUniversalVars)+
        (bindingExp=mathFunctionApplicationExp ':'
         typeExp=mathFunctionApplicationExp) ';'
    ;

mathTypeTheoremUniversalVars
    :   ('Forall'|'Exists') mathVariableDeclGroup ','
    ;

// mathematical definitions

mathDefinitionDecl
    :   'Definition' name=infixOp (definitionParameterList)? ':'
        mathTypeExp ('is' mathAssertionExp)? ';'
    ;

infixOp
    :   ('implies'|'+'|'o'|'-'|'/'|'*'|'..'|'and'|'or')
    |   ('union'|'intersect'|'is_in'|'is_not_in'|'>'|'<'|'>='|'<=')
    |   Identifier
    ;

definitionParameterList
    :   '(' mathVariableDeclGroup (',' mathVariableDeclGroup)* ')'
    ;

// mathematical clauses

rememberClause
    :   'remember'
    ;

requiresClause
    :   'requires' mathAssertionExp (mathEntailsAddendum)? ';'
    ;

ensuresClause
    :   'ensures' mathAssertionExp ';'
    ;

constraintClause
    :   ('constraint'|'Constraint') mathAssertionExp ';'
    ;

changingClause
    :   'changing' progExp (',' progExp)* ';'
    ;

maintainingClause
    :   'maintaining' mathAssertionExp ';'
    ;

decreasingClause
    :   'decreasing' mathAssertionExp ';'
    ;

whereClause
    :   'where' mathAssertionExp
    ;

mathEntailsAddendum
    :   'which_entails' variableName=Identifier ':' mathTypeExp
    ;

correspondenceClause
    :   'correspondence' mathAssertionExp ';'
    ;

conventionClause
    :   'convention' mathAssertionExp ';'
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
    :   q=('Forall'|'Exists') mathVariableDeclGroup ',' mathQuantifiedExpBody
    ;

mathQuantifiedExpBody
    :   mathAssertionExp
    ;

mathExp
    :   mathPrimaryExp                                  #mathPrimeExp
    |   op=('+'|'-'|'~'|'not') mathExp                  #mathUnaryExp
    |   mathExp op=('*'|'/'|'~') mathExp                #mathInfixExp
    |   mathExp op=('+'|'-') mathExp                    #mathInfixExp
    |   mathExp op=('..'|'->') mathExp                  #mathInfixExp
    |   mathExp op=('o'|'union'|'intersect') mathExp    #mathInfixExp
    |   mathExp op=('is_in'|'is_not_in') mathExp        #mathInfixExp
    |   mathExp op=('<='|'>='|'>'|'<') mathExp          #mathInfixExp
    |   mathExp op=('='|'/=') mathExp                   #mathInfixExp
    |   mathExp op='implies' mathExp                    #mathInfixExp
    |   mathExp op=('and'|'or') mathExp                 #mathInfixExp
    |   '(' mathAssertionExp ')'                        #mathNestedExp
    ;

mathPrimaryExp
    :   mathLiteralExp
    |   mathFunctionApplicationExp
    |   mathCrossTypeExp
    |   mathDotExp
    |   mathOutfixExp
    |   mathSetExp
    |   mathTupleExp
    ;

mathLiteralExp
    :   BooleanLiteral      #mathBooleanExp
    |   IntegerLiteral      #mathIntegerExp
    ;

mathFunctionApplicationExp
    :   ('@')? name=Identifier '(' mathExp (',' mathExp)* ')'  #mathFunctionExp
    |   ('@')? name=Identifier    #mathVariableExp
    ;

mathCrossTypeExp
    :   'Cart_Prod' (mathVariableDeclGroup ';')+ 'end'
    ;

mathOutfixExp
    :   lop='<' mathExp rop='>'
    |   lop='|' mathExp rop='|'
    |   lop='||' mathExp rop='||'
    ;

mathSetExp
    :   '{' mathVariableDecl '|' mathAssertionExp '}'   #mathSetBuilderExp//Todo
    |   '{' (mathExp (',' mathExp)*)? '}'               #mathSetCollectionExp
    ;

mathTupleExp
    :   '(' mathExp (',' mathExp)+ ')'
    ;

mathDotExp
    :   mathFunctionApplicationExp ('.' mathFunctionApplicationExp)+
    ;

// program expressions

progExp
    :   op=('not'|'-') progExp                  #progApplicationExp
    |   progExp op=('*'|'/') progExp            #progApplicationExp
    |   progExp op=('+'|'-') progExp            #progApplicationExp
    |   progExp op=('<='|'>='|'>'|'<') progExp  #progApplicationExp
    |   progExp op=('='|'/=') progExp           #progApplicationExp
    |   '(' progExp ')'                         #progNestedExp
    |   progPrimary                             #progPrimaryExp
    ;

progPrimary
    :   progLiteralExp
    |   progNamedExp
    |   progParamExp
    |   progMemberExp
    ;

progMemberExp
    :   (progParamExp|progNamedExp) ('.' Identifier)+
    ;

progParamExp
    :   (qualifier=Identifier '::')? name=Identifier
        '(' (progExp (',' progExp)*)? ')'
    ;

progNamedExp
    :   (qualifier=Identifier '::')? name=Identifier
    ;

progLiteralExp
    :   IntegerLiteral      #progIntegerExp
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