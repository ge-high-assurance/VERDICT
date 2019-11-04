(* Example from
   "Quantitative Questions and Attack-Defense Trees"
   by Patrick Schweitzer,
   Joint work with Barbara Kordy and Sjouke Mauw.
   https://pdfs.semanticscholar.org/presentation/79b0/40cb51d1f06e1c67574cff74604981a717ff.pdf *)
   
(* Attacking a Bank Account *)  

(* atm *)
let eavesdrop = ALeaf(("Eavesdrop",""), 1.);;
let findnote = ALeaf(("FindNote",""), 1.);;
let force = ALeaf(("Force",""),1.);;
let card = ALeaf(("Card",""),1.);;
let memorize = DLeaf(("Memorize",""), 5);;

let c1 = C(findnote, ASUM[ memorize; force ]);;
let pin = ASUM [eavesdrop; c1];;
let atm = APRO [pin; card];;

(* online *)
let username = ALeaf(("UserName",""), 1.);;

let phishing = ALeaf(("Phishing",""), 1.);;
let keylogger = ALeaf(("KeyLogger",""), 1.);;
let browser = ALeaf(("Browser",""), 1.);;
let os = ALeaf(("OS", ""), 1.);;
let d1 = DLeaf(("KeyFobs", ""), 5);;
let d2 = DLeaf(("PINPad", ""), 5);;


let password = C( ASUM [phishing; keylogger], 
                  ASUM [ APRO[ d1; d2 ]; browser; os ] );;


let online = APRO [username; password];;

(* attack on bank account *)
let bankaccount = ASUM [ atm; online ];;

dot_gen_show_direct_adtree_file "system-breach.svg" bankaccount;;

(*
# online;;
- : string adtree =
APRO
 [C (ASUM [ALeaf ("Phishing", 1.); ALeaf ("KeyLogger", 1.)],
   APRO
    [ASUM [DLeaf ("KeyFobs", 1); DLeaf ("PINPad", 0)];
     APRO [DLeaf ("Browser", 5); DLeaf ("OS", 5)]]);
  ALeaf ("UserName", 1.)]
*)

formulaOfADTree online;;

(*
# formulaOfADTree online;;
- : string adexp =
APro
 [APro
   [ASum [AVar "Phishing"; AVar "KeyLogger"];
    APro
     [ASum [Not (AVar "KeyFobs"); Not (AVar "PINPad")];
      APro [Not (AVar "Browser"); Not (AVar "OS")]]];
  AVar "UserName"]
*)

dot_gen_show_direct_adtree_file ~rend:"pdf" "system-breach.gv" online;;

cutsets_ad online;;

(*
# cutsets_ad online;;
- : string adexp =
ASum
 [APro
   [AVar "KeyLogger"; AVar "UserName"; Not (AVar "Browser");
    Not (AVar "KeyFobs"); Not (AVar "OS")];
  APro
   [AVar "KeyLogger"; AVar "UserName"; Not (AVar "Browser"); Not (AVar "OS");
    Not (AVar "PINPad")];
  APro
   [AVar "Phishing"; AVar "UserName"; Not (AVar "Browser");
    Not (AVar "KeyFobs"); Not (AVar "OS")];
  APro
   [AVar "Phishing"; AVar "UserName"; Not (AVar "Browser"); Not (AVar "OS");
    Not (AVar "PINPad")]]
*)

likelihoodCut online;;
(*
# likelihoodCut online;;
- : float = 2.2e-10
*)

likelihoodCutImp online;;

(*
# likelihoodCutImp online;;
- : (string adexp * float * float) Core.List.t =
[(APro
   [AVar "KeyLogger"; AVar "UserName"; Not (AVar "Browser"); Not (AVar "OS");
    Not (AVar "PINPad")],
  1e-10, 0.454545454545);
 (APro
   [AVar "Phishing"; AVar "UserName"; Not (AVar "Browser"); Not (AVar "OS");
    Not (AVar "PINPad")],
  1e-10, 0.454545454545);
 (APro
   [AVar "KeyLogger"; AVar "UserName"; Not (AVar "Browser");
    Not (AVar "KeyFobs"); Not (AVar "OS")],
  1e-11, 0.0454545454545);
 (APro
   [AVar "Phishing"; AVar "UserName"; Not (AVar "Browser");
    Not (AVar "KeyFobs"); Not (AVar "OS")],
  1e-11, 0.0454545454545)]
# 0.454545454545 +. 0.454545454545 +. 0.0454545454545 +. 0.0454545454545;;
- : float = 0.999999999999
*)