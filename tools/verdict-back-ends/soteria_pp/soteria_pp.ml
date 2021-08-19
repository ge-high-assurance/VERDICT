(* 

Copyright © 2020 General Electric Company. All Rights Reserved.

Author: Paul Meng, Kit Siu
  
Date: 2019-11-04
  
Updates: 
    5/18/2020, Heber Herencia-Zapana, added inferring  cyber/safety parameters  ("-c","-s").
    7/20/2020, Kit Siu, updated with command line parsing using the Command library.
*)

(**
   Description: This tool provides a compositional, model-based framework
   for modeling, visualizing and analyzing the safety and security of system
   architectures. See the README file for instructions on how to build and use the tool.

   MODELING:

   The modeling language is designed to provide a minimal set of capabilities
   for modeling architectures at the level required to perform safety/
   reliability, as well as security analyses. The modeling language requires users to
   describe safety and security in a compositional manner; one defines how faults and
   attacks propagate through the components of an architecture. Once components are 
   modeled in this way, we wind up with a library of components that can be shared 
   throughout an organization, thereby allowing for consistency, reuse and rapid 
   architectural development. Our modeling language allows us to define components, 
   libraries and architectures. 

   KNOWN ISSUES

   If the visualizations generated have problems (e.g., node names extend beyond
   node shapes), then check that graphviz and your renderer are using the same
   fonts. This can cause problems, e.g., if graphviz is not using the same fonts
   as your renderer, then when graphviz determines the dimensions of bounding
   boxes, it will generate node shapes of the wrong size. 

*)

open Core
open PrintBox
open Xml
open FaultTree 
open AttackDefenseTree
open Qualitative
open Quantitative
open Modeling
open Validation
open TreeSynthesis
open Visualization
open ArchitectureSynthesis
open Translator
open TranslatorInputsValidation

(** Mapping between the file name and the file path *)
let input_file_table = Hashtbl.create ~size:5 (module String);;

let has_csv_extension file_name =
   Filename.check_suffix file_name ".csv"

let has_png_extension file_name =
   Filename.check_suffix file_name ".png"   

(** Collect all the CSV files in the input directory *)
let expand_dir input_dir =
  match Sys.is_directory input_dir with 
  | `Yes -> Sys.readdir input_dir
      |> Array.to_list
      |> List.filter ~f:(fun file_name -> (has_csv_extension file_name))
  | `No |`Unknown -> []

(** If the dir_path does not end with '/', we will
    append one for it. Otherwise return as it is. *)
let append_dir_sep dir_path = 
  match String.rindex dir_path Filename.dir_sep.[0] with 
  | Some index -> 
    if (String.length dir_path <> index + 1) then 
      dir_path^Filename.dir_sep
    else
      dir_path
  | None -> dir_path^Filename.dir_sep

(* Check if the csv files are using the designated names *)
let rec populate_hashtable input_dir file_names = 
   match file_names with
   | file_name::fns -> (

     (* Add the mapping to the hashtable*)
     let add_to_input_hashtable file_name file_path =
       Hashtbl.add_exn input_file_table ~key:file_name ~data:file_path in

     (* Construct the full input path *)
     let input_file_path = input_dir ^ file_name in

     (* Check if the first file name matches any of the designated names.
         If it matches, put the mapping between its name and path in the hashtable, 
         and continue with the rest file names. *)
     match file_name with 
     | "Mission.csv"
       -> add_to_input_hashtable "Mission.csv" input_file_path;
          populate_hashtable input_dir fns
     | "CompDep.csv" 
       -> add_to_input_hashtable "CompDep.csv" input_file_path;
          populate_hashtable input_dir fns      
     | "CompSaf.csv" 
       -> add_to_input_hashtable "CompSaf.csv" input_file_path;
          populate_hashtable input_dir fns      
     | "ScnConnections.csv" 
       -> add_to_input_hashtable "ScnConnections.csv" input_file_path;
          populate_hashtable input_dir fns      
     | "Defenses.csv" 
       -> add_to_input_hashtable "Defenses.csv" input_file_path; 
          populate_hashtable input_dir fns      
     | "Defenses2NIST.csv" 
       -> add_to_input_hashtable "Defenses2NIST.csv" input_file_path; 
          populate_hashtable input_dir fns      
     | "CAPEC.csv" 
       -> add_to_input_hashtable "CAPEC.csv" input_file_path;
          populate_hashtable input_dir fns
     | "Events.csv" 
       -> add_to_input_hashtable "Events.csv" input_file_path;
          populate_hashtable input_dir fns
     | _ -> populate_hashtable input_dir fns
   )
   | [] -> ()

(** Check if input_file_table has the correct number of csv files with designated names *)
let validate_input input_dir file_names = 
  match Sys.is_directory input_dir with 
  | `Yes -> 
    (populate_hashtable (append_dir_sep input_dir) file_names;
     if Hashtbl.length input_file_table = 8 then 
       Ok "Success" 
     else (
       Format.printf "Error: Insufficient input files!@.";
       Format.printf "       SOTERIA++ expects these input files: CAPEC.csv, CompDep.csv, CompSaf.csv, Defenses.csv, Defenses2NIST.csv, Events.csv, Mission.csv, ScnConnections.csv!@.";
       Error "Fail"
     )
    )
  | `No | `Unknown -> 
    Format.printf "Error: Invalid input directory: %s@." input_dir; 
    Error "Fail"
        

