package ee.ut.graph.util;

import de.bpt.hpi.graph.Edge;
import de.bpt.hpi.graph.Graph;
import junit.framework.TestCase;

public class PermutationGeneratorTest extends TestCase {
//	public void testPermutations() {
//		Graph graph = new Graph();
//		Integer _1 = graph.addVertex("s");
//		Integer _2 = graph.addVertex("A");
//		Integer _3 = graph.addVertex("B");
//		Integer _4 = graph.addVertex("e");
//		
//		graph.addEdge(_1, _2);
//		graph.addEdge(_1, _3);
//		graph.addEdge(_2, _4);
//		graph.addEdge(_3, _4);
//		
//		PermutationGenerator gen = new PermutationGenerator(4);
//		gen.reset();
//		while (gen.hasMore()) {
//			int[] perm = gen.getNext();
//			System.out.print("Permutation: ");
//			for (int i: perm)
//				System.out.printf("%2d", i);
//			System.out.println();
//			
//			System.out.println("Code: ");
//			for (int i: perm)
//				for (int j: perm)
//					if (i == j)
//						System.out.print(graph.getLabel(i+1));
//					else
//						System.out.print(graph.getEdges().contains(new Edge(i+1, j+1))? 1 : 0);
//			System.out.println();
//		}
//		System.out.println(">> "+ ("b".compareTo("a")));
//	}
	
	public void testPermutations() {
		
		PermutationGenerator gen = new PermutationGenerator(4);
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
