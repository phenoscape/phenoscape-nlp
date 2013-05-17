 /* $Id: MainPgm.java 827 2011-06-05 03:36:57Z hong1.cui $ */
package fna.charactermarkup;

public class MainPgm {

	public MainPgm(String database, String projectfolder) {
		// TODO Auto-generated constructor stub
		new CondenseMarkedsent(database);
		new HideCommas(database);
		//new NewSegmentation(database);
		new Segmentation(database);
		new ParseSimpleseg(database, projectfolder);//output to C:\\DATA\\evaluation\\fnav19\\TestCase_Benchmark\\
		new SegmentIntegrator(database, projectfolder);
		new CompareXML(database, projectfolder);
		//not needed
		//ParseComplexseg.main(null);
		//OcsFreq.main(null);
	}


	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new MainPgm("annotationevaluation_heuristics_fna", "C:\\DATA\\evaluation\\fnav19");
		//new MainPgm("annotationevaluation_heuristics_treatise", "C:\\DATA\\evaluation\\treatise");		
	}

}
