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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.Stack;

import ee.ut.bpstruct.RestructurerHelper;
import ee.ut.bpstruct.unfolding.uma.BPstructBP;
import ee.ut.bpstruct.unfolding.uma.Unfolder_PetriNet;

import hub.top.petrinet.PetriNet;
import hub.top.uma.DNode;

public class MEMEUnfolder {
	private Unfolder_PetriNet unfolder;
	private RestructurerHelper helper;
	private boolean meme;

	public MEMEUnfolder(RestructurerHelper helper, PetriNet net) {
		this(helper, net, true);
	}

	
	public MEMEUnfolder(RestructurerHelper helper, PetriNet net, boolean meme) {
		this.helper = helper;
		this.meme = meme;
		this.unfolder = new Unfolder_PetriNet(net, !meme);
	}
	
	public Unfolding perform() {
		unfolder.computeUnfolding();
		BPstructBP unf = unfolder.getBP();
		Unfolding result =  new Unfolding(helper, unf);
		
		if (meme) {

			// --- local deadlocks ... TODO: complete pruning of deadlocked branches
			unf.findDeadConditions(false);
			LinkedList<DNode> deadconds = unf.getDeadConditions();

			Set<DNode> deadlockedConds = new HashSet<DNode>();
			
			for (DNode cond: deadconds)
				if (!unf.properName(cond).startsWith("_exit_") && !unf.isCutOffEvent(cond.pre[0]))
					deadlockedConds.add(cond);

			if (deadlockedConds.size() > 0) {
				DNode icond = result.getInitialConditions().get(0);
				Set<DNode> nodes = new HashSet<DNode>();
				Set<DNode> andsplits = new HashSet<DNode>();
				Stack<DNode> worklist = new Stack<DNode>();
				worklist.addAll(deadlockedConds);
				
				while (!worklist.isEmpty()) {
					DNode curr = worklist.pop();
					nodes.add(curr);
					for (DNode pre: curr.pre)
						if (!nodes.contains(pre) && !worklist.contains(pre) && !pre.equals(icond)) {
							worklist.push(pre);
							if (pre.post.length > 1)
								andsplits.add(pre);
						}
				}
				
				for (DNode n: andsplits)
					for (DNode succ: n.post)
						if (!nodes.contains(succ))
							worklist.push(succ);

				while (!worklist.isEmpty()) {
					DNode curr = worklist.pop();
					nodes.add(curr);
					if (curr.post == null) continue;
					for (DNode succ: curr.post)
						if (!nodes.contains(succ) && !worklist.contains(succ))
							worklist.push(succ);
				}

				result.pruneNodes(nodes);
//				System.out.println("done");
			}

			
			
			// --- local unsafe branches
			LinkedList<DNode> maxNodes = unf.getBranchingProcess().getCurrentMaxNodes();
			HashMap<DNode, Set<DNode>> concconds = unf.getConcurrentConditions();
			HashMap<DNode, Set<DNode>> unboundedConds = new HashMap<DNode, Set<DNode>>();
			
			while (!maxNodes.isEmpty()) {
				DNode cond = maxNodes.removeFirst();
				if (concconds.containsKey(cond)) {
//					System.out.println("Analyzing:  " + cond);
					Set<DNode> found = new HashSet<DNode>();
					for (DNode condp : concconds.get(cond))
						if (result.getProperName(condp).equals(result.getProperName(cond))) {
							found.add(condp);
							maxNodes.remove(condp);
//							System.out.println("\t" + condp);
						}
//					System.out.println("bounded at:  " + (found.size() + 1));
					if (found.size() > 0)
						unboundedConds.put(cond, found);
				}
			}
			
			if (unboundedConds.size() > 0) {
				DNode icond = result.getInitialConditions().get(0);
				Set<DNode> nodes = new HashSet<DNode>();
				Set<DNode> andsplits = new HashSet<DNode>();
				Stack<DNode> worklist = new Stack<DNode>();
				for (DNode cond: unboundedConds.keySet()) {
					worklist.add(cond);
					worklist.addAll(unboundedConds.get(cond));
				}
				
				while (!worklist.isEmpty()) {
					DNode curr = worklist.pop();
					nodes.add(curr);
					for (DNode pre: curr.pre)
						if (!nodes.contains(pre) && !worklist.contains(pre) && !pre.equals(icond)) {
							worklist.push(pre);
							if (pre.post.length > 1)
								andsplits.add(pre);
						}
				}
				
				for (DNode n: andsplits)
					for (DNode succ: n.post)
						if (!nodes.contains(succ))
							worklist.push(succ);

				while (!worklist.isEmpty()) {
					DNode curr = worklist.pop();
					nodes.add(curr);
					if (curr.post == null) continue;
					for (DNode succ: curr.post)
						if (!nodes.contains(succ) && !worklist.contains(succ))
							worklist.push(succ);
				}

				result.pruneNodes(nodes);
//				System.out.println("done");
			}
		}
		return result;
	}


	public Unfolding expand(Set<DNode> toExpand) {
		throw new RuntimeException("MEME Unfolder cannot be used with cyclic rigids");
	}
}
