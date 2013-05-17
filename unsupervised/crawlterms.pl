# this program extracts plant characteristics terminology from
# http://herbaria.plants.ox.ac.uk/vfh/image/index.php?glossary=show&alpha=A
# where alpha=A-Z.
# and save the term, defintion, and parent term in the table terminology of the database characterTerms
#Oxford Plant Systematics
#

use strict;
use DBI;
use IO::Socket;
use URI;

my $host = "localhost";
my $user = "termsuser";
my $password = "termspassword";
my $dbh = DBI->connect("DBI:mysql:host=$host", $user, $password)
or die DBI->errstr."\n";

my $query = $dbh->prepare('use characterTerms')
or die $dbh->errstr."\n";
$query->execute() or die $query->errstr."\n";

$query = $dbh->prepare('delete from terminology');
if(!$query->execute()){
$query = $dbh->prepare('create table terminology (term varchar(100), definition varchar(500), parentterm varchar(100), primary key(term, parentterm)) ');
$query->execute();
}

my @alpha = ("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z");
my $base = "http://herbaria.plants.ox.ac.uk/vfh/image/index.php?glossary=show&alpha=";

my @links = ();
foreach (@alpha) {
        my $link = $base.$_;
        push(@links, $link);
}
my $alpha = 0;


foreach my $uri (@links) {
        my $domain = URI->new($uri)->authority;
        $uri =~ s#$domain##;
        $uri =~ s#http:///##;
        my $socket = IO::Socket::INET->new(PeerAddr => $domain,
                                PeerPort => 80,
                                Proto => 'tcp',
                                Type => SOCK_STREAM)
        or die "Couldn't connect";
        print $socket "GET /$uri HTTP/1.0\n\n";
        my $text = "";
        while (defined(my $line = <$socket>)) {
                $text .= $line;
                #print $line;
        }
        $text =~ s#[\n\r]# #g;
        $text =~ s#\s+# #g;
        extract($text, $alpha[$alpha++]);
        close($socket);
}

# a HTML record
#<table width="695" border="0" class="gloss_2" bgcolor="#E3E3BF">
#            <tr><td valign="top" width="250"><b><a name="margin_of_leaf_blade_(3d)" class="glossary">Margin of leaf blade (3D) :</a>
#
#                                    <a name="margin_of_leaf_blade_(3d)">&nbsp;</a>
#                </b></td>
#                <td valign="top">
#                     -
#                </td>
#            </tr>
#                        <tr><td width="250" valign="top"><span style="margin-left:10px;">Parent Term:</span></td><td valign="top"><a href="?glossary=show&alpha=L#leaf_blade_margin">Leaf_blade_margin</a></td></tr>
#                                                                        <tr><td width="250" valign="top"><span style="margin-left:10px;">Child Terms:<span></td><td valign="top">
#
#                                    <a href="?glossary=show&alpha=R#revolute">Revolute</a><br/>
#                                    <a href="?glossary=show&alpha=U#undulate">Undulate</a><br/>
#                                    <a href="?glossary=show&alpha=I#involute">Involute</a><br/>
#                                    <a href="?glossary=show&alpha=R#recurved">Recurved</a><br/>
#                                    <a href="?glossary=show&alpha=R#recurved_margin">Recurved_margin</a><br/>
#                                    <a href="?glossary=show&alpha=M#margin_recurved">Margin_recurved</a><br/>
#
#                                </td></tr>
#                                    <tr><td width="250" valign="top"><span style="margin-left:10px;">Difficulty Level:</span></td>
#                <td valign="top">
#                                            <img src="../img/diff.gif" onmouseover="this.T_WIDTH=500;return escape('<h3>Difficult</h3><table> <tr valign=\'top\'> <td><b>Difficulty, in terms of cost/benefit of use in field guide</b></td> <td><b>Examples</b></td> <td><b>The desirability of using these words in your field guide</b></td> </tr> <tr valign=\'top\'> <td><img src=\'../img/diff.gif\'/><img src=\'../img/diff.gif\'/><img src=\'../img/diff.gif\'/> - Technical, concise, slightly difficult</td> <td>Reniform, secund, serrulate, Corner’s model, mucronulate</td> <td>Unless your field guide is for boffins e.g. a ‘pragmatic’ flora used only by other scientists - or needs to be short on illustrations for weight or expense reasons, you should rephrase these words, and there is generally a good substitute.</td> </tr> </table> ')"/>
#                                            <img src="../img/diff.gif" onmouseover="this.T_WIDTH=500;return escape('<h3>Difficult</h3><table> <tr valign=\'top\'> <td><b>Difficulty, in terms of cost/benefit of use in field guide</b></td> <td><b>Examples</b></td> <td><b>The desirability of using these words in your field guide</b></td> </tr> <tr valign=\'top\'> <td><img src=\'../img/diff.gif\'/><img src=\'../img/diff.gif\'/><img src=\'../img/diff.gif\'/> - Technical, concise, slightly difficult</td> <td>Reniform, secund, serrulate, Corner’s model, mucronulate</td> <td>Unless your field guide is for boffins e.g. a ‘pragmatic’ flora used only by other scientists - or needs to be short on illustrations for weight or expense reasons, you should rephrase these words, and there is generally a good substitute.</td> </tr> </table> ')"/>
#                                            <img src="../img/diff.gif" onmouseover="this.T_WIDTH=500;return escape('<h3>Difficult</h3><table> <tr valign=\'top\'> <td><b>Difficulty, in terms of cost/benefit of use in field guide</b></td> <td><b>Examples</b></td> <td><b>The desirability of using these words in your field guide</b></td> </tr> <tr valign=\'top\'> <td><img src=\'../img/diff.gif\'/><img src=\'../img/diff.gif\'/><img src=\'../img/diff.gif\'/> - Technical, concise, slightly difficult</td> <td>Reniform, secund, serrulate, Corner’s model, mucronulate</td> <td>Unless your field guide is for boffins e.g. a ‘pragmatic’ flora used only by other scientists - or needs to be short on illustrations for weight or expense reasons, you should rephrase these words, and there is generally a good substitute.</td> </tr> </table> ')"/>
#                                    </td></tr>
#
#                                    <tr><td width="250" colspan="2"><span style="margin-left:10px;"><a href="?character_image=7626&lasta=12">Show examples »</a></span></td></tr>

