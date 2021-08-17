# VERDICT: Running STEM queries on CSV data

## About verdict-stem-runner

[OSATE](https://osate.org/about-osate.html) is an Open Source AADL
Tool Environment based on the Eclipse Modeling Tools IDE.  The VERDICT
tools consist of a VERDICT plugin for OSATE and a set of VERDICT
back-end programs invoked by the plugin.

This directory builds a program called verdict-stem-runner which runs
STEM queries on CSV data.  This program is usually called from another
program called verdict-bundle, but you can run this program by itself
using an executable capsule jar
(target/verdict-stem-runner-\<VERSION\>-capsule.jar) if you want.
The only argument the capsule jar needs is the path to the
[STEM](../../STEM) project on disk.  This program will load semantic
modeling and reasoning rules from the STEM/OwlModels directory, read
data from input CSV files in the STEM/CSVData directory, run STEM
queries on the CSV data, and write the queries' output to CSV files in
the STEM/Output directory and SVG graph files in the STEM/Graphs
directory.

The STEM queries are written in
[SADL](https://github.com/SemanticApplicationDesignLanguage/sadl), the
Semantic Application Design Language.  SADL is an English-like
language for building semantic models and authoring rules.  You can
edit SADL files and translate them to OWL, the Web Ontology Language
used by semantic reasoners and rule engines, with the SADL Integrated
Development Environment (SADL IDE), an Eclipse plug-in packaged as a
zip file that can be downloaded from SADL's
[Releases](https://github.com/SemanticApplicationDesignLanguage/sadl/releases)
page.  However, you won't need to use your own SADL IDE unless you
intend to change some SADL files since we already have translated the
STEM project's SADL files to OWL files in the STEM/OwlModels
directory.  Our verdict-stem-runner program loads all the semantic
modeling and reasoning rules and queries it needs from that
STEM/OwlModels directory and it calls some SADL libraries to do the
same thing that opening the STEM/Run.sadl file in the SADL IDE and
running the SADL IDE's "Test Model" command does: run the STEM queries
and write their output to files.  The output files have only a few
small differences; the SADL IDE writes quotes around numbers such as
"1.0" in its output and leaves an empty line at the end of its output
CSV files, neither of which the verdict-stem-runner program does.

## Note the use of our Maven snapshot repository

GE Global Research has open sourced SADL but the SADL libraries called
by verdict-stem-runner (reasoner-api, reasoner-impl, sadlserver-api,
and sadlserver-impl) have not been officially released and put into
the Maven central repository.  We have built the SADL libraries and
put them into another git repository
([sadl-snapshot-repository](https://github.com/ge-high-assurance/sadl-snapshot-repository))
to make these SADL libraries available when we build
verdict-stem-runner.  We have added a repositories section to the pom
to tell Maven to download these SADL libraries from our Maven snapshot
repository.

## Update VerdictStem.java when you change STEM/Run.sadl

The [STEM](../../STEM) project stores the STEM queries in the
STEM/Queries.sadl file and the SADL commands that run these queries in
the STEM/Run.sadl file.  The queries in the STEM/Queries.sadl file are
copied to a translated OWL file which our verdict-stem-runner program
loads from the STEM/OwlModels directory.  However, the SADL commands
in the STEM/Run.sadl are not copied to a translated OWL file because
they have no representation in OWL.  Therefore, we have to write
hardcoded SADL API calls in our Java source file
[VerdictStem.java](src/main/java/com/ge/verdict/stem/VerdictStem.java)
which do the same thing as the SADL commands in the
[STEM/Run.sadl](../../STEM/Run.sadl) file.  You can change almost any
of the SADL files in the STEM project (even STEM/Queries.sadl) and
simply rebuild the STEM project in the SADL IDE (which updates the
translated OWL files in the OwlModels directory) without needing to
make any changes to VerdictStem.java.  However, if you make a
significant change to STEM/Run.sadl, you will have to update
VerdictStem.java likewise so that the verdict-stem-runner program will
continue to do the same things the SADL IDE does when it executes the
commands in the STEM/Run.sadl file.

## Make sure the unit test continues to pass after changes

The unit test copies our verdict-back-ends' STEM directory to its own
target/test-classes/STEM directory before running the STEM queries so
that it can compare its own "test" output files to the "control"
output files in the verdict-back-ends' STEM directory.  The unit test
fails if there are any differences between the test and control output
files.  If you change the SADL files and run the SADL IDE on the
verdict-back-ends' STEM project to generate new OWL and output files,
the unit test will fail because the SADL IDE generates very slighly
different CSV output files such as quotes around numbers like "1.0"
and an empty line at the end of the CSV output files.  The easiest way
to remove these differences is to run the unit test, let it fail, and
copy the new CSV output files from target/test-classes/STEM/Output to
verdict-back-ends/STEM/Output; then the unit test will find no
differences when you run it again.

## How to make changes to columns in input CSV files

Here is the normal procedure for adding new columns or making other
changes to the input CSV files:

- Update the STEM/Templates files in the SADL IDE to handle the new
  columns or other changes
  
- Update any other SADL files in the SADL IDE and rebuild the STEM
  project as necessary

- Create new CSV files in the STEM/CSVData directory 

- Run "Test Model" on STEM/Run.sadl to verify the changes work in the
  SADL IDE

- Ensure the verdict-back-ends' STEM directory has the same changes as
  the SADL IDE's STEM project including the new input CSV files,
  modified SADL and template files, and translated OWL files

- Expect that the verdict-stem-runner unit test will fail; confirm
  that only known minor differences (quotes, empty line, etc.) exist

- Replace the files in the verdict-back-ends' STEM/Output directory
  with output files generated by verdict-stem-runner to make the
  differences go away

- Commit all modified files to the VERDICT git repository
