(* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.
*)

(** @author Daniel Larraz *)

type input = AADLAst.t list

type parse_error =
  | UnexpectedChar of Position.t * char
  | SyntaxError of Position.t

val from_channel: in_channel -> (input, parse_error) result

val from_file: string -> (input, parse_error) result

type sort_error =
  | CycleFound

(* Returns list of model units sorted by dependency order of
   imported model units, or an error if a cycle was found *)
val sort_model_units: input -> (input, sort_error) result

val merge_packages: input -> input

(* [get_verdict_properties n i a] returns [Some p] if there is an property set [p]
   in [i] named [n], or [None] if no such property set exists.
*)
val get_verdict_properties: string -> input -> AADLAst.property_set option

