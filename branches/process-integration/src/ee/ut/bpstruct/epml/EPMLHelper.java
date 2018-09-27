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
package ee.ut.bpstruct.epml;

import hub.top.petrinet.Node;
import hub.top.petrinet.PetriNet;
import hub.top.petrinet.Place;
import hub.top.petrinet.Transition;

import java.io.ByteArrayOutputStream;
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

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import de.bpt.hpi.graph.Edge;
import de.bpt.hpi.graph.Graph;
import de.bpt.hpi.graph.Pair;

import ee.ut.bpstruct.Petrifier;
import ee.ut.bpstruct.bpmn.BPMNHelper;
import ee.ut.graph.util.CombinationGenerator;
import ee.ut.graph.util.GraphUtils;

public class EPMLHelper extends BPMNHelper {
	static Logger logger = Logger.getLogger(EPMLHelper.class);
	private Element process;
	private String modelName;
	private Set<Integer> functions = new HashSet<Integer>();

	public int numelem1, numelem2;
	
	public EPMLHelper(String model_path_tpl, String model_name) throws Exception {
		super();
		this.modelName = model_name;
		String fileName = String.format(model_path_tpl, model_name);
		if (logger.isInfoEnabled()) logger.info("Reading EPML file: " + fileName);
		Document doc = new SAXBuilder().build(fileName);

		process = doc.getRootElement().getChild("epc");
		
		initGraph();
		numelem1 = numelem2 = graph.getVertices().size();
		splitMixedGWs();
		normalizeEntry();
		normalizeExit();		
	}

	protected void initGraph() {
		if (logger.isInfoEnabled()) logger.info("Creating graph");

		graph = new Graph();

		List<Element> edges = new LinkedList<Element>();
		for (Object obj : process.getChildren())
			if (obj instanceof Element) {
				Element elem = (Element) obj;
				String ename = elem.getName();
				if (ename.equals("arc")) {
					edges.add(elem);
					continue;
				}
				String id = elem.getAttributeValue("id");
				if (ename.equals("function")) {
					String label = elem.getChild("name").getTextNormalize();
					Integer vertex = graph.addVertex(label);
					map.put(id, vertex);
					rmap.put(vertex, id);
					functions.add(vertex);
				} else if (ename.equals("event")) {
					String label = elem.getChild("name").getTextNormalize();
					Integer vertex = graph.addVertex(label);
					map.put(id, vertex);
					rmap.put(vertex, id);
				} else if (ename.equals("xor")) {
					Integer vertex = graph.addVertex(id);
					map.put(id, vertex);
					rmap.put(vertex, id);
					gwmap.put(vertex, GWType.XOR);
				} else if (ename.equals("and")) {
					Integer vertex = graph.addVertex(id);
					map.put(id, vertex);
					rmap.put(vertex, id);
					gwmap.put(vertex, GWType.AND);
				} else if (ename.equals("or")) {
					Integer vertex = graph.addVertex(id);
					map.put(id, vertex);
					rmap.put(vertex, id);
					gwmap.put(vertex, GWType.OR);					
				}
			}
		
		for (Element arc : edges) {
			Element flow = arc.getChild("flow");
			Integer source = map.get(flow.getAttributeValue("source"));
			Integer target = map.get(flow.getAttributeValue("target"));
			Edge edge = new Edge(source, target);
			if (source != null && target != null && !graph.getEdges().contains(edge)) {
				graph.addEdge(edge);
			}
		}

		if (logger.isInfoEnabled()) logger.info("Graph created");
	}

	public String getModelName() {
		return modelName;
	}
	
	private boolean normalizeExit() {
		// multiple-exit models
		Set<Integer> sinks = graph.getSinkNodes();
		if (sinks.size() > 1) {
			Integer exit = graph.addVertex("_exit_");
			Integer gw = graph.addVertex("_exit_gw");
			gwmap.put(gw, GWType.OR);
			graph.addEdge(new Edge(gw, exit));
			for (Integer v: sinks)
				graph.addEdge(new Edge(v, gw));
			return true;
		}
		return false;
	}

	private boolean normalizeEntry() {
		// multiple-entry models
		Set<Integer> sources = graph.getSourceNodes();
		if (sources.size() > 1) {
			Integer entry = graph.addVertex("_entry_");
			Integer gw = graph.addVertex("_entry_gw");
			gwmap.put(gw, GWType.OR);
			graph.addEdge(new Edge(entry, gw));
			for (Integer v: sources)
				graph.addEdge(new Edge(gw, v));
			return true;
		}
		return false;
	}

