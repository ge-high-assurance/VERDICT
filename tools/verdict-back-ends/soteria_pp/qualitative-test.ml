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
	let a = ALeaf("a", 1.) 
	and b = DLeaf("b", 5)
	and c = ALeaf("c", 1.) in
	let t1 = C(a, b) in
    let t2 = ASUM [ c; t1 ]
    and t3 = APRO [ c; t1 ]
    and t4 = ASUM [ a; t1 ] in
    assert( cutsets_ad t1 = APro [ AVar "a"; ANot (AVar "b") ] );
    assert( cutsets_ad t2 = ASum [ AVar "c"; APro [AVar "a"; ANot (AVar "b")]] );
    assert( cutsets_ad t3 = APro [ AVar "a"; AVar "c"; ANot (AVar "b") ] );
    assert( cutsets_ad t4 = AVar "a" );
    true;;
    
test_cutsets_ad ();;

(* test cutsets_ad algo again with different combinations of PoS's and SoP's *)
let test_cutsets_ad () =
    let sss = C( ALeaf("i", 1.0), DSUM[ DSUM[ DLeaf("a", 5); DLeaf("b", 5) ]; DSUM[ DLeaf("c", 5); DLeaf("d", 5) ]; ] )(* working *)
    and spp = C( ALeaf("i", 1.0), DSUM[ DPRO[ DLeaf("a", 5); DLeaf("b", 5) ]; DPRO[ DLeaf("c", 5); DLeaf("d", 5) ]; ] )(* working *)
    and ssp = C( ALeaf("i", 1.0), DSUM[ DSUM[ DLeaf("a", 5); DLeaf("b", 5) ]; DPRO[ DLeaf("c", 5); DLeaf("d", 5) ]; ] )(* working *)
    and sps = C( ALeaf("i", 1.0), DSUM[ DPRO[ DLeaf("a", 5); DLeaf("b", 5) ]; DSUM[ DLeaf("c", 5); DLeaf("d", 5) ]; ] )(* working *)
    and ppp = C( ALeaf("i", 1.0), DPRO[ DPRO[ DLeaf("a", 5); DLeaf("b", 5) ]; DPRO[ DLeaf("c", 5); DLeaf("d", 5) ]; ] )(* working *)
    and p   = C( ALeaf("i", 1.0), DPRO[ DSUM[ DLeaf("c", 5); DLeaf("d", 5) ]; ] )(* working *) 
    and pss = C( ALeaf("i", 1.0), DPRO[ DSUM[ DLeaf("a", 5); DLeaf("b", 5) ]; DSUM[ DLeaf("c", 5); DLeaf("d", 5) ]; ] )(* working *)        
    and psp = C( ALeaf("i", 1.0), DPRO[ DSUM[ DLeaf("a", 5); DLeaf("b", 5) ]; DPRO[ DLeaf("c", 5); DLeaf("d", 5) ]; ] )(* was ANot working, now working *) 
    and pps = C( ALeaf("i", 1.0), DPRO[ DPRO[ DLeaf("a", 5); DLeaf("b", 5) ]; DSUM[ DLeaf("c", 5); DLeaf("d", 5) ]; ] )(* was ANot working, now working *)
     
    in
    
    assert( cutsets_ad sss = APro[ AVar "i"; DPro [ANot (AVar "a"); ANot (AVar "b"); ANot (AVar "c"); ANot (AVar "d")] ] );
    assert( cutsets_ad spp = APro[ AVar "i"; DPro [DSum [ANot (AVar "a"); ANot (AVar "b")]; DSum [ANot (AVar "c"); ANot (AVar "d")]] ]);
    assert( cutsets_ad ssp = APro[ AVar "i"; DPro [ANot (AVar "a"); ANot (AVar "b"); DSum [ANot (AVar "c"); ANot (AVar "d")]] ]);
    assert( cutsets_ad sps = APro[ AVar "i"; DPro [ANot (AVar "c"); ANot (AVar "d"); DSum [ANot (AVar "a"); ANot (AVar "b")]] ]);
    assert( cutsets_ad ppp = APro[ AVar "i"; DSum [ANot (AVar "a"); ANot (AVar "b"); ANot (AVar "c"); ANot (AVar "d")] ]);
    assert( cutsets_ad p   = APro[ AVar "i"; DPro [ANot (AVar "c"); ANot (AVar "d")] ]);
    assert( cutsets_ad pss = APro[ AVar "i"; DSum [DPro [ANot (AVar "a"); ANot (AVar "b")]; DPro [ANot (AVar "c"); ANot (AVar "d")];] ]);
    assert( cutsets_ad psp = APro[ AVar "i"; DSum [ANot (AVar "c"); ANot (AVar "d"); DPro [ANot (AVar "a"); ANot (AVar "b");] ] ]);
    assert( cutsets_ad pps = APro[ AVar "i"; DSum [ANot (AVar "a"); ANot (AVar "b"); DPro [ANot (AVar "c"); ANot (AVar "d") ] ] ]);
    
    true;;
    
test_cutsets_ad ();;

