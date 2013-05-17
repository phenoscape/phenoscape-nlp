#!C:\Perl\bin\perl

#use the database terms created by infernnp-clean-db.pl
#to populate database tables such as element, nestelement, property, elemprop, and propvalue.
#to output RDF ontology describing the semantic structure of the text collection 
#interact with end users for properties

#use XML::DOM;
#use lib 'U:\\Research\\Projects\\unsupervised\\';
#use NounHeuristics;
#use InitialMarkup;
#use SentenceSpliter;
#use ReadFile;
use strict;
use DBI;

my $db = "terms";
my $host = "localhost";
my $user = "termsuser";
my $password = "termspassword";
my $out = "out.rdf";

my $dbh = DBI->connect("DBI:mysql:database=$db:host=$host", $user, $password)
or die DBI->errstr."\n";
my $test = $dbh->prepare('use terms')
or die $dbh->errstr."\n";
$test->execute() or die $test->errstr."\n";
$test = $dbh->prepare('show tables from terms')
or die $dbh->errstr."\n";
$test->execute() or die $test->errstr."\n";
my ($table, $exists, $create, $del, $query);
while($table = $test->fetchrow_array()){
$exists.=$table." ";
}

die "Run InferNNP first\n" if($exists !~/\bsentence\b/ || $exists !~/\bwordsent\b/ || $exists !~/\bwordpos\b/ || $exists !~/\blinkage\b/);

if($exists =~ /\belement\b/){
$del = $dbh->prepare('drop table nestedelement');
$del->execute();

$del = $dbh->prepare('drop table elemprop');
$del->execute();

$del = $dbh->prepare('drop table element');
$del->execute();

$del = $dbh->prepare('drop table propvalue');
$del->execute();

$del = $dbh->prepare('drop table property');
$del->execute();
}

$create = $dbh->prepare('create table element (eid int auto_increment not null unique primary key, ename varchar(50)) engine=innodb');
$create->execute() or warn $create->errstr."\n";

$create = $dbh->prepare('create table property (pid int auto_increment not null unique primary key, pname varchar(50)) engine=innodb');
$create->execute() or warn $create->errstr."\n";

$create = $dbh->prepare('create table nestedelement (parentid int not null, childid int not null, primary key(parentid, childid), foreign key(parentid) references element(eid) on update cascade on delete cascade, foreign key(childid) references element(eid) on update cascade on delete cascade) engine=innodb');
$create->execute() or warn $create->errstr."\n";

$create = $dbh->prepare('create table elemprop (eid int not null, pid int not null, primary key(eid, pid), foreign key(eid) references element(eid) on update cascade on delete cascade, foreign key(pid) references property(pid) on update cascade on delete cascade) engine=innodb');
$create->execute() or warn $create->errstr."\n";

$create = $dbh->prepare('create table propvalue (pid int not null, value varchar(100) not null, primary key(pid, value), foreign key(pid) references property(pid) on update cascade on delete cascade) engine=innodb');
$create->execute() or warn $create->errstr."\n";

# populate property table
$create = $dbh->prepare("insert into property(pname) values(\"presence\"),(\"type\"),(\"odor\"),(\"taste\"),(\"color\"),(\"size\"),(\"thickness\"),(\"height\"),(\"length\"),(\"width\"),(\"diameter\"),(\"area\"),(\"volume\"),(\"count\"),(\"cycly\"),(\"merosity\"),(\"fusion\"),(\"shape\"), (\"arrangement\"),(\"position\"),(\"orientation\"),(\"posture\"),(\"surface configuration\"),(\"venation\"),(\"vestiture\"),(\"epidermal-excrescence\"),(\"symmetry\"),(\"duration\"),(\"maturation\")");
$create->execute() or warn $create->errstr."\n";

# populate other tables
$query = $dbh->prepare("select filename, endindex from linkage order by endindex");
$query->execute() or warn $query->errstr."\n";