	private void splitMixedGWs() {
		Map<Integer, Collection<Edge>> mixedgws = new HashMap<Integer, Collection<Edge>>();
		for (Integer gw: gwmap.keySet())
			if (graph.getIncomingEdges(gw).size() > 1 && graph.getOutgoingEdges(gw).size() > 1)
				mixedgws.put(gw, graph.getOutgoingEdges(gw));
		for (Integer gw: mixedgws.keySet()) {
			Integer gwp = graph.addVertex(graph.getLabel(gw)+ "_p");
			for (Edge e: mixedgws.get(gw))
				e.setSource(gwp);
			graph.addEdge(new Edge(gw, gwp));
			gwmap.put(gwp, gwmap.get(gw));
		}
	}
	
	protected String getLabel(Integer id) {
		if (graph.getLabel(id) != null)
			return graph.getLabel(id).replaceAll("\n", " ");
		else
			return null;
	}
	
	public Petrifier getPetrifier(final Set<Integer> vertices, final Set<Edge> edges, final Integer _entry, final Integer _exit) {
		return new Petrifier() {
			private boolean meme = false;
			private Map<String, Integer> prmap = new HashMap<String, Integer>();

			private Node getNode(Integer node, PetriNet net, Map<Integer, Node> map) {
				Node res = map.get(node);
				if (res==null) {
					if (gwmap.get(node) == GWType.XOR)
						res = net.addPlace(graph.getLabel(node));
					else 
						res = net.addTransition(graph.getLabel(node));	
					
					map.put(node, res);
					prmap.put(graph.getLabel(node), node);
				}
				return res;
			}

			public PetriNet petrify() {
				Map<Integer, Node> map = new HashMap<Integer, Node>();
				Set<Edge> entryEdges = new HashSet<Edge>();
				Set<Edge> exitEdges = new HashSet<Edge>();
				List<Place> places = new LinkedList<Place>();
				PetriNet net = new PetriNet();
				Place pentry = null;
				
				for (Edge edge : edges) {
					Integer src = edge.getSource();
					Integer tgt = edge.getTarget();
					if (src.equals(_entry))
						entryEdges.add(edge);
					if (tgt.equals(_exit))
						exitEdges.add(edge);
					if (gwmap.get(src) == null || gwmap.get(src) == GWType.AND) {
						if (gwmap.get(tgt) == null || gwmap.get(tgt) == GWType.AND) {
							Transition psrc = (Transition)getNode(src, net, map);
							Transition ptgt = (Transition)getNode(tgt, net, map);
							Place p = net.addPlace(psrc.getName() + "_" + ptgt.getName());
							net.addArc(psrc, p);
							net.addArc(p, ptgt);
						} else if (gwmap.get(tgt) == GWType.XOR) {
							Transition psrc = (Transition)getNode(src, net, map);					
							Place ptgt = (Place)getNode(tgt, net, map);
							net.addArc(psrc, ptgt);
						}
					} else if (gwmap.get(src) == GWType.XOR) {
						if (gwmap.get(tgt) == null || gwmap.get(tgt) == GWType.AND) {
							Place psrc = (Place)getNode(src, net, map);
							Transition ptgt = (Transition)getNode(tgt, net, map);

							Place pintp = net.addPlace(psrc.getName() + "_p_" + ptgt.getName());
							Transition pintt = net.addTransition(psrc.getName() + "_t_" + ptgt.getName());
							net.addArc(psrc, pintt);
							net.addArc(pintt, pintp);
							net.addArc(pintp, ptgt);
							
						} else if (gwmap.get(tgt) == GWType.XOR) {
							Place psrc = (Place)getNode(src, net, map);
							Place ptgt = (Place)getNode(tgt, net, map);
							Transition inter = net.addTransition(psrc.getName() + "_" + ptgt.getName());
							net.addArc(psrc, inter);
							net.addArc(inter, ptgt);							
						}
					}
				}

				if (!map.containsKey(_entry)) {
					// Multiple-entries
					meme = true;
					pentry = net.addPlace("_entry_");
					net.setTokens(pentry, 1);
					for (Edge edge: entryEdges) {
						Place place = net.addPlace("_" + graph.getLabel(edge.getTarget()));
						Transition trans = (Transition) getNode(edge.getTarget(), net, map);
						net.addArc(place, trans);
						places.add(place);
					}
					int counter = 0;
					for (int i = 0; i < places.size(); i++) {
						CombinationGenerator generator = new CombinationGenerator(places.size(), i+1);
						while (generator.hasMore()) {
							Transition trans = net.addTransition("trans" + counter++);
							net.addArc(pentry, trans);
							for (int index: generator.getNext()) {
								Place place = places.get(index);
								net.addArc(trans, place);
							}
						}
					}
				} else {
					Node entry = map.get(_entry);
					if (entry instanceof Transition) {
						Place p = net.addPlace("_entry_");
						net.addArc(p, (Transition)entry);
						net.setTokens(p, 1);
					}
					else if (graph.isJoin(_entry)) {
						Place p = net.addPlace("_entry_");
						Transition t = net.addTransition("_from_entry_");
						
						net.addArc(p, t);
						net.addArc(t, (Place)entry);
						net.setTokens(p, 1);
					} else
						net.setTokens((Place)entry, 1);
				}
				if (!map.containsKey(_exit)) {
					// Multiple-exits
					meme = true;
					for (Edge edge: exitEdges) {
						Place place = net.addPlace("_exit_" + graph.getLabel(edge.getSource()));
						Transition trans = (Transition) getNode(edge.getSource(), net, map);
						net.addArc(trans, place);
					}
				} else {
					Node exit = map.get(_exit);
					if (exit instanceof Transition) {
						Place p = net.addPlace("_exit_");
						net.addArc((Transition)exit, p);
					}
					
					if (exit instanceof Place && graph.isSplit(_exit) && gwmap.get(_exit) == GWType.XOR) {
						Transition t = net.addTransition("_to_exit_");
						Place p = net.addPlace("_exit_");
						net.addArc((Place)exit, t);
						net.addArc(t, p);
					}
				}
				
					if (logger.isTraceEnabled()) {
					try {
						String filename = String.format(getDebugDir().getName() + "/pnet_%s.dot", getModelName());
						PrintStream out = new PrintStream(filename);
						out.print(net.toDot());
						out.close();
						logger.trace("Petri net serialized into: " + filename);
					} catch (FileNotFoundException e) {
						logger.error(e);
					}
					}
				
				return net;
			}

			public boolean isMEME() {
				return meme;
			}
		};
	}

	
	private Set<Integer> strFunctions = new HashSet<Integer>();

