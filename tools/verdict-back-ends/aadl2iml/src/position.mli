(* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.
*)

(** @author Daniel Larraz *)

type t = { fname : string; line: int; col: int }

val dummy_pos: t

val mk_position: Lexing.position -> t

val get_position: Lexing.lexbuf -> t

val pp_print_position: Format.formatter -> t -> unit

