# SOTERIA++ build and run instructions

## Installation

Follow the [Build the native back-end programs](../README.md) installation instructions to install the soteria++ 
application and dependencies. 

## Debug Prerequisites

The oCaml core package is required to bundle soteria++:

```
$ opam install core && eval $(opam config env)
```

## Build

The tool is built using the following command:

```
$ corebuild -pkg printbox -pkg xml-light soteria_pp.byte 
```

or for the native executable:

```
$ corebuild -pkg printbox -pkg xml-light soteria_pp.native 
```

You can clean a build with the following command. (If you do, you have
to rebuild the tool with the command above)

```
$ corebuild -clean
```

## Run

Soteria++ builds can be configured to [run in OSATE](../README.md) or run form the CLI. The input_path should include 
STEM files: CAPEC.csv, CompDep.csv, ScnArch.csv, Mission.csv, and Defenses.csv. Run the VERDICT OSATE once to 
initialize these files (in ~/workspace/extern/STEM/Output/).

```
$ ./soteria_pp.byte -o output_path input_path 
```
The output path may be omitted with:
```
$ ./soteria_pp.byte input_path 
```

## OCaml Debugger

The [oCaml debugger](https://ocaml.org/manual/debugger.html) provides a CLI to set breakpoints and easily inspect 
variable values. Refer to oCaml debugger manual's [Running the Program](https://ocaml.org/manual/debugger.html#s%3Adebugger-commands) for debug 
commands. After building the soteria++ .byte bundle run the debugger with: 

```
cd ./soteria_pp/_build/
ocamldebug soteria_pp.byte input_path
```

The input_path should include STEM files: CAPEC.csv, CompDep.csv, ScnArch.csv, Mission.csv, and Defenses.csv. Run VERDICT 
in OSATE once to initialize these files (in ~/workspace/extern/STEM/Output/). After the debugger initializes set a breakpoint, 
run the application and inspect a variable. For example:

```
(ocd) set print_length 10000
(ocd) break @ translator 1309
(ocd) run
(ocd) print l_librariesThreats
```

## OCaml Top (and Running Tests)

If you want to run using OCaml top-level, build the project then call ocaml. From the OCaml top CLI load top.ml and 
then your example or test file. For instance:

```
$ ocaml -I ./_build
# #use "examples/top.ml";;
# #use "examples/your_example_file.ml";;
# #use "qualitative-test.ml";;
```
