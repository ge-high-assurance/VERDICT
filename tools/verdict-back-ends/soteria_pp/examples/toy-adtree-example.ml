(* An attack-defense tree example *)  

let a1 = ALeaf(("GPRS-flood", "LoA"), 1.);;
let d1 = DLeaf(("denial-of-service-GPRSprotection", "LoA"), 7);;

let a2 = ALeaf(("RF-flood", "LoA"), 1.);;
let d2 = DLeaf(("denial-of-service-RFprotection", "LoA"), 5);;

let a3 = ALeaf(("code-injection", "LoI"), 1.);;
let d3 = DLeaf(("guarded-aircraft", "LoI"), 7);;

let threatScenario1 = C( a1, d1 );;
let threatScenario2 = C( a2, d2 );;
let threatScenario3 = C( a3, d3 );;

let loa_FlightController = ASUM [ threatScenario1; threatScenario2 ];;
let loi_FlightController = ASUM [ threatScenario3 ];;

let threatCondition = ASUM [ loa_FlightController; loi_FlightController ];;

dot_gen_show_adtree_file "toy-adtree.svg" threatCondition;;

formulaOfADTree threatCondition;;
cutsets_ad threatCondition;;
likelihoodCut threatCondition;;
likelihoodCutImp threatCondition;;

(* Another attack-defense tree example *)

let a11 = ALeaf(("a11", "loa"), 1.);;
let a12 = ALeaf(("a12", "loa"), 1.);;
let a13 = ALeaf(("a13", "loa"), 1.);;
let d11 = DLeaf(("d11", "loa"), 7 );;
let d12 = DLeaf(("d12", "loa"), 1 );;
let d13 = DLeaf(("d13", "loa"), 1 );;

let tree = ASUM[ a11; a12; C( a13, DPRO[ DPRO[ d11; d12 ]; d13]) ];;

formulaOfADTree tree;;
cutsets_ad tree;; 