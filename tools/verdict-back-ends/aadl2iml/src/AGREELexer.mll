(* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.
*)

(** @author Daniel Larraz *)

{
  module P = AGREEParser

  exception Unexpected_Char of char

  exception Unexpected_EOF

  let char_for_backslash = function
    | 'n' -> '\010'
    | 'r' -> '\013'
    | 'b' -> '\008'
    | 't' -> '\009'
    | c -> c

  let string_buf = Buffer.create 1024
}

let whitespace = [' ' '\t']

let newline = '\r' | '\n' | "\r\n"

let digit = ['0'-'9']

let int_exponent = ('e'|'E') '+'? digit+

let extended_digit = ['0'-'9' 'a'-'f' 'A'-'F']

let based_integer = extended_digit ('_'? extended_digit)*

let integer_lit = digit+ ('_' digit+)* (('#' based_integer '#' int_exponent?) | int_exponent?)

let exponent = ('e'|'E') ('+'|'-')? digit+

let real_lit = digit+ ('_' digit+)* '.' digit+ ('_' digit+)* exponent?

let id = ['a'-'z' 'A'-'Z'] ['a'-'z' 'A'-'Z' '_' '0'-'9']*

rule token = parse
  | "assert"         { P.ASSERT }
  | "assign"         { P.ASSIGN }
  | "assume"         { P.ASSUME }
  | "guarantee"      { P.GUARANTEE }
  | "lemma"          { P.LEMMA }
  | "const"          { P.CONST }
  | "returns"        { P.RETURNS }
  | "node"           { P.NODE }
  | "enum"           { P.ENUM }
  | "floor"          { P.FLOOR }
  | "real"           { P.REAL }
  | "bool"           { P.BOOL }
  | "int"            { P.INT }
  | "let"            { P.LET }
  | "tel"            { P.TEL }
  | "var"            { P.VAR }
  | "pre"            { P.PRE }
  | "not"            { P.NOT }
  | "and"            { P.AND }
  | "or"             { P.OR }
  | "div"            { P.INT_DIV }
  | "mod"            { P.MOD }
  | "eq"             { P.EQ }
  | "if"             { P.IF }
  | "then"           { P.THEN }
  | "else"           { P.ELSE }
  | "true"           { P.TRUE }
  | "false"          { P.FALSE }
  | "prev"           { P.PREV }

  | "**}"            { P.ANNEX_BLOCK_END }
  | "<=>"            { P.EQUIV }
  | "->"             { P.ARROW }
  | "=>"             { P.IMPLIES }

  | "::"             { P.DOUBLE_COLON }

  | "<="             { P.LTE }
  | ">="             { P.GTE }
  | "!="             { P.NEQ }
  | "<>"             { P.NEQ }
  | '<'              { P.LT }
  | '>'              { P.GT }
  | '='              { P.EQUAL }
  | '*'              { P.TIMES }
  | '/'              { P.DIV }
  | '.'              { P.DOT }
  | ','              { P.COMMA }
  | ':'              { P.COLON }
  | ';'              { P.SEMICOLON }
  | '{'              { P.LCURLY_BRACKET }
  | '}'              { P.RCURLY_BRACKET }
  | '['              { P.LSQ_BRACKET }
  | ']'              { P.RSQ_BRACKET }
  | '('              { P.LPAREN }
  | ')'              { P.RPAREN }

  | '+'              { P.PLUS }
  | '-'              { P.MINUS }
  | '^'              { P.POWER }

  | '"'              { Buffer.clear string_buf; str lexbuf }

  | "--"             { sl_comment lexbuf }

  | integer_lit as l { P.INTEGER_LIT l }
  | real_lit as l    { P.REAL_LIT l }

  | id as name       { P.ID name }

  | ';'              { P.SEMICOLON }
  | whitespace       { token lexbuf }
  | newline          { Lexing.new_line lexbuf ; token lexbuf }
  | _ as c           { raise (Unexpected_Char c) }

and str = parse
  | '"'
      { P.STRING (Buffer.contents string_buf) }
  | "\\" (_ as c)
      { Buffer.add_char string_buf (char_for_backslash c); str lexbuf }
  | newline
      { Lexing.new_line lexbuf; Buffer.add_char string_buf '\n'; str lexbuf }
  | eof
      { raise Unexpected_EOF }
  | _ as c
      { Buffer.add_char string_buf c; str lexbuf }

and sl_comment = parse
  | newline
      { Lexing.new_line lexbuf; token lexbuf }
  | eof
      { raise Unexpected_EOF }
  | _
      { sl_comment lexbuf }

