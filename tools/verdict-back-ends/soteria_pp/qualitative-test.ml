(* 

Copyright © 2020 General Electric Company. All Rights Reserved.

Authors: Kit Siu
Date: 2018-07-16

Updates: 01/11/2019, Kit Siu, updated to include defense (sub)tree types

*)

(* test_constant_prop_ad *)

let test_constant_prop_ad () =
  assert( constant_prop_ad (ANot ATRUE) = AFALSE );
  assert( constant_prop_ad (ANot AFALSE) = ATRUE);
  assert( constant_prop_ad (ANot (AVar "me")) = ANot (AVar "me"));
  assert( constant_prop_ad (ANot (ANot (AVar "me"))) = AVar "me");
  assert( constant_prop_ad (ANot (ANot (ANot (AVar "me")))) = ANot (AVar "me"));
  assert( constant_prop_ad (ANot (APro [ ASum [AVar "a"; AVar "b"]; ASum [AVar "c"; AVar "d"]])) 
          = ASum [ APro [ANot (AVar "a"); ANot (AVar "b")]; APro [ANot (AVar "c"); ANot (AVar "d")] ] );
  assert( constant_prop_ad (ASum[]) = AFALSE );
  assert( constant_prop_ad (APro[]) = ATRUE );
  true;;

test_constant_prop_ad ();;

(* test sumofprod_ad *)

let test_sumofprod_ad () =
    let af = APro [ASum [ANot (AVar "a"); ANot (AVar "b")]; ASum [ANot (AVar "c"); ANot (AVar "d")]]
    and df = DPro [DSum [ANot (AVar "a"); ANot (AVar "b")]; DSum [ANot (AVar "c"); ANot (AVar "d")]]
    in
    assert( sumofprod_ad af = ASum [APro [ANot (AVar "a"); ANot (AVar "c")];  
                                    APro [ANot (AVar "a"); ANot (AVar "d")];
                                    APro [ANot (AVar "b"); ANot (AVar "c")];
                                    APro [ANot (AVar "b"); ANot (AVar "d")]] );
    assert( sumofprod_ad df = DSum [DPro [ANot (AVar "a"); ANot (AVar "c")];  
                                    DPro [ANot (AVar "a"); ANot (AVar "d")];
                                    DPro [ANot (AVar "b"); ANot (AVar "c")];
                                    DPro [ANot (AVar "b"); ANot (AVar "d")]] );
    true
;;

test_sumofprod_ad ();;

(* testing with some AD trees *)
let test_cutsets_ad () =
	let a = ALeaf(("a_atk","a_def"), 1.)
	and b = DLeaf(("b_atk","b_def"), 5)
	and c = ALeaf(("c_atk","c_def"), 1.) in
	let t1 = C(a, b) in
    let t2 = ASUM [ c; t1 ]
    and t3 = APRO [ c; t1 ]
    and t4 = ASUM [ a; t1 ] in
    assert( cutsets_ad t1 = APro [ AVar ("a_atk","a_def",""); ANot (AVar ("b_atk","b_def","5")) ] );
    assert( cutsets_ad t2 = ASum [ AVar ("c_atk","c_def",""); APro [AVar ("a_atk","a_def",""); ANot (AVar ("b_atk","b_def","5"))]] );
    assert( cutsets_ad t3 = APro [ AVar ("a_atk","a_def",""); AVar ("c_atk","c_def",""); ANot (AVar ("b_atk","b_def","5")) ] );
    assert( cutsets_ad t4 = AVar ("a_atk","a_def","") );
    true;;
    
test_cutsets_ad ();;

