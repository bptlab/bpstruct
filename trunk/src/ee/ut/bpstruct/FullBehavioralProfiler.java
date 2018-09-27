/* 
 * Copyright (C) 2011 - Luciano Garcia Banuelos, Artem Polyvyanyy
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

import hub.top.petrinet.Place;
import hub.top.uma.DNode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ee.ut.graph.moddec.ColoredGraph;

public class FullBehavioralProfiler {
	
	enum OrderingRelation {NONE, PRECEDENCE, CONFLICT, CONCURRENCY};
	enum SoundnessStatus {SOUND, UNSAFE, DEADLOCKED, UNSAFE_DEADLOCKED}; 
	/**
	 * Matrix containing the ordering relations of the events
	 */
	OrderingRelation[][] eventRels = null;
	ColoredGraph orgraph = null;
	Unfolding unf = null;
	Map<DNode, Integer> entryMap = new LinkedHashMap<DNode, Integer>();
	List<DNode> entries = new LinkedList<DNode>();
	Map<Integer, String> map = new HashMap<Integer, String>();
	
	HashMap<DNode, Set<DNode>> unboundedConds = null;
	Set<DNode> localdeadlocks = null;

	public FullBehavioralProfiler(Unfolding unf) {
		this.unf = unf;		
	}
	

	
	// ------------------------------------------------------------------------
	// It might be preferable to use a topological order approach to simplify all loops
	// used for updating/propagating ordering relations.
	
	/**
	 * Computes ordering relations of all events in a Complete Prefix Unfolding
	 * (This method implements the first phase in Algorithm 1).
	 * 
	 * @param brproc	The complete prefix unfolding
	 */
	private void computePrefixRelations() {
		int nnodes = unf.getAllEvents().size() + unf.getAllConditions().size();
		// STEP 1: Initialize all ordering relations to CONCURRENCY
		eventRels = new OrderingRelation[nnodes][nnodes];
		
		for (int i = 0; i < nnodes; i++)
			for (int j = 0; j < nnodes; j++)
				eventRels[i][j] = OrderingRelation.CONCURRENCY;
		
		int index = 0;
		for (DNode node: unf.getAllEvents()) {
			entryMap.put(node, index++);
			entries.add(node);
		}
		for (DNode node: unf.getAllConditions()) {
			entryMap.put(node, index++);
			entries.add(node);
		}
		
		// STEP 2
		//   - Outer-most loop: Traverse the Unfolding using a pre-order DFS strategy.
		//   - Nested loops are implemented in updateEventRelations method.
		HashSet<DNode> visited = new HashSet<DNode>();
		LinkedList<DNode> worklist = new LinkedList<DNode>();
		
		DNode entry = unf.getInitialConditions().get(0);
		worklist.add(entry);
		while (!worklist.isEmpty()) {
			DNode node = worklist.removeFirst();
			visited.add(node);
			
			if (node.isEvent) {
				if (visited.containsAll(Arrays.asList(node.pre)))
					updateEventRelationsT(node);					// Critical stuff !!
				else {
					worklist.addLast(node);
					continue;
				}
			} else
				updateEventRelationsP(node);					// Critical stuff !!
			if (node.post != null)
				for (DNode succ: node.post)
					worklist.addFirst(succ);
		}
	}
	
	/**
	 * It updates the ordering relations for given event (nested loops of phase 2 in Algorithm 1).
	 * (This method is called from computePrefixRelations).
	 * 
	 * @param ev_i   The event for which ordering relations are computed/updated
	 */
	private void updateEventRelationsT(DNode ev_i) {
		for (DNode cond: ev_i.pre) {
			if (cond.pre.length != 0) {
				DNode ev_j = cond.pre[0];
				eventRels[entryMap.get(ev_j)][entryMap.get(ev_i)] = OrderingRelation.PRECEDENCE;
				eventRels[entryMap.get(ev_i)][entryMap.get(ev_j)] = OrderingRelation.NONE;	
				for (DNode ev_k: entries)
					if (ev_k.isEvent) {
						int k = entryMap.get(ev_k);
//				for (int k = 0; k < eventRels.length; k++) {
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
			for (DNode ev_j: cond.post) {
				if (ev_j != ev_i && entryMap.get(ev_j) != null && entryMap.get(ev_i) != null) {
					eventRels[entryMap.get(ev_j)][entryMap.get(ev_i)] = OrderingRelation.CONFLICT;
					eventRels[entryMap.get(ev_i)][entryMap.get(ev_j)] = OrderingRelation.CONFLICT;
					for (DNode ev_k: entries)
						if (ev_k.isEvent) {
							int k = entryMap.get(ev_k);

//					for (int k = 0; k < eventRels.length; k++) {
						if (entryMap.get(ev_i) != k && eventRels[entryMap.get(ev_j)][k] == OrderingRelation.PRECEDENCE) {
							eventRels[k][entryMap.get(ev_i)] = OrderingRelation.CONFLICT;
							eventRels[entryMap.get(ev_i)][k] = OrderingRelation.CONFLICT;		
						}
					}
				}
			}
		}
	}
	
	/**
	 * It updates the ordering relations for given event (nested loops of phase 2 in Algorithm 1).
	 * (This method is called from computePrefixRelations).
	 * 
	 * @param ev_i   The event for which ordering relations are computed/updated
	 */
	private void updateEventRelationsP(DNode ev_i) {
		if (ev_i.post == null) return;
		Set<DNode> postset = new HashSet<DNode>();
		for (DNode succ: ev_i.post) postset.add(succ);
		for (DNode c_r: ev_i.post) {
			for (DNode c_j: ev_i.pre) {
				eventRels[entryMap.get(c_j)][entryMap.get(c_r)] = OrderingRelation.PRECEDENCE;
				eventRels[entryMap.get(c_r)][entryMap.get(c_j)] = OrderingRelation.NONE;
				for (DNode c_k: entries)
					if (!c_k.isEvent) {
						if (eventRels[entryMap.get(c_k)][entryMap.get(c_j)] == OrderingRelation.PRECEDENCE) {
							eventRels[entryMap.get(c_k)][entryMap.get(c_r)] = OrderingRelation.PRECEDENCE;
							eventRels[entryMap.get(c_r)][entryMap.get(c_k)] = OrderingRelation.NONE;
						} else if (eventRels[entryMap.get(c_k)][entryMap.get(c_j)] == OrderingRelation.CONFLICT) {
							eventRels[entryMap.get(c_k)][entryMap.get(c_r)] = OrderingRelation.CONFLICT;
							eventRels[entryMap.get(c_r)][entryMap.get(c_k)] = OrderingRelation.CONFLICT;
						} else if (eventRels[entryMap.get(c_j)][entryMap.get(c_k)] == OrderingRelation.PRECEDENCE &&
								!postset.contains(c_k)) {
							eventRels[entryMap.get(c_k)][entryMap.get(c_r)] = OrderingRelation.CONFLICT;
							eventRels[entryMap.get(c_r)][entryMap.get(c_k)] = OrderingRelation.CONFLICT;
						}
					}
			}
		}
	}

	private void analyze(Set<Place> sinks) {
		
		HashMap<DNode, Set<DNode>> concconds = unf.getConcurrentConditions();
		LinkedList<DNode> maxNodes = new LinkedList<DNode>(unf.getCurrentMaxNodes());
		unboundedConds = new HashMap<DNode, Set<DNode>>();
		localdeadlocks = new HashSet<DNode>();

		Set<String> sinkNames = new HashSet<String>();
		for (Place sink: sinks)
			sinkNames.add(sink.getName());
		
		System.out.println();
		while (!maxNodes.isEmpty()) {
			DNode cond = maxNodes.remove();
			System.out.println("Analyzing:  " + cond);

			if (concconds.get(cond) != null && concconds.get(cond).size() > 1) {
				Set<DNode> found = new HashSet<DNode>();
				for (DNode condp : concconds.get(cond))
					if (unf.getProperName(condp).equals(unf.getProperName(cond))) {
						found.add(condp);
						maxNodes.remove(condp);
						System.out.println("\t" + condp);
					}
				System.out.println("bounded at:  " + (found.size() + 1));
				if (found.size() > 0) {
					unboundedConds.put(cond, found);
					continue;
				}
			}
			if (!sinkNames.contains(unf.getProperName(cond)) && !cond.isCutOff) {
				System.out.println("local deadlock!");
				localdeadlocks.add(cond);
			}
		}
				
		maxNodes = new LinkedList<DNode>(unf.getCurrentMaxNodes());
		
		for (DNode node: maxNodes) {
			Set<DNode> cut = unf.getCut(node.pre[0]);
			if (cut.size() > 1) {
				System.out.println("Maybe in deadlock");
				if (!node.isCutOff)
//					throw new UnsoundModelException("Unfolding is locally deadlocked ... case 2");
					;
			}
		}
	}

	public SoundnessStatus checkSoundness(Set<Place> sinks) {
		SoundnessStatus result = SoundnessStatus.SOUND;
		if (unboundedConds == null)
			analyze(sinks);
		
		if (unboundedConds.size() > 0 && localdeadlocks.size() > 0)
			result = SoundnessStatus.UNSAFE_DEADLOCKED;
		else if (unboundedConds.size() > 0)
			result = SoundnessStatus.UNSAFE;
		else if (localdeadlocks.size() > 0)
			result = SoundnessStatus.DEADLOCKED;
		
		return result;
	}
}
