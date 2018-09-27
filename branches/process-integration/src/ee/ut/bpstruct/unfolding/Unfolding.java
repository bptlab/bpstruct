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
package ee.ut.bpstruct.unfolding;

import hub.top.uma.DNode;
import hub.top.uma.DNodeBP;
import hub.top.uma.DNodeSet;
import hub.top.uma.DNodeSys;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.log4j.Logger;

import ee.ut.bpstruct.RestructurerHelper;
import ee.ut.bpstruct.unfolding.uma.BPstructBP;

/**
 * This class is a kind of container for storing the information about an
 * unfolding. It is intended for cloning and pruning (as required during the
 * restructuring of cyclic rigid components). As DNodeSet and DNodeBP are
 * immutable, we decided to add this utility class.
 * 
 * @author Luciano Garcia Banuelos
 */
public class Unfolding {
	static Logger logger = Logger.getLogger(Unfolding.class);
	protected RestructurerHelper helper = null;

	protected List<DNode> initialConditions = null;
	protected List<DNode> allConditions = null;
	protected List<DNode> allEvents = null;
	protected Set<DNode> cutoffs = null;
	protected HashMap<DNode, DNode> elementary_ccPair = null;
	protected DNodeBP brproc = null;
	protected DNodeSys dnodesys = null;
	
	protected Map<DNode, Unfolding> container = new HashMap<DNode, Unfolding>();
	protected Set<DNode> localCorrSet = new HashSet<DNode>();
	
	/**
	 * This constructor copies the information from a concrete unfolding.
	 * 
	 * @param brproc
	 */
	public Unfolding(RestructurerHelper helper, BPstructBP brproc) {
		this.helper = helper;
		this.brproc = brproc;
		this.dnodesys = brproc.getSystem();
		DNodeSet nodeSet = brproc.getBranchingProcess();
		allEvents = new LinkedList<DNode>(nodeSet.getAllEvents());
		elementary_ccPair = new HashMap<DNode, DNode>(brproc.getElementary_ccPair()); // TODO: Check if the equivalentNode() corresponds to getElementary_ccPair()
		initialConditions = new LinkedList<DNode>(nodeSet.initialConditions);
		cutoffs = new HashSet<DNode>();

		// "elementary_ccPair" includes postset of cutoffs
		for (DNode n : elementary_ccPair.keySet())
			if (n.isEvent) {
				cutoffs.add(n);
				container.put(elementary_ccPair.get(n), this);
			}
		
		allConditions = new LinkedList<DNode>(nodeSet.allConditions);
		
		if (logger.isTraceEnabled()) {
			try {
				String filename = String.format(this.helper.getDebugDir().getName() + "/unf_%s.dot", helper.getModelName());
				PrintStream out = new PrintStream(filename);
				out.print(brproc.toDot());
				out.close();
				logger.trace("Unfolding serialized into: " + filename);
			} catch (FileNotFoundException e) {
				logger.error(e);
			}
		}
	}

	/**
	 * This constructor is supposed to be used for CLONING parts of a concrete
	 * unfolding.
	 */
	protected Unfolding(DNodeBP parent) {
		allConditions = new LinkedList<DNode>();
		allEvents = new LinkedList<DNode>();
		elementary_ccPair = new HashMap<DNode, DNode>();
		initialConditions = new LinkedList<DNode>();
		cutoffs = new HashSet<DNode>();
		dnodesys = parent.getSystem();
		brproc = parent;
	}

	public List<DNode> getAllEvents() {
		return allEvents;
	}

	public List<DNode> getInitialConditions() {
		return initialConditions;
	}

	public List<DNode> getAllConditions() {
		return allConditions;
	}

	public Set<DNode> getCutoffs() {
		return cutoffs;
	}

	public DNode getCorr(DNode cutoff) {
		return elementary_ccPair.get(cutoff);
	}

	public Set<DNode> getLocalCorrSet() {
		return localCorrSet;
	}
	
	public String getProperName(DNode n) {
		return dnodesys.properNames[n.id];
	}
	
