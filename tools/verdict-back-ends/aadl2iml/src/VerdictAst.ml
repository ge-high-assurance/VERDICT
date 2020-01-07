
type cia = CIA_C | CIA_I | CIA_A

type ia = IA_I | IA_A

type severity =
  | Severity_None
  | Severity_Minor
  | Severity_Major
  | Severity_Hazardous
  | Severity_Catastrophic

type port = string * cia

type iaport = string * ia

type lexpr =
  | LPort of port
  | LAnd of lexpr list
  | LOr of lexpr list
  | LNot of lexpr

type slexpr =
  | SLPort of iaport
  | SLFault of string
  | SLAnd of slexpr list
  | SLOr of slexpr list
  | SLNot of slexpr

(* dot-separated strings *)
type var = string list

type tintro =
  {
    var : string;
    var_type : string;
  }

(* threat expression *)
type texpr =
  | TEqual of var * var
  | TContains of var * var
  | TForall of tintro * texpr
  | TExists of tintro * texpr
  | TImplies of texpr * texpr
  | TOr of texpr list
  | TAnd of texpr list
  | TNot of texpr

type statement =
  | Mission of
      {
        id : string;
        description : string option;
        comment : string option;
        reqs : string list;
      }

  | SafetyReq of
      {
        id : string;
        condition: slexpr;
        target_probability : string;
        description : string option;
        comment : string option;      
      }

  | SafetyRel of
      {
        id : string;
        output: iaport;
        faultSrc: slexpr option;
        description : string option;
        comment : string option;      
      }

  | SafetyEvent of
      {
        id : string;
        probability: string;
        description : string option;
        comment : string option;
      }

  | CyberReq of
      {
        id : string;
        cia : cia option;
        severity : severity;
        condition : lexpr;
        comment : string option;
        description : string option;
        phases : string option;
        extern : string option;
      }

  | CyberRel of
      {
        id : string;
        output : port;
        inputs : lexpr option;
        comment : string option;
        description : string option;
        phases : string option;
        extern : string option;
      }

  | ThreatModel of
      {
        id : string;
        intro : tintro;
        expr : texpr;
        cia : cia;
        reference : string option;
        assumptions : string list;
        description : string option;
        comment : string option;
      }

  | ThreatDefense of
      {
        id : string;
        threats : string list;
        description : string option;
        comment : string option;
      }

  | ThreatDatabase of string

type t = statement list

(* Note: pretty-printing is not fully implemented
   Some fields are not pretty-printed at all. *)

let pp_print_option format ppf = function
  | Some x -> Format.fprintf ppf format x
  | None -> ()

let pp_print_option_a format pp_printer ppf = function
  | Some x -> Format.fprintf ppf format pp_printer x
  | None -> ()

let pp_print_cia ppf = function
  | CIA_C -> Format.fprintf ppf "C"
  | CIA_I -> Format.fprintf ppf "I"
  | CIA_A -> Format.fprintf ppf "A"

let pp_print_ia ppf = function
  | IA_I -> Format.fprintf ppf "I"
  | IA_A -> Format.fprintf ppf "A"

let pp_print_severity ppf = function
  | Severity_None -> Format.fprintf ppf "None"
  | Severity_Minor -> Format.fprintf ppf "Minor"
  | Severity_Major -> Format.fprintf ppf "Major"
  | Severity_Hazardous -> Format.fprintf ppf "Hazardous"
  | Severity_Catastrophic -> Format.fprintf ppf "Catastrophic"

let pp_print_port ppf (port, cia) =
  Format.fprintf ppf "%s:%a" port pp_print_cia cia

let pp_print_iaport ppf (port, ia) =
  Format.fprintf ppf "%s:%a" port pp_print_ia ia

let rec pp_print_list_join join pp_printer ppf = function
  | [] -> ()
  | hd :: [] ->
     Format.fprintf ppf "%a" pp_printer hd
  | hd :: ((_ :: _) as tl) ->
     Format.fprintf ppf "%a %s %a" pp_printer hd join
       (pp_print_list_join join pp_printer) tl

let pp_print_wrap_parens ppf pp_printer v =
  Format.fprintf ppf "(%a)" pp_printer v

let rec pp_print_lexpr ppf = function
  | LPort port -> pp_print_port ppf port
  | LAnd exprs ->
     pp_print_wrap_parens ppf
       (pp_print_list_join "and" pp_print_lexpr) exprs
  | LOr exprs ->
     pp_print_wrap_parens ppf
       (pp_print_list_join "or" pp_print_lexpr) exprs
  | LNot expr ->
     pp_print_wrap_parens ppf
       (fun p -> Format.fprintf p "not %a" pp_print_lexpr) expr

let pp_print_fault ppf id =
  Format.fprintf ppf "happens(%s)" id

let rec pp_print_slexpr ppf = function
  | SLPort port -> pp_print_iaport ppf port
  | SLFault id -> pp_print_fault ppf id
  | SLAnd exprs ->
     pp_print_wrap_parens ppf
       (pp_print_list_join "and" pp_print_slexpr) exprs
  | SLOr exprs ->
     pp_print_wrap_parens ppf
       (pp_print_list_join "or" pp_print_slexpr) exprs
  | SLNot expr ->
     pp_print_wrap_parens ppf
       (fun p -> Format.fprintf p "not %a" pp_print_slexpr) expr