my ($filename, $endindex, $sentence, $count);
my $index = 0; #starts with 0
#my $count = 1;
while(($filename, $endindex) = $query->fetchrow_array()){
    #print "process file number $count \n";
	#$count++;
    my ($ptag, $ptagid, $tag, $tagid, $query2, $query3);#no parent tag
	while($index <= $endindex){
    	$query2 = $dbh->prepare("select sentence, tag from sentence where sentid = $index");
    	$query2->execute() or warn $query2->errstr."\n";
    	my ($sentence, $tag) = $query2->fetchrow_array();
		my $seeperiod = $sentence =~/\.\s*$/? 1 : 0;
    	#@todo: normalization
    	$query2 = $dbh->prepare("select eid from element where ename = \"$tag\"");
    	$query2->execute() or warn $query2->errstr."\n";
    	if($query2->rows == 0){#insert a new element
    		$query3 = $dbh->prepare("insert into element(ename) values(\"$tag\")");
    	    $query3->execute() or warn $query3->errstr."\n";
    	}
		$query2 = $dbh->prepare("select eid from element where ename = \"$tag\"");
    	$query2->execute() or warn $query2->errstr."\n";
		($tagid) = $query2->fetchrow_array();
		if($ptagid){
    		$query2 = $dbh->prepare("select eid from element where ename = \"$ptag\"");
        	$query2->execute() or warn $query2->errstr."\n";
    		($ptagid) = $query2->fetchrow_array();
		}
	    #cases: if has ptag not seeperiod, then add ptag/tag in nestedelement
		#       if has ptag and seeperiod, then add ptag/tag in nestedelement, unset $ptag
		#       if no ptag not seeperiod, then set tag as ptag
		#       if no ptag but seeperiod, do nothing.
	    if($ptagid){
		   $query2 = $dbh->prepare("select parentid from nestedelement where parentid = $ptagid and childid = $tagid");
           $query2->execute() or warn $query2->errstr."\n";
		   if($query2->rows ==0){
		      if($ptagid == 30 && $tagid == 13){
			  print;
			  }
		      $query2 = $dbh->prepare("insert into nestedelement values($ptagid, $tagid)") if ($ptagid != $tagid);
              $query2->execute() or warn $query2->errstr."\n";
		   }
		   $ptag ="" if($seeperiod);	
		   $ptagid ="" if($seeperiod);	
		}else{
		   $ptag = $tag if(!$seeperiod);
		   $ptagid = $tagid if(!$seeperiod);
		}
		#select properties and values
		property($sentence, $tagid);
    	$index++;
    }
}
my $PARENTSTR ="";
outputRDF();
############################## outputRDF
sub outputRDF{
    my (%root, $RDF);
    $RDF = "<rdf:RDF xmlns:rdf=\"http:\/\/www.w3.org\/1999\/02\/22-rdf-syntax-ns#\" ";
    $RDF .= "xmlns:hong=\"http:\/\/hong.fims.uwo.ca\/2007\/rawontology#\">\n";
    $RDF .= "<rdf:description rdf:about=\"plant\">\n";
    #@todo: add properties
	$RDF .= "<hong:type>unknown</hong:type>\n";
    #select root elements
    my $query = $dbh->prepare("select eid, ename from element where eid not in (select childid from nestedelement)");
    $query->execute() or warn $query->errstr."\n";
    while(my($eid, $ename) = $query->fetchrow_array()){
       $RDF .="<hong:has-organ rdf:resource=\"$ename\" />\n";
       $root{$eid} = $ename;
	}
    $RDF .="</rdf:description>\n";
    #internal nodes
    $RDF .= processnode(%root);
	$RDF.= "</rdf:RDF>";
	open(OUT, ">$out") || die "$!\n";
    print OUT $RDF;
}

############################# recusive
sub processnode{
my %parents = @_;
my ($RDF, $pid, $query, %elements);
foreach $pid (keys(%parents)){
  if($PARENTSTR !~ /\b$pid\b/){
    $PARENTSTR .= $pid." ";
	my $pname = $parents{$pid};
	$RDF .="<rdf:description rdf:about=\"$pname\">\n";
	$RDF .=addproperty($pname, $pid);
	#find children of the parent
	my $query = $dbh->prepare("select childid, ename from nestedelement, element where nestedelement.childid = element.eid and parentid = $pid");
	$query->execute() or warn $query->errstr."\n";
	if($query->rows > 0){
    	while(my($childid, $cname) = $query->fetchrow_array()){
       		$RDF .="<hong:has-organ rdf:resource=\"$cname\" />\n";
       		$elements{$childid} = $cname ;
    	}
	}
    $RDF .="</rdf:description>\n";
	$RDF .=processnode(%elements);
  }
}
return $RDF;
}
############### add property
sub addproperty{
    my ($ename,$eid) = @_;
    my ($RDF, $query2, $query3);
    $RDF .="<hong:name>$ename</hong:name>\n";
    $query2 = $dbh->prepare("select property.pid, pname from property, elemprop where property.pid = elemprop.pid and eid = $eid");
    $query2->execute() or warn $query2->errstr."\n";
    while(my($propid, $prop) = $query2->fetchrow_array()){
    	$query3 = $dbh->prepare("select value from propvalue where pid = $propid");
        $query3->execute() or warn $query3->errstr."\n";
        while(my($value) = $query3->fetchrow_array()){
           $RDF .= "<hong:$prop>$value</hong:$prop>\n";
        }
    }
    return $RDF;
}

############################## Property
sub property{
my ($sentence, $tagid) = @_;
my ($query, $query2, $pid, %property, $ps, @ps);

$query = $dbh->prepare("select pid, pname from property order by pid");
$query->execute() or warn "$query->errstr\n";
print "Sentence: $sentence\n\n";
while(my ($pid, $proper) = $query->fetchrow_array()){
   print "$pid: $proper";
   if($pid % 3 == 0){
     print "\n";
   }else{
     print "\t";
   }
   $property{$pid} = $proper;
}
print "properties mentioned:\n";
$ps = <>;
chomp($ps);
@ps = split(/\s+/, $ps);
foreach (@ps){
   $query2 = $dbh->prepare("insert into elemprop values ($tagid, $_)");
   $query2->execute() or warn $query2->errstr."\n";
   print "Sentence: $sentence\n\n";
   print "Value for property ".$property{$_}."\n";
   my $value = <>;
   chomp($value);
   $query2 = $dbh->prepare("insert into propvalue values ($_, \"$value\")");
   $query2->execute() or warn $query2->errstr."\n";
}
}