#                        <tr><td colspan="2">&nbsp;</td></tr>
#       </table>


#extraction cues:

#<h3><a name="M">M</a></h3>                                                                      ===>start the body

#<table width="695" border="0"                                                                   ===>start a record


#<a name="margin_of_leaf_blade_(3d)" class="glossary">Margin of leaf blade (3D) :</a>            ===>a term

#<td valign="top">
#                     -                                                                          ===>definition: plain text not starting with a <
#</td>

#Parent Term:</span></td><td valign="top"><a href="?glossary=show&alpha=L#leaf_blade_margin">Leaf_blade_margin</a>      ===>text follows the text "Parent Term"

#Child Terms:<span></td><td valign="top">
#                                    <a href="?glossary=show&alpha=R#revolute">Revolute</a><br/>
#                                    <a href="?glossary=show&alpha=U#undulate">Undulate</a><br/>
#                                    <a href="?glossary=show&alpha=I#involute">Involute</a><br/>                        ====>child terms may be a list
#                                    <a href="?glossary=show&alpha=R#recurved">Recurved</a><br/>
#                                    <a href="?glossary=show&alpha=R#recurved_margin">Recurved_margin</a><br/>
#                                    <a href="?glossary=show&alpha=M#margin_recurved">Margin_recurved</a><br/>
#</td></tr>

