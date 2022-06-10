(* 

Copyright © 2019-2020 General Electric Company and United States Government as represented 
by the Administrator of the National Aeronautics and Space Administration.  All Rights Reserved.

Author: Pete Manolios
Date: 2017-12-15

Updates: 7/24/2018, Kit Siu, added dot_gen_show_direct_adtree_file and dot_gen_show_adtree_file
         10/22/2018, Kit Siu, added functions to print out cutset reports
         6/5/2022, Chris Alexander, added defense profile DAL suffix toggle

*)

(**
   Fault trees, Attack-Defense trees, formulas, and models can be visualized 
   in various ways. Here are the top-level visualization functions. In order 
   to use the visualization code, you have to have graphviz installed in your path.

   - {b dot_gen_show_direct_tree_file} : a function for directly visualizing
   fault trees.

   - {b dot_gen_show_direct_adtree_file} : a function for directly visualizing
   attack-defense trees.

   - {b dot_gen_show_tree_file} : a function for simplifying and visualizing
   fault trees.

   - {b dot_gen_show_adtree_file} : a function for simplifying and visualizing
   attack-defense trees.

   - {b dot_gen_show_formula_file} : a function for visualizing monotone Boolean
   formulas.

   - {b dot_gen_show_ph_file} : This function generates and displays a
   visualization of the physical architecture. By this I mean we get one node
   per instance of a component in a model.  If there are k outputs from
   component A that are connected to k inputs from component B, then only one
   edge is shown between the components. This gives us the highest-level view of
   the system, as is shows an abstract view showing only components and
   connectivity information (yes/no).

   - {b dot_gen_show_funct_file} : This function generates and displays a
   visualization of the functional architecture. This is an elaboration of the
   physical architecture that in addition shows input port and output ports. Now
   edges are from port to port, so we get a mode detailed view of the model. For
   example, in this view of a model, if there are k outputs from component A
   that are connected to k inputs from component B, then k edges are shown
   between the components as there is an edge from one port to another if the
   first port's output is the input to the second port.

   - {b dot_gen_show_fault_file} : This function generate and displays a
   visualization of the propagation of faults through the architecture.  This is
   an elaboration of the functional model that in addition shows basic events
   associated with components and shows what parts of the model can affect the
   probability of a the top-level fault. If an edge (corresponding to a flow of
   information from one component to another) can affect the top-level fault, it
   is colored red. If a basic event can affect the top-level fault, then a (set
   of) red edge(s) is added from that event to the output(s) along which the
   basic event propogates.
   
   - {b saveADCutSetsToFile} : This function generates a report with the top-level 
   likelihood (of success of attack) and a list of the cutsets.
   
   - {b saveCutSetsToFile} : This function generates a report with the top-level 
   probability (of failure) and a list of the cutsets. 

   Here is an example of how the above functions can be used to visualize and
   analyze the example library and model given above.

   {[
   dot_gen_show_ph_file nasa_handbook_model "mpnasa.gv";;
   dot_gen_show_funct_file nasa_handbook_lib nasa_handbook_model "mfnasa.gv";;
   dot_gen_show_fault_file nasa_handbook_lib nasa_handbook_model "mftnasa.gv";;

   let nasa_ftree = model_to_ftree nasa_handbook_lib nasa_handbook_model ;;
   dot_gen_show_direct_tree_file "tdnasa.gv" nasa_ftree ;;
   dot_gen_show_tree_file "tnasa.gv" nasa_ftree ;;

   let nasa_cutsets = cutsets nasa_ftree;;
   dot_gen_show_formula_file "csnasa.gv" nasa_cutsets ;;

   let nasa_probErrorCut = probErrorCut nasa_ftree ;;
   let nasa_probErrorCutImp = probErrorCutImp nasa_ftree ;;
   ]}
*)

open Core ;; 
open PrintBox ;;
open FaultTree ;;
open AttackDefenseTree ;;
open Qualitative ;;
open Quantitative ;;
open Modeling ;;
open TreeSynthesis;;

(**
   unflatten_exe and dot_exe are strings that should be set to the 
   fullpaths of the "unflatten" and "dot" utilities from graphviz. 
*)
let whereIs_dot = 
   let stringOption = In_channel.input_line( Unix.open_process_in "which dot" ) in
   match stringOption with
   | Some x -> x
   | None -> raise( Error "visualization error: graphviz is not installed" );;

let whereIs_unflatten = 
   let stringOption = In_channel.input_line( Unix.open_process_in "which unflatten" ) in
   match stringOption with
   | Some x -> x
   | None -> raise( Error "visualization error: graphviz is not installed" );;

let unflatten_exe = whereIs_unflatten ;;
let dot_exe = whereIs_dot ;;

let exp_string_of_float (likelihood:float) = Printf.sprintf "%.0e" likelihood;;

(* Below is code for generating a fault tree diagram using graphviz. Actually it
   generates diagrams for formulas, so you have to convert a fault tree to a
   formula first, but that is easy to do. See below.
*)

