#compare extracted states with a number of ontologies
#Given [A]groups of termsExtractedFromLiterature, and [B]terms and their classes extracted from ontologies such as Oxford[B1], FNAGlossary[B2], RadfordText[B3], and PATO ontology[B4]
#check in different ontologies the semantic class a term belongs to.

#[A] takes the form of a relational table:
#terms(term, cooccurTerm, frequency, source files) for example:
#(green, grey, 2, "file3.txt-3, file5.txt-2")
#(grey, green, 2, "file3.txt-3, file5.txt-2")
#, which says that the terms green and grey appeared together in the 3rd sentence in file3.txt and the 2nd sentence in file5.txt.

#[B1] takes the form of a relational table:
#oxford(term, definition, category) for example:
#(abaxial, the side or surface facing away from the axis, position), which says that in the Oxford glossary, term "abaxial" is a type of "position".

#[B2] takes the form of a relational table:
#fna(id, term, definition, category)

#[B3] takes the form of a relational table:
#radford(id, term, category)

#[B4] takes the form of a text document in obo format, see here


#Write scripts to check the category of a term in list of terms of A. Will need a script to look up the terms in B1-B3 as they are all in a MySQL table (assume all the tables [A-B3] are in one database called ontologies), and another script to look up the terms in #B4. Save the results in a relational table called comparison in ontologies.

#comparison (term, ontology, category) for example:
#(grey, fna, "")
#(grey, pato, color)
#(grey, oxford, coloration), these say that fnaGloss does not contain "grey", PATO defines "grey" as a color, and Oxford defines "grey" as coloration.



use strict;
use DBI;
use LWP::Simple; #to read a URL

if(@ARGV !=2){
	print "usage: perl checkontologies.pl sourcedatabase corpusprefix\n";
}

my $db = $ARGV[0];
my $prefix = $ARGV[1];
my $host = "localhost";
my $user = "termsuser";
my $password = "termspassword";

my @ontologies=("oxford", "fna", "radford");

my $dbh = DBI->connect("DBI:mysql:host=$host", $user, $password) or die DBI->errstr."\n";
my $query = $dbh->prepare('use '.$db) or die $dbh->errstr."\n";
$query->execute() or die $query->errstr."\n";

$query = $dbh->prepare('create table if not exists '.$prefix.'_comparison (term varchar(100), ontology varchar(20), category varchar(100), categorypath varchar(1000), primary key (term, ontology, category)) ');
$query->execute() or die $query->errstr."\n";
$query = $dbh->prepare('delete from '.$prefix.'_comparison');
$query->execute() or die $query->errstr."\n";

$query = $dbh->prepare('select distinct term from '.$prefix.'_allcharacters');
$query->execute() or die $query->errstr."\n";

my $url = "http://obo.cvs.sourceforge.net/*checkout*/obo/obo/ontology/phenotype/quality.obo";
my $content = get($url);
$content =~ s#\n#@@#g;
$content =~ s/@@@@/#/g;
$content =~ s#@@#%#g;
	
while(my ($term) = $query->fetchrow_array()){
	$term = lc $term;
	if($term =~ /_/){
		$term =~ s#.*?-##g;
		next if termexists($term);
	}
	
	my ($nterm, @results, $onto);

	
	foreach $onto (@ontologies){
		print "work on $term in $onto\n";
		($nterm, @results) = checkthesaurus($term, $onto);
		if($nterm eq $term){
			insertresult($term, $onto, @results);#nterm is a close and not extact match of term
		}else{
			insertresult($term."[".$nterm."]", $onto, @results);#nterm is a close and not extact match of term
		}
	}
	
	print "work on $term in PATO\n";
	($nterm, @results) = checkOBO($term);
	if($nterm eq $term){
		insertresult($term, "pato", @results);#nterm is a close and not extact match of term
	}else{
		insertresult($term."[".$nterm."]", "pato", @results);#nterm is a close and not extact match of term
	}
}

sub termexists{
	my $term = shift;
	my $q = $dbh->prepare('select * from '.$prefix.'_allcharacters where term ="'.$term.'"');
	$q->execute() or die $q->errstr."\n";
	if($q->rows() > 0){
		return 1;
	}
	return 0;
	
}
	
sub checkOBO{
    my ($term, $mode) = @_;
	my ($cat, @results);
	my ($def, $nterm);
	#look for 
	#[Term]
	#id: PATO:0001599
	#name: rotated
	#def: "An oriented quality inhering in a bearer by virtue of the bearer's being relocated around an axis." [answers.com:answers.com "http://www.answers.com/"]
	#subset: attribute_slim
	#synonym: "rotation" EXACT []
	#is_a: PATO:0000614 ! oriented
	
	($nterm, $def) = getdef4($term, $mode); # may have 0-m defs.
	if($def=~/\w+/){ #$term is a preferred term
		($cat, @results) = getcats($def, "exact"); #get cat and cat's synonyms
	}else{#try to find the preferred term that is synomym to $term
		if($nterm eq $term){
			($cat,@results) = getsyncats4($term)
			#my $pterm = findpterm4($term);
			#($nterm, $def) = getdef4($pterm, $mode) if $pterm=~/\w+/;
			#@results = getcats($def, "exact") if $pterm=~/\w+/;
		}
	}
	return ($nterm, $cat, @results);
}

