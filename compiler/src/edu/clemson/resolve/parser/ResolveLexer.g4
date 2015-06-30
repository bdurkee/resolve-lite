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
lexer grammar ResolveLexer;

// keywords

BY				:	'by'							;
CATEGORICAL		:	'Categorical'					;
CART_PROD		:	'Cart_Prod'						;
CONCEPT	    	:	'Concept'						;
CONSTRAINT		:	('constraint'|'constraints')	;
CONVENTION		:	'convention'					;
COROLLARY		:	'Corollary'						;
CORRESPONDENCE 	:	'correspondence'				;
DEFINITION		:	'Definition'					;
DEPENDENT		:	'DependentTerms'				;
END         	:   'end'							;
ENTAILS			:	'which_entails'					;
EXISTS			:	'Exists'						;
EXTERNALLY		:	'externally'					;
EXEMPLAR		:	'exemplar'						;
FACILITY		:	'Facility'						;
FAMILY			:	'Family'						;
FOR 			:	'for'							;
FORALL			:	'Forall'						;
IMPLEMENTED		:	'implemented'					;
IMPL			:	'Implementation'				;
IMPLICIT		:	'Implicit'						;
INIT			:	'initialization'				;
INDUCTIVE		:	'Inductive'						;
INTERSECT		:	'intersect'						;
IS				:	'is'							;
IF				:	'if'							;
LAMBDA			:	'lambda'						;
MODELED			:	'modeled'						;
OF				:	'of'							;
ON 				:	'on'							;
OPERATION		:	('Operation'|'Oper')			;
OTHERWISE		:	'otherwise'						;
RECORD			:	'Record'						;
RECURSIVE		:	'Recursive'						;
REQUIRES		:	'requires'						;
PRECIS      	:   'Precis'						;
PROCEDURE		:	('Procedure'|'Proc')			;
ENSURES			:	'ensures'						;
THEOREM			:	'Theorem'						;
TYPE			:	'Type'							;
USES        	:   'uses'							;
UNION			:	'union'							;
VAR				:	'Var'							;

// parameter modes

ALTERS		:	('alters'|'alt')					;
UPDATES		:	('updates'|'upd')					;
EVALUATES	:	('evaluates'|'eval')				;
CLEARS		:	('clears'|'clr')					;
RESTORES	:	('restores'|'rest')					;
PRESERVES	:	('preserves'|'pres')				;
REPLACES	:	('replaces'|'rpl')					;

// punctuation

CAT 		:	'o'								;
COLON		:	':'								;
COLONCOLON	:	'::'							;
COMMA       :	','								;
DBL_LBRACE	:	'{{'							;
DBL_RBRACE	:	'}}'							;
DOT			:	'.'								;
RBRACE		:	'}'								;
LPAREN		:	'('								;
RPAREN		:	')'								;
SEMI        :   ';'								;
LBRACE		:	'{'								;
BASE_CASE	:	'(i.)'							;
INDUCT_CASE :	'(ii.)'							;

// operators

DIVIDE		:	'/'								;
PLUS		:	'+'								;
MINUS		:	'-'								;
CUTMINUS	:	'.-.'							;
MULT		:	'*'								;
TILDE		:	'~'								;
AND			:	'and'							;
OR 			:	'or'							;
IMPLIES		:	'implies'						;
IS_IN		:	'is_in'							;
IS_NOT_IN	:	'is_not_in'						;
RANGE		:	'..'							;
TRIPLEDOT	:	'...'							;
RARROW		:	'->'							;
LT 			:	'<'								;
GT			:	'>'								;
LTE			:	'<='							;
GTE			:	'>='							;
EQUALS		:	'='								;
NEQUALS		:	'/='							;
AT			:	'@'								;
BAR			:	'|'								;
DBL_BAR		:	'||'							;

INT			:	('0'|[1-9]+ [0-9]*)				;
BOOL		:	('B'|'true'|'false')			;
ID			:	NameStartChar NameChar*         ;
WS          :	[ \t\r\n\f]+ -> channel(HIDDEN)	;

DOC_COMMENT
	:	'(**' .*? ('*)' | EOF) -> channel(HIDDEN)
	;

BLOCK_COMMENT
	:	'(*' .*? ('*)' | EOF)  -> channel(HIDDEN)
	;

LINE_COMMENT
	:	'--' ~[\r\n]*  -> channel(HIDDEN)
	;

fragment
NameChar
	:   NameStartChar
	|   '0'..'9'
	|   '_'
	;

fragment
NameStartChar
	:   'A'..'Z'
	|   'a'..'z'
	;

// -----------------
// Illegal Character
//
// This is an illegal character trap which is always the last rule in the
// lexer specification. It matches a single character of any value and being
// the last rule in the file will match when no other rule knows what to do
// about the character. It is reported as an error but is not passed on to the
// parser. This means that the parser to deal with the gramamr file anyway
// but we will not try to analyse or code generate from a file with lexical
// errors.
//
ERRCHAR
	:	.	-> channel(HIDDEN)
	;