# VERDICT: Calling the VERDICT back-end programs

## About the VERDICT back-end programs

[OSATE](https://osate.org/about-osate.html) is an Open Source AADL
Tool Environment based on the Eclipse Modeling Tools IDE.  The VERDICT
tools consist of a VERDICT plugin for OSATE and a set of VERDICT
back-end programs invoked by the plugin.  Our VERDICT plugin sources
are in the [(../verdict)](../verdict) directory and our Java back-end
program sources are in the following subdirectories:

- [verdict-assurance-case](verdict-assurance-case) builds an assurance case
- [verdict-attack-defense-collector](verdict-attack-defense-collector) collects some attack defenses
- [verdict-blame-assignment](verdict-blame-assignment) prepares some blame assignments
- [verdict-bundle-app](verdict-bundle-app) provides an executable jar which can call any of the back-end programs
- [verdict-crv](verdict-crv) analyzes a model's behavior
- [verdict-instrumentor](verdict-instrumentor) instruments a model for analysis
- [verdict-lustre-translator](verdict-lustre-translator) translates a model from VDM to Lustre
- [verdict-mbas-translator](verdict-mbas-translator) analyzes a model's architecture
- [verdict-merit-assignment](verdict-merit-assignment) prepares merit assignments
- [verdict-stem-runner](verdict-stem-runner) runs STEM queries
- [verdict-synthesis](verdict-synthesis) synthesizes defenses
- [verdict-test-instrumentor](verdict-test-instrumentor) tests an instrumented model
- [z3-native-libs](z3-native-libs) encapsulates z3 native libraries

## Build the Java back-end programs

Please see the parent [README](../README.md) for build instructions.
