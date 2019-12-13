
{
  module P = VerdictParser

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

let id = ['a'-'z' 'A'-'Z'] ['a'-'z' 'A'-'Z' '_' '0'-'9']*

let decimal = ['0'-'9']+ ('.' ['0'-'9']+)? (('E'|'e') ('+'|'-')? ['0'-'9']+)?

rule token = parse
  | "MissionReq" { P.MISSION }
  | "SafetyReq" { P.SAFETY_REQ }
  | "SafetyRel" { P.SAFETY_REL }
  | "Event" { P.SAFETY_EV }
  | "CyberReq" { P.CYBER_REQ }
  | "CyberRel" { P.CYBER_REL }
  | "ThreatEffectModel" { P.THREAT_MODEL }
  | "ThreatDefense" { P.THREAT_DEFENSE }
  | "ThreatDatabase" { P.THREAT_DATABASE }
  | "id" { P.PARAM_ID }
  | "cia" { P.CIA }
  | "severity" { P.SEVERITY }
  | "condition" { P.CONDITION }
  | "comment" { P.COMMENT }
  | "output" { P.OUTPUT }
  | "inputs" { P.INPUTS }
  | "faultSrc" { P.FAULT_SRC }
  | "happens" { P.HAPPENS }
  | "probability" { P.PROBABILITY }
  | "description" { P.DESCRIPTION }
  | "phases" { P.PHASES }
  | "external" { P.EXTERNAL }
  | "targetLikelihood" { P.TARGET_LIKELIHOOD }
  | "entities" { P.ENTITIES }
  | "assumptions" { P.ASSUMPTIONS }
  | "reference" { P.REFERENCE }
  | "threats" { P.THREATS }
  | "reqs" { P.CYBER_REQS }
  | "C" | "Confidentiality" { P.CIA_C }
  | "I" | "Integrity" { P.CIA_I }
  | "A" | "Availability" { P.CIA_A }
  | "None" { P.SEVERITY_NONE }
  | "Minor" { P.SEVERITY_MINOR }
  | "Major" { P.SEVERITY_MAJOR }
  | "Hazardous" { P.SEVERITY_HAZARDOUS }
  | "Catastrophic" { P.SEVERITY_CATASTROPHIC }

  | "**}" { P.ANNEX_BLOCK_END }

  | ":" { P.COLON }
  | "=" { P.EQUAL }
  | "=>" | "->" { P.IMPLIES }
  | ";" { P.SEMICOLON }
  | "," { P.COMMA }
  | "." { P.PERIOD }
  | '{' { P.LCURLY_BRACKET }
  | '}' { P.RCURLY_BRACKET }
  | '[' { P.LSQ_BRACKET }
  | ']' { P.RSQ_BRACKET }
  | '(' { P.LPAREN }
  | ')' { P.RPAREN }
  | '|' { P.VERT_BAR }

  | "and" | "&&" | "/\\" { P.AND }
  | "or" | "||" | "\\/" { P.OR }
  | "not" | "!" { P.NOT }

  | "forall" { P.FORALL }
  | "exists" { P.EXISTS }
  | "contains" { P.CONTAINS }

  | decimal as d { P.DECIMAL d }

(*
  | "1" { P.TARGET_LIKELIHOOD_VAL }
  | "1e-3" { P.TARGET_LIKELIHOOD_VAL }
  | "1e-03" { P.TARGET_LIKELIHOOD_VAL }
  | "1e-5" { P.TARGET_LIKELIHOOD_VAL }
  | "1e-05" { P.TARGET_LIKELIHOOD_VAL }
  | "1e-7" { P.TARGET_LIKELIHOOD_VAL }
  | "1e-07" { P.TARGET_LIKELIHOOD_VAL }
  | "1e-9" { P.TARGET_LIKELIHOOD_VAL }
  | "1e-09" { P.TARGET_LIKELIHOOD_VAL }
*)

  | '"' { Buffer.clear string_buf; str_double lexbuf }
  | "'" { Buffer.clear string_buf; str_single lexbuf }

  | "--" { sl_comment lexbuf }

  | id as name { P.ID name }

  | whitespace { token lexbuf }
  | newline { Lexing.new_line lexbuf; token lexbuf }
  | _ as c { raise (Unexpected_Char c) }

and str_double = parse
  | '"' { P.STRING (Buffer.contents string_buf) }
  | "\\" (_ as c) { Buffer.add_char string_buf (char_for_backslash c); str_double lexbuf }
  | newline { Lexing.new_line lexbuf; Buffer.add_char string_buf '\n'; str_double lexbuf }
  | eof { raise Unexpected_EOF }
  | _ as c { Buffer.add_char string_buf c; str_double lexbuf }

and str_single = parse
  | "'" { P.STRING (Buffer.contents string_buf) }
  | "\\" (_ as c) { Buffer.add_char string_buf (char_for_backslash c); str_single lexbuf }
  | newline { Lexing.new_line lexbuf; Buffer.add_char string_buf '\n'; str_single lexbuf }
  | eof { raise Unexpected_EOF }
  | _ as c { Buffer.add_char string_buf c; str_single lexbuf }

and sl_comment = parse
  | newline { Lexing.new_line lexbuf; token lexbuf }
  | eof { raise Unexpected_EOF }
  | _ { sl_comment lexbuf }
