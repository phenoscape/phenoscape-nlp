#include external packages
use strict;
use DBI;
use DBD::mysql;
use WordNet::QueryData 1.40;
use Getopt::Long;
use WordNet::Similarity;
use List::Util qw(sum);
use List::Util qw[min max];


if($#ARGV != 0){
	print "Please specify the input file name.\n";
	print "Usage:\n";
	print "new_runner2.pl <input_file_name>\n";
}

my $filename = $ARGV[0];
my $MAX = 5;

open(INPUT,"<$filename");#open the input file
my(@lines) = <INPUT>;#read the entrie file into list

my $word1;

foreach $word1 (@lines){
	
	chomp($word1);
	
	my $host = "localhost";
	my $user = "root";
	my $pw = "";
	my %scores;
		
	my $dbh = DBI->connect("DBI:mysql::$host", $user, $pw)
	or die DBI->errstr."\n";
	
		
	my $stmt = "select distinct term from phenoscape.patokeywords";
	
	my $sth = $dbh->prepare($stmt);
	$sth->execute() or die $sth->errstr."\n";
	
	
	while(my $word2 = $sth->fetchrow_array()){
		$scores{$word1."<>".$word2} = process($word1, $word2);
		#print "$word1 <> $word2 : ". $scores{$word1."<>".$word2}."\n";
	}
	
	print "================================\n";
	print "$word1\n";
	print "================================\n";
	
	my $i = 0;
	foreach my $key (sort{$scores{$b}<=>$scores{$a}} keys %scores){
		if($i >= $MAX){
			last;
		}
		print "$key : $scores{$key}\n";
		$i++;
	
	}

}


#============Sub routines start here=========#

#Return the similarity of two words
sub process
{
	my $word1=shift;#our context
	my $word2=shift;#PATO
	
	my $wps1="$word1#a#1";
	my $wps2="$word2#a#1";
	
	# Load WordNet::QueryData
	my $wn = WordNet::QueryData->new;
	die "Unable to create WordNet object.\n" if(!$wn);

	my $opt_type = "WordNet::Similarity::vector";
	# Load the WordNet::Similarity module.
	my $type = $opt_type;
	$opt_type =~ s/::/\//g;
	$opt_type .= ".pm";
	require $opt_type;
	my $measure = undef;
	$measure = $type->new($wn);
	
	my $score = $measure->getRelatedness($wps1,$wps2);
	if (!defined $score){
		$score = 0;
	}
	return $score;
}