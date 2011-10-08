#!C:\Perl\bin\perl
package NounHeuristics;
#use lib 'C:\\Documents and Settings\\hongcui\\Desktop\\WorkFeb2008\\Projects\\workspace\\Unsupervised\\';
use Stemmer;
use ReadFile;

#$dir = "C:\\Docume~1\\hongcui\\Desktop\\WorkFeb2008\\Projects\\fossil\\";
#$outfile = "C:\\Docume~1\\hongcui\\Desktop\\WorkFeb2008\\Projects\\fossil-morphology-noun.txt";

#this program applies a simple heuristics to find nouns
#$STOP = "after|all|almost|also|and|amp|an|are|at|as|a|be|become|becomes|becoming|between|by|can|could|do|does|did|done|doing|have|had|has|how|here|from|if|in|it|its|is|was|were|be|been|being|less|may|might|more|not|of|often|or|on|so|some|sometimes|somewhat|should|soon|then|than|that|the|this|there|these|those|to|toward|towards|what|when|why|with|without|would";
#$STOP ="a|about|above|across|after|all|almost|along|also|amp|an|and|are|as|at|be|because|become|becomes|becoming|been|before|being|beneath|between|beyond|but|by|ca|can|could|did|do|does|doing|done|each|even|few|for|frequently|from|had|has|have|here|how|if|in|into|is|it|its|less|may|might|more|most|much|near|not|occasionally|of|off|often|on|onto|or|over|rarely|should|so|some|sometimes|somewhat|soon|than|that|the|then|there|these|this|those|throughout|to|toward|towards|up|upward|very|was|well|were|what|when|where|which|why|with|without|would";
#$STOP ="a|about|above|across|after|almost|along|also|although|amp|an|and|are|as|at|be|because|become|becomes|becoming|been|before|being|beneath|between|beyond|but|by|ca|can|could|did|do|does|doing|done|even|few|for|frequently|from|had|has|have|hence|here|how|if|in|into|inside|inward|is|it|its|less|may|might|more|most|much|near|no|not|occasionally|of|off|often|on|onto|or|out|outside|outward|over|rarely|should|so|sometimes|somewhat|soon|than|that|the|then|there|these|this|those|throughout|to|toward|towards|up|upward|very|was|well|were|what|when|where|which|why|with|within|without|would";
$STOP ="a|about|above|across|after|along|also|although|amp|an|and|are|as|at|be|because|become|becomes|becoming|been|before|behind|being|beneath|between|beyond|but|by|ca|can|could|did|do|does|doing|done|for|from|had|has|have|hence|here|how|if|in|into|inside|inward|is|it|its|may|might|more|most|near|no|not|of|off|on|onto|or|out|outside|outward|over|should|so|than|that|the|then|there|these|this|those|throughout|to|toward|towards|up|upward|was|were|what|when|where|which|why|with|within|without|would";

#heurnouns($dir, $outfile);

sub heurnouns{
	my ($dir, $outfile) = @_;
	%WORDS = ();
	@NOUNS = ();
 
	#$NENDINGS = "\\w\\w(?:tion|ness|ism|ist|ment|ance|ancy|ence|ency|sure)\\b";
	$NENDINGS = "\\w\\w(?:ist|sure)\\b";
	#$VENDINGS = "(?:ed|ing)\\b";
	$VENDINGS = "(?:ing)\\b";
	
	$SENDINGS = "(?:on|is|ex|ix|um|us|a)\\b";
	$PENDINGS = "(?:ia|es|ices|i|ae)\\b";
	
	#collect all words
	opendir(IN, "$dir") || die "$!:$dir\n";
	while(defined ($file = readdir(IN))){
  		if($file !~ /\w/){next;}
  		$content = ReadFile::readfile("$dir$file");
  		addwords(strip($content));
  		#print keys(%WORDS)."words\n";
	}

	#solve the problem: septa and septum are both s
	for(my $i = 0; $i < @NOUNS; $i++){
		if($NOUNS[$i]=~/a\[s\]/){
			my $sn = $NOUNS[$i];
			$sn =~ s#\[s\]##;
			$sn =~ s#a$#um#;
			if($WORDS{$sn} == 1){
				$NOUNS[$i]=~ s#\[s\]#\[p\]#;
			}
		}
	}
	
  	#find nouns
  	@wordlist = sortwords(); #put words with the same roots in one array element
  	findnouns(@wordlist);
	#dump to file
	if($outfile =~/\w/){
		open(OUT, ">$outfile") || die "$!:$outfile\n";
		foreach $n (@NOUNS){
			print OUT $n."\n";
		}
	}
	return @NOUNS;
}

