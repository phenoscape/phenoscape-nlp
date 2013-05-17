#!C:\Perl\bin\perl

use XML::DOM;
use nounheuristics;
use initialmarkup;

# my $parser = new XML::DOM::Parser;
# my $doc = $parser->parsefile ("file.xml");
# #print all HREF attributes of all CODEBASE elements
# my $nodes = $doc->getElementsByTagName ("CODEBASE");
# my $n = $nodes->getLength;
# for (my $i = 0; $i < $n; $i++)
# {
#     my $node = $nodes->item ($i);
#     my $href = $node->getAttributeNode ("HREF");
#     print $href->getValue . "\n";
# }
# #Print doc file
# $doc->printToFile ("out.xml");
# # Print to string
# print $doc->toString;
# # Avoid memory leaks - cleanup circular references for garbage collection
# $doc->dispose;
#source : http://jonas.liljegren.org/perl/libxml/XML/DOM.html



#infer NNP from collection of initally marked descriptions 
#mark up descriptions with new tags.

#($second, $minute, $hour, $dayOfMonth, $month, $yearOffset, $dayOfWeek, $dayOfYear, $daylightSavings) = localtime();
#The granddaddy of Perl profiling tools is Devel::DProf. To profile a code run, add the -d:DProf argument to your Perl command line and let it go:
$dir = "U:\\Research\\Data-Algea\\algea-data\\text-morphology\\";#textfile
$ttmarkeddir ="U:\\Research\\Data-Algea\\algea-data\\text-morphology-perl-tt\\";
$finalmarkeddir = "U:\\Research\\Data-Algea\\algea-data\\text-morphology-perl-final\\";
$seedsfile = "";#line: noun1[s]|[noun2[p]|]+
$hnouns = "U:\\Research\\Data-Algea\\algea-data\\text-morphology-perl-heurnouns.txt";
@nouns = (); #holds the seed nnps and nnps discovered later
#@BDRY = (); #holds the modifiers discovered
#@INITIALS = (); #holds tentatively marked descriptions
@nouns1 = NounHeuristics::heurnouns($dir, $hnouns);
push(@nouns, @nouns1);#nouns

%BDRY = (); #boundary words
@stops = split(/|/,$NounHeuristics::STOP);
for (@stops){
	$BDRY{$_} = "1/1"; #certainty NumOfThisDecision/NumOfTotalDecision
}
%NOUNS = ();
for (@nouns){#convert to hash
    #@todo: noun->s/p/n
	$NOUNS{$_} = "s1/1"; #sigular or plural and certainty
}
#sentence format 1{Basal leaves absent}Basel leaves absent .<d1-Basel leaves>
#1:sentenceid, {}:leading N words, <d1:decisionID, Basal leaves>: tag
%SENTS = (); #sentences found in files in $dir
#$SENTMARKER = "{}"; #sentence seperator
$CHECKEDWORDS = ""; #leading three words of sentences

$N = 3; #$N leading words 
%DESICIONS = ();
#%TAGS = (); #indexed by tag words

if(! -e $ttmarkeddir){
system("mkdir", $ttmarkeddir);
}
if(! -e $finalmarkeddir){
system("mkdir", $finalmarkeddir);
}
if($seedsfile eq ""){
#@todo: read nouns from seeds
}

###############################################################################
# bootstrap between %NOUNS and %BDRY 
# B: a boundary word, N: a noun or noun *phrase* ?: a unknown word
# goal: grow %NOUNS and %BDRY + confirm tags--include "#" in checked/confirmed tags
# 
# decision table: leading three words (exclude "at the center of xxx")
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
# rules:
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
#           Use rule 1-27 and clues to grow %NOUNS and %BDRY and confirm tags.
#           [If in conflict with previous decisions, merge the two sets of 
#                                       sentences, then apply rules and clues]
#           Include in the confirmed tag a decision number: 1,2,...,n
#           Index these sentences with the decision number 
#           
#           GO TO: Take a sentence 
#       Stop:
#           When no new term is entered to %NOUNS and %BDRY
###############################################################################
populatesents(); #@todo
$newdiscover = 1;
while($newdiscover > 0){
	$newdiscover = discover();
}
#@todo: go over all un-decided sentences, solve these cases by using instance-based learning
#case 3  
$unique .= @words[index($ptn, "?")]." ";
		$wordptn = buildwordpattern($words, $ptn);
		@matched = grep($wordptn, @$rsource);
		$count = 0;
		my $toppos="";
		my $topcertainty = 0;
		my $value = 0;
		foreach(@matched){
			@words = getleadwords($_);
			$temp = $words[@words-1]
			if($temp !~ /$unique/){
				$unique .= $temp." "; #POS of the unique ?s ??????? 
				($pos, $certainty) = checkpos($temp); #"2"
				if($pos ne "2" && $pos ne "?"){
					$value = tovalue($certainty);
					if($value > $topcertainty){
						$toppos = $pos;
						$topcertainty = $value;
					} 
					$count++;
				}
			}		
		}



