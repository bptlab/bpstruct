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
package ee.ut.comptech;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import de.bpt.hpi.graph.Edge;
import de.bpt.hpi.graph.Graph;
import ee.ut.comptech.DominatorTree.InfoNode;

public class DJGraph {

	enum DJEdgeType {DEdge, CJEdge, BJEdge};
	enum SPEdgeType {SPBack, SPTree, SPForward, SPCross};
	enum Color { GRAY, BLACK };

	Integer root;

	Graph graph;
	Map<Integer, List<Integer>> adjList;
	Map<Edge, DJEdgeType> djEdgeMap = new HashMap<Edge, DJEdgeType>();
	Map<Edge, SPEdgeType> spEdgeMap = new HashMap<Edge, SPEdgeType>();
	Map<Integer, Integer> level = new HashMap<Integer, Integer>();
	Map<Integer, Set<Integer>> rlevel = new HashMap<Integer, Set<Integer>>();

	public class CycleInfo {
		Set<Integer> entries;
		Set<Integer> exits;
		Set<Integer> loopbody;
		
		CycleInfo parent = null;
	}
	
	Map<Integer, CycleInfo> loopMap = new HashMap<Integer, CycleInfo>();
	Map<Integer, Integer> exit2loopMap = new HashMap<Integer, Integer>();

//	TreeMap<Integer, Set<Integer>> loopSizeMap = new TreeMap<Integer, Set<Integer>>();
	
	int loop = 0;
	
