(* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.
*)

(** @author Daniel Larraz *)

let element_position p l =
  let rec aux i = function
    | [] -> None
    | h :: _ when p h -> Some (h, i)
    | _ :: tl -> aux (i+1) tl
  in
  aux 0 l

