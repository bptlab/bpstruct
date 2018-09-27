package ee.ut.comptech;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.bpt.hpi.graph.Graph;
import ee.ut.comptech.DominatorTree.InfoNode;

public class Processor {
	
	public class Node {
		List<Node> preds = new LinkedList<Node>();
		List<Node> succs = new LinkedList<Node>();
		Node idom;
		List<Node> succs_dom = new LinkedList<Node>();
		int level;
		int weight;
		Node copy;
		Node header;
		boolean done;
		boolean active;
		String label;
		Integer original;
		
		public String toString() { return label; }
		
		public List<Node> getSuccs() { return succs; }
		
		public List<Node> getPreds() { return preds; }
		
		public Integer getOriginal() { return original; }
		
		public void setOriginal(Integer o) { original = o; }
		
		public Object clone() {
			Node theOther = new Node();
			theOther.preds = new LinkedList<Node>(this.preds);
			theOther.succs = new LinkedList<Node>(this.succs);
			theOther.idom = this.idom;
			theOther.succs_dom = new LinkedList<Node>(this.succs_dom);
			theOther.level = this.level;
			theOther.weight = this.weight;
			theOther.copy = null;
			theOther.header = this.header;
			theOther.done = this.done;
			theOther.active = this.active;
			theOther.label = this.label+"p";
			theOther.original = this.original;
			return theOther;
		}
		
		public int hashCode() {
			return label.hashCode();
		}
		public boolean equals(Object _theOther) {
			Node theOther = (Node)_theOther;
			return theOther.label.equals(label);
		}
	}
	
	class Edge {
		Node source, target;
		
		public Edge(Node source, Node target) {
			this.source = source;
			this.target = target;
		}
		public int hashCode() {
			return source.hashCode() + target.hashCode();
		}
		public boolean equals(Object o) {
			boolean result = false;
			if (o instanceof Edge) {
				Edge e = (Edge) o;
				result = this.source.equals(e.source) && this.target.equals(e.target);
			}
			return result;
		}
		
		public String toString() { return String.format("(%s, %s)", source.label, target.label);}
	}
	
	HashMap<Integer, Node> map = new HashMap<Integer, Node>();
	HashSet<Edge> marks = new HashSet<Edge>();
	Node start;
	
	
	public Node getStart() {
		return start;
	}
	
	public Processor(Graph g, Map<Integer, List<Integer>> adjList, Integer entry) {
		DominatorTree dt = new DominatorTree(adjList);
		dt.analyse(entry);
		copyGraphNDomtree(g, dt);
		start = getNode(g, entry);

		setLevel(start, 1);
		markUndone(start);
		searchSPBack(start);
		markUndone(start);
		
		spltLoops(start, new HashSet<Node>());
	}
	
	private boolean spltLoops(Node topNode, HashSet<Node> set) {
		boolean cross = false;
		
		for (Node child : topNode.succs_dom)
			if (set.isEmpty() || set.contains(child))
				if (spltLoops(child, set))
					cross = true;
		
		if (cross)
			handleIRChildren(topNode, set);
		
		for (Node pred : topNode.preds)
			if (marks.contains(new Edge(pred, topNode)) &&
				!dom(topNode, pred))
				return true;
		
		return false;
	}

	private void handleIRChildren(Node topNode, HashSet<Node> set) {
		List<Node> dfslist = new LinkedList<Node>();
		List<List<Node>> scclist = new LinkedList<List<Node>>();
		
		for (Node child : topNode.succs_dom)
			if (!child.done &&
				(set.isEmpty() || set.contains(child)))
				SCC1(dfslist, child, set, topNode.level);
		
		for (Node tmp : dfslist) {
			if (tmp.done) {
				List<Node> scc = new LinkedList<Node>();
				SCC2(scc, tmp, topNode.level);
				scclist.add(scc);
			}
		}
		
		for (List<Node> scc : scclist) {
			if (scc.size() > 1)
				handleSCC(topNode, scc);
		}
	}

	private void handleSCC(Node topNode, List<Node> scc) {
		List<Node> msed = new LinkedList<Node>();
		for (Node tmp : scc)
			if (tmp.level == topNode.level + 1) {
				getWeight(tmp, tmp, scc);
				msed.add(tmp);
			}
		
		if (msed.size() <= 1)
			return;
		
		Node tmp = chooseNode(msed);
		splitSCC(tmp, scc);
	}

