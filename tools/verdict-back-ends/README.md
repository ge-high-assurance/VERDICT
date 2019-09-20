# VERDICT: Building the back-end tools

## About VERDICT

The VERDICT tool consists of an OSATE plugin and a set of VERDICT
back-end tools invoked by the plugin.  The OSATE plugin sources are in
another directory [(../verdict)](../verdict) and the back-end tool
sources are in this directory.

## Set up your development environment

You will need Java and Maven to build the back-end tools since most of
the back-end tool sources are written in Java.  One exception is
aad2iml, which is written in OCaml; see its own [README](aad2iml) for
instructions how to build it (briefly, install OCaml, run make, rename
the newly built ./main.native executable to aad2iml, and put aad2iml
someplace where VERDICT can find it).

We have been building the tools with [Java
8](https://www.oracle.com/technetwork/java/javase/overview/java8-2100321.html)
to date.  We have not tried to build them with newer releases of Java.
Newer Java versions probably will work, but you would have to change
project.target.javaVersion from 1.8 to that newer version number in
this directory's [pom.xml](pom.xml).  Your operating system most
likely has a prebuilt [OpenJDK](http://openjdk.java.net/install/)
package which you can install.

We also have been building the tools with
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

## Build some dependencies (SADL) first

One of the back-end tools (verdict-stem-runner) uses some jars
(reasoner-api, reasoner-impl, sadlserver-api, and sadlserver-impl)
which Maven will not be able to find in any Maven repository.  You
will have to check out the source code for these jars from an open
source GitHub [repository](https://github.com/crapo/sadlos2) and build
these jars locally before you can build verdict-stem-runner.  These
jars are a part of the Semantic Application Design Language version 3
([SADL](http://sadl.sourceforge.net/)), which has been open sourced by
GE Global Research but used primarily only by some GE software
(including verdict-stem-runner).

Build the necessary SADL jars with these commands which clone the SADL
source tree from GitHub, check out the AugmentedTypes branch which
currently is the primary active branch, and change directories to the
right location in the source tree:

```shell
$ mdir ~/git
$ pushd ~/git
$ git clone https://github.com/crapo/sadlos2.git
$ cd sadlos2
$ git checkout AugmentedTypes
$ cd sadl3/com.ge.research.parent
$ mvn install
```

The Maven build usually takes at least ten minutes, but could take
much longer since Maven probably will have to download many
dependencies first.

## Build our back-end tools

Once you've built the SADL jars, you should be able to return to this
directory and build our back-end tools by running mvn install:

```shell
$ popd
$ mvn install
```

## Remaining steps

There are more
[steps](../verdict/com.ge.research.osate.verdict/README.md) you will
have to follow to make the back-end tools ready for use by our OSATE
plugin.  These steps primarily put the tools someplace and tell the
plugin where to find the tools.
