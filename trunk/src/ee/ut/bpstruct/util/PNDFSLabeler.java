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
package ee.ut.bpstruct.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.hpi.bpt.hypergraph.abs.Vertex;
import de.hpi.bpt.process.petri.Place;
import de.hpi.bpt.process.petri.Transition;

public class PNDFSLabeler {
	private Set<Pair> backedges = new HashSet<Pair>();
	private Set<Pair> forwardedges = new HashSet<Pair>();
	private boolean mixedLogic = false;
	private Object currLogic = null;
	
	static class Pair {
		Vertex src;
		Vertex tgt;
		public Pair(Vertex s, Vertex t) { src = s; tgt = t; }
	}
	public PNDFSLabeler(Map<Vertex, List<Vertex>> incoming, Map<Vertex, List<Vertex>> outgoing, Vertex vertex) {
		dfs(incoming, outgoing, vertex);
	}

	enum Color { GRAY, BLACK };
	private void dfs(Map<Vertex, List<Vertex>> incoming, Map<Vertex, List<Vertex>> outgoing, Vertex vertex) {
		dfs(incoming, outgoing, vertex, new HashMap<Vertex, Color>());
	}

	private void dfs(Map<Vertex, List<Vertex>> incoming, Map<Vertex, List<Vertex>> outgoing, Vertex curr, HashMap<Vertex, Color> hashMap) {
		hashMap.put(curr, Color.GRAY);
		
		if (incoming.get(curr).size() > 1 || outgoing.get(curr).size() > 1 || outgoing.get(curr).size() == 0) {
			Object currClass = curr instanceof Transition ? Transition.class : Place.class;
			if (currLogic == null)
				currLogic = currClass;
			else if (!currLogic.equals(currClass))
				mixedLogic = true;
		}
		
		for (Vertex succ : outgoing.get(curr)) {
			if (!hashMap.containsKey(succ)) {
				dfs(incoming, outgoing, succ, hashMap);
			}
			if (hashMap.get(succ) == Color.GRAY) {
				backedges.add(new Pair(curr, succ));
			} else {
				forwardedges.add(new Pair(curr, succ));
			}
		}
		hashMap.put(curr, Color.BLACK);
	}
	
	public boolean isMixedLogic() { return mixedLogic; }
	public boolean isCyclic() { return backedges.size() > 0; }
	public Object getLogic() { return currLogic; } 
}
