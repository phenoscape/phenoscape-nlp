use strict;

#my $word =  "leaves";

#my $pos = getnumber($word);

#print $pos;

my $sent = "hong's mom";
$sent =~ s#'#\\'#g;
print $sent; #escape

#$dbh->prepare("select * from anytable where avalue in ('a', 'b', 'c')");


