#infer equivalent terms in unsupervised-marked-up descriptions
#save equivalent terms in a database
# for example, "ovid to obovid" may suggest ovid and obovid are of same semantic class: shape
# this version is just for FNA

# use FNA glossary

#use lib 'C:\\Documents and Settings\\hong cui\\My Documents\\RESEARCH\\PROJECTS\\CharacterMarkup\\Unsupervised\\src\\';
use lib 'C:\\Documents and Settings\\hongcui\\Desktop\\WorkFeb2008\\Projects\\unsupervised\\';
use NounHeuristics;
use InitialMarkup;
use SentenceSpliter;
use ReadFile;
use strict;
use DBI;

#categorizeterms.pl C:\Docume~1\hongcui\Desktop\WorkFeb2008\Projects\algae120-result\ algae120
if(@ARGV != 3) {
print "\ncategorizeterms.pl discovers terms that belong to the same semantic class from table corpus.sentence. Results are saved in the database 'corpus.semanticclass'\n";
print "\nUsage:\n";
print "\tperl categorizeterms.pl workdir prefix glossarydir\n";
print "\twork directory: the full filepath to the raw markup descriptions\n";
print "\tprefix: the prefix for the database. Use the same prefix used in supervised.pl\n";
print "\tglossarydir: the full filepath to the corresponding glossary\n";
print "\tResults will be saved in prefix_corpus database\n";
exit(1);
}

print stderr "Start: ".time()."\n";
my $workdir = $ARGV[0];
if($workdir !~ /\\$/)
{$workdir .="\\";
}
my $prefix = $ARGV[1];
my $debug = 1;
#my $thresh = 1; #how to auto-set this threshold?
my $linkthreshold = 1; #all association among words with less than $linkthreshold will be removed from the graph.
my $stop = $NounHeuristics::STOP;
my $adverbs = $stop."|almost|along|beyond|few|frequently|occasionally|often|rarely|somewhat|throughout|very";
my $stop1 = $stop;
$stop1 =~ s#\|(and|or|to)\b##g;
#my $workdir = "C:\\Documents and Settings\\Hong Cui\\My Documents\\RESEARCH\\PROJECTS\\CharacterMarkup\\Unsupervised\\test\\";

print "use threshold $linkthreshold to classify\n";


my $db = $prefix."_corpus";
my $host = "localhost";
my $user = "termsuser";
my $password = "termspassword";


#my %groups = {};
my $dbh = DBI->connect("DBI:mysql:host=$host", $user, $password)
or die DBI->errstr."\n";

my $test = $dbh->prepare('use '.$db)
or die $dbh->errstr."\n";

$test->execute() or die $test->errstr."\n";

$test = $dbh->prepare('show tables from '.$db)
or die $dbh->errstr."\n";
$test->execute() or die $test->errstr."\n";
my ($table, $exists, $create, $del);
while($table = $test->fetchrow_array()){
$exists.=$table." ";
}

if($exists !~/\bsentence\b/){
print "Table sentence does not exist. Run unsupervised.pl first\n";
exit(1);
}

if($exists !~/\bsemanticclass\b/){
#create table sentence
$create = $dbh->prepare('create table semanticclass (classid int, word varchar(100), weight int, primary key(classid, word))');
$create->execute() or die "$create->errstr\n";
}else{
$del = $dbh->prepare('delete from semanticclass');
$del->execute();
}

if($exists !~/\blinktable\b/){
#create table linktable
$create = $dbh->prepare('create table linktable (id1 varchar(30), id2 varchar(30), links int, primary key(id1, id2))');
$create->execute() or die "$create->errstr\n";
}else{
$del = $dbh->prepare('delete from linktable');
$del->execute();
}

