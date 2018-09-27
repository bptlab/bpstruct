package ee.ut.bpstruct.eventstruct;

import java.util.HashMap;
import java.util.Map;

import de.hpi.bpt.graph.DirectedEdge;
import de.hpi.bpt.graph.DirectedGraph;
import de.hpi.bpt.graph.util.GMLUtils;
import de.hpi.bpt.hypergraph.abs.Vertex;
import ee.ut.graph.moddec.ColoredGraph;

public class OrderingRelationsGraph extends DirectedGraph {
	
	public OrderingRelationsGraph(ColoredGraph g, String name) {
		Map<Integer,Vertex> i2v = new HashMap<Integer,Vertex>();
		
		// map vertices
		for (Integer i : g.getVertices()) {
			Vertex v = new Vertex(g.getLabel(i));
			i2v.put(i,v);
			this.addVertex(v);
		}
		
		// map edges
		for (Integer i : g.getVertices()) {
			for (Integer j : g.getVertices()) {
				if (g.hasEdge(i,j))
					this.addEdge(i2v.get(i),i2v.get(j));
			}
		}
		
		System.err.println(this);
		// serialize
		GMLUtils<DirectedEdge,Vertex> gml = new GMLUtils<DirectedEdge,Vertex>();
		gml.serialize(this,"org_"+name+".gml");
	}
	
	public boolean areCausal(Vertex v1,Vertex v2) {
		DirectedEdge e1 = this.getDirectedEdge(v1,v2);
		DirectedEdge e2 = this.getDirectedEdge(v2,v1);
		return (e1!=null && e2==null);
	}
	
	public boolean areConcurrent(Vertex v1,Vertex v2) {
		DirectedEdge e1 = this.getDirectedEdge(v1,v2);
		DirectedEdge e2 = this.getDirectedEdge(v2,v1);
		return (e1==null && e2==null);
	}
	
	public boolean areInConflict(Vertex v1,Vertex v2) {
		DirectedEdge e1 = this.getDirectedEdge(v1,v2);
		DirectedEdge e2 = this.getDirectedEdge(v2,v1);
		return (e1!=null && e2!=null);
	}
}
