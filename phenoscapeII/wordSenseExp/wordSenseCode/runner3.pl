#! /usr/bin/perl -w 

# Include external packages
use strict;
use DBI;
use DBD::mysql;
use WordNet::QueryData 1.40;
use Getopt::Long;
use WordNet::Similarity;
use List::Util qw(sum);
use List::Util qw[min max];


my $filename;
my $fstword;
my $sndword;
my $mode;

my $meth = $ARGV[2];#measure method, value could be vector or lesk
my $MAXS=5;
my $MAX=5;

my $verbose=$ARGV[0];
my $compmode=$ARGV[1];
#my $posmode=$ARGV[2];

if($#ARGV == 3){
	print "verbose=[$verbose]\n";
	print "comparison mode=[$compmode]\n";
#	print "POS mode=[$posmode]\n";
	$filename = $ARGV[3];
	$mode=1;
}elsif($#ARGV == 4){
	print "verbose=[$verbose]\n";
	print "comparison mode=[$compmode]\n";
#	print "POS mode=[$posmode]\n";
	$fstword = $ARGV[3];
	$sndword = $ARGV[4];
	$mode=2;
}else{
	#printusage();
	#exit;
}

# Load WordNet::QueryData
my $wn = WordNet::QueryData->new;
die "Unable to create WordNet object.\n" if(!$wn);

my $opt_type="WordNet::Similarity::vector";

if($meth eq "vector"){
	#use vector measure
	$opt_type = "WordNet::Similarity::vector";
}elsif($meth eq "lesk"){
	#use lesk mearsure
	$opt_type = "WordNet::Similarity::lesk"; 
}else{
	printusage();
	exit 1;
}


# Load the WordNet::Similarity module.
my $type = $opt_type;
$opt_type =~ s/::/\//g;
$opt_type .= ".pm";
require $opt_type;
my $measure = undef;
$measure = $type->new($wn);

# If object not created.
if(!$measure)
{
  print STDERR "Unable to create WordNet::Similarity object.\n";
  exit 1;
}

# If serious error... stop.
my ($error, $errorString) = $measure->getError();
if($error > 1)
{
  print STDERR $errorString."\n";
  exit 1;
}

# CONFIG VARIABLES
my $database = "fnaglossaryfix";
my $database2 = "phenoscape";
my $host = "localhost";
my $user = "root";
my $pw = "";

my $dbh = DBI->connect("DBI:mysql::$host", $user, $pw)
or die DBI->errstr."\n";

#open(INPUT,"<$filename");#open the input file
#my(@lines) = <INPUT>;#read the entrie file into list

my $mainword="twisted";

#foreach $mainword (@lines){
#chomp($mainword);#strip newline from the line
print"===============================================\n";
print"The word to be mapped:  $mainword\n";
print"===============================================\n";
#siblings of the target word
#my %categories;

#my $stmtc="select category from ".$database.".fnaglossaryfixed2 where term = '".$mainword."'";

#my $sthc = $dbh->prepare($stmtc);
#$sthc->execute() or die $sthc->errstr."\n";

my %closestSib;
my %patocousins;
my %patocousinsa;
my %patocousinsm;
#while(my $cate = $sthc->fetchrow_array()){
#	$cate =~ tr/[A-Z]/[a-z]/;#covert the word to lower case
	
#	print "-----------------------------------\n";
#	print "Category: $cate\n";
#	print "-----------------------------------\n";

	#find $MAXS closest siblings
#	my $stmt1 = "select term from ".$database.".fnaglossaryfixed2 where lcase(category) ='".$cate."' AND lcase(term) <> '".$mainword."'";#lowercase category and the term!
	
