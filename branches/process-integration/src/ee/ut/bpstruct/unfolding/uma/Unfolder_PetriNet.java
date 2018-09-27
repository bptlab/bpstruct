/* 
 * Copyright (C) 2010 - Artem Polyvyanyy, Luciano Garcia Banuelos
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
package ee.ut.bpstruct.unfolding.uma;

import hub.top.petrinet.PetriNet;
import hub.top.uma.InvalidModelException;

/**
 * This class is a modification to the original implementation provided in uma package
 * This version provides access to the Unfolding and allows incremental unfolding.
 * 
 * @author Luciano Garcia Banuelos, Artem Polyvyanyy
 */
public class Unfolder_PetriNet {

	// a special representation of the Petri net
	private BPstructBPSys sys;
	
	// a branching process of the Petri net (the unfolding)
	private BPstructBP bp;
		
	public Unfolder_PetriNet(PetriNet net) {
		this(net, false);
	}
	
	/**
	 * Initialize the unfolder to construct a finite complete prefix
	 * of a safe Petri net.
	 * 
	 * @param net a safe Petri net
	 */
	public Unfolder_PetriNet(PetriNet net, boolean meme) {
		try {
			sys = new BPstructBPSys(net);

			// initialize unfolder
			bp = new BPstructBP(sys);
			// configure to unfold a Petri net
			bp.configure_PetriNet();
			// stop construction of unfolding when reaching an unsafe marking
			if (meme) {
				bp.configure_stopIfUnSafe();
				bp.setUnfoldMEME();
			}

		} catch (InvalidModelException e) {

			System.err.println("Error! Invalid model.");
			System.err.println(e);
			sys = null;
			bp = null;
		}
	}

	/**
	 * Compute the unfolding of the net
	 */
	public void computeUnfolding() {
		int total_steps = 0;
		int current_steps = 0;
		// extend unfolding until no more events can be added
		while ((current_steps = bp.step()) > 0) {
			total_steps += current_steps;
			System.out.print(total_steps+"... ");
		}
	}
	
	/**
	 * @return the unfolding in GraphViz dot format
	 */
	public String getUnfoldingAsDot() {
		return bp.getBranchingProcess().toDot(sys.properNames);
	}

	/**
	 * Get the branching process
	 */
	public BPstructBP getBP() {
		return bp;
	}
}
