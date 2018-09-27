package ee.ut.graph.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.google.gwt.dev.util.collect.HashSet;

import de.bpt.hpi.graph.Edge;
import de.bpt.hpi.graph.Graph;

public class GraphUtils {
	/**
	 * This method takes a set of edges and builds a adjacency list representation. This is required
	 * by some DFS-based methods (e.g. DFSLabeler). Note that the structure of the graph is modified,
	 * by adding/deleting edges in the set of edges "edges".
	 */
	public static Map<Integer, List<Integer>> edgelist2adjlist(Set<Edge> edges,
			Integer exit) {
		Map<Integer, List<Integer>> adjList = new HashMap<Integer, List<Integer>>();
		for (Edge e: edges) {
			List<Integer> list = adjList.get(e.getSource());
			if (list == null) {
				list = new LinkedList<Integer>();
				adjList.put(e.getSource(), list);
			}
			list.add(e.getTarget());
		}
		if (exit != null && adjList.get(exit) == null)
			adjList.put(exit, new LinkedList<Integer>());
		return adjList;
	}

	/**
	 * This method takes a set of edges and builds a adjacency list representation. This is required
	 * by some DFS-based methods (e.g. DFSLabeler). Note that the structure of the graph is modified,
	 * by adding/deleting edges in the set of edges "edges".
	 */
	public static Map<Integer, List<Integer>> edgelist2adjlist(Set<Edge> edges,
			Set<Integer> exits) {
		Map<Integer, List<Integer>> adjList = new HashMap<Integer, List<Integer>>();
		for (Edge e: edges) {
			List<Integer> list = adjList.get(e.getSource());
			if (list == null) {
				list = new LinkedList<Integer>();
				adjList.put(e.getSource(), list);
			}
			list.add(e.getTarget());
		}
		for (Integer exit: exits)
			adjList.put(exit, new LinkedList<Integer>());
		return adjList;
	}

	public static Set<Set<Integer>> computeSCCs(Graph graph) {
		Set<Set<Integer>> scc = new HashSet<Set<Integer>>();
		Stack<Integer> stack = new Stack<Integer>();
		Set<Integer> visited = new HashSet<Integer>();
		for (Integer vertex: graph.getVertices())
			if (!visited.contains(vertex))
				searchForward(graph, vertex, stack, visited);			

		visited.clear();
		while(!stack.isEmpty()) {
			Set<Integer> component = new HashSet<Integer>();
			searchBackward(graph, stack.peek(), visited, component);
			scc.add(component);
			stack.removeAll(component);
		}
		return scc;
	}

	private static void searchBackward(Graph graph, Integer node, Set<Integer> visited, Set<Integer> component) {
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

	private static void searchForward(Graph graph, Integer curr, Stack<Integer> stack, Set<Integer> visited) {
		visited.add(curr);
		for (Integer succ: graph.getSuccessorsOfVertex(curr))
			if (!visited.contains(succ))
				searchForward(graph, succ, stack, visited);
		stack.push(curr);
	}


}
