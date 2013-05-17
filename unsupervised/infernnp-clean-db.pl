#!C:\Perl\bin\perl

#infer nnps (concept) from text. 
#interact with end users for properties
#output marked descriptions to disk
#output GraphML for profuse visualization
#use rdf.pl to output RDF file describing the semantic structure of the text collection 

use lib 'C:\\Documents and Settings\\hong cui\\My Documents\\demos\\unsupervised\\src\\';
use NounHeuristics;
use InitialMarkup;
use SentenceSpliter;
use ReadFile;
use strict;
use DBI;

#infer NNP from collection of initally marked descriptions
#mark up descriptions with new tags.

#perl unsupervised.pl "C:\Documents and Settings\hong cui\My Documents\DEMO\unsupervised\" test test-unsupervised seednouns.txt learntnouns.txt graphml.xml
my $workdir = $ARGV[0];
my $dir = $workdir.$ARGV[1]."\\";#textfile
my $markeddir = $workdir.$ARGV[2]."\\" ;
my $seedsfile = $workdir.$ARGV[3];#line: noun1[spn]
my $hnouns = $workdir.$ARGV[4];
my $graphmlfile = $workdir.$ARGV[5];

#infer NNP from collection of initally marked descriptions
#mark up descriptions with new tags.
#my $dir = "..\\test\\";#textfile
#my $markeddir = "..\\test-unsupervised\\";
#my $seedsfile = "..\\seednouns.txt";#line: noun1[spn]
#my $hnouns = "..\\learntnouns.txt";
#my $graphmlfile = "graphml.xml";

my $db = "terms";
my $host = "localhost";
my $user = "termsuser";
my $password = "termspassword";

my $dbh = DBI->connect("DBI:mysql:database=$db:host=$host", $user, $password)
or die DBI->errstr."\n";
my $test = $dbh->prepare('use terms')
or die $dbh->errstr."\n";
$test->execute() or die $test->errstr."\n";
$test = $dbh->prepare('show tables from terms')
or die $dbh->errstr."\n";
$test->execute() or die $test->errstr."\n";
my ($table, $exists, $create, $del);
while($table = $test->fetchrow_array()){
$exists.=$table." ";
}

if($exists !~/\bsentence\b/){
#create table sentence
$create = $dbh->prepare('create table sentence (sentid varchar(10) not null unique, sentence varchar(500), lead varchar(50), status varchar(20), tag varchar(50),primary key (sentid)) engine=innodb');
$create->execute() or warn "$create->errstr\n";
}else{
$del = $dbh->prepare('delete from sentence');
$del->execute();
}

if($exists !~/\bwordpos\b/){
#create table wordpos
$create = $dbh->prepare('create table wordpos (word varchar(50) not null, pos varchar(2) not null, role varchar(5), certaintyu int, certaintyl int, primary key (word, pos)) engine=innodb');
$create->execute() or warn "$create->errstr\n";
}else{
$del = $dbh->prepare('delete from wordpos');
$del->execute();
}

if($exists !~/\bwordsent\b/){
#create table wordsent
$create = $dbh->prepare('create table wordsent (word varchar(50) not null, sentid varchar(10) not null, primary key (word, sentid), foreign key(sentid) references sentence(sentid) on update cascade, foreign key(word) references wordpos(word) on update cascade) engine=innodb');
$create->execute() or warn "$create->errstr\n";
}else{
$del = $dbh->prepare('delete from wordsent');
$del->execute();
}


if($exists !~/\blinkage\b/){
#create table bsentlinkage
$create = $dbh->prepare('create table linkage (filename varchar(300) not null unique primary key, endindex int not null) engine=innodb');
$create->execute() or warn "$create->errstr\n";
}else{
$del = $dbh->prepare('delete from linkage');
$del->execute();
}

my $debug = 0;

#sentence format 1{Basal leaves absent}Basel leaves absent .<d1-Basel leaves>
#1:sentenceid, {}:leading N words, <d1:decisionID, Basal leaves>: tag
#%SENTS = (); #sentences found in files in $dir
my $CHECKEDWORDS = ":"; #leading three words of sentences
my $N = 3; #$N leading words
#%DESICIONS = ();
my $SENTID = 0;
my $DECISIONID = 0;
my $NEWDESCRIPTION =""; #record the index of sentences that ends a description
#@START = (); #SENTs for sentences starting with single pl words.
my $GraphML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\">\n<graph edgedefault=\"directed\">\n<key id=\"name\" for=\"node\" attr.name=\"name\" attr.type=\"string\"/>\n<key id=\"type\" for=\"all\" attr.name=\"type\" attr.type=\"string\"/>\n<key id=\"weight\" for=\"all\" attr.name=\"weight\" attr.type=\"float\"/>";
$GraphML .= "\n<node id='0'>";
$GraphML .= "\n<data key='name'>description</data>";
$GraphML .= "\n<data key='type'>element</data>";
$GraphML .= "\n</node>";

if(! -e $markeddir){
system("mkdir", $markeddir);
print "$markeddir created\n" if $debug;
}

my @nouns1 = NounHeuristics::heurnouns($dir, $hnouns);
#print "nouns learnt from heuristics:\n@nouns1\n" if $debug;

my @nouns = (); #holds the seed nnps and nnps discovered later
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
#%BDRY = (); #boundary words
my @stops = split(/\|/,$NounHeuristics::STOP);
push(@stops, "NUMBER"); #"0","1", "2","3","4","5","6","7","8","9"
#print "stop list:\n@stops\n" if $debug;