	private void splitSCC(Node hdrnode, List<Node> scc) {
		
		for (Node tmp : scc) {
			if (tmp.header != hdrnode) {
				tmp.copy = (Node)tmp.clone();
				System.out.println("Cloning: " + tmp);
			}
		}
		
		for (Node tmp : scc) {
			if (tmp.header != hdrnode) {
				if (tmp.idom.copy == null)
					tmp.idom.succs_dom.add(tmp.copy);
				else {
					tmp.idom.copy.succs_dom.remove(tmp);
					tmp.idom.copy.succs_dom.remove(tmp.copy);
					tmp.copy.idom = tmp.idom.copy;
				}
				
				for (Node tmp1 : tmp.succs_dom)
					if (tmp1.copy == null)
						tmp.copy.succs_dom.remove(tmp1);
				
				for (Node tmp1 : tmp.succs) {
					if (tmp1.copy != null) {
						tmp.copy.succs.remove(tmp1);
						tmp.copy.succs.add(tmp1.copy);
						tmp1.copy.preds.remove(tmp);
						tmp1.copy.preds.add(tmp.copy);
					} else {
						tmp1.preds.add(tmp.copy);
					}
				}
				
				List<Node> toRemove = new LinkedList<Node>();
				for (Node tmp1 : tmp.preds) {
					if (tmp1.copy == null) {
						if (!scc.contains(tmp1))
							tmp.copy.preds.remove(tmp1);
						else {
							toRemove.remove(tmp1);
							tmp1.succs.remove(tmp);
							tmp1.succs.add(tmp.copy);
						}
					}
				}
				tmp.preds.removeAll(toRemove);
			}
		}
		
		List<Node> toAdd = new LinkedList<Node>();
		for (Node tmp : scc) {
			if (tmp.copy != null)
				toAdd.add(tmp.copy);
			tmp.copy = null;
		}
		scc.addAll(toAdd);
	}

	private Node chooseNode(List<Node> msed) {
		int maxWeight = 0;
		Node maxNode = null;
		for (Node tmp : msed) {
			if (tmp.weight > maxWeight) {
				maxWeight = tmp.weight;
				maxNode = tmp;
			}
		}
		return maxNode;
	}

	private void getWeight(Node tmp, Node hdnode, List<Node> scc) {
		tmp.weight = 1;
		for (Node child : tmp.succs_dom) {
			if (scc.contains(child)) {
				getWeight(child, hdnode, scc);
				tmp.weight = tmp.weight + child.weight;
			}
		}
		tmp.header = hdnode; 
	}

	private void SCC2(List<Node> scc, Node cnode, int level) {
		cnode.done = false;
		for (Node pred : cnode.preds)
			if (pred.done && pred.level > level)
				SCC2(scc, pred, level);
		scc.add(cnode);
	}

	private void SCC1(List<Node> dfslist, Node cnode, HashSet<Node> set,
			int level) {
		cnode.done = true;
		for (Node child : cnode.succs) {
			if (!child.done && child.level > level &&
				(set.isEmpty() || set.contains(child)))
				SCC1(dfslist, child, set, level);
		}
		dfslist.add(cnode);
	}

	private boolean dom(Node topNode, Node pred) {
		// TODO Auto-generated method stub
		return false;
	}

	private void searchSPBack(Node cnode) {
		cnode.done = true;
		cnode.active = true;

		removeMarks(cnode);

		for (Node child : cnode.succs)
			if (child.active)
				marks.add(new Edge(cnode, child));
			else if (!child.done)
				searchSPBack(child);
		
		cnode.active = false;
	}

	private void removeMarks(Node cnode) {
		for (Node succ : cnode.succs_dom)
			marks.remove(new Edge(cnode, succ));
	}

	private void markUndone(Node cnode) {
		cnode.done = false;
		cnode.active = false;
		for (Node child : cnode.succs_dom)
			markUndone(child);
	}
	
	private void setLevel(Node cnode, int level) {
		cnode.level = level;
		for (Node child : cnode.succs_dom)
			setLevel(child, level + 1);
	}
	

	// Auxiliary, nasty stuff !!!
	// --------------------------------
	private Node getNode(Graph graph, Integer act) {
		Node node = map.get(act);
		if (node == null) {
			node = new Node();
			node.original = act;
			map.put(act, node);
			node.label = graph.getLabel(act);
		}
		return node;
	}
	
	private void copyGraphNDomtree(Graph graph, DominatorTree dt) {
		HashSet<Integer> acts = new HashSet<Integer>(dt.map.keySet());
		
		for (Integer act : acts) {
			Node node = getNode(graph, act);
			for (Integer _succ : dt.adjList.get(act)) {
				Node succ = getNode(graph, _succ);
				node.succs.add(succ); succ.preds.add(node);
			}
		}
		
		for (InfoNode inode : dt.map.values()) {
			InfoNode inparent = inode.dom;
			if (inparent != null) {
				Node node = getNode(graph, inode.node);
				Node nparent = getNode(graph, inparent.node);
				
				node.idom = nparent;
				nparent.succs_dom.add(node);
			}
		}
	}
}
