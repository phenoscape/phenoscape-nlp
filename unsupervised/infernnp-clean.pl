#!C:\Perl\bin\perl
use XML::DOM;
#use lib 'U:\\Research\\Projects\\unsupervised\\';
use nounheuristics;
use initialmarkup;
use SentenceSpliter;
use ReadFile;

#infer NNP from collection of initally marked descriptions
#mark up descriptions with new tags.
$dir = "U:\\Research\\Data-Flora\\FNA\\test\\";#textfile
$markeddir = "U:\\Research\\Data-Flora\\FNA\\test-unsupervised\\";
$seedsfile = "U:\\Research\\Data-Flora\\FNA\\seednouns.txt";#line: noun1[spn]
$hnouns = "U:\\Research\\Data-Flora\\FNA\\learntnouns.txt";
$debug = 0;

#sentence format 1{Basal leaves absent}Basel leaves absent .<d1-Basel leaves>
#1:sentenceid, {}:leading N words, <d1:decisionID, Basal leaves>: tag
%SENTS = (); #sentences found in files in $dir
$CHECKEDWORDS = ":"; #leading three words of sentences
$N = 3; #$N leading words
%DESICIONS = ();
$SENTID = 0;
$DECISIONID = 0;
$NEWDESCRIPTION =""; #record the index of sentences that ends a description
@START = (); #SENTs for sentences starting with single pl words.

if(! -e $markeddir){
system("mkdir", $markeddir);
print "$markeddir created\n" if $debug;
}

@nouns1 = NounHeuristics::heurnouns($dir, $hnouns);
print "nouns learnt from heuristics:\n@nouns1\n" if $debug;

@nouns = (); #holds the seed nnps and nnps discovered later
if($seedsfile ne ""){
	open (N, "$seedsfile") || die "$!: $seedsfile\n";
	while(<N>){
		if(/\w/){
			s#[\n\r]##g;
			push(@nouns,$_);
			print "add $_ from seeds to noun list\n" if $debug;
  		}
	}
	close(N);
}
push(@nouns, @nouns1);#nouns
%BDRY = (); #boundary words
@stops = split(/\|/,$NounHeuristics::STOP);
print "stop list:\n@stops\n" if $debug;

for (@stops){
	$BDRY{$_} = "1/1"; #certainty NumOfThisDecision/NumOfTotalDecision
}
#print "@{[ %hash ]}\n";
#print map { "$_ => $hash{$_}\n" } keys %hash;
print "boundary words:\n";
print map{"$_=> $BDRY{$_}\n"} keys %BDRY;
%NOUNS = ();
foreach $n (@nouns){#convert to hash
	if($n =~ /\w/){
    	#note: what if the same word has two different pos?
		@ns = split(/\|/,$n);
		foreach $w (@ns){
			if($w =~ /(\w+)\[([spn])\]/){
				  $NOUNS{lc $1} = $2."1/1"; #sigular or plural, certainty,
				  #and role (""unknown, -main noun, _modifier, , +both)
			}
		}
	}
}
print "nouns in hash:\n" if $debug;
print map{"$_=> $NOUNS{$_}\n"} keys %NOUNS if $debug;


###############################################################################
# bootstrap between %NOUNS and %BDRY on plain text description
# B: a boundary word, N: a noun or noun *phrase* ?: a unknown word
# goal: grow %NOUNS and %BDRY + confirm tags
#
# decision table: leading three words (@todo: exclude "at the center of xxx")
# foremost, collect the patterns and find the number of unique instances (I) of "?"
# deal with the cases with good hints first
# leave the cases with high uncertainty for the next iteration
# use the NNP as the tag "flower buds" "basal leaves"
# with the assumption that "N N N"s are rare, "N N" and "N" are most common

