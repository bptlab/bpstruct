/* 
 * Copyright (C) 2011 - Luciano Garcia Banuelos, Artem Polyvyanyy, Dirk Fahland
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

import hub.top.petrinet.PetriNet;
import hub.top.petrinet.Place;
import hub.top.petrinet.Transition;
import hub.top.petrinet.unfold.DNodeSys_OccurrenceNet;
import hub.top.uma.DNode;
import hub.top.uma.DNodeRefold;
import hub.top.uma.InvalidModelException;
import hub.top.uma.Uma;
import hub.top.uma.DNodeSet.DNodeSetElement;
import hub.top.uma.synthesis.NetSynthesis;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.stixar.graph.BasicDigraph;
import net.stixar.graph.Edge;
import net.stixar.graph.MutableDigraph;
import net.stixar.graph.Node;
import net.stixar.graph.attr.ByteNodeMatrix;
import net.stixar.graph.conn.Transitivity;
import net.stixar.graph.order.NodeOrder;
import net.stixar.graph.order.TopSorter;
import net.stixar.util.CList;
import de.hpi.bpt.process.Gateway;
import de.hpi.bpt.process.GatewayType;
import de.hpi.bpt.process.Process;
import de.hpi.bpt.process.Task;
import ee.ut.bpstruct.eventstruct.RestrictedFlowEventStructure;
import ee.ut.graph.moddec.ColoredGraph;

public class MaxStr {

	class Pair {
		Integer first;
		BitSet second;

		public Pair(Integer f, BitSet s) {
			first = f;
			second = s;
		}

		public Integer getFirst() {
			return first;
		}

		public BitSet getSecond() {
			return second;
		}

		public void setFirst(Integer val) {
			first = val;
		}

		public void setSecond(BitSet val) {
			second = val;
		}

		public String toString() {
			return String.format("(%s,%s)", first, second);
		}

		public boolean equals(Object obj) {
			if (obj == null)
				return false;
			if (!(obj instanceof Pair))
				return false;
			Pair that = (Pair) obj;

			return this.first.equals(that.first)
					&& this.second.equals(that.second);
		}

		public int hashCode() {
			return (first == null ? 0 : first.hashCode())
					+ (second == null ? 0 : second.hashCode() * 37);
		}

		public Object clone() {
			return new Pair(first, (BitSet) second.clone());
		}
	}

	public MaxStr() {
	}

	public String getModelName() {
		return "model";
	}
	
	/**
	 * MAIN METHOD ---
	 * 
	 * @param graph
	 * @param edges
	 * @param vertices
	 * @param entry
	 * @param exit
	 */
	public void perform(ColoredGraph orgraph,
			Map<String, de.hpi.bpt.process.Node> tasks,
			Map<String, de.hpi.bpt.process.Node> clones, Process proc, ee.ut.bpstruct.jbpt.Pair pair) {

		boolean hasConflict = false;
		for (Integer v1: orgraph.getVertices())
			for (Integer v2: orgraph.getVertices())
				if (!v1.equals(v2) && orgraph.hasEdge(v1, v2) && orgraph.hasEdge(v2, v1)) {
					hasConflict = true;
					break;
				}
		
		if (!hasConflict) {
			Set<Integer> sources = new HashSet<Integer>();
			Set<Integer> sinks = new HashSet<Integer>();
			
//			try {
//				PrintStream out = new PrintStream("bpstruct2/orgraph.dot");
//				out.println(orgraph.toDot());
//				out.close();
//			} catch (FileNotFoundException e) {
//				e.printStackTrace();
//			}
			
			BasicDigraph dgraph = transitiveReduction(orgraph, sources, sinks);

			Map<Node, de.hpi.bpt.process.Node> nodes = new HashMap<Node, de.hpi.bpt.process.Node>();
			Map<Node, de.hpi.bpt.process.Node> outgoing = new HashMap<Node, de.hpi.bpt.process.Node>();
			Map<Node, de.hpi.bpt.process.Node> incoming = new HashMap<Node, de.hpi.bpt.process.Node>();
			Map<Node, Integer> outgoingCount = new HashMap<Node, Integer>();
			Map<Node, Integer> incomingCount = new HashMap<Node, Integer>();

			for (Edge edge: dgraph.edges()) {
				Node src = edge.source();
				Node tgt = edge.target();
				if (!outgoingCount.containsKey(src))
					outgoingCount.put(src, 1);
				else
					outgoingCount.put(src, 2);
				if (!incomingCount.containsKey(tgt))
					incomingCount.put(tgt, 1);
				else
					incomingCount.put(tgt, 2);
			}
			

			Gateway entryGw = new Gateway(GatewayType.AND);
			Gateway exitGw = new Gateway(GatewayType.AND);

			pair.setFirst(entryGw);
			pair.setSecond(exitGw);

			for (Node node: dgraph.nodes()) {
				String label = orgraph.getLabel(node.nodeId());
				de.hpi.bpt.process.Node vertex = tasks.get(label);
				nodes.put(node, vertex);
				if (outgoingCount.containsKey(node) && outgoingCount.get(node) > 1) {
					Gateway gw = new Gateway(GatewayType.AND);
					outgoing.put(node, gw);
					proc.addControlFlow(vertex, gw);
				} else
					outgoing.put(node, vertex);
				if (incomingCount.containsKey(node) && incomingCount.get(node) > 1) {
					Gateway gw = new Gateway(GatewayType.AND);
					incoming.put(node, gw);
					proc.addControlFlow(gw, vertex);
				} else
					incoming.put(node, vertex);
				
				if (sources.contains(node.nodeId()))
					proc.addControlFlow(entryGw, incoming.get(node));
				if (sinks.contains(node.nodeId()))
					proc.addControlFlow(outgoing.get(node), exitGw);
			}

			for (Edge edge: dgraph.edges()) {
				de.hpi.bpt.process.Node src, tgt;
				src = outgoing.get(edge.source());
				tgt = incoming.get(edge.target());
				proc.addControlFlow(src, tgt);
			}
			
		} else {
			Map<String, String> labelMap = new HashMap<String, String>();
			RestrictedFlowEventStructure fes = new RestrictedFlowEventStructure(
					orgraph);
			ColoredGraph primeEventStructure = fes.computePrimeEventStructure(
					labelMap, getModelName());

			PetriNet folded = synthesize(primeEventStructure, labelMap, tasks,
					clones);
	
//			try {	
//				try {
//					PetriNetIO.writeToFile(folded, "bpstruct2/folded_"
//							+ getModelName() + ".lola", PetriNetIO.FORMAT_LOLA, 0);
//				} catch (IOException e) {
//	
//				}
//				PrintStream out = new PrintStream(String.format(
//						"bpstruct2/folded_%s.dot", getModelName()));
//				out.println(folded.toDot());
//				out.close();
//			} catch (FileNotFoundException e) {
//				e.printStackTrace();
//			}
			synthesizePM(folded, tasks, proc, pair);
		}
		
//		try {
//			PrintStream out = new PrintStream(String.format(
//					"bpstruct2/synt_%s.dot", getModelName()));
//			out.println(Process2DOT.convert(proc));
//			out.close();
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}
	}
	
	private BasicDigraph transitiveReduction(ColoredGraph orgraph,
			Set<Integer> sources, Set<Integer> sinks) {

//			Map<String, String> labelMap,
//			Map<String, de.hpi.bpt.process.Node> tasks,
//			Map<String, de.hpi.bpt.process.Node> clones) {
		Set<Integer> vertices = orgraph.getVertices();
		sources.addAll(vertices);
		sinks.addAll(vertices);

		BasicDigraph dgraph = new BasicDigraph();
		int max = -1;
		for (Integer v: vertices)
			if (v > max)
				max = v;
		List<Node> dgnodes = dgraph.genNodes(max+1);

		for (Node node: dgnodes) {
			int src = node.nodeId();
			if (orgraph.vertices.contains(src))
				for (int tgt: orgraph.postSet(src)) {
					if (!orgraph.hasEdge(tgt, src)) {
						dgraph.genEdge(node, dgnodes.get(tgt));
						sources.remove(tgt);
						sinks.remove(src);
					}
				}
		}
		Transitivity.acyclicReduce(dgraph);
		return dgraph;
	}


	private void synthesizePM(PetriNet folded,
			Map<String, de.hpi.bpt.process.Node> tasks, Process proc, ee.ut.bpstruct.jbpt.Pair pair) {
		Map<Place, Gateway> places = new HashMap<Place, Gateway>();
		for (Transition trans : folded.getTransitions()) {
			de.hpi.bpt.process.Node vertexInFlow, vertexOutFlow;
			if (tasks.containsKey(trans.getName())) {
				de.hpi.bpt.process.Node vertex = vertexInFlow = vertexOutFlow = new Task(trans.getName());
				if (trans.getPreSet().size() > 1) {
					Gateway gw = new Gateway(GatewayType.AND);
					proc.addControlFlow(gw, vertex);
					vertexInFlow = gw;
				}
				if (trans.getPostSet().size() > 1) {
					Gateway gw = new Gateway(GatewayType.AND);
					proc.addControlFlow(vertex, gw);
					vertexOutFlow = gw;
				}
			} else {
				Gateway gw0 = new Gateway(GatewayType.AND);
				vertexInFlow = vertexOutFlow = gw0;
				if (trans.getPreSet().size() > 1
						&& trans.getPostSet().size() > 1) {
					Gateway gw = new Gateway(GatewayType.AND);
					proc.addControlFlow(vertexInFlow, gw);
					vertexOutFlow = gw;
				}
			}
			for (Place place : trans.getPreSet()) {
				Gateway gw = places.get(place);
				if (gw == null) {
					gw = new Gateway(GatewayType.XOR);
					places.put(place, gw);
				}
				proc.addControlFlow(gw, vertexInFlow);
				
				if (place.getPreSet().size() == 0)
					pair.setFirst(gw);
			}
			for (Place place: trans.getPostSet()) {
				Gateway gw = places.get(place);
				if (gw == null) {
					gw = new Gateway(GatewayType.XOR);
					places.put(place, gw);
				}
				proc.addControlFlow(vertexOutFlow, gw);
				if (place.getPostSet().size() == 0)
					pair.setSecond(gw);
			}

		}
	}

	private PetriNet synthesize(ColoredGraph primeEventStructure,
			Map<String, String> labelMap,
			Map<String, de.hpi.bpt.process.Node> tasks,
			Map<String, de.hpi.bpt.process.Node> clones) {
		PetriNet pnet = new PetriNet();

		Map<Integer, Set<Integer>> reducedFlow = new HashMap<Integer, Set<Integer>>();
	    Map<Integer, Set<Integer>> implicitFlow = new HashMap<Integer, Set<Integer>>();
		Set<Integer> sources = new HashSet<Integer>();
		Set<Integer> sinks = new HashSet<Integer>();

	    separateColors(primeEventStructure, reducedFlow, implicitFlow, sources, sinks, labelMap, tasks, clones);

	    // ---------------------------------------------------------
	    // 1. Initialize E
	    // ---------------------------------------------------------
	    Map<Integer, Transition> E = new LinkedHashMap<Integer, Transition>();
	    for (Integer n: primeEventStructure.vertices) {
	      String transitionLabel = getOriginalLabel(labelMap.get(primeEventStructure.getLabel(n)), tasks, clones);
	      E.put(n, pnet.addTransition(transitionLabel));
	      
//	      System.out.println(n+" ---> "+transitionLabel);
	    }

	    // ---------------------------------------------------------
	    // 2. COMPUTE CE
	    // ---------------------------------------------------------
	    
	    
	    Set<BitSet> CE = new HashSet<BitSet>();
	    LinkedList<BitSet> queue = new LinkedList<BitSet>();
	    for (Integer e : E.keySet()) { // add singletons
	      BitSet singleton = new BitSet();
	      singleton.set(e);
	      CE.add(singleton);
	      queue.add(singleton);
	    }
	    while (!queue.isEmpty()) {
	      BitSet conflictSet = queue.removeFirst();
	      for (Integer e : E.keySet()) {
	        if (!conflictSet.get(e)) {
	          boolean e_in_conflict_with_all = true;
	          for (int e2 = conflictSet.nextSetBit(0); e2 >= 0; e2 = conflictSet.nextSetBit(e2 + 1)) {
	            if (!(primeEventStructure.hasEdge(e, e2) && primeEventStructure.hasEdge(e2, e))) {
	              // not in conflict ?
	              e_in_conflict_with_all = false;
	              break;
	            }
	          }
	          if (e_in_conflict_with_all) {
	            BitSet x = (BitSet)conflictSet.clone();
	            x.set(e);
	            CE.add(x);
	            queue.add(x);
	          }
	        }
	      }
	    }
	    
	    //System.out.println(reducedFlow);
	    //System.out.println(implicitFlow);
	    
	    // implicit places with one successor
	    Set<Place> implicitPlaces = new HashSet<Place>();
	    // implicit places with 2 or more successors
	    Set<Place> implicitPlaces_2ormore = new HashSet<Place>();
	    
	    // ---------------------------------------------------------
	    // 3. COMPUTE B
	    // ---------------------------------------------------------
	    Map<Pair, Place> B = new LinkedHashMap<Pair, Place>();
	    Map<Place, Pair> B2 = new LinkedHashMap<Place,Pair>();
	    for (Integer e : E.keySet()){
	      for (BitSet ce : CE) {
	        
	        boolean isCondition = true;
	        boolean isImplicit = false;
	        for (Integer e2 = ce.nextSetBit(0);  e2 >= 0; e2 = ce.nextSetBit(e2 + 1)) {
	          if (   !reducedFlow.get(e).contains(e2)
	              && !implicitFlow.get(e).contains(e2)) isCondition = false;
	          
	          if (implicitFlow.get(e).contains(e2)) isImplicit = true;
	          
	          if (!isCondition) break;
	        }

	        if (isCondition) {
	          Pair pair = new Pair(e, ce);

	          String srcTransitionLabel = getOriginalLabel(labelMap.get(primeEventStructure.getLabel(e)), tasks, clones);

	          // translate the IDs of the defining events to the labels
	          // so that conditions are consistently labeled throughout the occurrence net
	          // then we can identify equivalent conditions based on their labels
	          String placeLabel = "("+srcTransitionLabel;
	          
	          int successors = 0;
	          //String placeLabel = "(";
	          for (int i=0; i<ce.length(); i++) {
	            if (ce.get(i)) {
	              String tgtTransitionLabel = getOriginalLabel(labelMap.get(primeEventStructure.getLabel(i)), tasks, clones);
	              placeLabel += ","+tgtTransitionLabel;
	              successors++;
	            }
	          }
	          
	          placeLabel += ")";
	          
	          Place place = pnet.addPlace(placeLabel);
	          B.put(pair, place);
	          B2.put(place, pair);
	          //System.out.printf("<%s, %s>\n", e, ce);
	          
	          if (isImplicit) {
	            if (successors == 1) implicitPlaces.add(place);
	            else implicitPlaces_2ormore.add(place);
	            //System.out.printf("implicit "+placeLabel+"\n");
	          }
	        }
	      }
	    }

	    // ---------------------------------------------------------
	    // 4. COMPUTE F
	    // ---------------------------------------------------------
	    for (Pair pair: B.keySet()) {
	      Place place = B.get(pair);
	      Integer e = pair.getFirst();
	      BitSet x = pair.getSecond();
	      for (Integer e2 = x.nextSetBit(0);  e2 >= 0; e2 = x.nextSetBit(e2 + 1)) {
	        Transition trans = E.get(e2);
	        pnet.addArc(place, trans);
	      }
	      Transition trans = E.get(e);
	      pnet.addArc(trans, place);
	    }
	    
	    // ---------------------------------------------------------
	    // 5. CLEAN THE NET
	    // ---------------------------------------------------------
	    
	    // --- Remove subsumed conditions
	    Set<Place> subsumedConditions = new HashSet<Place>();
	    for (Transition src: pnet.getTransitions())
	      if (src.getPostSet().size() > 1)
	        for (Place p1: src.getPostSet()) {
	          
	          for (Place p2: src.getPostSet()) {
	            
	            if (!p1.equals(p2) && p1.getPostSet().containsAll(p2.getPostSet()))
	              subsumedConditions.add(p2);
	            
	          }
	        }
	    
//	    System.out.println(subsumedConditions);
	    for (Place place: subsumedConditions) {
	      //System.out.println("removing subsumed "+place);
	      pnet.removePlace(place);
	    }
	    
	    implicitPlaces_2ormore.removeAll(subsumedConditions);
	    for (Place place: implicitPlaces_2ormore) {
	      //System.out.println("removing transitive conflicts 1 "+place);
	      pnet.removePlace(place);
	    }
	    
	    // --- Remove transitive conflicts
	    // A condition b is a transitive conflict between two events e_1 and e_2, iff there exist events f_1 < e_1 and f_2 < e_2 that are in conflict.
	    // A condition b is a transitive conflict, iff it is a transitive conflict between e_1 and e_2 for any two different post-events e_1, e_2 of b.
	    // !!! And there exists a path from pre-event of b to every post event of b which does not contain b (causality is preserved) 
	    // !!! In free-choice nets can be checked directly on postsets of conflict conditions
	    
	    Set<Place> transitiveConflicts = new HashSet<Place>();
	    
//	    System.out.println("searching transitive conflicts");
	    boolean iterate = true;
	    while (iterate)
	    {
	      iterate = false;
	      for (Place b: pnet.getPlaces()) { // check if b is a transitive conflict
	        if (b.getPostSet().size()==1) continue; // b is not a conflict
	        if (transitiveConflicts.contains(b)) continue;
	        
	        //System.out.println("checking "+b);
	        
	        boolean is_tr_conflict = true;
	        for (Transition e1 : b.getPostSet()) {
	          for (Transition e2 : b.getPostSet()) {
	            // for any two different post-events e_1, e_2 of b ...
	            if (e1 == e2) continue;
	            
	            // ... b is a transitive conflict between e_1 and e_2
	            is_tr_conflict &= isTransitiveConflict(b,e1,e2,pnet);
	            //if (!flag) System.out.println("   "+e1+","+e2+" --> "+flag);
	            
	            if (!is_tr_conflict) break;
	          }
	          if (!is_tr_conflict) break;
	        }
	        
	        if (is_tr_conflict) {
	          Transition pre = b.getPreSet().get(0);
	          boolean can_be_removed = true;
	          for (Transition t : b.getPostSet()) {
	            can_be_removed &= existsPathWithoutCondition(pre, t, b, pnet, transitiveConflicts);
	            if (!can_be_removed) break;
	          }
	          
	          if (can_be_removed) {
	            //System.out.println("   remove "+b);
	            //pnet.removePlace(b);
	            transitiveConflicts.add(b);
	            iterate = true;
	            break;
	          }
	        }
	      } 
	    }
	    
	    for (Place place: transitiveConflicts) {
	      //System.out.println("removing transitive conflicts "+place);
	      pnet.removePlace(place);
	    }
	    
	    // after removing subsumed conditions and transitive conflicts,
	    // some of the original implicit conditions are maybe no longer
	    // implicit. Check here.
	    Set<Place> final_implicitPlaces = new HashSet<Place>();
	    for (Place p : implicitPlaces) {
	      if (!pnet.getPlaces().contains(p)) continue;
	      if (p.getPreSet().isEmpty()) continue;
	      if (p.getPostSet().isEmpty()) continue;
	      
	      //System.out.println("is "+p+" still implied?");
	      boolean isImplicit = true;
	      
	      Transition t = p.getPreSet().get(0);
	      for (Transition t2 : p.getPostSet()) {

	        if (!existsPathWithoutCondition(t, t2, p, pnet, new HashSet<Place>())) {
	          isImplicit = false;
	          //System.out.println("  no path "+t+" --> "+t2);
	        }
	      }
	      if (isImplicit) {
	        //System.out.println("  yes, adding");
	        final_implicitPlaces.add(p);
	      }
	    }
	  
	    // --- Add source place
	    
	    {
	      Transition src = E.get(sources.iterator().next());
	      Place place = pnet.addPlace(src.getName());
	      place.setTokens(1);
	      pnet.addArc(place, src);
	    }
	    // --- Add sink places
	    for (Integer sink: sinks) {
	      Transition trans = E.get(sink);
	      Place place = pnet.addPlace(trans.getName());
	      pnet.addArc(trans, place);
	    }

//	    try {
//	      PrintStream out = new PrintStream(String.format("bpstruct2/occnet_%s.dot", getModelName()));
//	      out.println(pnet.toDot());
//	      out.close();      
//	    } catch (FileNotFoundException e) {
//	      e.printStackTrace();
//	    }

	    PetriNet folded = fold(pnet, final_implicitPlaces);
	    
//	    System.out.println(pnet.getPlaces().size());
		
		return folded;
	}
	
	
	  private boolean existsPathWithoutCondition(Transition t1, Transition t2, Place b, PetriNet pnet, Set<Place> toRemove) {
		    if (t1.equals(t2)) return true;
		    Set<Transition> visited = new HashSet<Transition>();
		    Set<Transition> heap = getPostTransitions(t1, b, toRemove);
		    if (heap.contains(t2)) return true;
		    
		    while (heap.size()>0) {
		      Transition tt = heap.iterator().next();
		      heap.remove(tt);
		      visited.add(tt);
		      Set<Transition> post = getPostTransitions(tt, b, toRemove);
		      if (post.contains(t2)) return true;
		      post.removeAll(visited);
		      heap.addAll(post);
		    }
		  
		    return false;
		  }

	// A condition b is a transitive conflict between two events e_1 and e_2, iff there exist events f_1 < e_1 and f_2 < e_2 that are in conflict.
	private boolean isTransitiveConflict(Place b, Transition e1, Transition e2, PetriNet pnet) {
    if (e1.getName().equals(e2.getName())) return false;
    
		for (Place p : pnet.getPlaces()) {
			if (p.equals(b) || p.getPostSet().size()<=1) continue;
			
			for (Transition f1 : p.getPostSet()) {
				for (Transition f2 : p.getPostSet()) {
					if (f1 == f2) continue;
					
					if (existsPath(f1,e1,pnet) && existsPath(f2, e2, pnet)) {
						return true;
					}
				}
			} 
			
		}
		return false;
	}

	private boolean existsPath(Transition t1, Transition t2, PetriNet pnet) {
		if (t1.equals(t2)) return true;
		Set<Transition> visited = new HashSet<Transition>();
		Set<Transition> heap = getPostTransitions(t1);
		if (heap.contains(t2)) return true;
		
		while (heap.size()>0) {
			Transition tt = heap.iterator().next();
			heap.remove(tt);
			visited.add(tt);
			Set<Transition> post = getPostTransitions(tt);
			if (post.contains(t2)) return true;
			post.removeAll(visited);
			heap.addAll(post);
		}
	
		return false;
	}
	
