# VERDICT: Calling the VERDICT back-end programs

## About the VERDICT back-end programs

[OSATE](https://osate.org/about-osate.html) is an Open Source AADL
Tool Environment based on the Eclipse Modeling Tools IDE.  The VERDICT
tools consist of an OSATE plugin and a set of VERDICT back-end
programs invoked by the plugin.  Our OSATE plugin sources are in
the [(../verdict)](../verdict) directory and our back-end program
sources are in the following subdirectories:

- [STEM](STEM) models and queries a model's system architecture
- [aadl2iml](aadl2iml) translates a model from AADL to IML
- [iml-verdict-translator](iml-verdict-translator) translates a model from IML to VDM
- [soteria_pp](soteria_pp) analyzes the safety and security of a model's system architecture
- [verdict-assurance-case](verdict-assurance-case) builds an assurance case
- [verdict-attack-defense-collector](verdict-attack-defense-collector) collects some attack defenses
- [verdict-blame-assignment](verdict-blame-assignment) prepares some blame assignments
- [verdict-bundle](verdict-bundle) provides an executable jar which can call any of the back-end programs
- [verdict-crv](verdict-crv) analyzes a model's behavior
- [verdict-instrumentor](verdict-instrumentor) instruments a model for analysis
- [verdict-lustre-translator](verdict-lustre-translator) translates a model from VDM to Lustre
- [verdict-mbas-translator](verdict-mbas-translator) analyzes a model's architecture
- [verdict-merit-assignment](verdict-merit-assignment) prepares merit assignments
- [verdict-stem-runner](verdict-stem-runner) runs STEM queries
- [verdict-synthesis](verdict-synthesis) synthesizes defenses
- [verdict-test-instrumentor](verdict-test-instrumentor) tests an instrumented model
- [z3-native-libs](z3-native-libs) encapsulates z3 native libraries

We also use two prebuilt executables which come from other source
repositories, not this repository:

- [kind2](https://github.com/kind2-mc/kind2) checks the safety properties of a Lustre model
- [z3](https://github.com/Z3Prover/z3) solves Satisfiability Modulo Theories

## Building the VERDICT back-end programs

Our back-end programs are written in Java and OCaml.  You will need
both Java and OCaml software development kits if you want to build all
of the back-end programs.  However, our GitHub repository has a
continuous integration workflow using GitHub Actions which will build
a Docker image and an Eclipse update site each time you merge a pull
request or push a commit to the main branch.  If you don't want to
edit any of the OCaml code, you can build only the Java programs with
Maven and let the CI workflow produce a Docker image which contains
all of the back-end programs ready to be called when needed.  You will
need to install both OSATE and Docker on your system and configure the
OSATE plugin to run the back-end programs using the Docker image.

## Set up your build environment

You will need both a [Java Development Kit](https://adoptopenjdk.net/)
and [Apache Maven](https://maven.apache.org) to build our Java program
sources.  You can use either Java 8 LTS or Java LTS 11 even though
OSATE itself officially supports only Java 8 LTS.  If OSATE switches
to Java 11 LTS and you don't want to support Java 8 LTS anymore, you
can replace the maven.compiler.source and maven.compiler.target
properties in our tools' parent [pom.xml](../../pom.xml) with
maven.compiler.release and set the release number to 11, e.g.,

```
    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <maven.compiler.release>11</maven.compiler.release>
    </properties>
```

The usual way to install Maven is to download the latest Maven
distribution from Apache's website, unpack the Maven distribution
someplace, and [add](https://maven.apache.org/install.html) the
unpacked distribution's bin directory to your PATH.  If you don't want
to install both Java and Maven manually or fiddle with prebuilt system
packages yourself, you can use [SDKMAN!](https://sdkman.io/) to manage
parallel versions of multiple software development kits on a Unix
based system.  SDKMAN! provides a convenient command line interface
for installing, switching, removing, and listing multiple versions of
JDKs and SDKs.

Some developers also will need to tell Maven to [use a
proxy](https://maven.apache.org/guides/mini/guide-proxies.html) in
their settings.xml file (usually ${user.home}/.m2/settings.xml).
Proxy environment variables won't affect Maven, so you still need to
create your own settings.xml file to tell Maven to use a proxy at
your site.

If you want verdict-stem-runner's unit test to pass (right now it's
disabled by default), you also will need to install the
[GraphViz](https://graphviz.gitlab.io/download/) software on your
system and set the environment variable `GraphVizPath` to the
directory where the `dot` executable can be found (usually `/usr/bin`
on Linux).

## Note the use of our Maven snapshot repository

Two of our back-end programs (iml-verdict-translator and
verdict-stem-runner) depend on libraries which are not available in
the Maven central repository.  For example, we use some SADL libraries
(reasoner-api, reasoner-impl, sadlserver-api, and sadlserver-impl)
which are part of the Semantic Application Design Language version 3
([SADL](http://sadl.sourceforge.net/)).  GE Global Research has open
sourced SADL but these SADL libraries have not been officially
released and put into the Maven central repository since SADL still is
used mostly by other GE Global Research software such as the SADL
Integrated Development Environment (SADL IDE).  Since the Maven
central repository normally distributes only releases of libraries,
not snapshots, we have set up our own Maven snapshot repository to
make these SADL libraries available when we build verdict-stem-runner.

The good news is that you will not need to clone SADL from its own git
[repository](https://github.com/crapo/sadlos2) and build SADL before
you can build our program sources.  We have already built the SADL
libraries and put them into another git repository
([sadl-snapshot-repository](https://github.com/ge-high-assurance/sadl-snapshot-repository)).
We have a repositories section inside verdict-stem-runner's pom.xml
which tells Maven how to download these SADL libraries from the above
git repository.

However, some developers have been putting a `<mirrorOf>*</mirrorOf>`
in their .m2/settings.xml file.  Redirecting all Maven downloads to
the same mirror repository will prevent Maven from being able to
download any jars from our sadl-snapshot-repository.  You will have to
remove `*` from that mirrorOf section before your build will finish
successfully.

## Build the Java back-end programs

To build the Java program sources on your system, simply run the
following Maven command in this directory:

`mvn clean install`

When the build completes, you will have an executable jar called
verdict-bundle-\<VERSION\>-capsule.jar in the verdict-bundle/target
directory.  The OSATE plugin can run this verdict-bundle jar directly
on your system although you also would need to build or install the
OCaml and prebuilt back-end programs that the jar calls in
subprocesses.

## Build the non-Java back-end programs (optional)

The aadl2iml and soteria_pp sources are written in
[OCaml](https://ocaml.org/learn/description.html).  If you want to
build or debug the OCaml programs yourself, you will need to install
OCaml version 4.07.1, install some opam packages, run `opam exec make`
in each of these directories, and configure the OSATE plugin to tell
it where the executables are.

Here are some commands that might successfully set up OCaml and build
each program if you have an Ubuntu 20.04 LTS system:

```shell
$ sudo apt install build-essential m4 
$ sudo apt update
$ sudo apt install opam
$ opam switch create ocaml 4.07.1
$ opam install async core core_extended dune menhir ocamlbuild ocamlfind printbox xml-light
$ cd tools/verdict-back-ends/aadl2iml
$ opam exec make
$ cd tools/verdict-back-ends/soteria_pp
$ opam exec make
```

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

If you want to run graphviz, kind2, and z3 without using Docker, you
will need to download or install prebuilt executables on your
operating system too.  Here are some commands that might successfully
set up some of these programs if you have an Ubuntu 20.04 LTS system:

```shell
$ sudo apt install graphviz libzmq5 z3
```

You'll probably have to download and install a prebuilt kind2
executable manually, though:

<https://github.com/kind2-mc/kind2/releases>

## Build the Docker image (optional)

You usually won't need to build a Docker image since our CI workflow
will do it automatically for you.  If you still want to build a Docker
image yourself, you must build all of the Java and OCaml programs
first.  Then cd back into this directory and run the following
command:

```shell
$ docker build -t gehighassurance/verdict-dev .
```

Proxy environment variables won't affect Docker either, so if you need
Docker to use a proxy, you will need to go into Docker's Settings
(General -> Network), configure a proxy there, and restart Docker.
You also will have to run your `docker build` command with some
additional arguments:

```shell
$ docker build --build-arg http_proxy=http://PITC-Zscaler-US-Niskayuna.proxy.corporate.ge.com:8080/ --build-arg https_proxy=http://PITC-Zscaler-US-Niskayuna.proxy.corporate.ge.com:8080/ -t gehighassurance/verdict-dev .
```
