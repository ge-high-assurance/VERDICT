<!-- markdownlint-disable line-length -->

# VERDICT: Building the VERDICT plugin

## About the VERDICT plugin

[OSATE](https://osate.org/about-osate.html) is an Open Source AADL
Tool Environment based on the Eclipse Modeling Tools IDE.  The VERDICT
tools consist of a VERDICT plugin for OSATE and a set of VERDICT
back-end programs invoked by the plugin to perform cyber resiliency
analysis (CRV) and model based architecture analysis (MBAA).  The
VERDICT plugin sources are in this directory and the back-end program
sources are in another directory
[(../verdict-back-ends)](../verdict-back-ends).

Running Maven in this directory builds an update site (a directory,
not a website) containing our VERDICT plugin.  You can install the
directory
`com.ge.research.osate.verdict.updatesite/target/repository/` directly
into your OSATE to test your changes.  Once your changes are merged to
our repository's main branch, our GitHub Actions CI workflow will copy
that directory to a new directory in our
[VERDICT-update-sites](https://github.com/ge-high-assurance/VERDICT-update-sites)
repository, allowing others and you to install the latest development
plugin version via an update site URL.

## Set up your build environment

You will need [Java 17](https://adoptium.net/) to build all of our
Java program sources.  Maven's Eclipse Tycho build system no longer
supports Java 11 so Java 17 is required to build VERDICT.

You also will need [Apache Maven](https://maven.apache.org) to build
all of our Java program sources.  Your operating system may have
prebuilt Java and Maven packages available or you can install both
Java and Maven using [SDKMAN!](https://sdkman.io/) if you prefer.
SDKMAN! provides a convenient command line interface for listing,
installing, switching between, and removing multiple versions of JDKs
and SDKs.

Some developers also will need to tell Maven to [use a
proxy](https://maven.apache.org/guides/mini/guide-proxies.html) in
their settings.xml file (usually ${user.home}/.m2/settings.xml).
Maven is unaffected by proxy environment variables, so you still need
to create your own settings.xml file if Maven needs to use a proxy at
your site.

## Update our target definition file (if needed)

Our VERDICT plugin's target definition file tells Maven and Eclipse
Tycho where to find the OSATE, AGREE, and Eclipse APIs that our
VERDICT plugin needs to call.  Ideally, we do not want to have to
change the versions of the OSATE, AGREE, and Eclipse update sites in
our target definition file every time a stable OSATE release comes
out.  We use "0.0.0" for API version numbers in our target definition
file because we want our VERDICT plugin to work with any other OSATE,
AGREE, and Eclipse update sites besides the update sites put in our
file.

In practice, each AGREE version declares it is compatible with only
one specific OSATE release even though the OSATE and Eclipse APIs tend
to remain stable across multiple OSATE releases.  Therefore, the AGREE
version we bundle with VERDICT in our update site works only in OSATE
2.10.2 although you can install VERDICT in other OSATE versions by
installing a matching AGREE version in that OSATE version before you
install VERDICT from our update site.

If you do need to change our target
[definition](com.ge.research.osate.verdict.targetplatform/com.ge.research.osate.verdict.targetplatform.target)
file to use different versions of OSATE's and AGREE's update sites,
please go ahead and edit the file.  Note that you will have to bump
certain update sites' versions in lockstep with other update sites'
versions because only certain versions of OSATE, AGREE, and other
Eclipse update site versions will work together.  To guide you which
versions of which update sites are compatible with each other, look at
OSATE's own target
[definition](https://github.com/osate/osate2/blob/2.10.2/core/org.osate.build.target/osate2-platform.target)
file in its source repository.  That file is for OSATE 2.10.2; once
you find a more recent version of OSATE's target definition file, make
sure you use the same versions of that OSATE's update sites in
VERDICT's target definition file.

## Build our VERDICT plugin sources

This directory and its subdirectories have pom.xml files which will
allow you to build our VERDICT plugin sources with Maven and Eclipse
Tycho.  We recommend that you build our VERDICT plugin sources with
Maven from the command line in this directory or the directory above,
making sure to call the clean and package goals separately (not in the
same Maven command line):

```shell
mvn clean
mvn package -Dtycho.localArtifacts=ignore
```

Using the `-Dtycho.localArtifacts=ignore` argument may prevent some
build problems from happening.  Please change or leave out the
`-Dtycho.localArtifacts=ignore` argument only if you are familiar with
Tycho builds and want Tycho to use local artifacts that you have
installed into your Maven cache (maybe because you want to run Maven
in a module's subdirectory instead of this directory, in which case
you also need to first run "mvn install" instead of "mvn package" in
this directory as well).

If Maven encounters a problem and you need to see more information to
diagnose what caused the problem, rerun the command line with an
additional `-X` argument at the end to make Maven print very detailed
debugging output.

If the Maven build completes successfully, you will be able to install
our VERDICT plugin in an OSATE release as shown in the next section.
A successful build ends with the following output:

```text
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for verdict 1.0.0-SNAPSHOT:
[INFO]
[INFO] verdict ............................................ SUCCESS [  0.354 s]
[INFO] com.ge.research.osate.verdict.dsl .................. SUCCESS [ 31.264 s]
[INFO] com.ge.research.osate.verdict.dsl.ide .............. SUCCESS [  0.069 s]
[INFO] com.ge.research.osate.verdict.dsl.ui ............... SUCCESS [  0.804 s]
[INFO] com.ge.research.osate.verdict ...................... SUCCESS [  1.257 s]
[INFO] com.ge.research.osate.verdict.feature .............. SUCCESS [  0.079 s]
[INFO] com.ge.research.osate.verdict.targetplatform ....... SUCCESS [  0.011 s]
[INFO] com.ge.research.osate.verdict.updatesite ........... SUCCESS [  5.863 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  49.735 s
[INFO] Finished at: 2021-08-17T17:00:55-04:00
[INFO] ------------------------------------------------------------------------
```

The last module built by Maven is an update site for our VERDICT
plugin which can be used to install VERDICT into a stable OSATE
release.  That update site is actually located inside the directory
`com.ge.research.osate.verdict.updatesite/target/repository` and you
can use that directory's location to install VERDICT into OSATE.

## Install our VERDICT plugin

After you build our plugin sources, you can install our plugin into a
stable OSATE release.  If you don't have a stable OSATE release
installed, you can follow these
[instructions](https://osate.org/download-and-install.html) to
download and install the latest stable OSATE release.  Once you have
installed a stable OSATE release, you can follow these steps to
install our VERDICT plugin into it:

1. Start OSATE and open its Install New Software wizard using the
   pulldown menu (Help > Install New Software...).

2. Click the Add... button in the Install dialog.

3. Click the Local... button in the Add Repository dialog.

4. Select the newly built
   `com.ge.research.osate.verdict.updatesite/target/repository` folder
   using the file chooser dialog.

5. Click the Add button in the Add Repository dialog.

6. Click the Finish button in the Install dialog and restart OSATE
   when prompted to do so.

7. Finally, set up our VERDICT plugin to run either our Docker image
   or our back-end programs as specified in these
   [steps](../README.md).  Briefly speaking, you will have to download
   an "extern.zip" file from our
   [Releases](https://github.com/ge-high-assurance/VERDICT/releases)
   page, unpack it, and then set some Eclipse preferences to tell our
   VERDICT plugin how to find the STEM folder and either the Docker
   image or the back-end programs.

## Import and develop our VERDICT plugin sources

Follow these instructions if you want to import and develop our VERDICT
plugin sources in your Eclipse IDE.  You will need to use an Eclipse
IDE that has at least the Plugin Development Environment (PDE) feature
installed.  Most Eclipse IDEs (Java, Modeling, etc.) already have this
PDE feature installed unless you are using a very specialized Eclipse
IDE in which case you will have to install a standard Eclipse for Java
developers IDE.

We still recommend that you build our VERDICT plugin sources from the
command line first as we showed earlier.  This will reduce the number
of steps you need to perform in your Eclipse IDE to just the
following:

1. Launch your Eclipse IDE and select File -> Import... from the
   pulldown menu.

2. Expand Maven in the list of import wizards and select the Maven ->
   Existing Maven Projects wizard.  Press the Next button.

3. Click the Browse... button and use the file chooser dialog to
   navigate to either this directory or the tools directory (depending
   on whether you want to import just the VERDICT plugin's modules or
   all VERDICT modules).  If done correctly, you should see this
   directory's set of pom.xml files listed under Projects.  Press the
   Finish button.

4. Once your Eclipse IDE finishes importing our VERDICT plugin
   sources, these projects will have thousands of build errors in your
   workspace.  This is normal since your Eclipse IDE will be missing
   some necessary OSATE and other Eclipse APIs so your Eclipse IDE
   doesn't know where to find these APIs yet.  To make these build
   errors go away, you will need to open our target definition file in
   Eclipse and set it as your Eclipse IDE's current target platform.

5. In the Package Explorer pane, expand the verdict working set,
   expand the com.ge.research.osate.verdict.targetplatform project,
   and double click the
   com.ge.research.osate.verdict.targetplatform.target file to open it
   in the Eclipse editor window.

6. Click on "Set as Target Platform" in the upper right corner.
   Eclipse will spend some time resolving the target definition file
   and downloading the OSATE and other Eclipse APIs from update sites.
   Please note that if you have other projects in your workspace
   besides our VERDICT plugin sources, you may want to close these
   projects first because you will be changing the target platform
   they were built against before and they may get some build errors
   due to incompatible API versions.

7. Just setting the target platform isn't always enough to make all
   the build errors go away.  To force Eclipse to delete all the build
   errors and rebuild the VERDICT projects again using the new target
   platform, select all the VERDICT projects in the Package Explorer
   pane, right click on one of the VERDICT projects, and select Maven
   -> Update Project... from the popup menu.  Click the OK button in
   the Update Maven Project dialog to start the rebuild, and then all
   the errors in the Problems tab should be gone after the rebuid
   finishes.  Some warnings will be left, but that's expected since we
   haven't fixed all warnings in either Maven or Eclipse.

Now you can start developing our VERDICT plugin sources and making
changes to them.  You can launch a runtime Eclipse instance directly
from your Eclipse IDE whenever you want to run or debug our plugin.
To do so, right click on the com.ge.research.osate.verdict project and
select Run As... -> Eclipse Application or Debug As... -> Eclipse
Application.  Unless you have imported and built all the OSATE source
projects in your Eclipse IDE, however, your runtime Eclipse instance
will not be a complete OSATE product.  It will have a Verdict pulldown
menu but the OSATE and Analyses pulldown menus will be missing.  You
still will be able to open an AADL model project and run some Verdict
commands such as CRV and MBAS on the model, though.

If you decide that you do want to set up a full-blown OSATE
development instance capable of launching and debugging a complete
runtime OSATE product along with our VERDICT plugin, you can follow
the instructions in [Setting up an OSATE development
environment](https://osate.org/setup-development.html).  Once you have
launched your OSATE development IDE and waited for it to import its
OSATE sources, you can import our VERDICT plugin sources into it as
well.  You will not need to use our VERDICT plugin's target platform
to make some build errors go away; OSATE's target platform will be a
superset of our target platform except for the following features
(AGREE and Xtext Antlr SDK) which you will have to install into your
OSATE development IDE from their own update sites:

- [com.rockwellcollins.atc.agree.feature](https://loonwerks.github.io/AGREE-Updates/releases/2.9.1)
- [de.itemis.xtext.antlr.sdk](http://download.itemis.com/updates/releases/2.1.1)

# Additional notes

Even though our VERDICT plugin sources have pom files, we still need
to keep the com.ge.research.osate.verdict.dsl/.project file under
source control to ensure that it is present at all times.  If that
.project file is missing, the Tycho build will not be able to call the
GenerateVerdict.mwe2 workflow successfully.  I've spent lots of time
trying to ensure the GenerateVerdict.mwe2 workflow works in both Unix
and Window environments no matter which directory you run Maven in.
I've learned that both keeping the .project file and launching the
MWE2 workflow in a separate process with its current directory set to
the com.ge.research.osate.verdict.dsl directory are crucial steps.
The MWE2 workflow must find the .project file using a relative path
(hence the current directory must be set because a relative path is
the only filename that works on both Unix and Windows) and then
resolves all its project-relative paths against the .project file
(hence the file must be present at all times).  We may remove the
other .project and .classpath files from source control later but for
now, keep all of them under source control to ensure no one tries to
delete the single .project file that really matters.