//	private boolean existsPath(Set<Transition> pt, Set<Transition> qt, PetriNet pnet) {		
//		for (Transition t : pt) {
//			Set<Transition> heap = new HashSet<Transition>();
//			Set<Transition> post = getPostTransitions(t);
//			Set<Transition> visited = new HashSet<Transition>();
//			
//			if (setsIntersect(post,qt)) return true;
//			heap.addAll(post);
//			while (heap.size()>0) {
//				Transition tt = heap.iterator().next();
//				heap.remove(tt);
//				visited.add(tt);
//				post = getPostTransitions(tt);
//				if (setsIntersect(post,qt)) return true;
//				else { heap.addAll(post); post.removeAll(visited); }
//			}
//			
//		}
//		
//		return false;
//	}
//	
//	private boolean setsIntersect(Set<Transition> s1, Set<Transition> s2) {
//		for (Transition t : s1)
//			if (s2.contains(t)) return true;
//		
//		return false;
//	}
	
	private Set<Transition> getPostTransitions(Transition t) {
		Set<Transition> result = new HashSet<Transition>();
		
		for (Place p: t.getPostSet())
			result.addAll(p.getPostSet());
		
		return result;
	}
	
	private Set<Transition> getPostTransitions(Transition t, Place b, Set<Place> toRemove) {
	    Set<Transition> result = new HashSet<Transition>();
		    
	    for (Place p: t.getPostSet())
	      if (!p.equals(b) && !toRemove.contains(p))
	        result.addAll(p.getPostSet());
		    
	    return result;
	}