for (@stops){
	#$BDRY{$_} = "1/1"; #certainty NumOfThisDecision/NumOfTotalDecision
	#insert to table wordpos
	my $w = lc $_;
	my $stmt ="insert into wordpos values(\"$w\",\"b\",\"\",1,1)";
	my $sth = $dbh->prepare($stmt);
	$sth->execute();
}
#%NOUN=();
foreach my $n (@nouns){#convert to hash
	if($n =~ /\w/){
    	#note: what if the same word has two different pos?
		my @ns = split(/\|/,$n);
		foreach my $w (@ns){
			if($w =~ /(\w+)\[([spn])\]/){
				  #$NOUNS{lc $1} = $2."1/1"; #sigular or plural, certainty,
				  #and role (""unknown, -main noun, _modifier, , +both)
				  #insert to table wordpos
				  $w = lc $1;
				  my $sth = $dbh->prepare("insert into wordpos values(\"$w\",\"$2\",\"\",1,1)");
	                          $sth->execute();
			}
		}
	}
}

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

#start with sentences with "start" status
discover("start");
#>>>>checked here
#discover(@START);
my $newdiscovery = 0;
do{
   $newdiscovery += discover("normal");
}while($newdiscovery > 0);
print "##############andor cases\n" if $debug;
andor();
print "##############final round--mark up with default tags\n" if $debug;
finalround();
print "##############save marked examples to disk\n" if $debug;
normalizetags();
dumptodisk();
#print "Total Decisions=$DECISIONID\n" if $debug;

sub normalizetags{
  my ($sth, $tag, $ntag, $sth1);
  $sth = $dbh->prepare("select distinct tag from sentence order by tag");
  #$sth = $dbh->prepare("select * from sentence");
  $sth->execute() or warn "$sth->errstr\n";
  while(my($tag) = $sth->fetchrow_array()){
    $ntag = singular(lc($tag));
    my $query =  "update sentence set tag = \"$ntag\" where tag = \"$tag\"";
    $sth1 = $dbh->prepare($query);
    $sth1->execute() or warn "$sth1->errstr\n";;
  }
}

sub singular{
 my $p = shift;
 my @ts = split(/\s+/, $p);
 for(my $i = 0; $i<@ts; $i++){
  if($ts[$i] =~ /(.*?)[^aeiou]ies$/){
    $ts[$i] = $1.'y';
  }elsif($ts[$i] =~/(.*?)i$/){
    $ts[$i] = $1.'us';
  }elsif($ts[$i] =~/(.*?)ia$/){
    $ts[$i] = $1.'ium';
  }elsif($ts[$i] =~/(.*?x)es$/){
    $ts[$i] = $1;
  }elsif($ts[$i] =~/(.*?a)e$/ || $ts[$i]=~/(.*?)s$/ ){#pinnae ->pinna, fruits->fruit
    $ts[$i] = $1;
  }
 }
  return join(' ',@ts);
}
#deal with "herbs or lianas" cases
#@todo: test/debug
sub andor{
my ($sth1, $sth2, $sentid, $sentence, $tag);

$sth1 = $dbh->prepare('select sentid, sentence from sentence where lead like "% and %"');
$sth1->execute();
while(($sentid, $sentence)=$sth1->fetchrow_array()){
	$tag = andortag($sentence, "and");
	$sth2 = $dbh->prepare("update sentence set tag = \"$tag\" where sentid = \"$sentid\"");
    $sth2->execute();
}
$sth1 = $dbh->prepare('select sentid, sentence from sentence where lead like "% or %"');
$sth1->execute();
while(($sentid, $sentence)=$sth1->fetchrow_array()){
   $tag = andortag($sentence, "or");
   $sth2 = $dbh->prepare("update sentence set tag = \"$tag\" where sentid = \"$sentid\"");
   $sth2->execute();
}
}

sub andortag{
my ($sentence, $token) = @_;
my ($tag, @words, $ptn);

@words = tokenize($sentence);
foreach (@words){
 if($_ eq $token){
   last if $ptn =~/X/;
   $ptn .="X";
   }else{ 
   $ptn .=checkpos($_,"one");
   }
} 

print "Andor pattern $ptn for @words\n" if $debug;
if($ptn =~ /^(.*?[nsp]X.*?[nsp])/){
   print "$+[1], $+[1]\n" if $debug;
   $tag = join (" ", splice(@words, 0, ($+[1]-$-[1]))); 
}
print "Andor determine the tag $tag for $sentence\n" if $debug;
return $tag;
}

#also output a GraphML. 
sub dumptodisk{
	my @records = split(/\s+/, $NEWDESCRIPTION);
	my $start = 0;
	my ($sth, $sentence, $tag, $end, $filename, $content, $ptag, $sentstart);
	my ($stag, $clausecount, $taggedsent, %gmlid, $gmlid);
	$gmlid = 1;
	foreach (@records){
		if(/(.*?)\[(\d+)\]/){
			$filename = $1;
			$end = $2;
			$content = "<?xml version=\"1.0\"?><description>";
			#$ptag = ""; #previous tag
			$sentstart = 1;
			$clausecount = 0;
			for(my $i = $start; $i <= $end; $i++){
				 $sth = $dbh->prepare("select sentence, tag from sentence where sentid=\"$i\"");
				 $sth->execute();
				 ($sentence, $tag) = $sth->fetchrow_array();
         $tag =~ s#\s+$##;
         $tag =~ s#^\s+##;
         $tag =~ s#\s+#_#g;
         if($tag !~ /\w+/){
            $tag = "unknown";
         }
				 
				 if($sentstart){
				    if($clausecount != 0){
				       $content .= $clausecount > 1 ? "<$stag>".$taggedsent."</$stag>" : $taggedsent;
					}
					$stag = $tag."_block";#sentence tag
					$taggedsent = "";
				    $sentstart = 0;
					$clausecount = 0;
				 }
				 #if($tag eq "unknown"){
    		#		$taggedsent .= "<$ptag>$sentence</$ptag>";
					#$clausecount++;
    		#	 }else{
    			 $taggedsent .= "<$tag>$sentence</$tag>";
           #$ptag = $tag;
					 $clausecount++;
        # }
				 if($sentence =~/\.\s*$/){
				     $sentstart = 1;
				 }
				 $gmlid = add2GraphML($gmlid, $tag, $stag, \%gmlid);
			}
		    $content .= $clausecount > 1 ? "<$stag>".$taggedsent."</$stag>" : $taggedsent;
			$content .= "</description>";
      $content =~ s#[^-()\[\]<>_!`~\#$%^&/\\.,;:0-9a-zA-Z?="'+@ ]#*#g; #replace illegal char with *
      $filename =~ s#\.[^.]*$#\.xml#;
			open(OUT, ">$markeddir$filename") || die "$!: $markeddir$filename";
			print OUT $content;
			print "save [$content] to $markeddir$filename\n" if $debug;
			$start = $end+1;
		}
	}
	open(OUT, ">$graphmlfile") || die "$!: $graphmlfile";
	print OUT $GraphML."\n</graph>\n</graphml>";
	print "save [$GraphML] to $graphmlfile\n" if $debug;
}

