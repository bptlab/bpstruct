package ee.ut.bpstruct;

import java.util.List;
import java.util.Map;
import java.util.Set;

import de.hpi.bpt.process.GatewayType;
import de.hpi.bpt.process.Node;
import de.hpi.bpt.process.Process;
import ee.ut.bpstruct.jbpt.Pair;
import ee.ut.bpstruct.util.DFSLabeler;
import ee.ut.bpstruct.util.GraphUtils;

public class RestrictedRestructurerVisitor extends RestructurerVisitor
		implements Visitor {

	public RestrictedRestructurerVisitor(Helper helper) {
		super(helper);
	}
	
	public void visitRigid(Process proc, Set<Pair> edges,
			Set<Node> vertices, Node entry, Node exit) throws CannotStructureException {
		Map<Node, List<Node>> adjlist = GraphUtils.edgelist2adjlist(edges, exit);
		DFSLabeler labeler =  new DFSLabeler(adjlist, entry);

		if (!labeler.isMixedLogic() && labeler.getLogic() == GatewayType.XOR && !labeler.isCyclic())
			restructureXORAcyclicRigid(proc, edges, vertices, entry, exit, adjlist);
		else 
			helper.foldComponent(edges, vertices, entry, exit);
	}

}
