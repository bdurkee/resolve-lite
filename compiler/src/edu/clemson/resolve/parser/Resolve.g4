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
    :   (precisModuleDecl
    |   precisExtModuleDecl
    |   conceptExtModuleDecl
    |   conceptModuleDecl
    |   conceptImplModuleDecl
    |   conceptExtImplModuleDecl
    |   facilityModuleDecl
    |   shortFacilityModuleDecl) EOF
    ;

precisModuleDecl
    :   'Precis' name=ID ';'
        (usesList)?
        precisBlock
        'end' closename=ID ';' EOF
    ;

precisExtModuleDecl
    :   'Precis' 'Extension' name=ID 'for' precis=ID
        ('with' precisExt=ID)? ';'
        precisBlock
        'end' closename=ID ';'
    ;

conceptModuleDecl
    :   'Concept' name=ID specModuleParameterList? ';'
        (usesList)?
        (requiresClause)?
        conceptBlock
        'end' closename=ID ';'
    ;

conceptExtModuleDecl
    :   'Concept' 'Extension' name=ID specModuleParameterList?
        'for' concept=ID ';'
        (usesList)?
        conceptBlock
        'end' closename=ID ';'
    ;

conceptImplModuleDecl
    :   'Implementation' name=ID implModuleParameterList?
        'for' concept=ID ';'
        (usesList)?
        implBlock
        'end' closename=ID ';'
    ;

conceptExtImplModuleDecl
    :   'Implementation' name=ID implModuleParameterList?
        'for' extension=ID 'of' concept=ID ';'
        (usesList)?
        implBlock
        'end' closename=ID ';'
    ;

facilityModuleDecl
    :   'Facility' name=ID ';'
         (usesList)?
         (requiresClause)?
         facilityBlock
        'end' closename=ID ';'
    ;

shortFacilityModuleDecl
    :   facilityDecl
    ;

// uses, imports

usesList
    :   'uses' usesSpec (',' usesSpec)* ';'
    ;

usesSpec : id=ID fromClauseSpec? alias? ;
fromClauseSpec : 'from' qualifiedFromPath ;
qualifiedFromPath : ID ('.' ID)*;
alias : 'as' ID ;

// module blocks & items

precisBlock
    :   ( mathStandardDefnDecl
        | mathCategoricalDefnDecl
        | mathInductiveDefnDecl
        | mathTheoremDecl
        | mathClssftnTheoremDecl
        )*
    ;

conceptBlock
    :   ( mathStandardDefnDecl
        | typeModelDecl
        | operationDecl
        | constraintsClause
        )*
    ;

implBlock
    :   ( operationProcedureDecl
        | procedureDecl
        | typeRepresentationDecl
        | facilityDecl
        )*
    ;

facilityBlock
    :   ( facilityDecl
        | operationProcedureDecl
        | typeRepresentationDecl
        )*
    ;

// type refs & decls

type
    :   (qualifier=ID '::')? name=ID           #namedType
    |    'Record' (recordVarDeclGroup)* 'end'  #recordType
    ;

typeModelDecl
    :   'Type' 'family' name=ID 'is' 'modeled' 'by' mathClssftnExp ';'?
        'exemplar' exemplar=ID ';'?
        (constraintsClause)?
        (initializationClause)?
    ;

typeRepresentationDecl
    :   'Type' name=ID '=' type ';'?
        (conventionsClause)?
        (correspondenceClause)?
        (typeImplInit)?
    ;

// type initialization rules

specModuleInit
    :   'Facility_Init' (requiresClause)? (ensuresClause)?
    ;

typeImplInit
    :   'initialization'
        (varDeclGroup)* //(stmt)*
        'end' ';'
    ;

// parameter and parameter-list related rules

specModuleParameterList
    :   '(' specModuleParameterDecl (';' specModuleParameterDecl)* ')'
    ;

implModuleParameterList
    :   '(' implModuleParameterDecl (';' implModuleParameterDecl)* ')'
    ;

operationParameterList
    :   '(' (parameterDeclGroup (';' parameterDeclGroup)*)?  ')'
    ;

specModuleParameterDecl
    :   parameterDeclGroup
    |   mathStandardDefnDecl
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
        | 'evaluates'
        | ID
        )
    ;

// prog variable decls

recordVarDeclGroup
    :   ID (',' ID)* ':' type ';'?
    ;

varDeclGroup
    :   'Var' ID (',' ID)* ':' type ';'?
    ;

// facility decls

facilityDecl
    :   'Facility' name=ID 'is' spec=ID (specArgs=moduleArgumentList)?
        (externally='externally')? 'implemented' 'by' impl=ID
        (implArgs=moduleArgumentList)? (extensionPairing)* ';'?
    ;

