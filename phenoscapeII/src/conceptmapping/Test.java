/**
 * 
 */
package conceptmapping;

/**
 * @author Hong Updates
 *
 */


public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
			String[] patoresult = new String[]{"a", "b"};
			String[] taoresult = new String[]{"c"};
			String[] results = new String[patoresult.length + taoresult.length];
			if(patoresult!=null && taoresult!=null){//merge
				int i; int j;
				for(i=0, j=0; i<patoresult.length; i++, j++){
					results[i] = patoresult[j];
				}
				for(i=patoresult.length, j=0; i<patoresult.length + taoresult.length; i++, j++){
					results[i] = taoresult[j];
				}
				System.out.println("merged");
			}
			
		}

	}