	public void pruneNodes(Set<DNode> nodes) {
		DNode icond = this.initialConditions.get(0);
		LinkedList<DNode> post = new LinkedList<DNode>();
		for (DNode ev: icond.post)
			if (!nodes.contains(ev))
				post.add(ev);
		icond.post = (DNode[]) post.toArray(new DNode[0]);
		
		nodes.remove(icond);
		allEvents.removeAll(nodes);
		allConditions.removeAll(nodes);
		cutoffs.removeAll(nodes);
		
		if (logger.isTraceEnabled()) {
			try {
				String filename = String.format(this.helper.getDebugDir().getName() + "/pruned_unf_%s.dot", helper.getModelName());
				PrintStream out = new PrintStream(filename);
				out.print(toDot());
				out.close();
				logger.trace("Unfolding serialized into: " + filename);
			} catch (FileNotFoundException e) {
				logger.error(e);
			}
		}
	}

//	----------------------------------------------------------------------
//	----------------------------------------------------------------------
//	Expansion of the unfolding
//	----------------------------------------------------------------------	
//	----------------------------------------------------------------------

	// Helper class: Stores the information about cutoff
	class Info {
		DNode cutoff;
		DNode corresponding;
		Set<DNode> cutoff_cut;
		Set<DNode> corr_cut;
		Info(DNode cutoff, DNode corresponding, Set<DNode> cutoff_cut, Set<DNode> corr_cut) {
			this.cutoff = cutoff;
			this.corresponding = corresponding;
			this.cutoff_cut = cutoff_cut;
			this.corr_cut = corr_cut;
		}
	}

	// Helper ... to emulate a pass-by-reference 
	class Container {
		DNode dnode = null;
	}
	
	/**
	 * This method is the entry point for expanding the unfolding. It receives 
	 * @param toExpand
	 * @param phase
	 */
	public void expand(Set<DNode> toExpand, int phase) {		
		for (DNode event: toExpand)
			expand(event, toExpand);		
				
		for (DNode event: toExpand) {
			event.isCutOff = false;
			for (DNode cond: event.post) {
				cond.isCutOff = false;
			}
			cutoffs.remove(event);
			elementary_ccPair.remove(event);
		}

		if (logger.isTraceEnabled()) {
			try {
				String filename = String.format(this.helper.getDebugDir().getName() + "/expanded_unf_%d_%s.dot", phase, helper.getModelName());
				PrintStream out = new PrintStream(filename);
				out.print(toDot());
				out.close();
				logger.trace("Expanded unfolding serialized into: " + filename);
			} catch (FileNotFoundException e) {
				logger.error(e);
			}
		}

	}
	
	class Tuple {
		DNode dnode;
		int count;
		public Tuple(DNode dnode, int count) { this.dnode = dnode; this.count = count; }
	}
	
