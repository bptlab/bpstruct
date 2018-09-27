package ee.ut.bpstruct;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Map.Entry;

import de.hpi.bpt.graph.abs.IDirectedEdge;
import de.hpi.bpt.graph.algo.rpst.RPST;
import de.hpi.bpt.graph.algo.rpst.RPSTNode;
import de.hpi.bpt.graph.algo.tctree.TCType;
import de.hpi.bpt.hypergraph.abs.Vertex;
import de.hpi.bpt.process.ControlFlow;
import de.hpi.bpt.process.Gateway;
import de.hpi.bpt.process.GatewayType;
import de.hpi.bpt.process.Node;
import de.hpi.bpt.process.Process;
import de.hpi.bpt.process.Task;
import de.hpi.bpt.process.petri.Flow;
import de.hpi.bpt.process.petri.PetriNet;
import de.hpi.bpt.process.petri.Place;
import de.hpi.bpt.process.petri.Transition;
import ee.ut.bpstruct.jbpt.PNPair;
import ee.ut.bpstruct.jbpt.Pair;
import ee.ut.bpstruct.jbpt.PlaceHolder;
import ee.ut.bpstruct.util.GraphUtils;
import ee.ut.bpstruct.util.PNDFSLabeler;
import ee.ut.graph.moddec.ColoredGraph;
import ee.ut.graph.moddec.MDTNode;
import ee.ut.graph.moddec.MDTVisitor;
import ee.ut.graph.moddec.ModularDecompositionTree;

public class UnfoldingRestructurer {
	private Helper helper;
	private Set<Pair> edges;
	private Set<Node> vertices;
	private Node entry;
	private Node exit;
	private Map<String, Node> tasks;
	
	private Set<Vertex> nonEmpty = new HashSet<Vertex>();
	private Set<Node> alreadyUsed = new HashSet<Node>();
	private Map<Vertex, Pair> map = new HashMap<Vertex, Pair>();
	private Process proc = new Process();

	private PetriNet pnet;
	
	private int counter = 0;
	
	public UnfoldingRestructurer(Helper helper, UnfoldingHelper unfhelper,
			Set<Pair> edges, Set<Node> vertices, Node entry, Node exit, Map<String, Node> tasks) throws CannotStructureException {
		this.helper = helper;
		this.edges = edges;
		this.vertices = vertices;
		this.entry = entry;
		this.exit = exit;
		this.tasks = tasks;
		this.pnet = unfhelper.getGraph();
		
		process();
	}

	private void process() throws CannotStructureException {
		RPST<Flow,de.hpi.bpt.process.petri.Node> rpst = new RPST<Flow, de.hpi.bpt.process.petri.Node>(pnet);
		
		RPSTNode<Flow, de.hpi.bpt.process.petri.Node> root = rpst.getRoot();
		Set<PNPair> ledges = flattenEdgeSet(root.getFragment().getEdges());
		Set<Vertex> lvertices = new HashSet<Vertex>(root.getFragment().getVertices());
		traverse(rpst, rpst.getRoot(), ledges, lvertices);
		
		lvertices.clear();
		
		for (PNPair pair: ledges) {
			lvertices.add(pair.getFirst());
			lvertices.add(pair.getSecond());
		}
		
		Vertex lentry = root.getEntry();
		Vertex lexit = root.getExit();
		switch (root.getType()) {
		case P:
			visitPolygon(ledges, lvertices, lentry, lexit);
			break;
		case B:
			visitBond(ledges, lvertices, lentry, lexit);
			break;
		case R:
			visitRigid(ledges, lvertices, lentry, lexit);
			break;
		}

		Vertex placeHolder = null;
		for (Vertex v: lvertices)
			if (v instanceof Transition && !v.equals(lentry) && !v.equals(lexit)) {
//				System.out.println("place: " + v);
				placeHolder = v;
			}
		
		Pair pair = map.get(placeHolder);
		
		helper.foldRigidComponent(edges, vertices, entry, exit, proc, pair.getFirst(), pair.getSecond());
	}

	private Set<PNPair> flattenEdgeSet(Collection<Flow> edges2) {
		Set<PNPair> set = new HashSet<PNPair>();
		for (IDirectedEdge<de.hpi.bpt.process.petri.Node> e: edges2)
			set.add(new PNPair(e.getSource(), e.getTarget()));
		return set;
	}

