#!C:\Perl\bin\perl
package InitialMarkup;
use SentenceSpliter;
use ReadFile;
use NounHeuristics;
#this program markup a collection of plain text files in xml format, using
#the first n words (not seperated by a punct. mark) in a sentence (terminated by a period or a semi-colon).

sub tentativemarkup{
#$dir = "U:\\Research\\Data-Algea\\algea-data\\test\\";
#$outdir = "U:\\Research\\Data-Algea\\algea-data\\test-inimark\\";
my ($dir, $outdir) = @_;
my @ttmarked = ();
$N = 2;
$ROOT = "description";

if(! -e $outdir){
  system("mkdir", "$outdir");	
}
opendir(IN, "$dir") || die "$!:$dir\n";
while(defined ($file = readdir(IN))){
  if($file !~ /\w+/){next;}
  $content = ReadFile::readfile("$dir$file");
  $content = strip($content); #remove any html tags
  $content =~ s#(^\s+|\s+$)##; #remove leading and ending space
  @segments = SentenceSpliter::get_sentences($content);#segment content at real period and semi-colon
  $marked = initialmarkup(@segments);
  push(@ttmarked, $marked);
  open(OUT, ">$outdir$file") || die "$!:$outdir$file\n";
  print OUT "<".$ROOT.">".$marked."</".$ROOT.">";
  }
  return @ttmarked;
}

sub strip{
	my $content = shift;
	$content =~ s#<(([^ >]|\n)*)># #g; #too simple?
	$content =~ s#<\?[^>]*\?># #; #<? ... ?>
	$content =~ s#&[^ ]{2,5};# #g; #remove &nbsp;
	$content =~ s#\s+# #g;
	return $content;
}
#deal with ;
sub initialmarkup{
  my @content = @_;
  my $marked = "";
  my $sentence = "";
  my @clauses = ();
  my $tag = "";
  my $compoundtag = "";
  my $taggedsentence = "";
  foreach $sentence (@content){
  	$sentence =~ s#(^\s+|\s+$)##;
  	@clauses = split(/;\s*/, $sentence);
	$compoundtag = "";
	$taggedsentence = "";
  	foreach $clause (@clauses){
	  if($clause =~ /\w/){
		if($clause ne $clauses[@clauses-1]){
			$clause = $clause.";";
		}
		($tag, $tagged)= tagsentence($clause);
		$compoundtag .=$tag."-";
		$taggedsentence .=$tagged;
	  }
	}
	chop($compoundtag);
	if($compoundtag eq $tag){
	   $marked .= $taggedsentence;
	}else{
	   $marked .= "<".$compoundtag.">".$taggedsentence."</".$compoundtag.">";
	}
  }
  return $marked;
}

sub tagsentence{
	my $sentence = shift;
	#take the first N words
	my @words = split(/\s+/, $sentence);
	#skip numbers
	my $count  = 0;
	my $tag = "";
	foreach $w (@words){
	   if($count == $N){last;}
	   if($w !~ /\d/ && $w !~ /$NounHeuristics::STOP/){
	   		 if($w =~ /\p{Punct}$/){ #stop taking words at a puncturation mark
			   chop($w);
			   $tag .= $w."_";
			   $count = $N;
			 }else{
	   		   $tag .= $w."_";
			   $count++;
			}
	   }
	}
	chop($tag); #remove the last char.
	$tag = lc $tag;
	return ($tag, "<".$tag.">".$sentence."</".$tag.">");  
}

1;