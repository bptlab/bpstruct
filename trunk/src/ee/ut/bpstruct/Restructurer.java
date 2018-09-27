package ee.ut.bpstruct;

import hub.top.petrinet.PetriNet;
import hub.top.petrinet.Place;
import hub.top.petrinet.Transition;

import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Map.Entry;

import de.hpi.bpt.graph.abs.AbstractDirectedEdge;
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
import de.hpi.bpt.process.serialize.Process2DOT;
import de.hpi.bpt.utils.IOUtils;
import ee.ut.bpstruct.jbpt.Pair;
import ee.ut.bpstruct.jbpt.PlaceHolder;
import ee.ut.bpstruct.util.GraphUtils;
import ee.ut.bpstruct.util.ProcessUtils;
import ee.ut.graph.moddec.ColoredGraph;
import ee.ut.graph.moddec.MDTNode;
import ee.ut.graph.moddec.MDTVisitor;
import ee.ut.graph.moddec.ModularDecompositionTree;
import ee.ut.graph.moddec.MDTNode.NodeType;

public class Restructurer implements Helper {
	public Process proc;
	Set<Node> labeledElements = new HashSet<Node>();
	private Visitor visitor;
	private int nodeCloneCount = 0;

	private PrintStream profiling = null;
	private boolean maxStrRequired;
	
	public Restructurer(Process proc) {
		this(proc, new FullVisitorFactory());
	}
	
	private Restructurer(Process proc, VisitorFactory factory) {
		this.proc = proc;
		this.visitor = factory.createVisitor(this);
	}
		
	public void setProfiling(PrintStream profiling) {
		this.profiling = profiling;
	}

	public boolean perform() {
		boolean result = true;
		ProcessUtils putils = new ProcessUtils();
//		IOUtils.toFile(String.format("bpstruct2/proc_%s.dot", proc.getName()), Process2DOT.convert(proc));

		putils.materializeDecisions(proc);

		labeledElements.clear();
		labeledElements.addAll(proc.getTasks());

		RPST<ControlFlow, Node> rpst = new RPST<ControlFlow, Node>(proc);

		if (rpst.getVertices(TCType.R).size() >= 0) {
			RPSTNode<ControlFlow, Node> root = rpst.getRoot();
			Set<Pair> edges = flattenEdgeSet(root.getFragment().getEdges());
			Set<Node> vertices = new HashSet<Node>(root.getFragment().getVertices());
			try {
				traverse(rpst, root, edges, vertices);
				proc = installStructured(edges, vertices, root);
			} catch (CannotStructureException e) {
				result = false;
			}
		}
		
		putils.dematerializeDecisions(proc);
		return result;
	}
	
	private void traverse(RPST<ControlFlow, Node> rpst, RPSTNode<ControlFlow, Node> current,
			Set<Pair> edges, Set<Node> vertices) throws CannotStructureException {
		if (current.getType() == TCType.T) return;

		for (RPSTNode<ControlFlow, Node> child: rpst.getChildren(current)) {
			if (child.getType() == TCType.T) continue;
			Set<Pair> ledges = flattenEdgeSet(child.getFragment().getEdges());
			Set<Node> lvertices = new HashSet<Node>(child.getFragment().getVertices());
			Set<Pair> cledges = new HashSet<Pair>(ledges);
			traverse(rpst, child, ledges, lvertices);
			Node entry = child.getEntry();
			Node exit = child.getExit();
			switch (child.getType()) {
			case P:
				visitor.visitPolygon(proc, ledges, lvertices, entry, exit);
				break;
			case B:
				visitor.visitBond(proc, ledges, lvertices, entry, exit);
				break;
			case R:
				visitor.visitRigid(proc, ledges, lvertices, entry, exit);
				break;
			}
			edges.removeAll(cledges);
			vertices.removeAll(child.getFragment().getVertices());
			edges.addAll(ledges);
			vertices.addAll(lvertices);
		}
	}

	public Set<Pair> flattenEdgeSet(Collection<ControlFlow> edges) {
		Set<Pair> set = new HashSet<Pair>();
		for (AbstractDirectedEdge<Node> flow: edges)
			set.add(new Pair(flow.getSource(), flow.getTarget()));
		return set;
	}
	
