package ee.ut.bpstruct;

import hub.top.petrinet.PetriNet;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import de.hpi.bpt.process.ControlFlow;
import de.hpi.bpt.process.Node;
import de.hpi.bpt.process.Process;
import ee.ut.bpstruct.jbpt.Pair;

public interface Helper {

	void foldComponent(Set<Pair> ledges, Set<Node> vertices, Node entry,
			Node exit);

	PetriNet petrify(Set<Pair> ledges, Set<Node> vertices, Node entry, Node exit);

	Set<Node> getLabeledElements();

	Set<Pair> flattenEdgeSet(Collection<ControlFlow> edges);

	void foldRigidComponent(Set<Pair> ledges, Set<Node> vertices, Node entry,
			Node exit, Process childProc, Node entry2, Node exit2);

	void synthesizeFromOrderingRelations(Process proc, Set<Pair> edges,
			Set<Node> vertices, Node entry, Node exit, Unfolding unf,
			Map<String, Node> tasks) throws CannotStructureException;

}