(**/**)
let add_edges c l e =
  List.append
    (List.map l ~f:(fun x -> (x, c)) )
    e;;

(* Use tree_dot f, where f is a formula to generate an internal representation
   for the input graphviz needs to create a diagram.
*)

let rec tree_dot ?(acc = (0, [], [])) f =
  let (cnt, nodes, edges) = acc in
  match f with
    | TRUE ->
      (cnt+1, (cnt, "TRUE")::nodes, edges)
    | FALSE ->
      (cnt+1, (cnt, "FALSE")::nodes, edges)
    | Var (x, y) ->
      (cnt+1, (cnt, (String.concat ~sep:": " [x; y]))::nodes, edges)
    | Sum x ->
      let (l,(cnt2, nodes2, edges2)) = tree_dot_l ~acc x in
      let c = cnt2+1 in
      let n = (cnt2, "OR")::nodes2 in
      (c,n, add_edges cnt2 l edges2)
    | Pro x ->
      let (l,(cnt2, nodes2, edges2)) = tree_dot_l ~acc x in
      let c = cnt2+1 in
      let n = (cnt2, "AND")::nodes2 in
      (c,n, add_edges cnt2 l edges2)
	
and tree_dot_l ?(acc = (0, [], [])) ?(l=[]) z=
  match z with
    | [] -> (l, acc)
    | x::xs ->
      let acc2 = tree_dot ~acc x in
      let (cnt, _, _) = acc2 in
      tree_dot_l ~acc:(acc2) ~l:(cnt-1::l) xs 
;;

(* Use adtree_dot f, where f is a AD formula to generate an internal representation
   for the input graphviz needs to create a diagram.
*)

let rec adtree_dot ?(acc = (0, [], [])) f =
  let (cnt, nodes, edges) = acc in
  match f with
    | ATRUE ->
      (cnt+1, (cnt, "TRUE")::nodes, edges)
    | AFALSE ->
      (cnt+1, (cnt, "FALSE")::nodes, edges)
    | AVar (x, y, _) ->
      (cnt+1, (cnt, (String.concat ~sep:": " [x; y]))::nodes, edges)
    | ASum x ->
      let (l,(cnt2, nodes2, edges2)) = adtree_dot_l ~acc x in
      let c = cnt2+1 in
      let n = (cnt2, "OR")::nodes2 in
      (c,n, add_edges cnt2 l edges2)
    | APro x ->
      let (l,(cnt2, nodes2, edges2)) = adtree_dot_l ~acc x in
      let c = cnt2+1 in
      let n = (cnt2, "AND")::nodes2 in
      (c,n, add_edges cnt2 l edges2)
    | ANot( x ) ->
      let (l,(cnt2, nodes2, edges2)) = adtree_dot_l ~acc [x] in
      let c = cnt2+1 in
      let n = (cnt2, "NOT")::nodes2 in
      (c,n, add_edges cnt2 l edges2)
    | DSum x ->
      let (l,(cnt2, nodes2, edges2)) = adtree_dot_l ~acc x in
      let c = cnt2+1 in
      let n = (cnt2, "d-OR")::nodes2 in
      (c,n, add_edges cnt2 l edges2)
    | DPro x ->
      let (l,(cnt2, nodes2, edges2)) = adtree_dot_l ~acc x in
      let c = cnt2+1 in
      let n = (cnt2, "d-AND")::nodes2 in
      (c,n, add_edges cnt2 l edges2)
	
and adtree_dot_l ?(acc = (0, [], [])) ?(l=[]) z=
  match z with
    | [] -> (l, acc)
    | x::xs ->
      let acc2 = adtree_dot ~acc x in
      let (cnt, _, _) = acc2 in
      adtree_dot_l ~acc:(acc2) ~l:(cnt-1::l) xs 
;;


(* There are two versions of dot_gen_chan below. 
   
   The second version is one that I played around for about a day to get to
   work. I had to play around with generating images for the and/ or gates and I
   played around with cropping, adding drop boxes, trying to get the right size,
   figuring out what parameters to use for graphviz since it was generating
   boxes around the gates and there were lots of ways to do it. It seemed that
   using a custom shape was the way to go and after a lot of experimenting and
   searching, I settled on the approach below. I played around with .svg, .eps,
   etc. files and trying to get the bounding box for the images defined, ....

   The first version uses natively supported shapes for ands and ors.

   The function takes a channel, chan, and the output of tree_dot, as the second
   argument, acc. It generates the graphviz output needed for graphviz to create
   the diagram.
*)
(*
let dot_gen_chan
    ?(splines="true") ?(layout="dot") ?(overlap="false") ?(chan=stdout) ~acc =
  let (_, nodes, edges) = acc in
  Printf.fprintf chan "digraph G {\n" ;
  Printf.fprintf chan "rankdir = BT \n" ;
  Printf.fprintf chan "layout = %s\n" layout;
  Printf.fprintf chan "splines = %s\n" splines; 
  Printf.fprintf chan "overlap = %s\n" overlap; 
  Printf.fprintf chan "sep =\"+10, +10\"\n";
  Printf.fprintf chan "esep =\"+8, +8\"\n";
  Printf.fprintf chan "outputorder=edgesfirst\n";
  List.iter nodes
    ~f:(fun x ->
      let (a, b) = x in
      let shape =
	if b="OR" then "egg"
	else "house" in
      if (b="OR" || b="AND")
      then (Printf.fprintf chan
	      "%d [label = \"%s\", shape = %s, style=\"rounded\"]\n"
	      a b shape;)
      else (Printf.fprintf chan
	      "%d [label = \"%s\", shape = box]\n"
	      a b;);)
    ;
  List.iter edges
    ~f:(fun x ->
      let (a, b) = x in
      Printf.fprintf chan "%d -> %d\n" a b;)
    ;
  Printf.fprintf chan "}\n" ;
;;
*)
(** Splines can be 
   "true", "spline": default dot setting, edges are splines (default)
   "false", "line": edges are straight line segments
   "curved": edges are curved arcs
   "polyline": edges are polylines
   "none", "": no edges drawn
   "ortho": do not use because it doesn't work with the kinds of nodes I have

   Layout can be 
   "dot"  (default)
   "fdp"
   "circo"
   "twopi"
   "neato"
   "sfdp"
   "patchwork"
   
   Rend (renderer) can be 
   "svg" (default)
   "png"
   "pdf"
   "eps"
   "ps"
   "fig"
   "gif"
   "jpeg"
   "tiff"
   "fv"
   ... (try "dot -Txxx" and the error message will tell you what renderers are supported)
   See the graphviz documentation

   overlap can be 
   "true" 
   "scale" (remove by scaling) 
   "false"
   "prism" 
   "scalexy" 
   "compress" 
   "vpsc" 
   "orthoxy" 
   "orthoyx"
   "voronoi" 

   flatten if true will call the graphviz flatten command which
   tries to create a good aspect ratio by adding invisible edges,
   etc.
*)
let dot_gen_chan
    ?(splines="true") ?(layout="dot") ?(overlap="false") ?(chan=stdout) ~acc =
  let (_, nodes, edges) = acc in
  Printf.fprintf chan "digraph G {\n" ;
  Printf.fprintf chan "rankdir=BT \n" ;
  Printf.fprintf chan "layout = %s\n" layout;
  Printf.fprintf chan "splines = %s\n" splines; 
  Printf.fprintf chan "overlap = %s\n" overlap; 
  Printf.fprintf chan "sep =\"+10, +10\"\n";
  Printf.fprintf chan "esep =\"+8, +8\"\n";
  Printf.fprintf chan "outputorder=edgesfirst\n";
  List.iter nodes
    ~f:(fun x ->
      let (a, b) = x in
      let (label,image) =
	if b="OR" then ("Or", "or.png")
	else if b="AND" then ("And", "and.png")
	else if b="d-AND" then ("d-And", "and_gray.png")
	else if b="d-OR" then ("d-Or", "or_gray.png")
	else ("\nNot", "not_gray.png") in
      if (b="OR" || b="AND" || b="NOT" || b="d-AND" || b="d-OR")
      then (Printf.fprintf chan
	      "%d [label =\"%s\", shape=none, margin=0, height=0, width=0, image=\"%s\"]\n"
	      a label image;)
      else (Printf.fprintf chan
	      "%d [label = \"%s\", shape = box]\n"
	      a b;);)
    ;
  List.iter edges
    ~f:(fun x ->
      let (a, b) = x in
      Printf.fprintf chan "%d -> %d\n" a b;)
    ;
  Printf.fprintf chan "}\n" ;
;;

(** This function generates the a dot file. *)
let dot_gen_file
    ?(splines="true") ?(layout="dot") ?(overlap="false") file acc =
  let chan = Out_channel.create file in
  dot_gen_chan ~splines ~layout ~overlap ~chan ~acc;
  Out_channel.flush chan ;;

(** Converting formulas that do not have an instance name associated with
    variables to ones that have a pair of strings for variable names.
*)
let rec convert f =
  match f with
    | Var x -> Var ("", x)
    | Sum x -> Sum (List.map x ~f:convert)
    | Pro x -> Pro (List.map x ~f:convert)
    | TRUE -> TRUE
    | FALSE -> FALSE ;;

(* figure out what OS *)
let whichOS =
  let stringOption = In_channel.input_line( Unix.open_process_in "uname" ) in
  match stringOption with
  | Some "Darwin" -> "Mac"
  | Some "Linux"  -> "Linux"
  | _             -> "Windows" ;;

let dot_core_gen_show_file ?(rend="svg") ?(splines="true") ?(layout="dot")
    ?(overlap="false") ?(unflatten=true) ?(show=false) file
    (f : ?splines:string -> ?layout:string -> ?overlap:string -> ?chan:Out_channel.t -> unit)
    =  
  let chan = Out_channel.create file in
  let fcom = String.concat ~sep:""
    [unflatten_exe; " -o "; file; "-u.gv "; file] in
  let com = String.concat ~sep:""
    [dot_exe; " -T"; rend; " "; file; "-u.gv"; " -o "; file; "." ;rend] in
  let ocom =
    if whichOS = "Mac" then (String.concat ~sep:"" ["open "; file; "."; rend]) (* <-- use "open" for mac *)
    else (String.concat ~sep:"" ["xdg-open "; file; "."; rend]) (* <-- use "xdg-open" for linux *) 
  in 
  f ~splines ~layout ~overlap ~chan ;
  Out_channel.flush chan;
  if unflatten then ignore (Sys.command fcom);
  ignore (Sys.command com);
  if show then ignore (Sys.command ocom) ;;

(** Code to construct the graphviz file and display it.  For this to work, you
    need to have graphviz installed and you have an .svg viewer (any browser
    should work). 
*)

(**/**)
(** A function for directly visualizing fault trees. *)
let dot_gen_show_direct_tree_file ?(rend="svg") ?(splines="true") ?(layout="dot")
    ?(overlap="false") ?(unflatten=true) ?(show=false) file tree =
  dot_core_gen_show_file ~rend ~splines ~layout ~overlap ~unflatten ~show file
    (dot_gen_chan ~acc:(tree_dot (formulaOfTree tree))) ;;

