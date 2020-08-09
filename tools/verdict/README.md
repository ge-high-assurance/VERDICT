# VERDICT: Building the OSATE plugin

## About the OSATE plugin

[OSATE](https://osate.org/about-osate.html) is an Open Source AADL
Tool Environment based on the Eclipse Modeling Tools IDE.  The VERDICT
tools consist of an OSATE plugin and a set of VERDICT back-end
programs invoked by the plugin.  The OSATE plugin sources are in this
directory and the back-end program sources are in another directory
[(../verdict-back-ends)](../verdict-back-ends).

Running Maven in this directory builds an update site (a directory,
not a website) containing our OSATE plugin.  You can install the
directory
`com.ge.research.osate.verdict.updatesite/target/repository/` directly
into an OSATE release or copy that directory to a new directory in our
[VERDICT-update-sites](https://github.com/ge-high-assurance/VERDICT-update-sites)
repository, from where you can install our plugin via an update site
URL.

## Set up your build environment

You will need a [Java Development Kit](https://adoptopenjdk.net/)
(version 8 or 11) to build all of our Java program sources.  We have
tried Java 11 LTS successfully, but OSATE itself is officially
supported only on Java 8 LTS so we recommend using Java 8 LTS anyway.

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

## Update our target definition file (if needed)

Our OSATE plugin's target definition file tells Maven and Eclipse
Tycho where to find the OSATE and other Eclipse APIs that our OSATE
plugin sources should be compiled against.  Generally speaking, you do
not need to change the OSATE update site in our target definition file
every time a stable OSATE release comes out.  The OSATE and other
Eclipse APIs used by our plugin tend to remain compatible across
several stable OSATE releases.  We use "0.0.0" as API version numbers
in our target definition file because we want our plugin to work in
other OSATE releases besides the OSATE update site that happens to be
in our file.

If you still want to change our target
[definition](com.ge.research.osate.verdict.targetplatform/com.ge.research.osate.verdict.targetplatform.target)
file to use another stable OSATE release's update site, please go
ahead and edit the file.  Our file also has some other Eclipse update
sites that you may want to change in lockstep with the OSATE update
site.  You can use the OSATE source repository's own corresponding
target
[definition](https://github.com/osate/osate2/blob/2.7.1/core/org.osate.build.target/osate2-platform.target)
file as a reference; make sure to copy the same update site urls OSATE
itself uses for similar Eclipse features to our own target definition
file.

## Build our OSATE plugin sources

This directory and its subdirectories have pom.xml files which will
allow you to build our OSATE plugin sources with Maven and Eclipse
Tycho.  Unless you want to import and develop the OSATE plugin sources
in your Eclipse IDE, we recommend that you build our OSATE plugin
sources only with Maven from the command line in this directory:

`$ mvn clean install -Dtycho.localArtifacts=ignore`

Including the `-Dtycho.localArtifacts=ignore` argument may prevent
some build problems from happening.  You may not always need the
`-Dtycho.localArtifacts=ignore` argument, but please change the
argument or leave it out only if you are familiar with Tycho builds
and want Tycho to use local artifacts that you have already built,
such as if you are running Maven in a subdirectory.

If Maven encounters a problem and you need to see more information to
diagnose what caused the problem, rerun the command line with an
additional `-X` argument at the end to make Maven print very detailed
debugging output.

If the Maven build completes successfully, you will be able to install
our OSATE plugin in an OSATE release as shown in the next section.  A
successful build ends with the following output:

```text
[INFO] --- tycho-p2-plugin:1.7.0:update-local-index (default-update-local-index) @ com.ge.research.osate.verdict.updatesite ---
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for verdict 1.0.0-SNAPSHOT:
[INFO]
[INFO] verdict ............................................ SUCCESS [  0.367 s]
[INFO] com.ge.research.osate.verdict.dsl .................. SUCCESS [ 38.026 s]
[INFO] com.ge.research.osate.verdict.dsl.ide .............. SUCCESS [  0.296 s]
[INFO] com.ge.research.osate.verdict.dsl.ui ............... SUCCESS [  1.160 s]
[INFO] com.ge.research.osate.verdict.vdm .................. SUCCESS [  2.029 s]
[INFO] com.ge.research.osate.verdict ...................... SUCCESS [  2.278 s]
[INFO] com.ge.research.osate.verdict.feature .............. SUCCESS [  0.298 s]
[INFO] com.ge.research.osate.verdict.targetplatform ....... SUCCESS [  0.066 s]
[INFO] com.ge.research.osate.verdict.updatesite ........... SUCCESS [  2.307 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  02:14 min
[INFO] Finished at: 2020-07-15T20:35:52-04:00
[INFO] ------------------------------------------------------------------------
```

The last directory built by Maven is an update site for the OSATE
plugin which you can install into an OSATE release.  That update site
is actually located inside the directory
`com.ge.research.osate.verdict.updatesite/target/repository` and that
directory can be installed directly into OSATE.

## Install our OSATE plugin

After you build our plugin sources, you can install our plugin into a
stable OSATE release.  If you don't have a stable OSATE release
installed, you can follow these
[instructions](https://osate.org/download-and-install.html) to
download and install the latest stable OSATE release.  Once you have
installed a stable OSATE release, you can follow these steps to
install our OSATE plugin into it:

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

7. Finally, set up our OSATE plugin to run either our Docker image or
   our back-end programs as specified in these [steps](../README.md).
   Briefly speaking, you will have to download an "extern.zip" file
   from our
   [Releases](https://github.com/ge-high-assurance/VERDICT/releases)
   page, unpack it, and then set some Eclipse preferences to tell the
   OSATE plugin how to find the STEM folder and either the Docker
   image or the back-end programs.

## Import and develop our OSATE plugin sources

Follow these instructions if you want to import and develop the OSATE
plugin sources in your Eclipse IDE.  You will need to use an Eclipse
IDE that has at least the Plugin Development Environment (PDE) feature
installed.  Most Eclipse IDEs (Java, Modeling, etc.) already have this
PDE feature installed unless you are using a very specialized Eclipse
IDE in which case you will have to install a standard Eclipse for Java
developers IDE.

We still recommend that you build our OSATE plugin sources from the
command line first as we showed earlier.  This will reduce the number
of steps you need to perform in your Eclipse IDE to just the
following:

1. Launch your Eclipse IDE and select File -> Import... from the
   pulldown menu.

2. Expand Maven in the list of import wizards and select the Maven ->
   Existing Maven Projects wizard.  Press the Next button.

3. Enter this directory's path as the Root Directory or click the
   Browse... button and navigate to this directory using a file
   chooser dialog.  If done correctly, you should see this directory's
   set of pom.xml files listed under Projects.  Press the Finish
   button.

4. Once your Eclipse IDE finishes importing the OSATE plugin sources,
   these projects will have thousands of build errors in your
   workspace.  This is normal since your Eclipse IDE will be missing
   some necessary OSATE and other Eclipse APIs so your Eclipse IDE
   doesn't know where to find these APIs yet.

5. To make these build errors go away, you need to open our target
   definition file in Eclipse and set it as your Eclipse IDE's current
   target platform.  In the Package Explorer pane, expand the verdict
   working set, expand the
   com.ge.research.osate.verdict.targetplatform project, and double
   click the com.ge.research.osate.verdict.targetplatform.target file
   to open it in the Eclipse editor window.

6. Eclipse will need to spend some time resolving the target
   definition file and downloading the OSATE and other Eclipse APIs
   from update sites.  Wait for it to finish, then click on "Set as
   Target Platform" in the upper right corner.  Eclipse will rebuild
   the VERDICT projects again and the build errors should go away.
   Please note that if you have other projects in your workspace
   besides our OSATE plugin sources, you may have to close these
   projects first in case they get some build errors because you will
   be changing the target platform they were built against before.

Now you can start developing our OSATE plugin sources and making
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
runtime OSATE product along with the VERDICT plugin, you can follow
the instructions in [Setting up an OSATE development
environment](https://osate.org/setup-development.html).  Once you have
launched your OSATE development IDE and waited for it to import the
OSATE sources, you can import our OSATE plugin sources into it as
well.  You will not need to change your OSATE development IDE's target
platform to make some build errors go away; OSATE's target platform
will be a superset of our OSATE plugin's target platform except for
one feature (de.itemis.xtext.antlr.sdk.feature.group) which you may
have to install into your IDE from its own
[update](http://download.itemis.com/updates/releases/2.1.1) site.

# Additional notes

Even though our OSATE plugin sources have pom files, we still need to
keep the com.ge.research.osate.verdict.dsl/.project file under source
control to ensure that it is present at all times.  If that .project
file is missing, the Tycho build will not be able to call the
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
