(* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.
*)

(** @author Daniel Larraz *)

let newline = '\r' | '\n' | "\r\n"

rule ignore_annex = parse
  | "**}"            { }
  | newline          { Lexing.new_line lexbuf; ignore_annex lexbuf }
  | _                { ignore_annex lexbuf }

