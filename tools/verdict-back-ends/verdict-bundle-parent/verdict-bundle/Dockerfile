# Build z3 from source since buster's version of z3 is too old

FROM debian:buster-slim AS builder
RUN apt-get update -qq \
 && DEBIAN_FRONTEND=noninteractive apt-get -y install --no-install-recommends \
    binutils \
    ca-certificates \
    curl \
    g++ \
    make \
    python3 \
    python3-distutils

RUN cd /tmp \
 && curl --fail --location --silent --show-error https://github.com/Z3Prover/z3/archive/z3-4.7.1.tar.gz | tar xzf - \
 && cd z3-z3-4.7.1 \
 && python3 scripts/mk_make.py --prefix=/usr/local \
 && cd build \
 && make \
 && make install

# Pick a base image that is as small as possible, keeping in mind our
# verdict binaries will run only on Debian, not Alpine (otherwise
# openjdk:8-jre-alpine would be an even smaller base image)

FROM openjdk:8-jre-slim-buster

# Install graphviz and a lib needed by z3 from Debian repositories

RUN apt-get update -qq \
 && DEBIAN_FRONTEND=noninteractive apt-get -y install --no-install-recommends \
    graphviz \
    libgomp1 \
 && rm -rf /var/lib/apt/lists/*

# Copy z3 from builder image since Debian package is too old

COPY --from=builder /usr/local /usr/local

# Run verdict as an ordinary user, not as root

RUN adduser --disabled-password --gecos VERDICT verdict
USER verdict:verdict
WORKDIR /data
ENTRYPOINT ["java", "-jar", "/app/verdict.jar"]
# Note these --add-open arguments would be needed for Java 11 LTS or Java 17 LTS
#ENTRYPOINT ["java", "--add-opens", "java.management/com.sun.jmx.mbeanserver=ALL-UNNAMED", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-jar", "/app/verdict.jar"]

# Copy our verdict binaries into the image

ARG AADL2IML_BIN=target/classes/nix/aadl2iml
ARG KIND2_BIN=target/classes/nix/kind2
ARG SOTERIA_PP_BIN=target/classes/nix/soteria_pp
ARG VERDICT_JAR=target/verdict-bundle-1.0-SNAPSHOT-capsule.jar
ENV GraphVizPath=/usr/bin

COPY $AADL2IML_BIN /app/aadl2iml
COPY $KIND2_BIN /app/kind2
COPY $SOTERIA_PP_BIN /app/soteria_pp
COPY $VERDICT_JAR /app/verdict.jar
