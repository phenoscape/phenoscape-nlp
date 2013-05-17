 /* $Id: DescriptionAssembler.java 790 2011-04-11 17:57:38Z hong1.cui $ */
package fna.charactermarkup;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@SuppressWarnings({  "unused" })
public class DescriptionAssembler {
    static protected Connection conn = null;
    static protected String database = "fnav19_benchmark";
    static protected String username = "root";
    static protected String password = "";
    private String save2dir = null;
    private String filenametype = null;
    private String fileLengthIndexes = null;
    private Integer hasSeg = new Integer(0);

    public DescriptionAssembler(String save2dir, String filenametype) {
        //set up a connection to the database
        this.save2dir = save2dir;
        this.filenametype = filenametype; //number or taxon
        StringBuffer fileLenIndexes = new StringBuffer();
        try{
            if(conn == null){
                Class.forName("com.mysql.jdbc.Driver");
                String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
                conn = DriverManager.getConnection(URL);
                //read description length indexes from sentinfile table
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("select filename, endindex from sentinfile");
                while(rs.next()){
                    fileLenIndexes.append(rs.getString("filename"));
                    fileLenIndexes.append("["+rs.getString("endindex")+"]");
                    fileLenIndexes.append(" ");
                }
            }
            this.fileLengthIndexes =fileLenIndexes.toString();
        }catch(Exception e){
        	System.err.println(e);
            e.printStackTrace();
        }        
    }

    private void dump2disk() throws SQLException {
        //assemble
        String[] records = this.fileLengthIndexes.toString().trim().split("\\s+");
        int start = 0;
        for(int n = 0 ; n <records.length; n++){
            Pattern p = Pattern.compile("(.*?)\\[(\\d+)\\]");
            Matcher m = p.matcher(records[n]);
            if(m.matches()){
                String filename = m.group(1);
                filename=filename.substring(0, filename.indexOf(".")+1)+"xml";
                String in = m.group(2);
                int end = Integer.parseInt(in);
                StringBuffer content = new StringBuffer( "<?xml version=\"1.0\" encoding=\"iso8859-1\"?><description>");
                int clausecount = 0;
                for(int i = start; i <= end; i++){
                    Statement stmt = conn.createStatement();
                    //grab marked-up sentences from sentence table
                    ResultSet rs = stmt.executeQuery("select source, originalsent, tag, modifier from sentence where sentid="+i);
                    rs.next();
                    String source = rs.getString("source");
                    String original = rs.getString("originalsent");
                    String tag = rs.getString("tag");
                    if(tag.charAt(0)=='['){
                    	tag=tag.substring(1, tag.length()-1);
                    }
                    String modifier = rs.getString("modifier");
                    if(modifier.compareTo("")!=0 && modifier.charAt(0)=='['){
                    	modifier=modifier.substring(1, modifier.length()-1);
                    }
                    String sentence = composeMarkedSentence(source);//depending on hasSeg, the returned value could either be a set of marked up segments or simply a character set
                    
                    tag = tag.replaceAll("\\s+", "_");
                    
                    if(tag.compareTo("") == 0){
                        tag = "unknown";
                    }
                    String finaltag = "";
	                if(hasSeg.compareTo(new Integer(1))==0){
	                    finaltag = "<"+tag+" modifier=\""+modifier+"\">";
	                }else{
	                	finaltag = "<"+tag+" modifier=\""+modifier+"\" "+sentence+">";
	                	sentence = original;
	                }
	                String endtag = "</"+tag+">";
	                String markedsent = finaltag+sentence+endtag;
                    content.append(markedsent);        
                }
                content.append("</description>");
                write2disk(filename, content.toString());
                start = end+1;
            }
        }
    }

    
    private String composeMarkedSentence(String source) {
		// TODO Auto-generated method stub
    	String charstates="";
    	try{
    		int ct=0;
    		Statement stmt = conn.createStatement();
    		Statement stmt1 = conn.createStatement();
    		Statement stmt2 = conn.createStatement();
	        ResultSet rs = stmt.executeQuery("select * from marked_simpleseg where source='"+source+"'");
	    	while(rs.next()){
	    		ct+=1;
	    	}
			if(ct==1){
				ResultSet rs1 = stmt1.executeQuery("select * from marked_simpleseg where source='"+source+"'");
				while(rs1.next()){
					String str=rs1.getString(3);
					Pattern pattern1 = Pattern.compile("[a-zA-Z\\_]+=\"[\\w±\\+\\–\\-\\.:/\\_;x´X\\×\\s,]+\"");
	            	Matcher matcher = pattern1.matcher(str);
	            	while ( matcher.find()){
	            		int i=matcher.start();
	            		int j=matcher.end();
	            		charstates=charstates.concat(str.subSequence(i,j).toString()+" ");
	                }
	            	matcher.reset();
				}
				hasSeg = 0;
			}
			else if(ct>1){
				ResultSet rs2 = stmt2.executeQuery("select * from marked_simpleseg where source='"+source+"'");
				while(rs2.next()){
					charstates=charstates.concat(rs2.getString(3));
				}
				hasSeg = 1;
			}
    	}catch (Exception e)
        {
    		System.err.println(e);
        }
    	return charstates;
	}

	private String composeCharacterString(String string) {
        // TODO Auto-generated method stub
        return null;
    }

    private void write2disk(String filename, String content) {
    	
        if(filenametype.indexOf("num")!=0){
            //access filename2taxon table to compose a file name containing taxa
            filename = getTaxonName(filename);
        }
        FileOutputStream out; // declare a file output object
        PrintStream s; // declare a print stream object
        try{
           out = new FileOutputStream(save2dir+filename);
           s = new PrintStream(out);
           s.println (content);
           s.close();
        }catch (Exception e){
           System.err.println ("Error writing to file "+save2dir+filename);
        }
    }

    private String getTaxonName(String filename) {
        StringBuffer newname = new StringBuffer();
        ArrayList<String> taxa = new ArrayList<String>();
        try{
        	newname.append(filename.substring(0,filename.indexOf("."))+"_");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("describe filename2taxon");
            while(rs.next()){
                taxa.add(rs.getString("Field"));
            }
            rs = stmt.executeQuery("select * from filename2taxon where filename='"+filename+"'");
            rs.next();
            //3-13 columns has taxonnames
            for(int i = 2; i <=12; i++){
            	//String temp = rs.getString(i);
            	//String temp2 = taxa.get(i);
            	if(rs.getString(i+1).compareTo("") != 0){
            		newname.append(taxa.get(i)+"_"+rs.getString(i+1)+"_");
            	}
            }
            String name = newname.toString();
            name = name.replaceAll("_+", "_").replaceFirst("_$", "");
            return name+".xml";
        }catch(Exception e){
        	System.err.println(e);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        DescriptionAssembler da = new DescriptionAssembler("C:\\Users\\Sriramu\\Desktop\\Description\\", "taxon");
        try{
            da.dump2disk();
        }catch(Exception e){
        	System.err.println(e);
            e.printStackTrace();
        }
    }

}

