package ee.ut.bpstruct;

public class RestrictedVisitorFactory implements VisitorFactory {
	public Visitor createVisitor(Helper helper) {
		return new RestrictedRestructurerVisitor(helper);
	}

}