(** Remove old svg, pdf files and etc from the output_dir *)
let clean_up_dir output_dir =
  match Sys.is_directory output_dir with 
  | `Yes -> let output_dir_path = append_dir_sep output_dir in
      Sys.readdir output_dir
      |> Array.to_list
      |> List.map 
           ~f:(fun file_name -> output_dir_path ^ file_name)
      |> List.iter 
           ~f:(fun file_full_path -> 
             if (has_csv_extension file_full_path) 
               || (has_png_extension file_full_path) 
               || (Sys.is_directory_exn file_full_path) then () 
             else Sys.remove file_full_path)
  | `No |`Unknown -> ()

(** Clean up output_dir if it exists. Otherwise, 
return the input_dir as the output_dir *)
let process_output_dir input_dir output_dir =
  match Sys.is_directory output_dir with
  | `Yes -> 
    (clean_up_dir output_dir;
     append_dir_sep output_dir)
  | `No | `Unknown -> 
    (clean_up_dir input_dir;
     append_dir_sep input_dir)    

(** Execute the analysis *)
let execute input_dir output_dir infeCyber infeSafe saveDotML =
  try (
    (* Collect all CSV files *)
    let input_files = expand_dir input_dir in

    (* Check if all the CSV files are present with the designated names *)
    match validate_input input_dir input_files with
    | Ok _ -> (
      Format.printf 
        "Info: Got all input files: CAPEC.csv, CompDep.csv, CompSaf.csv, Defenses.csv, Defenses2NIST.csv, Events.csv, Mission.csv, ScnConnections.csv@."; 
      
      (* Process the output dir path: clean up old files or create a new directory *)
      let output_dir_path = process_output_dir input_dir output_dir in
      Format.printf 
        "Info: Output from SOTERIA++ will be generated at: %s@." output_dir_path;

      (* Obtain all the channels and pass them to the architecture analysis *)
      let mission_ch  = In_channel.read_lines (Hashtbl.find_exn input_file_table "Mission.csv") in
      let comp_dep_ch = In_channel.read_lines (Hashtbl.find_exn input_file_table "CompDep.csv") in
      let comp_saf_ch = In_channel.read_lines (Hashtbl.find_exn input_file_table "CompSaf.csv") in
      let scn_arch_ch = In_channel.read_lines (Hashtbl.find_exn input_file_table "ScnConnections.csv") in
      let defense_ch = In_channel.read_lines (Hashtbl.find_exn input_file_table "Defenses.csv") in
      let defense2nist_ch = In_channel.read_lines (Hashtbl.find_exn input_file_table "Defenses2NIST.csv") in
      let attack_ch =  In_channel.read_lines (Hashtbl.find_exn input_file_table "CAPEC.csv") in
      let events_ch =  In_channel.read_lines (Hashtbl.find_exn input_file_table "Events.csv") in
      let () = do_arch_analysis ~save_dot_ml:saveDotML comp_dep_ch comp_saf_ch attack_ch events_ch scn_arch_ch mission_ch defense_ch defense2nist_ch output_dir_path infeCyber infeSafe in

      Format.printf "Info: Done!@."
    )
    | Error _ -> ()
  )
  with
  | Sys_error msg ->
      Format.eprintf "%s@." msg 

(* command line parsing *)
let command =
  Command.basic
    ~summary: "SOTERIA++: a framework for analyzing and visualizing the safety and security of system architecture."
    ~readme:(fun () -> "Given the expected .csv input files in the input directory, calculates cutsets and outputs in .svg and .txt file formats.")
    Command.Let_syntax.(
       let%map_open 
          outputdir_param = flag "-o" (optional Filename.arg_type) ~doc:" output directory"
          and inputdir_param = anon ("inputdir" %: Filename.arg_type)
          and cyberInfer_flag = flag "-c" no_arg ~doc:" infer cyber relations if none are defined"
          and safetyInfer_flag = flag "-s" no_arg ~doc:" infer safety relations if none are defined"
          and saveDotML_flag = flag "-ml" no_arg ~doc:" save the .ml files with the SOTERIA++ library and model for debugging"
         in
         fun () -> 
          match outputdir_param with
          | Some o_param -> execute inputdir_param o_param cyberInfer_flag safetyInfer_flag saveDotML_flag
          | None -> execute inputdir_param inputdir_param cyberInfer_flag safetyInfer_flag saveDotML_flag );; 

(* run the command *)
let () = Command.run 
   ~version:"pre 1.0"  (* <-- TODO: check this default periodically *)
   ~build_info:"corebuild -pkg printbox -pkg xml-light soteria_pp.native" 
   command  ;;