#sort the sentences into their pattern type
#then start from the easiest pattern.
#collect most reliable information first
#[synonyms]
#or 2 - cleft [ pinnate ]
#simple list with 1- or n-grams (only after "or"), also covers simple nested list
#exocarp smooth , warty , prickly , or hirsute [ corky or scaly ] ;
#segments lanceolate , linear , or cuneate [ rhombic ] , glabrous or variously scaly , unarmed or bearing prickles ( proximal segments modified into spines in Phoenix ) .
#unarmed or bearing prickles or marginal teeth ;
#leading n should be tagged before applying this pattern <mesocarp> fleshy or ...
#my $simplelist ="((?:[,>] ".$word.")*),? ?or (".$word."{1,})";   #a, b, c, or e f g
#n-gram pattern
#, prostrate and reniform or polyreniform , or nearly erect
#double or
#use $simplelist twice
#plication ( folding lengthwise into pleats or furrows ) * - or tent - shaped ( reduplicate , splitting along abaxial ridges ) or V - shaped ( induplicate , splitting along adaxial ridges ) ;
#staminodes in pistillate flowers <distinct> or <variously connate> or <adnate to <pistil> or <petals>> ;
#abbreviated
#to
#Roots translucent to brown
#number
#<perianth>perianth 1 * 2 - seriate ;</perianth>
# <sepal>sepals [ 2 * ] 3 [ * 4 ] , distinct or connate ;</sepal>
# <petal>petals [ 2 * ] 3 [ * 4 ] , distinct or variously connate ;</petal>
#<stamen>stamens [ 3 * ] 6 * 34 [ * 1000 ] ;</stamen>
#Seeds 1 ( * 2 + )
my $word = "(?:[\\w-]+\\s)";
my $synonyms ="(?:>|^|,|$stop) (".$word."{1,3})(\\[ (.{1, 30}) \\])";
my $simple = "((?:(?:^|,|>) ?".$word."))or (".$word."{1,})";     # a or b
my $list = "((?:(?:^|,|>) ?".$word.")*), or (".$word."{1,})";   #a, b, c, or e f g
my $to =  "((?:(?:^|,|>) ?(?:[-a-z]+\\s)))to ((?:[-a-z]+\\s){1,})";
my $tolist ="((?:(?:^|,|>) ?(?:[-a-z]+\\s))*), to ((?:[-a-z]+\\s){1,})";
my $nextid = 1;

my $count = 0;
my $texts = "";
opendir(IN, "$workdir") || die "$!: $workdir\n";
while(defined (my $file=readdir(IN))){
	if($file !~ /\w/){next;}
	my $text = ReadFile::readfile("$workdir$file");
  $texts .= lc $text;
  #todo: find "to-phrases" such as "reduced to" and not take the phrase as a "to" pattern
  $count++;
  if($count % 10 == 0){
    $texts =~ s#<\?xml version="1.0"\?>##g;
    $texts =~ s#-#_#g;
    $texts =~ s#(\W)# \1 #g;
    $texts =~ s# \(.*?\)##g;     #remove (...)
    $texts =~ s#(\d)[\d\.\s]+#NUM #g;    #turn number patterns to the first number
    $texts =~ s#\s+# #g;
    $texts =~ s#<\s+#<#g;
    $texts =~ s#\s+>#>#g;
    $texts =~ s#</\s+#</#g;
    $texts =~ s#^\s*##;
    $texts =~ s#\s*$##;
    $texts = mark($texts);  #mark organ/structure names in sentences
    dothis($texts);
    $texts = "";
  }
}
print stderr "Completed $count files: ".time()."\n";
#setthresh();
print stderr "pruning: ".time()."\n";
prunefalselinks();
print stderr "classifying: ".time()."\n";
classify();
print stderr "completed: ".time()."\n";

########################################################################
sub mark{
  my $sent = shift;
  #my $copy = $sent;
  my $select = $dbh->prepare('select distinct tag from sentence');
  $select->execute() or warn "$select->errstr\n";
  while((my $tag) = $select->fetchrow_array()){
    #create tag pattern to cover pl forms
    if(length($tag) >= 3){
      my $ctag = $tag;
      chop($ctag);
      my $select2 = $dbh->prepare('select word from wordpos where word like "'.$ctag.'%" and pos="p"');
      $select2->execute() or warn "$select2->errstr\n";
      $tag = "(?:".$tag."|";
      while((my $p) = $select2->fetchrow_array()){
       $tag .=$p."|";
      }
      chop($tag);
      $tag .=")";
    }
    if($tag =~/\w+/){
      $sent =~ s#\b($tag)\b#<\1>#g;
    }
  }
  $sent =~ s#<<#<#g;
  $sent =~ s#>>#>#g;
  $sent =~ s#</<#</#g;
  return $sent;
}


