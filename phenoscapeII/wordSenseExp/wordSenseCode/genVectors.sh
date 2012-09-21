#!/bin/bash
perl huge-count.pl --tokenlist --stop stoplist-nsp.regex . ./fishdesc
perl vector-input.pl fishindex1 fishmatrix1 complete-huge-count.output
