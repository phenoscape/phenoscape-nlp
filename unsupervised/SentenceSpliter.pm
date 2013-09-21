package SentenceSpliter;
require 5.005_03;
use strict;
#use encoding "iso-8859-1"; #latin1
#use encoding "cp1252";
use POSIX qw(locale_h);
#==============================================================================
#
# Modules
#
#==============================================================================
require Exporter;

#==============================================================================
#
# Public globals
#
#==============================================================================
use vars qw/$VERSION @ISA @EXPORT_OK $EOS $LOC $AP $P $PAP $SAP @ABBREVIATIONS/;
use Carp qw/cluck/;

$VERSION = '0.25';

# LC_CTYPE now in locale "French, Canada, codeset ISO 8859-1"
#comment out 2lines by hong, 10/27/08
$LOC=setlocale(LC_CTYPE, "fr_CA.ISO8859-1"); 
use locale;

@ISA = qw( Exporter );
@EXPORT_OK = qw( get_sentences 
		add_acronyms get_acronyms set_acronyms
		get_EOS set_EOS);

$EOS="\001";
$P = q/[\.!?;:]/;			## PUNCTUATION
$AP = q/(?:'|"|�|\)|\]|\})?/;	## AFTER PUNCTUATION
$PAP = $P.$AP;
$SAP = q/(?:\.|'|"|�|\)|\]|\})?/;

my @PEOPLE = ( 'jr', 'mr', 'mrs', 'ms', 'dr', 'prof', 'sr', "sens?", "reps?", 'gov',
		"attys?", 'supt',  'det', 'rev' );


my @ARMY = ( 'col','gen', 'lt', 'cmdr', 'adm', 'capt', 'sgt', 'cpl', 'maj' );
my @INSTITUTES = ( 'dept', 'univ', 'assn', 'bros' );
my @COMPANIES = ( 'inc', 'ltd', 'co', 'corp' );
my @PLACES = ( 'arc', 'al', 'ave', "blv?d", 'cl', 'ct', 'cres', 'dr', "expy?",
		'dist', 'mt', 'ft',
		"fw?y", "hwa?y", 'la', "pde?", 'pl', 'plz', 'rd', 'st', 'tce',
		'Ala' , 'Ariz', 'Ark', 'Cal', 'Calif', 'Col', 'Colo', 'Conn',
		'Del', 'Fed' , 'Fla', 'Ga', 'Ida', 'Id', 'Ill', 'Ind', 'Ia',
		'Kan', 'Kans', 'Ken', 'Ky' , 'La', 'Me', 'Md', 'Is', 'Mass', 
		'Mich', 'Minn', 'Miss', 'Mo', 'Mont', 'Neb', 'Nebr' , 'Nev',
		'Mex', 'Okla', 'Ok', 'Ore', 'Penna', 'Penn', 'Pa'  , 'Dak',
		'Tenn', 'Tex', 'Ut', 'Vt', 'Va', 'Wash', 'Wis', 'Wisc', 'Wy',
		'Wyo', 'USAFA', 'Alta' , 'Man', 'Ont', 'Qu�', 'Sask', 'Yuk');
my @MONTHS = ('jan','feb','mar','apr','may','jun','jul','aug','sep','oct','nov','dec','sept');
my @MISC = ( 'vs', 'etc', 'no', 'esp');
my @BOT1 = ('diam', 'sq','Rottb');
my @BOT2 = ('ca', 'fl', 'Fl','Fr','fr', 'var'); # can not start a new sentence right after the abbrev.
@ABBREVIATIONS = (@PEOPLE, @ARMY, @INSTITUTES, @COMPANIES, @PLACES, @MONTHS, @MISC, @BOT1); 


#==============================================================================
#
# Public methods
#
#==============================================================================

#------------------------------------------------------------------------------
# get_sentences - takes text input and splits it into sentences.
# A regular expression cuts viciously the text into sentences, 
# and then a list of rules (some of them consist of a list of abbreviations)
# is applied on the marked text in order to fix end-of-sentence markings on 
# places which are not indeed end-of-sentence.
#------------------------------------------------------------------------------
sub get_sentences {
	my ($text)=@_;
	return [] unless defined $text;
	my $marked_text = first_sentence_breaking($text);
	my $fixed_marked_text = remove_false_end_of_sentence($marked_text);
	$fixed_marked_text = split_unsplit_stuff($fixed_marked_text);
	$fixed_marked_text =~ s#($EOS\s*)+#$EOS#g;
	my @sentences = split(/$EOS/,$fixed_marked_text);
	#my $cleaned_sentences = clean_sentences(\@sentences);
	#return $cleaned_sentences;
	return @sentences;
}

#------------------------------------------------------------------------------
# add_acronyms - user can add a list of acronyms/abbreviations.
#------------------------------------------------------------------------------
sub add_acronyms {
	push @ABBREVIATIONS, @_;
}

#------------------------------------------------------------------------------
# get_acronyms - get defined list of acronyms.
#------------------------------------------------------------------------------
sub get_acronyms {
	return @ABBREVIATIONS;
}

#------------------------------------------------------------------------------
# set_acronyms - run over the predefined acronyms list with your own list.
#------------------------------------------------------------------------------
sub set_acronyms {
	@ABBREVIATIONS=@_;
}

#------------------------------------------------------------------------------
# get_EOS - get the value of the $EOS (end-of-sentence mark).
#------------------------------------------------------------------------------
sub get_EOS {
	return $EOS;
}

#------------------------------------------------------------------------------
# set_EOS - set the value of the $EOS (end-of-sentence mark).
#------------------------------------------------------------------------------
sub set_EOS {
	my ($new_EOS) = @_;
	if (not defined $new_EOS) {
		cluck "Won't set \$EOS to undefined value!\n";
		return $EOS;
	}
	return $EOS = $new_EOS;
}

#------------------------------------------------------------------------------
# set_locale - set the value of the locale.
#
#		Revceives language locale in the form
#			language.country.character-set
#		for example:
#				"fr_CA.ISO8859-1"
#		for Canadian French using character set ISO8859-1.
#
#		Returns a reference to a hash containing the current locale 
#		formatting values.
#		Returns undef if got undef.
#
#
#               The following will set the LC_COLLATE behaviour to
#               Argentinian Spanish. NOTE: The naming and avail�
#               ability of locales depends on your operating sys�
#               tem. Please consult the perllocale manpage for how
#               to find out which locales are available in your
#               system.
#
#                       $loc = set_locale( "es_AR.ISO8859-1" );
#
#
#		This actually does this:
#
#			$loc = setlocale( LC_ALL, "es_AR.ISO8859-1" );
#------------------------------------------------------------------------------
sub set_locale {
	my ($new_locale) = @_;
	if (not defined $new_locale) {
		cluck "Won't set locale to undefined value!\n";
		return undef;
	}
	$LOC = setlocale(LC_CTYPE, $new_locale); 
	return $LOC;
}


#==============================================================================
#
# Private methods
#
#==============================================================================

## Please email me any suggestions for optimizing these RegExps.
sub remove_false_end_of_sentence {
	my ($marked_segment) = @_;
##	## don't do u.s.a.
##	$marked_segment=~s/(\.\w$PAP)$EOS/$1/sg; 
	$marked_segment=~s/([^-\w]\w$PAP\s)$EOS/$1/sg;
	$marked_segment=~s/([^-\w]\w$P)$EOS/$1/sg;   #" 0.$EOS5" =>0.5

	# don't plit after a white-space followed by a single letter followed
	# by a dot followed by another whitespace.
  #$marked_segment=~s/(\s\w\.\s+)$EOS/$1/sg;
  $marked_segment=~s/(\s\w\.\s+)$EOS([[:lower:]])/$1$2/sg;
  #keep $EOS if "5 m. $EOS Root";

	# fix: bla bla... yada yada
	$marked_segment=~s/(\.\.\. )$EOS([[:lower:]])/$1$2/sg; 
	# fix "." "?" "!"
	$marked_segment=~s/(['"]$P['"]\s+)$EOS/$1/sg;
	## fix where abbreviations exist
	foreach (@ABBREVIATIONS) {
     $marked_segment=~s/(\b$_$SAP\s)$EOS\s*(?!([A-Z]|\d+-+[a-zA-Z]))/$1/sg;  #replace when not matach [A-Z] or 1-flowered
  }

	foreach (@BOT2) {
     $marked_segment=~s/(\b$_$SAP\s)$EOS/$1/sg;  
  }
	# don't break after quote unless its a capital letter.
	$marked_segment=~s/(["']\s*)$EOS(\s*[[:lower:]])/$1$2/sg;

	# don't break: text . . some more text.
	$marked_segment=~s/(\s\.\s)$EOS(\s*)/$1$2/sg;

	$marked_segment=~s/(\s$PAP\s)$EOS/$1/sg;
	return $marked_segment;
}

sub split_unsplit_stuff {
	my ($text) = @_;

	$text=~s/(\D\d+)($P)(\s+)/$1$2$EOS$3/sg;
	$text=~s/($PAP\s)(\s*\()/$1$EOS$2/gs;
	$text=~s/('\w$P)(\s)/$1$EOS$2/gs;


	$text=~s/(\sno\.)(\s+)(?!\d)/$1$EOS$2/gis;

##	# split where single capital letter followed by dot makes sense to break.
##	# notice these are exceptions to the general rule NOT to split on single
##	# letter.
##	# notice also that sibgle letter M is missing here, due to French 'mister'
##	# which is representes as M.
##	#
##	# the rule will not split on names begining or containing 
##	# single capital letter dot in the first or second name
##	# assuming 2 or three word name.
##	$text=~s/(\s[[:lower:]]\w+\s+[^[[:^upper:]M]\.)(?!\s+[[:upper:]]\.)/$1$EOS/sg;


	# add EOS when you see "a.m." or "p.m." followed by a capital letter.
	$text=~s/([ap]\.m\.\s+)([[:upper:]])/$1$EOS$2/gs;

	return $text;
}

sub clean_sentences {
	my ($sentences) = @_;
		my $cleaned_sentences;
		foreach my $s (@$sentences) {
			next if not defined $s;
			next if $s!~m/\w+/;
			$s=~s/^\s*//;
			$s=~s/\s*$//;
      $s=~s/\s+/ /g;
			push @$cleaned_sentences,$s;
		}
	return $cleaned_sentences;
}

sub first_sentence_breaking {
	my ($text) = @_;
	$text=~s/\n\s*\n/$EOS/gs;	## double new-line means a different sentence.
	$text=~s/($PAP\s)/$1$EOS/gs; #break at any $P, or any $P followed immediately $AP.
	$text=~s/(\s\w$P)/$1$EOS/gs; # breake also when single letter comes before punc.
	return $text;
}

1;
