package testing_lab.lab1;

public class Progress {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int total = 10;
		for (int count = 1; count <= total; count++) {
			System.out.println("finalizing "+count);
			System.out.println(40+(count*60/total));
		}
	}

}