	/**
	 * This method expands the complete prefix unfolding starting at a given cutoff. It uses a kind of Depth-First Traversal over
	 * a AND/OR graph (AND nodes correspond to branching/synchronizing transitions).
	 * 
	 * @param cutoff
	 * @param toExpand
	 */
	private void expand(DNode cutoff, Set<DNode> toExpand) {
		DNode corresponding = getCorr(cutoff);
		
		// Initialize the cuts for cutoff and corresponding events
		Set<DNode> cutoff_cut = getCut(cutoff); // Arrays.asList(brproc.getBranchingProcess().getPrimeCut(cutoff, false, false)));
		Set<DNode> corr_cut = getCut(corresponding); // Arrays.asList(brproc.getBranchingProcess().getPrimeCut(corresponding, false, false)));
		
		
		// activeCutoff is a local copy of the set of cutoffs
		Set<DNode> activeCutoff = new HashSet<DNode>(getCutoffs());
		// we have to hide this cutoff to avoid wrong hops, as "cutoff" might be already expanded
		activeCutoff.remove(cutoff);

		Stack<Info> stack = new Stack<Info>();
		Stack<Info> waitingstack = new Stack<Info>();
		stack.push(new Info(cutoff, corresponding, cutoff_cut, corr_cut));
		Container actualCorr = new Container();
		while (!stack.isEmpty()) {
			Info info = stack.pop();
			
			// Check whether the event in the top of the stack is a "proper cyclic" cutoff or not
			// Note that the stack stores all the information about cutoff and corresponding
			if (!info.cutoff.equals(cutoff) && checkCyclicCase(info.cutoff, info.corresponding, info.cutoff_cut, info.corr_cut, actualCorr)) {
				// Yeah! Candidate event is actually a cutoff
				info.cutoff.isCutOff = true;
				// Note that during the expansion, the original corresponding might be updated (i.e. (bottom-up) closest event)
				info.corresponding = actualCorr.dnode;
				
				// Conditions on the postset of "cutoff" must be also marked as cutoff (according to Dirk's unfolder)
				// and pairs of "cutoff" and "corresponding" conditions must be added to "elementary_ccPair"
				for (DNode cond: info.cutoff.post) {
					cond.isCutOff = true;
					for (DNode condp: info.corresponding.post)
						if (cond.id == condp.id) {
							elementary_ccPair.put(cond, condp);
							break;
						}
				}
				// Finally, the "cutoff" event is properly registered in the corresponding data structures
				cutoffs.add(info.cutoff);
				elementary_ccPair.put(info.cutoff, info.corresponding);				
			} else {
				
				// Iterate over the set of conditions in the postset of the candiate (info.cutoff)
				for (DNode cond: info.cutoff.post) {
					DNode ccond = null;
					for (DNode _cond: info.corresponding.post)
						if (cond.id == _cond.id) {
							ccond = _cond;
							break;
						}
					
					if (ccond.post == null || ccond.post.length == 0) continue;
					
					// Now --- we have to iterated over the set of events
					for (DNode _ev: ccond.post) {
						DNode ev = _ev;
						
						if (activeCutoff.contains(_ev)) {
							ev = elementary_ccPair.get(_ev);
						}
						
						DNode new_ev = null;
						
						boolean found = false;
						// Check whether the event reached is a synchronizing one and is in "waitingstack"
						
						if (!waitingstack.isEmpty()) {
							Info linfo_ref = null;
							for (Info linfo : waitingstack)
								if (waitingstack.peek().cutoff.id == _ev.id) {
									// If so, then retrieve the information from "waitingstack"
									new_ev = linfo.cutoff;
									// add arc:    cond -> new_ev
									new_ev.addPreNode(cond);
									found = true;
									linfo_ref = linfo;
									break;
								}
							if (found)
								waitingstack.remove(linfo_ref);
						}
						if (!found) {
							// The event is reached for the first time
							new_ev = new DNode(_ev.id, cond);
							new_ev.isEvent = true;
							allEvents.add(new_ev);
							for (DNode c: ev.post) {
								DNode new_cond = new DNode(c.id, new_ev);
								allConditions.add(new_cond);
								new_ev.addPostNode(new_cond);
								corr_cut.add(c);
								cutoff_cut.add(new_cond);
							}
						}
						
						cond.addPostNode(new_ev);
						
						// Test if all preset of new_ev has been already visited
						if (new_ev.pre.length == _ev.pre.length) {
							cutoff_cut = getCut(new_ev);
							corr_cut = getCut(ev);
							// all preset conditions have been visited
							stack.push(new Info(new_ev, ev, cutoff_cut, corr_cut));
						} else
							// waiting for some branches to be completed
							waitingstack.push(new Info(new_ev, ev, cutoff_cut, corr_cut));
					}
					
//					if (logger.isTraceEnabled()) {
//						try {
//							String filename = String.format(this.helper.getDebugDir().getName() + "/expanded_unf__%s.dot", helper.getModelName());
//							PrintStream out = new PrintStream(filename);
//							out.print(toDot());
//							out.close();
//							logger.trace("Expanded unfolding serialized into: " + filename);
//						} catch (FileNotFoundException e) {
//							logger.error(e);
//						}
//					}

				}
			}
		}
		
	}
	
	/**
	 * Restrict cutoff criterion for cyclic case
	 * 
	 * A cutoff is cyclic if either cutoff or its corresponding event 
	 * refer to a transition of the originative net that is part of
	 * some cyclic path of the net
	 * 
	 * @param cutoff Cutoff event
	 * @param corr Corresponding event
	 * @param cutoff_cut Cutoff cut
	 * @param corr_cut Corresponding cut
	 * @return <code>true</code> if cyclic cutoff criterion holds; otherwise <code>false</code>
	 */
	protected boolean checkCyclicCase(DNode cutoff, DNode corr, Set<DNode> cutoff_cut, Set<DNode> corr_cut, Container actual) {
		return checkConcurrency(cutoff,corr,cutoff_cut,corr_cut) &&
				isCorrInLocalConfig(cutoff,corr, actual) &&
				cutoff.post.length==1 &&
				corr.post.length==1 && 
				corr.post[0].post.length>1;
	}
	
