# VERDICT: Calling the VERDICT back-end programs

## About the VERDICT executable jar

[OSATE](https://osate.org/about-osate.html) is an Open Source AADL
Tool Environment based on the Eclipse Modeling Tools IDE.  The VERDICT
tools consist of an OSATE plugin and a set of VERDICT back-end
programs invoked by the plugin.  The OSATE plugin sources are in
another directory [(../../verdict)](../../verdict) and some of the
back-end program sources (those that are written in Java) are in
subdirectories under this directory.  Some other back-end program
sources (those that are not written in Java) are in other directories
under our [parent](../) directory.

Running Maven in this directory builds an executable jar called
verdict-bundle-\<VERSION\>-capsule.jar which runs some back-end
programs (those not written in Java) in subprocesses and directly
calls some other back-end programs (those written in Java).  You can
run our verdict-bundle jar and other back-end programs directly on
your system or run them in a Docker container.  If you want to build a
Docker image which contains all of our back-end programs, you need to
build it in our [parent](../) directory, not this directory which
builds only our verdict-bundle jar containing all of our back-end
programs that are written in Java.

## Set up your build environment

If you have not done it yet, you will need to install the Java
Development Kit (version 8) and Apache Maven (latest version).

If you want verdict-stem-runner's unit test to pass, you also will
need to install the [GraphViz](https://graphviz.gitlab.io/download/)
software on your system and set the environment variable
`GraphVizPath` to the directory where the `dot` executable can be
found (usually `/usr/bin` on Linux).

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
remove that mirrorOf section before your build will finish
successfully.

## Build the verdict-bundle programs

To build the Java program sources on your system, simply run a Maven
command like the following in this directory:

`mvn clean install`

If the build completes successfully, you will have an executable jar
called verdict-bundle-\<VERSION\>-capsule.jar in the
verdict-bundle/target directory.  The OSATE plugin can run this
verdict-bundle jar directly on your system although you also would
need to have the other back-end programs that the jar calls in
subprocesses.  You also can add this verdict-bundle jar to a Docker
image containing the other back-end programs, allowing the OSATE
plugin to run all of the back-end programs in a Docker container.  You
would have to build that Docker image in our parent directory since it
will need other programs' sources inside other directories besides
this directory.
