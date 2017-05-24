/**
 * Copyright 2017 The GreyCat Authors.  All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
grammar GreyCatModel;

fragment ESC :   '\\' (["\\/bfnrt] | UNICODE) ;
fragment UNICODE : 'u' HEX HEX HEX HEX ;
fragment HEX : [0-9a-fA-F] ;

STRING :  '"' (ESC | ~["\\])* '"' | '\'' (ESC | ~["\\])* '\'' ;
IDENT : [a-zA-Z_][a-zA-Z_0-9]*;
TYPE_NAME : [a-zA-Z_][a-z0-9]*;
NUMBER : [\-]?[0-9]+'.'?[0-9]*;
WS : ([ \t\r\n]+ | SL_COMMENT) -> skip ; // skip spaces, tabs, newlines
SL_COMMENT :  '//' ~('\r' | '\n')* ;

modelDcl: (enumDcl | classDcl)*;

enumDcl: 'enum' (TYPE_NAME|IDENT) '{' enumLiteralsDcl '}';
enumLiteralsDcl: IDENT (',' IDENT)*;

classDcl: 'class' (TYPE_NAME|IDENT) parentDcl? '{' (attributeDcl | relationDcl | indexDcl)* '}';
parentDcl: 'extends' (TYPE_NAME|IDENT);
attributeDcl: 'att' IDENT ':' attributeTypeDcl;
attributeTypeDcl: ('String' | 'Double' | 'Long' | 'Integer' | 'Boolean') ('[]')?;
relationDcl: 'rel' IDENT ':' (TYPE_NAME|IDENT) relationIndexDcl?;
relationIndexDcl: 'indexed' 'by' IDENT (',' IDENT)*;

indexDcl: 'indexed' withTimeDcl? 'by' IDENT (',' IDENT)* ('as' indexNameDcl)?;
withTimeDcl: 'with time';
indexNameDcl: IDENT;
