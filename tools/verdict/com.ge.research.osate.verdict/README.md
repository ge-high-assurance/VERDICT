# VERDICT: An OSATE plugin for architectural and behavioral analysis of AADL models
 ----

## Installation Instructions:

**Dependencies:**

1. [Java 8](https://www.java.com/en/download/)

2. [Ocaml 4.07.1](https://ocaml.org/docs/install.html) or above on
   Linux or Mac OS

3. [GraphViz](https://www.graphviz.org/download/): Graph Visualization
   Software

4. [OSATE 2](https://osate-build.sei.cmu.edu/download/osate/stable/):
   AADL Tool Environment (The instructions are only tested on OSATE
   version 2.3.2, 2.5.0, and 2.6.0. With OSATE 2.5.0 & 2.6.0, you need
   to manually install
   [AGREE](https://osate-build.sei.cmu.edu/download/osate/stable/2.3.7/updates/))

5. [Z3](https://github.com/Z3Prover/z3): The Z3 Theorem Prover (Make
   sure Z3 is installed under your system path, and the recommended
   version for Z3 is 4.7.1)

    * On Mac OS: brew install z3 (if you have installed brew)
    * On Ubuntu: sudo apt install z3

6. Unpack the release's "extern" folder to a location on your machine.

## Install the VERDICT plugin for OSATE

   1. Open OSATE; go to "Help -> Install New Software -> Add... -> Archive..." and
     find the "com.ge.research.osate.verdict.feature.zip" file in
     the "extern" folder
   2. Make sure uncheck the "Group items by category" to be able to see
     the plugin. Finish the installation
   3. Set VERDICT bundle path: Go to "OSATE -> Preferences -> Verdict
     -> Verdict Bundle" and find the "verdict-bundle-1.0.jar" file in
     the "extern" folder
   4. In the same place, also set the STEM project, aadl2iml, Kind 2 and 
     Soteria++ and GraphViz paths for your OS respectively, and then save and close the dialog
