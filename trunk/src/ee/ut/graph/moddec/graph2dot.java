/* 
 * Copyright (C) 2010 - Luciano Garcia Banuelos
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
package ee.ut.graph.moddec;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ee.ut.bpstruct.CannotStructureException;
import ee.ut.graph.moddec.MDTNode.NodeType;

public class graph2dot {
	public static void generate(final PrintStream out, final ModularDecompositionTree mdt) throws CannotStructureException {
		final ColoredGraph graph = mdt.getGraph();
	    out.println("digraph G {");
	    out.println("   compound=true;");
	    mdt.traversePostOrder(new MDTVisitor() {
			
	    	Map<MDTNode, String> map = new HashMap<MDTNode, String>();
	    	int counter = 0;
	    	String prefix = "   ";
			
	    	public void visitLeaf(MDTNode node, String alabel) {
				String label = "node" + counter++;
				map.put(node, label);
				String taskName = graph.getLabel(node.getProxy());
				out.println(prefix + label + String.format("[label=\"%s\"];", taskName));
			}
		
			public void visitPrimitive(MDTNode node, Set<MDTNode> children) {
				Map<Integer, MDTNode> proxies = new HashMap<Integer, MDTNode>();
				
				for (MDTNode child: children)
					proxies.put(child.getProxy(), child);
				
				ColoredGraph subgraph = graph.subgraph(proxies.keySet());

				for (Integer _s: proxies.keySet()) {
					for (Integer _t: subgraph.postSet(_s)) {
						String source = map.get(firstLeaf(proxies.get(_s)));
						String target = map.get(firstLeaf(proxies.get(_t)));
						String str = srctgtString(proxies.get(_s), proxies.get(_t));

						out.println(String.format("%s%s -> %s%s;", prefix, source, target, str));						
					}
				}
			}

			private String srctgtString(MDTNode _s,
					MDTNode _t) {
				String tail = _s.getType() != NodeType.LEAF ? "ltail=" + map.get(_s) : "";
				String head = _t.getType() != NodeType.LEAF ? "lhead=" + map.get(_t) : "";
				String str = "";
				if (head.length() > 0 && tail.length() > 0)
					str = "["+head+ "," +tail+"]";
				else if (head.length() > 0)
					str = "["+head+"]";
				else if (tail.length() > 0)
					str = "["+tail+"]";
				return str;
			}
			
			private MDTNode firstLeaf(MDTNode node) {
				MDTNode result = node;
				while (result.getType() != NodeType.LEAF)
					result = result.getChildren().iterator().next();
				return result;
			}
			
			public void visitLinear(MDTNode node, List<MDTNode> children) {
				for (int i = 1; i < children.size(); i++) {
					MDTNode _s = firstLeaf(children.get(i-1));
					MDTNode _t = firstLeaf(children.get(i));

					String source = map.get(_s);
					String target = map.get(_t);

					String str = srctgtString(children.get(i-1), children.get(i));

					out.println(String.format("%s%s -> %s%s;", prefix, source, target, str));						
				}
			}
			
			public void visitComplete(MDTNode node, Set<MDTNode> children, int color) {
				if (color == 1) {
					for (MDTNode _s: children) {
						MDTNode s = firstLeaf(_s);
						for (MDTNode _t: children) {
							if (_s.equals(_t)) continue;
							MDTNode t = firstLeaf(_t);
							String source = map.get(s);
							String target = map.get(t);

							String str = srctgtString(_s, _t);
							out.println(String.format("%s%s -> %s%s;", prefix, source, target, str));
						}
					}
				}
			}

			public void closeContext(MDTNode node) {
				prefix = prefix.substring(3);
				out.println(prefix + "}");
			}

			public void openContext(MDTNode node) {
				String label = "cluster" + counter++; 
				map.put(node, label);
				
				out.println(prefix + String.format("subgraph %s {", label));
				if (node.getType() == NodeType.PRIMITIVE)
					out.println("color=red;");
				prefix += "   ";
			}
		});
	    out.println("}");
	}
}
