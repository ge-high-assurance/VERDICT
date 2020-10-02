grammar Lustre;

program
   : (include | typeDecl | constDecl | node | contractNode | importedNode)* EOF
   ;

include returns [java.io.File includeFile, com.ge.verdict.lustre.LustreParser.ProgramContext includeProgramContext]
   : 'include' STRING ';'?
   ;

typeDecl
   : 'type' oneTypeDecl+
   ;

oneTypeDecl
   : ID ';'
   | ID '=' type ';'
   ;

type returns [verdict.vdm.vdm_data.DataType dataType]
   : 'int' # intType
   | 'subrange' '[' bound ',' bound ']' 'of' 'int' # subrangeType
   | 'bool' # boolType
   | 'real' # realType
   | type '^' expr # arrayType
   | type '[' expr ']' # arrayType
   | '[' type (',' type)* ']' # tupleType
   | 'struct'? '{' field (';' field)* ';'? '}' # recordType
   | 'enum' '{' ID (',' ID)* '}' # enumType
   | identifier # userType
   ;

bound
   : '-'? INT
   | ID
   ;

expr returns [verdict.vdm.vdm_lustre.Expression expression]
   : BOOL # boolExpr
   | INT # intExpr
   | REAL # realExpr
   | identifier # idExpr
   | op = 'not' expr # unaryExpr
   | op = ('-' | 'pre' | 'current') expr # unaryExpr
   | op = ('int' | 'real') expr # castExpr
   | expr op = 'when' expr # binaryExpr
   | expr op = ('->' | 'fby') expr # binaryExpr
   | expr op = 'and' expr # binaryExpr
   | expr op = ('or' | 'xor') expr # binaryExpr
   | < assoc = right > expr op = '=>' expr # binaryExpr
   | expr op = ('<' | '<=' | '=' | '>=' | '>' | '<>') expr # binaryExpr
   | expr op = ('*' | '/' | '%' | 'div' | 'mod') expr # binaryExpr
   | expr op = ('+' | '-') expr # binaryExpr
   | 'if' expr 'then' expr 'else' expr # ifThenElseExpr
   | op = ('#' | 'nor') '(' expr (',' expr)* ')' # naryExpr
   | userOp '(' (expr (',' expr)*)? ')' # callExpr
   | '[' expr (',' expr)* ']' # arrayExpr
   | expr op = '^' expr # binaryExpr
   | expr op = '|' expr # binaryExpr
   | array = expr '[' selector = expr ('..' trancheEnd = expr ('step' sliceStep = expr)?)? ']' # arraySelectionExpr
   | expr '.' ID # recordProjectionExpr
   | ID '{' fieldExpr (';' fieldExpr)* '}' # recordExpr
   | '(' expr (',' expr)* ')' # listExpr
   | '{' expr (',' expr)* '}' # tupleExpr
   | 'merge' ID mergeCase+ # mergeExpr
   ;

identifier
   : ID
   | ID? '::' ID
   ;

userOp
   : (identifier | '*' | '/' | 'div' | 'mod' | '+' | '-' | '<' | '<=' | '>' | '>=' | '=' | '<>' | '|' | 'and' | 'or' | 'xor' | 'if')
   | iterator = ('map' | 'red' | 'fill' | 'fillred' | 'boolred') '<<' userOp (',' | ';') expr '>>'
   ;

fieldExpr returns [verdict.vdm.vdm_lustre.FieldDefinition fieldDefinition]
   : ID '=' expr
   ;

mergeCase
   : '(' (identifier | BOOL) '->' expr ')'
   ;

field
   : ID (',' ID)* ':' type
   ;

constDecl
   : 'const' oneConstDecl+
   ;

oneConstDecl
   : ID (',' ID)* (':' type)? ';'
   | ID (':' type)? '=' expr ';'
   ;

node
   : 'unsafe'? 'extern'? nodeType = ('node' | 'function') ID staticParams? '(' input = paramList? ';'? ')' 'returns' '(' output = paramList? ')' ';'? inlineContract? importedContract* nodeBody?
   ;

staticParams
   : '<<' staticParam (';' staticParam)? '>>'
   ;

staticParam
   : 'type' ID
   | 'const' ID ':' type
   | 'unsafe'? ('node' | 'function') ID '(' paramList? ';'? ')' 'returns' '(' paramList? ')'
   ;

paramList returns [java.util.List<verdict.vdm.vdm_lustre.NodeParameter> nodeParameters]
   : paramGroup (';' paramGroup)*
   ;

paramGroup returns [java.util.List<verdict.vdm.vdm_lustre.NodeParameter> nodeParameters]
   : isConst = 'const'? ID (',' ID)* (':' type declaredClock?)?
   ;

declaredClock
   : 'when' clockExpr
   ;

