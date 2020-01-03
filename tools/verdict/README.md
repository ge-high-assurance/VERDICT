# VERDICT: Building the OSATE VERDICT plugin

## About OSATE

[OSATE](https://osate.org/about-osate.html) is an Open Source AADL
Tool Environment based on the Eclipse Modeling Tools IDE.  The VERDICT
tool consists of an OSATE plugin and a set of VERDICT back-end tools
invoked by the plugin.  The OSATE plugin sources are in this directory
and the back-end tool sources are in the
[../verdict-back-ends](../verdict-back-ends) directory.

## Build our OSATE plugin sources

This directory and its subdirectories have a set of pom.xml files
which will allow you to build our OSATE plugin sources with Maven
using Eclipse Tycho.  Unless you want to import and edit the OSATE
plugin sources in your Eclipse IDE, we recommend that you build our
OSATE plugin sources from the command line in this directory as shown
below:


```shell
$ mvn clean install -Dtycho.localArtifacts=ignore
```

You may not always need the `-Dtycho.localArtifacts=ignore` argument
but it is much safer to include it every time you build the OSATE
plugin sources since it may prevent some build problems from
happening.  You should change the argument or leave it out only if you
are very familiar with Tycho builds and really want Tycho to use a
local artifact that you have already built.

If the Maven build encounters a problem and you need to see more
information to diagnose what caused the problem, rerun the command
line with an additional `-X` argument at the end to make Maven print
very detailed debugging output.

If the Maven build completes successfully, you will be able to install
our OSATE plugin in an OSATE release as shown in the next section.  A
successful build ends with the following output:

```text
[INFO] --- tycho-p2-repository-plugin:1.5.1:archive-repository (default-archive-repository) @ com.ge.research.osate.verdict.updatesite ---
[INFO] Building zip: /home/interran/git/VERDICT/tools/verdict/com.ge.research.osate.verdict.updatesite/target/com.ge.research.osate.verdict.updatesite-1.0.0-SNAPSHOT.zip
[INFO]
[INFO] --- maven-install-plugin:3.0.0-M1:install (default-install) @ com.ge.research.osate.verdict.updatesite ---
[INFO] Installing /home/interran/git/VERDICT/tools/verdict/com.ge.research.osate.verdict.updatesite/target/com.ge.research.osate.verdict.updatesite-1.0.0-SNAPSHOT.zip to /home/interran/.m2/repository/com/ge/research/osate/verdict/com.ge.research.osate.verdict.updatesite/1.0.0-SNAPSHOT/com.ge.research.osate.verdict.updatesite-1.0.0-SNAPSHOT.zip
[INFO] Installing /home/interran/git/VERDICT/tools/verdict/com.ge.research.osate.verdict.updatesite/pom.xml to /home/interran/.m2/repository/com/ge/research/osate/verdict/com.ge.research.osate.verdict.updatesite/1.0.0-SNAPSHOT/com.ge.research.osate.verdict.updatesite-1.0.0-SNAPSHOT.pom
[INFO] Installing /home/interran/git/VERDICT/tools/verdict/com.ge.research.osate.verdict.updatesite/target/p2content.xml to /home/interran/.m2/repository/com/ge/research/osate/verdict/com.ge.research.osate.verdict.updatesite/1.0.0-SNAPSHOT/com.ge.research.osate.verdict.updatesite-1.0.0-SNAPSHOT-p2metadata.xml
[INFO] Installing /home/interran/git/VERDICT/tools/verdict/com.ge.research.osate.verdict.updatesite/target/p2artifacts.xml to /home/interran/.m2/repository/com/ge/research/osate/verdict/com.ge.research.osate.verdict.updatesite/1.0.0-SNAPSHOT/com.ge.research.osate.verdict.updatesite-1.0.0-SNAPSHOT-p2artifacts.xml
[INFO]
[INFO] --- tycho-p2-plugin:1.5.1:update-local-index (default-update-local-index) @ com.ge.research.osate.verdict.updatesite ---
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for verdict 1.0.0-SNAPSHOT:
[INFO]
[INFO] verdict ............................................ SUCCESS [  0.694 s]
[INFO] com.ge.research.osate.verdict.dsl .................. SUCCESS [01:57 min]
[INFO] com.ge.research.osate.verdict.dsl.ide .............. SUCCESS [  0.516 s]
[INFO] com.ge.research.osate.verdict.dsl.ui ............... SUCCESS [  4.216 s]
[INFO] com.ge.research.osate.verdict ...................... SUCCESS [  5.133 s]
[INFO] com.ge.research.osate.verdict.feature .............. SUCCESS [  0.381 s]
[INFO] com.ge.research.osate.verdict.target ............... SUCCESS [  0.089 s]
[INFO] com.ge.research.osate.verdict.updatesite ........... SUCCESS [  3.868 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  02:27 min
[INFO] Finished at: 2020-01-02T16:29:21-05:00
[INFO] ------------------------------------------------------------------------
```

Maven will build an update site repository for the OSATE plugin and
then archive the repository into a zip file which you can install into
an OSATE release.  Look for the "Building zip:" line in the build
output above to find the location of that zip file in your filesystem.

## Install our OSATE plugin zip file

After you build a zip file (which Eclipse calls a deployable archive),
you can install our OSATE plugin into an OSATE release.  If you have
not already done it, please follow these
[instructions](https://osate.org/download-and-install.html) to
download and install the latest stable OSATE release.  Generally
speaking, our OSATE plugin's target definition file usually specifies
the latest stable OSATE release as the target platform that the OSATE
plugin should be compiled against.  Click on this
[target](com.ge.research.osate.verdict.target/com.ge.research.osate.verdict.target.target)
definition file if you need to find out or change that OSATE release's
version.  Please keep in mind that you also may need to change the
versions of some dependent features in this file in lockstep when you
change the OSATE version.  The OSATE source repository also has its
own OSATE
[target](https://github.com/osate/osate2/blob/2.6.1/core/org.osate.build.target/osate2-platform.target)
definition file which you can use as a reference; simply copy the same
versions it uses for these other features in the OSATE release you
want to compile against.

Once you have installed and started your OSATE release, follow these
steps to deploy our OSATE plugin into it:

1. Open OSATE's Install New Software wizard using the pulldown menu
   (Help > Install New Software...).

2. Click the Add... button in the Install dialog.

3. Click the Archive... button in the Add Repository dialog.

4. Select the newly built zip file using the file chooser dialog

5. Click the Add button in the Add Repository dialog.

6. Click the Finish button in the Install dialog and restart OSATE
   when prompted to do so.

7. Finally, click on these
   [steps](com.ge.research.osate.verdict/README.md) for some more
   instructions you will have to follow to make the OSATE plugin ready
   for use.  Briefly speaking, you will have to download all the
   necessary VERDICT back-end binaries from our
   [Releases](https://github.com/ge-high-assurance/VERDICT/releases)
   page, unpack the release archive to get these binaries on your
   filesystem, set an environment variable, and then set some Eclipse
   preferences to tell the OSATE plugin where to find these
   executables on your filesystem.

## Import and edit our OSATE plugin sources

Follow these instructions if you want to import and edit the OSATE
plugin sources in your Eclipse IDE.  You will need to use an Eclipse
IDE that has at least the Plugin Development Environment (PDE) feature
installed.  Most Eclipse IDEs (Java, Modeling, etc.) already have this
PDE feature installed unless you are using a very specialized Eclipse
IDE in which case you will have to install a standard Eclipse IDE for
Java developers.

We still recommend that you build our OSATE plugin sources from the
command line first as we showed earlier.  This will reduce the number
of steps you need to perform in your Eclipse IDE to the following:

1. Launch your Eclipse IDE and select File -> Import... from the
   pulldown menu.

2. Expand Maven in the list of import wizards and select the Maven ->
   Existing Maven Projects wizard.  Press the Next button.

3. Enter this directory's path as the Root Directory or click the
   Browse... button to use a file chooser dialog to get this
   directory's path.  If done correctly, you should see this
   directory's set of pom.xml files listed under Projects.  Press the
   Finish button.

4. Once your Eclipse IDE finishes importing the OSATE plugin sources,
   these projects will have thousands of build errors in your
   workspace.  This is normal since your current target platform will
   be missing some necessary APIs and your Eclipse doesn't know where
   to find these APIs yet.

5. To make these build errors go away, you need to open our target
   definition file in Eclipse and set the current target platform to
   what it specifies.  In the Package Explorer pane, expand the
   verdict working set, expand the
   com.ge.research.osate.verdict.target project, and double click the
   com.ge.research.osate.verdict.target.target file to open it in the
   Eclipse editor window.

6. Eclipse will need to spend some time resolving the target
   definition file and downloading the features from update sites.
   Wait until it is finished before you click on "Set as Target
   Platform" in the upper right corner.  Now the build errors should
   go away after Eclipse finishes building the projects again.  Please
   note that if you have other projects in your workspace besides our
   OSATE plugin sources, you may have to close these projects first in
   case they get some build errors after you change the target
   platform they were compiled against before.

Now you can start editing our OSATE plugin sources if you want to make
some changes to them.  You can run Maven from Eclipse or the command
line whenever you want to assemble a new deployable archive, or you
can launch a runtime Eclipse instance directly from your development
Eclipse instance.  To do so, right click on the
com.ge.research.osate.verdict project and select Run As... -> Eclipse
Application.  Unless you have set up and are running an OSATE
development IDE, however, your runtime Eclipse instance will not be a
complete OSATE product.  It will have a Verdict pulldown menu but the
OSATE, Analyses, and AGREE pulldown menus will be missing.  You still
may be able to open an AADL project and do some debugging, though.

If you decide that you do want to set up a full-blown OSATE
development instance capable of launching and debugging a complete
runtime OSATE product along with the VERDICT plugin, you can follow
the instructions in [Setting up an OSATE development
environment](https://osate.org/setup-development.html).  Once you have
launched your OSATE development IDE and waited for it to import the
OSATE sources, you can import our OSATE plugin sources into it as
well.  You will not need to change your OSATE development IDE's target
platform to make some build errors go away; OSATE's target platform
will be a superset of our OSATE plugin's target platform.

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
