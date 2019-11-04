(* 

Authors: Kit Siu
Date: 2019-01-14

Updates: YYYY-MM-DD, <name>, <modifications>.

*)

(**
 
Since the translator from STEM outputs to SOTERIA++ inputs are highly dependent on the 
headers of the STEM output files, these checks are to validate that the STEM outputs
are what's expected by the translator. Each check below lists the expected header of 
each input file. The order of the header does not matter. Also, only headers used by
SOTERIA++ are listed (e.g., STEM can output headers not used by SOTERIA++, but headers
expected by SOTERIA++ need to be present).

Checks:

   - {b check_CompInstance_file} : check_CompInstance_file compNames.
   
   - {b check_InputPorts_file} : check_InputPorts_file compIn.
   
   - {b check_OutputPorts_file} : check_OutputPorts_file compOut.
   
   - {b check_CompDependencies_file} : check_CompDependencies_file compSeman.
   
   - {b check_CompDependencies2_file} : check_CompDependencies2_file compCIA.

   - {b check_CAPEC_file} : check_CAPEC_file capecs.

   - {b check_SelectedDefenses_file} : check_SelectedDefenses_file compAD.

   - {b check_ScnArch_file} : check_SelectedDefenses_file arch.

   - {b check_Mission_file} : check_Mission_file mission.

*)


open Core ;;

(* utility function *)
let rec doHeadersMatch headerList inChannelList =
  match headerList with
  | hd::tl -> ( List.exists inChannelList ~f:(fun x -> x = hd) ) && (doHeadersMatch tl inChannelList)
  | [] -> true
;;


(* file:            CompInstance.csv 
   description:     This file lists the Component and the Instance names.
   expected header: Component | Instance *)
   
let check_CompInstance_file compNames =
  let inChannelHeader = List.hd_exn compNames
  in 
  let inChannelList = String.split_on_chars inChannelHeader ~on:[','; '\"']
  and expectedHeader = ["Component"; "Instance"]
  in 
  assert( doHeadersMatch expectedHeader inChannelList ); true 
;;


(* file:            InputPorts.csv 
   description:     This file lists all the input ports by component.
   expected header: Component | InputPort *)
   
let check_InputPorts_file compIn =
  let expectedHeader = ["Component"; "InputPort"]
  and inChannelHeader = List.hd_exn compIn
  in 
  let inChannelList = String.split_on_chars inChannelHeader ~on:[','; '\"']
  in 
  assert( doHeadersMatch expectedHeader inChannelList ); true 
;;


(* file:            OutputPorts.csv 
   description:     This file lists all the Output ports by component.
   expected header: Component | OutputPort *)
   
let check_OutputPorts_file compOut =
  let expectedHeader = ["Component"; "OutputPort"]
  and inChannelHeader = List.hd_exn compOut
  in 
  let inChannelList = String.split_on_chars inChannelHeader ~on:[','; '\"']
  in 
  assert( doHeadersMatch expectedHeader inChannelList ); true 
;;


(* file:            CompDependencies.csv 
   description:     This file lists, by component, the InputPort/OutputPort dependencies based on CIA.
   expected header: Component | InputPort | InputConfidentiality | InputIntegrity | InputAvailability |
                    OutputPort | OutputConfidentiality | OutputIntegrity | OutputAvailability *)
   
let check_CompDependencies_file compSeman =
  let expectedHeader = ["Component"; "InputPort"; "InputConfidentiality"; "InputIntegrity"; "InputAvailability";
                        "OutputPort"; "OutputConfidentiality"; "OutputIntegrity"; "OutputAvailability"]
  and inChannelHeader = List.hd_exn compSeman
  in 
  let inChannelList = String.split_on_chars inChannelHeader ~on:[','; '\"']
  in 
  assert( doHeadersMatch expectedHeader inChannelList ); true 
;;


(* file:            CompDependencies2.csv 
   description:     This file lists, by component, if it is affected by CIA.
   expected header: Component | Integrity | Availability *)
   
let check_CompDependencies2_file compCIA =
  let expectedHeader = ["Component"; "Integrity"; "Availability";]
  and inChannelHeader = List.hd_exn compCIA
  in 
  let inChannelList = String.split_on_chars inChannelHeader ~on:[','; '\"']
  in 
  assert( doHeadersMatch expectedHeader inChannelList ); true 
;;


(* file:            CAPEC.csv 
   description:     This file lists the component, instance, CAPEC effected, CAPEC description, CIA.
   expected header: CompType | CompInst | Confidentiality | Integrity | Availability *)
   
let check_CAPEC_file capecs =
  let expectedHeader = ["CompType"; "CompInst"; "Integrity"; "Availability"; "Confidentiality"]
  and inChannelHeader = List.hd_exn capecs
  in 
  let inChannelList = String.split_on_chars inChannelHeader ~on:[','; '\"']
  in 
  assert( doHeadersMatch expectedHeader inChannelList ); true 
;;


(* file:            SelectedDefenses.csv 
   description:     This file lists the component, instance, CAPEC, CIA, defense, and the DAL.
   expected header: Component | Component Instance | Attack | Integrity | Availability | Defense | DAL *)
   
let check_SelectedDefenses_file compAD =
  let expectedHeader = ["Component"; "Component Instance"; "Attack"; "Integrity"; "Availability"; "Defense"; "DAL"]
  and inChannelHeader = List.hd_exn compAD
  in 
  let inChannelList = String.split_on_chars inChannelHeader ~on:[','; '\"']
  in 
  assert( doHeadersMatch expectedHeader inChannelList ); true 
;;


(* file:            ScnArch.csv 
   description:     This file lists the output component instance and output port and maps it to 
                    the input component instance and input port.
   expected header: CompOut | PortOut | CompIn | PortIn *)
   
let check_ScnArch_file arch =
  let expectedHeader = ["CompOut"; "PortOut"; "CompIn"; "PortIn"; ]
  and inChannelHeader = List.hd_exn arch
  in 
  let inChannelList = String.split_on_chars inChannelHeader ~on:[','; '\"']
  in 
  assert( doHeadersMatch expectedHeader inChannelList ); true 
;;


(* file:            Mission.csv 
   description:     This file describes the mission.
   expected header: CyberReqId | CompOutputDependency | Confidentiality | Integrity | Availability
                    Severity | MissionImpactCIA | CyberReq | CompInstanceDependency *)
   
let check_Mission_file mission =
  let expectedHeader = ["CyberReqId"; "CompOutputDependency"; 
                        "Confidentiality"; "Integrity"; "Availability"; 
                        "Severity"; "MissionImpactCIA"; "CyberReq"; "CompInstanceDependency"]
  and inChannelHeader = List.hd_exn mission
  in 
  let inChannelList = String.split_on_chars inChannelHeader ~on:[','; '\"']
  in 
  assert( doHeadersMatch expectedHeader inChannelList ); true 
;;