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

import de.hpi.bpt.process.Gateway;
import de.hpi.bpt.process.Node;

public class DFSLabeler {
	private Set<Pair> backedges = new HashSet<Pair>();
	private Set<Pair> forwardedges = new HashSet<Pair>();
	private boolean mixedLogic = false;
	private Object currLogic = null;
	
	static class Pair {
		Node src;
		Node tgt;
		public Pair(Node s, Node t) { src = s; tgt = t; }
	}
	public DFSLabeler(Map<Node, List<Node>> adjList, Node entry) {
		dfs(adjList, entry);
	}

	enum Color { GRAY, BLACK };
	private void dfs(Map<Node, List<Node>> adjList, Node v) {
		dfs(adjList, v, new HashMap<Node, Color>());
	}

	private void dfs(Map<Node, List<Node>> adjList, Node v, Map<Node, Color> colorMap) {
		colorMap.put(v, Color.GRAY);
		
		if (v instanceof Gateway) {
			Gateway gw = (Gateway)v;
			if (currLogic == null)
				currLogic = gw.getGatewayType();
			else if (currLogic != gw.getGatewayType())
				mixedLogic = true;
		}
		
		for (Node next : adjList.get(v)) {
			if (!colorMap.containsKey(next)) {
				dfs(adjList, next, colorMap);
			}
			else if (colorMap.get(next) == Color.GRAY) {
				backedges.add(new Pair(v, next));
			} else {
				forwardedges.add(new Pair(v, next));
			}
		}
		colorMap.put(v, Color.BLACK);
	}
	
	public boolean isMixedLogic() { return mixedLogic; }
	public boolean isCyclic() { return backedges.size() > 0; }
	public Object getLogic() { return currLogic; } 
}