# clues: ?~pl, tag words' POS patterns
# N <=>B [last N (likely to be a pl) is followed by a B, the first B is proceeded by a N]
# need to distinguish the two usages of Ns, [1] when used as the main N in a tag e.g. <female flowers>
# [2] when used to modify another N in a tag <flower buds>
# if [1] is seen, then <flower> shouldn't be a tag.
# cases: see *rulebasedlearn*  for heuritics
# 1  N N N => make "N N N" the tag
# 2    N B => make "N N" the tag
# 3    N ? => if I>2 or "N N" is a phrase or N2-pl, ? -> %BDRY and make "N N" the tag
# 4    B N => make "N1" the tag
# 5    B B => make N the tag
# 6    B ? => make N the tag
# 7    ? N => if I>2 or N1-pl, ? -> %BDRY and make N1 the tag; else @nextiteration
# 8    ? B => if N-pl, ? -> %BDRY and make N the tag; if I>2 and N1-sg and ?~pl, ? -> %NOUNS and make "N ?" the tag; else @nextiteration
# 9    ? ? => if N-pl, ? -> %BDRY and make N the tag; if any ?~pl, make up to the ? the tag and that ?->@NOUNS; else @nextiteration
# 10 B N N => make "B N N" the tag
# 11   N B => make "B N" the tag
# 12   N ? => if N-pl, ? -> %BDRY and make "B N" the tag; if ?~pl, make "B N ?" the tag and ?->@NOUNS;else @nextiteration
# 13   B N => make "B B N" the tag
# 14   B B => search for the first N and make it the tag
# 15   B ? => if I>2, ?-> %NOUNS and make "B B N" the tag; else @nextiteration
# 16   ? N => make "B ? N" the tag
# 17   ? B => ? -> %NOUNS and make "B1 ?" the tag.
# 18   ? ? => @nextiteration
# 19 ? N N => if I>2 or any N-pl, make up to the N the tag and ? ->%BDRY
# 20   N B => make "? N" the tag
# 21   N ? => make N the tag and ?2->%BDRY
# 22   B N => make "? B N" the tag
# 23   B B => ? -> %NOUNS
# 24   B ? => ?1 -> %NOUNS
# 25   ? N => make N the tag @nextiteration
# 26   ? B => @nextiteration
# 27   ? ? => @nextiteration
################################################################################
# Attach sequential numbers to sentences 1,2,3,..., n.
# Take a sentence,
#       Collect:
#           POSs for the first 3 words of the sentence: W1/P W2/P W3/P
#           All sentences with not checked W[1-3] as their first 3 words.
#           Add W[1-3] to the checked words list
#       Do:
#           Use rulebasedlearn(doit) on cases 1-27 and clues to grow %NOUNS and %BDRY and attach tags to the end of sentences.
#           [@todo:If in conflict with previous decisions, merge the two sets of
#                                       sentences, then apply rules and clues]
#           Use instancebaselearn on the remaining sentences
#           Index these sentences with the decision number
#
#           GO TO: Take a sentence
#       Stop:
#           When no new term is entered to %NOUNS and %BDRY
#       Markup the remaining sentences with default tags
#       Dump marked sentences to disk
###############################################################################
populatesents();
#do{
#	$newdiscover += discover();
#	$newdiscover += instancebasedlearn();
#}while($newdiscover > 0);
discover(@START);
$newdiscovery = 0;
do{
   $newdiscovery +=discover(values(%SENTS));
}while($newdiscovery > 0);
print "##############final round--mark up with default tags\n" if $debug;
finalround();
print "##############save marked examples to disk\n" if $debug;
dumptodisk();
print "Total Decisions=$DECISIONID\n" if $debug;

sub dumptodisk{
	my @records = split(/\s+/, $NEWDESCRIPTION);
	my $start = 0;
	foreach (@records){
		if(/(.*?)\[(\d+)\]/){
			$filename = $1;
			$end = $2;
			$content = "<?xml version=\"1.0\"><description>";
			for($i = $start; $i <= $end; $i++){
				 $sentence = $SENTS{$i};
				 if($sentence =~ /\d+\{.*?\}(.*?)<d\d+-(.*?)>/){
				 	 $content .= "<$2>$1</$2>";
				 }else{
				 	 print STDERR "Wrong format $sentence\n";
				 }
			}
			$content .= "</description>";
			$filename =~ s#\.[^.]*$#\.xml#;
			open(OUT, ">$markeddir$filename") || die "$!: $markeddir$filename";
			print OUT $content;
			print "save [$content] to $markeddir$filename\n" if $debug;
			$start = $end+1;
		}
	}
}

#just use up to the first noun as the tag to mark up remaining sentences
#if the first noun is too far away from the starting of a sentence, take the
#first two words as the tag
sub finalround{
	my ($id, $sent);
	foreach $id (keys(%SENTS)){
		$sent = $SENTS{$id};
		if(!ismarked($sent)){
			defaultmarkup($sent);
		}
	}
}
#<fertileplants>plants frequently fertile . </fertileplants>
sub defaultmarkup{
	my $sent = shift;
	my ($text,@words, $tag, $word);
	if($sent =~ /achenes obovoid/){
			 print;
	}
	if($sent =~ /\d+\{.*?\}\s*(.*)/){
		$text = $1;
		@words = split(/[,:;.]/,$text);
		@words = split(/\s+/, $words[0]);
		#take the first noun/nnp as default tag
		$count = 0;
		foreach $word (@words){
			if($count++ > 4) {last;}
			if($NOUNS{$word} =~/\w/){
				$tag .= $word;
			}elsif($tag ne ""){
				tag($sent, $tag);return;
			}
		}
		if($tag ne ""){
		    tag($sent, $tag);return;
		}
		#check for the first pl
		$count = 0;
		foreach $word (@words){
		    if($count++ > 4) {last;}
			if(getnumber($word) eq "p"){
				$tag .= $word;
			}elsif($tag ne ""){
				tag($sent, $tag);return;
			}
		}
		if($tag ne ""){
			tag($sent, $tag);return;
		}
	}else{
		print STDERR "Sentence in wrong format: $sent\n";
	}

	if($tag ne ""){
		tag($sent, $tag);
	}else{
		tag($sent, $words[0]);
	}
}


