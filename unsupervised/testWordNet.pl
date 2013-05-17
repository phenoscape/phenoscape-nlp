      #use WordNet::QueryData;

      #my $wn = WordNet::QueryData->new;

      #print "Synset: ", join(", ", $wn->querySense("cat#n#7", "syns")), "\n";
      #print "Hyponyms: ", join(", ", $wn->querySense("cat#n#1", "hypo")), "\n";
      #print "Parts of Speech: ", join(", ", $wn->querySense("run")), "\n";
      #print "Senses: ", join(", ", $wn->querySense("run#v")), "\n";
      #print "Forms: ", join(", ", $wn->validForms("lay down#v")), "\n";
      #print "Noun count: ", scalar($wn->listAllWords("noun")), "\n";
      #print "Antonyms: ", join(", ", $wn->queryWord("dark#n#1", "ants")), "\n";
      
      #print "Instances: ", join(", ", $wn->querySense("color#n")), "\n";
      #print "Parts of Speech: ", join(", ", $wn->querySense("petals")), "\n";

      $word = "abaxially";
      $command = "wn $word -over";
      $result = `wn $word -over`;
      $result =~ s#\n##g;
      $pos = "";
      while($result =~/.*?Overview of ([a-z]*) (.*)/){
         $t = $1;
         $result = $2;
         $pos .= "n" if $t eq "noun";
         $pos .= "v" if $t eq "verb";
         $pos .= "a" if $t eq "adj";
         $pos .= "r" if $t eq "adv";

      }
      print $pos;
