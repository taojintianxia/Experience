package performance;

import java.util.Random;

/**
 * 
 * 
 * @author Kane.Sun
 * @version Sep 2, 2013 10:31:07 AM
 * 
 */
public class LogicKeyWordComparation {

	static Random random = new Random();

	// execute for 10000 times
	public static void main(String... args) {
		long start = System.nanoTime();
		for (int i = 0; i < 10000; i++) {
			String str = random.nextBoolean() + "";
			if (str.equals("true")) {
			} else if (str.equals("false")) {
			}
		}

		System.out.println("it takes : " + (System.nanoTime() - start));

		start = System.nanoTime();
		for (int i = 0; i < 10000; i++) {
			String str = random.nextBoolean() + "";
			switch (str) {
			case "true":
				break;
			case "false":
				break;
			}
		}

		System.out.println("it takes : " + (System.nanoTime() - start));
	}

}
