package fna.parsing;



public class Test{
	
	public Test(String com){

	}


	public static void main(String[] args) {
		String content = "basihyal bone , bone and cartilage";
		if(!content.endsWith(")")){//format it
			content = content.replaceAll(" +(?=(,|and\\b|or\\b))", ") ")+")";
			content = content.replaceAll(" +(?=\\w+\\))", " (");					
		}
		System.out.println(content);

	}
}