##########################################################
# extract sets of words of equivalent relationships
sub dothis{
  my $sent = shift;
  while($sent =~/\w+/){
    $sent = extractFrom($sent);
  }
}


############
#my $synonyms ="(?:?|^|,|$stop) (".$word."{1,3})(\\[ (.*?) \\])";
#my $simple = "((?:(?:^|,|>) ?".$word."))or (".$word."{1,})";     # a or b
#my $list = "((?:(?:^|,|>) ?".$word.")*), or (".$word."{1,})";   #a, b, c, or e f g
#my $to =  "((?:(?:^|,|>) ?(?:[-a-z]+\\s)))to ((?:[-a-z]+\\s){1,})"
#my $tolist ="((?:(?:^|,|>) ?(?:[-a-z]+\\s))*), to ((?:[-a-z]+\\s){1,})";
sub extractFrom{
  my $sent = shift;
  my @terms = ();
  #print "### unaided eye in $sent\n\n" if($sent =~ /\bunaided\b/ && $sent =~ /\beye\b/);
  #print "### forked into in $sent\n\n" if($sent =~ /\bforked\b/ && $sent =~ /\binto\b/);
  #print "### green toward in $sent\n\n" if($sent =~ /\bgreen\b/ && $sent =~ /\btoward\b/);
  #print "### for green in $sent\n\n" if($sent =~ /\bfor\b/ && $sent =~ /\bgreen\b/);
  #print "### reduce small in $sent\n\n" if($sent =~ /\breduced\b/ && $sent =~ /\bsmall\b/);
  if($sent =~ /$synonyms/){
     print "\t".substr($sent, $-[0], $+[0]-$-[0])."\n" if $debug;
     my $save = $2;
     my $t1 = trim($1);
     my $t2 = trim($3);
     @terms = split(/\s*(\bor\b|,)\s*/, $t2);
     if($debug){
        print "[$t1] and [@terms] are in the same group [syn]\n";
     }
     push(@terms, $t1);
     group(@terms);
     $save =~ s#\[#\\[#g;
     $save =~ s#\]#\\]#g;
     $sent =~ s# $save##;
     return $sent;
  }

  if($sent =~/$simple/){
    print "\t".substr($sent, $-[0], $+[0]-$-[0])."\n" if $debug;
    my $t1 = trim($1);
    my $t2 = trim($2);
    #if($t1 =~/\w+/){
      push(@terms, $t1, $t2);
      group(@terms);
      if($debug){
        print "[$t1] and [$t2] are in the same group [simple]\n";
     }
    #}
    $sent =~ s#$simple##;
    return $sent;
  }

  if($sent =~/$list/){
    print "\t".substr($sent, $-[0], $+[0]-$-[0])."\n" if $debug;
    my $t1 = trim($1);
    my $t2 = trim($2);
    #if($t1 =~/\w+/){
      @terms = split(/\s*,\s*/,$t1);
      shift(@terms) if @terms >= 3; #be conservative to avoid sessile, rhomic, lanceolate, or oblanceolate
      if($debug){
        print "[@terms] and [$t2] are in the same group [list]\n";
      }
      push(@terms, $t2);
      group(@terms);
    #}
    $sent =~ s#$list##;
    return $sent;
  }

  if($sent =~/$to/){
    print "\t".substr($sent, $-[0], $+[0]-$-[0])."\n" if $debug;

    my $t1 = trim($1);
    my $t2 = trim($2);
    #if($t1 =~/\w+/){
      push(@terms, $t1, $t2);
      group(@terms);
      if($debug){
        print "[$t1] and [$t2] are in the same group [to]\n";
     }
    #}
    $sent =~ s#$to##;
    return $sent;
  }

  if($sent =~/$tolist/){
    print "\t".substr($sent, $-[0], $+[0]-$-[0])."\n" if $debug;
    my $t1 = trim($1);
    my $t2 = trim($2);
    #if($t1 =~/\w+/){
      @terms = split(/\s*,\s*/,$t1);
      if($debug){
        print "[@terms] and [$t2] are in the same group [tolist]\n";
      }
      push(@terms, $t2);
      group(@terms);

    #}
    $sent =~ s#$tolist##;
    return $sent;
  }

  return "";
  #n-gram

  #simple

  #nested...

}
##########
sub trim{
	my $string = shift;
  $string =~ s/^\s*,//g;
  $string =~ s/[<>]//g;
  $string =~ s/\b($stop1)\b.*//g; #remove words after a stop words
  $string =~ s/NUM//g;
  $string =~ s/[a-z]+ly\b//g;
  $string =~ s/_ //g;
  $string =~ s/\s+/ /g;
	$string =~ s/^\s+//;
	$string =~ s/\s+$//;
	return $string;
}


