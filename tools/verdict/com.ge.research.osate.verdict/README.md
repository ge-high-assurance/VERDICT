# VERDICT: An OSATE plugin for architectural and behavioral analysis of AADL models
 ----

## Installation Instructions:

**Dependencies:**

1. [Java 8](https://www.java.com/en/download/)

2. [Ocaml 4.07.1](https://ocaml.org/docs/install.html) or above on
   Linux or Mac OS

3. [GraphViz](https://www.graphviz.org/download/): Graph Visualization
   Software

4. [OSATE 2](https://osate-build.sei.cmu.edu/download/osate/stable/):
   AADL Tool Environment (The instructions are only tested on OSATE
   version 2.3.2 and 2.5.0. With OSATE 2.5.0, you need to manually
   install
   [AGREE](https://osate-build.sei.cmu.edu/download/osate/stable/2.3.7/updates/))

5. [Z3](https://github.com/Z3Prover/z3): The Z3 Theorem Prover (Make
   sure Z3 is installed under your system path, and the recommended
   version for Z3 is 4.7.1)

    * On Mac OS: brew install z3 (if you have installed brew)
    * On Ubuntu: sudo apt install z3

6. Copy the "extern" folder in the project to a location on your
   machine where you normally store binaries.

## Setup Instructions on MacOS

1. Create a plist file anywhere on your Mac called
   "com.ge.verdict.vars.plist" as follows:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>Label</key>
  <string>setenv.stem</string>
  <key>ProgramArguments</key>
  <array>
    <string>/bin/launchctl</string>
    <string>setenv</string>
    <string>GraphVizPath</string>
    <string>/path/to/the/dir/contains/dot</string>
    <!-- "dot" is a graph visualization tool; This path would probably look like /usr/local/bin -->
  </array>
  <key>RunAtLoad</key>
  <true/>
  <key>ServiceIPC</key>
  <false/>
</dict>
</plist>
```

2.  Modify the path to GraphVizPath in the plist file to reflect your
    system setup.

3. Move the plist file to /Library/LaunchDaemons

        mv /path/to/the/plist/file  /Library/LaunchDaemons/

4. Load the environment variables on terminal:

        launchctl load -w /Library/LaunchDaemons/com.ge.verdict.vars.plist

5. To test if you set the parameters successfully, type the following
   command on terminal and it should return the path you just set in
   the plist file. Similarly, you can test other paths.

        launchctl getenv ECLIPSE_SADL

6. Install the VERDICT plugin for OSATE
   * Open OSATE
   * Go to "Help -> Install New Software -> Add... -> Archive..." and find the plugin archive (com.ge.research.verdict.feature.zip) in the "plugin" folder of the VERDICT project
   * Make sure uncheck the "Group items by category" to be able to see the plugin
   * Finish the installation
   * Set VERDICT bundle path: Go to "OSATE -> Preferences -> Verdict -> Verdict Bundle". Browse to the "verdict-bundle-1.0.jar" in the extern folder to set the bundle jar path
   * In the same place, also set the STEM path to the extern/STEM folder, and then save and close

## Setup Instructions on Ubuntu

1. Open the environment variable setting script by typing the
   following on a termial (you may need to type your password):

        sudo -H gedit /etc/environment

2. Set environment variables for GraphVizPath and append the following
   text to the environment variables file (change the paths according
   to the locations of those apps on your platform):

`GraphVizPath="/usr/bin"`

3. Save it, logout Ubuntu and login again.

4. Install the VERDICT plugin for OSATE:
   * Open OSATE
   * Go to "Help -> Install New Software -> Add... -> Archive..." and find the plugin archive (com.ge.research.verdict.feature.zip) in the "plugin" folder of the VERDICT project
   * Make sure uncheck the "Group items by category" to be able to see the plugin
   * Finish the installation
   * Set VERDICT bundle path: Go to "Windows -> Preferences -> Verdict -> Verdict Bundle". Browse to the "verdict-bundle-1.0.jar" in the extern folder to set the bundle jar path
   * In the same place, also set the STEM path to the extern/STEM folder, and then save and close
