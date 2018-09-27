/* 
 * Copyright (C) 2010, 2011 - Luciano Garcia Banuelos, Artem Polyvyanyy
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

import hub.top.petrinet.PetriNet;
import hub.top.uma.DNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.log4j.Logger;


import de.bpt.hpi.graph.Edge;
import de.bpt.hpi.graph.Graph;
import ee.ut.bpstruct.Helper.BLOCK_TYPE;
import ee.ut.bpstruct.unfolding.Unfolder;
import ee.ut.bpstruct.unfolding.Unfolding;
import ee.ut.bpstruct.unfolding.UnfoldingHelper;
import ee.ut.bpstruct.unfolding.UnfoldingRestructurer;
import ee.ut.graph.util.GraphUtils;

public class RestructurerVisitor implements Visitor {
	static Logger logger = Logger.getLogger(RestructurerVisitor.class);

	private RestructurerHelper helper;

	// "instances" and "labels" are used for cloning labels in cyclic rigid components
	// Should they be handled elsewere ?
	Map<Integer, Stack<Integer>> instances = new HashMap<Integer, Stack<Integer>>();
	Map<String, Integer> labels = new HashMap<String, Integer>();


	public RestructurerVisitor(RestructurerHelper helper) {
		this.helper = helper;
	}

	/**
	 * This method analyze the flow relation of a given rigid component and
	 * calls the method to handle either cyclic or acyclic components, accordingly.
	 * 
	 * @param graph		The original process graph
	 * @param edges		The subset of edges that conform the rigid component
	 * @param vertices	The subset of vertices that conform the rigid component
	 * @param entry		The entry vertex of the rigid component
	 * @param exit		The exit vertex of the rigid component
	 */
	public void visitRNode(Graph graph, Set<Edge> edges, Set<Integer> vertices,
			Integer entry, Integer exit) throws CannotStructureException {
		if (logger.isInfoEnabled()) logger.info("Rigid component: " + edges);

		// We use a simple DFS method to: a) identify loops (cf. |backedges| > 0),
		// b) characterize logic of the component (cf. xor, and, mixed) 
		DFSLabeler labeler =  new DFSLabeler(helper, GraphUtils.edgelist2adjlist(edges, exit), entry);

		if (labeler.isCyclic())
			restructureCyclicRigid(graph, edges, vertices, entry, exit);
		else
			restructureAcyclicRigid(graph, edges, vertices, entry, exit);

		//helper.foldComponent(graph, edges, vertices, entry, exit, BLOCK_TYPE.RIGID);
	}

	private void restructureAcyclicRigid(Graph graph, Set<Edge> edges,
			Set<Integer> vertices, Integer entry, Integer exit) throws CannotStructureException {
		if (logger.isInfoEnabled()) logger.info("Acyclic case");

		// In the case of Multi-source/Mult-sink fragments, check if the internal logic is homogeneous
		// if it is homogeneous dummy entry/exit nodes are set to the corresponding logic.
		checkHomogeneousLogic(vertices, entry, exit);
		
		// STEP 1: Petrify process component
		Petrifier petrifier = helper.getPetrifier(vertices, edges, entry, exit);
		PetriNet net = petrifier.petrify();


		// STEP 2: Compute Complete Prefix Unfolding
		Unfolder unfolder = new Unfolder(helper, net);
		Unfolding unf = unfolder.perform();

		Map<String, Integer> tasks = new HashMap<String, Integer>();		
		for (Integer vertex: vertices)
			if (helper.gatewayType(vertex) == null)
				tasks.put(graph.getLabel(vertex), vertex);

		// Rewire the unfolding using according to definition ... in new paper
		UnfoldingHelper unfhelper = new UnfoldingHelper(helper, unf);		
		unfhelper.rewire2();
		
		edges.clear(); vertices.clear();
		UnfoldingRestructurer restructurer = new UnfoldingRestructurer(helper, unfhelper, graph, vertices, edges, entry, exit, tasks, labels, instances);
		restructurer.process();
	}

	/**
	 * For acyclic multi-source/multi-sink rigids having homogeneous logic in the internal gateways,
	 * it is worth correcting the logic of the source/sink node (this allow us to avoid the combinatorial problem
	 * associated to the unfolding of multi-source rigids). This method performs a pre-processing analysis
	 * and correct the logic of source/sink node.
	 * 
	 * @param vertices
	 * @param entry
	 * @param exit
	 */
	private void checkHomogeneousLogic(Set<Integer> vertices, Integer entry,
			Integer exit) {
		if (!(helper.isChoice(entry) || helper.isParallel(entry)) ||
				!(helper.isChoice(exit) || helper.isParallel(exit))) {
			Object logic = null;
			boolean parallel = false;
			boolean mixed = false;
			
			for (Integer v: vertices) {
				// skip entry/exit nodes
				if (v.equals(entry) || v.equals(exit)) continue;
				if (helper.gatewayType(v) != null) {
					if (logic == null) {
						logic = helper.gatewayType(v);
						parallel = helper.isParallel(v);
					} else if (logic != helper.gatewayType(v)) {
						mixed = true;
						break;
					}
				}
			}
			
			if (!mixed)
				if (parallel) {
					helper.setANDGateway(entry);
					helper.setANDGateway(exit);
				} else {
					helper.setXORGateway(entry);
					helper.setXORGateway(exit);
				}
		}
	}

	private void restructureCyclicRigid(final Graph graph, final Set<Edge> edges,
			final Set<Integer> vertices, final Integer entry, final Integer exit) throws CannotStructureException {
		if (logger.isInfoEnabled()) logger.info("Cyclic case");

		// STEP 1: Petrify process component
		PetriNet net = helper.getPetrifier(vertices, edges, entry, exit).petrify();
		
		// STEP 2: Compute Complete Prefix Unfolding
		Unfolder unfolder = new Unfolder(helper, net);
		final Unfolding unf = unfolder.perform();
		
		Map<String, Integer> tasks = new HashMap<String, Integer>();
		for (Integer vertex: vertices)
			if (helper.gatewayType(vertex) == null)
				tasks.put(graph.getLabel(vertex), vertex);

		// Expand cyclic cutoffs
		final UnfoldingHelper unfhelper = new UnfoldingHelper(helper, unf);
		
		Set<DNode> properRepCutoffs = new java.util.HashSet<DNode>();
		boolean done = false;
		int iteration = 1;
		
		// PHASE 1 --------------------------------------------------------------------------------------
		// In this phase, we ensure that every reproductive behavior has a heading reproductive cutoff. If
		// the heading cutoff does not have its corresponding in the same local configuration, then it must
		// be expanded.
		// The analysis is made on SCCs. To deal with nesting, we iteratively disconnect outer loops, then
		// process inner loops until fixpoint. At the same time, we check for eventual "jump into a loop"
		// patterns and expand the unfolding so as to eliminate this situation.
		do {
			Set<DNode> toExpand = new HashSet<DNode>();
			done = true;	
			
			// Rewire the unfolding
			// To deal with nesting, proper reproductive cutoffs are not rewired!
			unfhelper.rewire(properRepCutoffs);
			
			// Compute the set of Strongly Connected Components of the rewired unfolding
			Graph rewiredUnfGraph = unfhelper.getGraph();
			Set<Set<Integer>> sccs = GraphUtils.computeSCCs(rewiredUnfGraph);
			
			for (Set<Integer> scc: sccs) {
				if (scc.size() == 1) continue; // Skip singletons !

				// Reference to the heading reproductive cutoff
				DNode repCutoff = null;

				for (DNode cutoff: unf.getCutoffs()) {
					DNode corr = unf.getCorr(cutoff);
					Integer _cutoff = unfhelper.getVertex(cutoff);
					Integer _corr = unfhelper.getVertex(corr);

					// CASE 1 --
					// Cutoffs leading to looping behavior must have its corresponding
					// in the same local configuration.
					// --------------------------------------------------------------------------------
					// Heuristic: Identify cutoff events within the SCC for which the corresponding
					// event is not in the SCC. Check whether both are in the same local configuration.
					// If it is not the case cutoff must be expanded.
					if (scc.contains(_cutoff) && !scc.contains(_corr)) {
						done = false;
						repCutoff = cutoff;
						// Reproducing cutoffs and corresponding must be in the same
						// local configuration
						// NOTE: Corresponding must be just before a branching condition!
						if (!(unfolder.isCorrInLocalConfig(cutoff, corr) &&
								corr.post[0].post.length > 1))
							toExpand.add(cutoff); // Expand if conditions do not hold
						else
							// Mark cutoff as "reproductive cutoff" 
							properRepCutoffs.add(cutoff);
					}
				}

				// Only if repCutoff is not marked to expand by CASE 1 then proceed
				// to analyze CASE 2
				if (repCutoff != null && !toExpand.contains(repCutoff)) {

					// CASE 2 --
					// A Cutoff is a "jump into a loop" if it is not in the SCC
					// but its corresponding event is in the SCC
					for (DNode cutoff: unf.getCutoffs()) {
						DNode corr = unf.getCorr(cutoff);
						Integer _cutoff = unfhelper.getVertex(cutoff);
						Integer _corr = unfhelper.getVertex(corr);

						// If cutoff is a "jump into a loop", then the repCutoff heading
						// the SCC has to be expanded.
						if (scc.contains(_corr) && !scc.contains(_cutoff)) {
							done = false;
							properRepCutoffs.remove(repCutoff);
							toExpand.add(repCutoff);
							break;
						}
					}
				}
			}

			if (!toExpand.isEmpty())
				unfolder.expand(toExpand, iteration++);

			// RULE 3 --
			// This loop has to be repeated until every reproductive cutoff is properly contained
			// and no more "jump into a loop" can be detected at the level of SCCs. (Fixpoint)
		} while (!done);
		
		
		// PHASE 2 -------------------------------------------------------------------------------
		// In the second phase we want to reach a point where every loop is explicitly represented
		// in the unfolding and no Oulsnam's LL looping patterns are present.
		// When a LL pattern is found, we basically apply Oulsnam IL-0 transformation: 
		// the heading reproductive cutoff is expanded.
		//
		// This corresponds to RULE 4
		//
		// HEURISTIC: Within an LL pattern, the heading loop (the one which reaches the entry point)
		// does not reach the exit points. The procedure is as follows:
		// Identify the heading reproductive cutoff
		// For each heading reproductive cutoff
		// If its local configuration contains a corresponding (a kind of incoming edge in LL pattern)
		// it might be the case where that corresponding is involved in a looping behavior. If it is
		// the case, it might also be that this looping behavior can reach the exit point without 
		// reentering the heading loop ... -- I bit difficult to explain ... but it works.
		do {
			done = true;
			Set<DNode> toExpand = new HashSet<DNode>();
			
			// Rewire the unfolding -- no restriction on rewiring
			unfhelper.rewire();
			
			// Compute SCCs over the induced graph
			Graph rewiredUnfGraph = unfhelper.getGraph();
			Set<Set<Integer>> sccs = GraphUtils.computeSCCs(rewiredUnfGraph);
			
			// Analyze one SCC at a time
			for (Set<Integer> _scc: sccs) {
				if (_scc.size() == 1) continue; // Skip singletons
				
				Set<DNode> exitConds = new HashSet<DNode>();
				Set<DNode> pathsToExit = new HashSet<DNode>();
				// To ease the comparisons, translate "_scc" which is a set of Integers to
				// a set of DNodes (nodes in the Unfolding)
				Set<DNode> scc = new HashSet<DNode>();
				for (Integer v: _scc) {
					DNode dnode = unfhelper.getDNode(v);
					scc.add(dnode);
				}
				
				// identify exit conditions (SCC exit points)
				for (DNode dnode: scc) {
					if (!dnode.isEvent && dnode.post.length > 1) { // Only branching conditions
						boolean isInternal = true;
						// If one Event in postset of the branching condition is not inside the
						// SCC then the condition is an exit condition
						for (DNode post: dnode.post)
							if (!scc.contains(post)) {
								isInternal = false;
								break;
							}
						
						if (!isInternal) {
							exitConds.add(dnode);
							// Compute the backwards closed set for this condition
							Set<DNode> backclosedset = unf.getBackwardsClosedSet(dnode);

							// Keep only the subset of nodes which are inside the strongly connected component
							backclosedset.retainAll(scc);
							// Compute the aggregated set of nodes which are included in paths leading
							// to the exit points. (Note that we are implicitly discarding some nested loops and
							// some other blocks ... uhm ... TO BE COMPLETED)
							pathsToExit.addAll(backclosedset);
						}
					}
				}
				
				// Identify the set of reproductive cutoffs which reach ten entry point of the SCC
				// also referred to as "heading reproductive cutoffs"
				Set<DNode> repCutoffsReachingSCCEntry = new HashSet<DNode>();
				// We build a reverse map:  Corresponding -> Set<Cutoff>
				Map<DNode, Set<DNode>> corr2cutoff = new HashMap<DNode, Set<DNode>>();
				
				for (DNode cutoff: unf.getCutoffs()) {
					if (!scc.contains(cutoff)) continue; // Only cutoffs within the SCC
					DNode corr = unf.getCorr(cutoff);
					
					// If corresponding is not in SCC, then cutoff is a reproductive cutoff
					// ... at this stage, SCC have only one entry point. Cutoff is then a
					// heading reproductive cutoff
					if (!scc.contains(corr))
						repCutoffsReachingSCCEntry.add(cutoff);
					else {
						// Build the reverse map
						if (!corr2cutoff.containsKey(corr))
							corr2cutoff.put(corr, new HashSet<DNode>());
						corr2cutoff.get(corr).add(cutoff);
					}
				}

				// For each "heading reproductive cutoff"
				for (DNode headingRepCutoff: repCutoffsReachingSCCEntry) {
					
					// Compute the reproductive behavior -covered- by
					// this reproductive cutoff
					Set<DNode> headingLoopingBehavior = unf.getBackwardsClosedSet(headingRepCutoff);
					headingLoopingBehavior.retainAll(scc);
					
					Set<DNode> prunedPathsToExit = null;
					// Look for corresponding events included in the reproductive behavior
					// being currently analyzed
					for (DNode innercorr: corr2cutoff.keySet()) {
						// Is corresponding within reproductive behavior?
						if (headingLoopingBehavior.contains(innercorr)) {
							if (prunedPathsToExit == null) {
								// Compute all possible paths leading to exit points
								// excluding the "heading reproductive behavior" that
								// we are analyzing
								prunedPathsToExit = new HashSet<DNode>(pathsToExit);
								prunedPathsToExit.removeAll(headingLoopingBehavior);
							}

							// Analyze every cutoff associated to corresponding "innercorr"
							for (DNode innercutoff: corr2cutoff.get(innercorr)) {
								Set<DNode> leadingBehavior = unf.getBackwardsClosedSet(innercutoff);
								for (DNode dnode: leadingBehavior)
									// If the behavior covered by "innercutoff" and "innercorr"
									// reach any exit point without going through the heading
									// loop, then ... we have an LL pattern.
									if (prunedPathsToExit.contains(dnode)) {
										// So ... we have to expand heading reproductive cutoff
										toExpand.add(headingRepCutoff);
										break;
									}
							}
						}
					}
				}
			}
			if (!toExpand.isEmpty()) {
				done = false;
				unfolder.expand(toExpand, iteration++);
			}
		} while (!done);
				
		unfhelper.rewire2();
		// Restructure the rewired unfolding
		edges.clear(); vertices.clear();
		UnfoldingRestructurer restructurer = new UnfoldingRestructurer(helper, unfhelper, graph, vertices, edges, entry, exit, tasks, labels, instances);
		restructurer.process();
	}
		
	// ------------------------------------------------
	// ------------ Structured Components
	// ------------ Only dummy methods
	// ------------------------------------------------

	public void visitSNode(Graph graph, Set<Edge> edges, Set<Integer> vertices,
			Integer entry, Integer exit) {
		helper.foldComponent(graph, edges, vertices, entry, exit, BLOCK_TYPE.POLYGON);		
	}

	public void visitPNode(Graph graph, Set<Edge> edges, Set<Integer> vertices,
			Integer entry, Integer exit) {
		helper.foldComponent(graph, edges, vertices, entry, exit, BLOCK_TYPE.BOND);
	}

	public void visitRootSNode(Graph graph, Set<Edge> edges,
			Set<Integer> vertices, Integer entry, Integer exit) {		
		helper.foldComponent(graph, edges, vertices, entry, exit, BLOCK_TYPE.POLYGON);		
	}
}