(** A function for directly visualizing attack-defense trees. *)
let dot_gen_show_direct_adtree_file ?(rend="svg") ?(splines="true") ?(layout="dot")
    ?(overlap="false") ?(unflatten=true) ?(show=false) file tree =
  dot_core_gen_show_file ~rend ~splines ~layout ~overlap ~unflatten ~show file
    (dot_gen_chan ~acc:(adtree_dot (formulaOfADTree tree))) ;;

(** A function for simplifying and visualizing fault trees. *)
let dot_gen_show_tree_file ?(rend="svg") ?(splines="true") ?(layout="dot")
    ?(overlap="false") ?(unflatten=true) ?(show=false) file tree =
  dot_core_gen_show_file ~rend ~splines ~layout ~overlap ~unflatten ~show file
    (dot_gen_chan ~acc:(tree_dot (nnsimp (formulaOfTree tree)))) ;;

(** A function for simplifying and visualizing attack-defense trees. *)
let dot_gen_show_adtree_file ?(rend="svg") ?(splines="true") ?(layout="dot")
    ?(overlap="false") ?(unflatten=true) ?(show=false) file tree =
  dot_core_gen_show_file ~rend ~splines ~layout ~overlap ~unflatten ~show file
    (dot_gen_chan ~acc:(adtree_dot (nnsimp_ad (formulaOfADTree tree)))) ;;

(* A function for simplifying, with ssfc, and visualizing fault trees. *)
let dot_gen_show_ssfc_tree_file ?(rend="svg") ?(splines="true") ?(layout="dot")
    ?(overlap="false") ?(unflatten=true) ?(show=false) file tree =
  dot_core_gen_show_file ~rend ~splines ~layout ~overlap ~unflatten ~show file
    (dot_gen_chan ~acc:(tree_dot (ssfc (formulaOfTree tree)))) ;;

(* A function for simplifying, with nndsimp, and visualizing fault trees. *)
let dot_gen_show_nndsimp_tree_file ?(rend="svg") ?(splines="true") ?(layout="dot")
    ?(overlap="false") ?(unflatten=true) ?(show=false) file tree =
  dot_core_gen_show_file ~rend ~splines ~layout ~overlap ~unflatten ~show file
    (dot_gen_chan ~acc:(tree_dot (nndsimp (formulaOfTree tree)))) ;;

