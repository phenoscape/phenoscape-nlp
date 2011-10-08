/**
 * $Id: ParsingUtil.java 840 2011-06-05 03:57:51Z hong1.cui $
 */
package fna.parsing;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.jdom.Comment;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * @author chunshui
 */
public class ParsingUtil {
	
	private static final Logger LOGGER = Logger.getLogger(ParsingUtil.class);
	public static void outputXML(Element treatment, File file, Comment comment) throws ParsingException {
		try {
			XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
			Document doc = new Document(treatment);
			// File file = new File(path, dest + "/" + count + ".xml");
			BufferedOutputStream out = new BufferedOutputStream(
					new FileOutputStream(file));
			
			outputter.output(doc, out);
			if(comment!=null) outputter.output(comment, out);
			out.close(); // don't forget to close the output stream!!!
			
			// generate the information to the listener (gui)
			// listener.info(String.valueOf(count), "", file.getPath());
		} catch (IOException e) {
			LOGGER.error("Exception in ParsingUtil:outputXML");
			e.printStackTrace();
			throw new ParsingException(e);
		}
	}
}