	private void traverse(RPST<Flow, de.hpi.bpt.process.petri.Node> rpst,
			RPSTNode<Flow, de.hpi.bpt.process.petri.Node> current,
			Set<PNPair> edges2,
			Set<Vertex> vertices2) throws CannotStructureException {
		if (current.getType() == TCType.T) return;

		for (RPSTNode<Flow, de.hpi.bpt.process.petri.Node> child: rpst.getChildren(current)) {
			if (child.getType() == TCType.T) continue;
			Set<PNPair> ledges = flattenEdgeSet(child.getFragment().getEdges());
			Set<Vertex> lvertices = new HashSet<Vertex>(child.getFragment().getVertices());
			Set<PNPair> cledges = new HashSet<PNPair>(ledges);
			traverse(rpst, child, ledges, lvertices);
			Vertex entry = child.getEntry();
			Vertex exit = child.getExit();
			switch (child.getType()) {
			case P:
				visitPolygon(ledges, lvertices, entry, exit);
				break;
			case B:
				visitBond(ledges, lvertices, entry, exit);
				break;
			case R:
				visitRigid(ledges, lvertices, entry, exit);
				break;
			}
			edges2.removeAll(cledges);
			vertices2.removeAll(child.getFragment().getVertices());
			edges2.addAll(ledges);
			vertices2.addAll(lvertices);
		}
	}
	