//
//	private Set<Transition> getPostTransitions(Transition t, Place b) {
//		Set<Transition> result = new HashSet<Transition>();
//		
//		for (Place p: t.getPostSet())
//			if (!p.equals(b))
//				result.addAll(p.getPostSet());
//		
//		return result;
//	}
//
//	private Set<Integer> getMarkedBits(BitSet set) {
//		Set<Integer> result = new HashSet<Integer>();
//		
//		for (int i=0; i<set.length(); i++)
//			if (set.get(i))
//				result.add(i);
//		
//		return result;
//	}
//	
//	private Set<Transition> getTransitions(BitSet set, Map<Integer, Transition> E) {
//		Set<Transition> result = new HashSet<Transition>();
//		
//		Set<Integer> is = getMarkedBits(set);
//		for (Integer i : is) {
//			result.add(E.get(i));
//		}
//		
//		return result;
//	}

	private static String getOriginalLabel(String label,
			Map<String, de.hpi.bpt.process.Node> tasks,
			Map<String, de.hpi.bpt.process.Node> clones) {

		if (tasks.get(label) == null) {
			for (String originalLabel : tasks.keySet()) {
				if (tasks.get(originalLabel) == clones.get(label)) {
					label = originalLabel;
					break;
				}
			}
		}
		return label;
	}

	private PetriNet fold(PetriNet occnet, Set<Place> implicitPlaces) {
		try {
			  
		      DNodeSys_OccurrenceNet sys = new DNodeSys_OccurrenceNet(occnet, implicitPlaces);
		      
		      //System.out.println("implicit places: "+implicitPlaces);

//		      try {
//		        
//		       try {
//		         PetriNetIO.writeToFile(occnet, "output/dnode_"+ getModelName()+".lola", PetriNetIO.FORMAT_LOLA, 0);
//		       } catch (IOException e) {
//		         e.printStackTrace();
//		       }
//		           
//		        PrintStream out = new PrintStream(String.format("output/dnode_%s.dot", getModelName()));
//		        out.println(sys.initialRun.toDot(sys.properNames));
//		        out.close();
//		      } catch (FileNotFoundException e) {
//		        e.printStackTrace();
//		      }     

		      DNodeRefold build = Uma.initBuildPrefix_View(sys, 0);

//		      Uma.out.println("equivalence..");
		      build.futureEquivalence();

		      //build.debug_printFoldingEquivalence();

//		      Uma.out.println("join maximal..");
		      build.extendFutureEquivalence_maximal();

//		      Uma.out.println("fold backwards..");

		      while (build.extendFutureEquivalence_backwards()) {
//		        Uma.out.println("fold backwards..");
		      }
		      
		      //while (build.refineFoldingEquivalence_removeSuperfluous()) {
		      //  Uma.out.println("remove superfluous..");
		      //}

		      hub.top.uma.synthesis.EquivalenceRefineSuccessor splitter = new hub.top.uma.synthesis.EquivalenceRefineSuccessor(build); 

//		      Uma.out.println("relax..");
		      //build.relaxFoldingEquivalence(splitter);
//		      Uma.out.println("determinize..");
		      //while (build.extendFoldingEquivalence_deterministic()) {
		      //  Uma.out.println("determinize..");
		      //}
		      
		      build.futureEquivalence()._debug_printFoldingEquivalence();
		      
		      NetSynthesis synth = new NetSynthesis(build);
		      DNodeSetElement nonImplied = new DNodeSetElement();
		      for (DNode d : build.getBranchingProcess().getAllNodes()) {
		        if (nonImplied.contains(d)) {
//		          System.out.println("duplicate node "+d);
		          continue;
		        }
		        if (!d.isImplied) {
//		          System.out.println("node "+d);
		          nonImplied.add(d);
		        } else {
//		          System.out.println("implicit "+d);
		        }
		      }
		      PetriNet net = synth.foldToNet_labeled(nonImplied, false);
		      
		      return net;
		} catch (InvalidModelException e) {
			e.printStackTrace();
		}

		return null;
	}

	  /**
	   * This method separates causality and conflict relations into two subgraphs. Causality is copied to
	   * a directed graph (i.e. "dgraph") and Conflict is copied into an undirected graph (i.e. "uedges" & "unodes").
	   * At the same time, this method identifies the set of source and sink nodes.
	   * 
	   * @param orgraph IN: Ordering relations graph
	   * @param dgraph  IN/OUT: Directed graph representing the causality relation
	   * @param dgnodes   IN: List of nodes in "dgraph"
	   * @param sources OUT: Set of source nodes
	   * @param sinks   OUT: Set of sink nodes
	   */
	  private void separateColors(ColoredGraph orgraph,
	      Map<Integer, Set<Integer>> reducedFlow,
	      Map<Integer, Set<Integer>> implicitFlow,
	      Set<Integer> sources, Set<Integer> sinks,
	      
	      Map<String, String> labelMap, Map<String, de.hpi.bpt.process.Node> tasks,
	      Map<String, de.hpi.bpt.process.Node> clones)
	  {
	    Set<Integer> vertices = orgraph.getVertices();
	    sources.addAll(vertices);
	    sinks.addAll(vertices);

	    BasicDigraph dgraph = new BasicDigraph();
	    List<Node> dgnodes = dgraph.genNodes(vertices.size());

	    for (Node node: dgnodes) {
	      int src = node.nodeId();
	      for (int tgt: orgraph.postSet(src)) {
	        if (!orgraph.hasEdge(tgt, src)) {
	          dgraph.genEdge(node, dgnodes.get(tgt));
	          sources.remove(tgt);
	          sinks.remove(src);
	        }
	      }
	    }

	    CList<net.stixar.graph.Edge> removed = acyclicReduce(dgraph, labelMap, tasks, clones);
	    for (Node n: dgnodes) {
	      reducedFlow.put(n.nodeId(), new HashSet<Integer>());
	      implicitFlow.put(n.nodeId(), new HashSet<Integer>());
	    }
	    for (net.stixar.graph.Edge edge: dgraph.edges()) {
	      reducedFlow.get(edge.source().nodeId()).add(edge.target().nodeId());
	    }
	    for (net.stixar.graph.Edge edge : removed) {
//	      System.out.println("keeping implicit edge "+edge);
	      implicitFlow.get(edge.source().nodeId()).add(edge.target().nodeId());
	    }
	  }

	  /**
	  Remove redundant edges from a mutable acyclic digraph.

	  @param mdg the mutable acyclic digraph from which to remove
	  edges.
	  @return a list of the removed edges.
	  */
	  public static CList<net.stixar.graph.Edge> acyclicReduce(MutableDigraph mdg,
	      Map<String, String> labelMap, Map<String, de.hpi.bpt.process.Node> tasks,
	      Map<String, de.hpi.bpt.process.Node> clones)
	  {
	     TopSorter ts = new TopSorter(mdg);
	     ts.run();
	     CList<Node> tsort = ts.getSort();
	     mdg.sortEdges(NodeOrder.getEdgeComparator(ts.order()));
	     ByteNodeMatrix m = Transitivity.acyclicClosure(mdg);
	     CList<net.stixar.graph.Edge> remove = new CList<net.stixar.graph.Edge>();
	     for (Node i : tsort) {
	         for (net.stixar.graph.Edge e = i.out(); e != null; e = e.next()) {
	             Node j = e.target();
	             if (m.get(i, j) != 0) {
	                 for (net.stixar.graph.Edge ee = e.next(); ee != null; ee = ee.next()) {
	                     Node k = ee.target();
	                     if (m.get(j, k) != 0) {
	                         m.set(i, k, (byte) 0);
	                     }
	                 }
	             } else {
	                 remove.add(e);
	             }
	         }
	     }
	     for (net.stixar.graph.Edge e : remove) {
	         mdg.remove(e);
	     }
	     return remove;
	  }
}
