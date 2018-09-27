/* 
 * Copyright (C) 2011 - Luciano Garcia Banuelos, Artem Polyvyanyy
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ee.ut.bpstruct.eventstruct;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import ee.ut.graph.moddec.ColoredGraph;

public class ConfGenerator {
	ColoredGraph g;
//	Map<Integer, Set<Integer>> icausal;
	TupleDAG dag;
	public ConfGenerator(ColoredGraph g, //Map<Integer, Set<Integer>> icausal, 
			String modelName) {
		this.g = g;
//		this.icausal = icausal;
		this.dag = new TupleDAG();

		Set<Tuple> queue = new LinkedHashSet<Tuple>();
		Set<Tuple> lattice = new HashSet<Tuple>();

		Tuple root = new Tuple(new LinkedHashSet<Integer>(), new HashSet<Integer>(), new HashSet<Integer>(), null);
		
		for (Integer v: g.vertices)
			root.getSecond().add(v);
		
		queue.add(root);
		
		while (!queue.isEmpty()) {
			Tuple tuple = queue.iterator().next();
			queue.remove(tuple);
			lattice.add(tuple);
			Set<Tuple> succs = computeSuccessors(tuple);
			for (Tuple succ: succs)
				dag.add(tuple, succ);
			succs.removeAll(lattice);
			queue.addAll(succs);
		}		
		dag.pack();
		
//		try {
//			PrintStream out = new PrintStream(String.format("output/lattice_%s.dot", modelName));
//			out.println(dag.toDot());
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}
	}
	
	public TupleDAG getLattice() {
		return dag;
	}
	
	private Set<Tuple> computeSuccessors(Tuple tuple) {
		Set<Tuple> successors = new HashSet<Tuple>();
		Set<Integer> d1 = tuple.first;
		
//		if (d1.isEmpty()) {
//			for (Integer v: tuple.second)
//				if (!hasImmediatePredecessorIn(tuple.second, v))
//					successors.add(packSuccessor(g, tuple, v));
//		} else {
//			for (Integer v: tuple.second)
//				if (hasImmediatePredecessorIn(d1, v)  && !hasConflictingPeerIn(d1, v))
//					successors.add(packSuccessor(g, tuple, v));
//		}
		
		for (Integer v: tuple.second)
			if (d1.isEmpty() || hasPredecessorIn(d1, v) || hasConcurrentPeerIn(d1, v)) {
				boolean hasPredecessor = false;
				boolean hasExternalConflictingPredecessor = false;
				for (Integer b: tuple.second)
					if (areCausal(b, v)) {
						hasPredecessor = true;
						hasExternalConflictingPredecessor = hasConflictingPeerIn(d1, b);
						if (hasExternalConflictingPredecessor) break;
					}
				if (hasExternalConflictingPredecessor || !hasPredecessor)
					successors.add(packSuccessor(g, tuple, v));
			}
		return successors;
	}
	
	
	private boolean areConflicting(Integer b, Integer c) {
		return g.hasEdge(b, c) && g.hasEdge(c, b);
	}
	private boolean areConcurrent(Integer b, Integer c) {
		return !b.equals(c) && !g.hasEdge(b, c) && !g.hasEdge(c, b);
	}
	private boolean areCausal(Integer b, Integer c) {
		return g.hasEdge(b, c) && !g.hasEdge(c, b);
	}
	
	private boolean hasConflictingPeerIn(Set<Integer> d1, Integer b) {
		for (Integer c: d1)
			if (areConflicting(b, c))
				return true;
		return false;
	}


//	private boolean hasImmediatePredecessorIn(Set<Integer> d1, Integer v) {
//		for (Integer a: d1)
//			if (icausal.get(a).contains(v))
//				return true;		
//		return false;
//	}
	
	private boolean hasPredecessorIn(Set<Integer> d1, Integer v) {
		for (Integer a: d1)
			if (areCausal(a, v))
				return true;		
		return false;
	}	

	private boolean hasConcurrentPeerIn(Set<Integer> d1, Integer v) {
		for (Integer a: d1)
			if (areConcurrent(a, v))
				return true;		
		return false;		
	}

	private Tuple packSuccessor(ColoredGraph g, Tuple tuple, Integer v) {
		Tuple newtuple = (Tuple) tuple.clone();
		newtuple.rep = v;
		newtuple.first.add(v);
		newtuple.second.remove(v);
		
		for (Integer w: tuple.second)
			if (g.hasEdge(v, w) && g.hasEdge(w, v)) {
				newtuple.second.remove(w);
				newtuple.third.add(w);
			}
		
		return newtuple;
	}	
}
