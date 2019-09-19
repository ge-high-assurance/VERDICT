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

