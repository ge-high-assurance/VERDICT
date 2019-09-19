# Translator from AADL to IML

## Requirements

- OCaml 4.06 or later,
- [Ocamlbuild](https://github.com/ocaml/ocamlbuild), and
- [Menhir](http://gallium.inria.fr/~fpottier/menhir/)

To install the OCaml compiler and libraries is recommended to use [opam](https://opam.ocaml.org/)

## Building and executing

Run `make`, it will generate a binary called `main.native`. Then run:

`./main.native [-o <output.iml>] <input_1.aadl|dir1> ... <input_N.aadl|dir_N>`