sub strip{
	my $content = shift;
	$content =~ s#<(([^ >]|\n)*)># #g; #<...> and </...> too simple?
	$content =~ s#<\?[^>]*\?># #; #<? ... ?>
	$content =~ s#&[^ ]{2,5};# #g; #remove &nbsp;
	$content =~ s#\s+# #g;
	return $content;
}

#fix 1547  'æhedgedÆ'
#1548  'ôbranchesö'
#1549  'ôconesö'
#1550  'ôdoublesö'
#Dec 8, 2008: Hong
sub addwords{
	my $content = shift;
	$content = lc $content;
	
	presentabsent($content); #4/7/09
	
	$content =~ s#\W# #g;
	my @tokens = split(/\s+/,$content);
	foreach $t (@tokens){
	  if($t !~ /\b(?:$STOP)\b/ && $t =~/\w/ && $t !~ /\d/ && length $t > 1){
		$WORDS{$t}=1; #check to make sure new $t replaces old $t
		#print $WORDS{$t};
	  }
	}
}

#any word preceeding "present"/"absent" would be a n
#4/7/09
my $pachecked = "and|or|to";
sub presentabsent{
	my $text = shift;
	if($text =~ /(\w+?)\s+(present|absent)/){
		my $n = $1;
		if($n !~ /\b($pachecked)\b/ && $n!~/\b($STOP)\b/ && $n !~/\b(always|often|seldom|sometimes|[a-z]+ly)\b/){
			print "present/absent [$n]\n";
			
			if(($n =~/$PENDINGS/ or $n =~/[^s]s$/ or $n =~ /teeth/) and $n !~/$SENDINGS/){
				push(@NOUNS, $n."[p]");
			}else{
				push(@NOUNS, $n."[s]");
			}
			$pachecked .= "|$n";
		}
	}
}
sub sortwords{
#sort words in %WORDS, return an array. 
#Words with the same root become an array element.
	my @words = keys(%WORDS);
	my @sorted = sort @words;
#	foreach (@sorted){
#			print $_."\n";
#	}
	my @array = ();
	my $element = $array[0]." ";
	my $t1 = 0;
	my $t2 = 0;
	for($i = 0; $i < @sorted; $i++){
		$t1 = length $sorted[$i];
		$t2 = length $sorted[$i+1];   		 
		#if($i+1 < @sorted && abs($t1 - $t2) < 3 && sameroot($sorted[$i], $sorted[$i+1])==1 ){
		if($i+1 < @sorted && sameroot($sorted[$i], $sorted[$i+1])==1 ){
			$element .= $sorted[$i+1]." ";
		}else{
			$element =~ s#^\s+##g;
			$element =~ s#\s+$##g;
			push(@array, $element);
			$element = $sorted[$i+1]." ";
		}
	}
	return @array;
}

sub sameroot{
#return false if w1 and w2 do not share the same root
#otherwise return true;
	my ($w1, $w2) = @_;
	$w1 = Stemmer::stem($w1);
	$w2 = Stemmer::stem($w2);
	if($w1 =~/$w2/ || $w2=~/$w1/){
		return 1;
	}else{
		return 0;
	}
}