#integrate a tag to the Graph
sub add2GraphML{
  my ($id, $tag, $ptag, $rgmlid) = @_; #$id: the next id to be assigned
  #my %gmlid = %$rgmlid;
  $ptag =~ s#_block##;
  my $cid = $$rgmlid{$tag};
  if($cid > 0){#a corresponding node exists
      if($ptag =~/\w+/ && $ptag !~/$tag/){#create an edge in between
	  	my $pid = $$rgmlid{$ptag};
		$GraphML .= "\n<edge source='$pid' target='$cid'>";
  	  	$GraphML .= "\n<data key='type'>element</data>";
  	    $GraphML .= "\n</edge>";
	  }else{#create an edge to the root
	    $GraphML .= "\n<edge source='0' target='$cid'>";
  	  	$GraphML .= "\n<data key='type'>element</data>";
  	    $GraphML .= "\n</edge>";
	  }
  }else{#create a new node and its edge
      $GraphML .= "\n<node id='$id'>";
  	  $GraphML .= "\n<data key='name'>$tag</data>";
  	  $GraphML .= "\n<data key='type'>element</data>";
  	  $GraphML .= "\n</node>";
 	  if($ptag =~/\w+/&& $ptag !~/$tag/){#create an edge in between
	  	my $pid = $$rgmlid{$ptag};
		$GraphML .= "\n<edge source='$pid' target='$id'>";
  	  	$GraphML .= "\n<data key='type'>element</data>";
  	    $GraphML .= "\n</edge>";
	  }else{#create an edge to the root
	    $GraphML .= "\n<edge source='0' target='$id'>";
  	  	$GraphML .= "\n<data key='type'>element</data>";
  	    $GraphML .= "\n</edge>";
	  }
	  $$rgmlid{$tag} = $id++;
  }
  return $id;
}
#just use up to the first noun as the tag to mark up remaining sentences
#if the first noun is too far away from the starting of a sentence, take the
#first two words as the tag
sub finalround{
	my ($id, $sent, $sth, $TAGS);
  $sth = $dbh->prepare("select tag from sentence group by tag order by count(sentid) desc");
  $sth->execute();
  $TAGS ="^\\w*\\s?\b(";
  while( my ($tag)=$sth->fetchrow_array()){
    $TAGS .= $tag."|" if ($tag=~/\w+/);
  }
  chop($TAGS);
  $TAGS .=")\b";
	$sth = $dbh->prepare("Select sentid, sentence from sentence where isnull(tag)");
	$sth->execute();
	while(($id, $sent) = $sth->fetchrow_array()){
		defaultmarkup($id, $sent, $TAGS);
	}
}
#check for sentence table for exsiting tags
#take up to the first n, if it is within 3 word range from the starting of the sentence
#else use tag "unknown"
sub defaultmarkup{
	my ($id,$sent, $TAGS) = @_;
	my (@words, $tag, $word, $count, $ptn);
    print "default markup: $sent\n" if $debug;
    @words = split(/[,:;.]/,$sent);
    @words = split(/\s+/, $words[0]);
	$sent = join(" ", @words);
	if($sent =~/$TAGS/i){
		$tag = substr($sent, $-[0], $+[0]-$-[0]);
		print "$tag is found in TAGS \n" if $debug;
		tag($id, $tag);return;
	}
	#ptn
	#$ptn = getPOSptn(@words);

    #take the first noun/nnp as default tag
    $count = 0;
    foreach $word (@words){
    	if($count++ > 4) {last;}
        if(checkpos($word,"one") =~/[psn]/){
    		$tag .= $word." ";
    	}elsif($tag ne ""){
			print "first [psn] $tag is used\n" if $debug;
    		tag($id, $tag);return;
    	}
    }
    if($tag ne ""){
	  print "first [psn] $tag is used\n" if $debug;
      tag($id, $tag);return;
    }
    #check for the first pl
    $count = 0;
    foreach $word (@words){
       if($count++ > 4) {last;}
       if(getnumber($word) eq "p"){
           $tag .= $word." ";
       }elsif($tag ne ""){
	       print "a likely [p] $tag is used\n" if $debug;
           tag($id, $tag);return;
       }
    }
    if($tag ne ""){
	   print "a likely [p] $tag is used\n" if $debug;
       tag($id, $tag);return;
    }
    #<TQ>
	tag($id, "unknown");
    print "unknown tag\n" if $debug;
	return;
}


