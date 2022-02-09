# VERDICT: Developing the VERDICT tools

## About the VERDICT tools

[OSATE](https://osate.org/about-osate.html) is an Open Source AADL
Tool Environment based on the Eclipse Modeling Tools IDE.  The VERDICT
tools consist of a VERDICT plugin for OSATE and a set of VERDICT
back-end programs invoked by the plugin to perform cyber resiliency
analysis (CRV) and model based architecture analysis (MBAA).  The
plugin has two ways to call the back-end programs; it can run an
executable jar called verdict-bundle-app-\<VERSION\>-capsule.jar in a
subprocess or it can run a Docker image called gehighassurance/verdict
in a temporary container.

## Build the VERDICT tools

The VERDICT tools' sources are divided into the following
subdirectories:

- [verdict](verdict): VERDICT plugin sources
- [verdict-back-ends](verdict-back-ends): VERDICT back-end program sources
- [verdict-data-model](verdict-data-model): common library used by both sources

Please read the development instructions in the first two
subdirectories' README.md files for further details.  Note that you
can descend into each subdirectory and build it with Maven separately
or you can build all three subdirectories from this location with
these Maven commands, making sure to call the clean, install, and
package goals separately (not on the same Maven command line):

```shell
mvn clean
mvn install --file verdict-back-ends/verdict-bundle/z3-native-libs/pom.xml
mvn package -Dtycho.localArtifacts=ignore
```

## Install our VERDICT plugin

Please follow the appropriate installation instructions depending on
whether you are a VERDICT user or a VERDICT developer:

- [VERDICT user](../docs/README.md)
- [VERDICT developer](verdict/README.md)

## Install our back-end programs

Again, please follow the appropriate installation instructions
depending on whether you are a VERDICT user or a VERDICT developer:

- [VERDICT user](../docs/README.md)
- [VERDICT developer](verdict-back-ends/README.md)

## Run the VERDICT tools

Now you are ready to run our VERDICT plugin on an AADL model.  You can
pick one of the example AADL models in our GitHub repository or the
extern.zip file you unpacked (for example,
"extern/examples/DeliveryDrone") and use OSATE's "File > Import... >
General > Projects from Folder" wizard to import that model into your
OSATE workspace.  You can open the model's AADL files to see how that
model uses AGREE annexes, VERDICT annexes, and VERDICT properties.  If
OSATE asks you whether to convert the project to an Xtext project
after you open an AADL file, answer yes since the project must have a
Xtext nature to enable our VERDICT plugin's functionality.

You can invoke our VERDICT plugin's functionality from the Verdict
pulldown menu.  First click on a project's name in the AADL Navigator
pane to make it the currently selected project, then pull down the
Verdict menu and run the appropriate back-end tools (MBAA, MBAS, CRV,
etc.).  Our plugin will use Docker Java API calls to pull down the
appropriate Docker image from Docker Hub (only if the image isn't
already in your Docker's local cache), start a temporary Docker
container to run the MBAA or CRV command, and then display any output
from that command.  If you use the executable jar instead of the
Docker image, the plugin will run the executable jar in a subprocess
directly on your system.  Both the temporary container and the
executable jar will run several VERDICT back-end tool chain programs
in additional subprocesses inside the temporary container or directly
on your system as well.

For further information how to analyze a model's system architecture
using our VERDICT tools, please read our VERDICT wiki's
[documentation](https://github.com/ge-high-assurance/VERDICT/wiki).
