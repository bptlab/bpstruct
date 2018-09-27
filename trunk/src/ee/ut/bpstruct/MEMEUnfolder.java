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
package ee.ut.bpstruct;

import hub.top.petrinet.Arc;
import hub.top.petrinet.PetriNet;
import hub.top.petrinet.Place;
import hub.top.uma.DNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ee.ut.bpstruct.unfolding.uma.BPstructBP;
import ee.ut.bpstruct.unfolding.uma.MEMEUnfolder_PetriNet;

public class MEMEUnfolder {
	private MEMEUnfolder_PetriNet unfolder;
	private Unfolding unfolding;
	private Set<Place> sinks;
	private FullBehavioralProfiler profiler;

	public MEMEUnfolder(PetriNet net) {
		this.unfolder = new MEMEUnfolder_PetriNet(net);
		sinks = new HashSet<Place>(net.getPlaces());
		
		for (Arc arc: net.getArcs())
			sinks.remove(arc.getSource());
	}
	
	public Unfolding perform() {
		unfolder.computeUnfolding();
		BPstructBP unf = unfolder.getBP();
		Unfolding result =  unfolding = new Unfolding(unf);

		// Check soundness
		
		profiler = new FullBehavioralProfiler(unfolding);
		profiler.checkSoundness(sinks);
		
		return result;
	}

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
		
		String unsafeFillString = "fillcolor=green";
		String localDeadlockFillString = "fillcolor=red";
		
		Map<DNode, Integer> unsafecondsMap = new HashMap<DNode, Integer>();

		int counter = 0;
		for (DNode n: profiler.unboundedConds.keySet()) {
			unsafecondsMap.put(n, counter);
			for (DNode m: profiler.unboundedConds.get(n))
				unsafecondsMap.put(m, counter);
			counter++;
		}
		
		// first print all conditions
		b.append("\n\n");
		b.append("node [shape=circle];\n");
		for (DNode n : unfolding.getAllConditions()) {

			if (n.isAnti && n.isHot)
				b.append("  c" + n.globalId + " [" + antiFillString + "]\n");
			else if (n.isCutOff)
				b.append("  c" + n.globalId + " [" + cutOffFillString + "]\n");
			else if (profiler.localdeadlocks.contains(n))
				b.append("  c" + n.globalId + " [" + localDeadlockFillString + "]\n");
			else if (unsafecondsMap.containsKey(n))
				b.append("  c" + n.globalId + " [" + unsafeFillString + "]\n");				
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
		for (DNode n : unfolding.getAllEvents()) {

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
		for (DNode n : unfolding.allConditions) {
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
		for (DNode n : unfolding.allEvents) {
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
		for (DNode n : unfolding.cutoffs) {
			if (n.isCutOff && unfolding.getCorr(n) != null) {
				b.append("  e" + n.globalId + " -> e" + unfolding.getCorr(n).globalId
						+ " [weight=10000.0]\n");
			}
		}
		
		b.append("}");
		return b.toString();	
	}
	
	public Unfolding expand(Set<DNode> toExpand, int phase) {
		unfolding.expand(toExpand, phase);
		
		return unfolding;
	}

	public boolean isCorrInLocalConfig(DNode cutoff, DNode corr) {
		return unfolding.isCorrInLocalConfig(cutoff, corr);
	}
}
