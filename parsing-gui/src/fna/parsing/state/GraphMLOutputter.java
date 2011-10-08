/**
 * 
 */
package fna.parsing.state;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import fna.parsing.ApplicationUtilities;
import fna.parsing.Registry;

/**
 * @author Hong Updates
 *
 */

public class GraphMLOutputter {
	private static String nl = System.getProperty("line.separator");
	public static String header = "<?xml version='1.0' encoding='UTF-8' standalone='no'?> " +nl+
			"<graphml xmlns='http://graphml.graphdrawing.org/xmlns' " +nl+
			"xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +nl+
			"xsi:schemaLocation='http://graphml.graphdrawing.org/xmlns " +nl+
			"http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd'> " +nl+
			"<graph edgedefault='undirected' id='graph'> " +nl+
			"<key attr.name='name' attr.type='string' for='node' id='name'/> " +nl+
			"<key attr.name='type' attr.type='string' for='all' id='type'/> " +nl+
			"<key attr.name='weight' attr.type='float' for='edge' id='weight'/>"+nl;
	/**
	 * 
	 */
	public GraphMLOutputter() {
		//empty up the folder

		String path = Registry.TargetDirectory;
		File dir = new File(path, ApplicationUtilities.getProperty("CHARACTER-STATES"));
		File[] files = dir.listFiles();
		for(int i = 0; i<files.length; i++){
			files[i].delete();
		}
		
	}
	
	public GraphMLOutputter(boolean cleanUp){
		
	}
	/**
	 * output groups as a GraphML document, with nodes and links
	 * @param sets
	 */

	@SuppressWarnings("unchecked")
	public void output(ArrayList<ArrayList> groups, int initialgroupnumber){		
		int gcount = initialgroupnumber;
		Iterator<ArrayList> sets = groups.iterator();
		while(sets.hasNext()){
			String graphXML = GraphMLOutputter.header+nl;
			ArrayList<ArrayList> group = (ArrayList)sets.next();
			System.out.println("Group "+gcount+ ":");
			Hashtable<String, String> nodes = new Hashtable<String, String>();
			int nid = 1;
			for(int i = 0; i < group.size(); i++){
				String t1 =(String)((ArrayList)group.get(i)).get(0);
				String t2 =(String)((ArrayList)group.get(i)).get(1);
				String t1id = null, t2id = null;
				if (t1 != null && !t1.equals("")) {
					t1id = nodes.get(t1);
					if(t1id==null){
						t1id = nid+"";
						graphXML += "<node id='"+t1id+"'><data key='name'>"+t1+"</data></node>"+nl;
						nodes.put(t1, nid+"");
						nid++;
					}
				}
				
				if (t2 != null && !t2.equals("")) {
					t2id = nodes.get(t2);
					if(t2id==null){
						t2id = nid+"";
						graphXML += "<node id='"+t2id+"'><data key='name'>"+t2+"</data></node>"+nl;
						nodes.put(t2, nid+"");
						nid++;
					}
				}
				
				
				/* We are not using weights so no need to have this */
				int w  = 0;//Integer.parseInt((String)((ArrayList)group.get(i)).get(2));

				if (t1id != null && t2id != null) {
					graphXML +="<edge source='"+t1id+"' target='"+t2id+"' weight='"+normalize(w)+"'/>"+nl;
				}
				
			}
			
			graphXML+="</graph>"+nl+"</graphml>";
			output2file(gcount, graphXML);
			gcount++;
		}
	}
		
	private float normalize(int v) {
		// TODO Auto-generated method stub
		return 1;
	}
	
	private void output2file(int id, String text) {
		try {
			String path = Registry.TargetDirectory;
			File file = new File(path, ApplicationUtilities.getProperty("CHARACTER-STATES") + "/" +"Group_"+id+".xml");
			BufferedWriter out = new BufferedWriter(new FileWriter(file));
			out.write(text);
			out.close(); // don't forget to close the output stream!!!
		} catch (IOException e) {
			e.printStackTrace();
			//LOGGER.error("", e);
			//throw new ParsingException("", e);
		}		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
