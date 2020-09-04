# VERDICT: An OSATE plugin for architectural and behavioral analysis of AADL models

[OSATE](https://osate.org/about-osate.html) is an Open Source AADL
Tool Environment based on the Eclipse Modeling Tools IDE.  The VERDICT
tools consist of an OSATE plugin and a set of VERDICT back-end
programs invoked by the plugin to perform cyber resiliency analysis
(CRV) and model based architecture analysis (MBAA).  The plugin has
two ways to call the back-end programs; it can run an executable jar
called verdict-bundle-1.0.0-SNAPSHOT-capsule.jar in a subprocess or it
can run a Docker image called gehighassurance/verdict in a temporary
container.

1. Install OSATE and the VERDICT front-end plugin

**Prerequisites**

- [Java 8](https://adoptopenjdk.net/)
- [OSATE 2](https://osate-build.sei.cmu.edu/download/osate/stable/2.7.1-vfinal/products/)
- [AGREE plugin for OSATE 2](https://raw.githubusercontent.com/loonwerks/AGREE-Updates/master/agree_2.5.2)

If necessary, read OSATE's installation directions
<https://osate.org/download-and-install.html> to install OSATE.  Also
install the AGREE plugin in OSATE using its Install New Software
wizard.

To install our VERDICT plugin in OSATE:

- Start OSATE and open its Install New Software wizard using the
  pulldown menu (Help > Install New Software...).

- Click the Add... button in the Install dialog.

- Paste the following URL in the Location field:

  <https://raw.githubusercontent.com/ge-high-assurance/VERDICT-update-sites/master/verdict-latest>

- Click the Add button in the Add Repository dialog.

- Click the Finish button in the Install dialog and restart OSATE when
  prompted to do so.

2. Install the VERDICT back-end tool chain

We support two ways to run the VERDICT back-end programs: Docker image
and native binaries.  Users may choose whichever approach they prefer.
The native binaries work only on up to date Mac (Catalina 10.15) and
Ubuntu (20.04) distributions.  The Docker image works on any other
platform which can run Docker.

a. Run the back-end tool chain via Docker

**Prerequisites**

- [Docker](https://docs.docker.com/get-docker/)
- [extern.zip](https://github.com/ge-high-assurance/VERDICT/releases)

If you are running Docker on Windows, you will also have to do the
following things to allow our plugin to communicate with Docker:

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

Download the extern.zip archive and unpack it somewhere on your
machine too.  You will need some files from the extern folder
regardless of whether you use Docker or native binaries.  After
unpacking extern.zip, please configure some VERDICT settings in OSATE:

- Launch OSATE, go to Window > Preferences > Verdict > Verdict
  Settings, and fill out the following fields.

- Fill in the "VERDICT Properties Name:" field with the property set
  name. In the DeliveryDrone model, we use the property set name
  "CASE_Consolidated_Properties".

- Click the Browse... button next to the "STEM Project PATH:" field,
  navigate to the "STEM" folder inside the "extern" folder, and make
  it the field's setting.

- Click within the "Bundle Docker Image:" field, type
  "gehighassurance/verdict" for our Docker image's name, and save your
  changes by clicking the "Apply and Close" button.

Finally, you will need to pull down the latest VERDICT Docker image to
your machine by running the following command in your terminal:

`docker pull gehighassurance/verdict:latest`

b. Run the back-end binaries natively

You can run the VERDICT back-end native binaries on Ubuntu 20.04 and
MacOS Catalina 10.15.

**Prerequisites**

- [GraphViz](https://www.graphviz.org/download/): Graph Visualization Software
- [Z3](https://github.com/Z3Prover/z3): The Z3 Theorem Prover
- [extern.zip](https://github.com/ge-high-assurance/VERDICT/releases)

Make sure both graphviz and z3 are installed under your system path.
On Ubuntu, you would say "sudo apt install graphviz z3".  On Mac, you
would say "brew install graphviz z3".

Download the extern.zip archive and unpack it somewhere on your
machine too.  You will need some files from it regardless of whether
you use Docker or native binaries.  After unpacking extern.zip, please
configure some VERDICT settings in OSATE:

- Launch OSATE, go to Window > Preferences > Verdict > Verdict
  Settings, and fill out the following fields.

- Fill in the "VERDICT Properties Name:" field with the property set
  name. In the VERDICT program, we use the property set name
  "CASE_Consolidated_Properties".

- Click the Browse... button next to the "STEM Project PATH:" field,
  navigate to the "STEM" folder inside the "extern" folder, and make
  it the field's setting.

- Make sure the field "Bundle Docker Image:" stays empty and perform
  the following steps instead.

- Click the Browse... button next to the "Bundle Jar:" field, navigate
  to the "verdict-bundle-1.0.0-SNAPSHOT-capsule.jar" file inside the
  "extern" folder, and make it the field's setting.

- Click the Browse... button next to the "Aadl2iml Binary:" field,
  navigate to the appropriate "aadl2iml" binary for your operating
  system inside either the "extern/mac" or "extern/nix" folders, and
  make it the field's setting.

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

3. Run VERDICT

Now you are ready to run the VERDICT plugin on an AADL model.  You can
use one of the example AADL models in the extern.zip file you unpacked
(for example, "extern/examples/DeliveryDrone") and use OSATE's "File >
Import... > General > Existing Projects into Workspace" wizard to
import that model into your OSATE workspace.  You can open the model's
AADL files to see how that model uses AGREE annexes, VERDICT annexes,
and VERDICT properties.

You can invoke VERDICT's functionality from the Verdict pulldown menu.
First click on a project's name in the AADL Navigator pane to make it
the currently selected project, then pull down the Verdict menu and
run the appropriate back-end tools (MBAA, MBAS, CRV, etc.).

For further information how to analyze a model's system architecture
using our VERDICT tools, please read our VERDICT wiki's
[documentation](https://github.com/ge-high-assurance/VERDICT/wiki).