let pp_print_safety_req_body ind ppf
      (id, condition, description, comment) =
  Format.fprintf ppf
    "{id=%s; condition=%a%a%a}" id
    pp_print_slexpr condition
    (pp_print_option "; comment=%s") comment
    (pp_print_option "; description=%s") description 

let pp_print_safety_rel_body ind ppf
      (id, output, faultSrc, description, comment) =
  Format.fprintf ppf
    "{id=%s; output=%a%a%a%a}"
    id pp_print_iaport output
    (pp_print_option_a "; faultSrc=%a" pp_print_slexpr) faultSrc
    (pp_print_option "; comment=%s") comment
    (pp_print_option "; description=%s") description 

let pp_print_safety_event_body ind ppf
      (id, probability, description, comment) =
  Format.fprintf ppf
    "{id=%s; probability=%s%a%a}" id probability
    (pp_print_option "; comment=%s") comment
    (pp_print_option "; description=%s") description

let pp_print_cyber_req_body ind ppf
      (id, cia, severity, condition, comment) =
  Format.fprintf ppf
    "{id=%s; severity=%a; condition=%a%a%a}" id
    pp_print_severity severity
    pp_print_lexpr condition
    (pp_print_option "; comment=%s") comment
    (pp_print_option_a "; cia=%a" pp_print_cia) cia

let pp_print_cyber_rel_body ind ppf
      (id, output, inputs, comment) =
  Format.fprintf ppf
    "{id=%s; output=%a%a%a}"
    id pp_print_port output
    (pp_print_option_a "; inputs=%a" pp_print_lexpr) inputs
    (pp_print_option "; comment=%s") comment

let pp_print_intro ppf {var; var_type} =
  Format.fprintf ppf "%s:%s" var var_type

let pp_print_str ppf str =
  Format.fprintf ppf "%s" str

let pp_print_var var =
  pp_print_list_join "." pp_print_str var

let rec pp_print_texpr ppf = function
  | TEqual (v1, v2) ->
     Format.fprintf ppf "%a=%a"
       pp_print_var v1 pp_print_var v2
  | TContains (v1, v2) ->
     Format.fprintf ppf "%a contains %a"
       pp_print_var v1 pp_print_var v2
  | TForall (intro, expr) ->
     Format.fprintf ppf "forall %a, %a"
       pp_print_intro intro pp_print_texpr expr
  | TExists (intro, expr) ->
     Format.fprintf ppf "exists %a, %a"
       pp_print_intro intro pp_print_texpr expr
  | TImplies (e1, e2) ->
     Format.fprintf ppf "%a => %a"
       pp_print_texpr e1 pp_print_texpr e2
  | TOr exprs ->
     pp_print_wrap_parens ppf
       (pp_print_list_join "or" pp_print_texpr) exprs
  | TAnd exprs ->
     pp_print_wrap_parens ppf
       (pp_print_list_join "and" pp_print_texpr) exprs
  | TNot expr ->
     pp_print_wrap_parens ppf
       (fun p -> Format.fprintf p "not %a" pp_print_texpr) expr

let pp_print_threat_model ind ppf
      (id, intro, expr) =
  Format.fprintf ppf "\"%s\" forall %a, %a" id
    pp_print_intro intro pp_print_texpr expr

let pp_print_statement ind ppf = function
  | Mission {id; description; reqs} ->
     Format.fprintf ppf "Mission \"%s\"" id (* TODO *)
  | SafetyReq {id; condition; description; comment} ->
     Format.fprintf ppf "SafetyReq %a"
       (pp_print_safety_req_body ind)
       (id, condition, description, comment)
  | SafetyRel {id; output; faultSrc; description; comment} ->
     Format.fprintf ppf "SafetyRel %a"
       (pp_print_safety_rel_body ind)
       (id, output, faultSrc, description, comment)
  | SafetyEvent {id; probability; description; comment} ->
     Format.fprintf ppf "SafetyEvent %a"
       (pp_print_safety_event_body ind)
       (id, probability, description, comment)
  | CyberReq {id; cia; severity; condition; comment} ->
     Format.fprintf ppf "CyberReq %a"
       (pp_print_cyber_req_body ind)
       (id, cia, severity, condition, comment)
  | CyberRel {id; output; inputs; comment} ->
     Format.fprintf ppf "CyberRel %a"
       (pp_print_cyber_rel_body ind)
       (id, output, inputs, comment)
  | ThreatModel {id; intro; expr} ->
     Format.fprintf ppf "ThreatEffectModel %a"
       (pp_print_threat_model ind)
       (id, intro, expr)
  | ThreatDefense {id; description; comment; threats} ->
     Format.fprintf ppf "ThreatDefense %s {}" id
  | ThreatDatabase id ->
     Format.fprintf ppf "ThreatDatabase \"%s\"" id

let pp_print_verdict_annex ind ppf annex =
  Format.pp_print_list (pp_print_statement ind) ppf annex

let pp_print_ast_indent = pp_print_verdict_annex

let pp_print_ast = pp_print_ast_indent 4
