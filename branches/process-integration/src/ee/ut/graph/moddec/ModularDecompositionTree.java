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

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ee.ut.bpstruct.CannotStructureException;
import ee.ut.graph.moddec.MDTNode.NodeType;

public class ModularDecompositionTree {
	private ColoredGraph graph;
	private MDTNode root;
	
	public ModularDecompositionTree(ColoredGraph graph) {
		this.graph = graph;
		root = decompose(graph, pack(graph.getVertices()));
	}
	
	public MDTNode getRoot() { return root; }
	
	private List<BitSet> partitionSubsets(ColoredGraph g, BitSet s, int w) {
		List<BitSet> partitions = new LinkedList<BitSet>();
		for (int i = 0; i < 4; i++)
			partitions.add(new BitSet());
		
		for(int i=s.nextSetBit(0); i>=0; i=s.nextSetBit(i+1)) {
			if (g.hasEdge(w, i) && g.hasEdge(i, w))
				partitions.get(0).set(i);
			else if (g.hasEdge(w, i))
				partitions.get(1).set(i);
			else if (g.hasEdge(i, w))
				partitions.get(2).set(i);
			else
				partitions.get(3).set(i);
		}
		
		return partitions;
	}
	
	private static BitSet pack(Set<Integer> set) {
		BitSet s = new BitSet();
		for (Integer v: set)
			s.set(v);
		return s;
	}
	
	private static Set<Integer> unpack(BitSet set) {
		Set<Integer> result = new LinkedHashSet<Integer>();
		for(int i=set.nextSetBit(0); i>=0; i=set.nextSetBit(i+1))
			result.add(i);
		return result;
	}
	
	private Set<BitSet> partition(ColoredGraph g, BitSet domain, int v) {
		Set<BitSet> l = new HashSet<BitSet>();
		Map<BitSet, BitSet> z = new HashMap<BitSet, BitSet>();
		Set<BitSet> result = new LinkedHashSet<BitSet>();
		BitSet s = (BitSet)domain.clone();
		s.clear(v);
		BitSet sp = new BitSet();
		sp.set(v);
		l.add(s);
		z.put(s, sp);
		while (!l.isEmpty()) {
			s = l.iterator().next(); l.remove(s);
			int w = z.get(s).nextSetBit(0);
			
			for (BitSet W : partitionSubsets(g, s, w)) {
				if (W.isEmpty()) continue;
				
				BitSet tmp = (BitSet)s.clone();
				tmp.andNot(W);
				tmp.or(z.get(s));
				tmp.clear(w);
			
				if (!tmp.isEmpty()) {
					l.add(W);
					z.put(W, tmp);
				} else
					result.add(W);
			}
		}
		return result;
	}
	
	private MDTNode decompose(ColoredGraph g, BitSet domain) {
		int v = domain.nextSetBit(0);
		MDTNode t = new MDTNode(domain, v);
		
		if (domain.cardinality() == 1)
			return t;
		
		Set<BitSet> m = partition(g, domain, v);
				
		ComponentGraph gpp = computeGpp(g, m, v);
		
		MDTNode u = t;
		while (gpp.graph.getVertices().size() > 0) {
			BitSet tmp = gpp.getPartitionUnion();
			tmp.set(v);
			u.setValue(tmp);
			
			tmp = new BitSet();
			tmp.set(v);
			MDTNode w = new MDTNode(tmp, v);
			u.addChild(w);
			Set<Integer> sinks = gpp.graph.sinkVertices();
			Set<BitSet> F = gpp.getPartitions(sinks);
			gpp.removeVertices(sinks);
			
			if (sinks.size() == 1 && F.size() > 1)
				u.setType(NodeType.PRIMITIVE);
			else {
				if (F.size() < 1) {
					System.out.println("Sinks.size() " + sinks.size());
					System.out.println(F);
					System.exit(-1);
				}
				int x = F.iterator().next().nextSetBit(0);
				
				if ((g.hasEdge(v, x) && g.hasEdge(x, v)) ||
						(!g.hasEdge(v, x) && !g.hasEdge(x, v))) {
					u.setType(NodeType.COMPLETE);
					u.setColor(g.hasEdge(v, x) ? 1 : 0);
				} else
					u.setType(NodeType.LINEAR);
			}
			
			for (BitSet partition: F) {	
				MDTNode root = decompose(g, partition);
				if (((u.getType() == NodeType.COMPLETE && root.getType() == NodeType.COMPLETE) ||
						(u.getType() == NodeType.LINEAR && root.getType() == NodeType.LINEAR)) &&
						u.getColor() == root.getColor())
					u.addChildren(root.getChildren());
				else
					u.addChild(root);
			}
			
			u = w;
		}
		
		return t;
	}
	
	private ComponentGraph computeGpp(ColoredGraph g, Set<BitSet> m, int v) {
		ComponentGraph cg = new ComponentGraph();
		int vertexid = 0;
		Map<String, Integer> repmap = new HashMap<String, Integer>();
		for (BitSet p : m) {
			Set<Integer> vertices = unpack(p);
			String label = g.createLabel(vertices);
			
			repmap.put(label, vertices.iterator().next());
			
			cg.addVertex(vertexid++, label, p);
		}
		
		for (String x: repmap.keySet()) {
			int _x = repmap.get(x); 
			if (_x == v) continue;
			for (String y: repmap.keySet()) {
				int _y = repmap.get(y); 
				if (_y == v || _x == _y) continue;
				if (g.distinguishes(_x, _y, v))
					cg.addEdge(x, y);
			}
		}
		
		cg.computeSCC();
		
		return cg;
	}
	
	public void traversePostOrder(MDTVisitor v) throws CannotStructureException {
		postOrder(v, root);
	}
	
	private void postOrder(MDTVisitor visitor, MDTNode node) throws CannotStructureException {
		if (node.getType() == NodeType.LEAF) {
			int mdtleaf = node.getValue().nextSetBit(0);
			visitor.visitLeaf(node, graph.getLabel(mdtleaf));
		} else {
			visitor.openContext(node);
			for (MDTNode child : node.getChildren())
				postOrder(visitor, child);
			if (node.getType() == NodeType.COMPLETE)
				visitor.visitComplete(node, node.getChildren(), node.getColor());
			else if (node.getType() == NodeType.LINEAR) {
				// Get set of child proxies
				Set<Integer> proxies = new HashSet<Integer>();
				Map<Integer, MDTNode> proxyMap = new HashMap<Integer, MDTNode>();
				for (MDTNode child: node.getChildren()) {
					proxies.add(child.getProxy());
					proxyMap.put(child.getProxy(), child);
				}
				// Get subgraph induced by proxies
				ColoredGraph subgraph = graph.subgraph(proxies);
				
				MDTNode ordered[] = new MDTNode[proxies.size()];
				for (Integer v: proxies) {
					int pos = proxies.size() - subgraph.postSet(v).size() - 1;
					ordered[pos] = proxyMap.get(v);
				}
								
				visitor.visitLinear(node, Arrays.asList(ordered));
			} else
				visitor.visitPrimitive(node, node.getChildren());
			visitor.closeContext(node);
		}
	}

	public ColoredGraph getGraph() {
		return graph;
	}
}
