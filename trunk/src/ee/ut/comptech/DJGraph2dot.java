package ee.ut.comptech;

import java.io.PrintStream;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Stack;

import de.bpt.hpi.graph.Edge;
import de.bpt.hpi.graph.Graph;
import ee.ut.comptech.DJGraph.DJEdgeType;
import ee.ut.comptech.Processor.Node;

public class DJGraph2dot {
	public static void printDJGraph(DJGraph djgraph, PrintStream out) {
		Graph g = djgraph.graph;
		out.println("digraph G {");
		for (Integer source: g.getVertices()) {
			for (Integer target: g.getSuccessorsOfVertex(source)) {
				out.printf("\t%s -> %s ", g.getLabel(source), g.getLabel(target));
				Edge e = new Edge(source, target);
				if (djgraph.djEdgeType(e) == DJEdgeType.DEdge)
					out.println("[style=dashed];");
				else if (djgraph.djEdgeType(e) == DJEdgeType.BJEdge)
					out.println("[style=solid];");
				else
					out.println("[style=solid,arrowshape=dot];");
			}
		}
		out.println("}");
	}

	public static void printRestructured(Processor proc, PrintStream out) {
		Node entry = proc.getStart();
		Stack<Node> worklist = new Stack<Node>();
		Set<Node> visited = new LinkedHashSet<Node>();
		worklist.push(entry);
		out.println("digraph G {");
		while (!worklist.isEmpty()) {
			Node curr = worklist.pop();
			visited.add(curr);
			for (Node succ: curr.getSuccs()) {
				out.printf("\t%s -> %s;\n", curr.label, succ.label);
				if (!visited.contains(succ) && !worklist.contains(succ))
					worklist.push(succ);
			}
		}
		out.println("}");
	}

	public static void printDJGraph(DJGraph djgraph, Graph g,
			PrintStream out) {
		out.println("digraph G {");
		for (Integer source: g.getVertices()) {
			for (Integer target: g.getSuccessorsOfVertex(source)) {
				out.printf("\t%s -> %s ", g.getLabel(source), g.getLabel(target));
				Edge e = new Edge(source, target);
				if (djgraph.djEdgeType(e) == DJEdgeType.DEdge)
					out.println("[style=dashed];");
				else if (djgraph.djEdgeType(e) == DJEdgeType.BJEdge)
					out.println("[style=solid];");
				else
					out.println("[style=solid,arrowshape=dot];");
			}
		}
		out.println("}");
	}
}