extensionPairing
    :   'extended' 'by' spec=ID (specArgs=moduleArgumentList)?
        (externally='externally')? 'implemented' 'by' impl=ID
        (implArgs=moduleArgumentList)?
    ;

moduleArgumentList
    :   '(' progExp (',' progExp)* ')'
    ;

// operations & procedures

operationDecl
    :   'Operation' name=ID operationParameterList (':' type)? ';'
        (requiresClause)? (ensuresClause)?
    ;

operationProcedureDecl
    :   'Operation' name=ID operationParameterList (':' type)? ';'
        (requiresClause)?
        (ensuresClause)?
        (recursive='Recursive')? 'Procedure'
        (varDeclGroup)*
        (stmt)*
        'end' closename=ID ';'
    ;

procedureDecl
    :   (recursive='Recursive')? 'Procedure' name=ID operationParameterList
        (':' type)? ';'
        (varDeclGroup)*
        (stmt)*
        'end' closename=ID ';'
    ;

// statements

stmt
    :   assignStmt
    |   swapStmt
    |   callStmt
    |   whileStmt
    |   ifStmt
    ;

assignStmt : left=progExp ':=' right=progExp ';' ;
swapStmt : left=progExp ':=:' right=progExp ';' ;
callStmt : progParamExp ';' ;

whileStmt
    :   'While' progExp
        changingClause? maintainingClause? decreasingClause?
        'do' stmt* 'end' ';'
    ;

ifStmt : 'If' progExp 'then' stmt* elseStmt? 'end' ';' ;
elseStmt : 'else' stmt* ;

// program expressions

progExp
    :   progPrimary                                     #progPrimaryExp
    |   '(' progExp ')'                                 #progNestedExp
    |   lhs=progExp op='.' rhs=progExp                  #progSelectorExp
    |   op=('-'|'not') progExp                          #progUnaryExp
    |   progExp op=('*'|'/'|'%') progExp                #progInfixExp
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

//operations are first class citizens with respect to a certain class of
//parameterized resolve implementation-modules. So we
//represent the name portion as its own (potentially qualified) expression,
//once again this makes building our function application AST node much easier.
progParamExp
    :   progNamedExp '(' (progExp (',' progExp)*)? ')'
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

// math constructs

mathTheoremDecl
    :   ('Corollary'|'Theorem') name=ID ':' mathAssertionExp ';'
    ;

mathClssftnTheoremDecl
    :   'Type' 'Theorem' name=ID ':' mathExp ':' mathExp ';'
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
                ':' mathClssftnExp
    ;

mathPrefixDefnSigs
    :   mathPrefixDefnSig
        (',' mathPrefixDefnSig)*
    ;

mathInfixDefnSig
    :   '(' mathVarDecl ')' name=mathSymbolName
        '(' mathVarDecl ')' ':' mathClssftnExp
    ;

mathOutfixDefnSig
    :   leftSym=('|'|'||'|'<'|'⎝'|'⟨') mathVarDecl
        rightSym=('⟩'|'⎠'|'|'|'||'|'>') ':' mathClssftnExp
    ;

mathPostfixDefnSig
    :   '('mathVarDecl')' lop='[' mathVarDecl rop=']' ':' mathClssftnExp
    ;

mathSymbolName
    : ( ID |
      ('o'|'true'|'false'|INT|'+'|'-'|'*'|'/'|'>'|'≤'|
       '<'|'<='|'>='|'≥'|'not'|'⌐'|'≼'|'ϒ'|'∪₊'|'≤ᵤ'|'⨩'))
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
    :   ID (',' ID)* ':' mathClssftnExp
    ;

mathVarDecl
    :   ID ':' mathClssftnExp
    ;

// mathematical clauses

initializationClause : 'initialization' (ensuresClause);
requiresClause : 'requires' mathAssertionExp (entailsClause)? ';';
ensuresClause : 'ensures' mathAssertionExp ';';
constraintsClause : ('constraints') mathAssertionExp ';';
conventionsClause : 'conventions' mathAssertionExp (entailsClause)? ';';
correspondenceClause : 'correspondence' mathAssertionExp ';';
changingClause : 'changing' mathExp (',' mathExp)* ';' ;
maintainingClause : 'maintaining' mathAssertionExp ';' ;
decreasingClause : 'decreasing' mathExp (',' mathExp)* ';' ;
entailsClause : 'which_entails' mathExp ;

// mathematical expressions

mathClssftnExp
    :   mathExp
    ;

mathAssertionExp
    :   mathExp
    |   mathQuantifiedExp
    ;

