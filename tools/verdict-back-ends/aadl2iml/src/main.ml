(* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.
*)

(** @author Daniel Larraz *)

let process_ast ast =
  (*Format.printf "@[<v>== AADL Model ==@,%a@]@."
    AADLAst.pp_print_ast ast ;
  *)
  Format.printf "%a@." AADLAst.pp_print_ast ast

let read_input_from_files filenames =
  let rec loop acc = function
    | [] -> Ok acc
    | fname :: tl -> (
      match AADLInput.from_file fname with
      | Ok input -> loop (List.rev_append input acc) tl
      | other -> other
    )
  in
  loop [] filenames


let write_vdm_iml_model_to_file vdm_iml output_file =
  let out_ch = open_out output_file in
  try (
    let ppf = Format.formatter_of_out_channel out_ch in
    Format.fprintf ppf "%a@." VDMIML.pp_print_vdm_iml vdm_iml;
    close_out out_ch
  )
  with e -> (
    close_out_noerr out_ch;
    raise e
  )

let expand_dirs input_files =
  let has_aadl_extension fname =
    let len = String.length fname in
    len > 5 && (String.sub fname (len-5) 5) = ".aadl"
  in
  let rec loop result = function
    | f::fs when Sys.is_directory f ->
        Sys.readdir f
        |> Array.to_list
        |> List.map (Filename.concat f)
        |> List.append fs
        |> loop result
    | f::fs when has_aadl_extension f ->
        loop (f::result) fs
    | _::fs -> loop result fs
    | []    -> result
  in
  loop [] input_files


let process_aadl_project input output_file verdict_props =
  let input = AADLInput.merge_packages (List.rev input) in
  match List.find_opt AADLAst.is_aadl_package input with
  | None -> (
    Format.eprintf "aadl2iml: no IML model generated!@."; exit 6
  )
  | Some ast -> (
    match AADL2VDMIML.aadl_ast_to_vdm_iml verdict_props ast with
    | None -> (
      Format.eprintf "aadl2iml: no IML model generated!@."; exit 6
    )
    | Some vdm_iml -> (
      if output_file = "" then
        Format.printf "%a@." VDMIML.pp_print_vdm_iml vdm_iml
      else
        write_vdm_iml_model_to_file vdm_iml output_file
    )
  )
 
let process_aadl_project_error_version input output_file prop_set_name =
  (*List.iter process_ast (AADLInput.merge_packages (List.rev input));*)
  match AADLInput.get_verdict_properties prop_set_name input with
  | None -> Format.eprintf "Property set '%s' not found!@." prop_set_name
  | Some verdict_props -> process_aadl_project input output_file verdict_props

let process_aadl_project_warning_version input output_file prop_set_name =
  let verdict_props =
    match AADLInput.get_verdict_properties prop_set_name input with
    | Some vp -> vp
    | None -> (
      Format.eprintf "WARNING: Property set '%s' not found!@." prop_set_name;
      let name = (Position.dummy_pos, prop_set_name) in
      AADLAst.({ name; imported_units = []; declarations = [] }) 
    )
  in
  process_aadl_project input output_file verdict_props


let process_input_files input_files output_file prop_set_name =

  try (
    let input_files = expand_dirs input_files in
    match read_input_from_files input_files with
    | Ok input -> (
      match AADLInput.sort_model_units input with
      | Ok input -> (
        (* process_aadl_project_error_version input output_file prop_set_name *)
        process_aadl_project_warning_version input output_file prop_set_name
      )
      | Error _ ->
        Format.eprintf "aadl2iml: cycle found in AADL model!@."; exit 5
    )
    | Error (AADLInput.UnexpectedChar (pos, c)) ->
        Format.eprintf "%a: error: unexpected character ‘%c’@."
          Position.pp_print_position pos c;
        exit 4

    | Error (AADLInput.SyntaxError pos) ->
        Format.eprintf "%a: syntax error@." Position.pp_print_position pos;
        exit 4
  )
  with
  | Sys_error msg ->
      Format.eprintf "%s@." msg; exit 3
  | e ->
      Format.eprintf "%s@." (Printexc.to_string e); exit 3


let parse_command_line_args () =
  let num_args = (Array.length Sys.argv) - 1 in
  if num_args < 3 then
    None
  else if Sys.argv.(3) = "-o" then
    if num_args < 5 then
      None
    else (
      let input_files =
        Array.to_list (Array.sub Sys.argv 5 (num_args - 4))
      in
      Some (input_files, Sys.argv.(4), Sys.argv.(2))
    )
  else
    let input_files =
      Array.to_list (Array.sub Sys.argv 3 (num_args - 2))
    in
    Some (input_files, "", Sys.argv.(2))

let main () =
  match parse_command_line_args () with
  | Some (input_files, output_file, prop_set_name) ->
    process_input_files input_files output_file prop_set_name
  | None ->
    Format.eprintf
      "Usage: %s -ps <prop_set_name> [-o <output.iml>] <file_1.aadl|dir1> ... <file_N.aadl|dir_N>@."
      Sys.argv.(0);
    exit 1

let () = main ()

