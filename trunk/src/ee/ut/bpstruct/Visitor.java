package ee.ut.bpstruct;

import java.util.Set;

import de.hpi.bpt.process.Node;
import de.hpi.bpt.process.Process;
import ee.ut.bpstruct.jbpt.Pair;

public interface Visitor {

	void visitPolygon(Process proc, Set<Pair> ledges, Set<Node> lvertices,
			Node entry, Node exit);

	void visitBond(Process proc, Set<Pair> ledges, Set<Node> lvertices,
			Node entry, Node exit);

	void visitRigid(Process proc, Set<Pair> ledges, Set<Node> lvertices,
			Node entry, Node exit) throws CannotStructureException;

}