########return a positive number if any new discovery is made in this iteration
########use rule based learning first for easy cases
########use instance based learning for the remining unsolved cases
sub discover{
	my @sents = @_;
	my ($sentence, @threewords, $pattern, @matched, $round);
	my $new = 0;
	foreach $sentence (@sents){
		if(ismarked($sentence)){next;}
		@startwords = getleadwords($sentence);
		print "\n>>>>>>>>>>>>>>>>>>start a new sentence\n" if $debug; 
		print "Build pattern from starting words [@startwords]\n of sentence [$sentence]\n" if $debug;
		$pattern = buildpattern(@startwords);
		#sentence: "127{Stems gray -}Stems gray - white ,..."
		if($pattern =~ /\w+/){
			$pattern = "^\\d+{.*?}$pattern";
			print "Modified pattern: $pattern\n" if $debug;
			@matched = grep(/$pattern/i, values(%SENTS));#this and other sents sharing the pattern. must use // in grep, not a string or a variable
			print "Matched sentences\n " if $debug;
			if($debug){
			    foreach(@matched){
					print substr($_,0,50)."\n";
				}
			}
			$round = 1;
			do{
			    print "####################round $round: rule based learning on matched sentences\n" if $debug;
			    $new = rulebasedlearn(@matched); #grow %NOUNS, %BDRY, tag sentences in %SENTS, record %DECISIONS
				$round++;
			}while ($new > 0);
			$round = 1;
			do{
			    print "~~~~~~~~~~~~~~~~~~~~round $round: instance based learning on matched sentences\n" if $debug;
			    $new = instancebasedlearn(@matched);
				$round++;
			}while($new > 0);
		}
	}
	#return $new;
}

