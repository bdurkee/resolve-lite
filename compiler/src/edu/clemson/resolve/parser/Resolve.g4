grammar Resolve;

moduleDecl
    :   (precisModuleDecl
    |   precisExtModuleDecl
    |   enhancementModuleDecl
    |   conceptModuleDecl
    |   conceptRealizationModuleDecl
    |   enhancementRealizationModuleDecl
    |   facilityModuleDecl) EOF
    ;

precisModuleDecl
    :   'Precis' name=ID ';'
        (usesList)?
        precisBlock
        'end' closename=ID ';' EOF
    ;

precisExtModuleDecl
    :   'Extension' name=ID 'for' precis=ID
        ('with' precisExt=ID)? ';'
        (usesList)?
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

enhancementModuleDecl
    :   'Enhancement' name=ID specModuleParameterList?
        'for' concept=ID ';'
        (usesList)?
        conceptBlock
        'end' closename=ID ';'
    ;

conceptRealizationModuleDecl
    :   'Realization' name=ID realizModuleParameterList?
        'for' concept=ID ';'
        (usesList)?
        implBlock
        'end' closename=ID ';'
    ;

enhancementRealizationModuleDecl
    :   'Realization' name=ID realizModuleParameterList?
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

// uses, imports

usesList
    :   'uses' (usesSpecs|'(' usesSpecs ')') ';'
    ;

usesSpecs
    :   moduleIdentifierSpec (',' moduleIdentifierSpec)*
    ;

moduleIdentifierSpec : id=ID fromClause?;
fromClause : 'from' moduleLibraryIdentifier ;
moduleLibraryIdentifier : ID ('.' ID)* ;

// module blocks & items

precisBlock
    :   ( mathStandardDefnDecl
        | mathCategoricalDefnDecl
        | mathInductiveDefnDecl
        | mathTheoremDecl
        | mathClssftnAssertionDecl
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
        | mathStandardDefnDecl
        )*
    ;

facilityBlock
    :   ( facilityDecl
        | operationProcedureDecl
        | typeRepresentationDecl
        | mathStandardDefnDecl
        )*
    ;

// type refs & decls

type
    :   (qualifier=ID '::')? name=ID            #namedType
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
        (varDeclGroup)* (stmt)*
        'end' ';'
    ;

// parameter and parameter-list related rules

specModuleParameterList
    :   '(' specModuleParameterDecl (';' specModuleParameterDecl)* ')'
    ;

realizModuleParameterList
    :   '(' realizModuleParameterDecl (';' realizModuleParameterDecl)* ')'
    ;

operationParameterList
    :   '(' (parameterDeclGroup (';' parameterDeclGroup)*)?  ')'
    ;

specModuleParameterDecl
    :   parameterDeclGroup
    |   mathStandardDefnDecl
    |   genericTypeParameterDecl
    ;

realizModuleParameterDecl
    :   parameterDeclGroup
    |   operationDecl
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
    :   'Var' ID (',' ID)* ':' type ';'
    ;

// facility decls

facilityDecl
    :   'Facility' name=ID 'is' spec=ID (specArgs=specModuleArgumentList)? specFrom=fromClause?
        (externally='externally')? 'realized' 'by' realiz=ID
        (realizArgs=realizModuleArgumentList)? realizFrom=fromClause?
        (enhancementPairing)* ';'
    ;

enhancementPairing
    :   'enhanced' 'by' spec=ID (specArgs=specModuleArgumentList)? specFrom=fromClause?
        (externally='externally')? 'realized' 'by' realiz=ID
        (realizArgs=realizModuleArgumentList)? realizFrom=fromClause?
    ;

realizModuleArgumentList
    :   '(' progExp (',' progExp)* ')'
    ;

specModuleArgumentList
    :   '(' specModuleArg (',' specModuleArg)* ')'
    ;

//this is cool, I want to match program expressions FIRST, then if
//I see a strange glyph (or something involving a strange glyph),
//THEN we parse the math and carry on as usual.
specModuleArg
    :   progExp
    |   mathExp
    ;

// operations & procedures

operationDecl
    :   'Operation' name=ID alt=progSymbolName? operationParameterList (':' type)? ';'
        (requiresClause)? (ensuresClause)?
    ;

operationProcedureDecl
    :   'Operation' name=ID operationParameterList (':' type)? ';'
        (requiresClause)?
        (ensuresClause)?
        (recursive='Recursive')? 'Procedure'
        decreasingClause?
        (varDeclGroup)*
        (stmt)*
        'end' closename=ID ';'
    ;

procedureDecl
    :   (recursive='Recursive')? 'Procedure' name=ID operationParameterList
        (':' type)? ';'
        //(noticeClause)*
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
    |   noticeStmt
    |   ifStmt
    ;

