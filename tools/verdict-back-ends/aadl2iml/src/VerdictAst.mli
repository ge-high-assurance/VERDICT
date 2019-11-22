
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

val pp_print_ast_indent : int -> Format.formatter -> t -> unit

val pp_print_ast : Format.formatter -> t -> unit
