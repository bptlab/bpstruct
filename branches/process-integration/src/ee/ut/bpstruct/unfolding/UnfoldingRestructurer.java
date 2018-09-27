/* 
 * Copyright (C) 2010 - Luciano Garcia Banuelos
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
package ee.ut.bpstruct.unfolding;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import de.bpt.hpi.graph.Edge;
import de.bpt.hpi.graph.Graph;
import de.bpt.hpi.graph.Pair;
import de.bpt.hpi.ogdf.rpst.ExpRPST;
import de.bpt.hpi.ogdf.spqr.SPQRNodeType;
import de.bpt.hpi.ogdf.spqr.TreeNode;
import ee.ut.bpstruct.CannotStructureException;
import ee.ut.bpstruct.RestructurerHelper;
import ee.ut.bpstruct.Visitor;

public class UnfoldingRestructurer {
	private UnfoldingHelper helper;
	private Visitor visitor;

	public static int counter = 0;
		
	public UnfoldingRestructurer(RestructurerHelper helper, UnfoldingHelper unfhelper,
			Graph graph, Set<Integer> vertices, Set<Edge> edges, Integer entry,
			Integer exit, Map<String, Integer> tasks, Map<String, Integer> labels, Map<Integer, Stack<Integer>> instances) {
		this.helper = unfhelper;
		this.visitor = new UnfoldingRestructurerVisitor(helper, unfhelper, graph, vertices, edges, entry, exit, tasks, labels, instances);
	}

	public void process() throws CannotStructureException {
		ExpRPST tree = new ExpRPST(helper.getGraph());
		Graph graph = tree.getExpandedGraph();
		TreeNode root = tree.getRootNode();
		Set<Edge> edges = new HashSet<Edge>(root.getOriginalEdges());
		Set<Integer> vertices = new HashSet<Integer>(root.getOriginalVertices());		
		traverse(visitor, tree, graph, root, edges, vertices);
		if (root.getNodeType() == SPQRNodeType.R)
			visitor.visitRNode(graph, edges, vertices, tree.getEntry(root), tree.getExit(root));
		else if (root.getNodeType() == SPQRNodeType.P)
			visitor.visitPNode(graph, edges, vertices, tree.getEntry(root), tree.getExit(root));
		else if (root.getNodeType() == SPQRNodeType.S)
			visitor.visitSNode(graph, edges, vertices, tree.getEntry(root), tree.getExit(root));
			
		visitor.visitRootSNode(graph, edges, vertices, tree.getEntry(root), tree.getExit(root));
		
	}
	
	private void traverse(Visitor visitor, ExpRPST tree, Graph graph, TreeNode curr, Set<Edge> edges, Set<Integer> vertices) throws CannotStructureException {
		if (curr.getNodeType() == SPQRNodeType.Q) return;
		
		Set<Edge> ledges = new HashSet<Edge>(curr.getOriginalEdges());
		Set<Integer> lvertices = new HashSet<Integer>(curr.getOriginalVertices());
				
		for (TreeNode child : curr.getChildNodes()) {
			if (child.getNodeType() == SPQRNodeType.Q) continue;
			Set<Edge> cedges = new HashSet<Edge>(child.getOriginalEdges());
			Set<Integer> cvertices = new HashSet<Integer>(child.getOriginalVertices());
			
			traverse(visitor, tree, graph, child, cedges, cvertices);
			
			Pair pair = child.getBoundaryVertices();
			Integer entry = tree.getEntry(child);
			Integer exit = entry == pair.getFirst() ? pair.getSecond() : pair.getFirst();
			
			switch (child.getNodeType()) {
			case S:
				visitor.visitSNode(graph, cedges, cvertices, entry, exit);
				ledges.removeAll(child.getOriginalEdges());
				lvertices.removeAll(child.getOriginalVertices());
				ledges.addAll(cedges);
				lvertices.addAll(cvertices);
				break;
			case P:
				visitor.visitPNode(graph, cedges, cvertices, entry, exit);
				ledges.removeAll(child.getOriginalEdges());
				lvertices.removeAll(child.getOriginalVertices());
				ledges.addAll(cedges);
				lvertices.addAll(cvertices);
				break;
			case Q: break;
			case R:
				visitor.visitRNode(graph, cedges, cvertices, entry, exit);
				ledges.removeAll(child.getOriginalEdges());
				lvertices.removeAll(child.getOriginalVertices());
				ledges.addAll(cedges);
				lvertices.addAll(cvertices);
				break;
			}
		}
		
		edges.clear();
		edges.addAll(ledges);
		vertices.clear();
		vertices.addAll(lvertices);
	}

}
