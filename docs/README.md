# VERDICT: Tools for architectural and behavioral analysis of AADL models

[OSATE](https://osate.org/about-osate.html) is an Open Source AADL
Tool Environment based on the Eclipse Modeling Tools IDE.  The VERDICT
tools consist of a VERDICT plugin for OSATE and a set of VERDICT
back-end programs invoked by the plugin to perform cyber resiliency
analysis (CRV) and model based architecture analysis (MBAA).  The
plugin has two ways to call the back-end programs; it can run an
executable jar called verdict-bundle-app-\<VERSION\>-capsule.jar in a
subprocess or it can run a Docker image called gehighassurance/verdict
in a temporary container.

## 1. Install OSATE and our VERDICT plugin

Front-end **prerequisites**

- [Java 11](https://adoptium.net/)
- [OSATE 2.7.1](https://osate-build.sei.cmu.edu/download/osate/stable/2.7.1-vfinal/products/)

You will need [Java 11](https://adoptium.net/) to run OSATE and our
plugin.  OSATE runs fine on Java 11 even though its documentation says
it officially supports only Java 8.  You cannot build our plugin
source code with Java 8; Java 8 is too old and no longer supported by
Maven's Eclipse Tycho build system.

You will need [OSATE](https://osate.org/about-osate.html) (Open Source
AADL Tool Environment) in order to use our VERDICT plugin:

- Download the [OSATE
  2.7.1](https://osate-build.sei.cmu.edu/download/osate/stable/2.7.1-vfinal/products/)
  product for your operating system.

- If necessary, refer to these [installation
  instructions](https://osate.org/download-and-install.html) to
  install OSATE.

- Start OSATE and open its Install New Software wizard using the
  pulldown menu (Help > Install New Software...).

- Click the Add... button in the Install dialog.

- Paste the following URL in the Location field:

  <https://raw.githubusercontent.com/ge-high-assurance/VERDICT-update-sites/master/verdict-latest>

  If you want to install VERDICT's most current development version
  rather than VERDICT's latest stable version, then use the following
  URL instead:

  <https://raw.githubusercontent.com/ge-high-assurance/VERDICT-update-sites/master/verdict-dev>

- Click the Finish button in the Install dialog and restart OSATE when
  prompted to do so.

Our VERDICT plugin also uses another OSATE plugin called
[AGREE](https://github.com/loonwerks/AGREE-updates).  AGREE is short
for "Assume-Guarantee REasoning Environment" and you can learn more
about it at <https://github.com/loonwerks/AGREE>.  We bundle the
specific parts we need from the AGREE 2.5.2 plugin in our VERDICT
update site so you can install only VERDICT in OSATE 2.7.1 without
your having to install AGREE as well.  If you want to use all of
AGREE's functionality or install VERDICT in earlier or later OSATE
versions, you also will have to install AGREE using the following URL
in OSATE's "Install New Software..."  dialog:

<https://raw.githubusercontent.com/loonwerks/AGREE-Updates/master>

The above URL will install the matching AGREE version in your OSATE
automatically, although you may find that some OSATE versions do not
have not any matching AGREE versions.  Each AGREE version works only
with a specific OSATE version while our VERDICT plugin tries to work
with any OSATE and AGREE version.  We have tested and verified that
the combination of OSATE 2.7.1, AGREE 2.5.2, and VERDICT works fine
together.  Our VERDICT plugin will work with earlier OSATE and AGREE
versions but we need to do some work on the translators to accommodate
later AGREE versions that changed their API calls' behavior before we
can guarantee that our VERDICT plugin will work with later OSATE and
AGREE versions.

## 2. Install the VERDICT back-end tool chain

We support two ways to run the VERDICT back-end programs: Docker image
and native binaries.  Users may choose whichever approach they prefer.
The native binaries work only on current OS X (Catalina 10.15) and
Ubuntu (Focal 20.04) distributions.  The Docker image works on any
platform which can run Docker.

### a. Run the back-end tool chain via Docker

Docker **prerequisites**

- [Docker](https://docs.docker.com/get-docker/)
- [extern.zip](https://github.com/ge-high-assurance/VERDICT/releases)

Install Docker if you haven't installed it yet.  If you are running
Docker on Windows, you will also have to do the following to allow our
plugin to communicate with Docker:

- Set an environment variable called DOCKER_HOST to the value
  "tcp://localhost:2375" to tell our plugin to connect to the daemon
  using the daemon's TCP port instead of the daemon's Unix file
  socket.

- Go into Settings in Docker Desktop and enable the checkbox next to
  "Expose daemon on tcp://localhost:2375 without TLS".

- Click on Resources under Settings, click on FILE SHARING under
  Resources, and add your drive letter to the list of directories
  which Docker can bind mount into containers.

- Click the "Apply & Restart" button at the bottom to restart Docker
  after these changes.

Once Docker is installed, you also need to pull a VERDICT Docker image
from Docker Hub to your machine by running one of the following
commands in your terminal depending on whether you want the latest
release image or the most current development image::

```shell
docker pull gehighassurance/verdict:latest
docker pull gehighassurance/verdict-dev:latest
```

Also download the extern.zip archive and unpack it somewhere on your
machine.  You will need some files from the extern folder regardless
of whether you use Docker or native binaries.  After unpacking
extern.zip (make sure you remember the location of the unpacked extern
folder), then configure some VERDICT settings in OSATE:

- Launch OSATE, go to Window > Preferences > Verdict > Verdict
  Settings, and fill out the following fields.

- Click the Browse... button next to the "STEM Project PATH:" field,
  navigate to the "STEM" folder inside the "extern" folder, and make
  it the field's setting.

- Click within the "Bundle Docker Image:" field and type our Docker
  image's name "gehighassurance/verdict" in that field.

  If you want to use VERDICT's most current development image
  rather than VERDICT's latest release image, then enter the
  following name instead: "gehighassurance/verdict-dev".

- Click the "Apply and Close" button to save all the settings that you
  just entered into the fields.

### b. Run the back-end binaries natively

You can run the VERDICT back-end native binaries on Ubuntu 20.04 and
MacOS 10.15.

Native **prerequisites**

- [extern.zip](https://github.com/ge-high-assurance/VERDICT/releases):
  VERDICT back-end programs
- [GraphViz](https://www.graphviz.org/download/): Graph Visualization Software
- [Z3](https://github.com/Z3Prover/z3): The Z3 Theorem Prover

Make sure both graphviz and z3 are installed under your system path.
On Ubuntu, you would say "sudo apt install graphviz z3".  On MacOS,
you would say "brew install graphviz z3".

Download the extern.zip archive and unpack it somewhere on your
machine.  You will need some files from the extern folder regardless
of whether you use Docker or native binaries.  After unpacking
extern.zip (make sure you remember the location of the unpacked extern
folder), then configure some VERDICT settings in OSATE:

- Launch OSATE, go to Window > Preferences > Verdict > Verdict
  Settings, and fill out the following fields.

- Click the Browse... button next to the "STEM Project PATH:" field,
  navigate to the "STEM" folder inside the "extern" folder, and make
  it the field's setting.

- Make sure the field "Bundle Docker Image:" stays empty and perform
  the following steps instead.

- Click the Browse... button next to the "Bundle Jar:" field, navigate
  to the "verdict-bundle-app-\<VERSION\>-capsule.jar" file inside the
  "extern" folder, and make it the field's setting.

- Click the Browse... button next to the "Kind2 Binary:" field,
  navigate to the appropriate "kind2" binary for your operating system
  inside either the "extern/mac" or "extern/nix" folders, and make it
  the field's setting.

- Click the Browse... button next to the "Soteria++ Binary:" field,
  navigate to the appropriate "soteria_pp" binary for your operating
  system inside either the "extern/mac" or "extern/nix" folders, and
  make it the field's setting.

- Click the Browse... button next to the "GraphViz Path:" field,
  navigate to the folder on your operating system where your GraphViz
  Graph Visualization software is installed, and make it the field's
  setting. On Ubuntu, that location would be "/usr/bin".

- Click the "Apply and Close" button to save all the settings that you
  just entered into the fields.

## 3. Run VERDICT

Now you are ready to run our VERDICT plugin on an AADL model.  You can
pick one of the example AADL models in the extern.zip file you unpacked
(for example, "extern/examples/DeliveryDrone") and use OSATE's "File >
Import... > General > Existing Projects into Workspace" wizard to
import that model into your OSATE workspace.  You can open the model's
AADL files to see how that model uses AGREE annexes, VERDICT annexes,
and VERDICT properties.

You can invoke VERDICT's functionality from the Verdict pulldown menu.
First click on a project's name in the AADL Navigator pane to make it
the currently selected project, then pull down the Verdict menu and
run the appropriate back-end tools (MBAA, MBAS, CRV, etc.).  Our
plugin will use Docker Java API calls to pull the appropriate Docker
image from Docker Hub (only if the image isn't already in your
Docker's local cache), start a temporary Docker container to run the
MBAA or CRV command, and then display any output from that command.
If you use the executable jar instead of the Docker image, the plugin
will run the executable jar in a subprocess directly on your system.
Both the temporary container and the executable jar will run several
VERDICT back-end tool chain programs in additional subprocesses inside
the temporary container or directly on your system as well.

For further information how to analyze a model's system architecture
using our VERDICT tools, please read our VERDICT wiki's
[documentation](https://github.com/ge-high-assurance/VERDICT/wiki).