	public void installStructured() {
		installStructured(new Edge(null, null));
		numelem2 = graph.getVertices().size();
	}
	
	private boolean simplify = true;
	public void disableSimplication() { simplify = false; }
	protected void installStructured(Edge pair) {
		if (rootcomponent == null) return;
		
		if (!simplify) {
			super.installStructured(pair);
			return;
		}
		
		Graph structured = new Graph();
		
		Map<Integer, Object> strgwmap = new HashMap<Integer, Object>();

		traverseBlocks(structured, rootcomponent, pair, strgwmap);

		Map<Integer, List<Integer>> outgoing = GraphUtils.edgelist2adjlist(new HashSet<Edge>(structured.getEdges()), pair.getSecond());
		Map<Integer, List<Integer>> incoming = new HashMap<Integer, List<Integer>>();
		incoming.put(pair.getFirst(), new LinkedList<Integer>());
		for (Integer src: outgoing.keySet())
			for (Integer tgt: outgoing.get(src)) {
				List<Integer> list = incoming.get(tgt);
				if (list == null)
					incoming.put(tgt, list = new LinkedList<Integer>());
				list.add(src);
			}
		
		Set<Integer> toremove = new HashSet<Integer>();
		for (Integer gw: strgwmap.keySet())
			if (outgoing.get(gw).size() == 1 && incoming.get(gw).size() == 1)
				toremove.add(gw);
		
		Set<Integer> visited = new HashSet<Integer>();
		simplify(outgoing, pair.getFirst(), toremove, structured, visited, pair.getFirst());
		
		structured.removeVertices(toremove);
		graph = structured;
		gwmap = strgwmap;
		
		functions = strFunctions;
		
		toremove.clear();
		Stack<Integer> stack = new Stack<Integer>();
		stack.push(pair.getFirst());
		while (!stack.isEmpty()) {
			Integer curr = stack.pop();
			for (Integer succ: outgoing.get(curr)) {
				if (gwmap.containsKey(succ)) {
					toremove.add(succ);
					stack.add(succ);
				}
			}
		}
		stack.push(pair.getSecond());
		while (!stack.isEmpty()) {
			Integer curr = stack.pop();
			for (Integer pred: incoming.get(curr)) {
				if (gwmap.containsKey(pred)) {
					toremove.add(pred);
					stack.add(pred);
				}
			}
		}
		
		graph.removeVertices(toremove);
		graph.removeVertex(pair.getFirst());
		graph.removeVertex(pair.getSecond());
	}
	
