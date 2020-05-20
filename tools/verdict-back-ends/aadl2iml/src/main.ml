(* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.
*)

(** @author Daniel Larraz *)

let process_ast ast =
  (*Format.printf "@[<v>== AADL Model ==@,%a@]@."
    AADLAst.pp_print_ast ast ;
  Format.printf "@[<v>== IML Model ==@,%a@]@."
    AADL2IML.pp_print_aadl_ast_as_iml ast;*)

  Format.printf "%a@." AADLAst.pp_print_ast ast
  (* Format.printf "%a@." AADL2IML.pp_print_aadl_ast_as_iml ast *)

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
    | f::fs -> loop result fs
    | []    -> result
  in
  loop [] input_files

let process_input_files input_files output_file =

  try (
    let input_files = expand_dirs input_files in
    match read_input_from_files input_files with
    | Ok input -> (
      match AADLInput.sort_model_units input with
      | Ok input -> (
        (*List.iter process_ast (AADLInput.merge_packages (List.rev input));*)
        match AADLInput.get_verdict_properties input with
        | None -> Format.eprintf "VERDICT Properties file not found!"
        | Some verdict_props ->
          let input = AADLInput.merge_packages (List.rev input) in
          match List.find_opt AADLAst.is_aadl_package input with
          | None -> ()
          | Some ast -> (
            match AADL2VDMIML.aadl_ast_to_vdm_iml verdict_props ast with
            | None -> ()
            | Some vdm_iml -> (
              if output_file = "" then
                Format.printf "%a@." VDMIML.pp_print_vdm_iml vdm_iml
              else
                write_vdm_iml_model_to_file vdm_iml output_file
            )
          )
      )
      | Error _ ->
        Format.eprintf "Cycle found!@."
    )
    | Error (AADLInput.UnexpectedChar (pos, c)) ->
        Format.eprintf "%a: error: unexpected character ‘%c’@."
          Position.pp_print_position pos c

    | Error (AADLInput.SyntaxError pos) ->
        Format.eprintf "%a: syntax error@." Position.pp_print_position pos
  )
  with
  | Sys_error msg ->
      Format.eprintf "%s@." msg
  | e ->
      Format.eprintf "%s@." (Printexc.to_string e)


let parse_command_line_args () =
  let num_args = (Array.length Sys.argv) - 1 in
  if num_args = 0 then
    None
  else if Sys.argv.(1) = "-o" then
    if num_args < 3 then
      None
    else (
      let input_files =
        Array.to_list (Array.sub Sys.argv 3 (num_args - 2))
      in
      Some (input_files, Sys.argv.(2))
    )
  else
    Some (List.tl (Array.to_list Sys.argv), "")

let main () =
  match parse_command_line_args () with
  | Some (input_files, output_file) ->
    process_input_files input_files output_file
  | None ->
    Format.printf
      "Usage: %s [-o <output.iml>] <file_1.aadl|dir1> ... <file_N.aadl|dir_N>@."
      Sys.argv.(0)

let () = main ()

