cd "C:\stanford-parser-2010-08-20"
C:
java -mx900m -cp "stanford-parser.jar;" edu.stanford.nlp.parser.lexparser.LexicalizedParser -sentences newline -tokenized -tagSeparator / englishPCFG.ser.gz  "C:\DEMO\FNA-v19-excerpt\target\fna_v19_test_posedsentences.txt" >C:\DEMO\FNA-v19-excerpt\target\fna_v19_test_parsedsentences.txt 2<&1
	  		