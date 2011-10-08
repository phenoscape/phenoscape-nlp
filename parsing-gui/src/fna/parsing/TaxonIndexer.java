/**
 * $Id: TaxonIndexer.java 840 2011-06-05 03:57:51Z hong1.cui $
 */
package fna.parsing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

import org.apache.log4j.Logger;

/**
 * To parse the taxon file and build the taxon index
 * 
 * The relationship of the taxon is: F -> (SF) -> G -> (SG) - S
 * 
 * @author chunshui
 */
public class TaxonIndexer implements Serializable {

	private static final long serialVersionUID = -626445898401165211L;

	private static final String TXT_FILE = "TaxaList.txt"; //TODO:configurable

	private static final String BIN_FILE = "TaxonIndexer.bin";
	private static final Logger LOGGER = Logger.getLogger(TaxonIndexer.class);
	
	private String path;
	//Hong: 10/6/08: use arraylist for numbers and names
	//private String[] numbers;
	//private String[] names;
	private ArrayList<String> numberList = new ArrayList<String>();
	private ArrayList<String> nameList = new ArrayList<String>();
	/*if TXT_FILE is available, build TaxonIndex. Otherwise create an empty TaxonIndex to be populated by VolumeVerifier*/

	public static void saveUpdated(String path, TaxonIndexer ti) throws ParsingException {
		try {
			File file = new File(path, BIN_FILE);
			ObjectOutput out = new ObjectOutputStream(
					new FileOutputStream(file));
			out.writeObject(ti);
			out.close();
		} catch (IOException e) {
			LOGGER.error("Save the updated TaxonIndexer failed.", e);
			e.printStackTrace();
			throw new ParsingException(
					"Save the updated TaxonIndexer failed.", e);
		}
	}
	
	public static TaxonIndexer loadUpdated(String path) throws ParsingException{
		try {
			File file = new File(path, BIN_FILE);
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(
					file));
			// Deserialize the object
			TaxonIndexer ti = (TaxonIndexer) in.readObject();
			in.close();
			
			return ti;
		} catch (Exception e) {
			LOGGER.error("Load the updated TaxonIndexer failed.", e);
			e.printStackTrace();
			throw new ParsingException(
					"Load the updated TaxonIndexer failed.", e);
		}
	}


	public TaxonIndexer(String path) {
		this.path = path;
	}

	public void build() throws ParsingException {
	
		try {
			File file = new File(path, TXT_FILE);
			if(file.exists()){//otherwise do nothing. 
				BufferedReader reader = new BufferedReader(new FileReader(file));
				String line = null;
				while ((line = reader.readLine()) != null) {
					String[] fields = line.split("#");
					if (fields.length == 1)
						continue; // empty or illegal line
					String number = buildNumber(fields[1]);
					numberList.add(number);
	
					String name = buildName(fields[2]);
					nameList.add(name);
				}
				reader.close();
			}
		} catch (Exception e) {
			LOGGER.error("build failed in TaxonIndexer", e);
			e.printStackTrace();
			throw new ParsingException(e);
		}

		/*numbers = new String[numberList.size()];
		numberList.toArray(numbers);

		names = new String[nameList.size()];
		nameList.toArray(names);*/
	}

	public boolean emptyNumbers(){
		return numberList.size() == 0;
	}
	
	public boolean emptyNames(){
		return nameList.size() == 0;
	}
	
	public void addNumber(String number) {		
		numberList.add(number);
	}

	public void addName(String name) {
		nameList.add(name);
	}

	public String getNumber(int index) {
		return numberList.get(index);
	}

	public String getName(int index) {
		return nameList.get(index);
	}

	private String buildNumber(String field) {
		int pos = field.trim().indexOf('=');
		return (pos == -1) ? field : field.substring(pos + 1);
	}

	private String buildName(String field) {
		return field.trim();
	}
}