(** A function for visualizing monotone Boolean formulas. *)
let dot_gen_show_formula_file ?(rend="svg") ?(splines="true") ?(layout="dot")
    ?(overlap="false") ?(unflatten=true) ?(show=false) file formula =
  dot_core_gen_show_file ~rend ~splines ~layout ~overlap ~unflatten ~show file
    (dot_gen_chan ~acc:(tree_dot formula)) ;;

(** This section of the code generates a physical architecture.  By this I mean
    we get one node per instance of a component in a model. There is an edge
    between i1 and i2 if there is any connection between an output of i1 and an
    input of i2.
*)

(**/**)
(** *)
let rec model_phy_nodes ?(cnt=0) inst =
  match inst with
    | [] -> []
    | hd::tl -> (cnt, hd)::(model_phy_nodes ~cnt:(cnt+1) tl) ;;

let lookup_inst name tag_is =
  List.find_exn tag_is ~f:(fun (_, inst) -> inst.i_name = name)  ;;

let model_phy_edges cs tag_is =
  List.map cs
    ~f:(fun ((target, _), (source, _)) ->
      let (t_i, _) = lookup_inst target tag_is in
      let (s_i, _) = lookup_inst source tag_is in
      (s_i, t_i))
     ;;

let model_to_physical_arch model =
  let is = model.instances in
  let cs = model.connections in
  let tag_is = model_phy_nodes is in 
  let nodes = List.map tag_is
    ~f:(fun (tag, i) -> (tag, i.i_name)) in
  let edges = model_phy_edges cs tag_is in 
  let redges = removeDups edges in
  (nodes, redges) ;;

let dot_gen_phy_arch_chan
    ?(splines="true") ?(layout="dot") ?(overlap="false")
    ?(chan=stdout) ~acc =
  let (nodes, edges) = acc in
  Printf.fprintf chan "digraph G {\n" ;
  Printf.fprintf chan "rankdir=BT \n" ;
  Printf.fprintf chan "layout = %s\n" layout;
  Printf.fprintf chan "splines = %s\n" splines; 
  Printf.fprintf chan "overlap = %s\n" overlap; 
  Printf.fprintf chan "sep =\"+10, +10\"\n";
  Printf.fprintf chan "esep =\"+8, +8\"\n";
  Printf.fprintf chan "outputorder=edgesfirst\n";
  List.iter nodes
    ~f:(fun (i, l) ->
      (Printf.fprintf chan
	 "%d [label = \"%s\", shape = box]\n"
	 i l;);)
    ;
  List.iter edges
    ~f:(fun x ->
      let (a, b) = x in
      Printf.fprintf chan "%d -> %d\n" a b;)
    ;
  Printf.fprintf chan "}\n" ;
;;

(**/**)
(** This function generates and displays a visualization of the physical
    architecture. By this I mean we get one node per instance of a component in
    a model.  If there are k outputs from component A that are connected to k
    inputs from component B, then only one edge is shown between the
    components. This gives us the highest-level view of the system, as is shows
    an abstract view showing only components and connectivity information
    (yes/no).
*)
let dot_gen_show_ph_file 
    ?(rend="svg") ?(splines="true") ?(layout="dot") ?(overlap="false")
    ?(unflatten=true) ?(show=false) model file  =
  dot_core_gen_show_file ~rend ~splines ~layout ~overlap ~unflatten ~show file
    (dot_gen_phy_arch_chan ~acc:(model_to_physical_arch model)) ;;

