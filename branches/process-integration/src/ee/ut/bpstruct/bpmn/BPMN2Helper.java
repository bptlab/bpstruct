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
package ee.ut.bpstruct.bpmn;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

import de.bpt.hpi.graph.Edge;
import de.bpt.hpi.graph.Graph;

/**
 * This helper class parses BPMN 2.0 files and creates jdbt graph representation of the process graph.
 * Note that the implementation uses JDOM, for simplicity and generality.
 * 
 * @author Luciano Garcia Banuelos
 */
public class BPMN2Helper extends BPMNHelper {
	// log4j ---
	static Logger logger = Logger.getLogger(BPMN2Helper.class);
	
	// OMG BPMN 2.0 URI --- JDOM Namespace
    Namespace BPMN2NS = Namespace.getNamespace("http://schema.omg.org/spec/BPMN/2.0");
	
	private Element process;
	
	private String modelName;

	Map<String, Object> edgemap;
	Map<Edge, String> edgelabelmap;
	Map<Edge, String> edgelabelmapp;
	
	public BPMN2Helper(String model_path_tpl, String model_name) throws Exception {
		super();
		this.modelName = model_name;
		String fileName = String.format(model_path_tpl, model_name);
		if (logger.isInfoEnabled()) logger.info("Reading BPMN 2.0 file: " + fileName);
		Document doc = new SAXBuilder().build(fileName);
		
		process = doc.getRootElement().getChild("process", BPMN2NS);
		if (process == null) {
			BPMN2NS = Namespace.getNamespace("http://www.omg.org/spec/BPMN/20100524/MODEL");
			process = doc.getRootElement().getChild("process", BPMN2NS);
		}
		initGraph();
	}	
	
	protected void initGraph() {
		if (logger.isInfoEnabled()) logger.info("Creating graph");
		
		graph = new Graph();
		edgemap = new HashMap<String, Object>();
		edgelabelmap = new HashMap<Edge, String>();
		
		List<Element> edges = new LinkedList<Element>();
		for (Object obj : process.getChildren())
			if (obj instanceof Element) {
				Element elem = (Element) obj;
				String id = elem.getAttributeValue("id");
				String name = elem.getAttributeValue("name");
				if (elem.getName().equals("task") || elem.getName().equals("startEvent") || elem.getName().equals("endEvent")) {
					Integer vertex = graph.addVertex(name);
					map.put(id, vertex);
					rmap.put(vertex, id);
				} else if (elem.getName().equals("exclusiveGateway")) {
						Integer vertex = graph.addVertex(name);
						map.put(id, vertex);
						rmap.put(vertex, id);
						gwmap.put(vertex, GWType.XOR);
				} else if (elem.getName().equals("parallelGateway")) {
					Integer vertex = graph.addVertex(name);
					map.put(id, vertex);
					rmap.put(vertex, id);
					gwmap.put(vertex, GWType.AND);
				} else if (elem.getName().equals("inclusiveGateway")) {
					Integer vertex = graph.addVertex(name);
					map.put(id, vertex);
					rmap.put(vertex, id);
					gwmap.put(vertex, GWType.OR);
				} else if (elem.getName().equals("sequenceFlow"))
					edges.add(elem);
				else if (logger.isInfoEnabled()) logger.warn("Unprocessed Element: " + elem.getName() + ", id: " + elem.getAttributeValue("id"));
			}

		for (Element flow : edges) {
			Integer src = map.get(flow.getAttributeValue("sourceRef"));
			Integer tgt = map.get(flow.getAttributeValue("targetRef"));
			if (src != null && tgt != null) {
				Edge edge = graph.addEdge(src, tgt);
				String label = null;
				Element expr = flow.getChild("conditionExpression", BPMN2NS);
				if (expr != null)
					label = expr.getText();
				edgemap.put(edge.toString(), label);
			} else {
				logger.fatal("Malformed graph! Dangling edge: " + flow.getAttributeValue("id"));
				throw new RuntimeException("Malformed graph");
			}
		}
		
		if (logger.isInfoEnabled()) logger.info("Graph created");
	}

	
	protected void recordEdge(Edge edge, String label) {
		label = edgemap.get(label) != null ? edgemap.get(label).toString() : null;
		edgelabelmap.put(edge, label); // flow.getAttributeValue("name"));
	}

	public void dematerializeDecisions() {
		edgelabelmapp = new HashMap<Edge, String>();
		super.dematerializeDecisions();
		edgelabelmap = edgelabelmapp;
	}
	
	/**
	 * The following method is hook for implementing the re-insertion of metadata
	 * associated to an XOR split outgoing edge (e.g. predicate, branching probability)
	 * 
	 * @param label
	 * @param nedge
	 */
	protected void reinsertEdge(Edge edge, String label) {
		label = edgemap.get(label) != null ? edgemap.get(label).toString() : null;
		System.out.println("Reinstalling: " + label + " on " + edge);
		edgelabelmapp.put(edge, label);
	}

	
	public String getModelName() {
		return this.modelName;
	}
	
	public void serializeDot(PrintStream out, Set<Integer> vertices, Set<Edge> edges) {
		
		out.println("digraph G {");
		for (Integer v: vertices) {
			if (gwmap.get(v) == GWType.AND)
				out.printf("\tn%d [shape=diamond,label=\"+\"];\n", v);
			else if (gwmap.get(v) == GWType.XOR)
				out.printf("\tn%d [shape=diamond,label=\"X\"];\n", v);
			else
				out.printf("\tn%d [shape=box,style=rounded,label=\"%s\"];\n", v, getLabel(v));
		}
		for (Edge e: edges) {
			if (e.getSource() == null || e.getTarget() == null) continue;
			if (edgelabelmap.get(e) != null)
				out.printf("\tn%d -> n%d [label=\"%s\"];\n", e.getSource(), e.getTarget(), edgelabelmap.get(e));				
			else
				out.printf("\tn%d -> n%d;\n", e.getSource(), e.getTarget());
		}
		out.println("}");				
	}
}
