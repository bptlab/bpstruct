package ee.ut.bpstruct.eventstruct;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import de.hpi.bpt.graph.DirectedEdge;
import de.hpi.bpt.graph.DirectedGraph;
import de.hpi.bpt.graph.util.GMLUtils;
import de.hpi.bpt.hypergraph.abs.Vertex;

public class POSet extends DirectedGraph {
	
	public class Event extends HashSet<Vertex>{
		private static final long serialVersionUID = 1L;

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof Event)) return false;
			Event that = (Event) o;
			return this.hashCode()==that.hashCode();
		}

		@Override
		public int hashCode() {
			int result = 0;
			for (Vertex v : this) result += v.hashCode();
			return result;
		}
	}
	
	public POSet(OrderingRelationsGraph org, String name) {
		// create root
		Set<Event> poset = new HashSet<Event>();
		
		Event root = new Event();
		poset.add(root);
		Queue<Event> queue = new ConcurrentLinkedQueue<Event>();
		queue.add(root);
		
		// construct poset
		while (queue.size()>0) {
			Event d1 = queue.poll();
			for (Vertex v : org.getVertices()) {
				if (d1.contains(v)) continue;
				Event d2 = new Event();
				d2.addAll(d1);
				d2.add(v);
				//if (poset.contains(d2)) continue;
				
				// check (d1,d2) pair
				if ((d1.isEmpty() || isPossibleExtension(org, d1, v)) && isConflictFreeExtension(org, d1, v)) {
					poset.add(d2);
					queue.add(d2);
				}
			}
		}
		
		// augment poset
		Vertex i = new Vertex("_I_");
		Vertex o = new Vertex("_O_");
		for (Event e : poset) {
			e.add(i);
		}
		
		poset.add(new Event()); // new root
		
		Set<Event> max = new HashSet<Event>();
		for (Event e1 : poset) {
			
			boolean flag = true;
			for (Event e2 : poset) {
				if (e1.equals(e2)) continue;
				if (e2.containsAll(e1)) { flag = false; break; }
			}
			
			if (flag) max.add(e1); 
		}
		for (Event e : max) {
			Event maxE = new Event();
			maxE.addAll(e);
			maxE.add(o);
			poset.add(maxE);
		}
			
		// construct graph
		Map<Event,Vertex> s2v = new HashMap<Event,Vertex>();
		for (Event src : poset) {
			for (Event tgt : poset) {
				if ((src.size()+1 == tgt.size()) && tgt.containsAll(src)) {
					Vertex s = s2v.get(src);
					Vertex t = s2v.get(tgt);
					if (s==null) {
						s = new Vertex(src.toString());
						s.setTag(src);
						s2v.put(src,s);
					}
					if (t==null) {
						t = new Vertex(tgt.toString());
						t.setTag(tgt);
						s2v.put(tgt,t);
					}
					
					this.addEdge(s,t);
				}
			}
		}
		
		// serialize
		GMLUtils<DirectedEdge,Vertex> gml = new GMLUtils<DirectedEdge,Vertex>();
		gml.serialize(this,"poset_"+name+".gml");
	}
	
	// Exists a in d1 : a->v or a||v
	private boolean isPossibleExtension(OrderingRelationsGraph org, Event d1, Vertex v) {
		for (Vertex a : d1) {
			if (org.areCausal(a,v) || org.areConcurrent(a,v))
				return true;
		}
		
		return false;
	}
	
	private boolean isConflictFreeExtension(OrderingRelationsGraph org, Event d1, Vertex v) {
		for (Vertex x : d1) {
			if (org.areInConflict(x,v))
				return false;
		}
		
		for (Vertex b : org.getVertices()) {
			if (org.areCausal(b,v) && !d1.contains(b))
			{
				boolean flag = false;
				for (Vertex c : d1) {
					if (org.areInConflict(b,c)) { 
						flag = true;
					}
				}
				
				if (!flag) return false;
			}
		}
		return true;
	}
}