####go over all un-decided sentences, solve these cases by using instance-based learning
#endings of marked sentences: "<d".$DECISIONID."-".lc $tag.">"
sub ismarked{
	my $sent = shift;
	if($sent =~ /^(\d+)\{/){
		return $SENTS{$1}=~/<d\d+\-.*?>$/;
	}else{
		print STDERR "Sentence wrong format: $sent\n";
	}
}

sub fetchfresh{
    my $sent = shift;
	if($sent =~ /^(\d+)\{/){
		return $SENTS{$1};
	}else{
		print STDERR "Sentence wrong format: $sent\n";
	}
}
#deal with those untagged sents and those with unknown POSs for leading words.
sub instancebasedlearn{
	my @sentences = @_;
	my ($sent, $index, $ptn, $unique, $temp, $toppos, $topcertainty, $toprole, $value, $freshsent, @unknown,$text, $word);
	my $flag = 0; #no new information may be found in matched cases
	my $new =0;
	my %register=("n","","s","","p","","b","","?","");
	foreach $sent (@sentences){
		if(!ismarked($sent)){
			print "For unmarked sentence: $sent\n" if $debug;
			if($sent =~/margins shallowly crenate ( shade forms )/){
			  print;
			}
			@words = getleadwords($sent);
			$ptn = getPOSptn(@words);
			print "\t leading words: @words\n\t pos pattern: $ptn\n" if $debug;
			$index = index($ptn, "?");
#			if($index < 0){
#			    #it is not possible for an unmarked sentence has known POSs (which are discovered after the sentence
#				# is processed in rule-based learning) due to the iterations on rule-based learning.
#				$new += applyrules($sent, $ptn);
#			}els
			if(rindex($ptn,"?") == $index && length $ptn > 1 && $index >=0){ #only one unknown
    			$wordptn = buildwordpattern(join(" ", @words), $ptn); # nn? => flower buds \w+
				print "\t word pattern: $wordptn\n" if $debug;
    			@matched = grep(/$wordptn/i, @sentences);# sentences matching the pattern: tagged or untagged, pos known or unknown
				print "\t matched sentences:\n " if $debug;
				if($debug){
			    	foreach(@matched){
						print "\t\t".substr($_,0,50)."\n";
					}
				}
    			#check the leading words' POSs in @matched
    			#$count = 0;
    			$toppos="";
    			$topcertainty = 0;
    			$value = 0;
        		foreach(@matched){
        			@words = getleadwords($_);
					$freshsent = fetchfresh($_);
        			$temp = $words[$index]; #the word at the position of "?"
					                        #we hope one of these words have a known pos, so we can infer others' pos 
					if($unique !~ /\b$temp\b/){
					    $unique .= $temp." ";
        				($pos, $certainty, $role) = checkpos($temp);
        				if($pos ne "2" && $pos ne "?"){
        					$value = tovalue($certainty);
							$register{$pos} .= $freshsent." ($value-$role)\$\$\$\$ ";
							$flag = 1;
        				}elsif($pos eq "?"){
							$register{"?"} .= $freshsent." ($temp)\$\$\$\$ ";
						}else{
						    print "$pos: $_\n";
						}
					}
        		}
				if($flag == 0) {
				    print "\t no new POS discovered by instance-based learning\n" if $debug;
				    return $new;
				}
     			@unknown = split(/\$\$\$\$ /,$register{"?"});
				delete($register{"?"});
				print "\t collected instances with an unknown POS:\n " if $debug;
				if($debug){
			    	foreach(@unknown){
						print "\t\t".substr($_,0,50)."\n";
					}
				}
    			foreach $unknown (@unknown){
					chomp($unknown);
					if($unknown =~ /(.*?)\s*\(([^\d]*?)\)$/){
						$text = $1;
						$word = $2;
						($pos, $role) = selectpos($text,$word,$index,%register);
						$new += update($word, $pos, $role) if($pos =~/\w/);
						
					}
				}
			}
		}
#@todo                  check for and resolve any conflict with marked @matched sentences
				
		if($new > 0){
			foreach (@matched){
				markup($_) if (!ismarked($text));	
			}
		}
	}
	return $new;
}

##look in %register for the most similar instance to $text
##to find $pos and $role for $word,
##and to find $tag for $text
sub selectpos{
	my ($text, $word, $index,%register) = @_;
	my ($ipos, $inst, $itag, $icertainty,$irole,$sim, $pos, $role, $tag, @instances);
	my $top = 0;
 	
	print "\t seen examples in hash:\n" if $debug;
	print map{"\t\t $_=> ".substr($register{$_}, 0, 50)."\n"} keys %register if $debug;
	foreach $ipos (keys(%register)){
		@instances = split(/\$\$\$\$ /,$register{$ipos});#with known pos
		foreach $inst (@instances){
		#"<d".$DECISIONID."-".$tag.">"
		 	if($inst =~ /(.*?)(?:<d\d+-([a-z]+)>)?\s*\(([0-9.]+)-([-_+]?)\)$/){
				$itag = $2;
				$sim = sim($text, $1, $word, $index);
				$icertainty = $3;
			    $irole = $4;
				if($top < $sim){
					$top = $sim;
					$role = $irole;
					$pos = $ipos;
					#$tag = $itag !~ /\w/? markup($inst): $itag;
				}
			}
		}
	}
	if($pos =~ /[ps]/){
	    $pos = getnumber($word);
	}
	print "\t selected pos for $word: $pos\n" if $debug;
	return ($pos,$role);
}
#check the before and after word of $word
sub sim{
	my($sent1, $sent2,$word, $index) = @_;
	#$sent1: {xx?}xx?abc
	#$sent2: {xxy}xxydef
	#if pos(a) = pos(d) +1
	#if a = d +2
	my ($ab, $aa, $bb, $ba, @t1, @t2);
	my $sim = 0;
	if($sent1 =~ /\d+\{.*?\}(.*)/){
		@a = split(/\s+/,$1);
		$aa = $a[$index+1];
		$ab = $a[$index-1];
	}
	if($sent2 =~ /\d+\{.*?\}(.*)/){
		@b = split(/\s+/,$1);
		$ba = $b[$index+1];
		$bb = $b[$index-1];
	}
	if($ba eq $aa && $ba =~ /\w/){ $sim++;}
	if($bb eq $ab && $bb =~ /\w/){ $sim++;}
	if($sim != 0){
		print "\t\t similarity between ".substr($sent1, 0, 20)." and ".substr($sent2, 0, 20)." is $sim\n" if $debug;
	    return $sim;
    }
	
    @t1 = checkpos($ba);
	@t2 = checkpos($aa);
	if(shift (@t1) eq shift (@t2)){ $sim += 0.5;}
	@t1 = checkpos($bb);
	@t2 = checkpos($ab);
	if(shift (@t1) eq shift (@t2)){ $sim += 0.5;}
	if($sim != 0){
	      print "\t\t similarity between ".substr($sent1, 0, 20)." and ".substr($sent2, 0, 20)." is $sim\n" if $debug;
		  return $sim;
	}
	print "\t\t similarity between ".substr($sent1, 0, 20)." and ".substr($sent2, 0, 20)." is 0\n" if $debug;

}

#mark up a sentence whose leading words have known pos. return the tag
sub markup{
	my $sentence = shift;
	my @words = getleadwords($sentence);
	my $ptn = getPOSptn(@words);
	my ($tag, $sign) = doit($sentence, $ptn);
	tag($sentence, $tag);
}
#the length of the ptn must be the same as the number of words in @words
#if certainty is < 50%, replace POS with ?.
sub getPOSptn{
	my @words = @_;
	my $ptn = "";#pattern of POSs of @words
	my $pos, $certainty, $role;
	foreach $word (@words){
		($pos, $certainty, $role)= checkpos($word);
		$pos = "?" if ($pos eq "2"); #if a word is marked as N and B, make it unknown
		if ($pos ne "?" && tovalue($certainty) <= 0.5){
		   $pos = "?" ;
		   print "$word 's POS $pos has a certainty ".tovalue($certainty)." (<0.5). This POS is ignored\n" if $debug;
		}
		$ptn .= $pos;  	#pbb,sbb, sb?
	}
	#chop($ptn);
	return $ptn;
}



########return a positive number if anything new is learnt from @source sentences
########by applying rules and clues to grow %NOUNS and %BDRY and to confirm tags
########create and maintain decision tables
sub rulebasedlearn{
	my @source = @_; #an array of sentences with similar starting words
	my ($ptn,$n,$b,$t);
	my $sign = 0;
	foreach $sent (@source){
	if(!ismarked($sent)){#without decision ids
			@words = getleadwords($sent);
			print "\nLead words: @words\n" if $debug;
			$ptn = getPOSptn(@words);
			print "POS pattern of the lead words: $ptn\n" if $debug;
			#$sign += applyrules($sent, $ptn, \@source);
			$sign += applyrules($sent, $ptn);
		}
	}
	return $sign;
}

sub applyrules{ #return a positive number if new discovery is made from processing $sent
	my ($sentence, $ptn) = @_;
	my ($tag,$sign) = doit($sentence, $ptn);
	#tag the sentence
	#save in decision pool
	if($tag ne ""){
	   tag($sentence, $tag);
	}
	return $sign+0;
}

sub doit{
	my ($sentence, $ptn) = @_; #$sentence \d+{}\w+
	my $tag = "";
	my $sign = 0;
	my ($i, @ws, @tws, @cws, $pos, $certainty, $role, $start, $end, @t);
	@ws = getleadwords($sentence);
	if($ptn =~ /^[pns]$/){
        #single-word cases, e.g. herbs, ...
        $tag = $ws[0];
	}elsif($ptn =~ /ps/){#questionable
		print "Found [ps] pattern\n" if $debug;
		$i = $+[0]; #"s"'s index
		$t = $ws[$i];
		@tws = splice(@ws, 0, $-[0]+1);
		$tag = join(" ",@tws);
		print "\t:determine the tag: $tag\n" if $debug;
		@t = checkpos($t);
		print "\t:check the questionable [ps] pattern in $sent: @t\n" if $debug;
		$sign += update($t," ","");#unset s
		print STDERR "unset a pos can not cause a > 0 sign\n" if $sign > 0;
		@t = checkpos($t);
		print "\t:reduced certainty on the questionable [ps] pattern in $sent: @t\n" if $debug;
		@t = ();
   }elsif($ptn =~/p(\?)/){#case 3,7,8,9,12,21: p? => ?->%BDRY
		#use up to the pl as the tag
		#?->%BDRY
		#save any NN in %NOUNS, note the role of Ns
		$i = $-[1];#index of ?
		print "Found [p?] pattern\n" if $debug;
		#@ws = getleadwords($sentence);
		@cws = @ws;
		@tws = splice(@ws,0,$i);#get tag words, @ws changes as well
		$tag = join(" ",@tws);
		print "\t:determine the tag: $tag\n" if $debug;
		print "\t:updates on POSs\n" if $debug;
		$sign += update($cws[$i], "b", "");
		if($ptn =~/([psn]*p)\?/){#note role for each N
			if($-[1] - $+[1] > 2){
			print;
			}
			$sign += updatenn($-[1], $+[1], @tws);# @-: offsets of starts of groups. @+:offsets of ends of groups
		}
	}elsif($ptn =~ /[psn](b)/){#case 2,4,5,11,6,20: nb => collect the tag
		#use up to the N before B as the tag
		#save NN in %NOUNS, note the role of Ns
		#anything may be learned from rule 20?
		print "Found [[psn](b)] pattern\n" if $debug;
		$i = $-[1]; #index of b
		#@ws = getleadwords($sentence);
		@tws = splice(@ws,0,$i);#get tag words, @ws changes.
		$tag = join(" ",@tws);
		print "\t:determine the tag: $tag\n" if $debug;
		print "\t:updates on POSs\n" if $debug;
		if($ptn =~/([psn]*)b/){#note role for each N
		    if($-[0] - $+[0] > 2){
			print;
			}
			$sign += updatenn($-[0], $+[0],@tws);
		}
	}elsif($ptn =~ /^\?(b)/){#case 8,17,22,23,24,26: ?b => ?->%NOUNS
		#?->%NOUNS, note the role of ?
		#use up to ? as the tag ===>
		#$ptn=~/\?(b)/
#		Lead words: Leaves basally white
#		POS pattern of the lead words: ??b
#		Found [?(b)] pattern
#			  :determine the tag: Leaves basally
#			  :updates on POSs
#			  for []: old pos [] is updated
#			  to the new pos [pos:n;certainty:1/1;role:-]

#		Lead words: lateral leaflets often
#		POS pattern of the lead words: ??b
#		Found [?(b)] pattern
#			  :determine the tag: lateral leaflets
#			  :updates on POSs
#			  for [lateral]: old pos [] is updated
#			  to the new pos [pos:s;certainty:1/1;role:-]
#@todo: check "leaves" and "basally" in a dictionary to determine how to update on their POSs
		print "Found [?(b)] pattern\n" if $debug;
		$i = $-[1];
		@tws = splice(@ws,0,$i);#get tag words
		$tag = join(" ",@tws);
		print "\t:determine the tag: $tag\n" if $debug;
		print "\t:updates on POSs\n" if $debug;
		$sign += updatenn(0,1,@tws);###<=todo.
	}elsif($ptn =~ /([psn][psn])/){#case 1,3,10,19: nn is phrase or n2 is a main noun
		#if nn is phrase or n2 is a main noun
		#make nn the tag
		#the word after nn ->%BDRY
		#save NN in %NOUNS, note the role of Ns
		print "Found [[psn][psn]] pattern\n" if $debug;
		$start = $-[0];
		$end = $+[0];
		#@ws = getleadwords($sentence);
		@tws = splice(@ws,$start, $end-$start);#get tag words
		$tag = join(" ",@tws);
		print "\t:updates on POSs\n" if $debug;
		($pos, $certainty, $role) = checkpos($tws[@tws-1]); #last n
		if(defined $NOUNS{lc $tag} || $role =~ /[-+]/){
			#the word after nn ->%BDRY
			if($sentence =~ /\d+\{.*?\}(.*)/){
				@t = split(/\s+/,$1);
				$sign += update($t[$end+1],"b",""); #test
			}
		    if(@tws > 2){
			print;
			}
			$sign += updatenn(0, @tws.length, @tws);
		}else{
			$tag = "";
		}
		print "\t:determine the tag: $tag\n" if $debug;
	}elsif($ptn =~ /[?b]\?([psn])$/){#case 16, 25
		#if n can be a main noun
		#make n the tag
		#the word after n ->%BDRY
		print "Found [[?b]?[psn]] pattern\n" if $debug;
		$start = $-[0];
		#@ws = getleadwords($sentence);
		#my @tws = splice(@ws,$start, $end-$start);#get tag words
		$tag = $ws[$start];
		print "\t:updates on POSs\n" if $debug;
		($pos, $certainty, $role) = checkpos($tag);
		if($role =~ /[-+]/){
			#the word after n ->%BDRY
			if($sentence =~ /\d+\{.*?\}(.*)/){
			    @t = split(/\s+/,$1);
				$sign += update($t[$start+1],"b",""); #test
			}
		}else{
			$tag = "";
		}
		print "\t:determine the tag: $tag\n" if $debug;
	}
	return ($tag,$sign);
}

sub tag{
	my ($sentence, $tag) = @_;
	if($tag =~ /\w+/){
        if($sentence =~ /^(\d+)\{.*?\}/){
            $sid = $1;
            $DECISIONID++;
            #one sentence one decision here
            $DECISIONS{$DECISIONID} = $sid."<".lc $tag."> ";
            $SENTS{$sid} = $SENTS{$sid}."<d".$DECISIONID."-".lc $tag.">";
			print "\t:mark up ".$SENTS{$sid}."\n" if $debug;
        }else{
            print STDERR "Sentence in wrong format: $sentence\n";
        }
	}
}


#####for the nouns in @words, make the last n the main noun("-").
#####update NN's roles
#####return a positive number if an update is made,
sub updatenn{
	my($start,$end,@words) = @_;
	my $update = 0;
	@words = splice(@words, $start, $end-$start);#all Ns
	if(@words > 2){
			  print;
	}
	for($i = 0; $i < @words-1; $i++){#one N at a time
		$update += update($words[$i],"n","_");#modifier
	}
	$update += update($words[$i], "n", "-"); #last one is the main noun
	$update += update(join(" ",@words), "n","");#NN altogether
	return $update; 
}

####find the numerical value of a certainty
sub tovalue{
	my $certainty = shift;
	return -1 if (index($certainty, "/") != rindex($certainty,"/")); 
	my ($u,$l) = split(/\//,$certainty);
	if ($l == 0){
	  print $u."/".$l ;
	 }
	return $u/$l;
}
########update %NOUNS and %BDRY; handle all updates
########save any NN in %NOUNS, note the role of Ns
########"1/1_" modifier:"_", main noun:"-", both: "+";
#####return 1 if an update is made, otherwise 0;
sub update{
	my ($word, $pos, $role) = @_; #("Base", "n", "_") or ("Base", "b", ""), or ("Multiple", " ","")--unset Multiple as singular noun
	my ($oldpos, $certainty, $oldrole) = checkpos($word);
	my $update = 0;
	my $set1 = 0;
	my $set2 = 0;
	#update the role
	if($word eq ""){
	  return;
	}
	$role = mergerole($oldrole, $role);
	if($oldpos eq "?" && $pos ne " "){
		$update = setpos($word, $pos, "1/1", $role);
	}elsif($oldpos eq $pos){
	    my ($u,$l)=split(/\//, $certainty);
		$update = setpos($word, $pos, ($u+1)."/".($l+1), $role);
	}elsif($oldpos ne $pos){ #conflicts
		if($oldpos eq "2"){
			if($certainty =~ /([spn])(\d+)\/(\d+)[-_]b(\d+)\/(\d+)/){
				if($pos eq "b"){
					$set1 = setpos($word, $1, $2."/".($3+1), $role);
					$set2 = setpos($word, "b", ($4+1)."/".($5+1),$role);
					$update = $set1 + $set2;
				}elsif($pos eq " "){
					$set1 = setpos($word, $1, $2."/".($3+1), $role);
					$set2 = setpos($word, "b", $4."/".($5+1),$role);
					$update = $set1 + $set2;
				}else{
					$set1 = setpos($word, $1, ($2+1)."/".($3+1),$role);
					$set2 = setpos($word, "b", $4."/".($5+1),$role);
					$update = $set1 + $set2;
				}
			}
		}else{# if $pos = "n", $oldpos="[sp]", keep the oldpos, otherwise update
			if($oldpos eq "n" && $pos =~ /[sp]/){
				if($certainty =~ /([spnb])(\d+)\/(\d+)/){
					$update = setpos($word, $pos,($2+1)."/".($3+1), $role);
				}
			}else{
			    if($certainty =~ /([spnb])(\d+)\/(\d+)/){
					$set1 = setpos($word, $oldpos, $2."/".($3+1), $role);
					$set2 = setpos($word, $pos, "1"."/".($3+1), $role) if $pos ne " ";
					$update = $set1 + $set2;
				}
			}
		}
	}
	return $update;
}

sub mergerole{
	my($role1, $role2) = @_;
	$role1 .= $role2;
	if(length($role1) == 2){
		return "+";
	}else{
		return $role1;
	}
}

sub setpos{
    #return 1 if new pos/role is set, otherwise return 0
	my ($word, $pos, $certainty, $role) = @_;
	my $new = 0;
	my $old;
	chomp($word);
	$word =~ s#^\s*##;
	if($word =~ /shade forms \) to/){
	    print;
	}
	if($pos eq "n"){
		$pos = getnumber($word); #update pos may be p,s, or n	
	}
	if($pos =~ /[nsp]/){
		$old = $NOUNS{lc $word};
		$NOUNS{lc $word} = $pos.$certainty.$role;
		if(!defined($old) || $old !~ /$pos/ || $old !~ /[$role]/){
		 $new = 1;
		 print "\t\tfor [$word]: old pos [$old] is updated\n\t\tto the new pos [pos:$pos;certainty:$certainty;role:$role]\n" if $debug;
		}
	}elsif($pos =~ /[b]/){
		$old = $BDRY{lc $word};
		$BDRY{lc $word} = $certainty;
		if(!defined $old){
		 $new = 1;
		 print "\t\tfor [$word]: old pos [$old] is updated\n\t\tto the new pos [pos:$pos;certainty:$certainty]\n" if $debug;
		}
	}
	return $new;
}
####
sub getnumber{
	my $word = shift;
	return "p" if ($word =~ /es$/ || $word =~ /s$/);
    return "s";
}


########return (pos, certainty, role)
sub checkpos{
	my $word = lc shift;
	my $posn;
	chomp($word);
	$word =~ s#^\s*##;
	$posn = $NOUNS{$word};
	$posb = $BDRY{$word};
	if(defined $posn && !defined $posb){ #"s1/2_"
	    if($posn =~ /([psn])(\d+\/\d+)([-_+]?)/){
			return ($1,$2,$3); #("s", "1/2", "_")
		}else{
			print STDERR "POS in wrong format: $posn\n";
		}
	}elsif(defined $posb && !defined $posn){
		return ("b", $posb,"");
	}elsif(!defined $posn && !defined $posb){
		return ("?","?","");
	}else{#has conflict POSs, return both
		return ("2", $posn."b".$posb, ""); #"s1/2_b1/2"
	}
}

########return an array
sub getleadwords{
	my $sentence = shift;
	if($sentence =~ /\d+\{(.*?)\}.*/){
		return split(/\s+/,$1);
	}else{
		print STDERR "Sentence in wrong format: $sentence\n";
	}
}


########e.g. given $ptn = NN?,
########           $words = flower buds few,
########     return pattern "/\d+\{.*?\}flower buds \w+/i"
########should not rely on the length of $ptn or the number of words in $words
sub buildwordpattern{
	my ($words, $ptn) = @_;
	my $pattern = "\\d+\{.*?\}";
	my @w = split(/\s+/, $words);
	for($i = 0; $i < @w; $i++){
		if(substr($ptn,$i,1)eq "?"){
			$pattern .="\\w+ ";
		}else{
			$pattern .= $w[$i]." ";
		}
	}
	chop($pattern);
	return $pattern;
}
########return the pattern that matches any sentence
########whose first $N words match any word in @words that is not in $CHECKEDWORDS
########e.g. /^(cat|dogs|fish)|^\w+\s(cat|dogs|fish)|^\w+\s\w+\s(cat|dogs|fish)/
sub buildpattern{
	my @words = @_;
	my @newwords =();
	my ($pattern, $tmp);
    my $prefix="\\w+\\s";
	print ("CHECKEDWORDS is\n[$CHECKEDWORDS]\n") if $debug;
	#identify new words
	foreach (@words){
		if($_!~ /[!"\#$%&'()*+,-.\/:;=?@[\]^_`{|}~0-9]/ && $CHECKEDWORDS !~ /:$_:/i){
			$tmp .= $_."|"; #(cat|dogs|fish)
			push(@newwords,$_);
		}
	}
	if($tmp !~ /\w/){return "";}#no new words
	#build pattern out of the new words
	chop($tmp);
	print ("Pattern segment: [$tmp]\n") if $debug;
	$tmp = "(?:".$tmp.")";
	#$pattern ="^".$tmp."|";
	$pattern =$tmp."|";
	for($i = 0; $i < $N-1; $i++){
		$tmp = $prefix.$tmp;
		#$pattern .= "^".$tmp."|";
		$pattern .=$tmp."|";
		#^(?:cat|dogs|fish)|^\w+\s(?:cat|dogs|fish)|^\w+\s\w+\s(?:cat|dogs|fish)
	}
	chop($pattern);
	$pattern = "(?:".$pattern.")";
	print ("Pattern: [$pattern]\n") if $debug;
	$CHECKEDWORDS .= join(":",@newwords).":";
	return $pattern;
}


########read $dir, mark sentences with $SENTMARKER,
########put space around puncts, save sentences in %SENTS,
########"sentences" here include . or ; ending text blocks.
sub populatesents{
my @sentences;
my ($text,$count, @tmp);
opendir(IN, "$dir") || die "$!: $dir\n";
while(defined ($file=readdir(IN))){
	if($file !~ /\w/){next;}
	$text = ReadFile::readfile("$dir$file");
	@sentences = SentenceSpliter::get_sentences($text);
	foreach (@sentences){
		#may have fewer than $N words
		if(!/\w+/){next;}
		s#([!"\#$%&'()*+,-./:;=?@[\]^_`{|}~])# $1 #g;
	    s#\s+# #g;
		@words = getfirstnwords($_, $N); # "w1 w2 w3"
		@tmp = checkpos($words[0]);
		if($tmp[0] =~ /p/){
			push(@START, $SENTID."{@words}".$_);
		} 
		$SENTS{$SENTID} = $SENTID."{@words}".$_;
		print "Sentence: $_\n" if $debug;
		print "Leading words: @words\n\n" if $debug;
		$SENTID++;
	}
	$NEWDESCRIPTION.=$file."[".($SENTID-1)."] ";
}
print "Total sentence=$SENTID" if $debug;
}

sub getfirstnwords{
	########return the first up to $n words of $sentence as an array, excluding 
	my($sentence, $n) = @_;
	my @words, $index;
	if($sentence =~ / [,:;.]/){
		$index = $-[0];
	}else{
		$index = length($sentence);
	}
	$sentence = substr($sentence, 0, $index);
	$sentence =~ s#[!"\#$%&'()*+,-./:;=?@[\]^_`{|}~0-9]# #g; #remove all punct. marks and numbers
	@words = split(/\s+/, $sentence);
	#print "words in sentence: @words\n" if $debug;
	@words = splice(@words, 0, $n);
	return @words;
}