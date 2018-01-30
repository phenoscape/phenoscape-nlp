#!/usr/bin/env bash

BUILDDIR=build
CLASSESDIR=${BUILDDIR}/classes
TARGETJAR=gui/SemanticCharaParser.jar
PARSING_GUI_SRC=parsing-gui/src
PHENOSCAPE_II_SRC=phenoscapeII/src
JAVA_LIB_DIR=gui/SemanticCharaParser_lib
MAIN_CLASS=fna.parsing.MainForm

# create buil directory if it doesn't exist yet
echo "==> creating build directory if it doesn't exist yet"
if [ ! -d $CLASSESDIR ] ; then mkdir -p $CLASSESDIR ; fi

# compile all java source files to the classes build directory
echo "==> compiling java classes"
find . -name "*.java" | xargs \
    javac -sourcepath ${PARSING_GUI_SRC}:${PHENOSCAPE_II_SRC} \
          -d ${CLASSESDIR} \
          -encoding windows-1252 \
          -classpath "${JAVA_LIB_DIR}/*"

# copy additional files for packaging into the final jar
echo "==> copying other files into build directory"
(cd ${PARSING_GUI_SRC}; \
    find . -type f ! -name *.java -exec cp -p "{}" ../../${CLASSESDIR}/"{}" \; )
for f in log4j.properties XMLschemas ; do
    cp -p -R ${PHENOSCAPE_II_SRC}/$f ${CLASSESDIR}
done

# create final jar file
echo "==> creating ${TARGETJAR}"
jar cfe ${TARGETJAR} fna.parsing.MainForm -C ${CLASSESDIR} .

# done
echo "==> Done."
