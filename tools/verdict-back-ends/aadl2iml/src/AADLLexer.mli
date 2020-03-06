(* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.
*)

(** @author Daniel Larraz *)

exception Unexpected_Char of char

exception Unexpected_EOF

val token: Lexing.lexbuf -> AADLParser.token

