my $pato="C:\\Users\\updates\\CharaParserTest\\Ontologies\\pato.obo";
my $out="C:\\Users\\updates\\CharaParserTest\\Ontologies\\pato.attributes";
open(F, $pato) || die "$!:$pato\n";
open(OUT, ">$out") || die "$!:$out\n";
my @content = <F>;
my $text = join("", @content);
my @segs = split(/\[Term\]/, $text);
foreach (@segs){
	if(/subset: attribute_slim/){
		print OUT $_;
	}
	
}