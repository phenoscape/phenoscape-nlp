phenoscape-nlp
==============

Source for [EQ]-Generating CharaParser application, which uses unsupervised lexicon learning and natural language processing to propose EQ-based phenotype annotations.
The software is currently being developed as part of the Phenoscape project (http://phenoscape.org/). It is useful for character descriptions written in the style similar to the example below:

> Distal end of cleithrum
> 1. is not bifurcate, bearing only an anterior process
> 2. bifurcate

The input to EQ-Generating CharaParser is

1. character descriptions in NeXML format
2. a (may be empty) glossary of the related domain
3. ontologies for EQs

Some algorithms used in CharaParser are reported in:

1. Cui, H., Boufford, D., & Selden, P. (2010). Semantic Annotation of Biosystematics Literature without Training Examples. Journal of American Society of Information Science and Technology. 61 (3): 522-542 [doi:10.1002/asi.21246]

2. Cui, H. (2012). CharaParser for fine-grained semantic annotation of organism morphological descriptions. Journal of American Society of Information Science and Technology. 63(4) [doi:10.1002/asi.22618]

License
-------

EQ-Generating CharaParser source code can be used, modified, and distributed under the terms of the MIT License. Please see the file LICENSE for details.

How to cite
-----------

If you use EQ-Generating CharaParser in your research, please cite the following publications:

•	Cui, H., Dahdul, W., Dececchi, A., Ibrahim, N., Mabee, P., Balhoff, J., Gopalakrishnan, H. (2015) CharaPaser+EQ: Performance Evaluation Without Gold Standard. Annual Meeting of the Association for Information Science and Technology, Nov 6-10, St Louis, Missouri, 2015. (Full paper, acceptance rate: 36.%) https://www.asist.org/files/meetings/am15/proceedings/openpage15.html

[EQ]: https://wiki.phenoscape.org/wiki/EQ_for_character_matrices
[doi:10.1002/asi.21246]: http://doi.org/10.1002/asi.21246
[doi:10.1002/asi.22618]: http://doi.org/10.1002/asi.22618
