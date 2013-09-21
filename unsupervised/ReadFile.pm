package ReadFile;
#use encoding "iso-8859-1"; #latin1
#use encoding "cp1252";

sub readfile{
	my $file = shift;
	my $content = "";
	open(F, "$file") || die "$!:$file\n";
	while($line =<F>){
		$line =~ s#\r|\n# #g;
		$content .= $line;
	}		 
	$content =~ s#\s+# #g;
	return $content;
}

1;