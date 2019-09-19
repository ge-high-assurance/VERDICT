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

val pname_to_string: pname -> string

val compare_pnames: pname -> pname -> int

val pp_print_pname: Format.formatter -> pname -> unit

val pp_print_qcref: Format.formatter -> qcref -> unit

val pp_print_numeric_literal: Format.formatter -> numeric_literal -> unit

val numeric_literal_to_string: numeric_literal -> string