#	my $sth1 = $dbh->prepare($stmt1);
#	$sth1->execute() or die $sth1->errstr."\n";
	my @synonyms = ("awry", "bent", "braided", "contorted", "curled", "gnarled", "tangled", "twined", "twisting", "wrenched","awry", "bent", "braided", "coiled", "complicated", "contorted", "convoluted", "crooked", "curled", "gnarled", "intricate", "involved", "knurly", "perverted", "tangled", "tortile", "tortuous", "wound", "wry", "crumpled", "crushed", "distorted", "warped", "buckled", "deformed", "misshapen", "evil", "corrupt", "corrupted", "distorted", "abnormal", "warped", "unhealthy", "degenerate", "deviant", "wicked", "sadistic", "depraved", "debased", "debauched", "aberrant", "pervy", "perverted", "sick");
	my %siblings = ();

	#while(my $word1 = $sth1->fetchrow_array()){
	foreach my $word1 (@synonyms){
		if($word1 =~ /^_.*/|| $word1 =~ /\s/ || $word1 =~ /.*-.*/ || $word1 eq ""){
			#ignore, similarity cannot handle words with these chars, and these sibling don't have support words.
		}else{
			my $result = process($word1,$mainword);
			#if($result>=0){
				$siblings{$word1}=$result;
			#}
		}
	}
	
	my $value;
	my $i=0;
	if($verbose eq "true"){
		print "\nAll siblings:\n";
	}else{
		print "\n$MAXS closest siblings:\n";
	}
	
	foreach $value (sort{$siblings{$b} <=> $siblings{$a}} keys %siblings){
		if($i<$MAXS){
			$closestSib{$value}=$siblings{$value};
			print "\t[$value : $siblings{$value}]\n";
		}else{
			if($verbose eq "true"){
				print "\t$value : $siblings{$value}\n";
			}else{
				last;
			}
		}
		$i++;
	}

	#map the category(parent), then find all terms in PATO that have the related parent(category)
	#my $stmt2 = "select term from ".$database2.".patorelations where relative in (select patoterm from ".$database2.".patocatemap where category ='".$cate."') and relation = 'ac'" ;
	#CHANGED!!Map to entire pato space!
	my $stmt2 = "select distinct term from ".$database2.".patorelations";
	my $sth2 = $dbh->prepare($stmt2);
	$sth2->execute() or die $sth2->errstr."\n";
	
	while(my $word2 = $sth2->fetchrow_array()){
		#if($word2 =~ /^_.*/|| $word2 =~ /\s/ || $word2 =~ /.*-.*/ || $word2 eq ""){
			#ignore
		
		#}else{
			$patocousins{$word2}=0.0;
		#}
	}

if($verbose eq "true"){
	print "\nAll the pato cousins and the similarity:\n";
}

#compare the cousin with the closest siblings, and output the maximum or the average smilarity based on user's choice.
for my $cousin ( keys %patocousins){
	my @values=();
	my $j=0;
	my $maxi=-1;
	
	my %cclosurekey;
	my %cclosuresyn;
	
	my $stmt20="select keyword from ".$database2.".patokeywords where term='".$cousin."'";
	my $sth20 = $dbh->prepare($stmt20);
	$sth20->execute() or die $sth20->errstr."\n";
	
	while(my $kw = $sth20->fetchrow_array()){
		$cclosurekey{$kw} = 0.0; 
	}
	
	my $stmt30="select relative from ".$database2.".patorelations where term='".$cousin."' and relation = 'sy'";
	my $sth30 = $dbh->prepare($stmt30);
	$sth30->execute() or die $sth30->errstr."\n";
	
	while (my $syn = $sth30->fetchrow_array()){
		$cclosuresyn{$syn} = 0.0;
	}
	
	for my $sib ( keys %closestSib){
		if($verbose eq "true"){
			print "\tCousin <$cousin> vs Sibling <$sib> :\n";
		}
		
		my $max1 = -1;
		
		if((keys %cclosuresyn)==0){
			for my $cck (keys %cclosurekey){
				$cclosurekey{$cck} = process($cck, $sib);
			}

			my $i1=0;
			for my $scck (sort{$cclosurekey{$b} <=>$cclosurekey{$a}} keys %cclosurekey){
				if($i1<1){
					$max1 = $cclosurekey{$scck};
				}
				if($verbose eq "true"){
					print "\t\tSimilarity keyword <$scck> vs sibling <$sib> : $cclosurekey{$scck}\n";
				}
				$i1++;
			}
			if($verbose eq "true"){
				print "\n";
			}
		
		}else{
			for my $ccs (keys %cclosuresyn){
				$cclosuresyn{$ccs} = process($ccs, $sib);
			}
			my $i2=0;
			for my $sccs (sort{$cclosuresyn{$b} <=>$cclosuresyn{$a}} keys %cclosuresyn){
				if($i2<1){
					$max1 = $cclosuresyn{$sccs};
				}
				if($verbose eq "true"){
					print "\t\tSimilarity synonym <$sccs> vs sibling <$sib> : $cclosuresyn{$sccs}\n";
				}
				$i2++;
			}
			if($verbose eq "true"){
				print "\n";
			}
		}
		if($verbose eq "true"){
			print "\t\tSimilarity itself <$cousin> vs sibling <$sib> : ".process($cousin,$sib)."\n\n";
		}
		
		#result for comparing current sibling and the cousin--(maximum of the closure).
		my $r= max $max1, process($cousin,$sib);
		
		if($verbose eq "true"){
			print "\tSimilarity cousin <$cousin> vs sibling <$sib> : $r\n\n";
			print "................................\n";
		}
		
		if($r>=0){#filter pos
			$values[$j]=$r*$closestSib{$sib};
			$j++;
			if($r>$maxi){
				$maxi=$r;
			}
		}
		
	}
	$patocousinsa{$cousin}=wavg(@values);
	$patocousinsm{$cousin}=$maxi;
}