########return 1 if any new discovery is made in this iteration
sub discover{
	my $sentence, @threewords, $pattern, @matched, $new, $learn
	$new = 0;
	foreach $sentence (values(%SENTS)){
		@startwords = getleadwords($sentence)
		$pattern = buildpattern(@startwords);
		if($pattern =~ /\w+/){
			@matched = grep($pattern, values(%SENTS));#this and other sents sharing the pattern
			$learn = learn(@matched);
			$new = $learn > 0? $learn, $new;
		}
	}
	return $new;
}
########return a positive number if anything new is learnt from @source sentences
########by applying rules and clues to grow %NOUNS and %BDRY and to confirm tags
########create and maintain decision tables
sub learn{
	my @source = @_;
	my $ptn,$n,$b,$t,$sign;
	my $new = 0;
	my $rdecisionID = 0;
	foreach $sent (@source){
		if($sent !~ /<d\d+-.*?>/){#without decision ids
			@words = getleadwords($sent);
			$ptn = "";
			foreach $word (@words){
				$n = $NOUNS{$word};
				$n = $n eq ""? "?" : $n;
				$b = $BDRY{$word};
				$b = $b > 0? "b" : "?";
				$t = $n eq "?"? $b : $n; #if a word is marked as N and B, make it N
				$ptn .= $t;  	#pbb,sbb, sb?
			}
			chop($ptn);
			$sign = applyrules($sent, $ptn, \@source, \$rdecisionID);
			$new = $sign > 0? $sign : $new;			
		}
	}
	return $new;
}
#@todo, record in %NOUNS the role of the noun, as a modifier or a main noun.
sub applyrules{ #return a positive number if new discovery is made from processing $sent
	my ($sent, $ptn, $rsource, $rdid) = @_; 
	#$rsource: the reference to source; $rdid: reference to the decision id
	my $new = 0;
	my $sign = 0;
	if($ptn =~ /ps/){#questionable
		print STDOUT "check [ps] pattern in $sent\n";
	}elsif($ptn =~/p?/){#case 3,7,8,9,12,21: p? => ?->%BDRY
		#use up to the pl as the tag
		#?->%BDRY
		#save any NN in %NOUNS, note the role of Ns
	}elsif($ptn =~ /[psn]b/){#case 2,4,5,11,6,20: nb => collect the tag
		#use up to the N before B as the tag
		#save NN in %NOUNS, note the role of Ns
		#anything may be learned from rule 20?
	}elsif($ptn =~ /?b/){#case 8,17, 22,23,24, 26: ?b => ?->%NOUNS
		#?->%NOUNS, note the role of ?
		#use up to ? as the tag
	}elsif($ptn =~ /[psn][psn]/){#case 1,3,10,19: nn is phrase or n2 is a main noun
		#if nn is phrase or n2 is a main noun
		#make nn the tag
		#the word after nn ->%BDRY
		#save NN in %NOUNS, note the role of Ns
	}elsif($ptn =~ /[?b]?[psn]$/){#case 16, 25
		#if n can be a main noun
		#make n the tag
		#the word after n ->%BDRY
	}
#	if($ptn =~ /[psn] [psn] [psn]/){
#		$sign = rule1($sent,$ptn, $rsource, $rdid);
#	}elsif($ptn =~/[psn] [psn] b/){
#		$sign = rule2($sent,$ptn, $rsource, $rdid);
#	}elsif($ptn =~/[psn] [psn] \?/){
#		$sign = rule3($sent,$ptn, $rsource, $rdid);
#	}elsif($ptn =~/[psn] b [psn]/){
#		$sign = rule4($sent,$ptn, $rsource, $rdid);
#	}elsif($ptn =~/[psn] b b/){
#		$sign = rule5($sent,$ptn, $rsource, $rdid);
#	}elsif($ptn =~/[psn] b \?/){
#		$sign = rule6($sent,$ptn, $rsource, $rdid);
#	}elsif($ptn =~/[psn] \? [psn] /){
#		$sign = rule7($sent,$ptn, $rsource, $rdid);
#	}elsif($ptn =~/[psn] \? b/){
#		$sign = rule8($sent,$ptn, $rsource, $rdid);
#	}elsif($ptn =~/[psn] \? \?/){
#		$sign = rule9($sent,$ptn, $rsource, $rdid);
#	}elsif($ptn =~/b [psn] [psn]/){
#		$sign = rule10($sent,$ptn, $rsource, $rdid);
#	}elsif($ptn =~/b [psn] b/){
#		$sign = rule11($sent,$ptn, $rsource, $rdid);
#	}elsif($ptn =~/b [psn] \?/){
#		$sign = rule12($sent,$ptn, $rsource, $rdid);
#	}elsif($ptn =~/b b [psn]/){
#		$sign = rule13($sent,$ptn, $rsource, $rdid);
#	}elsif($ptn =~/b b b/){
#		$sign = rule14($sent,$ptn, $rsource, $rdid);
#	}elsif($ptn =~/b b \?/){
#		$sign = rule15($sent,$ptn, $rsource, $rdid);
#	}elsif($ptn =~/b \? [psn]/){
#		$sign = rule16($sent,$ptn, $rsource, $rdid);
#	}elsif($ptn =~/b \? b/){
#		$sign = rule17($sent,$ptn, $rsource, $rdid);
#	}elsif($ptn =~/b \? \?/){
#		$sign = rule18($sent,$ptn, $rsource, $rdid);
#	}elsif($ptn =~/\? [psn] [psn]/){
#		$sign = rule19($sent,$ptn, $rsource, $rdid);
#	}elsif($ptn =~/\? [psn] b/){
#		$sign = rule20($sent,$ptn, $rsource, $rdid);
#	}elsif($ptn =~/\? [psn] \?/){
#		$sign = rule21($sent,$ptn, $rsource, $rdid);
#	}elsif($ptn =~/\? b [psn]/){
#		$sign = rule22($sent,$ptn, $rsource, $rdid);
#	}elsif($ptn =~/\? b b/){
#		$sign = rule23($sent,$ptn, $rsource, $rdid);
#	}elsif($ptn =~/\? b \?/){
#		$sign = rule24($sent,$ptn, $rsource, $rdid);
#	}elsif($ptn =~/\? \? [psn]/){
#		$sign = rule25($sent,$ptn, $rsource, $rdid);
#	}elsif($ptn =~/\? \? b/){
#		$sign = rule26($sent,$ptn, $rsource, $rdid);
#	}elsif($ptn =~/\? \? \?/){
#		$sign = rule27($sent,$ptn, $rsource, $rdid);
#	}	
	$new = $sign > 0? $sign : $new;
	return $new;
}

