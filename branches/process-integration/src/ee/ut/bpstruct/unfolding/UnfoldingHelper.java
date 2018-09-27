/* 
 * Copyright (C) 2010 - Luciano Garcia Banuelos, Artem Polyvyanyy
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ee.ut.bpstruct.unfolding;

import hub.top.petrinet.PetriNet;
import hub.top.uma.DNode;
import hub.top.uma.DNodeSys;

import java.io.File;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.bpt.hpi.graph.Edge;
import de.bpt.hpi.graph.Graph;
import de.bpt.hpi.graph.MultiSet;
import ee.ut.bpstruct.CannotStructureException;
import ee.ut.bpstruct.Petrifier;
import ee.ut.bpstruct.RestructurerHelper;
import ee.ut.bpstruct.unfolding.uma.BPstructBPSys;

public class UnfoldingHelper implements RestructurerHelper {
	protected Unfolding unf;

	protected Graph graph = new Graph();
	protected List<Integer> mappedConditions = new LinkedList<Integer>();
	protected Map<DNode, Integer> map = new HashMap<DNode, Integer>();
	protected Map<Integer, DNode> rmap = new HashMap<Integer, DNode>();
	protected DNode exitCond = null;
	protected RestructurerHelper helper;
	
	protected PetriNet rewiredNet = new PetriNet();

	protected void initialize() {
		graph = new Graph();
		mappedConditions = new LinkedList<Integer>();
		map = new HashMap<DNode, Integer>();
		rmap = new HashMap<Integer, DNode>();
		exitCond = null;
	}
	public UnfoldingHelper(RestructurerHelper helper, Unfolding unf) {
		this.helper = helper;
		this.unf = unf;
	}
	
	protected Integer getCondition(DNode n) {
		if (n.post == null) {
			if (exitCond == null)
				exitCond = n;
			n = exitCond;
		}
		
		Integer condition = map.get(n);
		if (condition == null) {
			condition = graph.addVertex(String.format("\"%s\"",n.toString()));
			map.put(n, condition);
			rmap.put(condition, n);
			mappedConditions.add(condition);
		}

		return condition;
	}
	
	public Integer getVertex(DNode n) {
		return map.get(n);
	}
	
	public DNode getDNode(Integer v) {
		return rmap.get(v);
	}
	
	public void rewire() {
		initialize();
		for (DNode _event: unf.getAllEvents()) {
			Integer event = map.get(_event);
			if (event == null) {
				event = graph.addVertex(String.format("\"%s\"",_event.toString()));
				map.put(_event, event);
				rmap.put(event, _event);
			}
			
			for (DNode _cond: _event.pre) {
				Integer cond = getCondition(_cond);
				graph.addEdge(cond, event);
			}
			
			for (DNode _cond: _event.post) {
				Integer cond = null;
				DNode _condp = null;
				if (unf.getCutoffs().contains(_event)) {
					_condp = unf.elementary_ccPair.get(_cond);
					cond = getCondition(_condp);
					map.put(_cond, cond);
				} else
					cond = getCondition(_cond);
				graph.addEdge(event, cond);
			}
		}
		
		List<Integer> tosplit = new LinkedList<Integer>();
		for (Integer v: mappedConditions)
			if (graph.getIncomingEdges(v).size() > 1 && graph.getOutgoingEdges(v).size() > 1) {
				tosplit.add(v);
				Integer vp = graph.addVertex("_" + graph.getLabel(v) + "_");
				helper.setXORGateway(vp);
				rmap.put(vp, rmap.get(v));
				for (Edge edge: graph.getOutgoingEdges(v))
					edge.setSource(vp);
				graph.addEdge(v, vp);
			}
	}
	
	public void rewire(Set<DNode> properRepCutoffs) {		
		initialize();
		for (DNode _event: unf.getAllEvents()) {
			Integer event = map.get(_event);
			if (event == null) {
				event = graph.addVertex(String.format("\"%s\"",_event.toString()));
				map.put(_event, event);
				rmap.put(event, _event);
			}
			
			for (DNode _cond: _event.pre) {
				Integer cond = getCondition(_cond);
				graph.addEdge(cond, event);
			}
			
			for (DNode _cond: _event.post) {
				Integer cond = null;
				DNode _condp = null;
				if (unf.getCutoffs().contains(_event) && !properRepCutoffs.contains(_event)) {
					_condp = unf.elementary_ccPair.get(_cond);
					cond = getCondition(_condp);
					map.put(_cond, cond);
				} else
					cond = getCondition(_cond);
				graph.addEdge(event, cond);
			}
		}
		
		List<Integer> tosplit = new LinkedList<Integer>();
		for (Integer v: mappedConditions)
			if (graph.getIncomingEdges(v).size() > 1 && graph.getOutgoingEdges(v).size() > 1) {
				tosplit.add(v);
				Integer vp = graph.addVertex("_" + graph.getLabel(v) + "_");
				helper.setXORGateway(vp);
				rmap.put(vp, rmap.get(v));
				for (Edge edge: graph.getOutgoingEdges(v))
					edge.setSource(vp);
				graph.addEdge(v, vp);
			}
	}

	public void rewire2() {
		initialize();

		Set<Integer> vertices2remove = new HashSet<Integer>();
		MultiSet<Edge> edges2remove = new MultiSet<Edge>();
		for (DNode _event: unf.getAllEvents()) {
			if (_event.isCutOff && _event.post.length > 1) {
				DNode _corr = unf.getCorr(_event);
				DNode _precondp = _event.pre[0];
				DNode _precond = _corr.pre[0];
				for (DNode _preevent: _precondp.pre) {
					if (_preevent.post.length > 1) {
						System.err.println("Check the topology of this unfolding ... seems to be very special");
						System.exit(-1);
					}
					_preevent.post[0] = _precond;
					_precond.addPreNode(_preevent);
					graph.addEdge(map.get(_preevent), getCondition(_precond));
					vertices2remove.add(getCondition(_precondp));
					edges2remove.add(new Edge(map.get(_preevent), getCondition(_precondp)));
				}
				
			} else {
				Integer event = map.get(_event);
				if (event == null) {
					event = graph.addVertex(unf.getProperName(_event));
					map.put(_event, event);
					rmap.put(event, _event);
				}
				
				for (DNode _cond: _event.pre) {
					Integer cond = getCondition(_cond);
					graph.addEdge(cond, event);
				}
				
				for (DNode _cond: _event.post) {
					Integer cond = null;
					if (_cond.isCutOff) {
						cond = getCondition(unf.elementary_ccPair.get(_cond));
						map.put(_cond, cond);
					} else
						cond = getCondition(_cond);
					graph.addEdge(event, cond);
				}
			}
		}
		
		graph.removeVertices(vertices2remove);
		graph.removeEdges(edges2remove);
		
		List<Integer> tosplit = new LinkedList<Integer>();
		for (Integer v: mappedConditions)
			if (graph.getIncomingEdges(v).size() > 1 && graph.getOutgoingEdges(v).size() > 1) {
				tosplit.add(v);
				Integer vp = graph.addVertex("_" + graph.getLabel(v) + "_");
				helper.setXORGateway(vp);
				rmap.put(vp, rmap.get(v));
				for (Edge edge: graph.getOutgoingEdges(v))
					edge.setSource(vp);
				graph.addEdge(v, vp);
			}
	}

	public Graph getGraph() {
		return graph;
	}

	public Object gatewayType(Integer vertex) {
		return rmap.get(vertex);
	}

	public Unfolding extractSubnetFromAbstracted(Graph graph2, Set<Edge> edges,
			Set<Integer> vertices, Integer entry, Integer exit) {
		Unfolding unf = new Unfolding(this.unf.brproc);
		DNodeSys dnsys = this.unf.dnodesys;
		Map<Integer, DNode> lmap = new HashMap<Integer, DNode>();
		
		for (Integer vertex: vertices) {
			DNode node = rmap.get(vertex);
			if (node == null) {
				short id = (short)dnsys.nameToID.size();
				dnsys.nameToID.put(graph2.getLabel(vertex), id);
				node = new DNode(id, 0);
				node.isEvent = true;
			} else {
				boolean isEvent = node.isEvent;
				node = new DNode(node.id, 0);
				node.isEvent = isEvent;
				if (!node.isEvent)
					System.out.println("Found a condition");
			}
			
			if (node.isEvent)
				unf.allEvents.add(node);
			else
				unf.allConditions.add(node);
			lmap.put(vertex, node);
		}
		
		for (Edge edge: edges) {
			DNode src = lmap.get(edge.getSource());
			DNode tgt = lmap.get(edge.getTarget());
			
			if (src.isEvent && tgt.isEvent) {
				short id = (short)dnsys.nameToID.size();
				dnsys.nameToID.put(String.valueOf(id), id);
				DNode cond = new DNode(id, 1);
				
				unf.allConditions.add(cond);
				
				src.addPostNode(cond);
				cond.pre[0] = src;
				cond.addPostNode(tgt);
				tgt.addPreNode(cond);
				
			} else if (!src.isEvent && !tgt.isEvent){
				short id = (short)dnsys.nameToID.size();
				dnsys.nameToID.put(String.valueOf(id), id);
				DNode event = new DNode(id, 1);
				event.isEvent = true;
				
				unf.allEvents.add(event);
				
				src.addPostNode(event);
				event.pre[0] = src;
				event.addPostNode(tgt);
				tgt.addPreNode(event);
			} else {
				src.addPostNode(tgt);
				tgt.addPreNode(src);
			}
		}
		
		short id = (short)dnsys.nameToID.size();
		dnsys.nameToID.put(String.valueOf(id), id);
		DNode cond = new DNode(id, 0);
		DNode event = lmap.get(entry);
		cond.addPostNode(event);
		event.addPreNode(cond);
		unf.allConditions.add(cond);
		unf.initialConditions.add(cond);

		DNode dnexit = lmap.get(exit);
		if (dnexit.isEvent) {
			id = (short)dnsys.nameToID.size();
			dnsys.nameToID.put(String.valueOf(id), id);
			cond = new DNode(id, 0);
			cond.addPreNode(dnexit);
			dnexit.addPostNode(cond);
			unf.allConditions.add(cond);
		}
		
		((BPstructBPSys)dnsys).packageProperNames();
		return unf;
	}
	
	public File getDebugDir() { return null; }

	public String getModelName() { return null; }

	public Petrifier getPetrifier(Set<Integer> vertices, Set<Edge> edges,
			Integer entry, Integer exit) { return null; }

	public void processOrderingRelations(Set<Edge> edges,
			Set<Integer> vertices, Integer entry, Integer exit, Graph graph,
			Unfolding unf, Map<String, Integer> tasks)
			throws CannotStructureException {}

	public void setANDGateway(Integer vertex) {}

	public void setXORGateway(Integer vertex) {}

	public Object getModelElementId(Integer vertex) { return null; }

	public boolean isChoice(Integer vertex) { return false; }

	public boolean isParallel(Integer vertex) { return false; }

	public String toDot(Set<Integer> vertices, Set<Edge> edges) { return null; }

	public Integer foldComponent(Graph graph, Set<Edge> edges,
			Set<Integer> vertices, Integer entry, Integer exit, BLOCK_TYPE type) {
		return null; }

	public void installStructured() {}

	public void serializeDot(PrintStream out) {}
	
	public void dematerializeDecisions() {}
	
	public void materializeDecisions() {}
}
