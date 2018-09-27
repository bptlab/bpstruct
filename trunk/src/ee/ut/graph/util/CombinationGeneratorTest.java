package ee.ut.graph.util;

import junit.framework.TestCase;

public class CombinationGeneratorTest extends TestCase {	
	public void testPermutations() {
		
		CombinationGenerator gen = new CombinationGenerator(2, 1);
		gen.reset();
		while (gen.hasMore()) {
			int[] perm = gen.getNext();
			System.out.print("Permutation: ");
			for (int i: perm)
				System.out.printf("%2d", i);
			System.out.println();
		}
	}

}
