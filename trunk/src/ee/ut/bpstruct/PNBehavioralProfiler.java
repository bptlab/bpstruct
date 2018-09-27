/* 
 * Copyright (C) 2010 - Luciano Garcia Banuelos, Artem Polyvyanyy
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
package ee.ut.bpstruct;


import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.hpi.bpt.hypergraph.abs.Vertex;
import de.hpi.bpt.process.petri.Place;
import de.hpi.bpt.process.petri.Transition;
import ee.ut.graph.moddec.ColoredGraph;

public class PNBehavioralProfiler {
	
	enum OrderingRelation {NONE, PRECEDENCE, CONFLICT, CONCURRENCY};
	/**
	 * Matrix containing the ordering relations of the events
	 */
	OrderingRelation[][] eventRels = null;
	ColoredGraph orgraph = null;
	Map<Vertex, Integer> entryMap = new LinkedHashMap<Vertex, Integer>();
//	List<Vertex> entries = new LinkedList<Vertex>();
	Map<Integer, String> map = new HashMap<Integer, String>();
	private Vertex entry;
	private Map<Vertex, List<Vertex>> incoming;
	private Map<Vertex, List<Vertex>> outgoing;
	
	public PNBehavioralProfiler(Map<Vertex, List<Vertex>> incoming, Map<Vertex, List<Vertex>> outgoing, Vertex entry, Map<String, Vertex> tasks, Map<String, Vertex> clones) {
		this.incoming = incoming;
		this.outgoing = outgoing;
		this.entry = entry;
		
		int index = 0;
		for (Vertex v: incoming.keySet()) {
			if (v instanceof Transition) {
				entryMap.put(v, index);
				String label = v.getName();
				if (tasks.containsKey(label))
					map.put(index, label);
				index++;
			}
		}
		
		computePrefixRelations();
		computeOrderingRelationsGraph(map);
	}

	public String serializeOrderRelationMatrix() {
		ByteArrayOutputStream barray = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(barray);
		
		for (int i: map.keySet()) {
			out.printf("%3d ", i);
			for (int j: map.keySet()) {
				if (eventRels[i][j] == OrderingRelation.CONCURRENCY)
					out.print(" @");
				else if (eventRels[i][j] == OrderingRelation.CONFLICT)
					out.print(" #");
				else if (eventRels[i][j] == OrderingRelation.PRECEDENCE)
					out.print(" <");
				else
					out.print(" _");
			}
			out.println();
		}
		return barray.toString();
	}
		
	private void computeOrderingRelationsGraph(Map<Integer, String> map) {
		orgraph = new ColoredGraph();
		
		// add vertices
		for (String label: map.values())
			orgraph.addVertex(label);
		
		// add edges
		for (int i = 0; i < eventRels.length; i++) {
			if (!map.containsKey(i)) continue;
			for (int j = i + 1; j < eventRels.length; j++) {
				if (!map.containsKey(j)) continue;
				if (eventRels[i][j] == OrderingRelation.CONCURRENCY)
					;
				else if (eventRels[i][j] == OrderingRelation.CONFLICT) {
					orgraph.addEdge(map.get(i), map.get(j));
					orgraph.addEdge(map.get(j), map.get(i));
				}
				else if (eventRels[i][j] == OrderingRelation.PRECEDENCE)
					orgraph.addEdge(map.get(i), map.get(j));
				else if (eventRels[j][i] == OrderingRelation.PRECEDENCE)
					orgraph.addEdge(map.get(j), map.get(i));
			}
		}
	}

	public ColoredGraph getOrderingRelationsGraph() {
		return orgraph;
	}

	
	// ------------------------------------------------------------------------
	// It might be preferable to use a topological order approach to simplify all loops
	// used for updating/propagating ordering relations.
	
	/**
	 * Computes ordering relations of all events in a Complete Prefix brprocolding
	 * (This method implements the first phase in Algorithm 1).
	 * 
	 * @param brproc	The complete prefix brprocolding
	 */
	private void computePrefixRelations() {
		// STEP 1: Initialize all ordering relations to CONCURRENCY
		eventRels = new OrderingRelation[entryMap.size()][entryMap.size()];
		for (int i = 0; i < eventRels.length; i++)
			for (int j = 0; j < eventRels.length; j++)
				eventRels[i][j] = OrderingRelation.CONCURRENCY;
				
		// STEP 2
		//   - Outer-most loop: Traverse the brprocolding using a pre-order DFS strategy.
		//   - Nested loops are implemented in updateEventRelations method.
		HashSet<Vertex> visited = new HashSet<Vertex>();
		LinkedList<Vertex> worklist = new LinkedList<Vertex>();
		
//		Vertex entry = pnet.getInitialConditions().get(0);
		worklist.add(entry);
		while (!worklist.isEmpty()) {
			Vertex curr = worklist.removeFirst();
			visited.add(curr);
			
			if (curr instanceof Place) {
				for (Vertex succ: outgoing.get(curr))
					if (!worklist.contains(succ) && !visited.contains(succ))
						worklist.addFirst(succ);
			} else {
				if (visited.containsAll(incoming.get(curr))) {
					updateEventRelations(curr);					// Critical stuff !!
					for (Vertex succ: outgoing.get(curr))
						if (!worklist.contains(succ) && !visited.contains(succ))
							worklist.addFirst(succ);
				} else
					worklist.addLast(curr);
			}
		}
	}
	
	/**
	 * It updates the ordering relations for given event (nested loops of phase 2 in Algorithm 1).
	 * (This method is called from computePrefixRelations).
	 * 
	 * @param ev_i   The event for which ordering relations are computed/updated
	 */
	private void updateEventRelations(Vertex ev_i) {
		for (Vertex cond: incoming.get(ev_i)) {
			if (incoming.get(cond).size() != 0) {
				Vertex ev_j = incoming.get(cond).get(0);
				eventRels[entryMap.get(ev_j)][entryMap.get(ev_i)] = OrderingRelation.PRECEDENCE;
				eventRels[entryMap.get(ev_i)][entryMap.get(ev_j)] = OrderingRelation.NONE;		
				for (int k = 0; k < eventRels.length; k++) {
					if (eventRels[entryMap.get(ev_j)][k] == OrderingRelation.NONE) {
						eventRels[k][entryMap.get(ev_i)] = OrderingRelation.PRECEDENCE;
						eventRels[entryMap.get(ev_i)][k] = OrderingRelation.NONE;
					}
					if (eventRels[entryMap.get(ev_j)][k] == OrderingRelation.CONFLICT) {
						eventRels[k][entryMap.get(ev_i)] = OrderingRelation.CONFLICT;
						eventRels[entryMap.get(ev_i)][k] = OrderingRelation.CONFLICT;					
					}
				}
			}
			for (Vertex ev_j: outgoing.get(cond)) {
				if (ev_j != ev_i && entryMap.get(ev_j) != null && entryMap.get(ev_i) != null) {
					eventRels[entryMap.get(ev_j)][entryMap.get(ev_i)] = OrderingRelation.CONFLICT;
					eventRels[entryMap.get(ev_i)][entryMap.get(ev_j)] = OrderingRelation.CONFLICT;
					for (int k = 0; k < eventRels.length; k++) {
						if (entryMap.get(ev_i) != k && eventRels[entryMap.get(ev_j)][k] == OrderingRelation.PRECEDENCE) {
							eventRels[k][entryMap.get(ev_i)] = OrderingRelation.CONFLICT;
							eventRels[entryMap.get(ev_i)][k] = OrderingRelation.CONFLICT;		
						}
					}
				}
			}
		}
	}	
}
