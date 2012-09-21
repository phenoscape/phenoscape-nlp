#This script is used to test the subroutines that extract the synonyms 
#for some particular word 
#from dictionary.com. This module will us HTML::Tree::Scanning and 
#HTML::TreeBuilder moduels. You can find documentation on these two modules
#here:
#http://search.cpan.org/~jfearn/HTML-Tree-4.2/lib/HTML/Tree/Scanning.pod
#http://search.cpan.org/~jfearn/HTML-Tree-4.2/lib/HTML/TreeBuilder.pm

use HTML::TreeBuilder;
use HTML::Tree;
use LWP::Simple;

print getSection("good", "Synonyms");
#This subroutine takes a word and the name of the section which
#we want to extract content from. Then it strips all the punctuations
#and returns the string as result.  
sub getSection{
	my $word = shift;
	my $sec = shift;
	$sec .= ":";
	my $result="";
	my $url = "http://thesaurus.com/browse/$word";
	my $content = get $url;
	die "Couldn't get $url" unless defined $content;
	
	my $tree = HTML::TreeBuilder->new_from_content($content);
	die "Couldn't build the tree" unless defined $tree;
	
	my @matches = $tree->look_down('_tag','td',
		sub {
			$_[0]->as_text eq $word or 
			$_[0]->look_down('id','Headserp');
			#$_[0]->parent->as_text =~ m/Main Entry:/i;
		}
	);
	print scalar(@matches)."\n";
	foreach (@matches){
		my $table=$_->parent->parent;
		my $synlabel = $table->look_down(
			sub{
				$_[0]->as_text eq $sec;#find content of specified section 
			}
		);
		
		if (defined $synlabel){		
			my $synstring = $synlabel->parent->as_text;
			$synstring =~ s/$sec//;
			$synstring =~ s/[,;*]//g;
			
			$result .= ($synstring." ");
			
			#print $synstring;
			#print "\n";
			#TODO
		}else{#In dictionary.com, there could be sections
			#explaining the word, but having no relation with
			#synonyms		
			#TODO
		}
	}
	
	$tree->delete;
	return $result;
}