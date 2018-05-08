#!/usr/bin/env bash

set -e

## where are the parsing-gui and phenoscapeII directories
PARSING_GUI_BASE=parsing-gui
PARSING_GUI_JAR=${PARSING_GUI_BASE}/parsing-gui.jar
PHENOSCAPE_II_BASE=phenoscapeII
PHENOSCAPE_II_JAR=${PHENOSCAPE_II_BASE}/phenoscapeII.jar

## the name of the main class for the GUI application
MAIN_CLASS=fna.parsing.MainForm

## where to find the SWT jar(s)
SWT_CLASSPATH=${SWT_CLASSPATH:-${CLASSPATH}}

##################################################
## should not need to customize any of the below #
##################################################

echo "==> building ${PHENOSCAPE_II_BASE}"

echo "    o creating build directory if it doesn't exist yet"
if [ ! -d ${PHENOSCAPE_II_BASE}/classes ] ; then
    mkdir -p ${PHENOSCAPE_II_BASE}/classes
fi

echo "    o compiling java classes"
find ${PHENOSCAPE_II_BASE}/src -name "*.java" | xargs \
    javac -sourcepath ${PHENOSCAPE_II_BASE}/src \
          -d ${PHENOSCAPE_II_BASE}/classes \
          -encoding windows-1252 \
          -classpath "${PHENOSCAPE_II_BASE}/lib/*"

echo "    o copying other files into classes directory"
for f in log4j.properties XMLschemas ; do
    cp -p -R ${PHENOSCAPE_II_BASE}/src/main/java/$f ${PHENOSCAPE_II_BASE}/classes
done

echo "    o creating ${PHENOSCAPE_II_JAR}"
jar cf ${PHENOSCAPE_II_JAR} -C ${PHENOSCAPE_II_BASE}/classes .

echo "==> building ${PARSING_GUI_BASE}"

echo "    o creating build directory if it doesn't exist yet"
if [ ! -d ${PARSING_GUI_BASE}/classes ] ; then
    mkdir -p ${PARSING_GUI_BASE}/classes
fi

echo "    o compiling java classes"
find ${PARSING_GUI_BASE}/src -name "*.java" | xargs \
    javac -sourcepath ${PARSING_GUI_BASE}/src \
          -d ${PARSING_GUI_BASE}/classes \
          -encoding windows-1252 \
          -classpath "${SWT_CLASSPATH}:${PHENOSCAPE_II_JAR}:${PHENOSCAPE_II_BASE}/lib/*:${PARSING_GUI_BASE}/lib/*"

echo "    o copying other files into classes directory"
(cd ${PARSING_GUI_BASE}/src; \
    find . -type f ! -name *.java -exec cp -p "{}" ../classes/"{}" \; )

echo "    o creating ${PARSING_GUI_JAR}"
jar cfe ${PARSING_GUI_JAR} ${MAIN_CLASS} -C ${PARSING_GUI_BASE}/classes .

# done
echo "==> Done."
