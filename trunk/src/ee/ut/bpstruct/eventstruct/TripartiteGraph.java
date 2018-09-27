/* 
 * Copyright (C) 2011 - Luciano Garcia Banuelos
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import ee.ut.graph.moddec.ColoredGraph;

public class TripartiteGraph {
	Map<Integer, Integer> AMap;
	Map<Integer, Integer> BMap;
	Map<Integer, Integer> ZMap;
	List<Integer> sortedA;
	int [][] Gpt;
	
	class NodeAndOutDegree implements Comparable<NodeAndOutDegree> {
		Integer node;
		Integer outdegree;
		
		public NodeAndOutDegree(Integer node, Integer outdegree) {
			this.node = node;
			this.outdegree = outdegree;
		}
		
		public int compareTo(NodeAndOutDegree theOther) {
			return theOther.outdegree.compareTo(outdegree);
		}
		
		public String toString() {
			return String.format("(Node: %d, Outdegree: %d)", node, outdegree);
		}
	}
		
	public TripartiteGraph(ColoredGraph g, List<Integer> A, List<Integer> B) {
		AMap = new HashMap<Integer, Integer>();
		BMap = new HashMap<Integer, Integer>();
		ZMap = new HashMap<Integer, Integer>();
		sortedA = new LinkedList<Integer>();
		
		int offset = A.size();
		int index = 0;
		
		for (Integer a: A) {
			AMap.put(a, index);
			ZMap.put(a, index + offset);
			index++;
		}
		index = offset * 2;

		for (Integer b: B)
			BMap.put(b, index++);
		
		int size = A.size() * 2 + B.size();
		
		Gpt = new int [size][size];
		
		computeMu(g, A, B, AMap, BMap, Gpt);
		computeMup(g, A, B, A, AMap, BMap, ZMap, Gpt);
		transitiveClosure(Gpt);
		
		PriorityQueue<NodeAndOutDegree> sortedlist = new PriorityQueue<NodeAndOutDegree>();
		for (Integer a: A) {
			int outdegree = 0;
			for (Integer b: B)
				if (g.hasEdge(a, b))
					outdegree++;
			sortedlist.add(new NodeAndOutDegree(a, outdegree));
		}
		
		while (!sortedlist.isEmpty()){
			NodeAndOutDegree nplus = sortedlist.remove();
			sortedA.add(nplus.node);
		}
	}
	
	public boolean isGammaSubset(Integer ai, Integer aj) {
		return Gpt[AMap.get(ai)][ZMap.get(aj)] == 0;
	}
	
	public List<Integer> getAOrderedByOutdegree() {
		return sortedA;
	}
	
	private void computeMup(ColoredGraph g, List<Integer> A, List<Integer> B,
			List<Integer> Z, Map<Integer, Integer> AMap,
			Map<Integer, Integer> BMap, Map<Integer, Integer> ZMap, int[][] Gpt) {
		
		for (Integer ai: A) {
			Set<Integer> BminusGamma = new HashSet<Integer>(B);
			for (Integer b: B)
				if (g.hasEdge(ai, b))
					BminusGamma.remove(b);
			for (Integer b: BminusGamma)
				Gpt[BMap.get(b)][ZMap.get(ai)] = 1;
		}
	}

	private void computeMu(ColoredGraph g, List<Integer> A, List<Integer> B,
			Map<Integer, Integer> AMap, Map<Integer, Integer> BMap, int[][] Gpt) {
		
		for (Integer src: A)
			for (Integer tgt: B)
				if (g.hasEdge(src, tgt))
					Gpt.clone()[AMap.get(src)][BMap.get(tgt)] = 1;
	}
	
	/**
	 * Computes the Transitive Closure of a graph, using
	 * Floyd-Warshall's algorithm
	 * 
	 * @param m Graph adjacency matrix (in: original one, out: transitive closure)
	 */
	protected void transitiveClosure(int [][] m) {
		int n = m.length;
		
		for (int k = 0; k < n; k++)
            for (int i = 0; i < n; i++)
                for (int j = 0; j < n; j++)
                    m[i][j] |= (m[i][k] & m[k][j]);		
	}
		
	protected void print(int [][] _m) {
		for (int i = 0; i < _m.length; i++) {
			for (int j = 0; j < _m.length; j++)
				System.out.printf("%3d",_m[i][j]);
			System.out.println();
		}
	}
}
