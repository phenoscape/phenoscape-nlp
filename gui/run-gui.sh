#!/usr/bin/env bash

PARSING_GUI_JAR=../parsing-gui/parsing-gui.jar
PARSING_GUI_LIB=../parsing-gui/lib/*
PHENOSCAPE_II_JAR=../phenoscapeII/phenoscapeII.jar
PHENOSCAPE_II_LIB=../phenoscapeII/lib/*
MAIN_CLASS=fna.parsing.MainForm
JDBC_CLASSPATH=${JDBC_CLASSPATH:-lib/mysql-connector-java-5.0.8-bin.jar}

JAVA_OPTIONS=-XstartOnFirstThread

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
