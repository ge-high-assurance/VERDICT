(* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.
*)

(** @author Daniel Larraz *)

type t = { fname : string; line: int; col: int }

let dummy_pos = { fname = ""; line = -1; col = -1 }

let mk_position pos =
  {
    fname = pos.Lexing.pos_fname ;
    line = pos.Lexing.pos_lnum ; 
    col = pos.Lexing.pos_cnum - pos.Lexing.pos_bol + 1 
  }

let get_position lexbuf =
  mk_position (Lexing.lexeme_start_p lexbuf)

let pp_print_position fmt { fname; line; col } =
  let fname = if fname = "" then "<stdin>" else fname in
  Format.fprintf fmt "%s:%d:%d" fname line col