############
sub group{
  my @terms = @_;
  my $select;
  my @tokens = ();
  foreach my $t (@terms){
  if ($t=~/\w+/){
    if($t=~/\s/){
      #print "====>skip multiwords: $t\n" if $debug;
      $t =~ s#^\s+##;
      $t =~ s#\s+$##;
      #$t =~ s#\W# #g;
      #$t =~ s#\bor\b# #g;
      #$t =~ s#\bto\b# #g;
      #$t =~ s#\band\b# #g;
      my @words = split(/\b(?:or|to)\b/, $t);
      foreach my $w (@words){
        if($w =~ /\w/){
         $w =~ s#^\s+##;
         $w =~ s#\s+$##;
         push(@tokens, $w) if $w !~ /\s/;
        }
      }
    }else{
      push(@tokens, $t);
    }
  }
  }
  @tokens = sort @tokens;
  my $length = @tokens;
  for(my $i = 0; $i < $length; $i++){
    for(my $j = $i; $j < $length; $j++){
      my $t1 = $tokens[$i];
      my $t2 = $tokens[$j];
      if($t1 ne $t2 && $t1 !~/\b($adverbs)\b/ && $t2 !~/\b($adverbs)\b/){
        print "Save [$t1] and [$t2] to database\n" if $debug;
        $select = $dbh->prepare('select links from linktable where id1="'.$t1.'" and id2="'.$t2.'"');
        $select->execute() or warn "$select->errstr\n";
        if($select->rows < 1){
          #$t1 may not match $terms[i]
          $select = $dbh->prepare('insert into linktable values ("'.$t1.'", "'.$t2.'", 1)');
          $select->execute() or warn "$select->errstr\n";
        }else{
          my $link = $select->fetchrow_array();
          $link++;
          #$t1 may not match $terms[i]
          $select = $dbh->prepare('update linktable set links = '.$link.' where id1="'.$t1.'" and id2="'.$t2.'"');
          $select->execute() or warn "$select->errstr\n";
        }
      }
    }
  }
}

##########################################################
# set the threshold on the number of links for pruning the graph (linktable)
sub setthresh{
  my $select = $dbh->prepare('select sum(links) from linktable group by links order by links');
  $select->execute() or warn "$select->errstr\n";
  my $total = $select->fetchrow_array();
  $select = $dbh->prepare('select links, count(links) from linktable group by links order by links');
  $select->execute() or warn "$select->errstr\n";
  my $half = 0;
  while(my ($links, $count) = $select->fetchrow_array()){
    $half += $count;
    if($half > $total*0.5){
      $linkthreshold = $links;
      print "threshold is readjusted to $linkthreshold\n";
      last;
    }
  }
}

##########################################################
#from linkage table find words that have very broad weak links, for example "somewhat"
#prune "long tails" ==>to be seen if the new classify function helps with this
sub prunefalselinks{

  my $select = $dbh->prepare('delete from linktable where links =1');
  $select->execute() or warn "$select->errstr\n";

}

