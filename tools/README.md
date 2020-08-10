# VERDICT: Running the VERDICT tools

## About the VERDICT tools

[OSATE](https://osate.org/about-osate.html) is an Open Source AADL
Tool Environment based on the Eclipse Modeling Tools IDE.  The VERDICT
tools consist of an OSATE plugin and a set of VERDICT back-end
programs invoked by the plugin.  The plugin has two ways to call the
back-end programs; it can run an executable jar called
verdict-bundle-\<VERSION\>-capsule.jar in a subprocess or it can run
a Docker image called gehighassurance/verdict in a temporary
container.

The OSATE plugin sources are in this [verdict](verdict) subdirectory
and the back-end program sources are in this
[verdict-back-ends](verdict-back-ends) subdirectory.  We normally cd
into and build each subdirectory separately, but you can build both
subdirectories with a single command here if you want to:

`mvn clean install -Dtycho.localArtifacts=ignore`

## Install OSATE and our OSATE plugin

You will need an installed [OSATE](https://osate.org/about-osate.html)
(Open Source AADL Tool Environment) in order to use our OSATE plugin.
We have tested and verified our plugin works in OSATE 2.6.1, 2.7.0,
and 2.7.1; later versions may work as well and earlier versions may or
may not work since we haven't tested the current plugin version in
them.  Our OSATE plugin will function without the AGREE plugin for
OSATE, but you may want to install the AGREE plugin too so that you
can write both AGREE and VERDICT annexes when developing a model.

1. Download the [OSATE
   2.7.1](https://osate-build.sei.cmu.edu/download/osate/stable/2.7.1-vfinal/products/)
   product for your operating system.  (AGREE does not have a version
   that works with OSATE 2.8.0 yet.)

2. If necessary, refer to these [installation
   instructions](https://osate.org/download-and-install.html) to
   install OSATE.

3. Launch OSATE and navigate to Help > Install New Software...

4. Paste the following URL in the Work With: field and press the Enter
   key.  Select the AGREE item and then keep clicking Next through the
   rest of the wizard (you will have to accept the license agreement
   and install unsigned plugins) until you reach the end and click
   Finish.

`https://raw.githubusercontent.com/loonwerks/AGREE-Updates/master/agree_2.5.2`

5. Restart OSATE when prompted and navigate to Help > Install New
   Software... again.

6. Paste the following URL in the Work With: field and press the Enter
   key.  Select the VERDICT for OSATE item and then keep clicking Next
   through the rest of the wizard (you will have to accept the license
   agreement and install unsigned plugins) until you reach the end and
   click Finish.

`https://raw.githubusercontent.com/ge-high-assurance/VERDICT-update-sites/master/verdict-latest`

7. Restart OSATE when prompted.

## Install Docker (to run our back-end programs)

Our OSATE plugin has two ways to call our back-end programs.  Our
plugin can run an executable jar or it can run a Docker image in a
temporary container.  The latter way (running a Docker container) is
easier to use, but you need to install Docker on your operating system
first.  Note that we have pushed our verdict image to Docker Hub in
order to make the image available to anyone running both Docker and
our OSATE plugin.  You only have to tell our OSATE plugin which Docker
image you want to use; the plugin will look for the image in the local
Docker cache and then tell Docker to pull the image from Docker Hub if
necessary.

The instructions for installing Docker are operating system specific,
so you will have to read and follow the appropriate instructions for
your operating system:

- [Install Docker on
  Mac](https://docs.docker.com/docker-for-mac/install/)

- [Install Docker on
  Windows](https://docs.docker.com/docker-for-windows/install/)

- [Install Docker on
  Ubuntu](https://phoenixnap.com/kb/how-to-install-docker-on-ubuntu-18-04)
  
- [Install Docker on other Linux operating
  systems](https://docs.docker.com/install/)

If you are running Docker on Windows, you will have to do the
following things to allow our plugin to communicate with Docker:

1. Set an environment variable called DOCKER_HOST to the value
   "tcp://localhost:2375" to tell our plugin to connect to the daemon
   using the daemon's TCP port instead of the daemon's Unix file
   socket.

2. Go into Settings in Docker Desktop and enable the checkbox
   next to "Expose daemon on tcp://localhost:2375 without TLS".

3. Click on Resources under Settings, click on FILE SHARING under
   Resources, and add your drive letter to the list of directories
   which Docker can bind mount into containers.

4. Click the "Apply & Restart" button at the bottom to restart Docker
   after these changes.

If you want to check that Docker is installed and running correctly,
run the following command to verify that it prints a "Hello from
Docker!" message.

`docker run hello-world`

In addition, you can go into Docker's Settings panel and configure
Docker to use more resources if you want the back-end programs to run
more quickly.  Our back-end programs tend to be CPU-bound, not
memory-bound, so increasing the number of CPUs probably will be more
effective than increasing the memory.

## Install our back-end programs

Even if you use a Docker image, you still need to unpack the
"extern.zip" file containing our back-end programs and enter the
complete path to the "extern/STEM" folder when you configure our
plugin.  Here is how to configure our plugin to run either our Docker
image or our back-end programs:

1. Download our most recent VERDICT
   [release's](https://github.com/ge-high-assurance/VERDICT/releases)
   "extern.zip" file from GitHub.

2. Unpack the "extern.zip" file in a place of your choosing and
   remember the location of the "extern" folder you just unpacked (you
   will need it in later steps).

3. Launch OSATE, go to Window > Preferences > Verdict > Verdict
   Settings, and fill out the following fields.
   
4. Skip the "VERDICT Properties Name" field unless you are working on
   the DeliveryDrone model or another model which uses a differently
   named property set than "VERDICT_Properties".  If so, enter that
   property set's name into the field, e.g.,
   "CASE_Consolidated_Properties" for DeliveryDrone.

5. Click the Browse... button next to the "STEM Project PATH:" field,
   navigate to the "STEM" folder inside the "extern" folder, and make
   it the field's setting.

6. Click within the "Bundle Docker Image:" field, type
   "gehighassurance/verdict" for our Docker image's name, and save
   your changes by clicking the "Apply and Close" button.  If you
   don't want to run our Docker image, make sure this field stays
   empty and follow steps 6-11 instead.

7. Click the Browse... button next to the "Bundle Jar:" field,
   navigate to the "verdict-bundle-\<VERSION\>-capsule.jar" file
   inside the "extern" folder, and make it the field's setting.

8. Click the Browse... button next to the "Aadl2iml Binary:" field,
   navigate to the appropriate "aadl2iml" binary for your operating
   system inside either the "extern/mac" or "extern/nix" folders, and
   make it the field's setting.

9. Click the Browse... button next to the "Kind2 Binary:" field,
   navigate to the appropriate "kind2" binary for your operating
   system inside either the "extern/mac" or "extern/nix" folders, and
   make it the field's setting.

10. Click the Browse... button next to the "Soteria++ Binary:" field,
   navigate to the appropriate "soteria_pp" binary for your operating
   system inside either the "extern/mac" or "extern/nix" folders, and
   make it the field's setting.

11. Click the Browse... button next to the "GraphViz Path:" field,
   navigate to the folder on your operating system where your
   [GraphViz](https://www.graphviz.org/download/) Graph Visualization
   software is installed, and make it the field's setting.  On Linux,
   that location probably would be "/usr/bin".

12. Click the "Apply and Close" button to save all the settings that
   you just entered into the fields.

## Run our OSATE plugin

Now you are ready to run our OSATE plugin on an AADL model.  You can
pick one of the AADL models in our GitHub repository or the extern.zip
file you unpacked (for example,
"VERDICT/models/Thermostat_with_verdict_props" or
"extern/examples/DeliveryDrone") and use OSATE's "File > Import... >
General > Projects from Folder" wizard to import that model into your
OSATE workspace.  You can open the model's AADL files to see how that
model uses AGREE annexes, VERDICT annexes, and VERDICT properties.  If
OSATE asks you whether to convert the project to an Xtext project
after you open an AADL file, answer yes since the project must have a
Xtext nature to enable our OSATE plugin's functionality.

You can invoke our OSATE plugin's functionality from the Verdict
pulldown menu.  First click on a project's name in the AADL Navigator
pane to make it the currently selected project, then pull down the
Verdict menu and run MBAA or CRV as appropriate.  Our plugin will use
Docker Java API calls to pull down the appropriate Docker image from
Docker Hub (only if the image isn't already in your Docker's local
cache), start a temporary Docker container to run the MBAA or CRV
command, and then display any output from that command.  If you use
the executable jar instead of the Docker image, the plugin will run
the executable jar in a subprocess directly on your system.  Both the
temporary container and the executable jar will run several VERDICT
back-end tool chain programs in additional subprocesses inside the
temporary container or directly on your system as well.

For further information about how to analyze a model's system
architecture using our VERDICT tools, please read our VERDICT [User
Manual](https://github.com/ge-high-assurance/VERDICT/wiki/VERDICT-Modeling-Style-Guide-&-User-Manual:-V1-to-support-VERDICT-VM-19.1-Tool-Assessment-%233).

## Setting up CI/CD environment in GitHub Actions

This is a placeholder.
