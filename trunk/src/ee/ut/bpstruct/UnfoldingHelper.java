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
package ee.ut.bpstruct;

import hub.top.uma.DNode;
import hub.top.uma.DNodeSys;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.hpi.bpt.graph.abs.IDirectedEdge;
import de.hpi.bpt.hypergraph.abs.Vertex;
import de.hpi.bpt.process.petri.Flow;
import de.hpi.bpt.process.petri.Node;
import de.hpi.bpt.process.petri.PetriNet;
import de.hpi.bpt.process.petri.Place;
import de.hpi.bpt.process.petri.Transition;
import ee.ut.bpstruct.jbpt.PNPair;
import ee.ut.bpstruct.unfolding.uma.BPstructBPSys;


public class UnfoldingHelper {
	protected Unfolding unf;

	protected PetriNet graph;
	protected List<Node> mappedConditions;
	protected Map<DNode, Node> map;
	protected Map<Node, DNode> rmap;
	protected DNode exitCond;

	public UnfoldingHelper(Unfolding unf) {
		this.unf = unf;
	}
	
	public PetriNet getGraph() {
		return graph;
	}
	
	protected void initialize() {
		graph = new PetriNet();
		mappedConditions = new LinkedList<Node>();
		map = new HashMap<DNode, Node>();
		rmap = new HashMap<Node, DNode>();
		exitCond = null;
	}

	public Node getVertex(DNode n) {
		return map.get(n);
	}

	public DNode getDNode(Node v) {
		return rmap.get(v);
	}
	
	protected Node getCondition(DNode n) {
		if (n.post == null) {
			if (exitCond == null)
				exitCond = n;
			n = exitCond;
		}
		Node condition = map.get(n);
		if (condition == null) {
			condition = new Place(String.format("%s",n.toString()));
			graph.addVertex(condition);
			map.put(n, condition);
			rmap.put(condition, n);
			mappedConditions.add(condition);
		}
		return condition;
	}

	public void rewire(Set<DNode> properRepCutoffs) {
		initialize();
		for (DNode _event: unf.getAllEvents()) {
			Node event = map.get(_event);
			if (event == null) {
				event = new Transition(String.format("\"%s\"",_event.toString()));
				graph.addVertex(event);
				map.put(_event, event);
				rmap.put(event, _event);
			}

			for (DNode _cond: _event.pre) {
				Node cond = getCondition(_cond);
				graph.addFlow(cond, event);
			}

			for (DNode _cond: _event.post) {
				if (_cond == null) continue;
				Node cond = null;
				DNode _condp = null;
				if (unf.getCutoffs().contains(_event) && (properRepCutoffs == null || !properRepCutoffs.contains(_event))) {
					_condp = unf.elementary_ccPair.get(_cond);
//					if (_condp == null) continue;
					
					cond = getCondition(_condp);
					map.put(_cond, cond);
				} else
					cond = getCondition(_cond);
				graph.addFlow(event, cond);
			}
		}

		List<Node> tosplit = new LinkedList<Node>();
		for (Node v: mappedConditions)
			if (graph.getIncomingEdges(v).size() > 1 && graph.getOutgoingEdges(v).size() > 1) {
				tosplit.add(v);
				Node vp = new Place("_" + v.getName() + "_");
				graph.addVertex(vp);
				rmap.put(vp, rmap.get(v));
				for (IDirectedEdge<Node> edge: graph.getOutgoingEdges(v))
					edge.setSource(vp);
				graph.addFlow(v, vp);
			}
	}

	public void rewire() {
		rewire(null);
	}


	public void rewire2() {
		initialize();

		Set<Node> vertices2remove = new HashSet<Node>();
		Set<Flow> edges2remove = new HashSet<Flow>();
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
					graph.addFlow(map.get(_preevent), getCondition(_precond));
					vertices2remove.add(getCondition(_precondp));
					edges2remove.add(graph.getDirectedEdge(map.get(_preevent), getCondition(_precondp)));
				}
				
			} else {
				Node event = map.get(_event);
				if (event == null) {
					event = new Transition(unf.getProperName(_event));
					graph.addVertex(event);
					map.put(_event, event);
					rmap.put(event, _event);
				}
				
				for (DNode _cond: _event.pre) {
					Node cond = getCondition(_cond);
					graph.addFlow(cond, event);
				}
				
				for (DNode _cond: _event.post) {
					Node cond = null;
					if (_cond.isCutOff) {
						DNode _condp = unf.elementary_ccPair.get(_cond);
//						if (_condp == null) continue;
						cond = getCondition(_condp);
						map.put(_cond, cond);
					} else
						cond = getCondition(_cond);
					graph.addFlow(event, cond);
				}
			}
		}
		
		graph.removeVertices(vertices2remove);
		graph.removeEdges(edges2remove);
		
//		List<Node> tosplit = new LinkedList<Node>();
		for (Node v: mappedConditions)
			if (graph.getIncomingEdges(v).size() > 1 && graph.getOutgoingEdges(v).size() > 1) {
//				tosplit.add(v);
				Node vt = new Transition("_" + v.getName() + "_");
				Node vp = new Place("_" + v.getName() + "_");
				graph.addVertex(vp);
				rmap.put(vp, rmap.get(v));
				for (IDirectedEdge<Node> edge: graph.getOutgoingEdges(v))
					edge.setSource(vp);
				graph.addFlow(v, vt);
				graph.addFlow(vt, vp);
			}
	}

	public Unfolding extractSubnetFromAbstracted(Set<PNPair> edges2,
			Set<Vertex> vertices2, Vertex entry2, Vertex exit2) {
		Unfolding unf = new Unfolding(this.unf.brproc);
		
		DNodeSys dnsys = this.unf.dnodesys;
		Map<Vertex, DNode> lmap = new HashMap<Vertex, DNode>();

		for (Vertex vertex: vertices2) {
			DNode node = rmap.get(vertex);
			if (node == null) {
				short id = (short)dnsys.nameToID.size();
				dnsys.nameToID.put(vertex.getName(), id);
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

		for (PNPair edge: edges2) {
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
		DNode event = lmap.get(entry2);
		cond.addPostNode(event);
		event.addPreNode(cond);
		unf.allConditions.add(cond);
		unf.initialConditions.add(cond);

		DNode dnexit = lmap.get(exit2);
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
}
