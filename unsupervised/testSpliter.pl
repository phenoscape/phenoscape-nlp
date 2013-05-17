use SentenceSpliter;
#$P = q/[\.!?;:]/;			## PUNCTUATION
#$AP = q/(?:'|"|�|\)|\]|\})?/;	## AFTER PUNCTUATION
#$PAP = $P.$AP;
#print $PAP."\n";

$text = "Leaves basally white with pink or red, otherwise bright green; single midvein   prominently raised above leaf surface,";
$text .= " usually somewhat off-center, other veins barely or not raised; cross section rhomboid. Vegetative leaves to 1.75 m; ";
$text .="sheathing base   22.1�66.5  cm; distal part of leaf 31.9�95.8 0.5�2 cm, 1.4�1.8 times longer than proximal part of leaf,";
$text .= " margins sometimes undulate or crisped. Sympodial leaf  34.7�159.1  cm, usually shorter than to nearly equal to vegetative";
$text .= " leaves; sheathing base 16.1�76.4  cm; distal part of leaf 13.5�86.2   0.4�1.9 cm. Spadix  4.9�8.9 cm  5.3�10.8 mm at";
$text .= "anthesis, post-anthesis spadix 5.5�8.7 cm � 6�12.6 mm. Flowers 3�4 mm; pollen grains not staining in aniline blue. Fruit";
$text .= "not produced in North America. 2n = 36.";

$text = "forming ball or flat-topped mass; filament short, stout;";
#$text = "diam. Leaves:";
@sentences = SentenceSpliter::get_sentences($text);#@todo: avoid splits in brackets. how?
foreach (@sentences){
  print $_."\n";
}