(** This part of the code generates graphs for the "functional" or "full
    physical" architecture. There is no ideal name because what is shown really
    depends on how one models things. For some models, what is generated may be
    a "full physical" architecture since we show input/output ports and
    therefore one sees more details that are in the physical architecture. For
    other models what se show will really be the "functional" architecture,
    e.g., there may be one physical connection between two instances, but they
    may correspond to six flows and if the model reflects that, then the
    input/output ports are really functional, not physical.

    The idea here is to show nodes as structures where the inputs and outputs
    are identified and connections are between such input/ output ports, not
    just the nodes..

    Here is the structure of the nodes and edges in the internal representation
    we are using.

    (nodes, edges)
    nodes: [node1, ...]
    edges: [edge1, ...]

    node: (inst, 0, [(1, "in1"), ...], [(221, "out1"), ...]
    edge: ((0, 1), (10, 22))
*)

(**/**)
(** *)
let rec tag_cnt ?(acc=[]) cnt l =
  match l with
    | [] -> (cnt, List.rev acc)
    | x::xs -> tag_cnt ~acc:((cnt, x)::acc) (cnt + 1) xs ;;

let gen_funct_node cnt inst lib =
  let c = get_c_name lib inst.c_name in
  let inputs = c.input_flows in
  let outputs = c.output_flows in
  let (cnt1, node_inputs) = tag_cnt cnt inputs in
  let (cnt2, node_outputs) = tag_cnt cnt1 outputs in
  (cnt2+1, (cnt2, node_inputs, node_outputs)) ;;
(**/**)

let rec gen_funct_nodes ?(acc = (0, [])) lib insts =
  let (cnt, nodes) = acc in 
  match insts with
    | [] -> acc
    | i::is ->
      let (c, (id, ins, outs)) = gen_funct_node cnt i lib in
      gen_funct_nodes ~acc:(c, (i, id, ins, outs)::nodes)
	lib is ;;

(**/**)
let rec findl f l =
  match l with
    | [] -> raise Not_found
    | e::es ->
      try List.find_exn e ~f:f
      with Not_found -> findl f es ;;

let get_funct_id i_name port nodes =
  let (_, id, inputs, outputs) =
    List.find_exn nodes
      ~f:(fun x -> let (inst, _, _, _) = x in
		inst.i_name = i_name)
       in
  let (a,_) =
    findl (fun (_, name2) -> name2=port) [inputs; outputs] in
  (id, a) ;;

let gen_funct_edge nodes connection =
  let ((target, tin), (source, sout)) = connection in 
  let source_id = get_funct_id source sout nodes in
  let target_id = get_funct_id target tin nodes in
  (source_id, target_id) ;;
    
let gen_funct_edges nodes connections =
  List.map connections
    ~f:(fun x -> gen_funct_edge nodes x)
     ;;

let dot_gen_funct_node node chan =
  let (inst, id, inputs, outputs) = node in
  let inputs_len = List.length inputs in
  let outputs_len = List.length outputs in
  let len = max inputs_len outputs_len in
  (Printf.fprintf chan
     "\n%d [label=< \n  <TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\">\n"
     id );
  if outputs_len > 0
  then
    begin
      (Printf.fprintf chan
	 "  <TR><TD border=\"0\">   </TD>\n");
      List.iter outputs ~f:(fun (o_c, o_port) ->
	(Printf.fprintf chan
	   "    <TD PORT=\"%d\"> %s </TD>\n    <TD border=\"0\">   </TD>\n"
	   o_c o_port))
	;
      (Printf.fprintf chan "  </TR>\n");
    end
  else  () ;
  (Printf.fprintf chan
     "  <TR> <TD ROWSPAN=\"3\" COLSPAN=\"%d\"> <BR/>%s<BR/> </TD> </TR>\n"
     (2*len+1) inst.i_name) ;
  (Printf.fprintf chan "  <TR> <TD border=\"0\"> </TD> </TR>\n");
  (Printf.fprintf chan "  <TR> <TD border=\"0\"> </TD> </TR>\n");
  if inputs_len > 0
  then
    begin
      (Printf.fprintf chan "  <TR><TD border=\"0\">   </TD>\n");
      List.iter inputs ~f:(fun (i_c, i_port) ->
	(Printf.fprintf chan
	   "    <TD PORT=\"%d\"> %s </TD>\n  <TD border=\"0\">   </TD>\n"
	   i_c i_port))
	;
      (Printf.fprintf chan "  </TR>\n");
    end
  else  () ;
  (Printf.fprintf chan "  </TABLE>>];\n");
;;

let dot_gen_funct_nodes nodes chan =
  List.iter nodes
    ~f:(fun n -> dot_gen_funct_node n chan)
    ;;

let dot_gen_funct_edges edges chan =
  List.iter edges
    ~f:(fun ((a,b), (c,d)) ->
      Printf.fprintf chan
	"%d:%d -> %d:%d ;\n" a b c d)
     ;;

let full_funct_dot lib model chan =
  let (_, nodes) = gen_funct_nodes lib model.instances in
  let edges = gen_funct_edges nodes model.connections in 
  dot_gen_funct_nodes nodes chan; 
  Printf.fprintf chan "\n";
  dot_gen_funct_edges edges chan ;;

let dot_gen_funct_arch_chan_helper splines layout overlap chan =
  Printf.fprintf chan "digraph G {\n" ;
  Printf.fprintf chan "rankdir=BT \n" ;
  Printf.fprintf chan "layout = %s\n" layout;
  Printf.fprintf chan "splines = %s\n" splines; 
  Printf.fprintf chan "overlap = %s\n" overlap; 
  Printf.fprintf chan "sep =\"+10, +10\"\n";
  Printf.fprintf chan "esep =\"+8, +8\"\n";
  Printf.fprintf chan "outputorder=edgesfirst\n";
  Printf.fprintf chan "node [shape=plain]\n" ;
;;

let dot_gen_funct_arch_chan
    ?(splines="true")  ?(layout="dot") ?(overlap="false") ?(chan=stdout) ~lib ~model  =
  dot_gen_funct_arch_chan_helper
    splines layout overlap chan ;
  full_funct_dot lib model chan;
  Printf.fprintf chan "}\n" ;
;;

(**/**)
(** This function generates and displays a visualization of the functional
    architecture. This is an elaboration of the physical architecture that in
    addition shows input port and output ports. Now edges are from port to port,
    so we get a mode detailed view of the model. For example, in this view of a
    model, if there are k outputs from component A that are connected to k
    inputs from component B, then k edges are shown between the components as
    there is an edge from one port to another if the first port's output is the
    input to the second port. *)
let dot_gen_show_funct_file 
    ?(rend="svg") ?(splines="true") ?(layout="dot") ?(overlap="false")
    ?(unflatten=true) ?(show=false) lib model file = 
  dot_core_gen_show_file ~rend ~splines ~layout ~overlap ~unflatten ~show file
    (dot_gen_funct_arch_chan ~lib ~model) ;;


(** This part of the code generates a diagram corresponding to the propagation
    of a particular fault.

    After a lot of experimentation, what I decided is that we need to start with
    the functional architecture, overlayed with (or annotated with) the basic
    events (since they are relevant to faults). Then the idea is to add edges in
    a different color (red) that correspond to the fault propagation described
    in the model. In this way, one can visually see what events and flows
    contribute to a fault (but not how since formulas are not displayed). For
    that, use the visualization of fault trees generated.

    The visualization is for a particular fault, as determined by the model.
*)

(**/**)
(** *)
let gen_fault_node cnt inst lib =
  let c = get_c_name lib inst.c_name in
  let inputs = c.input_flows in
  let events = c.basic_events in 
  let outputs = c.output_flows in
  let (cnt1, node_inputs) = tag_cnt cnt inputs in
  let (cnt2, node_events) = tag_cnt cnt1 events in
  let (cnt3, node_outputs) = tag_cnt cnt2 outputs in
  (cnt3+1, (cnt3, node_inputs, node_events, node_outputs)) ;;

let rec gen_fault_nodes ?(acc = (0, [])) lib insts =
  let (cnt, nodes) = acc in 
  match insts with
    | [] -> acc
    | i::is ->
      let (c, (id, ins, evs, outs)) = gen_fault_node cnt i lib in
      gen_fault_nodes ~acc:(c, (i, id, ins, evs, outs)::nodes)
	lib is ;;

let get_fault_id i_name port nodes =
  let (_, id, inputs, events, outputs) =
    List.find_exn nodes
      ~f:(fun x -> let (inst, _, _, _, _) = x in
		inst.i_name = i_name)
      in
  let (a,_) =
    findl (fun (_, name2) -> name2=port) [inputs; events; outputs] in
  (id, a) ;;

let gen_fault_edge nodes connection =
  let ((target, tin), (source, sout)) = connection in 
  let source_id = get_fault_id source sout nodes in
  let target_id = get_fault_id target tin nodes in
  (source_id, target_id) ;;
    
let gen_fault_edges nodes connections =
  List.map connections
    ~f:(fun x -> gen_fault_edge nodes x)
     ;;

let dot_gen_fault_node node chan =
  let (inst, id, inputs, events, outputs) = node in
  let inputs_len = List.length inputs in
  let events_len = List.length events in
  let outputs_len = List.length outputs in
  let len = max inputs_len outputs_len in
  let lene = match events_len with
    | 0 -> 3
    | n -> (n+1)*2 in
  (Printf.fprintf chan
     "\n%d [label=< \n  <TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\">\n"
     id );
  if outputs_len > 0
  then
    begin
      (Printf.fprintf chan
	 "  <TR><TD border=\"0\">   </TD>\n");
      List.iter outputs ~f:(fun (o_c, o_port) ->
	(Printf.fprintf chan
	   "    <TD PORT=\"%d\"> %s </TD>\n    <TD border=\"0\">   </TD>\n"
	   o_c o_port))
	;
      (Printf.fprintf chan "  </TR>\n");
    end
  else  () ;
  (Printf.fprintf chan
     "  <TR> <TD ROWSPAN=\"%d\" COLSPAN=\"%d\"> <BR/>%s<BR/> </TD> </TR>\n"
     lene (2*len+1) inst.i_name) ;
  (Printf.fprintf chan "  <TR> <TD border=\"0\"> </TD> </TR>\n");
  if events_len > 0
  then
    List.iter events ~f:(fun (e_c, e_port) ->
      (Printf.fprintf chan
	 "  <TR><TD PORT=\"%d\"> %s </TD></TR>\n  <TR> <TD border=\"0\"> </TD></TR>\n" e_c e_port))
  else
    (Printf.fprintf chan
       "  <TR> <TD border=\"0\"> </TD></TR>\n  ");
  if inputs_len > 0
  then
    begin
      (Printf.fprintf chan "  <TR><TD border=\"0\">   </TD>\n");
      List.iter inputs ~f:(fun (i_c, i_port) ->
	(Printf.fprintf chan
	   "    <TD PORT=\"%d\"> %s </TD>\n  <TD border=\"0\">   </TD>\n"
	   i_c i_port))
	;
      (Printf.fprintf chan "  </TR>\n");
    end
  else  () ;
  (Printf.fprintf chan "  </TABLE>>];\n");
;;

let dot_gen_fault_nodes nodes chan =
  List.iter nodes
    ~f:(fun n -> dot_gen_fault_node n chan)
    ;;

let dot_gen_fault_edges edges chan =
  List.iter edges
    ~f:(fun ((a,b), (c,d)) ->
      Printf.fprintf chan
	"%d:%d -> %d:%d ;\n" a b c d)
     ;;

let fault_dot ?(es=[]) lib model chan =
  let (_, nodes) = gen_fault_nodes lib model.instances in
  let gedges = gen_fault_edges nodes model.connections in
  let edges = List.filter gedges ~f:(fun x -> (not (List.mem es x ~equal:(=))))  in 
  dot_gen_fault_nodes nodes chan; 
  Printf.fprintf chan "\n";
  dot_gen_fault_edges edges chan ;;

let get_atoms_formula f = 
  let rec get_atoms_formula_ f =
    (match f with
      | F(x) -> [x] 
      | And(l) ->
	let nf = List.map l ~f:get_atoms_formula_ in
	List.concat nf
      | Or(l) ->
	let nf = List.map l ~f:get_atoms_formula_ in
	List.concat nf
      | N_of(_, l) ->
	let nf = List.map l ~f:get_atoms_formula_ in
	List.concat nf)  in
  removeDups (get_atoms_formula_ f) ;;

let get_node_pair n nodes =
  let (i_name, out) = n in
  let (_, id, ins, evs, outs)
      = List.find_exn nodes ~f:(fun (a,_,_,_,_) -> a.i_name = i_name) in
  let (port_id,_) = findl (fun (_,b) -> b=out ) [ins; evs; outs] in
  (id, port_id) ;;

let get_conn_pair x inst connections nodes =
  let (_,(s,p)) =
    List.find_exn connections ~f:(fun ((a,b), (_,_)) ->
      (inst.i_name, x) = (a,b)) in
  ((s,p), get_node_pair (s,p) nodes) ;;

let gen_fault_f_edges library instances connections nodes f ff  =
  let (i_name, F [out; _]) = f in
  let (fa, fb) = get_node_pair (i_name, out) nodes in 
  let inst = get_i_name instances i_name in
  let cn = inst.c_name in
  let c = get_c_name library cn in
  let es =
    List.map ff 
      ~f:(fun x ->
	let xh = List.hd_exn x in
	let (a,b) = get_node_pair (i_name, xh) nodes in
	let (l1,l2) =
	  if (List.mem c.input_flows xh ~equal:(=))
 	  then
	    let ((s,p), (a1,b1)) =
	      get_conn_pair xh inst connections nodes in
	    ([((a1,b1), (a,b))], [(s,F [p; List.nth_exn x 1])])
       	  else ([],[]) in 
	(l1@[((a,b), (fa, fb))], l2))
      in
  let (es1, es2) = List.unzip es in 
  (removeDups (List.concat es1),
   removeDups (List.concat es2)) ;;

(** This should generate the new flows. Elements in ff that are events get
    ignored, but elements that are input ports require that we look up to see who
    is connected to them to generate a new flow. *)
let rec cons_fault_edges library instances connections nodes his fl res =
  match fl with
    | [] -> List.rev res
    | f::fs ->
      let (i_name, form) = f in
      let inst = get_i_name instances i_name in
      let cn = inst.c_name in
      let c = get_c_name library cn in
      let fms = c.fault_formulas in
      let ffx = get_atoms_formula form in
      let fm = List.map ffx ~f:(fun x -> find_form fms x c) in
      let fmx = removeDups (List.concat (List.map fm ~f:get_atoms_formula)) in
      let (edges, nextfaults) = gen_fault_f_edges
	library instances connections nodes f fmx in
      let nedges = List.filter edges ~f:(fun x -> not (List.mem res x ~equal:(=))) in
      let nfaults = List.filter nextfaults
	~f:(fun x -> not (List.mem his x ~equal:(=))) in
      cons_fault_edges
	library instances connections nodes
	(List.append nfaults his)
	(List.append fs nfaults)
	(List.append nedges res);;

let gen_fault_edges library model nodes =
  let i = model.instances in
  let c = model.connections in
  cons_fault_edges library i c nodes [model.top_fault] [model.top_fault] [] ;;

let dot_gen_fault_edges ?(chan=stdout) edges =
  List.iter edges
    ~f:(fun ((a,b), (c,d)) ->
      Printf.fprintf chan
	"%d:%d -> %d:%d [color=\"red\"];\n" a b c d)
     ;;

let dot_gen_fault_arch_chan
    ?(splines="true") ?(layout="dot") ?(overlap="false") ?(chan=stdout)
    ~lib ~model = 
  let (_, fault_nodes) =
    gen_fault_nodes lib model.instances in
  let edges = gen_fault_edges lib model fault_nodes in 
  Printf.fprintf chan "digraph G {\n" ;
  Printf.fprintf chan "rankdir=BT \n" ;
  Printf.fprintf chan "layout = %s\n" layout;
  Printf.fprintf chan "splines = %s\n" splines; 
  Printf.fprintf chan "overlap = %s\n" overlap; 
  Printf.fprintf chan "sep =\"+10, +10\"\n";
  Printf.fprintf chan "esep =\"+8, +8\"\n";
  Printf.fprintf chan "outputorder=edgesfirst\n";
  Printf.fprintf chan "node [shape=plain]\n" ;
  fault_dot ~es:(edges) lib model chan;
  dot_gen_fault_edges ~chan edges; 
  Printf.fprintf chan "}\n" ;
;;

(**/**)
(** This function generates and displays a visualization of the propagation of
    faults through the architecture.  This is an elaboration of the functional
    model that in addition shows basic events associated with components and
    shows what parts of the model can affect the probability of a the top-level
    fault. If an edge (corresponding to a flow of information from one component
    to another) can affect the top-level fault, it is colored red. If a basic
    event can affect the top-level fault, then a (set of) red edge(s) is added
    from that event to the output(s) along which the basic event propogates.*)
let dot_gen_show_fault_file
    ?(rend="svg") ?(splines="true") ?(layout="dot") ?(overlap="false")
    ?(unflatten=true) ?(show=false) lib model file =
  dot_core_gen_show_file ~rend ~splines ~layout ~overlap ~unflatten ~show file
    (dot_gen_fault_arch_chan ~lib ~model) ;;

(**/**)

(* This function prints each cutset directly to a file 
   using Out_channel.output_string and Out_channel. output_char *)
let rec print_each_cs adexp ch  =
   match adexp with
   | AVar( comp, event, _) -> Out_channel.output_string ch (comp ^ ":" ^ event); 
   | ASum l -> 
      (match l with
      | hd::tl -> print_each_cs hd ch; Out_channel.output_char ch '\n'; print_each_cs (ASum tl) ch ;
      | [] -> Out_channel.output_char ch '\n'; )
   | APro l ->
      (match l with
      | hd::tl -> print_each_cs hd ch; Out_channel.output_char ch '\t'; print_each_cs (APro tl) ch ;
      | [] -> Out_channel.output_char ch '\t'; )
   | DSum l -> 
      (match l with
      | hd::tl -> print_each_cs hd ch; Out_channel.output_string ch (" /"^"\\ "); print_each_cs (DSum tl) ch ;
      | [] -> Out_channel.output_char ch '\t'; )
   | DPro l ->
      (match l with
      | hd::tl -> print_each_cs hd ch; Out_channel.output_string ch " \\/ "; print_each_cs (DPro tl) ch ;
      | [] -> Out_channel.output_char ch '\t'; )
   | ANot form -> Out_channel.output_string ch "Not("; print_each_cs form ch; Out_channel.output_string ch ")";
   | _ -> Out_channel.output_char ch '\n' ;; 

let rec print_each_csImp l ch =
   match l with
   | hd::tl -> 
      (let (adexp, likelihood, _) = hd in 
       Out_channel.output_string ch ((exp_string_of_float likelihood) ^ "\t\t");
       print_each_cs adexp ch ; 
       Out_channel.output_char ch '\n';
       print_each_csImp tl ch );
   | [] -> Out_channel.output_char ch '\n';; 


(* This function generates a string representation of the defense profiles *)
let rec sprint_defenseProfile adexp wDALSuffix =
   let andStr = " ^ \n" 
   and orStr = " v " in
   match adexp with
   | AVar(comp, event, dal) -> comp ^ ":" ^ event ^ (if wDALSuffix && dal <> "" then ":" ^ dal else "")
   | DPro l -> 
     (match l with 
     | hd::tl -> 
       if List.length tl = 0 then sprint_defenseProfile hd wDALSuffix
       else sprint_defenseProfile hd wDALSuffix ^ andStr ^ sprint_defenseProfile (DPro tl) wDALSuffix
     | []  -> "")
   | DSum l -> 
     (match l with 
     | hd::tl -> 
       if List.length tl = 0 then sprint_defenseProfile hd wDALSuffix
       else sprint_defenseProfile hd wDALSuffix ^ orStr ^ sprint_defenseProfile (DSum tl) wDALSuffix
     | []  -> "")
   | ANot form -> "Not(" ^ sprint_defenseProfile form wDALSuffix ^ ")"
   | _ -> "" ;; 


(* This function generates 2 strings: attackStr, defenseStr *)
let rec sprint_AProAVar2 adexp str wDALSuffix =
   let andStr = " ^ \n" 
   and orStr = " v " 
   and (attackStr, defenseStr) = str in
   match adexp with
   | AVar (comp, event, _) -> (comp ^ ":" ^ event, defenseStr)
   | APro l -> 
     (match l with
        | hd::tl -> 
           (match hd with
              | AVar(comp, event, dal) -> 
                    let suffix = if wDALSuffix && dal <> "" then ":" ^ dal else "" in
                    let aStr = (if attackStr = "" then (comp ^ ":" ^ event ^ suffix) else (attackStr ^ andStr ^ comp ^ ":" ^ event ^ suffix)) in
                    sprint_AProAVar2 (APro tl) (aStr, defenseStr ) wDALSuffix
              | DSum l -> 
                    let dStr = (if defenseStr = "" then (sprint_defenseProfile (DSum l) wDALSuffix) else (defenseStr ^ andStr ^ (sprint_defenseProfile (DSum l) wDALSuffix))) in
                    sprint_AProAVar2 (APro tl) (attackStr, dStr ) wDALSuffix
              | DPro l -> 
                    let dStr = (if defenseStr = "" then (sprint_defenseProfile (DPro l) wDALSuffix) else (defenseStr ^ andStr ^ (sprint_defenseProfile (DPro l) wDALSuffix))) in
                    sprint_AProAVar2 (APro tl) (attackStr, dStr ) wDALSuffix
              | ANot x -> 
                    let dStr = (if defenseStr = "" then (sprint_defenseProfile (ANot x) wDALSuffix) else (defenseStr ^ andStr ^ (sprint_defenseProfile (ANot x) wDALSuffix))) in
                    sprint_AProAVar2 (APro tl) (attackStr, dStr ) wDALSuffix
              | _ -> str)
        | [] -> (attackStr, defenseStr))
   | _ -> str;;

(* Given the list of cutset from likelihoodCutImp, generates an output to use with PrintBox *)
let rec sprint_each_csImp l_csImp csArray wDALSuffix =
   match l_csImp with
   | hd::tl -> 
      (let (adexp_APro, likelihood, _) = hd in 
      let (attackStr, defenseStr) = sprint_AProAVar2 adexp_APro ("","") wDALSuffix in
      sprint_each_csImp tl (Array.append csArray [| [| (exp_string_of_float likelihood); attackStr; defenseStr |] |] ) wDALSuffix)
   | [] -> csArray ;; 

(* This function generates a string of failure events *)
let rec sprint_Events pexp =
   let andStr = " ^ \n" 
   and orStr = " v " in
   match pexp with
   | Var(comp, event) -> comp ^ ":" ^ event
   | Pro l ->
     (match l with 
     | hd::tl -> 
       (* have to see when len tl = 0, so that you don't print a trailing andStr *)
       if List.length tl = 0 then sprint_Events hd 
       else sprint_Events hd ^ andStr ^ sprint_Events (Pro tl)
     | []  -> "")
   | Sum l -> 
     (match l with 
     | hd::tl -> 
       (* have to see when len tl = 0, so that you don't print a trailing orStr *)
       if List.length tl = 0 then sprint_Events hd 
       else sprint_Events hd ^ orStr ^ sprint_Events (Sum tl)
     | []  -> "") 
   | TRUE  -> "true"
   | FALSE -> "false";;

(* Given the list of cutset from probErrorCutImp, generates an output to use with PrintBox *)
let rec sprint_each_safety_csImp l_csImp csArray =
   match l_csImp with
   | hd::tl -> 
      (let (pexp, probability, _) = hd in 
      sprint_each_safety_csImp tl (Array.append csArray [| [| (exp_string_of_float probability); (sprint_Events pexp)|] |] ) )
   | [] -> csArray ;; 
     
(**/**)

(** This function generates a report with the top-level likelihood, the likelihood of 
    each cutset, and the cutset list. The cutset list is printed in a frame. *)
let saveADCutSetsToFile ?cyberReqID:(cid="") ?risk:(r="") ?header:(h="") file adtree wDALSuffix =
   if (Sys.file_exists file = `Yes) then Sys.command_exn("rm " ^ file);
   (* if header file is specified, then copy it as the cutset file *)
   if (not(String.is_empty h) && (Sys.file_exists h = `Yes )) then Sys.command_exn("cp " ^ h ^ " " ^ file);
   let ch = Out_channel.create ~append:true ~perm:777 file in
   let csImp = likelihoodCutImp adtree 
   and likelihood = likelihoodCut adtree 
   and targetString = (if (String.is_empty r) then "\n" else ("Acceptable level of risk must be less than or equal to " ^ r))
   in
   let myArray = sprint_each_csImp csImp [| [|"Cutset\nlikelihood: "; "CAPEC: "; "Defense Profile: "|] |] wDALSuffix in
   let box = PrintBox.(hlist [ text ("Cyber\nReqID: \n" ^ cid); grid_text myArray ]) |> PrintBox.frame
   in
   (Out_channel.output_string ch ("\n");
    Out_channel.output_string ch ("Calculated likelihood of successful attack = " ^ (exp_string_of_float likelihood) ^ "\n");
    Out_channel.output_string ch (targetString);
    Out_channel.output_string ch ("\n");
    PrintBox_text.output ch box;
    Out_channel.close ch ;
    )
