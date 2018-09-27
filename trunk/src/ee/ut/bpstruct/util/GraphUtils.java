package ee.ut.bpstruct.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import de.hpi.bpt.graph.abs.AbstractDirectedEdge;
import de.hpi.bpt.hypergraph.abs.Vertex;
import de.hpi.bpt.process.Node;
import de.hpi.bpt.process.petri.PetriNet;
import ee.ut.bpstruct.jbpt.PNPair;
import ee.ut.bpstruct.jbpt.Pair;

public class GraphUtils {
	/**
	 * This method takes a set of edges and builds a adjacency list representation. This is required
	 * by some DFS-based methods (e.g. DFSLabeler). Note that the structure of the graph is modified,
	 * by adding/deleting edges in the set of edges "edges".
	 */
	public static Map<Node, List<Node>> edgelist2adjlist(Set<Pair> ledges,
			Node exit) {
		Map<Node, List<Node>> adjList = new HashMap<Node, List<Node>>();
		for (Pair e: ledges) {
			List<Node> list = adjList.get(e.getSource());
			if (list == null) {
				list = new LinkedList<Node>();
				adjList.put(e.getSource(), list);
			}
			list.add(e.getTarget());
		}
		if (exit != null && adjList.get(exit) == null)
			adjList.put(exit, new LinkedList<Node>());
		return adjList;
	}
	
	public static Map<Vertex, List<Vertex>> edgelist2adjlist(Set<PNPair> ledges, Vertex exit2) {
		Map<Vertex, List<Vertex>> adjList = new HashMap<Vertex, List<Vertex>>();
		for (PNPair e: ledges) {
			List<Vertex> list = adjList.get(e.getSource());
			if (list == null) {
				list = new LinkedList<Vertex>();
				adjList.put(e.getSource(), list);
			}
			list.add(e.getTarget());
		}
		if (exit2 != null && adjList.get(exit2) == null)
			adjList.put(exit2, new LinkedList<Vertex>());
		return adjList;
	}

	public static Set<Set<Vertex>> computeSCCs(PetriNet rewiredUnfGraph) {
		Set<Set<Vertex>> scc = new HashSet<Set<Vertex>>();
		Stack<Vertex> stack = new Stack<Vertex>();
		Set<Vertex> visited = new HashSet<Vertex>();
		for (Vertex vertex: rewiredUnfGraph.getVertices())
			if (!visited.contains(vertex))
				searchForward(rewiredUnfGraph, vertex, stack, visited);			

		visited.clear();
		while(!stack.isEmpty()) {
			Set<Vertex> component = new HashSet<Vertex>();
			searchBackward(rewiredUnfGraph, stack.peek(), visited, component);
			scc.add(component);
			stack.removeAll(component);
		}
		return scc;
	}

	private static void searchBackward(PetriNet rewiredUnfGraph, Vertex node, Set<Vertex> visited, Set<Vertex> component) {
		Stack<Vertex> worklist = new Stack<Vertex>();
		worklist.push(node);
		while (!worklist.isEmpty()) {
			Vertex curr = worklist.pop();
			visited.add(curr);
			component.add(curr);
			for (Vertex pred: rewiredUnfGraph.getPredecessors((de.hpi.bpt.process.petri.Node) curr))
				if (!visited.contains(pred) && !worklist.contains(pred))
					worklist.add(pred);
		}
	}

	private static void searchForward(PetriNet rewiredUnfGraph, Vertex curr, Stack<Vertex> stack, Set<Vertex> visited) {
		visited.add(curr);
		for (Vertex succ: rewiredUnfGraph.getSuccessors((de.hpi.bpt.process.petri.Node) curr))
			if (!visited.contains(succ))
				searchForward(rewiredUnfGraph, succ, stack, visited);
		stack.push(curr);
	}
	
	public static void gatherControlFlow(Set<AbstractDirectedEdge<Node>> edges,
			Node entry, Node exit,
			Map<Node, List<AbstractDirectedEdge<Node>>> incoming,
			Map<Node, List<AbstractDirectedEdge<Node>>> outgoing) {
		for (AbstractDirectedEdge<Node> e: edges) {
			List<AbstractDirectedEdge<Node>> in = incoming.get(e.getSource());
			if (in == null) {
				in = new LinkedList<AbstractDirectedEdge<Node>>();
				incoming.put(e.getSource(), in);
			}
			
			List<AbstractDirectedEdge<Node>> out = outgoing.get(e.getTarget());
			if (out == null) {
				out = new LinkedList<AbstractDirectedEdge<Node>>();
				outgoing.put(e.getTarget(), out);
			}			
			in.add(e);
			out.add(e);
		}
		if (entry != null && incoming.get(entry) == null)
			incoming.put(entry, new LinkedList<AbstractDirectedEdge<Node>>());
		if (exit != null && outgoing.get(exit) == null)
			outgoing.put(exit, new LinkedList<AbstractDirectedEdge<Node>>());		
	}
}
