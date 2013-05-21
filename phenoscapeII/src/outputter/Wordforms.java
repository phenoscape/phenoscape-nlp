package outputter;

import java.util.ArrayList;
import java.util.List;


public class Wordforms {

	
	static String adjective_suffixes = "ed|-less|less|-like|like|shaped|-shaped|y|ly|ic|";
	
	public static List<String> toAdjective(String word)
	{
		String suffix[] = adjective_suffixes.split("\\|");
		ArrayList<String> forms = new ArrayList<String>();
		
		for(String suf:suffix)
		{
			forms.add(word.trim()+suf);
			if(word.matches(".*ion"))
				forms.add(word.substring(0, word.lastIndexOf("ion"))+suf);
		}
		
		return forms;
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