assignStmt : left=progExp ':=' right=progExp ';' ;
swapStmt : left=progExp ':=:' right=progExp ';' ;
callStmt : progParamExp ';' ;
noticeStmt : 'Notice' mathAssertionExp ';' ;

whileStmt
    :   'While' progExp
        changingClause? maintainingClause decreasingClause
        'do' stmt* 'end' ';'
    ;

ifStmt : 'If' progExp 'then' stmt* elseStmt? 'end' ';' ;
elseStmt : 'else' stmt* ;

// program expressions

/*
progExp
    :   progPrimary                                     #progPrimaryExp
    |   '(' progExp ')'                                 #progNestedExp
    |   lhs=progExp '.' rhs=progExp                     #progSelectorExp
    |   progExp progOperatorExp progExp                 #progInfixExp
    ;

progPrimary
    :   progLiteralExp
    |   progParamExp
    |   progNameExp
    ;

progParamExp
    :   progNameExp '(' (progExp (',' progExp)*)? ')'
    ;
*/
progExp
    :   lhs=progExp '.' rhs=progExp                     #progSelectorExp
    |   progExp op=progOperatorExp progExp              #progInfixExp
    |   '(' progExp ')'                                 #progNestedExp
    |   progPrimary                                     #progPrimaryExp
    ;

progPrimary
    :   progLiteralExp
    |   progParamExp
    |   progNameExp
    ;

progParamExp
    :   progNameExp '(' (progExp (',' progExp)*)? ')'
    ;

progNameExp
    :   (qualifier=ID '::')? name=ID
    ;

progOperatorExp
    :   (qualifier=ID '::')? name=progSymbolName
    ;

progSymbolName
    :   (ID | SYM)
    ;

progLiteralExp
    :   BOOL                #progBooleanLiteralExp
    |   INT                 #progIntegerLiteralExp
  //  |   CHAR                #progCharacterLiteralExp
    |   STRING              #progStringLiteralExp
    ;

// math constructs

mathTheoremDecl
    :   ('Corollary'|'Theorem') name=ID ':' mathAssertionExp ';'
    ;

//had better be an assertion involving the use of ':' or
mathClssftnAssertionDecl
    :   'Classification' 'Corollary' ':' mathAssertionExp ';'
    ;

mathDefnSig
    :   mathPrefixDefnSigs
    |   mathInfixDefnSig
    |   mathOutfixDefnSig
    |   mathMixfixDefnSig
    ;

mathPrefixDefnSig
    :   mathSymbolName (',' mathSymbolName)* ('('
        mathVarDeclGroup (',' mathVarDeclGroup)* ')')? ':' mathClssftnExp
    ;

mathPrefixDefnSigs
    :   mathPrefixDefnSig (',' mathPrefixDefnSig)*
    ;

mathInfixDefnSig
    :   '(' mathVarDecl ')' name=mathSymbolName
        '(' mathVarDecl ')' ':' mathClssftnExp
    ;

mathOutfixDefnSig
    :   leftSym=mathBracketOp '(' mathVarDecl ')'
        rightSym=mathBracketOp ':' mathClssftnExp
    ;

mathMixfixDefnSig
    :   '(' mathVarDecl ')' lop=mathBracketOp '(' mathVarDecl ')'
        rop=mathBracketOp ':' mathClssftnExp
    ;

mathSymbolName:     (ID | MATH_UNICODE_SYM | SYM | INT | BOOL) ;
mathBracketOp :     ('⟨'|'⟩'|'⌈'|'⌉'|'⎝'|'⎠'|'∥'|'['|']'|'|') ;

mathCategoricalDefnDecl
    :   'Categorical' 'Definition' 'for' mathPrefixDefnSigs
        'is' mathAssertionExp ';'
    ;

mathStandardDefnDecl
    :   (chainable='Chainable')? ('Implicit')? 'Definition' mathDefnSig
        ('is' body=mathAssertionExp)? ';'
    ;

mathInductiveDefnDecl
    :   'Inductive' 'Definition' mathDefnSig 'is'
        '(i.)' mathAssertionExp ';'
        '(ii.)' mathAssertionExp ';'
    ;

mathVarDeclGroup
    :   mathSymbolName (',' mathSymbolName)* ':' mathClssftnExp
    ;

mathVarDecl
    :   mathSymbolName ':' mathClssftnExp
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
decreasingClause : 'decreasing' mathExp ';' ;
entailsClause : 'which_entails' mathExp ;
noticeClause : 'Notice' mathExp ';' ;

// mathematical expressions

mathClssftnExp
    :   mathExp
    ;

mathAssertionExp
    :   mathExp
    |   mathQuantifiedExp
    ;

mathQuantifiedExp
    :   q=(FORALL|EXISTS) mathVarDeclGroup ',' mathAssertionExp
    ;

