package ee.ut.bpstruct.jbpt;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import de.hpi.bpt.process.Node;

public class PlaceHolder extends Node {
	Set<Pair> edges;
	Set<Node> vertices;
	Node entry, exit;
	public PlaceHolder(Collection<Pair> edges,
			Collection<Node> vertices, Node entry, Node exit) {
		this.edges = new HashSet<Pair>(edges);
		this.vertices = new HashSet<Node>(vertices);
		this.entry = entry;
		this.exit = exit;
	}

	public Set<Pair> getEdges() {
		return edges;
	}
	public Set<Node> getVertices() {
		return vertices;
	}
	public Node getEntry() {
		return entry;
	}
	public Node getExit() {
		return exit;
	}
	
	public Object clone() {
		return new PlaceHolder(edges, vertices, entry, exit);
	}
}
