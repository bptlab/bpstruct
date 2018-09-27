/* 
 * Copyright (C) 2011 - Luciano Garcia Banuelos, Artem Polyvyanyy
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
package ee.ut.bpstruct.eventstruct;

import java.util.Map;

import ee.ut.graph.moddec.ColoredGraph;

public class RestrictedFlowEventStructure {
  
  public static final String ARTIFICIAL_START_EVENT = "_I_";
  public static final String ARTIFICIAL_END_EVENT = "_O_";
  
	ColoredGraph orgraph;
	public RestrictedFlowEventStructure(ColoredGraph orgraph) {
		this.orgraph = (ColoredGraph) orgraph.clone();
//		try {
//			String filename = String.format("bpstruct2/primitive_0.dot");
//			PrintStream out = new PrintStream(filename);
//			out.print(this.orgraph.toDot());
//			out.close();
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}				

		Integer _i_ = this.orgraph.addVertex(ARTIFICIAL_START_EVENT);
		Integer _o_ = this.orgraph.addVertex(ARTIFICIAL_END_EVENT);

//		try {
//			String filename = String.format("bpstruct2/primitive_1.dot");
//			PrintStream out = new PrintStream(filename);
//			out.print(this.orgraph.toDot());
//			out.close();
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}				

		for (Integer v: this.orgraph.vertices) {
			if (!_o_.equals(v))
				this.orgraph.addEdge(this.orgraph.getLabel(v), ARTIFICIAL_END_EVENT);
			if (!_i_.equals(v))
				this.orgraph.addEdge("_I_", this.orgraph.getLabel(v));			
		}
		
//		try {
//			String filename = String.format("bpstruct2/primitive_2.dot");
//			PrintStream out = new PrintStream(filename);
//			out.print(this.orgraph.toDot());
//			out.close();
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}				

	}
	
	public ColoredGraph computePrimeEventStructure(Map<String, String> labelMap, String modelName) {
		TupleDAG lattice = new ConfGenerator(orgraph, //immediateCausality(orgraph),
				modelName).getLattice();
//		try {
//			String filename = String.format("bpstruct2/lattice.dot");
//			PrintStream out = new PrintStream(filename);
//			out.print(this.orgraph.toDot());
//			out.close();
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}		
		return lattice.computeORG(orgraph, labelMap);
	}
	
//	private Map<Integer, Set<Integer>> immediateCausality(ColoredGraph orgraph) {
//		Map<Integer, Set<Integer>> reducedFlow = new HashMap<Integer, Set<Integer>>();
//		Set<Integer> vertices = orgraph.getVertices();
//
//		BasicDigraph dgraph = new BasicDigraph();
//		List<Node> dgnodes = dgraph.genNodes(vertices.size());
//
//		Map<Integer, Integer> indexes = new HashMap<Integer, Integer>();
//		Map<Integer, Integer> rindexes = new HashMap<Integer, Integer>();
//		int index = 0;
//		for (Integer v: orgraph.vertices) {
//			indexes.put(index, v);
//			rindexes.put(v, index);
//			index++;
//		}
//		
//		for (Node node: dgnodes) {
//			int src = indexes.get(node.nodeId());
//			for (int tgt: orgraph.postSet(src))
//				if (!orgraph.hasEdge(tgt, src)) {
//					int _tgt = rindexes.get(tgt);
//					dgraph.genEdge(node, dgnodes.get(_tgt));
//				}
//		}
//
//		Transitivity.acyclicReduce(dgraph);
//		for (Node n: dgnodes)
//			reducedFlow.put(n.nodeId(), new HashSet<Integer>());
//		for (net.stixar.graph.Edge edge: dgraph.edges())
//			reducedFlow.get(edge.source().nodeId()).add(edge.target().nodeId());
//		
//		return reducedFlow;
//	}

}