##########################################################
#this algorithm attempts to avoid the risky link problem associated with classify-single-link
#starting with the most strongly connected nodes
# a node is put into a class only when the node have strong connection to more than half of the exsiting nodes in the class
# two classes are merged into one only when there are more than half of total possible links
sub classify{
  my ($select, $id1, $id2, $links, $select1,$classid, $word, %classes1, %classes2, $m);
  my ($flag1, $flag2, $k, $k1, $k2, @set1, @set2, $l1, $id, %classes, $joined, $mergable, @merges, @cs);
  my $nextcid = 1;
  my $seenwords = "";
  #starts with the strongest links
  $select = $dbh->prepare('select * from linktable order by links DESC');
  $select->execute() or die "$select->errstr\n";
  print "classifying words into semantic classes\n" if $debug;
  while(($id1, $id2, $links)=$select->fetchrow_array()){
     %classes1 = ();
     %classes2 = ();
     %classes = ();
     print "word pair: $id1, $id2 at weight $links:\n" if $debug;
     #classes of $id1
        $select1 = $dbh->prepare('select classid, word from semanticclass where classid in (select classid from semanticclass where word = "'.$id1.'")');
        $select1->execute() or die "$select1->errstr\n";
        $flag1 = 0;
        while(($classid, $word) = $select1->fetchrow_array()){
          $classes1{$classid}.=$word."#";
          print "$id1 's class $classid has word $word\n" if $debug;
          $flag1 .= $classid;
        }
      #classes of $id2
        $flag2 = 0;
        $select1 = $dbh->prepare('select classid, word from semanticclass where classid in (select classid from semanticclass where word = "'.$id2.'")');
        $select1->execute() or die "$select1->errstr\n";
        while(($classid, $word) = $select1->fetchrow_array()){
          $classes2{$classid}.=$word."#";
          print "$id2 's class $classid has word $word\n" if $debug;
          $flag2 .= $classid;
        }
      if($flag1 + $flag2 == 0 ){
        if($links > $linkthreshold){
          #new class
          print "Neither $id1 nor $id2 has a class. Assign new class id $nextcid for the pair at weight $links\n" if($debug);
          $seenwords = newClass($nextcid++, $id1, $id2, $links, $seenwords);
          }
      }elsif($flag1*$flag2 == 0){
          #add the new id in which class?
          $id = $flag1 == 0? $id1 : $id2;
          %classes =  $flag1 == 0? %classes2 : %classes1;
          $joined = "";
          foreach $k (keys(%classes)){
            if(isStrongConnection(1, $id, split(/#/, $classes{$k}))){
              #join this class $k
              print "$id joins a linked class $k\n" if($debug);
              $seenwords = joinClass($k, $id1, $id2, $links, $seenwords);
              $joined .= $k." ";
            }
          }
          if($joined eq "" && $links > $linkthreshold){
            #didn't join, then create new
            print "$id didn't join any class. Assign new class id $nextcid for the pair at weight $links\n" if($debug);
            $seenwords = newClass($nextcid++, $id1, $id2, $links, $seenwords);
          }elsif($joined =~/\w \w/){
            #new node joined in multiple classes, should these classes be merged?
            print "$id linked classes $joined. Merge those classes\n" if($debug);
            $seenwords = mergeClasses($nextcid++, $seenwords, split(/\s/, $joined));
          }
      }elsif($flag1 != $flag2){#two set of classes
         print "$id1 and $id2 have two sets of classes. Try merging \n" if($debug);
         $mergable = "";
         foreach $k1 (keys(%classes1)){
          @set1 = split(/#/, $classes1{$k1});
          $l1 = @set1;
          foreach $k2 (keys(%classes2)){
           if($k1 != $k2){
             @set2 = split(/#/, $classes2{$k2});
             print "test class $k1 and $k2 for strong connection\n" if $debug;
             push(@set1, @set2);
             if(isStrongConnection($l1, @set1)){
               print "connection is strong between class $k1 and $k2\n" if $debug;
               if($mergable =~/\b$k1\b/){      #record possible mergable pairs.
                  $mergable =~ s#$k1#$k1-$k2#;
               }elsif($mergable =~/\b$k2\b/){      #record possible mergable pairs.
                  $mergable =~ s#$k2#$k1-$k2#;
               }else{
                  $mergable .= $k1."-".$k2." ";
               }
             }
           }
          }
        }
        if($mergable eq "" && $links > $linkthreshold){
           #didn't merge
           print "Two sets did not merge. Assign new class $nextcid for the pair at weight $links \n" if($debug);
           $seenwords = newClass($nextcid++, $id1, $id2, $links, $seenwords);
        }else{
          #how to merge, knowing c11-c21 c12-c21...
          @merges = split(/\s/, $mergable);
          foreach $m  (@merges){
            @cs = split(/-/, $m);
            print "merge ".@cs." to new class $nextcid\n" if($debug);
            $seenwords = mergeClasses($nextcid++, $seenwords, @cs); #merge the classids in @cs to the next $nextcid
          }
        }
      }
     }
  }

 ##
 sub mergeClasses{
  my ($cid, $seenwords, @cids) = @_;
  my ($select2, $select3, $id, $ids);
  $ids = join(",", @cids);
  $select2 = $dbh->prepare('select distinct word, weight from semanticclass where classid in ('.$ids.')');
  $select2->execute() or die "$select2->errstr\n";
  while (my ($word, $weight) = $select2->fetchrow_array()){
    if($seenwords !~/\b$cid-$word\b/){  #why need this check?
      $select3 = $dbh->prepare('insert into semanticclass values ('.$cid.', "'.$word.'", '.$weight.')');
      $select3->execute() or die "$select3->errstr\n";
      $seenwords.=$cid."-".$word." ";
      #print "$word\n" if $debug;
    }
  }
  #remove old ids
  foreach $id (@cids){
    $select3 = $dbh->prepare('delete from semanticclass where classid ='.$id);
    $select3->execute() or die "$select3->errstr\n";
    #print "delete $id\n" if($debug);
  }
  return $seenwords;
 }
##
sub joinClass{
     my($cid, $id1, $id2, $links, $seenwords) = @_;
     my $select3;
     #save $id1, $id2 in this id (may be the common id for the two nodes, or may be the known id of one of the nodes.
     if($seenwords !~/\b$cid-$id1\b/){
            $select3 = $dbh->prepare('insert into semanticclass values ('.$cid.', "'.$id1.'", '.$links.')');
            $select3->execute() or warn "$select3->errstr\n";
            $seenwords.=$cid."-".$id1." ";
            #print "Seen $seenwords\n";
     }
     if($seenwords !~/\b$cid-$id2\b/){
            $select3 = $dbh->prepare('insert into semanticclass values ('.$cid.', "'.$id2.'", '.$links.')');
            $select3->execute() or warn "$select3->errstr\n";
            $seenwords.=$cid."-".$id2." ";
            #print "Seen $seenwords\n";
      }
     return $seenwords;
}
##
sub newClass{
  my($cid, $id1, $id2, $links, $seenwords) = @_;
  my $select3;
  if($seenwords !~/\b$cid-$id1\b/){
     $select3 = $dbh->prepare('insert into semanticclass values ('.$cid.', "'.$id1.'", '.$links.')');
     $select3->execute() or warn "$select3->errstr\n";
     $seenwords.=$cid."-".$id1." ";
  }
  if($seenwords !~/\b$cid-$id2\b/){
      $select3 = $dbh->prepare('insert into semanticclass values ('.$cid.', "'.$id2.'", '.$links.')');
      $select3->execute() or warn "$select3->errstr\n";
      $seenwords.=$cid."-".$id2." ";
  }
  return $seenwords;
}

###return true is the links between two sets are considered strong
sub isStrongConnection{
  my ($size1, @nodes) = @_;  #the size of set1
  my $size2 = $#nodes + 1 - $size1;
  my $totallinks = $size1 * $size2;
  print "total possible links: $size1 * $size2 = $totallinks\n" if $debug;
  my $stronglinks = stronglinks($size1, @nodes);
  return 1 if ($stronglinks-1)*1.0/$totallinks > 0.33;
  return 0 if ($stronglinks-1)*1.0/$totallinks <= 0.33;
}

sub stronglinks{
  my ($size1, @nodes) = @_;
  my ($i, $j, @set1, @set2, $select, $link, $count, $size, @set);
  ($size, @set) = removeshared($size1, @nodes);
  @set1 = splice(@set, 0, $size);
  @set2 = @set;
  $count = 0;
  for($i = 0; $i<@set1; $i++){
    for($j = 0; $j<@set2; $j++){
        $link = 0;
        $select = $dbh->prepare('select links from linktable where (id1="'.$set1[$i].'" and id2="'.$set2[$j].'") or (id2="'.$set1[$i].'" and id1="'.$set2[$j].'")');
        $select->execute() or die "$select->errstr\n";
        $link = $select->fetchrow_array();
        $count++ if($link > $linkthreshold);
    }
  }
  print "Strong link count is $count\n" if $debug;
  return $count;
}

sub removeshared{
  my($size, @set) = @_;
  my(%sorter,$i, @set1, @set2);
  for ($i = 0; $i < @set; $i++){
    if($i < $size){
      $sorter{$set[$i]}.=1;
    }else{
      $sorter{$set[$i]}.=2;
    }
  }
  foreach $i (keys(%sorter)){
    if($sorter{$i} == 1){
      push(@set1, $i);
    }elsif($sorter{$i} == 2){
      push(@set2, $i);
    }
  }
  $size = @set1;
  push(@set1, @set2);
  return ($size, @set1);
}
##########
#this algorithm classifies nodes (id1, id2) by looking at the link between the two.
#one single frequent link is what it takes to merge two classes.
#this is too risky for a large collection (e.g. FNA with 2000+ records), even though it worked well with (FNA 200 records)
sub classifySingleLink{
#based on linktable
  my ($select, $select1, $select2, $select3, @cids, $id, $cid, $rows);
  my $nextcid = 0;
  my $seenwords = "";
  print "use threshold $linkthreshold to classify\n";
  $select = $dbh->prepare('select * from linktable order by links DESC');
  $select->execute() or die "$select->errstr\n";
  while(my($id1, $id2, $links)=$select->fetchrow_array()){
     if($links > $linkthreshold){ # if the link frequency is above the threshold,
        $select1 = $dbh->prepare('select distinct classid from semanticclass where word = "'.$id1.'" or word = "'.$id2.'"');
        $select1->execute() or die "$select1->errstr\n";
        @cids = ();
        $rows = $select1->rows;
        if($rows > 1){
          #merge the two groups linked by this link
          for(my $i = 0; $i < $rows; $i++){
             $id = $select1->fetchrow_array();
             push(@cids, $id);
             print "to be deleted $id\n" if($debug);
          }
          if($debug){
          print "merge $id1, $id2 groups (links $links) to new class $nextcid\n";
          }
          $select2 = $dbh->prepare('select distinct word, weight from semanticclass where classid in (select classid from semanticclass where word = "'.$id1.'" or word = "'.$id2.'")');
          $select2->execute() or die "$select2->errstr\n";
          print "merge word: \n" if $debug;
          while (my ($word, $weight) = $select2->fetchrow_array()){
             if($seenwords !~/\b$nextcid-$word\b/){  #why need this check?
             $select3 = $dbh->prepare('insert into semanticclass values ('.$nextcid.', "'.$word.'", '.$weight.')');
             $select3->execute() or die "$select3->errstr\n";
             $seenwords.=$nextcid."-".$word." ";
             print "$word\n" if $debug;
             #print "Seen $seenwords\n";
             }
          }
          #next id++
          $nextcid++;
          #remove old ids
          foreach $cid (@cids){
              $select3 = $dbh->prepare('delete from semanticclass where classid ='.$cid);
              $select3->execute() or die "$select3->errstr\n";
              print "delete $cid\n" if($debug);
           }
        }elsif($rows == 1){
          #save $id1, $id2 in this id (may be the common id for the two nodes, or may be the known id of one of the nodes.
          if($debug){
          print "save\n";
          }
          $cid = $select1->fetchrow_array();
          if($seenwords !~/\b$cid-$id1\b/){
            $select3 = $dbh->prepare('insert into semanticclass values ('.$cid.', "'.$id1.'", '.$links.')');
            $select3->execute() or warn "$select3->errstr\n";
            $seenwords.=$cid."-".$id1." ";
            #print "Seen $seenwords\n";
          }
          if($seenwords !~/\b$cid-$id2\b/){
            $select3 = $dbh->prepare('insert into semanticclass values ('.$cid.', "'.$id2.'", '.$links.')');
            $select3->execute() or warn "$select3->errstr\n";
            $seenwords.=$cid."-".$id2." ";
            #print "Seen $seenwords\n";
          }
        }else{
          #the classid of both nodes are unknown, create a new id.
          if($debug){
          print "new class\n";
          }
          $cid = $nextcid++;
          if($seenwords !~/\b$cid-$id1\b/){
            $select3 = $dbh->prepare('insert into semanticclass values ('.$cid.', "'.$id1.'", '.$links.')');
            $select3->execute() or warn "$select3->errstr\n";
            $seenwords.=$cid."-".$id1." ";
            #print "Seen $seenwords\n";
          }
          if($seenwords !~/\b$cid-$id2\b/){
            $select3 = $dbh->prepare('insert into semanticclass values ('.$cid.', "'.$id2.'", '.$links.')');
            $select3->execute() or warn "$select3->errstr\n";
            $seenwords.=$cid."-".$id2." ";
            #print "Seen $seenwords\n";
          }
        }
     }
  }
}


###########################################################

#groups: term => classid1 classid2 ...
#classid: classid => count
#sub group{
#  my @terms = @_;
# my @splited = ();
# my %classid = {};
#  my ($ids, @ids, $select);
#
#  my $string = join(":",@terms);
# $string =~ s/^://;
# if($string =~ /absent/ && $string !~ /present/){
#   $string =~ s/absent//g;
# }elsif($string !~ /absent/ && $string =~ /present/){
#   $string =~ s/present//g;
#  }
#  $string =~ s/\s+/ /g;
# @terms = split(/:+/, $string);
#
# foreach my $term (@terms){
#     push(@splited, split(/\s*\bor\b\s*/, $term));   #sames good. add to and -, makes it bad.
#  }
#  @terms = @splited;
#  #count the different ids for each term
#  foreach my $term (@terms){
#       $select = $dbh->prepare('select distinct classid from semanticclass where word = "'.$term.'"');
#       $select->execute() or warn "$select->errstr\n";
#        while(my $cid = $select->fetchrow_array()){
#            $classid{$cid}++;
#       }
#  }
#  #find the most freq classid for this group of terms
#  my $max = 0;
#  my $freqid = $nextid;
#  foreach my $id (keys(%classid)){
#    if($classid{$id} > $max){
#      $freqid = $id;
#      $max = $classid{$id};
#    }
#  }
#  $nextid++ if $freqid == $nextid;
#
#  #set all terms in this group to the freqid
#  foreach my $term (@terms){
#    if($term =~ /\w+/){
#      if($debug){
#        print "Add $term\t$freqid\n";
#      }
#      $select = $dbh->prepare('select * from semanticclass where classid ='.$freqid.' and word ="'.$term.'"');
#      $select->execute() or warn "$select->errstr\n";
#      if($select->rows < 1){
#        $select = $dbh->prepare('insert into semanticclass values ('.$freqid.',"'.$term.'")');
#        $select->execute() or warn "$select->errstr\n";
#      }
#      #$groups{$term} .= $freqid." ";
#    }
# }
#}




















