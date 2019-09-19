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

type qcref = pname * pid option

type sign_type = Positive | Negative | NoSign

type numeric_literal = {
  sign: sign_type;
  lit: string;
  units: pid option;
}

let pp_print_id ppf (_, id) = Format.fprintf ppf "%s" id

let get_id (_, id) = id

let pp_print_pname ppf pn =
  let pp_sep ppf () = Format.fprintf ppf "::" in
  Format.pp_print_list ~pp_sep pp_print_id ppf pn

let pname_to_string pn = Format.asprintf "%a" pp_print_pname pn

let compare_pnames pn1 pn2 =
  String.compare (pname_to_string pn1) (pname_to_string pn2)

let pp_print_qcref ppf = function
  | pname, None ->
      pp_print_pname ppf pname
  | pname, Some pid ->
      Format.fprintf ppf "%a.%a"
        pp_print_pname pname pp_print_id pid

let pp_print_sign ppf = function
  | NoSign -> ()
  | Positive -> Format.fprintf ppf "+"
  | Negative -> Format.fprintf ppf "-"

let pp_print_units_opt ppf = function
  | None -> ()
  | Some u -> pp_print_id ppf u

let pp_print_numeric_literal ppf { sign; lit; units } =
  Format.fprintf ppf "%a%s%a"
    pp_print_sign sign lit pp_print_units_opt units

let numeric_literal_to_string nl =
  Format.asprintf "%a" pp_print_numeric_literal nl