sub extract{
  my ($html, $alpha) = @_;
  my $start = "<h3><a name=\"".$alpha."\">".$alpha."</a></h3>";
  my $rec = "<table width=\"695\" border=\"0\"";
  if($html =~/.*?$start(.*)/){
     my $records = $1;
     my @records = split(/$rec/, $records);
     foreach my $record (@records){
        my ($term, $pterm, $def, $synonym, $plural, $cterms, @cterms);
        $record = lc $record;
        if($record =~/<a name=".*?" class="glossary">(.*?) :<\/a>(.*)/){       #term
            $term = $1;
            $record = $2;
            if($record =~/<td valign="top">\s*([^<].*?)\s*<\/td>(.*)/){ #defination
               $def = $1;
               $record = $2;
               if($record =~/parent term:(.*?)<\/td><\/tr>(.*)/){  #parent term
                   $pterm = $1;
                   $record = $2;
                   $pterm =~ s#<.*?>##g;
               }
               if($record =~/plural:(.*?)<\/td><\/tr>(.*)/){  #parent term
                   $plural= $1;
                   $record = $2;
                   $plural =~ s#<.*?>##g;
               }
               if($record =~/synonyms:(.*?)<\/td><\/tr>(.*)/){  #parent term
                   $synonym= $1;
                   $record = $2;
                   $synonym =~ s#<.*?>##g;
               }
               if($record =~/child terms:(.*?)<\/td><\/tr>(.*)/){ #child terms
                   $cterms = $1;
                   $record = $2;
                   $cterms =~ s/<.*?>/#/g;
                   @cterms = split(/[# ]+/, $cterms);
               }
            }
        }
        $term = normalize($term);
        $pterm = normalize($pterm);
        $synonym = normalize($synonym);
        $plural = normalize($plural);

        $def =~s#<.*?># #g;
        $def =~s#"#'#g;
        #saveRecords($term, $def, $pterm, @cterms);
        saveRecords($term, $def, $pterm) if $term ne $pterm;
        saveMRecords($synonym, $def, $pterm, @cterms) if ($synonym =~/\w+/ && $synonym ne "all types");
        saveMRecords($plural, $def, $pterm, @cterms) if $plural =~/\w+/;
        print "extracted the following\n term = [$term];\n defination = [$def];\n parent term = [$pterm];\n";
     }
  }else{
    print err "Can not find the start of the record\n";
  }
}

sub normalize{
  my $term = shift;
  $term =~ s#_# #g;
  $term =~ s#<.*?># #g;
  $term =~ s#\s+# #g;
  $term =~ s#^\s+##g;
  $term =~ s#\s+$##g;
  return $term;
}


sub saveMRecords{
 my($term, $def, $pterm, @cterms) = @_;
 saveRecords($term, $def, $pterm) if $term ne $pterm;
 foreach my $t (@cterms){
    $t = normalize($t);
    saveRecords($t, "", $term);
 }
}

sub saveRecords{
  my($term, $def, $pterm) = @_;
  my $check;
  if($term =~/\w+/ && $pterm=~/\w+/){
    $term = fixexceptions($term);
    $pterm = fixexceptions($pterm);

    if($term eq "flower(s)"){
      save("flower", $def, $pterm);
      save("flowers", $def, $pterm);
      return;
    }elsif($pterm eq "flower(s)"){
      save($term, $def, "flower");
      save($term, $def, "flowers");
      return;
    }
    if($term eq "liane (=liana)"){
      save("liane", $def, $pterm);
      save("liana", $def, $pterm);
      return;
    }elsif($pterm eq "liane (=liana)"){
      save($term, $def, "liane");
      save($term, $def, "liana");
      return;
    }
    if($term eq "lobe (n) lobed (adj)" || $pterm eq "lobe (n) lobed (adj)"){
      return;
    }
    if($term eq "stoma (plural=stomata)"){
      save("stoma", $def, $pterm);
      save("stomata", $def, $pterm);
      return;
    }elsif($pterm eq "stoma (plural=stomata)"){
      save($term, $def, "stoma");
      save($term, $def, "stomata");
      return;
    }
    if($term eq "syconium (fig)"){
      save("syconium", $def, $pterm);
      save("fig", $def, $pterm);
      return;
    }elsif($pterm eq "syconium (fig)"){
      save($term, $def, "syconium");
      save($term, $def, "fig");
      return;
    }
    if($term eq "vein(s)"){
      save("vein", $def, $pterm);
      save("veins", $def, $pterm);
      return;
    }elsif($pterm eq "vein(s)"){
      save($term, $def, "vein");
      save($term, $def, "veins");
      return;
    }
    $term =~ s#\(.*?\)# #g;
    $pterm =~ s#\(.*?\)# #g;
    #assume exceptions only occur in one term, either term or pterm, but not both.

    if($term eq "flower/fruit position"){
      save("flower position", $def, $pterm);
      save ("fruit position", $def, $pterm);
      return;
    }elsif($pterm eq "flower/fruit position"){
      save($term,$def, "flower position");
      save ($term,$def, "fruit position");
      return;
    }

    if($term eq "vein prominence/visibility"){
      save("vein prominence", $def, $pterm);
      save("vein visibility", $def, $pterm);
      return;
    }elsif($pterm eq "vein prominence/visibility"){
      save($term, $def, "vein prominence");
      save($term, $def, "vein visibility");
      return;
    }


    
    if($term =~/,/ || $term=~/ or /){
      my @terms = split(/(,| or )/, $term);
      foreach my $tm (@terms){
        $tm =~s#^\s+##;
        $tm =~s#\s+$##;
        $tm = fixexceptions($tm);
        save($tm, $def, $pterm);
      }return;
    }elsif($pterm =~/,/ || $pterm=~/ or /){
      my @pterms = split(/(,| or )/, $pterm);
      foreach my $ptm (@pterms){
        $ptm =~s#^\s+##;
        $ptm =~s#\s+$##;
        $ptm = fixexceptions($ptm);
        save($term, $def, $ptm);
      }return;
    }
    save($term, $def, $pterm);
  }#end if
}

sub save{
   my ($term, $def, $pterm) = @_;
   my $check = $dbh->prepare('insert into terminology values ("'.$term.'", "'.$def.'", "'.$pterm.'")');
   $check->execute();
}

sub fixexceptions{
  my $term = shift;
  $term = "habit type" if $term eq "habit";
  $term =~ s# habit$##g; #monocot habit, litter-bin habit
  $term =~ s# etc$##g; #spines pricles etc
  $term = "compound leaf" if $term eq "parts of compound lvs";
  $term = "plant" if $term eq "plant parts";
  $term =~ s# of .*$##g; #apex of leaf blade
  $term =~ s# types# type#g; #fruit types
  $term =~ s# parts# part#g; #fruit types
  $term =~ s#colour#color#g;
  $term = "outer bark" if $term eq "bark, outer";
  return $term;
}