	private void visitRigid(Set<PNPair> edges, Set<Vertex> vertices,
			Vertex entry2, Vertex exit2) throws CannotStructureException {		
		Map<Vertex, List<Vertex>> incoming = new HashMap<Vertex, List<Vertex>>();
		Map<Vertex, List<Vertex>> outgoing = new HashMap<Vertex, List<Vertex>>();

		Set<Vertex> _vertices = extractSubnet(vertices, entry2, exit2,
				incoming, outgoing);
		
		PNDFSLabeler labeler = new PNDFSLabeler(incoming, outgoing, entry2);
		
		if (labeler.isCyclic()) {
			if (!labeler.isMixedLogic())
				processXORCyclicRigid(edges, vertices, entry2, exit2);
			else {
				System.err.println("Found a multi-exit loop embeded inside a parallel block ... still working in this case");
				throw new CannotStructureException("Found a multi-exit loop embeded inside a parallel block ... still working in this case");
			}
		} else {
			if (labeler.getLogic().equals(Place.class))
				processXORAcyclicRigid(edges, vertices, entry2, exit2);
			else
				processGeneralRigid(edges, vertices, entry2, exit2, incoming, outgoing, _vertices);
		}
	}
	
	
	private void processGeneralRigid(Set<PNPair> edges2, Set<Vertex> vertices2,
			Vertex entry2, Vertex exit2, Map<Vertex, List<Vertex>> incoming, Map<Vertex, List<Vertex>> outgoing, Set<Vertex> _vertices) throws CannotStructureException {
						
		final Map<String, Vertex> ltasks = new HashMap<String, Vertex>();
		final Map<String, Vertex> clones = new HashMap<String, Vertex>();

		for (Vertex v: _vertices)
			if (nonEmpty.contains(v))
				ltasks.put(v.getName(), v);

		PNBehavioralProfiler prof = new PNBehavioralProfiler(incoming, outgoing, entry2, ltasks, clones);

		final Map<Node, Vertex> tasksMap = new HashMap<Node, Vertex>();
		final Map<Node, Vertex> clonesMap = new HashMap<Node, Vertex>();
		final Map<String, Node> _tasks = new HashMap<String, Node>();
		final Map<String, Node> _clones = new HashMap<String, Node>();
		
		for (Entry<String, Vertex> ent: ltasks.entrySet()) {
			Task task = new Task(ent.getKey());
			tasksMap.put(task, ent.getValue());
			_tasks.put(ent.getKey(), task);
		}

		for (Entry<String, Vertex> ent: clones.entrySet()) {
			Task task = new Task(ent.getKey());
			clonesMap.put(task, ent.getValue());
			_clones.put(ent.getKey(), task);
		}
		
		final ColoredGraph orgraph = prof.getOrderingRelationsGraph();

//		System.out.println();
//		System.out.println(orgraph.toDot());
		
		ModularDecompositionTree mdec = new ModularDecompositionTree(orgraph);

		for (String label: clones.keySet()) {
			PlaceHolder ph = (PlaceHolder)clones.get(label);
			Node vertexp = new PlaceHolder(ph.getEdges(), ph.getVertices(), ph.getEntry(), ph.getExit());
			// Add code to complete the cloning (e.g. when mapping BPMN->BPEL)
			ltasks.put(label, vertexp);
		}

//		System.out.println(mdec.getRoot());

//		final Process proc = new Process();
		final Map<MDTNode, Node> nestedEntry = new HashMap<MDTNode, Node>();
		final Map<MDTNode, Node> nestedExit = new HashMap<MDTNode, Node>();	

		mdec.traversePostOrder(new MDTVisitor() {
			public void visitLeaf(MDTNode node, String label) {
				Vertex n = ltasks.get(label);
//				proc.addVertex(n);
				nestedEntry.put(node, map.get(n).getFirst());
				nestedExit.put(node, map.get(n).getSecond());
			}
			public void visitComplete(MDTNode node, Set<MDTNode> children, int color) {
				GatewayType type = color == 0 ? GatewayType.AND : GatewayType.XOR;
				Gateway _entry = new Gateway(type);
				Gateway _exit = new Gateway(type);
				proc.addVertex(_entry);
				proc.addVertex(_exit);
				for (MDTNode child : children) {
					proc.addControlFlow(_entry, nestedEntry.get(child));
					proc.addControlFlow(nestedExit.get(child), _exit);
				}
				nestedEntry.put(node, _entry);
				nestedExit.put(node, _exit);				
			}
			public void visitLinear(MDTNode node, List<MDTNode> children) {
				for (int i = 1; i < children.size(); i++) {
					MDTNode _source = children.get(i - 1);
					MDTNode _target = children.get(i);
					Node source = nestedExit.get(_source);
					Node target = nestedEntry.get(_target);
					proc.addControlFlow(source, target);
				}

				MDTNode _entry = children.get(0);
				MDTNode _exit = children.get(children.size() - 1);
				Node entry = nestedEntry.get(_entry);
				Node exit = nestedExit.get(_exit);

				nestedEntry.put(node, entry);
				nestedExit.put(node, exit);
			}

			public void visitPrimitive(MDTNode node, Set<MDTNode> children)
			throws CannotStructureException {
///////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////
///////////// ----------------------   MAXStruct
///////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////
				Map<Integer, MDTNode> proxies = new HashMap<Integer, MDTNode>();
				Map<String, MDTNode> rproxies = new HashMap<String, MDTNode>();
				for (MDTNode child: children) {
					proxies.put(child.getProxy(), child);
					rproxies.put(orgraph.getLabel(child.getProxy()), child);
				}
				
				ColoredGraph subgraph = orgraph.subgraph(proxies.keySet());
				
				Pair pair = new Pair();
				MaxStr maxstr = new MaxStr();
				Process innerProc = new Process();
				maxstr.perform(subgraph, _tasks, _clones, innerProc, pair);
				
				Set<Node> toremove = new HashSet<Node>();
				for (Node n: innerProc.getVertices()) {
					if (tasksMap.containsKey(n)) {
						Vertex v = tasksMap.get(n);
						ControlFlow in = innerProc.getIncomingEdges(n).iterator().next();
						ControlFlow out = innerProc.getOutgoingEdges(n).iterator().next();
						
						in.setTarget(map.get(v).getFirst());
						out.setSource(map.get(v).getSecond());
						toremove.add(n);
					} else if (clonesMap.containsKey(n)) {
						Vertex v = clonesMap.get(n);
						ControlFlow in = innerProc.getIncomingEdges(n).iterator().next();
						ControlFlow out = innerProc.getOutgoingEdges(n).iterator().next();
						
						in.setTarget(map.get(v).getFirst());
						out.setSource(map.get(v).getSecond());
						toremove.add(n);						
					}
				}
				
				innerProc.removeVertices(toremove);
				
				for (Gateway gw: innerProc.getGateways())
					proc.addGateway(gw);
				for (ControlFlow flow: innerProc.getControlFlow()) {
					Node src = flow.getSource();
					Node tgt = flow.getTarget();
					
					if (rproxies.containsKey(src.getName())) {
						MDTNode mdtnode = rproxies.get(src.getName());
						src = nestedExit.get(mdtnode);
					}
					if (rproxies.containsKey(tgt.getName())) {
						MDTNode mdtnode = rproxies.get(tgt.getName());
						tgt = nestedEntry.get(mdtnode);
					}
					
					proc.addControlFlow(src, tgt);
				}
				
				nestedEntry.put(node, pair.getFirst());
				nestedExit.put(node, pair.getSecond());				
				
///////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////
///////////// ----------------------   MAXStruct
///////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////
//				throw new CannotStructureException("FAIL: Cannot structure acyclic - MDT contains primitive");				
			}
			public void openContext(MDTNode node) {}
			public void closeContext(MDTNode node) {}
		});

//		try {
//			String filename = String.format("bpstruct2/proc_rigid_%s.dot", proc.getName());
//			PrintStream out = new PrintStream(filename);
//			out.print(Process2DOT.convert(proc));
//			out.close();
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}
		
