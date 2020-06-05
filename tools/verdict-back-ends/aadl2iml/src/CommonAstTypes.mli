(* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.
*)

(** @author Daniel Larraz *)

type pid = Position.t * string

type pname = pid list

type qcref = pname * pid option (* (pid '::')* pid . (pid)? *)

type sign_type = Positive | Negative | NoSign

type numeric_literal = {
  sign: sign_type;
  lit: string;
  units: pid option;
}

val pp_print_id: Format.formatter -> pid -> unit

val get_id: pid -> string

val get_pos: pid -> Position.t

val pname_to_string: pname -> string

val compare_pnames: pname -> pname -> int

val pp_print_pname: Format.formatter -> pname -> unit

val pp_print_qcref: Format.formatter -> qcref -> unit

val pp_print_numeric_literal: Format.formatter -> numeric_literal -> unit

val numeric_literal_to_string: numeric_literal -> string