mathQuantifiedExp
    :   q=(FORALL|EXISTS|'∀'|'∃') mathVarDeclGroup ',' mathAssertionExp
    ;

mathExp
    :   lhs=mathExp op='.' rhs=mathExp                      #mathSelectorExp
    |   name=mathExp lop='(' mathExp (',' mathExp)*rop=')'  #mathPrefixAppExp
    |   mathExp mathSqBrOpExp mathExp (',' mathExp)* ']'    #mathBracketAppExp
    |   mathExp mathMultOpExp mathExp                       #mathInfixAppExp
    |   mathExp mathAddOpExp mathExp                        #mathInfixAppExp
    |   mathExp mathJoiningOpExp mathExp                    #mathInfixAppExp
    |   mathExp mathSetContainmentOpExp mathExp             #mathInfixAppExp
    |   mathExp mathEqualityOpExp mathExp                   #mathInfixAppExp
    |   mathExp mathRelationalOpExp mathExp                 #mathInfixAppExp
    |   <assoc=right> mathExp mathArrowOpExp mathExp        #mathInfixAppExp
    |   mathExp ':' mathExp                                 #mathClssftnAssertionExp
    |   mathExp mathBooleanOpExp mathExp                    #mathInfixAppExp
    |   mathExp mathImpliesOpExp mathExp                    #mathInfixAppExp
    |   '(' mathAssertionExp ')'                            #mathNestedExp
    |   mathPrimeExp                                        #mathPrimaryExp
    ;

/** Because operators are now first class citizens with expressions all of their
 *  own (as opposed to being simple strings embedded within the context of some application)
 *  we need these intermediate rules to convince antlr to create visitable, *annotatable*,
 *  rule contexts for these guys -- which greatly eases the creation (and subsequent typing) of an AST.
 *  No longer a need to pass special maps around from Token -> MathClassification, etc --
 *  now we just need to visit and annotate these names like any other node).
 */
//mathMultOpExp : (qualifier=ID '::'|sym='ᶻ')? op=('*'|'/'|'%') ;
mathSqBrOpExp : op='[' ;
mathMultOpExp : (qualifier=ID '::')? op=('*'|'/'|'%') ;
mathAddOpExp : (qualifier=ID '::')? op=('+'|'-'|'~');
mathJoiningOpExp : (qualifier=ID '::')? op=('o'|'union'|'∪'|'∪₊'|'intersect'|'∩'|'∩₊');
mathArrowOpExp : (qualifier=ID '::')? op=('->'|'⟶') ;
mathRelationalOpExp : (qualifier=ID '::')? op=('<'|'>'|'<='|'≤'|'≤ᵤ'|'>='|'≥');
mathEqualityOpExp : (qualifier=ID '::')? op=('='|'/='|'≠');
mathSetContainmentOpExp : (qualifier=ID '::')? op=('is_in'|'is_not_in'|'∈'|'∉');
mathImpliesOpExp : (qualifier=ID '::')? op='implies';
mathBooleanOpExp : (qualifier=ID '::')? op=('and'|'or'|'∧'|'∨'|'iff');

mathPrimeExp
    :   mathLiteralExp
    |   mathCartProdExp
    |   mathSymbolExp
    |   mathOutfixAppExp
    |   mathSetRestrictionExp
    |   mathSetExp
    |   mathLambdaExp
    |   mathAlternativeExp
    ;

mathLiteralExp
    :   (qualifier=ID '::')? op=('true'|'false')    #mathBooleanLiteralExp
    |   (qualifier=ID '::')? num=INT                #mathIntegerLiteralExp
    ;

mathCartProdExp
    :   'Cart_Prod' (mathVarDeclGroup ';')+ 'end'
    ;

mathSymbolExp
    :   (incoming='@')? (qualifier=ID '::')? name=mathSymbolName
    ;

mathOutfixAppExp
    :   lop='|' mathExp rop='|'
    |   lop='||' mathExp rop='||'
    |   lop='<' mathExp rop='>'
    |   lop='⟨' mathExp rop='⟩'
    |   lop='[' mathExp rop=']'
    |   lop='⎝' mathExp rop='⎠'
    ;

mathSetRestrictionExp
    :   '{' mathVarDecl '|' mathAssertionExp '}'
    ;

mathSetExp
    :    '{' (mathExp (',' mathExp)*)? '}'
    ;

mathLambdaExp
    :   ('lambda'|'λ') mathVarDecl ',' mathExp
    ;

mathAlternativeExp
    :   '{{' (mathAlternativeItemExp)+ '}}'
    ;

mathAlternativeItemExp
    :   result=mathExp ('if' condition=mathExp ';' | 'otherwise' ';')
    ;

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