(* test cutsets_ad algo again with different combinations of PoS's and SoP's *)
let test_cutsets_ad () =

    let i_leaf = ("i_atk","i_def")
    and a_leaf = ("a_atk","a_def")
    and b_leaf = ("b_atk","b_def")
    and c_leaf = ("c_atk","c_def")
    and d_leaf = ("d_atk","d_def")
    in

    let sss = C( ALeaf(i_leaf, 1.0), DSUM[ DSUM[ DLeaf(a_leaf, 5); DLeaf(b_leaf, 5) ]; DSUM[ DLeaf(c_leaf, 5); DLeaf(d_leaf, 5) ]; ] )(* working *)
    and spp = C( ALeaf(i_leaf, 1.0), DSUM[ DPRO[ DLeaf(a_leaf, 5); DLeaf(b_leaf, 5) ]; DPRO[ DLeaf(c_leaf, 5); DLeaf(d_leaf, 5) ]; ] )(* working *)
    and ssp = C( ALeaf(i_leaf, 1.0), DSUM[ DSUM[ DLeaf(a_leaf, 5); DLeaf(b_leaf, 5) ]; DPRO[ DLeaf(c_leaf, 5); DLeaf(d_leaf, 5) ]; ] )(* working *)
    and sps = C( ALeaf(i_leaf, 1.0), DSUM[ DPRO[ DLeaf(a_leaf, 5); DLeaf(b_leaf, 5) ]; DSUM[ DLeaf(c_leaf, 5); DLeaf(d_leaf, 5) ]; ] )(* working *)
    and ppp = C( ALeaf(i_leaf, 1.0), DPRO[ DPRO[ DLeaf(a_leaf, 5); DLeaf(b_leaf, 5) ]; DPRO[ DLeaf(c_leaf, 5); DLeaf(d_leaf, 5) ]; ] )(* working *)
    and p   = C( ALeaf(i_leaf, 1.0), DPRO[ DSUM[ DLeaf(c_leaf, 5); DLeaf(d_leaf, 5) ]; ] )(* working *) 
    and pss = C( ALeaf(i_leaf, 1.0), DPRO[ DSUM[ DLeaf(a_leaf, 5); DLeaf(b_leaf, 5) ]; DSUM[ DLeaf(c_leaf, 5); DLeaf(d_leaf, 5) ]; ] )(* working *)        
    and psp = C( ALeaf(i_leaf, 1.0), DPRO[ DSUM[ DLeaf(a_leaf, 5); DLeaf(b_leaf, 5) ]; DPRO[ DLeaf(c_leaf, 5); DLeaf(d_leaf, 5) ]; ] )(* was ANot working, now working *) 
    and pps = C( ALeaf(i_leaf, 1.0), DPRO[ DPRO[ DLeaf(a_leaf, 5); DLeaf(b_leaf, 5) ]; DSUM[ DLeaf(c_leaf, 5); DLeaf(d_leaf, 5) ]; ] )(* was ANot working, now working *)
     
    in

    let i_cut = ("i_atk","i_def","")
    and a_cut = ("a_atk","a_def","5")
    and b_cut = ("b_atk","b_def","5")
    and c_cut = ("c_atk","c_def","5")
    and d_cut = ("d_atk","d_def","5")
    in

    assert( cutsets_ad sss = APro[ AVar i_cut; DPro [ANot (AVar a_cut); ANot (AVar b_cut); ANot (AVar c_cut); ANot (AVar d_cut)] ] );
    assert( cutsets_ad spp = APro[ AVar i_cut; DPro [DSum [ANot (AVar a_cut); ANot (AVar b_cut)]; DSum [ANot (AVar c_cut); ANot (AVar d_cut)]] ]);
    assert( cutsets_ad ssp = APro[ AVar i_cut; DPro [ANot (AVar a_cut); ANot (AVar b_cut); DSum [ANot (AVar c_cut); ANot (AVar d_cut)]] ]);
    assert( cutsets_ad sps = APro[ AVar i_cut; DPro [ANot (AVar c_cut); ANot (AVar d_cut); DSum [ANot (AVar a_cut); ANot (AVar b_cut)]] ]);
    assert( cutsets_ad ppp = APro[ AVar i_cut; DSum [ANot (AVar a_cut); ANot (AVar b_cut); ANot (AVar c_cut); ANot (AVar d_cut)] ]);
    assert( cutsets_ad p   = APro[ AVar i_cut; DPro [ANot (AVar c_cut); ANot (AVar d_cut)] ]);
    assert( cutsets_ad pss = APro[ AVar i_cut; DSum [DPro [ANot (AVar a_cut); ANot (AVar b_cut)]; DPro [ANot (AVar c_cut); ANot (AVar d_cut)];] ]);
    assert( cutsets_ad psp = APro[ AVar i_cut; DSum [ANot (AVar c_cut); ANot (AVar d_cut); DPro [ANot (AVar a_cut); ANot (AVar b_cut);] ] ]);
    assert( cutsets_ad pps = APro[ AVar i_cut; DSum [ANot (AVar a_cut); ANot (AVar b_cut); DPro [ANot (AVar c_cut); ANot (AVar d_cut) ] ] ]);
    
    true;;
    
test_cutsets_ad ();;

