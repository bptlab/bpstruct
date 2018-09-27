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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import ee.ut.graph.moddec.ColoredGraph;

public class TupleDAG {
	Map<Tuple, Set<Tuple>> parents;
	Map<Tuple, Set<Tuple>> children;
	
	Tuple root;
	Set<Tuple> primes;
	
	public TupleDAG() {
		parents = new LinkedHashMap<Tuple, Set<Tuple>>();
		children = new LinkedHashMap<Tuple, Set<Tuple>>();
	}
	
	public void add(Tuple parent, Tuple child) {
		if (!parents.containsKey(child))
			parents.put(child, new LinkedHashSet<Tuple>());
		if (!children.containsKey(parent))
			children.put(parent, new LinkedHashSet<Tuple>());
			
		children.get(parent).add(child);
		parents.get(child).add(parent);
	}

	public void pack() {
		Set<Tuple> set = new HashSet<Tuple>(children.keySet());
		set.removeAll(parents.keySet());
		if (set.size() != 1) return;
		
		root = set.iterator().next();
		primes = new HashSet<Tuple>();
		
		for (Tuple curr: parents.keySet())
			if (parents.get(curr).size() == 1)
				primes.add(curr);
		
	}
	public ColoredGraph computeORG(ColoredGraph original, Map<String, String> labelMap) {
		Set<Tuple> set = new HashSet<Tuple>(children.keySet());
		set.addAll(parents.keySet());
		Set<Tuple> _sinks = new HashSet<Tuple>(parents.keySet());
		_sinks.removeAll(children.keySet());
		Set<Integer> sinks = new HashSet<Integer>();

		int size = set.size();
		Map<Tuple, Integer> indexes = new HashMap<Tuple, Integer>();
		Tuple []rindexes = new Tuple[size];
		Map<Tuple, BitSet> lubs = new HashMap<Tuple, BitSet>();
		int index = 0;
		int [][] m = new int[size][size];
		for (Tuple tuple: set) {
			if (_sinks.contains(tuple)) sinks.add(index);
			rindexes[index] = tuple;
			indexes.put(tuple, index++);
			lubs.put(tuple, new BitSet());			
		}
		
		for (Tuple tsrc: children.keySet()) {
			Integer src = indexes.get(tsrc);
			for (Tuple ttgt: children.get(tsrc)) {
				Integer tgt = indexes.get(ttgt);
				m[src][tgt] = 1;
			}
		}
		transitiveClosure(m);
		for (Tuple tuple: children.keySet()) {
			BitSet bset = lubs.get(tuple);
			int i = indexes.get(tuple);
			for (Integer j: sinks)
				if (m[i][j] == 1)
					bset.set(j);
		}

		for (Tuple tuple: _sinks) {
			BitSet bset = lubs.get(tuple);
			int i = indexes.get(tuple);
			bset.set(i);
		}
		
		// Conflict relation		
		for (int i = 0; i < size; i++)
			for (int j = i + 1; j < size; j++) {
				Tuple t0 = rindexes[i];
				Tuple t1 = rindexes[j];
				if (!lubs.get(t0).intersects(lubs.get(t1)))
					m[i][j] = m[j][i] = 1;
			}
				
		ColoredGraph orgraph = new ColoredGraph();
		
		for (Tuple prime: primes) {
			labelMap.put(prime.toString(), original.getLabel(prime.rep));
			orgraph.addVertex(prime.toString());
		}
		
		for (Tuple t0: primes) {
			Integer i0 = indexes.get(t0);
			for (Tuple t1: primes) {
				Integer i1 = indexes.get(t1);
				if (!i0.equals(i1) && m[i0][i1] == 1)
					orgraph.addEdge(t0.toString(), t1.toString());
			}
		}
		return orgraph;
	}
		
	public String toDot() {
		if (root == null) return null;
		
		ByteArrayOutputStream outstream = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(outstream);

		out.println("digraph G {");
		
		Map<Tuple, Integer> map = new HashMap<Tuple, Integer>();
		map.put(root, 0);
		out.printf("\tn%d [label=\"%s\"]\n", 0, root);
		
		int index = 1;
		for (Tuple tuple: parents.keySet()) {
			map.put(tuple, index);
			if (primes.contains(tuple))
				out.printf("\tn%d [label=\"%s\",style=filled,color=skyblue]\n", index++, tuple);
			else
				out.printf("\tn%d [label=\"%s\"]\n", index++, tuple);
		}
					
		for (Tuple parent: children.keySet())
			for (Tuple child: children.get(parent))
				out.printf("\tn%d -> n%d\n", map.get(parent), map.get(child));
		
		out.println("}");
		return outstream.toString();
	}
	
	protected void transitiveClosure(int [][] m) {
		int n = m.length;
		
		for (int k = 0; k < n; k++)
            for (int i = 0; i < n; i++)
                for (int j = 0; j < n; j++)
                    m[i][j] |= (m[i][k] & m[k][j]);		
	}
}
