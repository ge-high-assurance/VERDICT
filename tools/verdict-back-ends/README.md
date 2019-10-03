# VERDICT: Building the back-end tools

## About VERDICT

The VERDICT tool consists of an OSATE plugin and a set of VERDICT
back-end tools invoked by the plugin.  The OSATE plugin sources are in
another directory [(../verdict)](../verdict) and the back-end tool
sources are in this directory's [aadl2iml](aadl2iml) and
[verdict-bundle-parent](verdict-bundle-parent) sub-directories.

## Set up your development environment

The aadl2iml source is written in OCaml; see its own
[README](aadl2iml) for instructions how to build it (briefly, install
OCaml, run make, rename the newly built ./main.native executable to
aadl2iml, and put aadl2iml someplace where VERDICT can find it).

The verdict-bundle-parent's sources are written in Java so you will
need Java and Maven to build the tools in verdict-bundle-parent.  We
have been building the tools with [Java
8](https://www.oracle.com/technetwork/java/javase/overview/java8-2100321.html)
to date.  We have not tried to build them with newer releases of Java.
Newer Java versions probably will work, but you would have to change
project.target.javaVersion from 1.8 to that newer version number in
verdict-bundle-parent's [pom.xml](verdict-bundle-parent/pom.xml).
Your operating system most likely has a prebuilt
[OpenJDK](http://openjdk.java.net/install/) package which you can
install.

We have been building verdict-bundle-parent's sources with
[Maven](https://maven.apache.org).  Your operating system may have a
prebuilt Maven package which you can install, but most people probably
will download a Maven archive from Apache's website, unpack the Maven
archive someplace, and add the unpacked directory's bin directory to
their PATH as described [here](https://maven.apache.org/install.html).

Some people also will need to tell Maven to [use a
proxy](https://maven.apache.org/guides/mini/guide-proxies.html) in
their settings.xml file (usually ${user.home}/.m2/settings.xml).
Maven is unaffected by proxy environment variables, so it is still
necessary to edit settings.xml.

## Note the use of a SADL snapshot repository

One of the back-end tools (verdict-stem-runner) uses some SADL
dependencies (reasoner-api, reasoner-impl, sadlserver-api, and
sadlserver-impl) that are part of the Semantic Application Design
Language version 3 ([SADL](http://sadl.sourceforge.net/)).  SADL has
been open sourced by GE Global Research but still is used mostly by
other GE Global Research software.  We need a snapshot version of
these dependencies which Maven will not be able to find in the Maven
central repositories since people normally distribute releases, not
snapshots, of dependencies.  To make your build easier, we have put
these SADL dependencies into a Maven repository inside another GitHub
project
([sadl-snapshot-repository](https://github.com/ge-high-assurance/sadl-snapshot-repository))
to make these dependencies available when you build
verdict-stem-runner.

Therefore, you will not have to check out SADL's source code from its
own GitHub [repository](https://github.com/crapo/sadlos2) and build
SADL before you can build verdict-stem-runner.  The pom.xml for
verdict-stem-runner has a repositories section that will tell Maven
how to download these SADL dependencies from the above SADL snapshot
repository.

## Build our back-end tools

Build our back-end tools by running make or mvn install inside these
sub-directories:

```shell
$ cd aad2iml
$ make
$ cd verdict-bundle-parent
$ mvn install
```

## Do some remaining steps

There are more
[steps](../verdict/com.ge.research.osate.verdict/README.md) you will
have to follow to make the back-end tools ready for use by our OSATE
plugin.  These steps primarily put the tools someplace and tell the
plugin where to find the tools.