sub rule1{ #NNN
	my($sentence, $ptn, $rsource, $rdid) = @_;
	my $sid, $tag, $decisionid, $new = -1;
	#the sentence to be tagged, pattern of the leading $N words, reference to the array of other similar sentences
	my $i = index($ptn, "p");
	$i = $i < 0? length($ptn) : $i+1; 
	#take the first $i words as the tag
	#add to decision group: decision ID => sentenceID<tag>
	if($sentence =~ /(\d+)\{.*?\}((?:\w+\s){0,$i})/){
		  $sid = $1;
		  $tag = $2;
		  $decisionid = $$rdid+1;
		  $DECISIONS{$decisionid} = $DECISIONS{$decisionid}.$sid."<".$tag.">";
		  $SENTS{$sid} = $SENTS{$sid}."<d".$decisionid."-".$tag.">";
		  if($i > 1 && !defined($NOUNS{lc $tag}){
		  	   update($tag, "n"); 
			   $new = 1;
		  }
	}else{
		  print STDERR "Sentence in wrong format: $sentence\n";
	}
	return $new;	#no grow to %NOUNS or %BDRY
}
sub rule2{#NNB
	my($sentence, $ptn, $rsource, $rdid) = @_;
	my $sid, $tag, $decisionid, $new=-1;
	#the sentence to be tagged, pattern of the leading $N words, reference to the array of other similar sentences
	my $i = index($ptn, "p");
	$i = $i < 0? index($ptn, "b"): $i+1; 
	#take the first $i words as the tag
	#add to decision group: decision ID => sentenceID<tag>
	if($sentence =~ /(\d+)\{.*?\}((?:\w+\s){0,$i})/){
		  $sid = $1;
		  $tag = $2;
		  $decisionid = $$rdid+1;
		  $DECISIONS{$decisionid} = $DECISIONS{$decisionid}.$sid."<".$tag.">";
		  $SENTS{$sid} = $SENTS{$sid}."<d".$decisionid."-".$tag.">";
		  if($i > 1 && !defined($NOUNS{lc $tag}){
		  	   update($tag, "n");
			   $new = 1;
		  }
	}else{
		  print STDERR "Sentence in wrong format: $sentence\n";
	}
	return $new;	#no grow to %NOUNS or %BDRY
}
sub rule3 {#NN?
	my($sentence, $ptn, $rsource, $rdid) = @_;
	my $wordptn, @matched, @words, $unique, $count, $temp, $nn, $pos,$certainty;
	my $sid, $tag, $decisionid, $new=-1;
	#case 1
	my $i = index($ptn, "p");
	#case 2
	@words = getleadwords($sentence);
	$nn = join(" ", splice(@words, 0, index($ptn, "?")+1)); 
	#decision
	if($i == index($ptn, "?")-1 || defined $NOUNS{lc $nn}){
		$tag = $nn;
		update($words[index($ptn, "?")], "b"); #new
		update($nn, "n");
		$new = 1;
		#...
	}
	return $new;	
}

else{
		
				
	}	
####find the numerical value of a certainty
sub tovalue{
	my certainty = shift;
	my ($u,$l) = split(/\//,certainty);
	return $u/$l;
}
########update %NOUNS and %BDRY; handle all updates
sub update{
	my ($word, $pos) = @_; #("Base", "n")
	#my $lword = lc $word;
	my ($oldpos, $certainty) = checkpos($word);
	if($oldpos eq "?"){
		setpos($word, $pos, "1/1");
	}elsif($oldpos eq $pos){
	    my ($u,$l)=split(/\//, $certainty);
		setpos($word, $pos, ($u+1)."/".($l+1));
	}elsif($oldpos ne $pos){ #conflicts 
		if($oldpos eq "2"){
			if($certainty =~ /([spn])(\d+)\/(\d+)b(\d+)\/(\d+)/){
				if($pos eq "b"){
					setpos($word, $1, $2."/".($3+1));
					setpos($word, "b", ($4+1)."/".($5+1));
				}else{
					setpos($word, $1, ($2+1)."/".($3+1));
					setpos($word, "b", $4."/".($5+1));
				}
			}
		}else{
			if($certainty =~ /([spnb])(\d+)\/(\d+)/){
				setpos($word, $oldpos, $2."/".($3+1));
				setpos($word, $pos, 1."/".($3+1));
			}
		}
	}
}
	
sub setpos{
	my ($word, $pos, $certainty) = @_;
	if($pos =~ /[nsp]/){
		$NOUNS{lc $word} = $pos.$certainty;	
	}elsif($pos =~ /[b]/){
		$BDRY{lc $word} = $certainty;
	}
}

########return (pos, certainty)
sub checkpos{
	my $word = lc shift;
	my $posn;
	$posn = $NOUNS{$word};
	$posb = $BDRY{$word}; 
	if(defined $posn && !defined $posb){ #"s1/2"
		return (substr($posn,0,1), substr($posn,1)); #("s", "1/2")
	}elsif(defined $posb && !defined $posn){
		return ("b", $posb);
	}elsif(!defined $posn && !defined $posb){
		return ("?","?");
	}else{#has conflict POSs, return both
		return ("2", $posn."b".$posb); #"s1/2b1/2"
	}
}

########return an array
sub getleadwords{
	my $sentence = shift;
	if($sentence =~ /\d+\{(.*?\)}.*/){
		return split(/\s+/,$1);
	}else{
		print STDERR "Sentence in wrong format: $sentence\n";
	}
}


########e.g. given $ptn = NN?, $words = flower buds few, return pattern "/\d+\{.*?\}flower buds \w+/i"
sub buildwordpattern{
	my ($words, $ptn) = @_;

}
########return the pattern that matches any sentence 
########whose first @words words match any word in @words that is not in $CHECKEDWORDS
sub buildpattern{
	my @words = @_;
	$CHECKEDWORDS .= join(":",@newwords).":"; 	
} 

########return the first $n words of $sentence as an array
sub getfirstnwords{
	my($sentence, $n) = @_;
	@words = split(/\s+/, $sentence);#\d+{}
	return splice(@words, 0, $n);
}

########read $dir, mark sentences with $SENTMARKER,
########put space around puncts, save sentences in %SENTS, ids to @SENTSPOOL
########"sentences" here include . or ; ending text blocks.
sub populatesents{
#@tocomplete
#may have fewer than $N words 
$words = getfirstnwords($sentence, $N); # "w1 w2 w3"
$SENTS{$count} = $count."{".$words."}".$sentence;
push(@SENTSPOOL, $count); 
}







































































################################################################################
sub concatall{
	my $all = "<collection>";
	foreach (@INITIALS){
		$all.=$_;
	}
	return $all."</collection>";
}
################################################################################
sub marknouns{
	my $first = "";
	my $i = "";
	foreach $n (@NOUNS){#$n all lower case
		#apex[s] => [aA]pex
		$n =~ s#\[[sp]\]\s*##;
		$first = substr $n,0,1;
		$first =~ tr/[a-z]/[A-Z/;
		$n = "[".(substr $n,0,1).$first."]".(substr $n,1);
		foreach $l (@INITIALS){#updated
			$l =~ s#(\b)($n)(\b)#\1\{\2\}\3#g;
		}
	}
	return;
}