	public DJGraph(Graph graph, Map<Integer, List<Integer>> adjList, Integer root) {
		this.root = root;
		this.graph = graph.clone();
		this.adjList = adjList;
		DominatorTree domtree = new DominatorTree(adjList);
		domtree.analyse(root);		
		completeDJGraph(domtree);
		
		try {
			toDot(new PrintStream(new File("debug/djgraph.dot")));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Identify reducible and irreducible (multi-entry) loops using DJ Graphs.
	 * See: Sreedhar et al. "Identifying Loops Using DJ Graphs", TOPLAS 18(6):649-658, 1996
	 * @param helper
	 */
	public void identifyLoops(DJGraphHelper helper) {
		spanningTree(root, new HashMap<Integer, Color>());
		Set<Integer> vertices = new HashSet<Integer>();
		for (int i = rlevel.size() - 1; i >= 0; i--) {
			boolean irreducibleLoop = false;
			vertices.addAll(rlevel.get(i));
			for (Integer node: rlevel.get(i)) {
				for (Integer predecessor: graph.getPredecessorsOfVertex(node)) {
					Edge edge = new Edge(predecessor, node);
					if (djEdgeMap.get(edge) == DJEdgeType.CJEdge && spEdgeMap.get(edge) == SPEdgeType.SPBack)
						irreducibleLoop = true;
					else if (djEdgeMap.get(edge) == DJEdgeType.BJEdge) {
						// Handle REDUCIBLE LOOPS (Single entry): Both single-exit and multi-exit loops
						Integer entry = edge.getTarget();
						Integer inner = edge.getSource();
						Set<Integer> reachUnderSet = reachUnder(i, entry, inner);
						reachUnderSet.add(entry);
						
						Set<Edge> incoming = new HashSet<Edge>();
						Set<Edge> outgoing = new HashSet<Edge>();
						for (Integer n: reachUnderSet) {
							incoming.addAll(graph.getIncomingEdges(n));
							outgoing.addAll(graph.getOutgoingEdges(n));
							
						}
						
						Set<Edge> tmp = new HashSet<Edge>(incoming);
						incoming.removeAll(outgoing);
						outgoing.removeAll(tmp);
						
						Set<Integer> entries = new HashSet<Integer>();
						for (Edge e: incoming) entries.add(e.getTarget());
						Set<Integer> exits = new HashSet<Integer>();
						for (Edge e: outgoing) exits.add(e.getSource());
						System.out.println("Entries: " + entries.size());
						System.out.println("Exits: " + exits.size());

						CycleInfo info = new CycleInfo();
						info.entries = entries;
						info.exits = exits;
						info.loopbody = reachUnderSet;
						
						CycleInfo parent = null;
						
						for (Integer exit: exits)
							if (exit2loopMap.containsKey(exit)) {
								parent = loopMap.get(exit2loopMap.get(exit));
								break;
							}
						
						if (parent != null) {
							for (Integer exit: parent.exits)
								exit2loopMap.remove(exit);
							info.parent = parent;
						}
						for (Integer exit: info.exits)
							exit2loopMap.put(exit, loop);
						
						loopMap.put(loop, info);
						
						loop++;
						
						// Handle SEME loops
						helper.processSEME(reachUnderSet);
					}
				}
			}
			
			if (irreducibleLoop) {
				Graph subgraph = graph.subgraph(vertices);
				for (Set<Integer> scc: kosaraju(subgraph))
					if (scc.size() > 1)
						// Handle IRREDUCIBLE LOOPS
						helper.processMEME(scc);
			}
		}

		
		// ---------------------------------------------------
		// The following corresponds to a in-lined analysis !!!
		// ---------------------------------------------------		
//		for (Integer exit: exit2loopMap.keySet()) {
//			CycleInfo info = loopMap.get(exit2loopMap.get(exit));
//			
//			if (info.exits.size() > 1) {
//				System.err.println("Found multiple exit loop .... Cannot be restructured!");
//				System.exit(0);
//			}
//			
//			if (info.parent != null) {
//				System.out.println("Nested loop need to be processed!");
//				
//				System.exit(0);
//			}
//		}
	}

	private Set<Integer> reachUnder(int i, Integer entry, Integer inner) {
		Set<Integer> reachUnderSet = new HashSet<Integer>();
		Stack<Integer> worklist = new Stack<Integer>();
		worklist.push(inner);
		while (!worklist.isEmpty()) {
			Integer curr = worklist.pop();
			reachUnderSet.add(curr);
			for (Integer pred: graph.getPredecessorsOfVertex(curr)) {
				if (level.get(pred) >= i && !reachUnderSet.contains(pred) && pred != entry)
					worklist.push(pred);
			}
		}
		return reachUnderSet;
	}

	/**
	 * Given a clone of the original graph and the corresponding Dominance Tree, this method
	 * adds some additional edges to complete the DJ Graph. Besides, it classifies edges as
	 *      - DEdge (dominance tree edge),
	 *      - BJEdge (backward J edge), and
	 *      - CJEdge (cross J Edge).
	 * @param domtree
	 */
	private void completeDJGraph(DominatorTree domtree) {
		for (InfoNode v: domtree.vertex)
			if (v.dom != null) {
				Edge edge = new Edge(v.dom.node, v.node);
				if (!graph.getEdges().contains(new Edge(v.dom.node, v.node))) {
					graph.addEdge(v.dom.node, v.node);
				}
				djEdgeMap.put(edge, DJEdgeType.DEdge);
			}

		Queue<Integer> queue = new LinkedList<Integer>();
		queue.offer(root);
		level.put(root, 0);
		Set<Integer> l0 = new HashSet<Integer>();
		l0.add(root);
		rlevel.put(0, l0);
		while (!queue.isEmpty()) {
			Integer curr = queue.poll();
			for (Integer succ: graph.getSuccessorsOfVertex(curr)) {
				Edge edge = new Edge(curr, succ);
				if (djEdgeMap.get(edge) == DJEdgeType.DEdge) {
					queue.offer(succ);   // Dominance relation results in a tree
					int l = level.get(curr) + 1;
					level.put(succ, l);
					Set<Integer> siblings = rlevel.get(l);
					if (siblings == null) rlevel.put(l, siblings = new HashSet<Integer>());
					siblings.add(succ);
				}
			}
		}

		for (Integer source: graph.getVertices()) {
			for (Integer target: graph.getSuccessorsOfVertex(source)) {
				Edge edge = new Edge(source, target);
				if (djEdgeMap.get(edge) == DJEdgeType.DEdge) continue;
				if (level.get(edge.getSource()) > level.get(edge.getTarget()) &&
						dominates(edge.getTarget(), edge.getSource(), domtree))
						//djEdgeMap.get(new Edge(edge.getTarget(), edge.getSource())) == DJEdgeType.DEdge)
					djEdgeMap.put(edge, DJEdgeType.BJEdge);
				else
					djEdgeMap.put(edge, DJEdgeType.CJEdge);
			}
		}
	}

	private boolean dominates(Integer v1, Integer v2, DominatorTree domtree) {
		InfoNode _v1 = domtree.map.get(v1);
		InfoNode _v2 = domtree.map.get(v2);
		
		while (_v2 != _v1 && _v2 != null) _v2 = _v2.dom;
		
		return _v2 == _v1;
	}

	/**
	 * Performs a depth first traversal to classify edges in the DJ Graph as:
	 *     - SPBack      Backward edge in the Spanning tree
	 *     - SPForward   Forward edge
	 *     
	 * TODO: Complete the labeling (required?)
	 *     - SPTree
	 *     - SPCross
	 * @param v
	 * @param colorMap
	 */
	private void spanningTree(Integer v, Map<Integer, Color> colorMap) {
		colorMap.put(v, Color.GRAY);

		for (Integer next : graph.getSuccessorsOfVertex(v)) {
			if (!colorMap.containsKey(next)) {
				spanningTree(next, colorMap);
			}
			else if (colorMap.get(next) == Color.GRAY) {
				spEdgeMap.put(new Edge(v, next), SPEdgeType.SPBack);
			} else {
				spEdgeMap.put(new Edge(v, next), SPEdgeType.SPForward);
			}
		}
		colorMap.put(v, Color.BLACK);
	}

	public DJEdgeType djEdgeType(Edge e) {
		return djEdgeMap.get(e);
	}
	
	
	
	private Set<Set<Integer>> kosaraju(Graph graph) {
		Set<Set<Integer>> scc = new HashSet<Set<Integer>>();
		Stack<Integer> stack = new Stack<Integer>();
		Set<Integer> visited = new HashSet<Integer>();
		for (Integer vertex: graph.getVertices())
			if (!visited.contains(vertex))
				searchForward(graph, vertex, stack, visited);			

		//graph.toDot(System.out);
		visited.clear();
		while(!stack.isEmpty()) {
			Set<Integer> component = new HashSet<Integer>();
			searchBackward(graph, stack.peek(), visited, component);
			scc.add(component);
			stack.removeAll(component);
		}
		return scc;
	}

	private void searchBackward(Graph graph, Integer node, Set<Integer> visited, Set<Integer> component) {
		Stack<Integer> worklist = new Stack<Integer>();
		worklist.push(node);
		while (!worklist.isEmpty()) {
			Integer curr = worklist.pop();
			visited.add(curr);
			component.add(curr);
			for (Integer pred: graph.getPredecessorsOfVertex(curr))
				if (!visited.contains(pred) && !worklist.contains(pred))
					worklist.add(pred);
		}
	}

	private void searchForward(Graph graph, Integer curr, Stack<Integer> stack, Set<Integer> visited) {
		visited.add(curr);
		for (Integer succ: graph.getSuccessorsOfVertex(curr))
			if (!visited.contains(succ))
				searchForward(graph, succ, stack, visited);
		stack.push(curr);
	}

	
	public void toDot(PrintStream out) {
		toDot(graph, out);
	}
	
	private void toDot(Graph g, PrintStream out) {
		out.println("digraph G {");
//		for (int i = 0; i < rlevel.size(); i++) {
//			out.println("\tsubgraph cluster" + i + " {");
//			for (Integer node: rlevel.get(i))
//				out.printf("\t\t%s;\n", g.getLabel(node));
//			out.println("\t}");
//		}
		out.println("{\n\tnode [shape=plaintext];");
		out.printf("\tlevel0 ");
		for (int i = 1; i < rlevel.size(); i++) {
			out.printf("-> level%d ", i);
		}
		out.println(";\n\tnode [shape=ellipse];");
		for (Integer node: g.getVertices())
			out.printf("\tn%d[label=\"%s\"];\n", node, g.getLabel(node).replaceAll("\"", ""));
		out.println("}");

		for (int i = 0; i < rlevel.size(); i++) {
			out.printf("\t{rank = same; level%d; ", i);
			for (Integer node: rlevel.get(i))
				out.printf("n%d; ", node);
			out.println("}");
		}
		for (Integer source: g.getVertices()) {
			for (Integer target: g.getSuccessorsOfVertex(source)) {
				out.printf("\tn%d -> n%d ", source, target);
				Edge e = new Edge(source, target);
				if (djEdgeType(e) == DJEdgeType.DEdge)
					out.println("[style=dashed];");
				else if (djEdgeType(e) == DJEdgeType.BJEdge)
					out.println("[style=solid];");
				else
					out.println("[style=solid,arrowhead=dot];");
			}
		}
		out.println("}");
	}

}
