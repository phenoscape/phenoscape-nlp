#!/usr/bin/env bash

## location of the parsing-gui jar and its dependencies
PARSING_GUI_JAR=../parsing-gui/parsing-gui.jar
PARSING_GUI_LIB=../parsing-gui/lib/*

## location of the phenoscapeII jar and its dependencies
PHENOSCAPE_II_JAR=../phenoscapeII/phenoscapeII.jar
PHENOSCAPE_II_LIB=../phenoscapeII/lib/*

## the name of the class to run the application
MAIN_CLASS=fna.parsing.MainForm

## the path for finding the MySQL JDBC driver jar
JDBC_CLASSPATH=${JDBC_CLASSPATH:-lib/mysql-connector-java-5.0.8-bin.jar}

## application-specific options to be passed to the JRE
##  -XstartOnFirstThread may be needed for SWT, depending on OS
##  -DentityExpansionLimit is used by the gui application
## you may also need to increase the heap size
JAVA_OPTIONS=-XstartOnFirstThread -DentityExpansionLimit=100000000

## the path to the SWT jar(s) must be either in SWT_CLASSPATH, or in CLASSPATH

##################################################
## should not need to customize any of the below #
##################################################

_CP=${PARSING_GUI_JAR}:${PARSING_GUI_LIB}:${PHENOSCAPE_II_JAR}:${PHENOSCAPE_II_LIB}

if [ -z "$CLASSPATH" ] ; then
    CLASSPATH=${_CP}
else
    CLASSPATH=${CLASSPATH}:${_CP}
fi
if [ -n ${SWT_CLASSPATH} ] ; then
    CLASSPATH=${CLASSPATH}:${SWT_CLASSPATH}
fi
if [ -n ${JDBC_CLASSPATH} ] ; then
    CLASSPATH=${CLASSPATH}:${JDBC_CLASSPATH}
fi

java $JAVA_OPTIONS -classpath ${CLASSPATH} ${MAIN_CLASS}