if($compmode eq "avg"){

	if($verbose eq "true"){
		print "\nBest $MAX matches by average (similarity between closest siblings and the cousin):\n";
	}else{
		print "\nAll matches sorted by average:\n";
	}
	
	my $ck=0;
	for my $cousina (sort{$patocousinsa{$b} <=>$patocousinsa{$a}}keys %patocousinsa){
		if($ck<$MAX){
			print"\t[$cousina->$patocousinsa{$cousina}]\n";
		}else{
			if($verbose eq "true"){
				print"\t$cousina->$patocousinsa{$cousina}\n";
			}else{
				last;
			}

		}
		$ck++;
	}

}elsif($compmode eq "max"){

	if($verbose eq "true"){
		print "\nBest $MAX matches by maximum (similarity between closest siblings and the cousin):\n";
	}else{
		print "\nAll matches sorted by max:\n";
	}
	
	my $cl=0;
	for my $cousinm (sort{$patocousinsm{$b} <=> $patocousinsm{$a}}keys %patocousinsm){
		if($cl<$MAX){
			print"\t[$cousinm->$patocousinsm{$cousinm}]\n";	
		}else{
			if($verbose eq "true"){
				print"\t$cousinm->$patocousinsm{$cousinm}\n";	
			}else{
				last;
			}	
		}
		$cl++;
	}
}else{
	print STDERR "Invalid comparison mode!\n";
	printusage();
}  