sub findnouns{
	my @array = @_; #each element is a list of sorted word sharing the same root
	my @list = ();
	my $single = "";
	my $plural = "";
	foreach $l (@array){
	   if($l =~ /$NENDINGS/){ #containing words with noun endings
	   		 push(@NOUNS, getnouns($l));
	   }elsif($l !~ /$VENDINGS/ && $l =~ /\s/){#at least two words without verb endings
			 my @pairs = getsingular($l); #one $l could hold > 1 pair of singular/plural words
			 foreach my $p (@pairs){
			 	($singular, $plural) = split(/\s+/, $p); 
			 	if($singular ne ""){
			 		push(@NOUNS, $singular."[s]",$plural."[p]" );
			 	}
			 }
       }
	}
}

sub getnouns{
	my $l = shift;
	my @n = ();
	my @list = split(/\s+/, $l);
	@list = sort bylength @list;
	my $string, $tmp;
	foreach (@list){
	  if($_ =~ /$NENDINGS/){
	  	push(@n, $_."[s]");
		$string .= $_." ";
	  }else{
	    $tmp = $_;
	    $tmp =~ s#(s|es)$##;
		if($string =~ /\b$tmp\b/){
		 push(@n, $_."[p]");
		}
	  }
	}
	return @n;	
}

#Nouns ending in -on becoming -a: phenomenon => phenomena *shorter
#Nouns ending in -is becoming -es in plural: analysis =>analyses *same
#Nouns ending in -ex, -ix becoming plural -ices: apex =>apices
#Nouns ending in -um with plural -a: datum => data *shorter
#Nouns ending in -us with plural -i: alumnus =>alumni *shorter
#Nouns ending in -us with plural -a (only in technical use): copus => copora
#Nouns ending in -a with plural -ae: alga => algae 

#$l could be "late later lateral laterally laterals". Dec 8, 2008, Hong
#return (singular, plural)
sub getsingular{
	my $l = shift;
	my (@pairs);
	my @list = split(/\s+/, $l);
	if(@list == 1){ #one word in the list
		push(@pairs, " ");
		return @pairs;
	}
	if(@list == 2){#two words in the list
		push(@pairs, singularpluralpair(@list));
		return @pairs;
	}
	for(my $i = 0; $i < @list; $i++){ #> 2 words in the list, do it pair by pair
		for(my $j = $i+1; $j < @list; $j++){
			$pair = singularpluralpair($list[$i], $list[$j]);
			push(@pairs, $pair);
		}
	}
	return @pairs;
}

sub singularpluralpair{
	my @list = @_;
	@list = sort bylength @list;
	my ($singular, $plural);
	my $l = join(" ", @list);
	my $len0 = length($list[0]);
	my $len1 = length($list[1]);
	#special cases
	if($l =~ /$SENDINGS/ && $l =~ /$PENDINGS/){
		if($list[0]=~/es$/ && $list[1]=~/is$/ && abs($len0-$len1) ==0){ $singular = $list[1]; $plural = $list[0];}  
        elsif($list[0]=~/a$/ && $list[1]=~/on$/  && abs($len0-$len1) <2){$singular = $list[1]; $plural = $list[0];}
		elsif($list[0]=~/a$/ && $list[1]=~/um$/  && abs($len0-$len1) <2){$singular = $list[1]; $plural = $list[0];} 
		elsif($list[0]=~/i$/ && $list[1]=~/us$/ && abs($len0-$len1) <2){$singular = $list[1]; $plural = $list[0];} 
		elsif($list[0]=~/a$/ && $list[1]=~/us$/ && abs($len0-$len1) <2){$singular = $list[1]; $plural = $list[0];}
	}else{
		if($list[1] =~ /s$/){ #thicker, thickness; species, specimens; tomentulose, tomentulous; later laterals
			#$list[1] has no other letters except those appearing in $list[0] or ies
			#and vice versa.
			if($list[1] !~/[^$list[0]yies]/  && $list[0] !~/[^$list[1]yies]/ && abs($len0-$len1) >0 && abs($len0-$len1) <3) {		
			  $singular = $list[0]; 
			  $plural = $list[1];
			}
		}
	}
	return $singular." ".$plural;		
}

sub bylength{
	my $la = length $a;
	my $lb = length $b;
	if($la < $lb ){
		return -1;
	}elsif($la == $lb){
		return 0;
	}elsif($la > $lb){
		return 1;
	}
}

1;