mathExp
    :   lhs=mathExp op='.' rhs=mathExp                                  #mathSelectorExp
    |   name=mathExp lop='(' mathExp (',' mathExp)* rop=')'             #mathPrefixAppExp
    |   mathExp mathBracketOp mathExp (',' mathExp)* mathBracketOp      #mathMixfixAppExp
    |   mathExp op=':' mathExp                                          #mathClssftnAssertionExp
    |   lhs=mathExp mathSymbolExp rhs=mathExp                           #mathInfixAppExp
    //|   l=mathExp op=('='|'≠') r=mathExp                                #mathEqualsAppExp
    |   '(' mathAssertionExp ')'                                        #mathNestedExp
    |   mathPrimeExp                                                    #mathPrimaryExp
    ;

mathPrimeExp
    :   mathCartProdExp
    |   mathSymbolExp
    |   mathSetRestrictionExp
    |   mathOutfixAppExp
    |   mathSetExp
    |   mathLambdaExp
    |   mathAlternativeExp
    ;

mathCartProdExp
    :   'Cart_Prod' (mathVarDeclGroup ';')+ 'end'
    ;

mathSymbolExp
    :   incoming='#'? (qualifier=ID '::')? name=mathSymbolName
    ;

mathOutfixAppExp
    :   lop=mathBracketOp mathExp rop=mathBracketOp
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

FORALL : ('Forall'|'∀');
EXISTS : ('Exists'|'∃');
BOOL : ('true'|'false');

LINE_COMMENT : '//' .*? ('\n'|EOF)	-> channel(HIDDEN) ;
COMMENT      : '/*' .*? '*/'    	-> channel(HIDDEN) ;

ID                  : [a-zA-Z_] [a-zA-Z0-9_]* ;
INT                 : [0-9]+ ;

//really I'm not sure we need this to be '+' at the end
SYM             : ('!'|'*'|'+'|'-'|'/'|'='|'/='|'~'|'<'|'>'|'>='|'<=') ;

MATH_UNICODE_SYM
    :   U_ARROW
    |   U_LOGIC
    |   U_LETTER
    |   U_OPERATOR
    |   U_RELATION
    |   [\u0370-\u03FF] //all greek letters
    ;

U_ARROW        : ('←'|'⇐'|'⟵'|'⟸'|'→'|'⇒'|'⟶'|'⟹'|'↔'|'⇔'|'⟷'|
                  '⟺'|'↩'|'↪'|'↽'|'⇁'|'↼'|'⇀'|'⇌'|'↝'|'⇃'|'⇂'|'↿'|'↾'|
                  '↑'|'⇑'|'↓'|'⇓'|'↕'|'⇕'|'↤'|'↦'|'↢'|'↣') ;

U_LOGIC        : ('∧'|'⋀'|'∨'|'⋁'|'¬'|'⋄') ;

/* Notice that U_BRACKET glyphs are special: they don't extend MATH_UNICODE_SYM */
U_LETTER       : ('𝔸'|'𝔹'|'ℂ'|'𝔻'|'𝔼'|'𝔽'|'𝔾'|'ℍ'|'𝕀'|'𝕁'|'𝕂'|'𝕃'|'𝕄'|'ℕ'|'𝕆'|
                  'ℙ'|'ℚ'|'ℝ'|'𝕊'|'𝕋'|'ℤ'|'℘') ;

U_OPERATOR     : ('∩'|'⋂'|'∪'|'⋃'|'⊔'|'⨆'|'⊓'|'⨅'|'∝'|'⊎'|'⨄'|'±'|'∓'|'×'|'÷'|
                  '⋅'|'⋆'|'∙'|'∘'|'⊕'|'⨁'|'⊗'|'⨂'|'⊙'|'⨀'|'⊖'|'⟕'|'⟖'|'⟗'|
                  '∑'|'∏'|'⨿'|'∐'|'⋈'|'⋉'|'⋊'|'⊠'|'⊡'|'∎'|'⨪') ;

U_RELATION     : ('≤'|'≥'|'≪'|'≫'|'≲'|'≳'|'⪅'|'⪆'|'∈'|'∉'|'⊂'|'⊃'|'⊆'|'⊇'|'⊏'|
                  '⊐'|'⊑'|'⊒'|'∼'|'≐'|'≃'|'≈'|'≍'|'≠'| '≅'|'≡'|'≼'|'≽'|'⊲'|'⊳'|
                  '⊴'|'⊵'|'△'|'≜') ;

CHAR: '\'' . '\'' ;
RAW_STRING : '\'' (ESC | ~["\\])* '\'' ;
STRING : '"' (ESC | ~["\\])* '"' ;
fragment ESC : '\\' ["\bfnrt] ;
WS : [ \t\n\r]+ -> channel(HIDDEN) ;