## ------------------- Subroutines Start Here ------------------- ##
# Subroutine that processes two words (finds relatedness).
sub process
{
  my $input1 = shift;
  my $input2 = shift;
  my $word1 = $input1;
  my $word2 = $input2;
  my $wps;
  my @w1options;
  my @w2options;
  my @senses1;
  my @senses2;
  my %distanceHash;

  if(!(defined $word1 && defined $word2))
  {
    print STDERR "Undefined input word(s).\n";
    return;
  }
  $word1 =~ s/[\r\f\n]//g;
  $word1 =~ s/^\s+//;
  $word1 =~ s/\s+$//;
  $word1 =~ s/\s+/_/g;
  $word2 =~ s/[\r\f\n]//g;
  $word2 =~ s/^\s+//;
  $word2 =~ s/\s+$//;
  $word2 =~ s/\s+/_/g;
  @w1options = &getWNSynsets($word1, "a");
  @w2options = &getWNSynsets($word2, "a");
  
  if(!scalar(@w1options)){
	@w1options = &getWNSynsets($word1, "nvar");
  }

  if(!scalar(@w2options)){
	 @w2options = &getWNSynsets($word2, "nvar");
  }
  
  if(!(scalar(@w1options) && scalar(@w2options)))
  {
    #print STDERR "'$word1' not found in WordNet.\n" if(!scalar(@w1options));
    #print STDERR "'$word2' not found in WordNet.\n" if(!scalar(@w2options));
    return -1;
  }

  @senses1 = ();
  @senses2 = ();
  foreach $wps (@w1options)
  {
    if($wps =~ /\#([nvar])\#/)
    {
      push(@senses1, $wps) if($measure->{$1});
    }
  }
  foreach $wps (@w2options)
  {
    if($wps =~ /\#([nvar])\#/)
    {
      push(@senses2, $wps) if($measure->{$1});
    }
  }
  if(!scalar(@senses1) || !scalar(@senses2))
  {
    #print STDERR "Possible part(s) of speech of word(s) cannot be handled by module.\n";
    return -1;
  }

  %distanceHash = &getDistances([@senses1], [@senses2]);

    my ($key) = sort {$distanceHash{$b} <=> $distanceHash{$a}} keys %distanceHash;
    my ($op1, $op2) = split(/\s+/, $key);
    return $distanceHash{$key};
    
  #}
}

# Subroutine to get all possible synsets corresponding to a word(#pos(#sense))
sub getWNSynsets
{
  my $word = shift;
  my $pos = shift;
  my $sense;
  my $key;
  my @senses;
  return () if(!defined $word);

#if($posmode eq "all"){
# 	$pos = "nvar";
#}elsif($posmode eq "adj"){
#	$pos = "a";
#}else{
#	print STDERR "Invalide POS mode!\n";
#	printusage();
#}
  # Get the senses corresponding to the raw form of the word.
  @senses = ();
  foreach $key ("n", "v", "a", "r")
  {
    if($pos =~ /$key/)
    {
      push(@senses, $wn->querySense($word."\#".$key));
    }
  }

  # If no senses corresponding to the raw form of the word,
  # ONLY then look for morphological variations.
  if(!scalar(@senses))
  {
    foreach $key ("n", "v", "a", "r")
    {
      if($pos =~ /$key/)
      {
        my @tArr = ();
        push(@tArr, $wn->validForms($word."\#".$key));
        push(@senses, $wn->querySense($tArr[0])) if(defined $tArr[0]);
      }
    }
  }
  return @senses;
}

# Subroutine to compute relatedness between all pairs of senses.
sub getDistances
{
  my $list1 = shift;
  my $list2 = shift;
  my $synset1;
  my $synset2;
  my $tracePrinted = 0;
  my %retHash = ();
  return {} if(!defined $list1 || !defined $list2);
  my %errcache;
LEVEL2:

  foreach $synset1 (@{$list1})
  {
    foreach $synset2 (@{$list2})
    {

      # modified 12/8/03 by JM
      # it is possible for getRelatedness to return a non-numeric value,
      # and this can cause problems in ::process() when the relatedness
      # values are sorted
      #$retHash{"$synset1 $synset2"} = $measure->getRelatedness($synset1, $synset2);
      my $score = $measure->getRelatedness($synset1, $synset2);
      $retHash{"$synset1 $synset2"} = $score;
      my ($err, $errString) = $measure->getError();

      #end modifications
      if($err)
      {

        # 12/9/03 JM (#1)
        # cache error strings indicating that two words belong
        # to different parts of speech
        $errString =~ m/(\S+\#[nvar])(?:\#\d+)? and (\S+\#[nvar])(?:\#\d+)?/;
        my $keystr = "$1 $2";
        print STDERR "$errString\n" unless $errcache{$keystr};
        $errcache{$keystr} = 1;

        # JM 12/8/2003
        # getRelatedness() can return a warning if the two concepts
        # are from different taxonomies, but we need to keep
        # comparing relatedness values anyways
        #
        # last LEVEL2;
        last LEVEL2 if ($err > 1);
      }
    }
  }
  #print "\n\n" if(defined $opt_trace && $tracePrinted);
  return %retHash;
}

# print the usage
sub printusage{
	print "Usage:\n 
	You need to specify five arguments:\n
	\t1. Verbose, value could be true or false.\n
	\t2. Comparison mode, value could be avg or max.\n
	\t3. POS mode, value could be all or adj.\n
	\t4. The measure method you want to use. (could be lesk or vector)\n
	\t5. The input file name.\n";	
}


# Print routine to print synsets...
sub printSet
{
  my $synset = shift;
  my $offset;
  my $printString = "";
  if($synset =~ /(.*)\#([nvar])\#(.*)/)
  {
      $printString = "$synset";
      $printString =~ s/\s+$//;
      $printString =~ s/^\s+//;
  }
  print "$printString";
}

sub wavg{
	if(@_==0){
		return -1;
	}else{
		return sum(@_);
	}
}
__END__