	public void synthesizeFromOrderingRelations(Process proc, Set<Pair> edges,
			Set<Node> vertices, Node entry, Node exit, Unfolding unf,
			final Map<String, Node> tasks) throws CannotStructureException {
		final Map<String, Vertex> tasksp = new HashMap<String, Vertex>(tasks);
		Map<String, Vertex> clones = new HashMap<String, Vertex>();
		BehavioralProfiler prof = new BehavioralProfiler(unf, tasksp, clones);
		
		final Map<String, Node> clonesp = new HashMap<String, Node>();
		for (Entry<String, Vertex> ent: clones.entrySet())
			clonesp.put(ent.getKey(), (Node) ent.getValue());
		
		final ColoredGraph orgraph = prof.getOrderingRelationsGraph();
		
		// Compute the Modular Decomposition Tree
		ModularDecompositionTree mdec = new ModularDecompositionTree(orgraph);

//		final Map<String, Node> taskspp = new HashMap<String, Node>(tasks);

		for (String label: clones.keySet()) {
			PlaceHolder ph = (PlaceHolder)clones.get(label);
			Node vertexp = new PlaceHolder(ph.getEdges(), ph.getVertices(), ph.getEntry(), ph.getExit());
			vertexp.setName(ph.getName());
			// Add code to complete the cloning (e.g. when mapping BPMN->BPEL)
			tasks.put(label, vertexp);
		}		
		
		if (profiling != null)
			profiling.print("\t" + unf.getAllEvents().size() + "\t" + unf.getAllConditions().size() + "\t" + mdec.getRoot());
		
		maxStrRequired = false;
		
		final Process childProc = new Process();
		final Map<MDTNode, Node> nestedEntry = new HashMap<MDTNode, Node>();
		final Map<MDTNode, Node> nestedExit = new HashMap<MDTNode, Node>();	

		mdec.traversePostOrder(new MDTVisitor() {
			public void visitLeaf(MDTNode node, String label) {
				Node n = tasks.get(label);
//				childProc.addVertex(n);
				nestedEntry.put(node, n);
				nestedExit.put(node, n);
			}
			public void visitComplete(MDTNode node, Set<MDTNode> children, int color) {
				GatewayType type = color == 0 ? GatewayType.AND : GatewayType.XOR;
				Gateway _entry = new Gateway(type);
				Gateway _exit = new Gateway(type);
//				childProc.addVertex(_entry);
//				childProc.addVertex(_exit);
				for (MDTNode child : children) {
					childProc.addControlFlow(_entry, nestedEntry.get(child));
					childProc.addControlFlow(nestedExit.get(child), _exit);
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
					childProc.addControlFlow(source, target);
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
				Map<String, Node> clonespp = new HashMap<String, Node>();
				Map<String, Node> tasksppp = new HashMap<String, Node>();
				for (MDTNode child: children) {
					String label = orgraph.getLabel(child.getProxy());
					if (child.getType() == NodeType.LEAF) {
						if (clonesp.containsKey(label))
							clonespp.put(label, clonesp.get(label));
						else
							tasksppp.put(label, tasks.get(label));
					} else
						tasksppp.put(label, new Task(label));
						
					proxies.put(child.getProxy(), child);
					rproxies.put(orgraph.getLabel(child.getProxy()), child);
				}
				
				Set<String> toremove = new HashSet<String>();
				for (String label: clonespp.keySet()) {
					Node task = clonespp.get(label);
					if (!tasksppp.containsKey(task.getName())) {
						tasksppp.put(label, new Task(label));
						toremove.add(label);
					}
				}
				
				for (String label: toremove)
					clonespp.remove(label);
				
				ColoredGraph subgraph = orgraph.subgraph(proxies.keySet());
				
				maxStrRequired = true;

				
				Pair pair = new Pair();
				MaxStr maxstr = new MaxStr();
				Process innerProc = new Process();
				maxstr.perform(subgraph, tasksppp, clonespp, innerProc, pair);				
				RPST<ControlFlow, Node> rpst = new RPST<ControlFlow, Node>(innerProc);
				
				if (rpst.getVertices(TCType.R).size() > 0) {
					RPSTNode<ControlFlow, Node> rigid = rpst.getVertices(TCType.R).iterator().next();
					if (((Gateway)rigid.getEntry()).getGatewayType().equals(GatewayType.XOR)) {
					
					innerProc.addControlFlow(new Task("_dummy_entry_"), pair.getFirst());
					innerProc.addControlFlow(pair.getSecond(), new Task("_dummy_exit_"));
					Restructurer recstr = new Restructurer(innerProc, new RestrictedVisitorFactory());
					recstr.perform();
					innerProc = recstr.proc;
					Set<Node> entries = new HashSet<Node>(innerProc.getNodes());
					Set<Node> exits = new HashSet<Node>(innerProc.getNodes());
					for (ControlFlow flow: innerProc.getControlFlow()) {
						exits.remove(flow.getSource());
						entries.remove(flow.getTarget());
					}
					
					Node tentry = entries.iterator().next();
					Node texit = exits.iterator().next();
					pair.setFirst(innerProc.getSuccessors(tentry).iterator().next());
					pair.setSecond(innerProc.getPredecessors(texit).iterator().next());
					innerProc.removeVertex(tentry);
					innerProc.removeVertex(texit);
					}
				}
		

				Set<String> alreadyUsed = new HashSet<String>();
				Map<Node, Pair> rproxiesp = new HashMap<Node, Pair>();
				for (Node innode: innerProc.getNodes()) {
					if (innode instanceof Task) {
					String label = innode.getName();
					MDTNode mdtnode = rproxies.get(label);
					if (!alreadyUsed.contains(label)) {
						rproxiesp.put(innode, new Pair(nestedEntry.get(mdtnode), nestedExit.get(mdtnode)));
						alreadyUsed.add(label);
					} else {
						Pair innerPair = new Pair();
						cloneInner(innerProc, childProc, nestedEntry.get(mdtnode), nestedExit.get(mdtnode), innerPair);
						innode.setName(innode.getName() + "_" + nodeCloneCount++);
						rproxiesp.put(innode, innerPair);
					}
					}
				}

				for (ControlFlow flow: innerProc.getControlFlow()) {
					Node src = flow.getSource();
					Node tgt = flow.getTarget();
					
					if (rproxiesp.containsKey(src))
						src = rproxiesp.get(src).getSecond();
					if (rproxiesp.containsKey(tgt))
						tgt = rproxiesp.get(tgt).getFirst();
					childProc.addControlFlow(src, tgt);
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
			
			private void cloneInner(Process proc, Process childProc, Node entry, Node exit,
					Pair innerPair) {
				Map<Node, Node> map = new HashMap<Node, Node>();
				if (!entry.equals(exit)) {
					Stack<Node> worklist = new Stack<Node>();
					worklist.add(entry);
					while (!worklist.isEmpty()) {
						Node curr = worklist.pop();
						Node _curr = cloneNode(map, curr);
						for (Node succ: childProc.getSuccessors(curr)) {
							Node _succ = cloneNode(map, succ);
							childProc.addControlFlow(_curr, _succ);
							if (!exit.equals(succ))
								worklist.push(succ);
						}
					}
				}
				innerPair.setFirst(cloneNode(map, entry));
				innerPair.setSecond(cloneNode(map, exit));
			}
			
			private Node cloneNode(Map<Node, Node> map, Node curr) {
				if (!map.containsKey(curr)) {
					if (curr instanceof Gateway) {
						Gateway _curr = new Gateway(((Gateway)curr).getGatewayType());
						map.put(curr, _curr);
					} else {
						PlaceHolder _ph = (PlaceHolder) curr;
						PlaceHolder ph = new PlaceHolder(_ph.getEdges(), _ph.getVertices(), _ph.getEntry(), _ph.getExit());
						ph.setName(curr.getName());
						map.put(curr, ph);
					}
				}
				return map.get(curr);
			}
			
			public void openContext(MDTNode node) {}
			public void closeContext(MDTNode node) {}
		});
		
		Node _entry = nestedEntry.get(mdec.getRoot());
		Node _exit = nestedExit.get(mdec.getRoot());

		foldRigidComponent(edges, vertices, entry, exit, childProc, _entry,
				_exit);
		
		if (profiling != null)
			profiling.print("\t" + maxStrRequired);
	}

	private Process installStructured(Set<Pair> edges,
			Set<Node> vertices, RPSTNode<ControlFlow, Node> root) {
		Process nproc = new Process();
		Pair pair = new Pair();
		installStructured(nproc, edges, vertices, root.getEntry(), root.getExit(), pair);
				
		edges = flattenEdgeSet(nproc.getEdges());
		
		
		Map<Node, List<Node>> adjlist = GraphUtils.edgelist2adjlist(edges, pair.getSecond());
		Map<Node, ControlFlow> toremove = new HashMap<Node, ControlFlow>();
		for (Gateway gw: nproc.getGateways())
			if (nproc.getIncomingEdges(gw).size() == 1 && nproc.getOutgoingEdges(gw).size() == 1)
				toremove.put(gw, nproc.getIncomingEdges(gw).iterator().next());
		
		Set<Node> visited = new HashSet<Node>();
		
		simplify(adjlist, pair.getFirst(), toremove, nproc, visited, pair.getSecond());
		
		nproc.removeVertices(toremove.keySet());
		
		Set<Node> vertexSet = new HashSet<Node>(nproc.getVertices());
		vertexSet.removeAll(visited);
		nproc.removeVertices(vertexSet);		

		return nproc;
	}

	/**
	 * This method does a depth-first traversal to update control flow so as to skip superfluous gateways.
	 */
	protected void simplify(Map<Node, List<Node>> adjlist, Node curr,
			Map<Node, ControlFlow> toremove, Process nproc, Set<Node> visited, Node last) {
		visited.add(curr);
		if (!toremove.containsKey(curr))
			last = curr;
		for (Node succ: adjlist.get(curr)) {
			if (toremove.containsKey(succ))
				nproc.removeEdge(toremove.get(succ));
			else
				nproc.addControlFlow(last, succ);
			if (!visited.contains(succ))
				simplify(adjlist, succ, toremove, nproc, visited, last);
		}
	}

	private void installStructured(Process nproc,
			Set<Pair> edges, Set<Node> vertices, Node entry, Node exit, Pair pair) {
		Map<Node, Pair> lmap = new HashMap<Node, Pair>();

		for (Node v: vertices) {
//			System.out.println("Analyzing: " + v);
			if (v instanceof PlaceHolder) {
				PlaceHolder pholder = (PlaceHolder) v;
				Pair cpair = new Pair();
				installStructured(nproc, pholder.getEdges(), pholder.getVertices(), pholder.getEntry(), pholder.getExit(), cpair);
				lmap.put(v, cpair);
			} else {
				Node nv = null;
				if (v instanceof Gateway)
					nv = new Gateway(((Gateway)v).getGatewayType(), v.getName());
				else 
					nv = new Task(v.getName(), v.getDescription());
				nproc.addVertex(nv);
				lmap.put(v, new Pair(nv, nv));
			}
		}
		for (Pair e: edges) {
			if (lmap.containsKey(e.getSource()) && lmap.containsKey(e.getTarget())) {
				Node src = lmap.get(e.getSource()).getSecond();
				Node tgt = lmap.get(e.getTarget()).getFirst();
				nproc.addControlFlow(src, tgt);
			}
		}
		
		pair.setFirst(lmap.get(entry).getFirst());
		pair.setSecond(lmap.get(exit).getSecond());
	}


	private hub.top.petrinet.Node getNode(Node node, PetriNet net, Map<Node, hub.top.petrinet.Node> map) {
		hub.top.petrinet.Node res = map.get(node);
		if (res==null) {
			if (isXORGateway(node) || isORGateway(node))
				res = net.addPlace(node.getName());
			else
				res = net.addTransition(node.getName());			
			map.put(node, res);
		}
		return res;
	}

	public PetriNet petrify(Set<Pair> ledges,
			Set<Node> vertices, Node _entry, Node _exit) {
		Map<Node, hub.top.petrinet.Node> map = new HashMap<Node, hub.top.petrinet.Node>();
		hub.top.petrinet.Node entry = null, exit = null;
		PetriNet net = new PetriNet();

		for (Pair edge : ledges) {
			Node src = edge.getSource();
			Node tgt = edge.getTarget();
			
			if (labeledElements.contains(src) || isANDGateway(src)) {
				if (labeledElements.contains(tgt) || isANDGateway(tgt)) {
					Transition psrc = (Transition)getNode(src, net, map);
					Transition ptgt = (Transition)getNode(tgt, net, map);
					Place p = net.addPlace(psrc.getName() + "_" + ptgt.getName());
					net.addArc(psrc, p);
					net.addArc(p, ptgt);
				} else if (isXORGateway(tgt)) {
					Transition psrc = (Transition)getNode(src, net, map);					
					Place ptgt = (Place)getNode(tgt, net, map);
					net.addArc(psrc, ptgt);
				}
			} else if (isXORGateway(src)) {
				if (labeledElements.contains(tgt) || isANDGateway(tgt)) {
					Place psrc = (Place)getNode(src, net, map);
					Transition ptgt = (Transition)getNode(tgt, net, map);

					Place pintp = net.addPlace(psrc.getName() + "_p_" + ptgt.getName());
					Transition pintt = net.addTransition(psrc.getName() + "_t_" + ptgt.getName());
					net.addArc(psrc, pintt);
					net.addArc(pintt, pintp);
					net.addArc(pintp, ptgt);
				} else if (isXORGateway(tgt)) {
					Place psrc = (Place)getNode(src, net, map);
					Place ptgt = (Place)getNode(tgt, net, map);
					Transition inter = net.addTransition(psrc.getName() + "_" + ptgt.getName());
					net.addArc(psrc, inter);
					net.addArc(inter, ptgt);
				}
			}
		}

		// fix entry/exit
		entry = getNode(_entry, net, map);
		exit = getNode(_exit, net, map);

		if (entry instanceof Transition) {
			Place p = net.addPlace("_entry_");
			net.addArc(p, (Transition)entry);
			net.setTokens(p, 1);
		}
		else if (hasInternalIncoming(_entry, ledges)) {
			Place p = net.addPlace("_entry_");
			Transition t = net.addTransition("_from_entry_");

			net.addArc(p, t);
			net.addArc(t, (Place)entry);
			net.setTokens(p, 1);
		} else
			net.setTokens((Place)entry, 1);

		if (exit instanceof Transition) {
			Place p = net.addPlace("_exit_");
			net.addArc((Transition)exit, p);
		}

		if (exit instanceof Place && isXORGateway(_exit) && hasInternalOutgoing(_exit, ledges)) {
			Transition t = net.addTransition("_to_exit_");
			Place p = net.addPlace("_exit_");
			net.addArc((Place)exit, t);
			net.addArc(t, p);
		}

		return net;
	}

	private boolean hasInternalIncoming(Node node, Set<Pair> ledges) {
		for (Pair e: ledges)
			if (node.equals(e.getTarget()))
				return true;
		return false;
	}
	private boolean hasInternalOutgoing(Node node, Set<Pair> edges) {
		for (Pair e: edges)
			if (node.equals(e.getSource()))
				return true;
		return false;
	}
	private boolean isANDGateway(Node node) {
		return node instanceof Gateway && ((Gateway)node).getGatewayType() == GatewayType.AND;
	}
	private boolean isXORGateway(Node node) {
		return node instanceof Gateway && ((Gateway)node).getGatewayType() == GatewayType.XOR;
	}
	private boolean isORGateway(Node node) {
		return node instanceof Gateway && ((Gateway)node).getGatewayType() == GatewayType.OR;
	}

	int count = 0;
	public void foldComponent(Set<Pair> ledges,
			Set<Node> vertices, Node entry, Node exit) {
		Node placeHolder = new PlaceHolder(ledges, vertices, entry, exit);
		labeledElements.add(placeHolder);
		placeHolder.setName("_N_"+count++);
		vertices.clear();
		ledges.clear();
		vertices.add(entry); vertices.add(exit); vertices.add(placeHolder);
		ledges.add(new Pair(entry, placeHolder));
		ledges.add(new Pair(placeHolder, exit));		
	}
	
	public void foldRigidComponent(Set<Pair> ledges, Set<Node> vertices,
			Node entry, Node exit, final Process childProc, Node _entry,
			Node _exit) {
		Node placeHolder = new PlaceHolder(flattenEdgeSet(childProc.getEdges()), (Collection)childProc.getVertices(), _entry, _exit);
		getLabeledElements().add(placeHolder);
		placeHolder.setName("_N_"+count ++);
		vertices.clear();
		ledges.clear();
		vertices.add(entry); vertices.add(exit); vertices.add(placeHolder);
		ledges.add(new Pair(entry, placeHolder));
		ledges.add(new Pair(placeHolder, exit));
	}

	public Set<Node> getLabeledElements() {
		return labeledElements;
	}
}
