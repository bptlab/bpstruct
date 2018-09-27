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

import hub.top.petrinet.PetriNet;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import de.bpt.hpi.graph.Edge;
import de.bpt.hpi.graph.Graph;
import ee.ut.bpstruct.Helper.BLOCK_TYPE;
import ee.ut.bpstruct.unfolding.MEMEUnfolder;
import ee.ut.bpstruct.unfolding.Unfolding;
import ee.ut.graph.util.GraphUtils;

/**
 * This class is a variation of BPStruct to restructure Multi-source/Multi-sink models.
 * NOTE: It cannot handle cyclic rigids!
 * 
 */
public class MEMERestructurerVisitor implements Visitor {
	static Logger logger = Logger.getLogger(MEMERestructurerVisitor.class);

	private RestructurerHelper helper;

	public MEMERestructurerVisitor(RestructurerHelper helper) {
		this.helper = helper;
	}

	public void visitRNode(Graph graph, Set<Edge> edges, Set<Integer> vertices,
			Integer entry, Integer exit) throws CannotStructureException {
		if (logger.isInfoEnabled()) logger.info("Rigid component: " + edges);

		// We use a simple DFS method to: a) identify loops (cf. |backedges| > 0),
		// b) characterize logic of the component (cf. xor, and, mixed) 
		DFSLabeler labeler =  new DFSLabeler(helper, GraphUtils.edgelist2adjlist(edges, exit), entry);

		if (!labeler.isCyclic())
			restructureAcyclicRigid(graph, edges, vertices, entry, exit);
		else
			throw new RuntimeException("CYCLIC RIGID found!");
	}

	private void restructureAcyclicRigid(Graph graph, Set<Edge> edges,
			Set<Integer> vertices, Integer entry, Integer exit) throws CannotStructureException {
		if (logger.isInfoEnabled()) logger.info("Acyclic case");

		if (!(helper.isChoice(entry) || helper.isParallel(entry)) ||
				!(helper.isChoice(exit) || helper.isParallel(exit))) {
			Object logic = null;
			boolean parallel = false;
			boolean mixed = false;
			
			for (Integer v: vertices) {
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
		
		// STEP 1: Petrify process component
		Petrifier petrifier = helper.getPetrifier(vertices, edges, entry, exit);
		PetriNet net = petrifier.petrify();


		// STEP 2: Compute Complete Prefix Unfolding
		MEMEUnfolder unfolder = new MEMEUnfolder(helper, net, petrifier.isMEME());
		Unfolding unf = unfolder.perform();

		Map<String, Integer> tasks = new HashMap<String, Integer>();		
		for (Integer vertex: vertices)
			if (helper.gatewayType(vertex) == null)
				tasks.put(graph.getLabel(vertex), vertex);

		edges.clear(); vertices.clear();
		helper.processOrderingRelations(edges, vertices, entry, exit, graph,
				unf, tasks);
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