		Node first = nestedEntry.get(mdec.getRoot());
		Node last = nestedExit.get(mdec.getRoot());

		foldComponent(edges2, vertices2, entry2, exit2, first, last, false);		

	}

	private Set<Vertex> extractSubnet(Set<Vertex> vertices2, Vertex entry2,
			Vertex exit2, Map<Vertex, List<Vertex>> incoming,
			Map<Vertex, List<Vertex>> outgoing) {
		Set<Vertex> _vertices = new HashSet<Vertex>();
		Set<Flow> _edges = new HashSet<Flow>();
		for (Vertex v: vertices2) {
			if (!v.equals(exit2))
				_edges.addAll(pnet.getOutgoingEdges((de.hpi.bpt.process.petri.Node) v));
			if (!v.equals(entry2))
				_edges.addAll(pnet.getIncomingEdges((de.hpi.bpt.process.petri.Node) v));
		}
				
		for (Flow f: _edges) {
			Vertex src = f.getSource();
			Vertex tgt = f.getTarget();
			if (!_vertices.contains(src)) {
				_vertices.add(src);
				incoming.put(src, new LinkedList<Vertex>());
				outgoing.put(src, new LinkedList<Vertex>());
			}
			if (!_vertices.contains(tgt)) {
				_vertices.add(tgt);
				incoming.put(tgt, new LinkedList<Vertex>());
				outgoing.put(tgt, new LinkedList<Vertex>());
			}
			outgoing.get(src).add(tgt);
			incoming.get(tgt).add(src);
		}
		return _vertices;
	}

	private void processXORCyclicRigid(Set<PNPair> edges2,
			Set<Vertex> vertices2, Vertex entry2, Vertex exit2) {
		for (Vertex v: vertices2)
			if (!map.containsKey(v)) {
				Gateway gw = new Gateway(GatewayType.XOR);
				proc.addGateway(gw);
				map.put(v, new Pair(gw, gw));
			}
		
		for (PNPair e: edges2) {
			Node src = map.get(e.getSource()).getSecond();
			Node tgt = map.get(e.getTarget()).getFirst();
			proc.addControlFlow(src, tgt);
		}
		
		Node first = map.get(entry2).getFirst();
		Node last = map.get(exit2).getSecond();
		map.remove(entry2);
		map.remove(exit2);
		foldComponent(edges2, vertices2, entry2, exit2, first, last, false);
	}
	
	private void processXORAcyclicRigid(Set<PNPair> edges2,
			Set<Vertex> vertices2, Vertex entry2, Vertex exit2) {
		Map<Vertex, Node> linstances = new HashMap<Vertex, Node>();
		Map<Vertex, List<Vertex>> adjlist = GraphUtils.edgelist2adjlist(edges2, exit2);
		
		Gateway first = new Gateway(GatewayType.XOR);
		proc.addGateway(first);
		linstances.put(entry2, first);
		
		Gateway last = new Gateway(GatewayType.XOR);
		proc.addGateway(last);
		linstances.put(exit2, last);
		
		Stack<Vertex> worklist = new Stack<Vertex>();
		worklist.push(entry2);
		while (!worklist.isEmpty()) {
			Vertex _curr = worklist.pop();
			Node curr = null;
			if (map.containsKey(_curr))
				curr = map.get(_curr).getSecond();
			else
				curr = linstances.get(_curr);
			
			for (Vertex _succ: adjlist.get(_curr)) {
				Node succ = null;
				if (!_succ.equals(exit2)) {
					if (map.containsKey(_succ)) {
						succ = map.get(_succ).getFirst();							
					} else if (tasks.containsKey(_succ.getName())) {
						succ = tasks.get(_succ.getName());
						if (alreadyUsed.contains(succ))
							succ = (Node)((PlaceHolder) succ).clone();
						else
							alreadyUsed.add(succ);
					} else if (!linstances.containsKey(_succ)) {
						succ = new Gateway(GatewayType.XOR);
					} else if (linstances.containsKey(_succ)) {
						succ = linstances.get(_succ);
					} 
						
//					proc.addVertex(succ);
					
					if (!linstances.containsKey(_succ)) {
						linstances.put(_succ, succ);
						worklist.push(_succ);
					}
					
				} else {
					succ = linstances.get(exit2);
				}
				proc.addControlFlow(curr, succ);
			}
		}
		foldComponent(edges2, vertices2, entry2, exit2, first, last, false);
	}

	private void visitBond(Set<PNPair> edges, Set<Vertex> vertices,
			Vertex entry2, Vertex exit2) {
		GatewayType type = entry2 instanceof Place ? GatewayType.XOR : GatewayType.AND;
		Gateway first = new Gateway(type);
		Gateway last = new Gateway(type);
		
		proc.addGateway(first);
		proc.addGateway(last);
		
		for (PNPair e: edges) {
			Node src = null, tgt = null;
			if (e.getFirst().equals(entry2))
				src = first;
			else if (e.getFirst().equals(exit2))
				src = last;
			else
				src = map.get(e.getFirst()).getSecond();
			
			if (e.getSecond().equals(exit2))
				tgt = last;
			else if (e.getSecond().equals(entry2))
				tgt = first;
			else
				tgt = map.get(e.getSecond()).getFirst();
			
			proc.addControlFlow(src, tgt);
		}
		
		foldComponent(edges, vertices, entry2, exit2, first, last, false);
	}

	private void visitPolygon(Set<PNPair> edges, Set<Vertex> vertices,
			Vertex entry2, Vertex exit2) {				
		Map<Vertex, Vertex> successor = new HashMap<Vertex, Vertex>();
		for (PNPair e: edges) successor.put(e.getSource(), e.getTarget());
		
		Vertex current = entry2;
		
		Node first = null;
		Node last = null;
		
		while (!current.equals(exit2)) {
			if (tasks.containsKey(current.getName())) {
				Node task = tasks.get(current.getName());
				if (alreadyUsed.contains(task))
					task = (Node)((PlaceHolder) task).clone();
				else
					alreadyUsed.add(task);
				proc.addVertex(task);
				if (first == null)
					first = last = task;
				else {
					proc.addControlFlow(last, task);
					last = task;
				}
			} else if (map.containsKey(current)) {
				Pair pair = map.get(current);
				
				if (first == null)
					first = pair.getFirst();
				else
					proc.addControlFlow(last, pair.getFirst());
				last = pair.getSecond();
			}
			current = successor.get(current);
		}
		
		boolean empty = false;
		if (first == null) {
			Gateway gw = new Gateway(GatewayType.UNDEFINED);
			first = last = gw;
			empty = true;
		}
		
		foldComponent(edges, vertices, entry2, exit2, first, last, empty);
	}

	private void foldComponent(Set<PNPair> edges, Set<Vertex> vertices,
			Vertex entry2, Vertex exit2, Node first, Node last, boolean empty) {
		
		for (Vertex v: vertices)
			if (v.equals(entry2) || v.equals(exit2)) continue;
			else pnet.removeVertex((de.hpi.bpt.process.petri.Node) v);
		

		Transition placeHolder = new Transition("_PlaceHolder" + counter++);
		map.put(placeHolder, new Pair(first, last));
		
		if (!empty)
			nonEmpty.add(placeHolder);
		{
			Place pred = null;
			if (entry2 instanceof Place)
				pred = (Place)entry2;
			else {
				pred = new Place();
				pnet.addNode(pred);
				pnet.addFlow((Transition)entry2, pred);
			}
			pnet.addFlow(pred, placeHolder);
			
			Place succ = null;
			if (exit2 instanceof Place)
				succ = (Place)exit2;
			else {
				succ = new Place();
				pnet.addNode(succ);
				pnet.addFlow(succ, (Transition)exit2);
			}
			pnet.addFlow(placeHolder, succ);
		}
		
		vertices.clear();
		vertices.add(entry2); vertices.add(exit2); vertices.add(placeHolder);
		edges.clear();
		edges.add(new PNPair((de.hpi.bpt.process.petri.Node)entry2, placeHolder));
		edges.add(new PNPair(placeHolder, (de.hpi.bpt.process.petri.Node)exit2));
	}
	
	public void toDOT(String fileName, Set<Vertex> vertices, Map<Vertex, List<Vertex>> outgoing) throws FileNotFoundException {
		PrintStream out = new PrintStream(fileName);
		
		out.println("digraph G {");
		for (Vertex v: vertices)
			if (v instanceof Transition)
				out.printf("\tn%s[shape=box,label=\"%s\"];\n", v.getId().replace("-", ""), v.getName());
			else
				out.printf("\tn%s[shape=circle,label=\"%s\"];\n", v.getId().replace("-", ""), v.getName());
		
		for (Vertex src: vertices)
			for (Vertex tgt: outgoing.get(src))
				out.printf("\tn%s->n%s;\n", src.getId().replace("-", ""), tgt.getId().replace("-", ""));
		
		out.println("}");
		
		out.close();
	}

}
