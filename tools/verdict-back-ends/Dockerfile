# Build aadl2iml, kind2, soteria_pp, and z3 using Docker

FROM openjdk:11-jdk-slim-buster AS z3-builder

# we need java in order to build the z3 java bindings
# it's just easier to build it separately from the ocaml things

USER 0
RUN apt-get update -qq \
 && DEBIAN_FRONTEND=noninteractive apt-get -y install --no-install-recommends \
    make \
    automake \
    libtool \
    pkg-config \
    python3 \
    python3-distutils \
    wget \
    curl \
    g++

# Download z3's sources and build z3

RUN mkdir --mode=777 /src \
 && cd /src \
 && curl --fail --location --silent --show-error https://github.com/Z3Prover/z3/archive/z3-4.7.1.tar.gz | tar xzf - \
 && cd z3-z3-4.7.1 \
 && python3 scripts/mk_make.py --java --prefix=/usr/local \
 && cd build \
 && make \
 && make install

FROM ocaml/opam2 AS ocaml-builder

# Switch to the root user and make sure we have all the Debian packages we need
# These are already installed:
# binutils ca-certificates g++ make unzip curl

USER 0
RUN apt-get update -qq \
 && DEBIAN_FRONTEND=noninteractive apt-get -y install --no-install-recommends \
    automake \
    libtool \
    pkg-config \
    python3 \
    python3-distutils \
    wget

RUN mkdir --mode=777 /src

# Switch to the opam user and make sure we have all the ocaml packages we need

USER opam
RUN opam switch 4.07 \
 && eval $(opam env) \
 && opam update \
 && opam install --yes \
    async \
    camlp4 \
    core \
    core_extended \
    num \
    ocamlbuild \
    ocamlfind \
    printbox \
    xml-light \
    yojson \
 && opam install "menhir<=20200211" \
 && echo '#use "topfind" ;;' >> /home/opam/.ocamlinit \
 && echo '#thread ;;' >> /home/opam/.ocamlinit \
 && echo '#load "stdlib.cma" ;;' >> /home/opam/.ocamlinit \
 && echo '#require "async" ;;' >> /home/opam/.ocamlinit \
 && echo '#require "core_extended" ;;' >> /home/opam/.ocamlinit \
 && echo 'open Core ;;' >> /home/opam/.ocamlinit

# Checkout kind2's sources and build kind2

RUN eval $(opam env) \
 && cd /src \
 && git clone https://github.com/daniel-larraz/kind2.git \
 && cd kind2 \
 && git checkout verdict_blame_assign \
 && ./autogen.sh \
 && ./build.sh

# Copy aadl2iml's sources and build aadl2iml

COPY --chown=opam aadl2iml /src/aadl2iml
RUN eval $(opam env) \
 && cd /src/aadl2iml \
 && make

# Copy soteria_pp's sources and build soteria_pp

COPY --chown=opam soteria_pp /src/soteria_pp
RUN eval $(opam env) \
 && cd /src/soteria_pp \
 && make

# Pick a base image that is as small as possible, keeping in mind our
# verdict binaries will run only on Debian, not Alpine (otherwise
# openjdk:8-jre-alpine would be an even smaller base image)

FROM openjdk:11-jre-slim-buster

# Install graphviz and a lib we need from Debian repositories

RUN apt-get update -qq \
 && DEBIAN_FRONTEND=noninteractive apt-get -y install --no-install-recommends \
    graphviz \
    libgomp1 \
 && rm -rf /var/lib/apt/lists/* \
 && adduser --disabled-password --gecos VERDICT verdict

# Copy the binaries we need to call

COPY --from=ocaml-builder /src/aadl2iml/main.native /app/aadl2iml
COPY --from=ocaml-builder /src/kind2/bin/kind2 /app/kind2
COPY --from=ocaml-builder /src/soteria_pp/soteria_pp.native /app/soteria_pp
COPY verdict-bundle-parent/verdict-bundle/target/verdict-bundle-1.0-SNAPSHOT-capsule.jar /app/verdict.jar
COPY --from=z3-builder /usr/local/bin /usr/local/bin
COPY --from=z3-builder /usr/local/lib /usr/local/lib
COPY --from=ocaml-builder /usr/local /usr/local

# Run verdict.jar as the verdict user, not as root

USER verdict
WORKDIR /data
# Java 11 LTS needs --add-open arguments
ENTRYPOINT ["java", "-Xmx1536m", "--add-opens", "java.management/com.sun.jmx.mbeanserver=ALL-UNNAMED", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-jar", "/app/verdict.jar"]
ENV GraphVizPath=/usr/bin
ENV LD_LIBRARY_PATH=/usr/local/lib