clockExpr
   : identifier '(' ID ')'
   | ID
   | 'not' ID
   | 'not' '(' ID ')'
   ;

inlineContract returns [verdict.vdm.vdm_lustre.ContractSpec spec]
   : '(*' '@' 'contract' (symbol | assume | guarantee | contractMode | contractImport)* '*)'
   ;

symbol returns [verdict.vdm.vdm_lustre.SymbolDefinition def]
   : keyword = ('const' | 'var') ID (':' type)? '=' expr ';'
   ;

assume returns [verdict.vdm.vdm_lustre.ContractItem item]
   : 'assume' expr ';'
   ;

guarantee returns [verdict.vdm.vdm_lustre.ContractItem item]
   : 'guarantee' STRING? expr ';'
   ;

contractMode returns [verdict.vdm.vdm_lustre.ContractMode mode]
   : 'mode' ID '(' ('require' require += expr ';')* ('ensure' ensure += expr ';')* ')' ';'
   ;

contractImport returns [verdict.vdm.vdm_lustre.ContractImport imprt]
   : 'import' ID '(' (inputArg += expr)? (',' inputArg += expr)* ')' 'returns' '(' (outputArg += expr)? (',' outputArg += expr)* ')' ';'
   ;

importedContract returns [verdict.vdm.vdm_lustre.ContractImport imprt]
   : '(*' '@' 'contract' 'import' ID '(' (inputArg += expr)? (',' inputArg += expr)* ')' 'returns' '(' (outputArg += expr)? (',' outputArg += expr)* ')' ';'? '*)'
   ;

nodeBody returns [verdict.vdm.vdm_lustre.NodeBody body]
   : localDecl* 'let' definition* 'tel' ';'?
   ;

localDecl returns [java.util.List<verdict.vdm.vdm_lustre.VariableDeclaration> variableDeclarations, java.util.List<verdict.vdm.vdm_lustre.ConstantDeclaration> constantDeclarations]
   : 'var' localVarDeclList+
   | 'const' localConstDecl+
   ;

localVarDeclList returns [java.util.List<verdict.vdm.vdm_lustre.VariableDeclaration> variableDeclarations]
   : ID (',' ID)* (':' type declaredClock?)? ';'
   ;

localConstDecl returns [verdict.vdm.vdm_lustre.ConstantDeclaration constantDeclaration]
   : ID (':' type)? '=' expr ';'
   ;

importedNode
   : nodeType = ('node' | 'function') 'imported' ID '(' input = paramList? ')' 'returns' '(' output = paramList? ')' ';' inlineContract?
   ;

contractNode
   : 'contract' ID '(' input = paramList? ')' 'returns' '(' output = paramList? ')' ';' contractSpec
   ;

contractSpec returns [verdict.vdm.vdm_lustre.ContractSpec spec]
   : 'let' (symbol | assume | guarantee | contractMode | contractImport)* 'tel' ';'?
   ;

definition
   : (equation | assertion | property | main | realizabilityInputs | ivc)
   ;

equation returns [verdict.vdm.vdm_lustre.NodeEquation equa]
   : ('(' leftList ')' | leftList) '=' expr ';'
   ;

leftList returns [verdict.vdm.vdm_lustre.NodeEquationLHS lhs]
   : left (',' left)*
   ;

assertion
   : 'assert' expr ';'
   ;

left
   : identifier
   | left '.' ID
   | left '[' expr ('..' expr)? ']'
   ;

property returns [verdict.vdm.vdm_lustre.NodeProperty prop]
   : '--%PROPERTY' STRING? expr ';'
   | '--%PROPERTY' STRING? equation ';'
   ;

main
   : '--%MAIN' ';'?
   ;

realizabilityInputs
   : '--%REALIZABLE' (ID (',' ID)*)? ';'
   ;

ivc
   : '--%IVC' (ID (',' ID)*)? ';'
   ;

STRING
   : '"' ('\\\\' | '\\"' | ~ [\\"])* '"'
   ;

REAL
   : INT '.' INT? Exponent?
   | INT Exponent
   ;

fragment Exponent
   : 'E' ('+' | '-')? INT
   ;

BOOL
   : 'true'
   | 'false'
   ;

INT
   : [0-9]+
   ;

ID
   : [a-zA-Z_] [a-zA-Z_0-9]*
   ;

WS
   : [ \t\n\r\f]+ -> skip
   ;

SL_COMMENT
   : '--' (~ [%\n\r] ~ [\n\r]* | /* empty */) ('\r'? '\n')? -> channel(HIDDEN)
   ;

ML_COMMENT
   : '(*' (~ '@' .*? | /* empty */) '*)' -> channel(HIDDEN)
   ;

ML_COMMENT2
   : '/*' .*? '*/' -> channel(HIDDEN)
   ;

ERROR
   : .
   ;
