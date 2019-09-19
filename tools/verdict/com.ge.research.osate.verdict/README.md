# VERDICT: An OSATE plugin for architectural and behavioral analysis of AADL models
 ----

## Installation Instructions: 

**Dependencies:**

1. [Java 8](https://www.java.com/en/download/)

2. [Ocaml 4.07.1](https://ocaml.org/docs/install.html) or above on Linux or Mac OS

3. [GraphViz](https://www.graphviz.org/download/): Graph Visualization Software

4. [OSATE 2](https://osate-build.sei.cmu.edu/download/osate/stable/): AADL Tool Environment (The instructions are only tested on OSATE version 2.3.2 and 2.5.0. With OSATE 2.5.0, you need to manually install [AGREE](https://osate-build.sei.cmu.edu/download/osate/stable/2.3.7/updates/))

5. [SADL](https://github.com/crapo/sadlos2): Semantic Application Design Language

    * Install a version of [eclipse (neon preferably)](https://www.eclipse.org/downloads/packages/release/neon/3). Choose either "IDE for Java Developers" or "IDE for Java and ISL Developers."

    * Go to Help -> Install New Software -> Add... -> Archive... -> Find the SADL archive (com.ge.research.sadl-3.2.0.201903111350.zip) in the "extern" folder and finish the installation.

6. [Z3](https://github.com/Z3Prover/z3): The Z3 Theorem Prover (Make sure Z3 is installed under your system path, and the recommended version for Z3 is 4.7.1)

    * On Mac OS: brew install z3 (if you have installed brew)
    * On Ubuntu: sudo apt install z3

7. Copy the "extern" folder in the project to a location on your machine where you normally store binaries.

**Note:** to set the value for SADL\_DEFAULT\_WORKSPACE below, you need to make sure the value is the default workspace path used by the eclipse with SADL installation. To check the default workspace of the eclipse with SADL, you can open the eclipse, and normally a pop-up window will show up to ask user to choose workspace path. The path is the value you should set for the SADL\_DEFAULT\_WORKSPACE. If you don't see a pop-up window while opening an eclipse, another way to check the default workspace of an Eclipse is to go to "File->Switch Workspace". You should see a path there.

## Setup Instructions on MacOS

1. Create a plist file anywhere on your Mac called "com.ge.verdict.vars.plist" as follows:  
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
    <string>ECLIPSE_SADL</string>
    <string>/path/to/eclipse/application/dir</string>
    <!-- This path would probably look like /Users/xxx/eclipse-neon/Eclipse.app/Contents/MacOS/eclipse  -->        
    <string>/bin/launchctl</string>
    <string>setenv</string>
    <string>SADL_DEFAULT_WORKSPACE</string>
    <string>/path/to/the/default/workspace/for/your/sadl/eclipse</string>    
    <!-- Check your SADL eclipse to find out -->        
    <string>/bin/launchctl</string>
    <string>setenv</string>
    <string>GraphVizPath</string>
    <string>/path/to/the/dir/contains/dot</string>    
    <!-- "dot" is a graph visualization tool; This path would probably look like /usr/local/bin -->  
    <string>/bin/launchctl</string>
    <string>setenv</string>
    <string>VERDICT_EXTERN</string>
    <string>/path/to/the/extern/dir</string>  
    <!-- This path would probably look like /xxx/extern  -->  
  </array>
  <key>RunAtLoad</key>
  <true/>
  <key>ServiceIPC</key>
  <false/>
</dict>
</plist>
```
2.  Modify the paths to ECLIPSE\_SADL, SADL\_DEFAULT\_WORKSPACE, GraphVizPath and VERDICT\_EXTERN in the plist file to reflect your system setup.

3. Move the plist file to /Library/LaunchDaemons 

        mv /path/to/the/plist/file  /Library/LaunchDaemons/

4. Load the environment variables on terminal:

        launchctl load -w /Library/LaunchDaemons/com.ge.verdict.vars.plist


5. To test if you set the parameters successfully, type the following command on terminal and it should return the path you just set in the plist file. Similarly, you can test other paths.

        launchctl getenv ECLIPSE_SADL

6. Install the VERDICT plugin for OSATE  
   * Open OSATE  
   * Go to "Help -> Install New Software -> Add... -> Archive..." and find the plugin archive (com.ge.research.verdict.feature.zip) in the "plugin" folder of the VERDICT project  
   * Make sure uncheck the "Group items by category" to be able to see the plugin  
   * Finish the installation  


## Setup Instructions on Ubuntu

1. Open the environment variable setting script by typing the following on a termial (you may need to type your password): 

        sudo -H gedit /etc/environment

2. Set environment variables for ECLIPSE\_SADL, STEM, GraphVizPath and VERDICT\_EXTERN and appending the following text to the envrionment variables file (change the paths according to the locations of those apps on your platform)  
`ECLIPSE_SADL="/path/to/the/eclipse/app/with/sadl/installed (the application not the folder)"`  
`SADL_DEFAULT_WORKSPACE="/path/to/the/default/workspace/for/your/sadl/eclipse"`  
`GraphVizPath="/usr/bin"`  
`VERDICT_EXTERN="/path/to/the/extern/folder"`

3. Save it, logout Ubuntu and login again.

4. Install the VERDICT plugin for OSATE:  
   * Open OSATE  
   * Go to "Help -> Install New Software -> Add... -> Archive..." and find the plugin archive (com.ge.research.verdict.feature.zip) in the "plugin" folder of the VERDICT project  
   * Make sure uncheck the "Group items by category" to be able to see the plugin  
   * Finish the installation  