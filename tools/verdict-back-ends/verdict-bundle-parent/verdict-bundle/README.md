# VERDICT: Building the VERDICT back-end tools

## About VERDICT

The VERDICT tool consists of an OSATE plugin and a set of VERDICT
back-end tools invoked by the plugin.  This module bundles these
VERDICT back-end tools together into a jar which can be run directly
or a Docker image which can be run as a command.

## Set up your build environment

We are in a transition period.  We have been building aadl2iml, kind2,
and soteria_pp binaries for OS X and Ubuntu and putting them under
source control in the src/main/resources/{mac,nix} directories so that
they can be copied into the jar or copied along with the jar.  Now we
are starting to add the back-end tools to a container image but we
have not started using container images to build all of the back-end
tools yet.  Eventually Maven, Java, and Docker (or Jib, perhaps) may
be the only build environment you will need, but for now you still
will need to set up OCaml and C to build aadl2iml, kind2, and
soteria_pp as well.

If you have Ubuntu 18.04 LTS available (even if you have to run it in
WSL, or Windows Subsystem for Linux), the following commands will set
up OCaml in your build environment for both aadl2iml and soteria_pp:

```shell
$ sudo add-apt-repository ppa:avsm/ppa
$ sudo apt update
$ sudo apt install opam
$ opam init --disable-sandboxing  # Need --disable-sandboxing only on WSL
$ eval $(opam env)
$ opam switch create ocaml 4.07.1
$ eval $(opam env)
$ opam install async core core_extended ocamlfind printbox
$ opam install ocamlbuild
$ opam install menhir
```

Once you have set up OCaml, you can run `make` in the aadl2iml and
soteria_pp directories to build these back-end tools and copy their
binaries here to be added to the container image.  To build the kind2
binary, you will need to check out a special branch from a separate
git repository,
[https://github.com/daniel-larraz/kind2/tree/verdict_blame_assign](https://github.com/daniel-larraz/kind2/tree/verdict_blame_assign),
and follow its own build instructions before you can copy the kind2
binary here as well.  However, you can continue using the kind2 binary
from src/main/resources/nix/kind2 in the container image if you want
since it's probably still up to date.

Hopefully we will start building aadl2iml, kind2, and soteria_pp using
container images so you won't have to set up your build environment to
build them yourself.

# Build the verdict image with Docker

First, you need to build this module and all of its sibling modules
with `mvn clean install` in the verdict-bundle-parent directory above
this directory.  Then cd into this directory and run the following
command to build the verdict image with Docker (change or remove the
--build-arg http_proxy= arguments if you have a different HTTP proxy
or don't need a HTTP proxy):

```shell
$ docker build --build-arg http_proxy=http://PITC-Zscaler-US-Niskayuna.proxy.corporate.ge.com:8080/ --build-arg https_proxy=http://PITC-Zscaler-US-Niskayuna.proxy.corporate.ge.com:8080/ -t verdict .
```

If the build succeeds, you will have a verdict image and be able to
run the VERDICT back-end tools in a container now:

```shell
$ docker image ls
REPOSITORY          TAG                 IMAGE ID            CREATED             SIZE
verdict             latest              c95f08a73c67        2 minutes ago       357MB
openjdk             8-jre-slim-buster   0fb3e301b161        6 days ago          184MB
debian              buster-slim         e1af56d072b8        4 weeks ago         69.2MB
```

# Run the verdict image as a command

Now you will be able to run our CRV and MBAS tools (which call
aadl2iml, kind2, and/or soteria_pp in turn) in a container with
commands like these:

### CRV

```shell
$ docker run --mount type=bind,src=C:/Users/200003548/git/VERDICT,dst=/data verdict --aadl /data/models/DeliveryDrone /app/aadl2iml --crv /data/crv_output.xml /app/kind2 -BA -LS -NI -LB -IT -OT -RI -SV
```

You will see two output files (crv_output.xml and crv_output_ba.xml)
appear in your host's directory (the src=VERDICT directory above).

### MBAS

```shell
$ docker run --mount type=bind,src=C:/Users/200003548/git/VERDICT,dst=/data verdict --aadl /data/models/DeliveryDrone /app/aadl2iml --mbas /data/tools/verdict-back-ends/verdict-bundle-parent/verdict-stem-runner/target/test-classes/STEM /app/soteria_pp
```

You will see some CSV and SVG files appear in your host's STEM/Graphs,
STEM/Output, and STEM/Output/Soteria_Output directories (inside the
src=VERDICT directory above).
