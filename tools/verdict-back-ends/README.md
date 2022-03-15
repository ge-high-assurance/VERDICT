<!-- markdownlint-disable line-length -->

# VERDICT: Building the VERDICT back-end programs

## About the VERDICT back-end programs

[OSATE](https://osate.org/about-osate.html) is an Open Source AADL
Tool Environment based on the Eclipse Modeling Tools IDE.  The VERDICT
tools consist of a VERDICT plugin and a set of VERDICT back-end
programs invoked by the plugin.  Our VERDICT plugin sources are in
the [(../verdict)](../verdict) directory and our back-end program
sources are in the following subdirectories:

- [STEM](STEM) models and queries a model's system architecture
- [soteria\_pp](soteria_pp) analyzes the safety and security of a
  model's system architecture
- [verdict-bundle](verdict-bundle) provides an executable jar which
  can call any of the back-end programs

We also use two prebuilt executables which come from other source
repositories, not this repository:

- [kind2](https://github.com/kind2-mc/kind2) checks the safety
  properties of a Lustre model
- [z3](https://github.com/Z3Prover/z3) solves Satisfiability Modulo
  Theories

Our back-end programs are written in Java and OCaml.  You will need
both Java and OCaml software development kits if you want to build all
of the back-end programs.  However, our GitHub repository has a
continuous integration workflow using GitHub Actions which will build
a Docker image and an Eclipse update site each time you merge a pull
request or push a commit to the main branch.  If you don't want to
edit any of the OCaml code, you can build only the Java programs with
Maven and let the CI workflow produce a Docker image which contains
all of the back-end programs ready to be called when needed.  You will
need to install both OSATE and Docker on your system and configure our
VERDICT plugin to run the back-end programs using the Docker image.

## Set up your build environment

You will need both [Java 11](https://adoptium.net/) and [Apache
Maven](https://maven.apache.org) to build our Java program sources.
You cannot build our plugin source code with Java 8; Java 8 is too old
and no longer supported by Maven's Eclipse Tycho build system.

The traditional way to install Maven is to download the latest Maven
distribution from Apache's website, unpack the Maven distribution
someplace, and [add](https://maven.apache.org/install.html) the
unpacked distribution's bin directory to your PATH.  If you don't want
to install both Java and Maven manually, you can use
[SDKMAN!](https://sdkman.io/) to manage parallel versions of multiple
software development kits on a Unix based system.  SDKMAN! provides a
convenient command line interface for listing, installing, switching
between, and removing multiple versions of JDKs and SDKs.

Some developers also will need to tell Maven to [use a
proxy](https://maven.apache.org/guides/mini/guide-proxies.html) in
their settings.xml file (usually ${user.home}/.m2/settings.xml).
Proxy environment variables won't affect Maven, so you still need to
create your own settings.xml file to tell Maven to use a proxy at
your site.

If you want verdict-stem-runner's unit test to pass (it's disabled by
default right now), you also will need to install the
[GraphViz](https://graphviz.gitlab.io/download/) software on your
system and set the environment variable `GraphVizPath` to the
directory where the `dot` executable can be found (usually `/usr/bin`
on Linux).

## Note the use of our Maven snapshot repository

One of our Java back-end programs (verdict-stem-runner) depends on
libraries which are not available in the Maven central repository.  It
uses some SADL libraries (reasoner-api, reasoner-impl, sadlserver-api,
and sadlserver-impl) which are part of the Semantic Application Design
Language version 3
([SADL](https://github.com/SemanticApplicationDesignLanguage/sadl)).
GE has made SADL open source but has not deployed releases of these
SADL libraries to the Maven central repository.  Since the Maven
central repository distributes only releases of libraries, not
snapshots, we have set up our own Maven snapshot repository to make
these SADL libraries available when we build verdict-stem-runner.

The good news is that you will not need to clone SADL from its own git
[repository](https://github.com/SemanticApplicationDesignLanguage/sadl)
and build SADL before you can build our program sources.  We have
already built the SADL libraries and put them into another git
repository
([sadl-snapshot-repository](https://github.com/ge-high-assurance/sadl-snapshot-repository)).
We have a repositories section inside verdict-stem-runner's pom.xml
which tells Maven how to download these SADL libraries from the above
git repository.

However, you will have a problem if you have put a line saying
`<mirrorOf>*</mirrorOf>` in your .m2/settings.xml file.  Redirecting
all Maven downloads to your chosen mirror will prevent Maven from
being able to download any jars directly from our
sadl-snapshot-repository.  You will have to replace `*` with `central`
in that mirrorOf section before your build will finish successfully.

## Build the Java back-end programs

To build the Java program sources on your system, run the following
commands in this directory (note that you usually need to install
z3-native-libs in your Maven local cache only the first time, not
every time).

```
mvn install --file verdict-bundle/z3-native-libs/pom.xml
mvn clean package
```

When the build completes, you will have an executable jar called
verdict-bundle-app-\<VERSION\>-capsule.jar in the
verdict-bundle/verdict-bundle-app/target directory.  Our VERDICT
plugin can run this verdict-bundle-app jar directly on your system if
you want although you also would need to build or install the OCaml
and prebuilt back-end programs that the jar calls in subprocesses (see
below).

## Install Docker (to run our back-end programs)

Our VERDICT plugin has two ways to call our back-end programs.  Our
plugin can run an executable jar or it can run a Docker image in a
temporary container.  The latter way (running a Docker container) is
easier to use, but you need to install Docker on your operating system
first.  Note that we have pushed our verdict image to Docker Hub in
order to make the image available to anyone running both Docker and
our VERDICT plugin.  You only have to tell our VERDICT plugin which
Docker image you want to use; the plugin will look for the image in
the local Docker cache and then tell Docker to pull the image from
Docker Hub if necessary.

The instructions for installing Docker are operating system specific,
so you will have to read and follow the appropriate instructions for
your operating system:

- [Install Docker on
  MacOS](https://docs.docker.com/docker-for-mac/install/)

- [Install Docker on
  Windows](https://docs.docker.com/docker-for-windows/install/)

- [Install Docker on
  Ubuntu](https://phoenixnap.com/kb/install-docker-on-ubuntu-20-04)

- [Install Docker on other Linux operating
  systems](https://docs.docker.com/install/)

If you are running Docker on Windows, you will have to do the
following things to allow our plugin to communicate with Docker:

1. Set an environment variable called DOCKER_HOST to the value
   "tcp://localhost:2375" to tell our plugin to connect to the daemon
   using the daemon's TCP port instead of the daemon's Unix file
   socket.

2. Go into Settings in Docker Desktop and enable the checkbox
   next to "Expose daemon on tcp://localhost:2375 without TLS". 

3. Click the "Apply & Restart" button at the bottom to restart Docker
   after these changes.

If you are running Docker on macOS, you will also have to 
configure the following:

1. Click on Resources under Settings, click on FILE SHARING under 
   Resources, and add your workspace directory to the list of directories
   which Docker can bind mount into containers.

2. Click the "Apply & Restart" button at the bottom to restart Docker
   after these changes.

If you want to check that Docker is installed and running correctly,
run the following command to verify that it prints a "Hello from
Docker!" message.

`docker run hello-world`

In addition, you can go into Docker's Settings panel and configure
Docker to use more resources if you want the back-end programs to run
more quickly.  Our back-end programs tend to be CPU-bound, not
memory-bound, so increasing the number of CPUs probably will be more
effective than increasing the memory.

## Build the native back-end programs (optional)

The native program sources are written in
[OCaml](https://ocaml.org/learn/description.html).  If you want to
build or debug the native executables yourself, you will need to
install OCaml version 4.09.1, install some opam packages, run `opam
exec make` in the soteria\_pp subdirectory, and configure our VERDICT
plugin to tell it where the native executables are.

Here are some commands that may successfully set up OCaml and build
the native executables if you have an Ubuntu 20.04 LTS system:

```shell
sudo apt install build-essential m4
sudo apt update
sudo apt install opam
opam switch create 4.09.1
cd tools/verdict-back-ends/soteria_pp
opam install --yes . --deps-only
opam exec make
```

If you want to run graphviz, kind2, and z3 without using Docker, you
will need to download or install prebuilt executables on your
operating system too.  Here are some commands that may successfully
install some of these programs if you have an Ubuntu 20.04 LTS system:

```shell
sudo apt install graphviz libzmq5 z3
```

For kind2, though, you'll probably have to download a prebuilt kind2
executable from <https://github.com/kind2-mc/kind2/releases>.

## Build the STEM files (optional)

The STEM sources are written in
[SADL](https://github.com/SemanticApplicationDesignLanguage/sadl), the
Semantic Application Design Language.  SADL is an English-like
language for building semantic models and authoring rules.  You can
edit SADL files and translate them to OWL, the Web Ontology Language
used by semantic reasoners and rule engines, with the SADL Integrated
Development Environment (SADL IDE), an Eclipse plug-in packaged as a
zip file that can be downloaded from SADL's
[Releases](https://github.com/SemanticApplicationDesignLanguage/sadl/releases)
page.  However, you won't need to use your own SADL IDE unless you
intend to change some STEM files since we already have translated the
STEM project's SADL files to OWL files and committed the translated
OWL files as well as the SADL files to our git repository.  SADL also
provides some libraries which help calling programs use the translated
OWL files.  Our verdict-stem-runner program reads translated OWL files
from a STEM project and runs STEM's rules on semantic model data
loaded from CSV data files created by some of our other back-end
programs.

## Build the Docker image (optional)

You usually won't need to build a Docker image since our CI workflow
will do it automatically for you.  If you still want to build a Docker
image yourself, you must build all of the Java and OCaml programs
first.  Then cd back into this directory and run the following
command:

```shell
docker build -t gehighassurance/verdict-dev .
```

Proxy environment variables won't affect Docker either, so if you need
Docker to use a proxy, you will need to go into Docker's Settings,
configure a proxy in the Resources - Proxies tab, and restart Docker.
You also will have to run your `docker build` command with additional
arguments to pass your proxy environment variables to the Dockerfile's
builder image as well:

```shell
docker build --build-arg http_proxy=${http_proxy} --build-arg https_proxy=${https_proxy} -t gehighassurance/verdict-dev .
```

If you prefer to let our CI workflow build a Docker image for you, you
will need to pull that Docker image from Docker Hub to your machine by
running one of the following commands in your terminal depending on
whether you want the latest release image or the most current
development image:

```shell
docker pull gehighassurance/verdict:latest
docker pull gehighassurance/verdict-dev:latest
```

Make sure that you enter the same image name (either
"gehighassurance/verdict" or "gehighassurance/verdict-dev") into our
VERDICT plugin's settings in your OSATE's preferences as well.