	/**
	 * Check if conditions in cuts of cutoff and corresponding events are shared, except of postsets
	 * 
	 * @param cutoff Cutoff event
	 * @param corr Corresponding event
	 * @param cutoff_cut Cutoff cut
	 * @param corr_cut Corresponding cut
	 * @return <code>true</code> if shared; otherwise <code>false</code>
	 */
	protected boolean checkConcurrency(DNode cutoff, DNode corr, Set<DNode> cutoff_cut, Set<DNode> corr_cut) {
		Set<Integer> cutoffSet = new HashSet<Integer>();
		Set<Integer> corrSet = new HashSet<Integer>();

		for (DNode n: cutoff_cut) cutoffSet.add(n.globalId);
		for (DNode n: corr_cut) corrSet.add(n.globalId);
		for (int i=0; i<cutoff.post.length; i++) cutoffSet.remove(cutoff.post[i].globalId);
		for (int i=0; i<corr.post.length; i++) corrSet.remove(corr.post[i].globalId);
		
		if (cutoffSet.size()!=corrSet.size()) return false;
		for (Integer n : cutoffSet) {
			if (!corrSet.contains(n)) return false;
		}
		
		return true;
	}
	
	/**
	 * Check if corresponding event is in the local configuration of the cutoff
	 * LUCIANO: ------------------------------------------------
	 * As the original unfolding is modified, the corresponding has to be updated to
	 * refer to the closest copy of the event (in the local cofiguration).
	 * ---------------------------------------------------------
	 * 
	 * @param cutoff Cutoff event
	 * @param corr   Corresponding event
	 * @param actual Closest copy of the corresponding event
	 * @return <code>true</code> if corresponding event is in the local configuration of the cutoff; otherwise <code>false</code>
	 */
	protected boolean isCorrInLocalConfig(DNode cutoff, DNode corr, Container actual) {
		Stack<DNode> stack = new Stack<DNode>();
		Set<DNode> visited = new HashSet<DNode>();
		stack.push(cutoff);
		while (!stack.isEmpty()) {
			DNode curr = stack.pop();
			visited.add(curr);
			if (!curr.equals(cutoff) && curr.id == corr.id) {
				if (actual != null)
					actual.dnode = curr;
				return true;
			}
			if (curr.pre == null) continue;
			for (DNode p: curr.pre)
				if (!visited.contains(p) && !stack.contains(p))
					stack.push(p);
		}
		return false;
	}

	public Set<DNode> getCut(DNode event) {
		Set<DNode> preset = new HashSet<DNode>();
		Set<DNode> postset = new HashSet<DNode>();
		Set<DNode> localconf = getLocalConfig(event);
		for (DNode e: localconf) {
			for (DNode pre: e.pre) preset.add(pre);
			for (DNode post: e.post) postset.add(post);
		}
		postset.removeAll(preset);
		
		return postset;
	}
	
	public Set<DNode> getLocalConfig(DNode event) {
		Stack<DNode> stack = new Stack<DNode>();
		Set<DNode> visited = new HashSet<DNode>();
		Set<DNode> events = new HashSet<DNode>();
		stack.push(event);
		while (!stack.isEmpty()) {
			DNode curr = stack.pop();
			visited.add(curr);
			if (curr.isEvent)
				events.add(curr);
			if (curr.pre == null) continue;
			for (DNode p: curr.pre)
				if (!visited.contains(p) && !stack.contains(p))
					stack.push(p);
		}
		return events;
	}

	public Set<DNode> getBackwardsClosedSet(DNode event) {
		Stack<DNode> stack = new Stack<DNode>();
		Set<DNode> visited = new HashSet<DNode>();
		stack.push(event);
		while (!stack.isEmpty()) {
			DNode curr = stack.pop();
			visited.add(curr);
			if (curr.pre == null) continue;
			for (DNode p: curr.pre)
				if (!visited.contains(p) && !stack.contains(p))
					stack.push(p);
		}
		return visited;
	}

	// -----------------------------------------------------------------
	// --------   UTILITIES
	// -----------------------------------------------------------------
	
