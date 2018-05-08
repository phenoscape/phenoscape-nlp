# Semantic Charaparser - How to install and run

This document includes instructions for building and running the Semantic Charaparser (SCP) GUI application, in the version used for the following manuscript (specifically, for the machine-generated phenotype annotations):

> Dahdul et al (2018) Annotation of phenotypes using ontologies: a Gold Standard for the training and evaluation of natural language processing systems. BioRxiv XXXXXX. Submitted to Database.

## Prerequisites

Building and running the Semantic Charaparser GUI application requires the following to be installed.

### System and software
* Java 1.7+ JDK (for compiling Java classes, as is required on Unix and MacOSX) and JRE (for running the application). The corresponding executables (`javac` and `java`, respectively) must be in your path.
* [SWT] (Standard Widget Toolkit) v4.x for your operating system. For Unix and MacOSX, the path to the SWT jar(s) must be in Java's `CLASSPATH` environment, or in the `SWT_CLASSPATH` environment.
* JDBC driver for MySQL 5.x. A pure-Java one is included in this repository, but your OS may require an OS-specific binary.
* Perl v5.16+. For Windows, [Strawberry Perl].
* MySQL v5.x, and a database user with full DML privileges.

### Data

* [WordNet]. For the results reported in the manuscript, we used WordNet v2.1 for Windows; the corresponding (in terms of corpus) version for Unix is v3.0.
* Ontologies, character state descriptions, and pre-computed content to be imported into the MySQL database. See the following archive:
    > Dahdul et al (2018) Gold standard corpus, ontologies, and Entity-Quality ontology annotations for evolutionary phenotypes. Zenodo. <https://doi.org/10.5281/zenodo.1217594>

## Building the application

Prepare as following, then proceed according to your operating system.
* Adjust log file name as appropriate in the following properties files:
    - `./parsing-gui/src/log4j.properties`
    - `./phenoscapeII/src/main/java/log4j.properties`

### Unix and MacOSX

Run the `./build.sh` script. This will produce the following.
- `./phenoscapeII/classes/` and `./parsing-gui/classes/`. These hold intermediary build products and can be deleted.
- `./phenoscapeII/phenoscapeII.jar`: the library component of the GUI application
- `./parsing-gui/parsing-gui.jar`: the GUI component of the GUI application

If you receive errors about classes `Button`, `Text` and others not being found, you most likely forgot to install SWT, or to give its location in the `SWT_CLASSPATH` or `CLASSPATH` environment variables.

## Running the application

Prepare as following, then proceed according to your operating system.
* Ensure that the MySQL server you will be using is running, and
    - has a database created for this application (e.g., `CREATE DATABASE goldstandard;`),
    - has a user with connection and full DML privileges for this database,
    - and that the SQL dump (see above) has been loaded into this database.
* Edit the file `./gui/application.properties` with the values appropriate for your setup. This includes the connection parameters for MySQL.

### Unix and MacOSX

Change directory to `./gui/`, then run the script `./run-gui.sh`:

    cd gui
    ./run-gui.sh
 
You must have the MySQL JDBC driver (if not using the one included here) and the SWT jar(s) in your `JDBC_CLASSPATH` and `SWT_CLASSPATH`, or in the `CLASSPATH` environment variables.

[SWT]: https://www.eclipse.org/swt/
[Strawberry Perl]: http://strawberryperl.com/
[WordNet]: https://wordnet.princeton.edu