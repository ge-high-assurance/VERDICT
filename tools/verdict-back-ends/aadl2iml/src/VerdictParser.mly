
%{

    module C = CommonAstTypes
    open VerdictAst

    let find_pred_opt (lst : 'a list) (pred : 'a -> 'b option) =
      List.fold_left
        begin
          fun prv nxt ->
            match pred nxt with
            | Some v -> Some v
            | None -> prv
        end
        None lst

    let force_opt ?(msg="Not present") = function
      | Some v -> v
      | None -> failwith msg

    let force_opt_def def = function
      | Some v -> v
      | None -> def

    type mission_param =
      | Mission_ID of string
      | Mission_DESCRIPTION of string
      | Mission_COMMENT of string
      | Mission_REQS of string list

    let build_mission params =
      Mission {
          id =
            find_pred_opt
              params (function Mission_ID id -> Some id | _ -> None)
            |> force_opt ~msg:"No ID specified";

          description =
            find_pred_opt
              params (function Mission_DESCRIPTION desc -> Some desc | _ -> None);

          comment =
            find_pred_opt
              params (function Mission_COMMENT comment -> Some comment | _ -> None);

          reqs =
            find_pred_opt
              params (function Mission_REQS reqs -> Some reqs | _ -> None)
            |> force_opt ~msg:"No cyber reqs specified";
        }

    type cyber_req_param =
      | Req_ID of string
      | Req_CIA of cia
      | Req_SEVERITY of severity
      | Req_CONDITION of lexpr
      | Req_COMMENT of string
      | Req_DESCRIPTION of string
      | Req_PHASES of string
      | Req_EXTERNAL of string
      | Req_DUD

    let build_cyber_req params =
      CyberReq {
          id = (match (List.find_opt
                        (function Req_ID _ -> true
                                | _ -> false) params) with
                | Some (Req_ID id) -> id
                | _ -> failwith "no ID specified");
          cia = (match (List.find_opt
                        (function Req_CIA _ -> true
                                | _ -> false) params) with
                | Some (Req_CIA cia) -> Some cia
                | _ -> None);
          severity = (match (List.find_opt
                        (function Req_SEVERITY _ -> true
                                | _ -> false) params) with
                | Some (Req_SEVERITY severity) -> severity
                | _ -> failwith "no severity specified");
          condition = (match (List.find_opt
                        (function Req_CONDITION _ -> true
                                | _ -> false) params) with
                | Some (Req_CONDITION condition) -> condition
                | _ -> failwith "no condition specified");
          comment = (match (List.find_opt
                        (function Req_COMMENT _ -> true
                                | _ -> false) params) with
                | Some (Req_COMMENT comment) -> Some comment
                | _ -> None);
          description = (match (List.find_opt
                        (function Req_DESCRIPTION _ -> true
                                | _ -> false) params) with
                | Some (Req_DESCRIPTION description) -> Some description
                | _ -> None);
          phases = (match (List.find_opt
                        (function Req_PHASES _ -> true
                                | _ -> false) params) with
                | Some (Req_PHASES phases) -> Some phases
                | _ -> None);
          extern = (match (List.find_opt
                        (function Req_EXTERNAL _ -> true
                                | _ -> false) params) with
                | Some (Req_EXTERNAL extern) -> Some extern
                | _ -> None);
      }

    type cyber_rel_param =
      | Rel_ID of string
      | Rel_OUTPUT of port
      | Rel_INPUTS of lexpr
      | Rel_COMMENT of string
      | Rel_DESCRIPTION of string
      | Rel_PHASES of string
      | Rel_EXTERNAL of string

    let build_cyber_rel params =
      CyberRel {
          id = (match (List.find_opt
                        (function Rel_ID _ -> true
                                | _ -> false) params) with
                | Some (Rel_ID id) -> id
                | _ -> failwith "no ID specified");
          output = (match (List.find_opt
                        (function Rel_OUTPUT _ -> true
                                | _ -> false) params) with
                | Some (Rel_OUTPUT output) -> output
                | _ -> failwith "no output specified");
          inputs = (match (List.find_opt
                        (function Rel_INPUTS _ -> true
                                | _ -> false) params) with
                | Some (Rel_INPUTS inputs) -> Some inputs
                | _ -> None);
          comment = (match (List.find_opt
                        (function Rel_COMMENT _ -> true
                                | _ -> false) params) with
                | Some (Rel_COMMENT comment) -> Some comment
                | _ -> None);
          description = (match (List.find_opt
                        (function Rel_DESCRIPTION _ -> true
                                | _ -> false) params) with
                | Some (Rel_DESCRIPTION description) -> Some description
                | _ -> None);
          phases = (match (List.find_opt
                        (function Rel_PHASES _ -> true
                                | _ -> false) params) with
                | Some (Rel_PHASES phases) -> Some phases
                | _ -> None);
          extern = (match (List.find_opt
                        (function Rel_EXTERNAL _ -> true
                                | _ -> false) params) with
                | Some (Rel_EXTERNAL extern) -> Some extern
                | _ -> None);
      }

    type threat_model_param =
      | Threat_ID of string
      | Threat_ENTITIES of tintro * texpr
      | Threat_CIA of cia
      | Threat_REFERENCE of string
      | Threat_ASSUMPTIONS of string list
      | Threat_DEFENSES of string list
      | Threat_DESCRIPTION of string
      | Threat_COMMENT of string

    let build_threat_model params =
      ThreatModel {
          id = (match (List.find_opt
                          (function Threat_ID _ -> true
                                  | _ -> false) params) with
                 | Some (Threat_ID id) -> id
                 | _ -> failwith "no ID specified");
          intro = (match (List.find_opt
                          (function Threat_ENTITIES _ -> true
                                  | _ -> false) params) with
                 | Some (Threat_ENTITIES (intro, _)) -> intro
                 | _ -> failwith "no entities specified");
          expr = (match (List.find_opt
                          (function Threat_ENTITIES _ -> true
                                  | _ -> false) params) with
                 | Some (Threat_ENTITIES (_, expr)) -> expr
                 | _ -> failwith "no entities specified");
          cia = (match (List.find_opt
                          (function Threat_CIA _ -> true
                                  | _ -> false) params) with
                 | Some (Threat_CIA cia) -> cia
                 | _ -> CIA_I);
          reference = (match (List.find_opt
                          (function Threat_REFERENCE _ -> true
                                  | _ -> false) params) with
                 | Some (Threat_REFERENCE reference) -> Some reference
                 | _ -> None);
          assumptions = (match (List.find_opt
                          (function Threat_ASSUMPTIONS _ -> true
                                  | _ -> false) params) with
                 | Some (Threat_ASSUMPTIONS assumptions) -> assumptions
                 | _ -> []);
          comment = (match (List.find_opt
                        (function Threat_COMMENT _ -> true
                                | _ -> false) params) with
                | Some (Threat_COMMENT comment) -> Some comment
                | _ -> None);
          description = (match (List.find_opt
                        (function Threat_DESCRIPTION _ -> true
                                | _ -> false) params) with
                | Some (Threat_DESCRIPTION description) -> Some description
                | _ -> None);
        }

    type threat_defense_param =
      | Defense_ID of string
      | Defense_THREATS of string list
      | Defense_DESCRIPTION of string
      | Defense_COMMENT of string

    let build_threat_defense params =
      ThreatDefense {
        id =
          find_pred_opt
            params (function Defense_ID id -> Some id | _ -> None)
          |> force_opt ~msg:"No ID specified";

        threats =
          find_pred_opt
            params (function Defense_THREATS threats -> Some threats | _ -> None)
          |> force_opt ~msg:"No threats specified";

        description =
          find_pred_opt
            params (function Defense_DESCRIPTION desc -> Some desc | _ -> None);

        comment =
          find_pred_opt
            params (function Defense_COMMENT comment -> Some comment | _ -> None);
      }

%}

%token MISSION CYBER_REQ CYBER_REL THREAT_MODEL THREAT_DEFENSE THREAT_DATABASE
%token PARAM_ID CIA SEVERITY CONDITION COMMENT OUTPUT INPUTS DESCRIPTION PHASES EXTERNAL ENTITIES ASSUMPTIONS REFERENCE THREATS CYBER_REQS TARGET_LIKELIHOOD
%token FORALL EXISTS CONTAINS
%token CIA_C CIA_I CIA_A
%token SEVERITY_NONE SEVERITY_MINOR SEVERITY_MAJOR SEVERITY_HAZARDOUS SEVERITY_CATASTROPHIC
%token ANNEX_BLOCK_END "**}"
%token COLON ":"
%token EQUAL "="
%token IMPLIES "=>"
%token SEMICOLON ";"
%token COMMA ","
%token PERIOD "."
%token LCURLY_BRACKET "{"
%token RCURLY_BRACKET "}"
%token LSQ_BRACKET "["
%token RSQ_BRACKET "]"
%token LPAREN "("
%token RPAREN ")"
%token VERT_BAR "|"
%token AND OR NOT
%token TARGET_LIKELIHOOD_VAL
%token <string>ID
%token <string>STRING

%start<VerdictAst.t> verdict_annex

%%

verdict_annex:
  | statements = list(s = statement option(";") { s }) "**}"
    { statements }

statement:
  | mission = mission { mission }
  | req = cyber_req { req }
  | rel = cyber_rel { rel }
  | threat = threat_model { threat }
  | defense = threat_defense { defense }
  | database = threat_database { database }

port:
  | port = ID ":" cia = cia { (port, cia) }

mission:
  | MISSION "{"
    params = list(p = mission_param option(";") { p }) "}"
    { build_mission params }

mission_param:
  | PARAM_ID "=" id = STRING { Mission_ID id }
  | DESCRIPTION "=" desc = STRING { Mission_DESCRIPTION desc }
  | COMMENT "=" comment = STRING { Mission_COMMENT comment }
  | CYBER_REQS "=" hd = STRING tl = list("," req = STRING { req })
    { Mission_REQS (hd :: tl) }

cyber_req:
  | CYBER_REQ "{"
    params = list(p = cyber_req_param option(";") { p }) "}"
    { build_cyber_req params }

cyber_req_param:
  | PARAM_ID "=" id = STRING { Req_ID id }
  | CIA "=" cia = cia { Req_CIA cia }
  | SEVERITY "=" severity = severity { Req_SEVERITY severity }
  | CONDITION "=" condition = lexpr { Req_CONDITION condition }
  | COMMENT "=" comment = STRING { Req_COMMENT comment }
  | DESCRIPTION "=" description = STRING { Req_DESCRIPTION description }
  | PHASES "=" phases = STRING { Req_PHASES phases }
  | EXTERNAL "=" extern = STRING { Req_EXTERNAL extern }
  | TARGET_LIKELIHOOD "=" TARGET_LIKELIHOOD_VAL { Req_DUD }

cyber_rel:
  | CYBER_REL "{"
    params = list(p = cyber_rel_param option(";") { p }) "}"
    { build_cyber_rel params }

  | CYBER_REL id = STRING "=" inputs = lexpr "=>" output = port
    { (Rel_ID id) :: (Rel_INPUTS inputs)
      :: (Rel_OUTPUT output) :: [] |> build_cyber_rel}
  | CYBER_REL id = STRING "=>" output = port
    { (Rel_ID id) :: (Rel_OUTPUT output) :: []
      |> build_cyber_rel }

cyber_rel_param:
  | PARAM_ID "=" id = STRING { Rel_ID id }
  | OUTPUT "=" output = port { Rel_OUTPUT output }
  | INPUTS "=" inputs = lexpr { Rel_INPUTS inputs }
  | COMMENT "=" comment = STRING { Rel_COMMENT comment }
  | DESCRIPTION "=" description = STRING { Rel_DESCRIPTION description }
  | PHASES "=" phases = STRING { Rel_PHASES phases }
  | EXTERNAL "=" extern = STRING { Rel_EXTERNAL extern }

lexpr:
  | l_or = l_or { l_or }

lexpr_term:
  | port = lport { port }
  | not = lnot { not }

  | "(" lexpr = lexpr ")" { lexpr }
  | "[" lexpr = lexpr "]" { lexpr }
  | "{" lexpr = lexpr "}" { lexpr }

lport:
  | port = port { LPort port }

l_or:
  | expr = l_and exprs = list(OR expr = l_and { expr })
    { if exprs = [] then expr else LOr (expr :: exprs) }

l_and:
  | expr = lexpr_term exprs = list(AND expr = lexpr_term { expr })
    { if exprs = [] then expr else LAnd (expr :: exprs) }

lnot:
  | NOT expr = lexpr_term { LNot expr }

cia:
  | CIA_C { CIA_C }
  | CIA_I { CIA_I }
  | CIA_A { CIA_A }

severity:
  | SEVERITY_NONE { Severity_None }
  | SEVERITY_MINOR { Severity_Minor }
  | SEVERITY_MAJOR { Severity_Major }
  | SEVERITY_HAZARDOUS { Severity_Hazardous }
  | SEVERITY_CATASTROPHIC { Severity_Catastrophic }

threat_defense:
  | THREAT_DEFENSE "{"
    params = list(p = threat_defense_param option(";") { p }) "}"
    { build_threat_defense params }

threat_defense_param:
  | PARAM_ID "=" id = STRING { Defense_ID id }
  | THREATS "=" hd = STRING tl = list("," t = STRING { t })
    { Defense_THREATS (hd :: tl) }
  | DESCRIPTION "=" desc = STRING { Defense_DESCRIPTION desc }
  | COMMENT "=" comment = STRING { Defense_COMMENT comment }

threat_database:
  | THREAT_DATABASE id = STRING { ThreatDatabase id }

threat_model:
  | THREAT_MODEL "{"
    params = list(p = threat_model_param option(";") { p }) "}"
    { build_threat_model params }

threat_model_param:
  | PARAM_ID "=" id = STRING { Threat_ID id }
  | ENTITIES "=" entities = entities { entities }
  | CIA "=" cia = cia { Threat_CIA cia }
  | REFERENCE "=" reference = STRING { Threat_REFERENCE reference }
  | ASSUMPTIONS "=" hd = ID tl = list("," v = ID { v })
    { Threat_ASSUMPTIONS (hd :: tl) }
  | DESCRIPTION "=" desc = STRING { Threat_DESCRIPTION desc }
  | COMMENT "=" comment = STRING { Threat_COMMENT comment }

entities:
  | "{" tintro = tintro "|" texpr = texpr "}"
    { Threat_ENTITIES (tintro, texpr) }

texpr:
  | FORALL tintro = tintro "," texpr = texpr
    { TForall (tintro, texpr) }
  | EXISTS tintro = tintro "," texpr = texpr
    { TExists (tintro, texpr) }
  | tor = tor { tor }
  | ant = tor "=>" cons = tor { TImplies (ant, cons) }

tor:
  | expr = tand exprs = list(OR expr = tand { expr })
    { TOr (expr :: exprs) }

tand:
  | expr = texpr_term exprs = list(AND expr = texpr_term {expr})
    { TAnd (expr :: exprs) }

texpr_term:
  | left = tvar "=" right = tvar
    { TEqual (left, right) }
  | left = tvar CONTAINS right = tvar
    { TContains (left, right) }
  | NOT expr = texpr_term
    { TNot expr }

  | "(" texpr = texpr ")" { texpr }
  | "[" texpr = texpr "]" { texpr }
  | "{" texpr = texpr "}" { texpr }

tintro:
  | var = ID ":" var_type = ID { {var=var; var_type=var_type} }

tvar:
  | id = ID ids = list("." id = ID { id }) { id :: ids }
  | id = STRING { id :: [] }