	/**
	 * Serializes the unfolding in the form of string, using DOT format
	 * 
	 * @return
	 */
	public String toDot() {
		StringBuilder b = new StringBuilder();
		b.append("digraph BP {\n");

		// standard style for nodes and edges
		b.append("graph [fontname=\"Helvetica\" nodesep=0.3 ranksep=\"0.2 equally\" fontsize=10];\n");
		b.append("node [fontname=\"Helvetica\" fontsize=8 fixedsize width=\".3\" height=\".3\" label=\"\" style=filled fillcolor=white];\n");
		b.append("edge [fontname=\"Helvetica\" fontsize=8 color=white arrowhead=none weight=\"20.0\"];\n");

		// String tokenFillString =
		// "fillcolor=black peripheries=2 height=\".2\" width=\".2\" ";
		String cutOffFillString = "fillcolor=gold";
		String antiFillString = "fillcolor=red";
		String impliedFillString = "fillcolor=violet";
		String hiddenFillString = "fillcolor=grey";

		// first print all conditions
		b.append("\n\n");
		b.append("node [shape=circle];\n");
		for (DNode n : getAllConditions()) {

			if (n.isAnti && n.isHot)
				b.append("  c" + n.globalId + " [" + antiFillString + "]\n");
			else if (n.isCutOff)
				b.append("  c" + n.globalId + " [" + cutOffFillString + "]\n");
			else
				b.append("  c" + n.globalId + " []\n");

			String auxLabel = "";

			b.append("  c" + n.globalId + "_l [shape=none];\n");
			b.append("  c" + n.globalId + "_l -> c" + n.globalId
					+ " [headlabel=\"" + n + " " + auxLabel + "\"]\n");
		}

		// then print all events
		b.append("\n\n");
		b.append("node [shape=box];\n");
		for (DNode n : getAllEvents()) {

			if (n.isAnti && n.isHot)
				b.append("  e" + n.globalId + " [" + antiFillString + "]\n");
			else if (n.isAnti && !n.isHot)
				b.append("  e" + n.globalId + " [" + hiddenFillString + "]\n");
			else if (n.isImplied)
				b.append("  e" + n.globalId + " [" + impliedFillString + "]\n");
			else if (n.isCutOff)
				b.append("  e" + n.globalId + " [" + cutOffFillString + "]\n");
			else
				b.append("  e" + n.globalId + " []\n");

			String auxLabel = "";

			b.append("  e" + n.globalId + "_l [shape=none];\n");
			b.append("  e" + n.globalId + "_l -> e" + n.globalId
					+ " [headlabel=\"" + n + " " + auxLabel + "\"]\n");
		}

		/*
		 * b.append("\n\n"); b.append(" subgraph cluster1\n");
		 * b.append(" {\n  "); for (CNode n : nodes) { if (n.isEvent)
		 * b.append("e"+n.localId+" e"+n.localId+"_l "); else
		 * b.append("c"+n.localId+" c"+n.localId+"_l "); }
		 * b.append("\n  label=\"\"\n"); b.append(" }\n");
		 */

		// finally, print all edges
		b.append("\n\n");
		b.append(" edge [fontname=\"Helvetica\" fontsize=8 arrowhead=normal color=black];\n");
		for (DNode n : allConditions) {
			String prefix = n.isEvent ? "  e" : "  c";
			if (n.post == null) continue;
			for (int i = 0; i < n.post.length; i++) {
				if (n.post[i] == null)
					continue;

				if (n.post[i].isEvent)
					b.append(prefix + n.globalId + " -> " + 
							 "e" + n.post[i].globalId + " [weight=10000.0]\n");
				else
					b.append(prefix + n.globalId + " -> " + 
							 "c" + n.post[i].globalId + " [weight=10000.0]\n");
			}
		}
		for (DNode n : allEvents) {
			String prefix = n.isEvent ? "  e" : "  c";
			if (n.post == null) continue;
			for (int i = 0; i < n.post.length; i++) {
				if (n.post[i] == null)
					continue;

				if (n.post[i].isEvent)
					b.append(prefix + n.globalId + " -> " + 
							 "e" + n.post[i].globalId + " [weight=10000.0]\n");
				else
					b.append(prefix + n.globalId + " -> " + 
							 "c" + n.post[i].globalId + " [weight=10000.0]\n");
			}
		}

		// add links from cutoffs to corresponding events
		b.append("\n\n");
		b.append(" edge [fontname=\"Helvetica\" fontsize=8 arrowhead=normal color=red];\n");
		for (DNode n : allEvents) {
			if (n.isCutOff && getCorr(n) != null) {
				b.append("  e" + n.globalId + " -> e" + getCorr(n).globalId
						+ " [weight=10000.0]\n");
			}
		}

		b.append("}");
		return b.toString();
	}

	public boolean isCorrInLocalConfig(DNode cutoff, DNode corr) {
		return isCorrInLocalConfig(cutoff, corr, new Container());
	}

	public DNode getLowestCommonAncestor(Set<DNode> localconf, DNode corr) {
		DNode branching = null;
		Stack<DNode> stack = new Stack<DNode>();
		Set<DNode> visited = new HashSet<DNode>();
		stack.push(corr);
		while (!stack.isEmpty()) {
			DNode curr = stack.pop();
			visited.add(curr);
			if (localconf.contains(curr)) {
				branching = curr;
				break;
			}
			if (curr.pre == null) continue;
			for (DNode p: curr.pre)
				if (!visited.contains(p) && !stack.contains(p))
					stack.push(p);
		}
		return branching;
	}
}
