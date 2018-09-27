package ee.ut.bpstruct;

import java.util.Set;

import de.bpt.hpi.graph.Edge;
import de.bpt.hpi.graph.Graph;

public interface Visitor {
	void visitRootSNode(Graph graph, Set<Edge> edges,
			Set<Integer> vertices, Integer entry, Integer exit);
	void visitSNode(Graph graph, Set<Edge> edges,
			Set<Integer> vertices, Integer entry, Integer exit);
	void visitPNode(Graph graph, Set<Edge> edges,
			Set<Integer> vertices, Integer entry, Integer exit);
	void visitRNode(Graph graph, Set<Edge> edges,
			Set<Integer> vertices, Integer entry, Integer exit) throws CannotStructureException;
}
