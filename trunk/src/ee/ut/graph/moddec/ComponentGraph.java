/* 
 * Copyright (C) 2010 - Luciano Garcia Banuelos
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
package ee.ut.graph.moddec;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class ComponentGraph {
	ColoredGraph graph = new ColoredGraph();
	Map<String, Set<BitSet>> map = new HashMap<String, Set<BitSet>>();
	
	public void addVertex(int id, String label, BitSet part) {
		Set<BitSet> set = new HashSet<BitSet>();
		set.add(part);
		graph.addVertex(label);
		map.put(label, set);
	}
	
	public void addEdge(String v1, String v2) {
		graph.addEdge(v1, v2);
	}
	
	public Set<BitSet> getPartitions(Set<Integer> vertices) {
		Set<BitSet> result = new HashSet<BitSet>();
		for (Integer v: vertices)
			result.addAll(map.get(graph.getLabel(v)));
		return result;
	}
	
	public BitSet getPartitionUnion() {
		BitSet result = new BitSet();
		for (String label: graph.getLabels()) {
			for (BitSet bs: map.get(label))
				result.or(bs);
		}
		return result;
	}

	public void removeVertices(Set<Integer> vertices) {
		graph = graph.removeVertices(vertices);
	}

	// Computation of strongly connected components
	
//	public void computeSCC() {
//		Stack<Integer> stack = new Stack<Integer>();
//		Set<Set<Integer>> scc = new HashSet<Set<Integer>>();
//		Set<Integer> sources = graph.sourceVertices();
//		if (sources.isEmpty())
//			sources.add(graph.getVertices().iterator().next());
//
//		for (Integer s: graph.getVertices()) {
//			for (Integer t: graph.postSet(s)) {
//				System.out.println(s + " " + t);
//			}
//		}
//		
//		while (!sources.isEmpty()) {
//			Integer v = sources.iterator().next();
//			sources.remove(v);
//			tarjan(v, 0, /*visited,*/ stack, scc);
//		}
//		
//		for (Set<Integer> cc : scc) {
//			if (cc.size() > 1) {
//				Set<BitSet> parts = getPartitions(cc);
//				Integer v = cc.iterator().next();
//				cc.remove(v);
//				graph = graph.removeVertices(cc);
//				map.put(graph.getLabel(v), parts);
//			}
//		}
//	}
//
//	Map<Integer, Integer> index = new HashMap<Integer, Integer>();
//	Map<Integer, Integer> lowlink = new HashMap<Integer, Integer>();
//	private void tarjan(Integer v, int _index, // Set<Integer> visited,
//			Stack<Integer> stack, Set<Set<Integer>> scc) {
//		index.put(v, _index);
//		lowlink.put(v, _index);
//		_index++;
//		stack.push(v); //visited.add(v);
//		for (Integer n: graph.postSet(v)) {
//			if (index.get(n) == null) {
//				tarjan(n, _index, /*visited,*/ stack, scc);
//				lowlink.put(v, Math.min(lowlink.get(v), lowlink.get(n)));
//			} else if (stack.contains(n))
//				lowlink.put(v, Math.min(lowlink.get(v), index.get(n)));
//		}
//		if (lowlink.get(v) == index.get(v)) {
//			Integer n;
//			Set<Integer> cc = new HashSet<Integer>();
//			do {
//				n = stack.pop();
//				cc.add(n);
//			} while (v != n);
//			scc.add(cc);
//		}
//	}
	
	public void computeSCC() {		
		Set<Set<Integer>> scc = kosaraju();
		
		for (Set<Integer> cc : scc) {
			if (cc.size() > 1) {
				Set<BitSet> parts = getPartitions(cc);
				Integer v = cc.iterator().next();
				cc.remove(v);
				graph = graph.removeVertices(cc);
				map.put(graph.getLabel(v), parts);
			}
		}
	}
	
	private Set<Set<Integer>> kosaraju() {
		Set<Set<Integer>> scc = new HashSet<Set<Integer>>();
		Stack<Integer> stack = new Stack<Integer>();
		Set<Integer> visited = new HashSet<Integer>();
		for (Integer vertex: graph.getVertices())
			if (!visited.contains(vertex))
				searchForward(vertex, stack, visited);			

		//graph.toDot(System.out);
		visited.clear();
		while(!stack.isEmpty()) {
			Set<Integer> component = new HashSet<Integer>();
			searchBackward(stack.peek(), visited, component);
			scc.add(component);
			stack.removeAll(component);
		}
		return scc;
	}

	private void searchBackward(Integer node, Set<Integer> visited, Set<Integer> component) {
		Stack<Integer> worklist = new Stack<Integer>();
		worklist.push(node);
		while (!worklist.isEmpty()) {
			Integer curr = worklist.pop();
			visited.add(curr);
			component.add(curr);
			for (Integer pred: graph.preSet(curr))
				if (!visited.contains(pred) && !worklist.contains(pred))
					worklist.add(pred);
		}
	}

	private void searchForward(Integer curr, Stack<Integer> stack, Set<Integer> visited) {
		visited.add(curr);
		for (Integer succ: graph.postSet(curr))
			if (!visited.contains(succ))
				searchForward(succ, stack, visited);
		stack.push(curr);
	}

}
