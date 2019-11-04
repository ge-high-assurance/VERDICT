(* Example from
   "Evil Twins: Handling Repetitions in Attack-Defense Trees"
   by Angele Bossuat and Barbara Kordy.
*)  

let a = ALeaf(("lapt",""), 1.);;
let b = ALeaf(("ex",""), 1.);;
let c = ALeaf(("prex",""), 1.);;
let d = ALeaf(("usb-L",""), 1.);;
let e = ALeaf(("sol",""), 1.);;
let f = ALeaf(("prsol",""), 1.);;
let g = ALeaf(("usb-G",""), 1.);;
let h = ALeaf(("break",""), 1.);;
let j = ALeaf(("memo",""), 1.);;

let i = DLeaf(("enc",""), 5);;

let store_exam = ASUM [c; d];;

let get_exam = APRO [a; b; store_exam];;

let store_sol = C( ASUM [f; g], ASUM[i; h] );;

let get_sol = APRO [a; e; store_sol];;

let exam_attack = APRO [get_exam; get_sol; j];;

dot_gen_show_direct_adtree_file "exam-attack.svg" exam_attack;;

cutsets_ad exam_attack;;