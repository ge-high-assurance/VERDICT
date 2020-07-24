# Instructions to build the SOTERIA++ tool

-------------------
** Prerequisites **
-------------------

OCaml version 4.07.0 is required, installed using opam version
2.0.0. The following OCaml packages are required: async, core,
core_extended, ocamlbuild, ocamlfind, printbox, and xml-light. These
can be installed using the following command:


```
$ opam install async core core_extended ocamlbuild ocamlfind printbox xml-light
```

Edit your ~/.ocamlinit file by adding the following lines:

```
#use "topfind" ;;
#thread ;;
#load "stdlib.cma" ;;
#require "async" ;;
#require "core_extended" ;;
open Core ;;
```


Graphviz is also required, which can be installed using MacPorts with the following command: 


```
$ port install graphviz
```

-------------------
**     Build     **
-------------------

The tool is built using the following command:


```
$ corebuild -pkg printbox -pkg xml-light soteria_pp.byte 
```

or

```
$ corebuild -pkg printbox -pkg xml-light soteria_pp.native 
```

Note that you can clean a build with the following command. (If you do, you have
to rebuild the tool with the command above)

```
$ corebuild -clean
```

-------------------
**      Run      **
-------------------

Run the tool by executing the following command. The following files are expected in the input_path: CAPEC.csv, CompDep.csv, ScnArch.csv, Mission.csv, and Defenses.csv.


```
$ ./soteria_pp.byte -o output_path input_path 
```

Alternatively, the output path can be left out.

```
$ ./soteria_pp.byte input_path 
```



-------------------
**   OCaml Top   **
-------------------

If you want to run using OCaml top-level, cd to /examples, then call ocaml. From OCaml top, load top.ml,
then load your example file. For instance:



$ cd examples

$ ocaml

\# \#use "top.ml";;

\# \#use "your_file_name.ml";;
