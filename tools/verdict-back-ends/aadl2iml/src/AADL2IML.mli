(* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.
*)

(** @author Daniel Larraz *)

val pp_print_aadl_ast_as_iml: Format.formatter -> AADLAst.t -> unit

val pp_print_aadl_input_as_iml: Format.formatter -> AADLInput.input -> unit