sub getsyncats4{
	my $term = shift;
	my (@results, $cat);
	if($content =~ /#[^#]*?synonym: "$term"(.*?)#/){
		if($1=~/is_a: [^%#]*?! (.*?)(%|$)/){
			$cat = $1;
			#push(@results, $cat);
			my @syn = getsyn4($cat, "exact"); #also find $cat (character) 's synonyms
			push(@results, @syn);
		}
		
	}
	return ($cat, @results);
}

#sub findpterm4{
#	my $term = shift;
#	if($content =~ /#([^#]*?)synonym: "$term".*?#/){
#		if($1=~ /name: (.*?)%/){
#			return $1;
#		}
#	}
#	return "";
#}

sub getcats{
	my ($def, $mode) = shift;
	my ($cat, @results);
	if($def !~ /is_obsolete: true/){#and not obsolete
		$cat = getcat4($def);
		#push(@results, $cat);
		if ($cat=~/\w/){
			my @syn = getsyn4($cat, $mode); #also find $cat (character) 's synonyms
			push(@results, @syn);
		}
	}else{
		return ("", @results);
	}	
	return ($cat, @results);
		
}


#find $term's synonyms
sub getsyn4{
	my ($term, $mode) = @_;
	my (@syn, $def);
	($term, $def) = getdef4($term, $mode);
	while($def =~/synonym: "([^@]*?)"(.*)/){
		push(@syn, $1);
		$def = $2;
	}
	return @syn;
}

sub getcat4{
	my $def = shift;
	if($def =~ /is_a: [^%#]*?! (.*?)(%|$)/){
		return $1;
	}
	return "";
}

sub getdef4{
	my ($term, $mode) = @_;
	my $termc = $term;
	my $def;
	if($content	=~ /name: $term%(.*?)#/){
		$def = $1;
	}elsif($mode ne "exact" and $content =~ /name: ([^%#]*?$term.*?)%(.*?)#/){
		$term = $1;
		$def = $2;
		if($def=~/is_obsolete: true/){
			$def = "";
			$term = $termc;
		}
	}
	return ($term, $def);
}


my @supers = ();
my @syns = ();

sub insertresult{
	my($term, $the, @results) = @_;
	my $inserted = 0;
	$term =~ s#"#\\"#g;
	foreach my $cat (@results){
		@supers = ();
		@syns = ();
		if($the eq "pato"){
			getallcategoriesinOBO(0, $cat, $the);
		}else{
			getallcategoriesinthesaurus(0, $cat, $the);
		}
		
		push(@supers, @syns);
		my $catpath= tostring(",", @supers);
		my $search = $dbh->prepare('select * from '.$prefix.'_comparison where term="'.$term.'" and ontology="'.$the.'" and category="'.$cat.'"');
		$search->execute() or die $search->errstr."\n";
		if($search->rows()<=0){
			my $insert = $dbh->prepare('insert into '.$prefix.'_comparison values ("'.$term.'", "'.$the.'", "'.$cat.'", "'.$catpath.'")');
			$insert->execute() or die $insert->errstr."\n";
		}
		$inserted = 1;
		print "$term completed for $the\n";
	}
	
	if($inserted == 0){
		my $insert = $dbh->prepare('insert into '.$prefix.'_comparison values ("'.$term.'", "'.$the.'", "", "")');
		$insert->execute() or die $insert->errstr."\n";
	}
	
}

sub tostring{
	my @terms = @_;
	my $string = ",";
	foreach my $t (@terms){
		if($string !~/,$t,/){
			$string .=$t.","
		}
	}
	$string =~ s#^,+##;
	$string =~ s#,+$##;
	return $string;
}
sub checkthesaurus{
	my ($term, $the, $mode) = @_;
	#$mode: exact match or fuzzy match
	my @results=();
	my $exactmatch = 0;
	my ($cat, $nterm);
	my $theq = $dbh->prepare('select distinct category from '.$the.' where term = "'.$term.'"');
	$theq->execute() or die $theq->errstr."\n";
	while(($cat) = $theq->fetchrow_array()){
		$exactmatch = 1;
		push(@results, $cat);
	}
	
	if($mode ne "exact"){
		if($exactmatch == 0){
			my $theq = $dbh->prepare('select distinct term, category from '.$the.' where term like "%'.$term.'%"');
			$theq->execute() or die $theq->errstr."\n";
			while(($nterm, $cat) = $theq->fetchrow_array()){
				push(@results, $cat);
				$term = $nterm if($nterm =~/\w/);
			}
		}
	}
	
	return ($term, @results);
}

sub getallcategoriesinthesaurus{
	my($level, $cat, $the) = @_;
	
	my ($super, @supersyns);
	if($cat !~/\w/ || $level > 10){
		return;
	}
	($cat, @supersyns) = checkthesaurus($cat, $the, "exact");
	push(@syns, @supersyns);
	foreach $super (@supersyns){
		if($super =~/\w+/){
			getallcategoriesinthesaurus($level+1, $super, $the);
		}
	}
	return;
}

sub getallcategoriesinOBO{
	my($level, $cat, $the) = @_;
	my ($super, @supersyns);
	if($cat !~/\w/ || $level > 10){
		return;
	}
	($cat, $super, @supersyns) = checkOBO($cat, $the, "exact");
	push(@supers, $super);
	push(@syns, @supersyns);
	if($super =~/\w+/){
			getallcategoriesinOBO($level+1, $super, $the);
		}
	return;
	
}