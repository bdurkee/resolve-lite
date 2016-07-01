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
    :   'uses' (usesSpecs|'(' usesSpecs ')') ';'
    ;

usesSpecs
    :   moduleIdentifierSpec (',' moduleIdentifierSpec)*
    ;

//TODO: To make this match up better with the plugin, do the following renames:
moduleIdentifierSpec : id=ID fromClause? aliasClause? ;   //TODO: Rename this moduleIdentifierSpec
fromClause : 'from' moduleLibraryIdentifier ; //TODO: Rename 'fromClause'
aliasClause : 'as' ID ;                           //TODO: Rename 'aliasClause'
moduleLibraryIdentifier : ID ('.' ID)* ;          //TODO: Rename ModuleLibraryIdentifier

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
    :   (qualifier=ID '.')? name=ID             #namedType
    |    'Record' (recordVarDeclGroup)* 'end'   #recordType
    ;

typeModelDecl
    :   'Type' 'family' name=ID 'is' 'modeled' 'by' mathClssftnExp ';'?
        'exemplar' exemplar=ID ';'?
        (constraintsClause)?
        (initializationClause)?
    ;

typeRepresentationDecl
    :   'Type' name=ID 'is' type ';'?
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
    :   'Facility' name=ID 'is' spec=ID (specArgs=moduleArgumentList)? specFrom=fromClause?
        (externally='externally')? 'implemented' 'by' impl=ID implFrom=fromClause?
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
    |   lhs=progExp '.' rhs=progExp                     #progSelectorExp
    |   progExp name=progSymbolExp progExp              #progInfixExp       //TODO: Change from progSymbolExp to just ID
    ;

progPrimary
    :   progLiteralExp
    |   progParamExp
    |   progSymbolExp
    ;

progParamExp
    :   progSymbolExp '(' (progExp (',' progExp)*)? ')'
    ;

progSymbolExp
    :   (qualifier=ID '.')? name=progSymbolName
    ;

progSymbolName
    :   (SYM | ID)
    ;

progLiteralExp
    :   BOOL                #progBooleanLiteralExp
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
//    |   mathOutfixDefnSig
    |   mathPostfixDefnSig
    ;

mathPrefixDefnSig
    :   mathSymbolName (',' mathSymbolName)* ('('
                mathVarDeclGroup (',' mathVarDeclGroup)* ')')?
                (':'|'⦂') mathClssftnExp
    ;

mathPrefixDefnSigs
    :   mathPrefixDefnSig (',' mathPrefixDefnSig)*
    ;

mathInfixDefnSig
    :   '(' mathVarDecl ')' name=mathSymbolName
        '(' mathVarDecl ')' (':'|'⦂') mathClssftnExp
    ;

mathOutfixDefnSig
    :   leftSym=mathSymbolNameNoID mathVarDecl
        rightSym=mathSymbolNameNoID (':'|'⦂') mathClssftnExp
    ;

mathPostfixDefnSig
    :   '('mathVarDecl')' lop=mathSymbolName mathVarDecl rop=mathSymbolName ':' mathClssftnExp
    ;

//the bar needs to be there because of the set restriction exp
mathSymbolName
    :   (ID | MATH_UNICODE_SYM | SYM | INT | BOOL | '|' )    //TODO: Maybe use BOOL instead?
    ;

mathSymbolNameNoID
    :   (MATH_UNICODE_SYM | SYM | '|' )
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
    :   ID (',' ID)* (':'|'⦂') mathClssftnExp
    ;

mathVarDecl
    :   ID (':'|'⦂') mathClssftnExp
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

//TODO: no unary
mathExp
    :   mathPrimeExp                                        #mathPrimaryExp
    |   '(' mathAssertionExp ')'                            #mathNestedExp
    |   lhs=mathExp op='.' rhs=mathExp                      #mathSelectorExp
    |   name=mathSymbolExp mathExp                          #mathUnaryExp
    |   name=mathExp lop='(' mathExp (',' mathExp)* rop=')' #mathPrefixAppExp
//    |   mathExp lop='[' mathExp (',' mathExp)* rop=']'    #mathBracketAppExp
    |   mathExp mathSymbolExp mathExp                       #mathInfixAppExp
    |   mathExp (':'|'⦂') mathExp                           #mathClssftnAssertionExp
    ;

mathPrimeExp
    :   mathCartProdExp
    |   mathSymbolExp
    |   mathOutfixAppExp
    |   mathSetRestrictionExp
    |   mathSetExp
    |   mathLambdaExp
    |   mathAlternativeExp
    ;

mathCartProdExp
    :   'Cart_Prod' (mathVarDeclGroup ';')+ 'end'
    ;

mathSymbolExp
    :   incoming='@'? (qualifier=ID '.')? name=mathSymbolName
    ;

mathOutfixAppExp
    :   '`' lop=mathSymbolNameNoID mathExp rop=mathSymbolNameNoID '`'
    ;

mathSetRestrictionExp
    :   '{' mathVarDecl 's.t.' mathAssertionExp '}'
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

FORALL : ('Forall'|'forall');
EXISTS : ('Exists'|'exists');
BOOL: ('true'|'false');

LINE_COMMENT : '//' .*? ('\n'|EOF)	-> channel(HIDDEN) ;
COMMENT      : '/*' .*? '*/'    	-> channel(HIDDEN) ;

ID  : [a-zA-Z_] [a-zA-Z0-9_]* ;
INT : [0-9]+ ;
SYM : [!-!#-&*-/<->|-|]+;

MATH_UNICODE_SYM
    :   [\u2200-\u22FF]
    |   [\u27F0-\u27FF]
    |   [\u2A00-\u2AFF]
    |   [\u2300-\u23BF]
    |   [\u0370-\u03FF] //greek letters
    ;

CHAR: '\'' . '\'' ;
RAW_STRING :  '\'' (ESC | ~["\\])* '\'' ;
STRING :  '"' (ESC | ~["\\])* '"' ;
fragment ESC :   '\\' ["\bfnrt] ;
WS : [ \t\n\r]+ -> channel(HIDDEN) ;