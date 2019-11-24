(* Copyright (c) 2019 by the Board of Trustees of the University of Iowa

   Licensed under the Apache License, Version 2.0 (the "License"); you
   may not use this file except in compliance with the License.  You
   may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0 

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
   implied. See the License for the specific language governing
   permissions and limitations under the License. 

*)

(** @author Daniel Larraz *)

{
  module P = AADLParser

  exception Unexpected_Char of char

  exception Unexpected_EOF

  let char_for_backslash = function
    | 'n' -> '\010'
    | 'r' -> '\013'
    | 'b' -> '\008'
    | 't' -> '\009'
    | c -> c

  let string_buf = Buffer.create 1024

  (* Create and populate a keyword hashtable *)
  let mk_hashtbl keywords =
    let tbl =
      Hashtbl.create (List.length keywords)
    in
    keywords |> List.iter (fun (k, v) -> Hashtbl.add tbl k v) ;
    tbl

  let keyword_table = mk_hashtbl [
    "aadlboolean",    P.AADLBOOLEAN ;
    "aadlstring",     P.AADLSTRING ;
    "aadlinteger",    P.AADLINTEGER ;
    "aadlreal",       P.AADLREAL ;
    "enumeration",    P.ENUMERATION ;
    "property",       P.PROPERTY ;
    "package",        P.PACKAGE ;
    "system",         P.SYSTEM ;
    "implementation", P.IMPLEMENTATION ;
    "features",       P.FEATURES ;
    "properties",     P.PROPERTIES ;
    "subcomponents",  P.SUBCOMPONENTS ;
    "connections",    P.CONNECTIONS ;
    "public",         P.PUBLIC ;
    "private",        P.PRIVATE ;
    "constant",       P.CONSTANT ;
    "type",           P.TYPE ;
    "none",           P.NONE ;
    "inherit",        P.INHERIT ;
    "applies",        P.APPLIES ;
    "units",          P.UNITS ;
    "with",           P.WITH ;
    "list",           P.LIST ;
    "data",           P.DATA ;
    "port",           P.PORT ;
    "end",            P.END ;
    "set",            P.SET ;
    "all",            P.ALL ;
    "out",            P.OUT ;
    "in",             P.IN ;
    "is",             P.IS ;
    "to",             P.TO ;
    "of",             P.OF ;
    "true",           P.TRUE ;
    "false",          P.FALSE ;
  ]
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

(* let id = '^'? ['a'-'z' 'A'-'Z' '_'] ['a'-'z' 'A'-'Z' '_' '0'-'9']* *)
let id = ['a'-'z' 'A'-'Z'] ['a'-'z' 'A'-'Z' '_' '0'-'9']*


rule token = parse

  | "{**"            { P.ANNEX_BLOCK_START }
  | "->"             { P.R_ARROW }
  | "<->"            { P.LR_ARROW }
  | "=>"             { P.EQ_ARROW }
  | "+=>"            { P.PEQ_ARROW }
  | ".."             { P.DOT_DOT }
  | "::"             { P.DOUBLE_COLON }
  | '.'              { P.DOT }
  | ','              { P.COMMA }
  | ':'              { P.COLON }
  | ';'              { P.SEMICOLON }
  | '{'              { P.LCURLY_BRACKET }
  | '}'              { P.RCURLY_BRACKET }
  | '('              { P.LPAREN }
  | ')'              { P.RPAREN }

  | '+'              { P.PLUS }
  | '-'              { P.MINUS }

  | '*'              { P.STAR }

  | '"'              { Buffer.clear string_buf; str lexbuf }

  | "--"             { sl_comment lexbuf }

  | integer_lit as l { P.INTEGER_LIT l }
  | real_lit as l    { P.REAL_LIT l }

  | id as s          { 

    let lwc_s = String.lowercase_ascii s in
    try
      Hashtbl.find keyword_table lwc_s
    with Not_found -> (
      if String.equal lwc_s "annex" then
        P.ANNEX lexbuf
      else
        P.ID s
    )

  }

  | whitespace       { token lexbuf }
  | newline          { Lexing.new_line lexbuf ; token lexbuf }
  | eof              { P.EOF }
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
      { token lexbuf }
  | _
      { sl_comment lexbuf }

