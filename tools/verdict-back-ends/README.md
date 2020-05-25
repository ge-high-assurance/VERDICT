# VERDICT: Building the VERDICT back-end programs

## About the VERDICT back-end programs

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

You will need a [Java Development Kit](https://adoptopenjdk.net/)
(version 8 or 11) to build all of our Java program sources.  We have
tried Java 11 LTS successfully, but OSATE itself is officially
supported only on Java 8 LTS so we recommend using Java 8 LTS anyway.
If you want to switch to a later LTS version of Java, replace the
maven.compiler.source and maven.compiler.target properties in our
tools' parent [pom.xml](../../pom.xml) with maven.compiler.release and
set the release number to 11, e.g.,

```
    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <maven.compiler.release>11</maven.compiler.release>
    </properties>
```

You also will need [Apache Maven](https://maven.apache.org) to build
all of our Java program sources.  Your operating system may have a
prebuilt Maven package available, but many developers would prefer to
download the latest Maven release from Apache's website, unpack the
Maven release someplace, and
[add](https://maven.apache.org/install.html) the unpacked directory's
bin directory to their PATH.

Some developers also will need to tell Maven to [use a
proxy](https://maven.apache.org/guides/mini/guide-proxies.html) in
their settings.xml file (usually ${user.home}/.m2/settings.xml).
Maven is unaffected by proxy environment variables, so you still need
to create your own settings.xml file if Maven needs to use a proxy at
your site.

You can use our Dockerfile to build all of the back-end programs not
written in Java so you can avoid needing to install C/C++ and OCaml
compilers on your system.  If you really want to build these programs
directly on your system with your system's native compilers, we will
describe briefly how to set up these native compilers on your system.
Our [Dockerfile](Dockerfile) also shows how to install or set up these
native compilers within a Debian 10-based OCaml builder image.

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
$ opam install num ocamlbuild ocamlfind printbox xml-light yojson
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

The verdict-bundle sources are written in Java.  You will find some
important notes about building these sources in verdict-bundle's
[README.md](verdict-bundle-parent/README.md).  In short, you will need
to set an environment variable `GraphVizPath` to the directory where
Graphviz's `dot` executable can be found (usually `/usr/bin` on Linux)
to make a unit test pass.  You also must allow Maven to download jars
from our sadl-snapshot-repository.

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
sources not written in Java.  If Docker is not installed on your
operating system, please see our parent [README.md](../README.md) for
instructions how to install Docker.  You also will need to download
two Docker images before you can build our own Docker image:

```shell
$ docker pull ocaml/opam2:latest
$ docker pull openjdk:8-jre-slim-buster
```

You need to run the above commands only when you haven't downloaded a
Docker image yet or you want to download a later version which you
know is available now.  You also need a fresh build of the Java
program sources on your system as well.  To get the fresh build, run
Maven in either this directory or the verdict-bundle-parent directory:

`mvn clean install`

Now cd back into this directory and run either of the following
commands to build a Docker image containing all of the back-end
programs:

```shell
: If you don't need a HTTP proxy
$ docker build -t gehighassurance/verdict .
: If you need a HTTP proxy
$ docker build --build-arg http_proxy=http://PITC-Zscaler-US-Niskayuna.proxy.corporate.ge.com:8080/ --build-arg https_proxy=http://PITC-Zscaler-US-Niskayuna.proxy.corporate.ge.com:8080/ -t gehighassurance/verdict .
```

Omit or change the build arguments if you don't need a HTTP proxy or
you use a different HTTP proxy.  If the docker build command finishes
successfully, you will have a gehighassurance/verdict image that will
be able to run all of the VERDICT back-end programs in a container:

```shell
$ docker image ls
REPOSITORY                TAG                 IMAGE ID            CREATED             SIZE
gehighassurance/verdict   latest              8bac939d1ece        25 minutes ago      336MB
ocaml/opam2               latest              f58ef9d0ce70        2 weeks ago         3.56GB
openjdk                   8-jre-slim-buster   bf20b099be53        2 weeks ago         184MB
```

Once you have done sufficient testing to make sure that the latest
version of the Docker image works with the latest version of our OSATE
plugin, then push the image to DockerHub to publish the image and make
its latest version available for everyone else to use:

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
