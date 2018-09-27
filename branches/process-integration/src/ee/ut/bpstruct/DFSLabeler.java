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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.bpt.hpi.graph.Edge;

public class DFSLabeler {
	private Set<Edge> backedges = new HashSet<Edge>();
	private Set<Edge> forwardedges = new HashSet<Edge>();
	private boolean mixedLogic = false;
	private Object currLogic = null;
	
	public DFSLabeler(RestructurerHelper helper, Map<Integer, List<Integer>> adjList, Integer entry) {
		dfs(helper, adjList, entry);
	}

	enum Color { GRAY, BLACK };
	private void dfs(RestructurerHelper helper, Map<Integer, List<Integer>> adjList, Integer v) {
		dfs(helper, adjList, v, new HashMap<Integer, Color>());
	}

	private void dfs(RestructurerHelper helper, Map<Integer, List<Integer>> adjList, Integer v, Map<Integer, Color> colorMap) {
		colorMap.put(v, Color.GRAY);
		
		Object type = helper.gatewayType(v);
		if (type != null) {
			if (currLogic == null)
				currLogic = type;
			else if (currLogic != type)
				mixedLogic = true;
		}
		
		for (Integer next : adjList.get(v)) {
			if (!colorMap.containsKey(next)) {
				dfs(helper, adjList, next, colorMap);
			}
			else if (colorMap.get(next) == Color.GRAY) {
				backedges.add(new Edge(v, next));
			} else {
				forwardedges.add(new Edge(v, next));
			}
		}
		colorMap.put(v, Color.BLACK);
	}
	
	public boolean isMixedLogic() { return mixedLogic; }
	public boolean isCyclic() { return backedges.size() > 0; }
	public Object getLogic() { return currLogic; } 
}