	protected Pair cloneVertex(Graph structured, Map<Integer, Pair> localpairs,
			Integer vertex, Map<Integer, Object> strgwmap) {
		Pair pair = super.cloneVertex(structured, localpairs, vertex, strgwmap);
		if (functions.contains(vertex))
			strFunctions.add(strMap.get(vertex));
		return pair;
	}
	
	public void serializeDot(PrintStream out) {
		out.println("digraph G" + modelName + " {");
		toDot(out, graph.getVertices());		
		for (Edge edge: new HashSet<Edge>(graph.getEdges()))
			out.println("\tnode" + edge.getSource() + " -> node" + edge.getTarget());
		out.println("}");
	}

	public String toDot() {
		return toDot(graph.getVertices(), new HashSet<Edge>(graph.getEdges()));
	}
	
	public String toDot(Set<Integer> vertices, Set<Edge> edges) {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(buffer);
		out.println("digraph G" + modelName + " {");

		toDot(out, vertices);
		
		for (Edge edge: edges)
			out.println("\tnode" + edge.getSource() + " -> node" + edge.getTarget());
		
		out.println("}");
		
		return buffer.toString();
	}

	private void toDot(PrintStream out, Set<Integer> vertices) {
		for (Integer id: vertices) {
			String label = getLabel(id);
			
			if (functions.contains(id)) {
				out.println("\tnode" + id + 
						" [shape=box, fillcolor=palegreen2, style=\"rounded,filled\", label=\"" +
						label + "\"]");
			} else if (!gwmap.containsKey(id)) {
		    	out.print("\tnode" + id + 
		    			" [shape=hexagon, fillcolor=lightpink1, style=\"filled\", label=\""+
						label + "\"]");
			} else {
				if (gwmap.get(id).equals(GWType.AND))
					out.print("\tnode" + id + " [shape=circle, label=\"^\"]");
				else if (gwmap.get(id).equals(GWType.XOR))
					out.print("\tnode" + id + " [shape=circle, label=\"x\"]");
				else
					out.print("\tnode" + id + " [shape=circle, label=\"v\"]");

			}
		}
	}

//	@SuppressWarnings("unchecked")
//	public String toEPML(String modelname, Set<Integer> vertices, Set<Edge> edges) {
//		Document doc = new Document();
//		
//		// Root element
//		Element root = new Element("epml", EPMLNS);
//		doc.setRootElement(root);
//
//		Element coordinates = new Element("coordinates", EPMLNS);
//		coordinates.getAttributes().add(new Attribute("xOrigin", "leftToRight"));
//		coordinates.getAttributes().add(new Attribute("yOrigin", "topToBottom"));
//		root.addContent(coordinates);
//		
//		Element directory = new Element("directory", EPMLNS);
//		directory.getAttributes().add(new Attribute("name", "Root"));
//		root.addContent(directory);
//		
//		Element proc = new Element("epc", EPMLNS);
//		proc.getAttributes().add(new Attribute("epcId", "1"));
//		proc.getAttributes().add(new Attribute("name", modelname));
//		directory.addContent(proc);
//		
//		Integer edgeid = 0;
//
//		for (Integer id: vertices) {
//			Element node;
//			if (gwmap.get(id) == GWType.XOR) {
//				node = new Element("xor", EPMLNS);
//			} else if (gwmap.get(id) == GWType.AND) {
//				node = new Element("and", EPMLNS);
//			} else if (gwmap.get(id) == GWType.OR)
//				node = new Element("or", EPMLNS);
//			else {
//				if (!functions.contains(id))
//					node = new Element("event", EPMLNS);					
//				else
//					node = new Element("function", EPMLNS);
//				Element name = new Element("name", EPMLNS);
//				name.addContent(graph.getLabel(id));
//				node.addContent(name);
//			}
//			node.getAttributes().add(new Attribute("id", id.toString()));
//			proc.addContent(node);
//			
//			if (id > edgeid) edgeid = id;
//		}
//		
//		for (Edge e: edges) {
//			Element arc = new Element("arc", EPMLNS);
//			arc.getAttributes().add(new Attribute("id", (++edgeid).toString()));
//			Element flow = new Element("flow", EPMLNS);
//			flow.getAttributes().add(new Attribute("source", e.getSource().toString()));
//			flow.getAttributes().add(new Attribute("target", e.getTarget().toString()));
//			arc.addContent(flow);
//			proc.addContent(arc);
//		}
//		
//		ByteArrayOutputStream baout = new ByteArrayOutputStream();
//		XMLOutputter out = new XMLOutputter();
//		out.setFormat(Format.getPrettyFormat());
//		try {
//			out.output(doc, baout);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		return baout.toString();
//	}
}
