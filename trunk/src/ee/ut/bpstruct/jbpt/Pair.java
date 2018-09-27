package ee.ut.bpstruct.jbpt;

import de.hpi.bpt.process.Node;

public class Pair {
	Node first = null, second = null;
	public Pair() {}
	public Pair(Node f, Node s) { first = f; second = s; };
	
	public Node getFirst() {
		return first;
	}
	public Node getSource() {
		return first;
	}
	public void setFirst(Node first) {
		this.first = first;
	}
	public Node getSecond() {
		return second;
	}
	public Node getTarget() {
		return second;
	}
	public void setSecond(Node second) {
		this.second = second;
	}

	public int hashCode() {
		return (first.getName() + second.getName()).hashCode();
	}
	public boolean equals(Object o) {
		if (!(o instanceof Pair)) return false;
		Pair theother = (Pair) o;
		return theother.first.equals(first) && theother.second.equals(second);
	}
	public String toString() {
		return String.format("[%s -> %s]", first, second);
	}
}
