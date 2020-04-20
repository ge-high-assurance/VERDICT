# VERDICT: Building the VERDICT back-end programs

## About VERDICT

[OSATE](https://osate.org/about-osate.html) is an Open Source AADL
Tool Environment based on the Eclipse Modeling Tools IDE.  The VERDICT
tools consist of an OSATE plugin and a set of VERDICT back-end
programs invoked by the plugin.  The OSATE plugin sources are in
another directory [(../verdict)](../verdict) and the back-end program
sources are in these directories:

- [aadl2iml](aadl2iml) translates a model from AADL to IML
- [soteria_pp](soteria_pp) analyzes the safety and security of a model's system architecture
- [STEM](STEM) models and queries a model's system architecture
- [verdict-bundle-parent](verdict-bundle-parent) runs the back-end programs

Not all of our back-end programs have sources inside this directory.
We also use programs called kind2 and z3 which come from different
source repositories or source tarballs than this repository.  If you
want to build all of these back-end programs natively from source, you
will need C/C++, Java, and OCaml compilers.  However, we normally
build only the Java programs directly on our system; we use a
Dockerfile in this directory with a builder image to build the rest of
the back-end programs.  If you want to build our Docker image, you
have come to the right place; you can use this directory's Dockerfile
to build a Docker image which contains all of our back-end programs.

The majority of our back-end program sources are written in Java; they
are all in the [verdict-bundle-parent](verdict-bundle-parent)
directory and get bundled together into an executable jar called
verdict-bundle-1.0-SNAPSHOT-capsule.jar.  The rest of our back-end
program sources (those that are not written in Java) are in the other
directories mentioned above or in other source repositories.  Our
verdict-bundle jar (the back-end programs' sole point of entry)
directly calls the back-end programs written in Java and runs the
back-end programs not written in Java in subprocesses.

## Set up your build environment

If you have not done it yet, you will need to install the Java
Development Kit (version 8), Apache Maven (latest version), and Docker
(latest version).  You can use our Dockerfile to build all of the
back-end programs not written in Java so you can avoid needing to
install C/C++ and OCaml compilers on your system.  If you really want
to build these programs directly on your system with your system's
native compilers, we will describe briefly how to set up these native
compilers on your system although you may find more instructions in
some other directories' README.md files so please look at all of the
other README.md files too.  Our [Dockerfile](Dockerfile) also shows
how to install or set up these native compilers within a Debian
10-based OCaml builder image.

The aadl2iml and soteria_pp sources are written in
[OCaml](https://ocaml.org/learn/description.html).  If you want to
build and/or debug these programs without using Docker, you will need
to install OCaml version 4.07, install some opam packages, run make,
copy/rename the newly built native executables some place, and tell
the verdict-bundle jar or the VERDICT plugin where the executables
are.  You will have to find out how to do that for your operating
system, although you can use the following commands if you have an
Ubuntu 18.04 LTS system:

```shell
$ sudo apt install graphviz libgomp1
$ sudo add-apt-repository ppa:avsm/ppa
$ sudo apt update
$ sudo apt install opam
$ opam init --disable-sandboxing  # Need --disable-sandboxing only on WSL
$ eval $(opam env)
$ opam switch create ocaml 4.07.1
$ eval $(opam env)
$ opam install async camlp4 core core_extended menhir
$ opam install num ocamlbuild ocamlfind printbox yojson
$ echo '#use "topfind" ;;' >> $HOME/.ocamlinit
$ echo '#thread ;;' >> $HOME/.ocamlinit
$ echo '#load "stdlib.cma" ;;' >> $HOME/.ocamlinit
$ echo '#require "async" ;;' >> $HOME/.ocamlinit
$ echo '#require "core_extended" ;;' >> $HOME/.ocamlinit
$ echo 'open Core ;;' >> $HOME/.ocamlinit
```

The kind2 sources are written in OCaml and live in a separate git
repository.  If you want to build kind2 without using Docker, you will
have to install OCaml as above and clone the kind2 sources from
[https://github.com/daniel-larraz/kind2/tree/verdict_blame_assign](https://github.com/daniel-larraz/kind2/tree/verdict_blame_assign).
You will have to check out the verdict_blame_assign branch and follow
the build instructions in kind2's README.md.

The STEM sources are written in [SADL](http://sadl.sourceforge.net/),
the Semantic Application Design Language.  SADL is an English-like
language for building semantic models and authoring rules.  You can
edit SADL files and translate them to OWL, the Web Ontology Language
used by semantic reasoners and rule engines, with the SADL Integrated
Development Environment (SADL IDE), an Eclipse plug-in packaged as a
zip file that can be downloaded from the SADL
[Releases](https://github.com/crapo/sadlos2/releases) page.  However,
you won't need to set up your own SADL IDE unless you intend to change
some STEM files since we already have translated the SADL files to OWL
files and committed the translated OWL files as well as the SADL files
to our git repository.  SADL also provides some libraries which help
calling programs use the translated OWL files.  Our verdict-bundle jar
reads translated OWL files from a STEM project and runs STEM's rules
on semantic model data loaded from CSV data files created by some of
our other back-end programs.

The verdict-bundle sources are written in Java.  If you need them, you
will find more detailed instructions for installing Java and Maven in
verdict-bundle's [README.md](verdict-bundle-parent/README.md).

The z3 sources are written in C++ and live in a separate git
repository.  Unfortunately, kind2 will not work with the latest
version of z3; if you want to run kind2 without using Docker, you will
need to download version 4.7.1 of the z3 [source
tarball](https://github.com/Z3Prover/z3/archive/z3-4.7.1.tar.gz) and
follow the build instructions in z3's
[README.md](https://github.com/Z3Prover/z3/tree/z3-4.7.1) unless your
operating system already has a z3-4.7.1 package.

## Build the back-end programs with Java and Docker

As we have said before, you normally will build only the Java program
sources on your system and use Docker to build the rest of the program
sources not written in Java.  To do so, simply run Maven in either
this directory or the verdict-bundle-parent directory:

`mvn clean install`

Now cd back into this directory and run the following command to build
a Docker image containing all of the back-end programs (change or
remove the --build-arg http_proxy= arguments if you have a different
HTTP proxy or don't need a HTTP proxy):

```shell
$ docker build --build-arg http_proxy=http://PITC-Zscaler-US-Niskayuna.proxy.corporate.ge.com:8080/ --build-arg https_proxy=http://PITC-Zscaler-US-Niskayuna.proxy.corporate.ge.com:8080/ -t gehighassurance/verdict .
```

If the above command runs successfully, you will have a
gehighassurance/verdict image that will be able to run all of the
VERDICT back-end programs in a container:

```shell
$ docker image ls
REPOSITORY                   TAG                 IMAGE ID            CREATED             SIZE
gehighassurance/verdict      latest              c95f08a73c67        2 minutes ago       357MB
```

Once you have done sufficient testing to make sure that the latest
version of the Docker image works with the latest version of our OSATE
plugin, then you would push the image to DockerHub to publish the
image and make its newest version available for everyone to use:

```shell
$ docker push gehighassurance/verdict
```

# Run the VERDICT back-end programs in a container

Once you have built and pushed the gehighassurance/verdict image, our
OSATE plugin will run the back-end programs in a container
automatically for you.  However, you can run the VERDICT back-end
programs with commands like these if you want to perform some manual
testing or debugging yourself:

### CRV

```shell
$ docker run --mount type=bind,src=C:/Users/200003548/git/VERDICT,dst=/data verdict --aadl /data/models/DeliveryDrone /app/aadl2iml --crv /data/crv_output.xml /app/kind2 -BA -LS -NI -LB -IT -OT -RI -SV
```

You will see two output files (crv_output.xml and crv_output_ba.xml)
appear in your host's directory (the src=VERDICT directory above).

### MBAS

```shell
$ docker run --mount type=bind,src=C:/Users/200003548/git/VERDICT,dst=/data verdict --csv DeliveryDrone /app/aadl2iml --mbas /data/tools/verdict-back-ends/STEM /app/soteria_pp
```

You will see some CSV and SVG files appear in your host's STEM/Graphs,
STEM/Output, and STEM/Output/Soteria_Output directories (inside the
src=VERDICT directory above).