;;

(** This function generates a report with the top-level probability, the probability of 
    each cutset, and the cutset list. *)
let saveCutSetsToFile ?reqID:(cid="") ?risk:(r="") ?header:(h="") file ftree =
   if (Sys.file_exists file = `Yes) then Sys.command_exn("rm " ^ file);
   (* if header file is specified, then copy it as the cutset file *)
   if (not(String.is_empty h) && (Sys.file_exists h = `Yes )) then Sys.command_exn("cp " ^ h ^ " " ^ file);
   let ch = Out_channel.create ~append:true ~perm:777 file in
   let csImp = probErrorCutImp ftree 
   and (probability,_) = probErrorCut ftree 
   and targetString = (if (String.is_empty r) then "\n" else ("Acceptable level of risk must be less than or equal to " ^ r))
   in
   let myArray = sprint_each_safety_csImp csImp [| [|"Cutset\nprobability: "; "Cutset: "|] |] in
   let box = PrintBox.(hlist [ text ("Safety\nReqID: \n" ^ cid); grid_text myArray ]) |> PrintBox.frame
   in
   (Out_channel.output_string ch ("\n");
    Out_channel.output_string ch ("Calculated probability of failure = " ^ (exp_string_of_float probability) ^ "\n");
    Out_channel.output_string ch (targetString);
    Out_channel.output_string ch ("\n");
    PrintBox_text.output ch box;
    Out_channel.close ch ;
    )
;;