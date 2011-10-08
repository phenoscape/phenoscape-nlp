#!C:\Perl\bin\perl

#Nov. 17 2008
#This version output GraphML for generation of visualization among organs and sub-organs.
#This version also assemble marked clauses into description documents and dump them to disk
#This version uses extra parameters
#This version output raw marked-up clauses with "unknown" tag.
#Newer development of unsupervised clause markup is done on unsupervisedClauseMarkupBenchmarked.pl
#The latter implements some additional new or replacement module to achieve ideas discussed in the FNAv19BenchmarkDocumentation.doc 


#infer nnps (concept) from text.
#interact with end users for properties
#output marked descriptions to disk
#output GraphML for profuse visualization
#use rdf.pl to output RDF file describing the semantic structure of the text collection 

#use WordNet::QueryData;

#2008/10/21 Hong
#add an originalsent field in sentence table, originalsent retains ()
#add an source field in sentence table, values like filename-sent#

#earlier modifications:
#prevent doit to direct process a and/or b cases solved the following problems.
#stems and xxx =>and(b)
#taprooted and /or => ?b(and)
#homophyllous or , rarely , heterophyllous => ?b(or)

#need to fix and/or
#"perianth and androecium hypogynous ..." tag determined in andor as perianth and androecium hypogynous

#TODO:
#select fossil_2000_nkb_corpus.sentence.sentid, fossil_2000_nkb_corpus.sentence.sentence, fossil_2000_nkb_corpus.sentence.tag, fossil_2000_corpus.sentence.tag 
#from fossil_2000_nkb_corpus.sentence, fossil_2000_corpus.sentence 
#where fossil_2000_nkb_corpus.sentence.sentid = fossil_2000_corpus.sentence.sentid and fossil_2000_nkb_corpus.sentence.tag != fossil_2000_corpus.sentence.tag; 
#??why some sentences are marked as "muscle/openning" while other similar ones as "unknown"?
#??
#?? in fossil_2000: the use of verb "resembles"
#??

#for the program to run, install perl, DBD-mysql perl module, mysql database, wordnet commandline
#use lib 'C:\\Documents and Settings\\hongcui\\Desktop\\WorkFeb2008\\Projects\\unsupervised\\';
#use lib 'C:\\fna\\code\\Unsupervised\\';
#use encoding "iso-8859-1"; #latin1
#use encoding "cp1252"; #cp1252 is not the same as iso-8859-1, although they sometimes are all referred to as latin1!!!
use lib 'C:\\Documents and Settings\\hongcui\\workspace\\Unsupervised\\';
use NounHeuristics;
use InitialMarkup;
use SentenceSpliter;
use ReadFile;
use strict;
use DBI;

my $debug = 0;
my $debugp = 0; #debug pattern
my $kb = "knowledgebase";

#infer NNP from collection of initally marked descriptions
#mark up descriptions with new tags.

#Unsupervised\src>perl unsupervised.pl C:\Docume~1\hongcui\Desktop\WorkFeb2008\Projects\unsupervised\ fna630-2-text test seednouns.txt learntnouns.txt graphml.xml fna630-2-text
#perl unsupervised.pl C:\Docume~1\hongcui\Desktop\WorkFeb2008\Projects\ fossil fossil-result seednouns.txt learntnouns.txt graphml.xml fossil

if(@ARGV != 7) {
my $l = @ARGV;
print $l."\n";
print $ARGV[0]."\n";
print $ARGV[1]."\n";
print @ARGV;
print "\nUnsupervised.pl learns nouns/subjects from a folder of plain text descriptions and outputs the results to a number of save-to folder, file, or database. All file/folder names are based on work directory\n";
print "\nUsage:\n";
print "\tperl unsupervised.pl [argument]+\n";
print "\targments in this order:\n";
print "\twork directory\n";
print "\ttext files' folder name\n";
print "\tsave-to folder name\n";
print "\tseed noun file name\n";
print "\tlearnt noun save-to file name\n";
print "\tGraphML save-to file name\n";
print "\tprefix: the prefix for the database\n";
#print "\tuse wordnet: true or false\n";
print "\tResults will be saved to the database 'prefix_corpus' \n";
exit(1);
}
print stdout "Initialized:\n";

my $prefix = $ARGV[6];
my $workdir = $ARGV[0];
my $dir = $workdir.$ARGV[1]."\\";#textfile
my $markeddir = $workdir.$ARGV[2]."\\" ;
my $seedsfile = $workdir.$ARGV[3];#line: noun1[spn]
my $hnouns = $workdir.$prefix."_".$ARGV[4];
my $graphmlfile = $workdir.$prefix."_".$ARGV[5];
#my $wordnet = $ARGV[7] eq "true"? 1 : 0;
my $db = $prefix;

#infer NNP from collection of initally marked descriptions
#mark up descriptions with new tags.

my $host = "localhost";
my $user = "root";
my $password = "root";
my $dbh = DBI->connect("DBI:mysql:host=$host", $user, $password)
or die DBI->errstr."\n";
#$dbh->do("set names 'latin1'"); #set charset to latin1, i.e. cp1252.

my $haskb = kbexists();
$haskb = 0;
setupdatabase();

#set up global variables
my $CHECKEDWORDS = ":"; #leading three words of sentences
my $N = 3; #$N leading words
my $SENTID = 0;
my $DECISIONID = 0;
my %WNNUMBER =(); #word->(p|s)
my %WNSINGULAR = ();#word->singular
my %WNPOS = ();   #word->POSs
my $NEWDESCRIPTION =""; #record the index of sentences that ends a description
my %WORDS = ();
my %PLURALS = ();
my %NUMBERS = (0, 'zero', 1, 'one', 2, 'two', 3, 'three',4,'four',5,'five',6,'six',7,'seven',8,'eight',9,'nine');
my $GraphML = "<?xml version=\"1.0\"?><graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\"\n xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd\">\n<key id=\"name\" for=\"node\" attr.name=\"name\" attr.type=\"string\"/>\n<key id=\"type\" for=\"all\" attr.name=\"type\" attr.type=\"string\"/>\n<key id=\"weight\" for=\"all\" attr.name=\"weight\" attr.type=\"float\"/>\n<graph edgedefault=\"directed\">\n";
$GraphML .= "\n<node id='0'>";
$GraphML .= "\n<data key='name'>description</data>";
$GraphML .= "\n<data key='type'>element</data>";
$GraphML .= "\n</node>";

if(! -e $markeddir){
system("mkdir", $markeddir);
print "$markeddir created\n" if $debug;
}

#initialize wordpos table with nouns and boundary words
my $forbidden ="to"; #words in this list can not be treated as boundaries "to|a|b" etc.

if($haskb){
	importfromkb();
}

my @nouns1 = NounHeuristics::heurnouns($dir, $hnouns);
print  "nouns learnt from heuristics:\n@nouns1\n" if $debug;

