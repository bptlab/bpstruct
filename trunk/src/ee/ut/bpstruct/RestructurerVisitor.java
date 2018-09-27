package ee.ut.bpstruct;

import hub.top.petrinet.PetriNet;
import hub.top.uma.DNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import de.hpi.bpt.hypergraph.abs.Vertex;
import de.hpi.bpt.process.Gateway;
import de.hpi.bpt.process.GatewayType;
import de.hpi.bpt.process.Node;
import de.hpi.bpt.process.Process;
import de.hpi.bpt.utils.IOUtils;
import ee.ut.bpstruct.jbpt.Pair;
import ee.ut.bpstruct.jbpt.PlaceHolder;
import ee.ut.bpstruct.util.DFSLabeler;
import ee.ut.bpstruct.util.GraphUtils;

public class RestructurerVisitor implements Visitor {
	
	protected Helper helper;

	public RestructurerVisitor(Helper helper) {
		this.helper = helper;
	}
	
	public void visitRigid(Process proc, Set<Pair> edges,
			Set<Node> vertices, Node entry, Node exit) throws CannotStructureException {
//		System.out.println("Found a rigid");
		Map<Node, List<Node>> adjlist = GraphUtils.edgelist2adjlist(edges, exit);
		DFSLabeler labeler =  new DFSLabeler(adjlist, entry);

		if (labeler.isCyclic())
			restructureCyclicRigid(proc, edges, vertices, entry, exit);
		else if (!labeler.isMixedLogic() && labeler.getLogic() == GatewayType.XOR)
			restructureXORAcyclicRigid(proc, edges, vertices, entry, exit, adjlist);
		else 
			restructureAcyclicRigid(proc, edges, vertices, entry, exit, adjlist);			
	}

	public void restructureCyclicRigid(Process proc, Set<Pair> edges,
			Set<Node> vertices, Node entry, Node exit) throws CannotStructureException {
//		System.out.println("\tCyclic rigid");
		PetriNet net = helper.petrify(edges, vertices, entry, exit);
		Unfolder unfolder = new Unfolder(net);
		Unfolding unf = unfolder.perform();
		
//		String filename = String.format("bpstruct2/unf_%s.dot", proc.getName());
//		IOUtils.toFile(filename, unf.toDot());


		final Map<String, Node> tasks = new HashMap<String, Node>();		
		for (Node vertex: vertices)
			if (helper.getLabeledElements().contains(vertex))
				tasks.put(vertex.getName(), vertex);

		final UnfoldingHelper unfhelper = new UnfoldingHelper(unf);

		unfhelper.rewire2();
		
//		filename = String.format("bpstruct2/rewired_unf_%s.dot", proc.getName());
//		IOUtils.toFile(filename, unfhelper.getGraph().toDOT());
		
		// Restructure the rewired unfolding
		edges.clear(); vertices.clear();
		new UnfoldingRestructurer(helper, unfhelper, edges, vertices, entry, exit, tasks);
	}

	public void restructureXORAcyclicRigid(Process proc, Set<Pair> edges,
			Set<Node> vertices, Node entry, Node exit, Map<Node, List<Node>> adjlist) throws CannotStructureException {
//		System.out.println("\tXOR Acyclic rigid");
		Process childProc = new Process();
		Gateway entry2 = new Gateway(GatewayType.XOR);
		Gateway exit2 = new Gateway(GatewayType.XOR);
		
		Map<Node, Node> lmap = new HashMap<Node, Node>();
		lmap.put(entry, entry2);
		lmap.put(exit, exit2);
		Stack<Node> worklist = new Stack<Node>();
		worklist.push(entry);
		while (!worklist.isEmpty()) {
			Node curr = worklist.pop();
			for (Node succ: adjlist.get(curr)) {
				if (!succ.equals(exit)) {
					if (!(succ instanceof Gateway)) {
						PlaceHolder ph = (PlaceHolder)succ;
						Node vertexp = new PlaceHolder(ph.getEdges(), ph.getVertices(), ph.getEntry(), ph.getExit());
						vertexp.setName(ph.getName());
						
						lmap.put(succ, vertexp);
					} else
						lmap.put(succ, new Gateway(GatewayType.XOR));
					worklist.push(succ);
				}
				childProc.addControlFlow(lmap.get(curr), lmap.get(succ));
			}
		}

		helper.foldRigidComponent(edges, vertices, entry, exit, childProc, entry2, exit2);
	}
	
	public void restructureAcyclicRigid(Process proc, Set<Pair> edges,
			Set<Node> vertices, Node entry, Node exit, Map<Node, List<Node>> adjlist) throws CannotStructureException {
//		System.out.println("\tAcyclic rigid");
		PetriNet net = helper.petrify(edges, vertices, entry, exit);
		Unfolder unfolder = new Unfolder(net);
		Unfolding unf = unfolder.perform();

		final Map<String, Node> tasks = new HashMap<String, Node>();		
		for (Node vertex: vertices)
			if (helper.getLabeledElements().contains(vertex))
				tasks.put(vertex.getName(), vertex);

		helper.synthesizeFromOrderingRelations(proc, edges, vertices, entry, exit,
				unf, tasks);
	}

	public void visitBond(Process proc, Set<Pair> edges,
			Set<Node> vertices, Node entry, Node exit) {
		helper.foldComponent(edges, vertices, entry, exit);
	}

	public void visitPolygon(Process proc, Set<Pair> edges,
			Set<Node> vertices, Node entry, Node exit) {
		helper.foldComponent(edges, vertices, entry, exit);
	}

}
