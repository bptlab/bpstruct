package ee.ut.bpstruct;

import hub.top.petrinet.PetriNet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.hpi.bpt.process.Node;
import de.hpi.bpt.process.Process;
import ee.ut.bpstruct.jbpt.Pair;
import ee.ut.bpstruct.util.DFSLabeler;
import ee.ut.bpstruct.util.GraphUtils;

public class MEMERestructurerVisitor extends RestructurerVisitor {
	
	private Helper helper;

	public MEMERestructurerVisitor(Helper helper) {
		super(helper);
		this.helper = helper;
	}
	
	public void visitRigid(Process proc, Set<Pair> edges,
			Set<Node> vertices, Node entry, Node exit) throws CannotStructureException {
		System.out.println("Found a rigid");
		Map<Node, List<Node>> adjlist = GraphUtils.edgelist2adjlist(edges, exit);
		DFSLabeler labeler =  new DFSLabeler(adjlist, entry);

		if (labeler.isCyclic()) {
//			restructureCyclicRigid(proc, edges, vertices, entry, exit);
			throw new CannotStructureException("Cyclic structuring is not supported for MEME models");
		} else
			restructureAcyclicRigid(proc, edges, vertices, entry, exit, adjlist);
	}

	public void restructureAcyclicRigid(Process proc, Set<Pair> edges,
			Set<Node> vertices, Node entry, Node exit, Map<Node, List<Node>> adjlist) throws CannotStructureException {
		System.out.println("\tAcyclic rigid");
		PetriNet net = helper.petrify(edges, vertices, entry, exit);
		MEMEUnfolder unfolder = new MEMEUnfolder(net);
			Unfolding unf = unfolder.perform();
	
			final Map<String, Node> tasks = new HashMap<String, Node>();		
			for (Node vertex: vertices)
				if (helper.getLabeledElements().contains(vertex))
					tasks.put(vertex.getName(), vertex);
	
			helper.synthesizeFromOrderingRelations(proc, edges, vertices, entry, exit,
					unf, tasks);
	}

}