########return a positive number if any new discovery is made in this iteration
########use rule based learning first for easy cases
########use instance based learning for the remining unsolved cases
sub discover{
	my $status = shift;
	my ($sid, $sentence, @startwords, $pattern, @matched, $round, $sth, $new, $lead, $tag);
	$sth = $dbh->prepare("select sentid,sentence,lead,tag from sentence where status=\"$status\"");
        $sth->execute();
	while(($sid, $sentence, $lead, $tag) = $sth->fetchrow_array()){
		if(ismarked($sid)){next;} #marked, check $sid for most recent info.
		@startwords = split(/\s+/,$lead);
		print "\n>>>>>>>>>>>>>>>>>>start an unmarked sentence [$sentence]\n" if $debug;
		$pattern = buildpattern(@startwords);
		print "Build pattern [$pattern] from starting words [@startwords]\n" if $debug;
		if($pattern =~ /\w+/){
			@matched = matchpattern($pattern, $status, 0); #ids of untagged sentences that match the pattern
			$round = 1;
		    $new = 0;
			do{
			    print "####################round $round: rule based learning on ".@matched." matched sentences\n" if $debug;
			    $new = rulebasedlearn(@matched); #grow %NOUNS, %BDRY, tag sentences in %SENTS, record %DECISIONS
				print "##end round $round";
				$round++;
			}while ($new > 0);
			#$round = 1;
			#$new = 0;
			#do{
			#    print "~~~~~~~~~~~~~~~~~~~~round $round: instance based learning on matched sentences\n" if $debug;
			#    $new = instancebasedlearn(@matched);
			#	$round++;
			#}while($new > 0);
		}
	}
}

#sentences that match the pattern
sub matchpattern{
my($pattern, $status, $hastag) = @_;
my ($stmt, $sth, @matchedids, $sentid, $sentence);
if($hastag){
$stmt = "select sentid,sentence from sentence where status=\"$status\" and !isnull(tag)";
}else{
$stmt = "select sentid,sentence from sentence where status=\"$status\" and isnull(tag)";
}
$sth = $dbh->prepare($stmt);
$sth->execute();
while(($sentid, $sentence) = $sth->fetchrow_array()){
	push(@matchedids, $sentid) if($sentence =~/$pattern/i)
}
return @matchedids;
}

####go over all un-decided sentences, solve these cases by using instance-based learning
#endings of marked sentences: "<d".$DECISIONID."-".lc $tag.">"
sub ismarked{
	my $id = shift;
	my $sth;
	$sth=$dbh->prepare("select * from sentence where sentid=\"$id\" and !isnull(tag)");
        $sth->execute();
	return $sth->rows != 0;
}
##@todo
sub instancebasedlearn{
	my @sentids = @_;
	my ($sent, $sentid, $lead, $tag, @words, $ptn, $index, $wordptn, $unique, $temp);
	my ($value, @unknown,$unknown, $text, $word, $pos, $role, $certainty, $sth, @matched, $flag, $sid, $new);
	my %register=("n","","s","","p","","b","","?","");
	foreach $sentid (@sentids){
                $flag = 0; #no new information may be found in matched cases
		$sth = $dbh->prepare("select sentence, lead, tag from sentence where sentid=\"$sentid\"");
		$sth->execute();
		($sent, $lead, $tag) = $sth->fetchrow_array();
		if(!ismarked($sentid)){
			print "For unmarked sentence: $sent\n" if $debug;
			if($sent =~/margins shallowly crenate ( shade forms )/){
			  print;
			}
			@words = split(/\s+/,$lead);
			$ptn = getPOSptn(@words);
			print "\t leading words: @words\n\t pos pattern: $ptn\n" if $debug;
			$index = index($ptn, "?");
			if(rindex($ptn,"?") == $index && length $ptn > 1 && $index >=0){ #only one unknown
    			$wordptn = buildwordpattern(join(" ", @words), $ptn); # nn? => flower buds \w+
				print "\t word pattern: $wordptn\n" if $debug;
    			@matched = matchwordptn($wordptn, @sentids); # ids of sentences matching the pattern: tagged or untagged, pos known or unknown
    			#check the leading words' POSs in @matched
        		foreach $sid (@matched){
        			$sth = $dbh->prepare("select lead from sentence where sentid=\"$sid\"");
					$sth->execute();
					($lead) = $sth->fetchrow_array();
					@words = split(/\s+/, $lead);
					$temp = $words[$index]; #the word at the position of "?"
					                        #we hope one of these words have a known pos, so we can infer others' pos
					if($unique !~ /\b$temp\b/){
					    $unique .= $temp." ";
						#@todo==fix syntax
        				($pos, $role, $certainty) = checkposinfo($temp,"one");
        				if($pos ne "2" && $pos ne "?"){
        					$register{$pos} .= $sid." \$\$\$\$ ";
							$flag = 1;
        				}elsif($pos eq "?"){
							$register{"?"} .= $sid." \$\$\$\$ ";
						}else{
						    print "$pos: $_\n";
						}
					}
        		}
				if($flag == 0) {
				    print "\t no new POS discovered by instance-based learning\n" if $debug;
				    return $new;
				}
     			@unknown = split(/\s*\$\$\$\$\s*/,$register{"?"});
				delete($register{"?"});
				foreach $unknown (@unknown){
						($pos, $role) = selectpos($unknown,$word,$index,%register);
						$new += update($word, $pos, $role) if($pos =~/\w/);

				}
			}
		}
#@todo                  check for and resolve any conflict with marked @matched sentences

		if($new > 0){
			foreach (@matched){
				markup($_) if (!ismarked($_));
			}
		}
	}
	return $new;
}

sub matchwordptn{
my($wordptn, @sentids) =@_;
my(@results, $sth, $sent);
foreach (@sentids){
 $sth = $dbh->prepare("select sentence from sentence where sentid=\"$_\"");
 $sth->execute();
 $sent = $sth->fetchrow_array();
 push(@results, $_) if $sent =~ /$wordptn/i
}
}

