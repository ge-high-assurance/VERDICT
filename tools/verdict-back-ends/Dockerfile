# Cache metadata in our image to make subsequent builds faster

ARG BUILDKIT_INLINE_CACHE=1

# Base our image on ubuntu:21.04 to be compatible with kind2's image

FROM ubuntu:21.04

# Install packages which are needed by verdict back-end tools
# (kind2 requires libzmq5 and z3)

RUN apt-get update -qq \
 && DEBIAN_FRONTEND=noninteractive apt-get -y install --no-install-recommends \
    openjdk-11-jre-headless \
    graphviz \
    libzmq5 \
    z3 \
 && rm -rf /var/lib/apt/lists/* \
 && adduser --disabled-password --gecos VERDICT verdict

# Copy each verdict back-end tool into our image

COPY --from=kind2/kind2:dev /kind2 /app/kind2
COPY soteria_pp/bin/soteria_pp /app/soteria_pp
COPY verdict-bundle/verdict-bundle-app/target/verdict-bundle-app-*-capsule.jar /app/verdict.jar

# Run verdict.jar as the verdict user, not as root

USER verdict
WORKDIR /data
# Java 11 LTS has better memory heuristics, but needs --add-open arguments
ENTRYPOINT ["java", "-Xmx1536m", "--add-opens", "java.management/com.sun.jmx.mbeanserver=ALL-UNNAMED", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-jar", "/app/verdict.jar"]
ENV GraphVizPath=/usr/bin