my @nouns = (); #holds the seed nnps and nnps discovered later
if($seedsfile !~ /\\null\b/){
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
my $stop = $NounHeuristics::STOP;
$stop =~ s#\|(and|or)\|#|#g;
my @stops = split(/\|/,$stop);
push(@stops, "NUMBER"); #"0","1", "2","3","4","5","6","7","8","9"
print "stop list:\n@stops\n" if $debug;

for (@stops){
	#$BDRY{$_} = "1/1"; #certainty NumOfThisDecision/NumOfTotalDecision
	#insert to table wordpos
	#my $w = lc $_;
	my $w = $_;
	if($w !~ /\w/ || $w =~/\b(?:$forbidden)\b/){next;}
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
				  #$w = lc $1;
    		      $w = $1;
    		      my $sth = $dbh->prepare("select * from wordpos where word ='".$w."' and pos='".$2."'");
    		      $sth->execute();
    		      if($sth->rows() < 1){
				  	$sth = $dbh->prepare("insert into wordpos values(\"$w\",\"$2\",\"\",1,1)");
	              	$sth->execute();
    		      }
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
print stdout "Reading sentences:\n";
populatesents();

print stdout "Learning rules with high certainty:\n";
discover("start");

print stdout "Bootstrapping rules:\n";
discover("normal");

print stdout "Final walkthrough:\n";
print "##############final round--mark up with default tags\n" if $debug;
finalround();

print stdout "Handling 'and' and 'or':\n";
print "##############andor cases\n" if $debug;
andor();
normalizetags();

#tagunknowns();

print "##############save marked examples to disk\n" if $debug;
print stdout "Writing to disk:\n";
dumptodisk();

print stdout "Done:\n";

sub kbexists{

my $test = $dbh->prepare('show databases') or die $dbh->errstr."\n";
$test->execute() or die $test->errstr."\n";
my ($database, $removedb);
while($database = $test->fetchrow_array()){
	if($database eq $kb){
		return 1;
	}
}
return 0;
}

sub importfromkb{
    #import from learnedboundary and learnedstates to "b"
    #impprt from learnedstructures to "n"
    #forbidden word to "f"
	my ($stmt1, $sth1, $stmt2, $sth2, $w, @forbid);
	
	@forbid = split(/\|/, $forbidden);
	foreach (@forbid){
		$stmt2 ="insert into wordpos values(\"$_\",\"f\",\"\",1,1)";
		$sth2 = $dbh->prepare($stmt2);
		$sth2->execute();
	}
	
	
	$stmt1 = "select distinct word from ".$kb.".learnedboundary where word !='' and not isnull(word)";
	$sth1 = $dbh->prepare($stmt1);
	$sth1->execute() or die $sth1->errstr."\n";
	while($w = $sth1->fetchrow_array()){
		if($w !~ /\w/ || $w =~/\b(?:$forbidden)\b/){next;}
		$stmt2 ="insert into wordpos values(\"$w\",\"b\",\"\",1,1)";
		$sth2 = $dbh->prepare($stmt2);
		$sth2->execute();
	}
	
	$stmt1 = "select distinct state from ".$kb.".learnedstates where state !='' and not isnull(state)";
	$sth1 = $dbh->prepare($stmt1);
	$sth1->execute() or die $sth1->errstr."\n";
	while($w = $sth1->fetchrow_array()){
		if($w !~ /\w/ || $w =~/\b(?:$forbidden)\b/){next;}
		$stmt2 ="insert into wordpos values(\"$w\",\"b\",\"\",1,1)";
		$sth2 = $dbh->prepare($stmt2);
		$sth2->execute();
	}
	
	$stmt1 = "select distinct tag from ".$kb.".learnedstructures where tag !='' and not isnull(tag)";
	$sth1 = $dbh->prepare($stmt1);
	$sth1->execute() or die $sth1->errstr."\n";
	while($w = $sth1->fetchrow_array()){
		if($w !~ /\w/ || $w =~/\b(?:$forbidden)\b/){next;}
		$stmt2 ="insert into wordpos values(\"$w\",\"n\",\"\",1,1)";
		$sth2 = $dbh->prepare($stmt2);
		$sth2->execute();
	}
}

sub setupdatabase{

#my $test = $dbh->prepare('show databases')
#or die $dbh->errstr."\n";
#$test->execute() or die $test->errstr."\n";
#my ($database, $removedb);
#while($database = $test->fetchrow_array()){
#if($database eq $db){
#$removedb = 1;
#last;
#}
#}

#if($removedb){
#my $test = $dbh->prepare('drop database '.$db)
#or die $dbh->errstr."\n";
#$test->execute() or die $test->errstr."\n";
#}

my $test = $dbh->prepare('create database if not exists '.$db.' CHARACTER SET utf8')
or die $dbh->errstr."\n";
$test->execute() or die $test->errstr."\n";

my $test = $dbh->prepare('use '.$db)
or die $dbh->errstr."\n";

$test->execute() or die $test->errstr."\n";

my ($create, $del);

$create = $dbh->prepare('create table if not exists sentence (sentid int(11) not null unique, source varchar(500), sentence varchar(2000), originalsent varchar(2000), lead varchar(50), status varchar(20), tag varchar(150),modifier varchar(150), charsegment varchar(500),primary key (sentid)) engine=innodb');
$create->execute() or warn "$create->errstr\n";
$del = $dbh->prepare('delete from sentence');
$del->execute();


$create = $dbh->prepare('create table if not exists wordpos (word varchar(50) not null, pos varchar(2) not null, role varchar(5), certaintyu int, certaintyl int, primary key (word, pos)) engine=innodb');
$create->execute() or warn "$create->errstr\n";
$del = $dbh->prepare('delete from wordpos');
$del->execute();

$create = $dbh->prepare('create table if not exists sentInFile (filename varchar(200) not null unique primary key, endindex int not null) engine=innodb');
$create->execute() or warn "$create->errstr\n";
$del = $dbh->prepare('delete from sentInFile');
$del->execute();

$create = $dbh->prepare('create table if not exists modifiers (word varchar(50) not null unique primary key, count int) engine=innodb');
$create->execute() or warn "$create->errstr\n";
$del = $dbh->prepare('delete from modifiers');
$del->execute();

$create = $dbh->prepare('create table if not exists isA (autoid int not null auto_increment primary key, instance varchar(50), class varchar(50)) engine=innodb');
$create->execute() or warn "$create->errstr\n";
$del = $dbh->prepare('delete from isA');
$del->execute();

}

#annotated unknown sentences with the terms learned
#tags: s, p, b
#many flat teeth => <b>many</b> flat <p>teeth</p> 
sub tagunknowns{
	my ($sth, $sth1, $sent, $sentid, $p, $s, $b, $word);
	$sth = $dbh->prepare("select word from wordpos where pos ='p'");
	$sth->execute() or warn "$sth->errstr\n";
	while(($word) = $sth->fetchrow_array()){
		$p .= $word."|";
	}
	chop($p);
	
	$sth = $dbh->prepare("select word from wordpos where pos ='s'");
	$sth->execute() or warn "$sth->errstr\n";
	while(($word) = $sth->fetchrow_array()){
		$s .= $word."|";
	}
	chop($s);
	
	$sth = $dbh->prepare("select word from wordpos where pos ='b'");
	$sth->execute() or warn "$sth->errstr\n";
	while(($word) = $sth->fetchrow_array()){
		$b .= $word."|";
	}
	chop($b);
	
	$sth = $dbh->prepare("select sentid, sentence from sentence where tag ='unknown' and lead not like 'similar to %'");
	$sth->execute() or warn "$sth->errstr\n";
	while(($sentid, $sent) = $sth->fetchrow_array()){
		$sent = annotateSent($sent, $p, $s, $b);
		$sth1 = $dbh->prepare("update sentence set sentence ='".$sent."' where sentid =".$sentid);
		$sth1->execute() or warn "$sth1->errstr\n";
	}
}

sub annotateSent{
	my ($sent, $p, $s, $b) = @_;
	$sent =~ s#\b($p)\b#<p>\1</p>#g;
	$sent =~ s#\b($b)\b#<b>\1</b>#g;
	$sent =~ s#\b($s)\b#<s>\1</s>#g;	
	return $sent;
}



#<tag modifier="">
#e.g. <plate modifier="basal"> === <basal plate>
sub normalizetags{
  my ($sth,$sth1, $tag, $ntag, $sth1, $sentid, @words, $modifier, $i, $j, $tmp);
  $sth = $dbh->prepare("select sentid, tag from sentence where !isnull(tag) and tag not regexp ' (and|or) '");
  #$sth = $dbh->prepare("select * from sentence");
  $sth->execute() or warn "$sth->errstr\n";
  while(($sentid, $tag) = $sth->fetchrow_array() ){
      #$ntag = lc($tag);
      $ntag = $tag;
      if( $ntag =~/\w+/){
       if($ntag !~ /\b($NounHeuristics::STOP)\b/){
        @words = split(/\s+/, $ntag);
        #print "Tag:Words=> $tag : @words\n";
	for($i = 1; $i <=@words; $i++){
		$tmp = singular($words[@words-$i]);
		if($tmp ne "v"){ last;}
	}
        $tag = $i <= @words? $tmp: "unknown";
        splice(@words, @words-$i<=0? 0 : @words-$i);
        $modifier = join(" ", @words);
        $sth1 = $dbh->prepare("update sentence set tag = \"".$tag."\", modifier = \"".$modifier."\" where sentid = ".$sentid);
        $sth1->execute() or die "$sth->errstr\n";
       }else{
        # treat them case by case
        #case 1: in some species, abaxially with =>unknown
        if($ntag =~/^in / || $ntag =~/\b(with|without)\b/){
          $sth1 = $dbh->prepare("update sentence set tag = \"unknown\", modifier = \"\" where sentid = ".$sentid);
          $sth1->execute() or die "$sth->errstr\n";
        }else{
          #case 2: at least some leaves/all filaments/all leaves/more often shrubs/some ultimate segements
          my $t = $ntag;
          $ntag =~ s#\b($NounHeuristics::STOP)\b#@#g;
          if($ntag =~ /@ ([^@]+)$/){
            my $tg = $1;
            my @tg = split(/\s+/, $tg);
	    for($j = 1; $j <=@tg; $j++){
		$tmp = singular($tg[@tg-$j]);
		if($tmp ne "v"){ last;}
	    }
            $tag = $j <= @tg? $tmp: "unknown";
            splice(@tg, @tg-$j<=0? 0: @tg-$j);
            $modifier = join(" ", @tg);
            $sth1 = $dbh->prepare("update sentence set tag = \"".$tag."\", modifier = \"".$modifier."\" where sentid = ".$sentid);
            $sth1->execute() or die "$sth->errstr\n";
          }
        }
       }
      }#if
  }#while
}
# turn a plural word to its singular form
sub singular{
 my $p = shift;
 #$p = lc $p;
 my $singular = checkWN($p, "singular");
 return $singular if $singular =~/\w/;
 if(getnumber($p) eq "p"){
    if($p =~ /(.*?[^aeiou])ies$/){
      $p = $1.'y';
    }elsif($p =~/(.*?)i$/){
      $p = $1.'us';
    }elsif($p =~/(.*?)ia$/){
      $p = $1.'ium';
    }elsif($p =~/(.*?(x|ch|sh))es$/){
      $p = $1;
    }elsif($p =~/(.*?)ves$/){
      $p = $1."f";
    }elsif($p =~/(.*?a)e$/ || $p=~/(.*?)s$/ ){#pinnae ->pinna, fruits->fruit
      $p = $1;
    }
 }
 return $p;

}
#deal with "herbs or lianas" cases
#@todo: test/debug
sub andor{
my ($sth1, $sth2, $sentid, $sentence, $tag);

$sth1 = $dbh->prepare('select sentid, sentence from sentence where lead like "% and %" or  lead like "% or %"');
$sth1->execute();
while(($sentid, $sentence)=$sth1->fetchrow_array()){
	$tag = andortag($sentence);
	$sth2 = $dbh->prepare("update sentence set tag = \"$tag\" where sentid = ".$sentid);
    $sth2->execute();
}
}

sub andortag{
my $sentence = shift;
my ($tag, @words, $ptn);
my $token = "(and|or)";

@words = tokenize($sentence);
foreach (@words){
 if($_ =~ /\b$token\b/){
   #last if $ptn =~/X/;
   $ptn .="X";
 }else{
   last if $_ =~/\b($NounHeuristics::STOP)\b/;
   $ptn .=checkpos($_,"one");
 }
} 

print "Andor pattern $ptn for @words\n" if $debug;
if($ptn =~ /^(.*?[nsp]X.*?[nsp])/){
   #if($ptn =~ /^(.*?[ns]X.{0,2}?[nsp])/){
   print "$+[1], $-[1]\n" if $debug;
   $tag = join (" ", splice(@words, 0, ($+[1]-$-[1]))); 
}elsif($ptn =~ /^(.*?[pns]X.*?b)/){
   print "$+[1], $-[1]\n" if $debug;
   $tag = join (" ", splice(@words, 0, ($+[1]-$-[1])-1));
   $tag =~ s#\band\b$##;
   $tag =~ s#\bor\b$##;
}elsif($ptn =~ /^(.*?[pns]X\?)\?*$/){
   print "$+[1], $-[1]\n" if $debug;
   $tag = join (" ", splice(@words, 0, ($+[1]-$-[1])));
}
print "Andor determine the tag <$tag> for $sentence\n" if $debug;
$tag = "unknown" if $tag !~ /\w/;
#$tag = lc $tag;
return $tag;
}

#also output a GraphML. 
sub dumptodisk{
	my @records = split(/\s+/, $NEWDESCRIPTION);
	my $start = 0;
	my ($sth, $sentence, $tag, $end, $filename, $content, $ptag, $sentstart, $modifier);
	my ($stag, $clausecount, $taggedsent, %gmlid, $gmlid);
	$gmlid = 1;
	foreach (@records){
		if(/(.*?)\[(\d+)\]/){
			$filename = $1;
			$end = $2;
			$content = "<?xml version=\"1.0\"?><description>";
			$sentstart = 1;
			$clausecount = 0;
			for(my $i = $start; $i <= $end; $i++){
				$sth = $dbh->prepare("select originalsent, tag, modifier from sentence where sentid=".$i);
				$sth->execute();
				($sentence, $tag, $modifier) = $sth->fetchrow_array();
         		$tag =~ s#(\d)# $NUMBERS{\1} #g;
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

           if($modifier =~ /\w+/){
	              $modifier =~ s# #_#g;
    	          my $mtag = $modifier."_".$tag;
        	      $taggedsent .= "<$mtag>$sentence</$mtag>";
           }else{
    		      $taggedsent .= "<$tag>$sentence</$tag>";
           }
           $clausecount++;
        
			if($sentence =~/\.\s*$/){
				$sentstart = 1;
			}
			$gmlid = add2GraphML($gmlid, $tag, $stag, \%gmlid);
		}
		$content .= $clausecount > 1 ? "<$stag>".$taggedsent."</$stag>" : $taggedsent;
		$content .= "</description>";
		#$content =~ s#[^-()\[\]<>_!`~\#$%^&/\\.,;:0-9a-zA-Z?="'+@ ]#*#g; #replace illegal char with *
		$filename =~ s#\.[^.]*$#\.xml#;
		open(OUT, ">$markeddir$filename") || die "$!: $markeddir$filename";
		print OUT $content;
		#print "save [$content] to $markeddir$filename\n" if $debug;
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
	my $flag = 0;
	my $TAGS = "";
	do{
	   $flag = 0;
	   $TAGS = currenttags();
	   $flag += wrapupmarkup();
	   $flag += oneleadwordmarkup($TAGS);
	   $flag += doitmarkup(); 
	}while ($flag>0);
	
	defaultmarkup($TAGS);
}

sub currenttags{
	my ($id, $sent, $sth, $TAGS, $lead);
  	$sth = $dbh->prepare("select tag from sentence group by tag order by count(sentid) desc");
  	$sth->execute();
  	$TAGS = "";
  	while( my ($tag)=$sth->fetchrow_array()){
    	$TAGS .= $tag."|" if ($tag=~/\w+/);
  	}
  	chop($TAGS);
  	return $TAGS;
}

sub doitmarkup{
	my ($id, $sent, $sth, $TAGS, $lead, $tag);
	my $sign = 0;
  	$sth = $dbh->prepare("Select sentid, lead, sentence from sentence where isnull(tag)");
	$sth->execute();
	while(($id, $lead, $sent) = $sth->fetchrow_array()){
 		($tag, $sign) = doit($id);   #for cases before relevant knowledge was learned.
    	if($tag =~/\w/){
      		tag($id, $tag);
    	}
	}
	return $sign;
 }

sub oneleadwordmarkup(){
	my $TAGS = shift;
	my ($id, $sent, $sth, $lead);
	my $tags = $TAGS."|";
	my $sign = 0;
		
	$sth = $dbh->prepare("Select sentid, lead, sentence from sentence where isnull(tag) and lead not like '% %'");
	$sth->execute();
	while(($id, $lead, $sent) = $sth->fetchrow_array()){
		if($tags=~/\b$lead\|/){
			tag($id, $lead);
			$sign += update($lead, "n", "-");
		}#else{
		#	tag($id, "unknown");
		#}
	}
	return $sign;
}





#for the remaining of sentences that do not have a tag yet,
#look for lead word co-ocurrance, use the most freq. co-occured phrases as tags

#e.g. plication induplicate (n times) and plication reduplicate (m times) => plication is the tag and a noun
#e.g. stigmatic scar basal (n times) and stigmatic scar apical (m times) => stigmatic scar is the tag and scar is a noun.
#what about externally like A; externally like B?
sub wrapupmarkup{
  my ($sth, $sth1, $id,$id1, $lead, @words, $match, $flag, $ptn, $b, $ld);
  print "wrapupmarkup\n" if $debug;
  my $sign = 0;
  my $checked = "#";
  #find n-grams, n > 1
  $sth1 = $dbh->prepare("Select sentid, lead from sentence where isnull(tag)  and lead regexp \".* .*\" order by length(lead) desc" );#multiple-word leads
  $sth1->execute();
  while(($id1, $lead) = $sth1->fetchrow_array()){
	  if($checked=~/#$id1#/){next;} 
      @words = split(/\s+/, $lead);
      $words[@words-1] = "[^[:space:]]+\$";
      $match = join(" ", @words);
      $sth = $dbh->prepare("Select distinct lead from sentence where lead regexp \"^".$match."\" and isnull(tag)" );
      $sth->execute();
      if($sth->rows > 1){ # exist x y and x z, take x as the tag
          $match =~ s# \[\^\[.*$##;
          $sth = $dbh->prepare("Select sentid, lead from sentence where lead like \"".$match."%\" and isnull(tag)" );
          $sth->execute();

          @words = split(/\s+/,$match);
          $ptn = getPOSptn(@words);#get from wordpos
          my $wnpos = checkWN($words[@words-1], "pos");
          if($ptn =~ /[nsp]$/ || ($ptn =~/\?$/ && $wnpos  =~ /n/) ){
	          while(($id, $ld) = $sth->fetchrow_array()){
	          	my @all = split(/\s+/, $ld);
	          	if(@all > @words && getPOSptn($all[@words]) =~/[psn]/){ #very good apples, don't cut after good.
	          		my $nb = @all > @words+1? $all[@words+1] : ""; #may need to skip two or more words to find a noun.
	          		splice(@all, @words+1); #remove from @words+1
	          		my $nmatch = join(" ", @all);
	          		tag($id, $nmatch) ; #save the last word in $match in noun?
               		$sign += update($all[@all-1], "n", "-");
               		$sign += update($nb, "b", "") if $nb ne "";
	          	}else{
	          	    $b = @all> @words? $all[@words]: ""; 
    		   		tag($id, $match) ; #save the last word in $match in noun?
               		$sign += update($words[@words-1], "n", "-");
               		$sign += update($b, "b", "") if $b ne "";
               	}
               	$checked .= $id."#";
              }
           }else{
	           while(($id) = $sth->fetchrow_array()){
    	          #tag($id, "unknown");
    	          $checked .= $id."#";
              }
            }
      }else{
         #tag($id1, "unknown");
         $checked .= $id1."#";
      }
    }
  
  return $sign;
}



#check for sentence table for exsiting tags
#take up to the first n, if it is within 3 word range from the starting of the sentence
#else "unknown";
sub defaultmarkup{
	my $TAGS = shift;
	my ($id,$sent, $sth);
	my (@words, $tag, $word, $count, $ptn);
  	print "in default markup: $sent\n" if $debug;
  
  	$sth = $dbh->prepare("Select sentid, sentence from sentence where isnull(tag)");
	$sth->execute();
	while(($id, $sent) = $sth->fetchrow_array()){
  		assigndefaulttag($id, $sent, $TAGS);
  	}
  	 $sth = $dbh->prepare("update sentence set tag = 'unknown' where isnull(tag)");
	 $sth->execute();
}

sub assigndefaulttag{
	my ($id,$sent, $TAGS) = @_;
	my (@words, $tag, $word, $count, $ptn);
  @words = split(/[,:;.]/,$sent);
  $words[0] =~ s#\W# #g;
	$words[0] =~ s#\d+\b# NUMBER #g;
	$words[0] =~ s#\s+$##;
	$words[0] =~ s#^\s+##;
  @words = split(/\s+/, $words[0]);
	$sent = join(" ", @words);
  if($sent =~/^\s*(in|on) /i){
    return;
  }
  #if($sent =~/^\s*\w+ (with|without) /i){
   # return;
  #}
  if($sent =~/^\w*\s?\b($TAGS)\b/i){
		$tag = substr($sent, $-[0], $+[0]-$-[0]);
		print "$tag is found in TAGS \n" if $debug;
		tag($id, $tag);return;
	}

  #take the first noun/nnp as default tag
  splice(@words, 4);
  my ($tag1, $tag2);
  my $ptn = getPOSptn(@words);
  my $n1 = 5; #my $n2 = 5;
  if($ptn =~ /^(.*?[psn]).*/){
    $n1 = length($1);
    splice(@words, $n1);
    $tag1 = join(" ", @words);
	#taking up to the first stop word is not a good idea for fossil_2000, e.g small to medium size; => <small>
    #my $t = join(" ", @words);
    #if ($t =~ /(.*?)\b($NounHeuristics::STOP)\b/){
    #  my @t = split(/ /, $1);
    #  $n2 = @t;
    #  splice(@words, $n2);
    #  $tag2 =  join(" ", @words);
    #}
    #my $TAGSTRING = $TAGS;
    #$TAGSTRING =~ s#[)|(]# #g;
    #if($tag1 !~/\w/ and $tag2 =~/\w/){
    # $tag = $tag2;
    #}elsif($tag2 !~/\w/ and $tag1 =~/\w/){
    #  $tag = $tag1;
    #}else{
    #  if($TAGSTRING =~ /\b$tag1\b/i and $TAGSTRING !~ /\b$tag2\b/i){
    #     $tag = $tag1;
    #  }elsif($TAGSTRING =~ /\b$tag2\b/i and $TAGSTRING !~ /\b$tag1\b/i){
    #     $tag = $tag2;
    #  }else{
    #     $tag = $n1 < $n2 ? $tag1 : $tag2;
    #  }
    #}
    $tag = $tag1;
    print " up to first [psn]. <$tag> is used\n" if $debug;
    tag($id,$tag);
    return;
  }elsif($ptn =~ /^(\??)(\??)b/){   #check WNPOS for pl.
    my $i1 = $-[1];
    my $i2 = $-[2];
    if(getnumber($words[$i2]) eq "p"){
       tag($id, $words[$i2-1]." ".$words[$i2]);
       print " pos determined tag <$words[$i2-1] $words[$i2]>" if $debug;
    }elsif(getnumber($words[$i1]) eq "p"){
       tag($id, $words[$i1]);
       print " pos determined tag <$words[$i1]>" if $debug;
    }
  }

  splice(@words, 3);     #keep first three words
  if($ptn =~/^(b\?)b/ and $words[2] eq "of"){#save ? as a noun?
    update($words[1], "n", "-");
    splice(@words, 2);
    $tag = join(" ", @words);
    tag($id, $tag);   #first two words for tag
    return;
  }
  #check for the first pl
  $count = 0;
  foreach $word (@words){
     if($count++ > 4) {last;}
     my $p = getnumber($word);
     if($word !~/\b($NounHeuristics::STOP)\b/ ||  $p ne "p"){
       $tag .= $word." ";
     }elsif ($p eq "p"){
       $tag .= $word;
	     print "a likely [p] $tag is used\n" if $debug;
       tag($id, $tag);
       return;
     }
   }
   return;
}


########return a positive number if any new discovery is made in this iteration
########use rule based learning first for easy cases
########use instance based learning for the remining unsolved cases
sub discover{
	my $status = shift;
  #$status .= "|start" if $status eq "normal";
	my ($sid, $sentence, @startwords, $pattern, @matched, $round, $sth, $new, $lead, $tag, $newdisc);
	$sth = $dbh->prepare("select sentid,sentence,lead,tag from sentence where status regexp \"($status)\"");
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
          $newdisc += $new;
				  print "##end round $round. made $newdisc discoveries\n" if $debug;
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
 return $newdisc;
}

#sentences that match the pattern
sub matchpattern{
my($pattern, $status, $hastag) = @_;
my ($stmt, $sth, @matchedids, $sentid, $sentence);
if($hastag == 1){
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
	$sth=$dbh->prepare("select * from sentence where sentid=".$id." and !isnull(tag)");
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
		$sth = $dbh->prepare("select sentence, lead, tag from sentence where sentid=".$sentid);
		$sth->execute();
		($sent, $lead, $tag) = $sth->fetchrow_array();
		if(!ismarked($sentid)){
			print "For unmarked sentence: $sent\n" if $debug;
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
        			$sth = $dbh->prepare("select lead from sentence where sentid=$sid");
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
						    print "$pos: $_\n" if $debug;
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
 $sth = $dbh->prepare("select sentence from sentence where sentid=$_");
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
		@posinfo = checkposinfo($word,"one");
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
	$sth = $dbh->prepare("select sentence, lead from sentence where sentid=$sentid");
	$sth->execute();
	($sentence, $lead) = $sth->fetchrow_array();
	
	if($lead =~/\b(and|or)\b/){
		return ($tag,$sign);
	}

	@ws = split(/\s+/,$lead);
	$ptn = getPOSptn(@ws);
	print "\nsentence: $sentence\n" if $debugp;

	if($ptn =~ /^[pns]$/){
    #single-word cases, e.g. herbs, ...
    $tag = $ws[0];
		$sign += update($tag, $ptn,"-");
		print "Directly markup with tag: $tag\n" if $debugp;
	}elsif($ptn =~ /ps/){#questionable 
	    #@todo test: stems multiple /ps/, stems 66/66, multiple 1/1
		print "Found [ps] pattern\n" if $debugp;
		my $i = $+[0];    #end of the matching
		my $s = $ws[$i-1];
		$i = $-[0]; #start of the matching
		my $p = $ws[$i];
		my @p = checkposinfo($p, "one");
		my @s = checkposinfo($s, "one");
		my ($pcertainty, $pcertaintyu, $pcertaintyl,$scertainty, $scertaintyu, $scertaintyl);
		($pos, $role, $pcertainty) = ($p[0][0], $p[0][1], $p[0][2]);
		($pos, $role, $scertainty) = ($s[0][0], $s[0][1], $s[0][2]);
    ($pcertaintyu, $pcertaintyl) = split(/\//, $pcertainty);
    ($scertaintyu, $scertaintyl) = split(/\//, $scertainty);
    $pcertainty = $pcertaintyu/$pcertaintyl;
    $scertainty = $scertaintyu/$scertaintyl;
		if($pcertainty >= $scertainty && $pcertaintyl >= $scertaintyl*2){
		   discount($s, "s");
       print "discount $s pos of s\n" if $debugp;
		   @tws = splice(@ws, 0, $i+1);   #up to the "p" inclusive
		   $tag = join(" ",@tws);
		   print "\t:determine the tag: $tag\n" if $debugp;
		   $sign += update($p, "p", "-");
		   $sign += updatenn(0, $#tws+1, @tws); #up to the "p" inclusive
		   if($scertainty * $scertaintyl < 2){
		     $sign += update($s, "b","");
		   }
		}elsif($pcertainty < $scertainty && $pcertaintyl < $scertaintyl*2){
		   discount($p, "p");
       print "discount $p pos of p\n" if $debugp;
		   $sign += update($s, "s", "");
       #@todo: determine the tag?
		}		
   }elsif($ptn =~/p(\?)/){#case 3,7,8,9,12,21: p? => ?->%BDRY
	  #use up to the pl as the tag
		#if ? is not "p"->%BDRY
		#save any NN in %NOUNS, note the role of Ns
		$i = $-[1];#index of ?
		#what to do with "flowers sepals" when sepals is ?
		print "Found [p?] pattern\n" if $debugp;
		if(getnumber($ws[$i]) eq "p"){    # pp pattern
		   #$tag = $ws[$i-1];
       $tag = $ws[$i];
		   $sign += update($tag, "p","-");
       my $sth = $dbh->prepare("insert into isA (instance, class) values (\"".$tag."\",\"".$ws[$i-1]."\")");
       $sth->execute();
       print "\t:[p p] pattern: determine the tag: $tag\n" if $debugp;
		}else{
		    @cws = @ws;
    		@tws = splice(@ws,0,$i);#get tag words, @ws changes as well
    		$tag = join(" ",@tws);
    		print "\t:determine the tag: $tag\n" if $debugp;
    		print "\t:updates on POSs\n" if $debugp;
    		$sign += update($cws[$i], "b", "");
			  $sign += update($cws[$i-1],"p", "-");
			  $sign += updatenn(0,$#tws+1, @tws);
		}
	}elsif($ptn =~ /[psn](b)/){#case 2,4,5,11,6,20: nb => collect the tag
		#use up to the N before B as the tag
		#save NN in %NOUNS, note the role of Ns
		#anything may be learned from rule 20?
		print "Found [[psn](b)] pattern\n" if $debugp;
		$i = $-[1]; #index of b
		$sign += update($ws[$i], "b","");
		@cws = @ws;
		@tws = splice(@ws,0,$i);#get tag words, @ws changes.
		$tag = join(" ",@tws);
		$sign += update($cws[$i-1], substr($ptn, $i-1, 1), "-");
		$sign += updatenn(0, $#tws+1,@tws);
		print "\t:determine the tag: $tag\n" if $debugp;
		print "\t:updates on POSs\n" if $debugp;
	}elsif($ptn =~ /([psn][psn]+)/){#case 1,3,10,19: nn is phrase or n2 is a main noun
		#if nn is phrase or n2 is a main noun
		#make nn the tag
		#the word after nn ->%BDRY
		#save NN in %NOUNS, note the role of Ns
		print "Found [[psn][psn]+] pattern\n" if $debugp;
		$start = $-[1];
		$end = $+[1]; #the last of the known noun
		@cws = @ws;
    #if contain pp, take the last p as the tag
    #otherwise, take the whole pattern
    if($ptn =~ /pp/){
       my $i = rindex($ptn, "p");
       $tag = $ws[$i];   #may be reset later
       @t = checkposinfo($tag, "one");#last p is the tag
       $end = $i+1;
       #my $q =
       my $sth = $dbh->prepare("insert into isA (instance, class) values (\"".$tag."\",\"".$ws[$i-1]."\")");
       $sth->execute();
    }else{
       @tws = splice(@ws,$start, $end-$start);#get tag words
       $tag = join(" ",@tws);      #may be reset later
       @t = checkposinfo($tws[@tws-1], "one");
    }
    ($pos, $role, $certainty) = ($t[0][0], $t[0][1], $t[0][2]);  #last n
    if($pos=~/[psn]/ && $role =~ /[-+]/){
    		#the word after nn ->%BDRY
    		@t = split(/\s+/,$sentence);
    		$sign += update($t[$end],"b",""); #@todo:test
			  $sign += update($cws[$start], substr($ptn, $start, 1), "") if $start < $end;
			  $sign += updatenn(0, $#tws+1, @tws);
    	  $sign += update($cws[$start+1], substr($ptn, $start+1, 1), "") if $start+1 < $end;
    }else{
    		print "\t:$tag not main role, reset tag to null\n" if $debugp;
		    $tag = "";
  	}
		#if($ptn =~/pp/){
    #    $tag = $ws[$start];
		#	  @t = checkposinfo($tag, "one");#first p is the tag
		#}else{
    #		@tws = splice(@ws,$start, $end-$start);#get tag words
    #		$tag = join(" ",@tws);
		#	  @t = checkposinfo($tws[@tws-1], "one");
		#}
 	  #($pos, $role, $certainty) = ($t[0][0], $t[0][1], $t[0][2]);  #last n
    #if($pos=~/[psn]/ && $role =~ /[-+]/){   #pp got processed again!?
    		#the word after nn ->%BDRY
    #		@t = split(/\s+/,$sentence);
    #		$sign += update($t[$end],"b",""); #@todo:test
		#	  $sign += update($cws[$start], substr($ptn, $start, 1), "");
		#	  $sign += updatenn(0, $#tws+1, @tws);
    #	  $sign += update($cws[$start+1], substr($ptn, $start+1, 1), "");
    #}else{
    #		print "\t:$tag not main role, reset tag to null\n" if $debug;
		#	  $tag = "";
  	#}
   	print "\t:determine the tag: $tag\n" if $debugp;
	}elsif($ptn =~ /b\?([psn])$/ or $ptn=~/\?b([psn])$/){#case 16, 25
		#if n can be a main noun
		#make n the tag
		#the word after n ->%BDRY
		print "Found [b?[psn]] or  [?b[psn]] pattern\n" if $debugp;
		$end = $-[1];#index of n
		@cws = @ws;
		@tws = splice(@ws, 0, $end+1);
		$tag = join(" ",@tws);
		@t = checkposinfo($ws[$end],"one");#n's posinfo
		($pos, $role, $certainty) = ($t[0][1], $t[0][1], $t[0][2]);
		if($role =~ /[-+]/){
			#the word after n ->%BDRY
      @t = tokenize($sentence);
			print "\t:updates on POSs\n" if $debugp;
			$sign += update($t[$end+1],"b",""); #test
			#$sign += update($cws[$end-2], "b", "");
			$sign += update($cws[$end], substr($ptn, $end, 1), $role);
		}else{
			print "\t:$tag not main role, reset tag to null\n" if $debugp;
			$tag = "";
		}
		print "\t:determine the tag: $tag\n" if $debugp;
	}elsif($ptn =~ /^s(\?)$/){
	    #? =>b
		#@todo:test, need test
		$i = $-[1];#index of ?
		print "Found [^s?\$] pattern\n" if $debugp;
		my $wnp = checkWN($ws[$i], "pos");
		if($wnp =~/p/){ #"hinge teeth,"
			$tag = $ws[$i-1]." ".$ws[$i];
			print "\t:determine the tag: $tag\n" if $debugp;
			print "\t:updates on POSs\n" if $debugp;
			my $n = checkWN($ws[$i], "number");
			$sign += update($ws[$i], $n, "-");			
		}else{
			$tag = $ws[$i-1];
			print "\t:determine the tag: $tag\n" if $debugp;
			print "\t:updates on POSs\n" if $debugp;
			$sign += update($ws[$i], "b", "");
			$sign += update($ws[$i-1], "s", "-");
		}
	}elsif($ptn =~ /^bs$/){
	    $tag = join(" ", @ws);
		$sign += update($ws[0], "b", "");
		$sign += update($ws[1], "s", "-");
	} elsif($ptn =~ /^\?(b)/){#case 8,17,22,23,24,26: ?b => ?->%NOUNS
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
		print "Found [?(b)] pattern\n" if $debugp;
		$i = $-[1];#index of (b)
		$sign += update($ws[$i], "b", "");
    	@cws = @ws;
		@tws = splice(@ws,0,$i);#get tag words
		$tag = join(" ",@tws);
    	my $word = $cws[$i-1]; #the "?" word;
    	my $wnp = checkWN($word, "pos");
    	my ($maincount, $modicount) = getroles($word);
    	print "main role = $maincount; modifier role = $modicount\n" if $debugp;
    	if(($wnp eq "" || $wnp =~ /[psn]/) && $maincount >= $modicount){#tag is not an adv or adj such as abaxially or inner
		  print "\t:determine the tag: $tag\n" if $debugp;
		  print "\t:updates on POSs\n" if $debugp;
		  $sign += update($cws[$i-1], "n", "-");
		  $sign += updatenn(0,$#tws,@tws);
    	}else{
		  print "\t:$tag is adv/adj or modifier. skip.\n" if $debugp;
      	  $tag = "";
    	}
	}else{
		print "Pattern [$ptn] is not processed\n" if $debugp;
	}
	return ($tag,$sign);
}

sub getroles{
  my $word = shift;
  my ($sth, $maincount, $modicount);
  $sth = $dbh->prepare("select certaintyu from wordpos where word = \"$word\" and role = \"-\"");
	$sth->execute();
  $maincount = $sth->fetchrow_array();
  $sth = $dbh->prepare("select count from modifiers where word = \"$word\"");
	$sth->execute();
  $modicount = $sth->fetchrow_array();
  return ($maincount, $modicount);
}
sub tag{
	#my ($sentence, $tag) = @_;
	my($sid, $tag) = @_;
	my $sth;
	if($tag !~ /\w+/){return;}
	#$tag = lc $tag;
	if($tag =~ /\w+/){
	   $sth = $dbh->prepare("update sentence set tag =\"$tag\" where sentid =$sid");
	   $sth->execute();
	   print "\t:mark up ".$sid." with tag $tag\n" if $debug;
	}
}


#####discount 1 from certainty for word's "-" role
sub discount{
my($word, $pos) =@_;
my ($sth1, $cu, $cl);
#$word = lc $word;
$sth1 = $dbh->prepare("select certaintyu, certaintyl from wordpos where word=\"$word\" and pos=\"$pos\"");
$sth1->execute();
($cu, $cl) = $sth1->fetchrow_array();
if(--$cu == 0){
#remove this record
$sth1 = $dbh->prepare("delete from wordpos where word=\"$word\" and pos=\"$pos\" ");
$sth1->execute();
}else{
#update
$sth1 = $dbh->prepare("update wordpos set certaintyu=$cu where word=\"$word\" and pos=\"$pos\" ");
$sth1->execute();
}

}
#####for the nouns in @words, make the last n the main noun("-").
#####update NN's roles
#####return a positive number if an update is made,
sub updatenn{
	my($start,$end, @words) = @_;
	my ($update, $i, $sth1, $count);
	@words = splice(@words, $start, $end-$start);#all Ns
	for($i = 0; $i < @words-1; $i++){#one N at a time
		#$update += update($words[$i],"n","_");#modifier
    $sth1 = $dbh->prepare("select count from modifiers where word=\"$words[$i]\"");
    $sth1->execute();
    $count = $sth1->fetchrow_array();
    if($count < 1){
       $sth1 = $dbh->prepare("insert into modifiers (word, count) values (\"$words[$i]\", 1)");
       $sth1->execute();
       $update = 1;
    }else{
       $count += 1;
       $sth1 = $dbh->prepare("update modifiers set count = $count where word=\"$words[$i]\"");
       $sth1->execute();
    }
	}
	#$update += update($words[$i], "n", "-"); #last one is the main noun
	#$update += update(join(" ",@words), "n","") if @words >=2;#NN altogether
	return $update;
}

####find the numerical value of a certainty
sub tovalue{
	my $certainty = shift;
	return -1 if (index($certainty, "/") != rindex($certainty,"/"));
	my ($u,$l) = split(/\//,$certainty);
	if ($l == 0){
	  print $u."/".$l if $debug ;
	 }
	return $u/$l;
}
########update %NOUNS and %BDRY; handle all updates, if a "p", also add its "s"
########save any NN in %NOUNS, note the role of Ns
########"1/1_" modifier:"_", main noun:"-", both: "+";
#####return 1 if an update is made, otherwise 0;
sub update{
	my ($word, $pos, $role, $new) = @_; #("Base", "n", "_") or ("Base", "b", ""),
	#$word = lc $word;
	$word =~ s#\s+$##;
	$word =~ s#^\s*##;
	if($word !~ /\w/ || $word =~/\b(?:$forbidden)\b/){return;}

	if($pos eq "n"){
		$pos = getnumber($word);
	}
  #updatePOS($word, $pos, $role);
  $new += updatePOS($word, $pos, $role);
  if($pos eq "p"){
    $word = singular($word);
    $new += updatePOS($word, "s", "");
  }
  if($pos eq "s"){
    my @words = plural($word);
    foreach my $w (@words){
      $new += updatePOS($w, "p", "") if $w =~/\w/;
    }
  }
  
  return $new;
}

sub updatePOS{
   my ($word, $pos, $role) = @_;
	 my ($sth1, $sth, $new, $oldrole, $certaintyu, $certaintyl);

  #updates should be in one transaction
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
	    #seens pos, update role and certaintyu
	    ($oldrole, $certaintyu) = $sth1->fetchrow_array();
		$role = mergerole($oldrole, $role);
		$certaintyu++;
		$sth = $dbh->prepare("update wordpos set role =\"$role\", certaintyu =$certaintyu where word=\"$word\" and pos=\"$pos\"");
	    $sth->execute();
		print "\t: update [$word($pos)] role: $oldrole=>$role, certaintyu=$certaintyu\n" if $debug;
	}
	#update certaintyl = sum (certaintyu)
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
  #$word = lc $word;
  my $number = checkWN($word, "number");
  return $number if $number =~/\w/;
  if ($word =~ /ss$/){return "s";}
  if($word =~/ia$/) {return "p";}
  if ($word =~/ae$/){return "p";}
  if($word =~/ous$/){return ""; }
  if ($word =~/us$/){return "s";}
  if($word =~/^[aiu]s$/){return ""; }
  if($word =~ /es$/ || $word =~ /s$/){return "p";}
  return "s";
}

#####return pos string
sub checkpos{
my ($word,$mode) = @_;#$mode = "one","all"
my @posinfo = checkposinfo($word, $mode);
if($mode eq "one"){
	return $posinfo[0][0];
}else{
  print "wrong mode in checkpos\n";
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


########return a 2d array of (pos,role, certainty/certaintyl)
sub checkposinfo{
	my ($word,$mode) = @_;#$mode = "one","top", "all"
	#"one":the top one with top certainty, "all":all
	my ($pos, $role, $certaintyu, $certaintyl, $certainty, $maxcertainty);
	my ($sth, @results, $count, $stmt);
	#$word = lc $word;
	$word =~ s#\s+$##;
	$word =~ s#^\s*##;
	#select pos from wordpos
	$stmt = "select pos, role, certaintyu, certaintyl from wordpos where word=\"$word\" order by certaintyu/certaintyl desc";
	$sth = $dbh->prepare($stmt);
	$sth->execute();
	if($sth->rows ==0){
	    my @temp = ("?", "?", "?");
	    $results[0] = \@temp;
	    return @results;
	}
 	$count = 0;
  #on sorted certainty ratio
  while(($pos, $role, $certaintyu, $certaintyl)=$sth->fetchrow_array()){
			my @temp = ();
			push(@temp, $pos, $role, $certaintyu."/".$certaintyl);
			$results[$count++] = \@temp;
	    return @results if $mode eq "one";
  }
	return @results; #if "all"
}

### find the one with the greatest $certaintyl (since the value of certainty is the same)
#sub chooseone{
#my @results = @_;
#my ($index, $max, $pos, $role, $certaintyu, $certaintyl, @result, $i);
#$max = 0;
#for($i = 0; $i < @results; $i++){
#  ($pos, $role, $certaintyu, $certaintyl) = ($results[$i][0],$results[$i][1],$results[$i][2],$results[$i][3]) ;
#  $max = $certaintyl if $max < $certaintyl;
#  $index = $i if $max < $certaintyl;
#}
#
#$result[0] = $results[$index];
#return @result;
#}

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
	$sth = $dbh->prepare("select lead from sentence where sentid=$sentid");
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

######check wordnet to gether information about a word
######save checked words in hashtables
######
sub checkWN{
  my ($word, $mode) = @_;
  #$word = lc $word;
  #check saved records
  my $singular = $WNSINGULAR{$word} if $mode eq "singular";
  return $singular if $singular =~ /\w/;
  my $number = $WNNUMBER{$word} if $mode eq "number";
  return $number if $number =~ /\w/;
  my $pos = $WNPOS{$word} if $mode eq "pos";
  return $pos if $pos =~/\w/;
  
  #special cases
  if ($word eq "teeth"){
    $WNNUMBER{"teeth"} = "p";
    $WNSINGULAR{"teeth"} = "tooth";
    return $mode eq "singular"? "tooth" : "p";
  }

  if ($word eq "tooth"){
    $WNNUMBER{"tooth"} = "s";
    $WNSINGULAR{"tooth"} = "tooth";
    return $mode eq "singular"? "tooth" : "p";
  }
  
  if ($word eq "NUMBER")
  {
    return $mode eq "singular"? "NUMBER" : "s";
  }
  
  if ($word =~ /[a-z]{3,}ly$/){#concentrically
    return "" if $mode eq "singular";
    return "" if $mode eq "number";
    return "r" if $mode eq "pos";
  }

  #otherwise, call wn
  my $result = `wn $word -over`;
  return "" if $result !~/\w/; #word not in WN
  $result =~ s#\n# #g;
  if($mode eq "singular" || $mode eq "number"){
    my $t = "";
    while($result =~/Overview of noun (\w+) (.*) /){
         $t .= $1." ";
         $result = $2;
    }
    if ($t !~ /\w/){return "v";} #not a noun
    $t =~ s#\s+$##;
    my @ts = split(/\s+/, $t);
    ###select the singular between roots and root.   bases => basis and base?
    if(@ts > 1){
      my $l = 100;
      print "Word $word has singular\n" if $debug;
      foreach (@ts){
       print "$_\n" if $debug;
       # -um => a, -us => i?
       if (length $_ < $l){
          $t = $_;
          $l = length $_;
       }
      }
      print "The singular is $t\n" if $debug;
    }
    if ($t ne $word){
       $WNSINGULAR{$word} = $t;
       $WNNUMBER{$word} = "p";
       return $mode eq "singular"? $t : "p";
    }else{
       $WNSINGULAR{$word} = $t;
       $WNNUMBER{$word} = "s";
       return $mode eq "singular"? $t : "s";
    }
 }elsif($mode eq "pos"){
   my $pos = "";
   while($result =~/.*?Overview of ([a-z]*) (.*)/){
         my $t = $1;
         $result = $2;
         $pos .= "n" if $t eq "noun";
         $pos .= "v" if $t eq "verb";
         $pos .= "a" if $t eq "adj";
         $pos .= "r" if $t eq "adv";

    }
    $WNPOS{$word} = $pos;
    print "Wordnet Pos for $word is $pos\n" if $debug;
    return $pos;
  }
 }
 
 ##turn a singular word to its plural form
 ##check to make sure the plural form appeared in the text.
sub plural{
 my $word = shift;
 return "" if $word =~/^(n|2n|x)$/;
 my $plural = $PLURALS{$word};
 if ($plural=~/\w+/){
    my @pls = split(/ /, $plural);
    return @pls;
 }
 
 #special cases
 if($word =~ /series$/){
  $plural = $word;
 }elsif($word =~ /(.*?)foot$/){
  $plural = $1."feet";
 }elsif($word =~ /(.*?)tooth$/){
  $plural = $1."teeth";
 }elsif($word =~ /(.*?)alga$/){
  $plural = $1."algae";
 }elsif($word =~ /(.*?)genus$/){
  $plural = $1."genera";
 }elsif($word =~ /(.*?)corpus$/){
  $plural = $1."corpora";
 }else{
    #rules
    if($word =~ /(.*?)(ex|ix)$/){
      $plural = $1."ices"; #apex=>apices
      $plural .= " ".$1.$2."es";
    }elsif($word =~ /(x|ch|ss|sh)$/){
      $plural = $word."es";
    }elsif($word =~ /(.*?)([^aeiouy])y$/){
      $plural = $1.$2."ies";
    }elsif($word =~ /(.*?)(?:([^f])feï¿½([oaelr])f)$/){
      $plural = $1.$2.$3."ves";
    }elsif($word =~ /(.*?)(x|s)is$/){
      $plural = $1.$2."es";
    }elsif($word =~ /(.*?)([tidlv])um$/){
      $plural = $1.$2."a";
    }elsif($word =~ /(.*?)(ex|ix)$/){
      $plural = $1."ices"; #apex=>apices
    }elsif($word =~ /(.*?[^t][^i])on$/){
      $plural = $1."a"; #taxon => taxa but not venation =>venatia
    }elsif($word =~/(.*?)a$/){
      $plural = $1."ae";
    }elsif($word =~ /(.*?)man$/){
      $plural = $1."men";
    }elsif($word =~ /(.*?)child$/){
      $plural = $1."children";
    }elsif($word =~ /(.*)status$/){
      $plural = $1."statuses";
    }elsif($word =~ /(.+?)us$/){
      $plural = $1."i";
      $plural .= " ".$1."uses";
    }elsif($word =~/s$/){
      $plural = $word."es";
    }
    $plural .= " ".$word."s"; #another choice
 }
  print "$word plural is $plural\n" if $debug;
  $plural =~ s#^\s+##;
  $plural =~ s#\s+$##;
  my @pls = split(/ /, $plural);
  my $plstring = "";
  foreach my $p (@pls){
    if ($WORDS{$p} >= 1){
      $plstring .=$p." ";
    }
  }
  $plstring =~ s#\s+$##;
  $PLURALS{$word} = $plstring;
  print "confirmed $word plural is *$plstring*\n" if $plstring=~/\w/ && $debug;
  @pls = split(/ /, $plstring);
  return @pls;

}

########read $dir, mark sentences with $SENTMARKER,
########put space around puncts, save sentences in %SENTS,
########"sentences" here include . or ; ending text blocks.
sub populatesents{
my ($file, $text, @sentences,@words,@tmp,$status,$lead,$stmt,$sth, $escaped, $original, $count);
opendir(IN, "$dir") || die "$!: $dir\n";
while(defined ($file=readdir(IN))){
	if($file !~ /\w/){next;}
	print "read READ $file\n" if $debug;
	$text = ReadFile::readfile("$dir$file");
	#print $text."\n";
	$original = $text;
  	$text =~ s/&[;#\w\d]+;/ /g; #remove HTML entities
  	$text =~ s#\([^()]*?[a-zA-Z][^()]*?\)# #g;  #remove (.a.)
  	$text =~ s#\[[^\]\[]*?[a-zA-Z][^\]\[]*?\]# #g;  #remove [.a.]
  	$text =~ s#{[^{}]*?[a-zA-Z][^{}]*?}# #g; #remove {.a.}
  	$text =~ s#_#-#g;   #_ to -
  	$text =~ s#\s+([:;\.])#\1#g;     #absent ; => absent;
  	$text =~ s#(\w)([:;\.])(\w)#\1\2 \3#g; #absent;blade => absent; blade
  	$text =~ s#(\d\s*\.)\s+(\d)#\1\2#g; #1 . 5 => 1.5
  	$text =~ s#(\sdiam)\s+(\.)#\1\2#g; #diam . =>diam.
  	$text =~ s#(\sca)\s+(\.)#\1\2#g;  #ca . =>ca.
	#@todo: use [PERIOD] replace . etc. in brackets. Replace back when dump to disk.
	@sentences = SentenceSpliter::get_sentences($text);#@todo: avoid splits in brackets. how?
 	foreach (@sentences){
		#may have fewer than $N words
		if(!/\w+/){next;}
    	s#([^\d])\s*-\s*([^\d])#\1_\2#g;          #2 - 5  => 2_5
		s#(\W)# \1 #g;                            #add space around nonword char 
    	#s#& (\w{1,5}) ;#&\1;#g;          
    	s#\s+# #g;                                #multiple spaces => 1 space 
    	s#^\s*##;                                 #trim
    	s#\s*$##;                                 #trim 
    	tr/A-Z/a-z/;                              #all to lower case
    	getallwords($_);
  	}	

	$count = 0;
 	foreach (@sentences){
		#may have fewer than $N words
		if(!/\w+/){next;}
		my $line = $_;
		my $oline = getOriginal($line, $original, $file);
    
    	@words = getfirstnwords($line, $N); # "w1 w2 w3"
    
    	$status = "";
		if(getnumber($words[0]) eq "p"){
		     $status = "start";
		}else{
		     $status = "normal";
		}
		$lead = "@words";
		$lead =~ s#\s+$##;
		$lead =~ s#^\s*##;
		$lead =~ s#\s+# #g;
    	s#"#\\"#g;
    	s#'#\\'#g;
    	s#\(#\\(#g;
    	s#\)#\\)#g;
    	my $source = $file."-".$count++;
		$stmt = "insert into sentence(sentid, source, sentence, originalsent, lead, status) values($SENTID,\"$source\" ,\"$line\",\"$oline\",\"$lead\", \"$status\")";
		$sth = $dbh->prepare($stmt);
    	$sth->execute() or die $sth->errstr."\n SQL Statement: ".$stmt."\n";
		#print "Sentence: $line\n" if $debug;
		#print "Leading words: @words\n\n" if $debug;
		$SENTID++;
	}
	my $end = $SENTID-1;
	$NEWDESCRIPTION.=$file."[".$end."] ";
	my $query = $dbh->prepare("insert into sentInFile values ('$file', $end)");
	$query->execute() or warn $query->errstr."\n";
}
print "Total sentence=$SENTID" if $debug;
#TODO: sort and print %WORDS
}

sub getallwords{
  my $sentence = shift;
	my @words = tokenize($sentence);
  foreach my $w (@words){
    $WORDS{$w}++;
  }
}

sub getfirstnwords{
	########return the first up to $n words of $sentence as an array, excluding
	my($sentence, $n) = @_;
	my (@words, $index, $w);
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
	$sentence =~ s#\d+\b# NUMBER #g;
	$sentence =~ s#\s+$##;
	$sentence =~ s#^\s+##;
   	$sentence =~ s#_ # #g;
	@words = split(/\s+/, $sentence);
  	return @words;
}


#extract the segment matching $line from $original, mainly to get original case and parentheses
#$line: pappi , 20 ï¿½ 40 mm , usually noticeably shorter than corolla .
#$orginal:... Pappi (white or tawny), 20ï¿½40mm, usually noticeably shorter than corolla. ...
#Pollen 70–100% 3-porate, mean 25 µm
sub getOriginal{
	my ($line, $original, $file) = @_;
	my $pattern1 = $line; 

	if($line=~/[)(]/ && $line !~/\(.*?\)/){
		print "====>Unmatched paranthesis in $file: $line\n\n";
	}	

	$pattern1 =~ s#([)(\[\]}{.+?*])#\\\1#g; #escape )([]{}.+*?
		
	$pattern1 =~ s#([-_])#\\s*[-_]\\s*#g; #deal with _-
	
	$pattern1 =~s#[^-()\[\]<>_!`~\#$%^&/\\.,*;:0-9a-zA-Z?="'+@ ]#.#g; #replace non-word non-punc char with a .
	
	$pattern1 =~ s#\s+#\\s*([\\(\\[\\{].*?[\\}\\]\\)])?\\s*#g;         #all spaces => \s*(\(.*?\))?\s*

	if($original =~ /($pattern1)/i){
		my $oline = $1;
		return $oline;
	}else{
		print "\nline ====> $line\n\n";
		print "orginal ====> $original\n";
		print "pattern ====> $pattern1\n";
		die "the above doesn't match in $file\n\n";
	}
}
