# Translator from AADL to IML

## Requirements

- OCaml 4.07 or later,
- [Dune 2.2 or later](https://github.com/ocaml/dune), and
- [Menhir](http://gallium.inria.fr/~fpottier/menhir/)

To install the OCaml compiler and libraries is recommended to use [opam](https://opam.ocaml.org/)

## Building and executing

Run `make`, it will generate a binary called `aadl2iml` in the `bin` directory. Then run:

`./bin/aadl2iml -ps <prop_set_name> [-o <output.iml>] <file_1.aadl|dir1> ... <file_N.aadl|dir_N>`

where `<prop_set_name>` is the name of the property set with the AADL VERDICT Properties.
For instance, `CASE_Consolidated_Properties`.
