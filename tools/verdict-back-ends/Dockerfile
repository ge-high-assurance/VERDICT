# Build aadl2iml and soteria_pp using Docker

FROM ocaml/opam2:ubuntu-lts AS ocaml-builder

# Switch to the root user and install m4 (required by ocamlfind).

USER 0
RUN mkdir --mode=777 /src \
 && apt-get update -qq \
 && DEBIAN_FRONTEND=noninteractive apt-get -y install --no-install-recommends \
    m4

# Switch to the opam user and make sure we have all the ocaml packages we need

USER opam
RUN opam switch 4.07 \
 && eval $(opam env) \
 && opam update \
 && opam install --yes \
    async \
    core \
    core_extended \
    dune \
    menhir \
    ocamlbuild \
    ocamlfind \
    printbox \
    xml-light \
 && echo '#use "topfind" ;;' >> /home/opam/.ocamlinit \
 && echo '#thread ;;' >> /home/opam/.ocamlinit \
 && echo '#load "stdlib.cma" ;;' >> /home/opam/.ocamlinit \
 && echo '#require "async" ;;' >> /home/opam/.ocamlinit \
 && echo '#require "core_extended" ;;' >> /home/opam/.ocamlinit \
 && echo 'open Core ;;' >> /home/opam/.ocamlinit

# Copy aadl2iml's sources and build aadl2iml

COPY --chown=opam aadl2iml /src/aadl2iml
RUN eval $(opam env) \
 && cd /src/aadl2iml \
 && make

# Copy soteria_pp's sources and build soteria_pp

COPY --chown=opam soteria_pp /src/soteria_pp
RUN eval $(opam env) \
 && cd /src/soteria_pp \
 && rm -f soteria_pp.native \
 && make

# Pick ubuntu as our base image for compatibility

FROM ubuntu:20.04

# Install packages we need to run verdict back-end tools
# libzmq5 is required by Kind 2

RUN apt-get update -qq \
 && DEBIAN_FRONTEND=noninteractive apt-get -y install --no-install-recommends \
    default-jre \
    graphviz \
    libzmq5 \
    z3 \
 && rm -rf /var/lib/apt/lists/* \
 && adduser --disabled-password --gecos VERDICT verdict

# Copy the verdict back-end tools we need to call

COPY --from=kind2/kind2:dev /kind2 /app/kind2
COPY --from=ocaml-builder /src/aadl2iml/bin/aadl2iml /app/aadl2iml
COPY --from=ocaml-builder /src/soteria_pp/_build/soteria_pp.native /app/soteria_pp
COPY verdict-bundle-parent/verdict-bundle/target/verdict-bundle-*-capsule.jar /app/verdict.jar

# Run verdict.jar as the verdict user, not as root

USER verdict
WORKDIR /data
# Java 11 LTS needs --add-open arguments
ENTRYPOINT ["java", "-Xmx1536m", "--add-opens", "java.management/com.sun.jmx.mbeanserver=ALL-UNNAMED", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-jar", "/app/verdict.jar"]
ENV GraphVizPath=/usr/bin
