# VERDICT: Calling the VERDICT back-end programs

## About VERDICT

[OSATE](https://osate.org/about-osate.html) is an Open Source AADL
Tool Environment based on the Eclipse Modeling Tools IDE.  The VERDICT
tools consist of an OSATE plugin and a set of VERDICT back-end
programs invoked by the plugin.  The OSATE plugin sources are in
another directory [(../../verdict)](../../verdict) and some of the
back-end program sources (those that are written in Java) are in
subdirectories under this directory.  Some other back-end program
sources (those that are not written in Java) are in other directories
under our [parent](../) directory.

This directory builds an executable jar called
verdict-bundle-1.0-SNAPSHOT-capsule.jar which runs some back-end
programs (those not written in Java) in subprocesses and directly
calls some other back-end programs (those written in Java).  You can
run our verdict-bundle jar and other back-end programs directly on
your system or run them in a Docker container.  If you want to build a
Docker image which contains all of our back-end programs, you need to
build it in our [parent](../) directory, not this directory which
builds only our verdict-bundle jar bundling all of our back-end
programs that are written in Java.

## Set up your build environment

You will need a [Java Development Kit](https://adoptopenjdk.net/)
(version 8) to compile all of our Java program sources.  Later
versions than Java 8 LTS may work, but we have not tested them.  We
are skipping Java 11 LTS and staying with Java 8 LTS until Java 17 LTS
comes out.  To use a later LTS version of Java, replace the
maven.compiler.source and maven.compiler.target properties in our
tools' parent [pom.xml](../../pom.xml) with maven.compiler.release and
set the release number to 11 or 17, e.g.,

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

## Note the use of our Maven snapshot repository

One of our back-end programs (verdict-stem-runner) uses some SADL
libraries (reasoner-api, reasoner-impl, sadlserver-api, and
sadlserver-impl) which are part of the Semantic Application Design
Language version 3 ([SADL](http://sadl.sourceforge.net/)).  GE Global
Research has open sourced SADL but these SADL libraries have not been
officially released and put into the Maven central repository since
SADL still is used mostly by other GE Global Research software such as
the SADL Integrated Development Environment (SADL IDE).  Since the
Maven central repository normally distributes only releases of
libraries, not snapshots, we needed to set up our own Maven snapshot
repository to make these SADL libraries available when we build
verdict-stem-runner.

The good news is that you will not need to clone SADL from its own git
[repository](https://github.com/crapo/sadlos2) and build SADL before
you can build our program sources.  We have already built the SADL
libraries and put them into another git repository
([sadl-snapshot-repository](https://github.com/ge-high-assurance/sadl-snapshot-repository)).
We have a repositories section inside verdict-stem-runner's pom.xml
which tells Maven how to download these SADL libraries from the above
git repository.

## Build the verdict-bundle programs

To build the Java program sources on your system, simply run a Maven
command like the following in this directory:

`mvn clean install`

If the build completes successfully, you will have an executable jar
called verdict-bundle-1.0-SNAPSHOT-capsule.jar in the
verdict-bundle/target directory.  The OSATE plugin can run this
verdict-bundle jar directly on your system although you also would
need to have the other back-end programs that the jar calls in
subprocesses.  You also can add this verdict-bundle jar to a Docker
image containing the other back-end programs, allowing the OSATE
plugin to run all of the back-end programs in a Docker container.  You
would have to build that Docker image in our parent directory since it
will need other programs' sources inside other directories besides
this directory.