##look in %register for the most similar instance to $text
##to find $pos and $role for $word,
##and to find $tag for $text
sub selectpos{
	my ($text, $word, $index, %register) = @_;
	my ($ipos, $inst, $icertainty,$irole,$sim, $pos, $role, $tag, @instances);
	my $top = 0;

	print "\t seen examples in hash:\n" if $debug;
	print map{"\t\t $_=> ".substr($register{$_}, 0, 50)."\n"} keys %register if $debug;
	foreach $ipos (keys(%register)){
		@instances = split(/\$\$\$\$ /,$register{$ipos});#with known pos
		foreach $inst (@instances){
		#"<d".$DECISIONID."-".$tag.">"@todo
		 	if($inst =~ /(.*?)(?:<d\d+-([a-z]+)>)?\s*\(([0-9.]+)-([-_+]?)\)$/){
				$sim = sim($text, $1, $word, $index);
				$icertainty = $3;
			    $irole = $4;
				if($top < $sim){
					$top = $sim;
					$role = $irole;
					$pos = $ipos;
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
	my($sent1, $sent2,$word, $index, @a, @b) = @_;
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
	#@todo
    @t1 = checkpos($ba, "one");
	@t2 = checkpos($aa, "one");
	if(shift (@t1) eq shift (@t2)){ $sim += 0.5;}
	@t1 = checkpos($bb, "one");
	@t2 = checkpos($ab,"one");
	if(shift (@t1) eq shift (@t2)){ $sim += 0.5;}
	if($sim != 0){
	      print "\t\t similarity between ".substr($sent1, 0, 20)." and ".substr($sent2, 0, 20)." is $sim\n" if $debug;
		  return $sim;
	}
	print "\t\t similarity between ".substr($sent1, 0, 20)." and ".substr($sent2, 0, 20)." is 0\n" if $debug;

}

#mark up a sentence whose leading words have known pos. return the tag
sub markup{
	my $sentid = shift;
	my ($tag, $sign) = doit($sentid);
	tag($sentid, $tag);
}
#the length of the ptn must be the same as the number of words in @words
#if certainty is < 50%, replace POS with ?.
sub getPOSptn{
	my @words = @_;
	my ($pos, $certainty, $role, $ptn, @posinfo, $word);
	foreach $word (@words){
		@posinfo = checkposinfo($word,"top");
		#@todo:test
		#$pos = "?" if (@posinfo > 1); #if a word is marked as N and B, make it unknown
		($pos, $role, $certainty)= ($posinfo[0][0],$posinfo[0][1],$posinfo[0][2]);
		if ($pos ne "?" && tovalue($certainty) <= 0.5){
    		print "$word 's POS $pos has a certainty ".tovalue($certainty)." (<0.5). This POS is ignored\n" if $debug;
		   $pos = "?" ;
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
	my @sentids = @_; #an array of sentences with similar starting words
	my ($sign, $new, $tag, $sentid);
	foreach $sentid (@sentids){
    	if(!ismarked($sentid)){#without decision ids
			($tag, $new) = doit($sentid);
			tag($sentid, $tag);
			$sign +=$new;
		}
	}
	return $sign;
}

#update wordpos table (on certainty) when a sentence is tagged for the first time.
#this update should not be done when a pos is looked up, because we may lookup a pos for the same example multiple times.
#if the tag need to be adjusted (not by doit function), also need to adjust certainty counts.

sub doit{
	my ($sentid) = shift;
	my ($tag, $sign, $ptn, $i,$t, @ws, @tws, @cws, $pos, $certainty);
	my ($role, $start, $end, @t, $sentence, $sth, $lead, $sent);
	$sth = $dbh->prepare("select sentence, lead from sentence where sentid=\"$sentid\"");
	$sth->execute();
	($sentence, $lead) = $sth->fetchrow_array();

	@ws = split(/\s+/,$lead);
	$ptn = getPOSptn(@ws);
	print "\nsentence: $sentence\n" if $debug;

	if($ptn =~ /^[pns]$/){
        #single-word cases, e.g. herbs, ...
        $tag = $ws[0];
		$sign += update($tag, $ptn,"-");
		print "Directly markup with tag: $tag\n";
	}elsif($ptn =~ /ps/){#questionable 
	    #@todo test: stems multiple /ps/, stems 66/66, multiple 1/1
		print "Found [ps] pattern\n" if $debug;
		my $i = $+[0];
		my $s = $ws[$i-1];
		$i = $-[0];
		my $p = $ws[$i];
		my @p = checkposinfo($p, "one");
		my @s = checkposinfo($s, "one");
		my ($pcertainty, $pcount, $scertainty, $scount);
		($pos, $role, $pcertainty, $pcount) = ($p[0][0], $p[0][1], $p[0][2], $p[0][3]);
		($pos, $role, $scertainty, $scount) = ($s[0][0], $s[0][1], $s[0][2], $s[0][3]);
		if($pcertainty >= $scertainty && $pcount > $scount*2){
		   discount($s, "s");
		   @tws = splice(@ws, 0, $i+1);
		   $tag = join(" ",@tws);
		   print "\t:determine the tag: $tag\n" if $debug;
		   $sign += update($p, "p", "");
		   $sign += updatenn($-[1], $#tws+1, @tws) if($ptn=~/^([nsp]+p)s/);
		   if($scertainty * $scount < 2){
		   $sign += update($s, "b","");
		   }
		}elsif($scertainty >= $pcertainty && $scount > $pcount*2){
		   discount($p, "p");
		   $sign += update($s, "s", "");
		}		
   }elsif($ptn =~/p(\?)/){#case 3,7,8,9,12,21: p? => ?->%BDRY
		#use up to the pl as the tag
		#if ? is not "p"->%BDRY
		#save any NN in %NOUNS, note the role of Ns
		$i = $-[1];#index of ?
		#what to do with "flowers sepals" when sepals is ?
		print "Found [p?] pattern\n" if $debug;
		if(getnumber($ws[$i]) eq "p"){
		   $tag = $ws[$i-1];
		   $sign += update($tag, "p","-");
		}else{
		    @cws = @ws;
    		@tws = splice(@ws,0,$i);#get tag words, @ws changes as well
    		$tag = join(" ",@tws);
    		print "\t:determine the tag: $tag\n" if $debug;
    		print "\t:updates on POSs\n" if $debug;
    		$sign += update($cws[$i], "b", "");
			$sign += update($cws[$i-1],"p", "-");
			$sign += updatenn($-[1],$#tws+1, @tws) if($ptn=~/^([nsp]+p)\?/);
		}
	}elsif($ptn =~ /[psn](b)/){#case 2,4,5,11,6,20: nb => collect the tag
		#use up to the N before B as the tag
		#save NN in %NOUNS, note the role of Ns
		#anything may be learned from rule 20?
		print "Found [[psn](b)] pattern\n" if $debug;
		$i = $-[1]; #index of b
		$sign += update($ws[$i], "b","");
		@cws = @ws;
		@tws = splice(@ws,0,$i);#get tag words, @ws changes.
		$tag = join(" ",@tws);
		$sign += update($cws[$i-1], substr($ptn, $i-1, 1), "-");
		$sign += updatenn($-[1], $#tws+1,@tws) if($ptn=~/^([nsp]+[psn])b/);
		print "\t:determine the tag: $tag\n" if $debug;
		print "\t:updates on POSs\n" if $debug;
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
		$i = $-[1];#index of (b)
		$sign += update($ws[$i], "b", "");
        @cws = @ws;
		@tws = splice(@ws,0,$i);#get tag words
		$tag = join(" ",@tws);
		print "\t:determine the tag: $tag\n" if $debug;
		print "\t:updates on POSs\n" if $debug;
		$sign += update($cws[$i-1], "n", "-");
		$sign += updatenn($-[1],$#tws,@tws) if($ptn =~/^([nsp]+\?)b/);
	}elsif($ptn =~ /([psn][psn])/){#case 1,3,10,19: nn is phrase or n2 is a main noun
		#if nn is phrase or n2 is a main noun
		#make nn the tag
		#the word after nn ->%BDRY
		#save NN in %NOUNS, note the role of Ns
		print "Found [[psn][psn]] pattern\n" if $debug;
		$start = $-[1];
		$end = $+[1];
		@cws = @ws;
		if($ptn =~/pp/){
		    $tag = $ws[$start];
			@t = checkposinfo($tag, "one");#first p is the tag
		}else{
    		@tws = splice(@ws,$start, $end-$start);#get tag words
    		$tag = join(" ",@tws);
    		print "\t:updates on POSs\n" if $debug;
			@t = checkposinfo($tws[@tws-1], "one");
		}
    	($pos, $role, $certainty) = ($t[0][0], $t[0][1], $t[0][2]);  #last n
    	if($pos=~/[psn]/ && $role =~ /[-+]/){
    		#the word after nn ->%BDRY
    		@t = split(/\s+/,$sentence);
    		$sign += update($t[$end+1],"b",""); #@todo:test
			$sign += update($cws[$start], substr($ptn, $start, 1), "");
			$sign += update($cws[$start+1], substr($ptn, $start+1, 1), "");
    		$sign += updatenn(0, $#tws+1, @tws);
    	}else{
    		print "\t:$tag not main role, reset tag to null\n" if $debug;
			$tag = "";
    	}
    	print "\t:determine the tag: $tag\n" if $debug;
	}elsif($ptn =~ /b\?([psn])$/){#case 16, 25
		#if n can be a main noun
		#make n the tag
		#the word after n ->%BDRY
		print "Found [b?[psn]] pattern\n" if $debug;
		$end = $-[1];#index of n
		@cws = @ws;
		@tws = splice(@ws, 0, $end+1);
		$tag = join(" ",@tws);
		@t = checkposinfo($ws[$end],"one");#n's posinfo
		($pos, $role, $certainty) = ($t[0][1], $t[0][1], $t[0][2]);
		if($role =~ /[-+]/){
			#the word after n ->%BDRY
		    @t = tokenize($sentence);
			print "\t:updates on POSs\n" if $debug;
			$sign += update($t[$end+1],"b",""); #test
			$sign += update($cws[$end-2], "b", "");
			$sign += update($cws[$end], substr($ptn, $end, 1), $role);
		}else{
			print "\t:$tag not main role, reset tag to null\n" if $debug;
			$tag = "";
		}
		print "\t:determine the tag: $tag\n" if $debug;
	}elsif($ptn =~ /^s(\?)$/){
	    #? =>b
		#@todo:test
		$i = $-[1];#index of ?
		print "Found [^s?\$] pattern\n" if $debug;
		$tag = $ws[$i-1];
		print "\t:determine the tag: $tag\n" if $debug;
		print "\t:updates on POSs\n" if $debug;
		$sign += update($ws[$i], "b", "");
		$sign += update($ws[$i-1], "s", "-");
	}elsif($ptn =~ /^bs$/){
	    $tag = join(" ", @ws);
		$sign += update($ws[0], "b", "");
		$sign += update($ws[1], "s", "-");
	}else{
		print "Pattern [$ptn] is not processed\n" if $debug;
	}
	return ($tag,$sign);
}

sub tag{
	#my ($sentence, $tag) = @_;
	my($sid, $tag) = @_;
	my $sth;
	if($tag =~/[^-a-zA-Z ]/){
			print;
	
	}
	if($tag !~ /\w+/){return;}
	$tag = lc $tag;
	if($tag =~ /\w+/){
	   $sth = $dbh->prepare("update sentence set tag =\"$tag\" where sentid = \"$sid\"");
	   $sth->execute();
	   print "\t:mark up ".$sid."\n" if $debug;
	}
}

#####discount 1 from certainty for word
sub discount{
my($word, $pos) =@_;
my ($sth1, $cu, $cl);
$word = lc $word;
$sth1 = $dbh->prepare("select certaintyu, certaintyl from wordpos where word=\"$word\" and pos=\"$pos\"");
$sth1->execute();
($cu, $cl) = $sth1->fetchrow_array();
if(--$cu == 0){
#remove this record
$sth1 = $dbh->prepare("delete from wordpos where word=\"$word\" and pos=\"$pos\"");
$sth1->execute();
}else{
#update
$sth1 = $dbh->prepare("update wordpos set certaintyu=$cu where word=\"$word\" and pos=\"$pos\"");
$sth1->execute();
}

}
#####for the nouns in @words, make the last n the main noun("-").
#####update NN's roles
#####return a positive number if an update is made,
sub updatenn{
	my($start,$end,@words) = @_;
	my ($update, $i);
	@words = splice(@words, $start, $end-$start);#all Ns
	#for($i = 0; $i < @words-1; $i++){#one N at a time
	#	$update += update($words[$i],"n","_");#modifier
	#}
	#$update += update($words[$i], "n", "-"); #last one is the main noun
	$update += update(join(" ",@words), "n","") if @words >=2;#NN altogether
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
	my ($word, $pos, $role) = @_; #("Base", "n", "_") or ("Base", "b", ""), 
	my ($sth1, $sth, $new, $oldrole, $certaintyu, $certaintyl);
	$word = lc $word;
	$word =~ s#\s+$##;
	$word =~ s#^\s*##;
	if($word !~ /\w/){return;}

	if($pos eq "n"){
		$pos = getnumber($word);
	}
	$sth1 = $dbh->prepare("select role, certaintyu from wordpos where word=\"$word\" and pos=\"$pos\"");
	$sth1->execute();
	if($sth1->rows() == 0){
	    #new pos, insert new record
		$certaintyu = 1;
		$sth = $dbh->prepare("insert into wordpos values(\"$word\",\"$pos\", \"$role\",$certaintyu,0)");
	    $sth->execute();
		$new = 1;
		print "\t: new [$word] pos=$pos, role =$role, certaintyu=$certaintyu\n" if $debug;
	}else{
	    #seens pos, update role and certainty
	    ($oldrole, $certaintyu) = $sth1->fetchrow_array();
		$role = mergerole($oldrole, $role);
		$certaintyu++;
		$sth = $dbh->prepare("update wordpos set role =\"$role\", certaintyu =$certaintyu where word=\"$word\" and pos=\"$pos\"");
	    $sth->execute();
		print "\t: update [$word($pos)] role: $oldrole=>$role, certaintyu=$certaintyu\n" if $debug;
	}
	#update certaintyl
	$sth = $dbh->prepare("select sum(certaintyu) from wordpos where word=\"$word\"");
    $sth->execute();
	($certaintyl) = $sth->fetchrow_array();
	$sth = $dbh->prepare("update wordpos set certaintyl=$certaintyl where word=\"$word\"");
    $sth->execute();
	print "\t: total occurance of [$word] =$certaintyl\n" if $debug;
	return $new;
}

sub mergerole{
	my($role1, $role2) = @_;
	if($role1 eq ""){
	    return $role2;
	}elsif($role2 eq ""){
	    return $role1;
	}elsif($role1 ne $role2){
		return "+"; 
	}else{
		return $role1;
	}
}

#sub setpos{
#    #return 1 if new pos/role is set, otherwise return 0
#	my ($word, $pos, $certainty, $role) = @_;
#	my $new = 0;
#	my ($certaintyu, $certaintyl, $sth);
#	chomp($word);
#	$word =~ s#^\s*##;
#	$word = lc $word;
#	($certaintyu, $certaintyl) = split(/\//, $certainty);
#	if($word =~ /shade forms \) to/){
#	    print;
#	}
#	if($pos eq "n"){
#		$pos = getnumber($word); #update pos may be p,s, or n
#	}
#	#select into wordpos
#	$sth = $dbh->prepare("select count(*) from wordpos where word=\"$word\" and pos=\"$pos\"");
#	$sth->execute();
#	if($sth->fetchrow_array() == 0){
#	    #update
#		$sth = $dbh->prepare("insert into wordpos values(\"$word\",\"$pos\", \"$role\",\"$certaintyu\",\"$certaintyl\")");
#	    $sth->execute();
#		$new = 1;
#	}
#	return $new;
#}
####
sub getnumber{
	my $word = shift;
	return "p" if ($word =~ /sori$/ || $word =~/indusia$/ || $word =~/phyllodia$/ || $word =~/pinnae/); 
	return "" if($word =~/ous$/);
	return "p" if ($word =~ /es$/ || $word =~ /s$/);
    return "s";
}

#####return pos string
sub checkpos{
my ($word,$mode) = @_;#$mode = "one","top", "all"
my @posinfo = checkposinfo($word, $mode);
if($mode eq "one"){
	return $posinfo[0][0];
}elsif($mode eq "top"){
    #@todo
}

}
sub printposinfo{
my @posinfo =@_;
my ($string, $i, $j);
for($i = 0; $i<@posinfo; $i++){
   for($j =0; $j <3; $j++){
     $string .= $posinfo[$i][$j]." ";
}
   $string .= "\n";
}
return $string;
}


########return an 2d array of (pos,role, certainty, certaintyl)
sub checkposinfo{
	my ($word,$mode) = @_;#$mode = "one","top", "all"
	#"one":the top one, "top":the ones with top certainty, "all":all
	my ($pos, $role, $certaintyu, $certaintyl, $certainty, $maxcertainty);
	my ($sth, @results, $count, $stmt);
	$word = lc $word;
	$word =~ s#\s+$##;
	$word =~ s#^\s*##;
	#select pos from wordpos
	$stmt = "select pos, role, certaintyu, certaintyl from wordpos where word=\"$word\" order by certaintyu/certaintyl";
	$sth = $dbh->prepare($stmt);
	$sth->execute();
	if($sth->rows ==0){
	    my @temp = ("?", "?", "?");
	    $results[0] = \@temp;
	    return @results;
	}
	$maxcertainty = 0;
	$count = 0;
	while(($pos, $role, $certaintyu, $certaintyl)=$sth->fetchrow_array()){
	    if($maxcertainty <= $certaintyu/$certaintyl){
			$maxcertainty = $certaintyu/$certaintyl;
			my @temp = ();
			push(@temp, $pos, $role, $certaintyu."/".$certaintyl);
			$results[$count++] = \@temp;
		}else{
		    if($mode eq "one"){
			    return chooseone(@results);
			}elsif($mode eq "top"){
				return @results;
			}elsif($mode eq "all"){
			    $maxcertainty = 0;
				my @temp = ();
				push(@temp, $pos, $role, $certaintyu."/".$certaintyl, $certaintyl);
				$results[$count++] = \@temp;
			}
		}
	}
	return @results;
}

### find the one with the greatest $certaintyl (since the value of certainty is the same)
sub chooseone{
my @results = @_;
my ($index, $max, $pos, $role, $certaintyu, $certaintyl, @result, $i);
$max = 0;
for($i = 0; $i < @results; $i++){
  ($pos, $role, $certaintyu, $certaintyl) = ($results[$i][0],$results[$i][1],$results[$i][2],$results[$i][3]) ;
  $max = $certaintyl if $max < $certaintyl;
  $index = $i if $max < $certaintyl;
}

$result[0] = $results[$index];
return @result;
}

	#@todo
#if(defined $posn && !defined $posb){ #"s1/2_"
#	    if($posn =~ /([psn])(\d+\/\d+)([-_+]?)/){
#			return ($1,$2,$3); #("s", "1/2", "_")
#		}else{
#			print STDERR "POS in wrong format: $posn\n";
#		}
#	}elsif(defined $posb && !defined $posn){
#		return ("b", $posb,"");
#	}elsif(!defined $posn && !defined $posb){
#		return ("?","?","");
#	}else{#has conflict POSs, return both
#		return ("2", $posn."b".$posb, ""); #"s1/2_b1/2"
#	}



########return an array
sub getleadwords{
	my $sentid = shift;
	my ($sth,$lead);
	$sth = $dbh->prepare("select lead from sentence where sentid=\"$sentid\"");
	$sth->execute();
	$lead = $sth->fetchrow_array();
	return split(/\s+/,$lead);
}


########e.g. given $ptn = NN?,
########           $words = flower buds few,
########     return pattern "/\d+\{.*?\}flower buds \w+/i"
########should not rely on the length of $ptn or the number of words in $words
sub buildwordpattern{
	my ($words, $ptn) = @_;
	my $pattern = "\\d+\{.*?\}";
	my @w = split(/\s+/, $words);
        my $i;
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
	my ($pattern, $tmp, $i);
        my $prefix="\\w+\\s";
	print ("CHECKEDWORDS is\n[$CHECKEDWORDS]\n") if $debug;
	#identify new words
	foreach (@words){
		if($_!~ /[[:punct:]0-9]/ && $CHECKEDWORDS !~ /:$_:/i){
			$tmp .= $_."|"; #(cat|dogs|fish)
			push(@newwords,$_);
		}
	}
	if($tmp !~ /\w/){return "";}#no new words
	#build pattern out of the new words
	chop($tmp);
	print ("Pattern segment: [$tmp]\n") if $debug;
	$tmp = "\\b(?:".$tmp.")\\b"; 
	$pattern ="^".$tmp."|";
	#$pattern =$tmp."|";
	for($i = 0; $i < $N-1; $i++){
		$tmp = $prefix.$tmp;
		$pattern .= "^".$tmp."|";
		#$pattern .=$tmp."|";
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
my ($file, $text, @sentences,@words,@tmp,$status,$lead,$stmt,$sth);
opendir(IN, "$dir") || die "$!: $dir\n";
while(defined ($file=readdir(IN))){
	if($file !~ /\w/){next;}
	$text = ReadFile::readfile("$dir$file");
	#@todo: use [PERIOD] replace . etc. in brackets. Replace back when dump to disk.
	@sentences = SentenceSpliter::get_sentences($text);#@todo: avoid splits in brackets. how?
	foreach (@sentences){
		#may have fewer than $N words
		if(!/\w+/){next;}
		s#(\W)# $1 #g;
	    s#\s+# #g;
		@words = getfirstnwords($_, $N); # "w1 w2 w3"
		if(checkpos($words[0],"one") =~ /p/){
		     $status = "start";
		}else{
		    $status = "normal";
		}
		$lead = "@words";
		$lead =~ s#\s+$##;
		$lead =~ s#^\s*##;
		$lead =~ s#\s+# #g;
		if(length $lead > 50){
				  print;
		}
		$stmt = "insert into sentence(sentid, sentence, lead, status) values(\"$SENTID\", \"$_\",\"$lead\", \"$status\")";
		$sth = $dbh->prepare($stmt);
	    $sth->execute();
		#print "Sentence: $_\n" if $debug;
		#print "Leading words: @words\n\n" if $debug;
		$SENTID++;
	}
	my $end = $SENTID-1;
	$NEWDESCRIPTION.=$file."[".$end."] ";
	my $query = $dbh->prepare("insert into linkage values ('$file', $end)");
	$query->execute() or warn $query->errstr."\n";
}
print "Total sentence=$SENTID" if $debug;
}

sub getfirstnwords{
	########return the first up to $n words of $sentence as an array, excluding
	my($sentence, $n) = @_;
	my (@words, $index);
	if($sentence =~ /^pollen sacs 0/i){
	print;
	}
	@words = tokenize($sentence);
	#print "words in sentence: @words\n" if $debug;
	@words = splice(@words, 0, $n);
	return @words;
}

sub tokenize{
    my $sentence = shift;
	my ($index, @words);
    if($sentence =~ / [(\[,:;.]/){
		$index = $-[0];
	}else{
		$index = length($sentence);
	}
	$sentence = substr($sentence, 0, $index);

	#$sentence =~ s#[[:punct:]]# #g; #remove all punct. marks
	$sentence =~ s#\W# #g;
	$sentence =~ s#\d+# NUMBER #g;
	$sentence =~ s#\s+$##;
	$sentence =~ s#^\s+##;
	@words = split(/\s+/, $sentence);
    return @words;
}

#my $special = "[´–×±]";
