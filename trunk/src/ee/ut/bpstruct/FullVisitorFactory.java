package ee.ut.bpstruct;

public class FullVisitorFactory implements VisitorFactory {
	public Visitor createVisitor(Helper helper) {
		return new RestructurerVisitor(helper);
	}

}
