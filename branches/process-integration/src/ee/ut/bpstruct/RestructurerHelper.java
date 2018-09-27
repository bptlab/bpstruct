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
package ee.ut.bpstruct;

import java.util.Map;
import java.util.Set;

import de.bpt.hpi.graph.Edge;
import de.bpt.hpi.graph.Graph;
import ee.ut.bpstruct.unfolding.Unfolding;

public interface RestructurerHelper extends Helper {
	Petrifier getPetrifier(Set<Integer> vertices, Set<Edge> edges, Integer entry, Integer exit);
	
	void processOrderingRelations(Set<Edge> edges,
			Set<Integer> vertices, Integer entry, Integer exit, Graph graph,
			Unfolding unf, Map<String, Integer> tasks) throws CannotStructureException;

	void setANDGateway(Integer vertex);
	void setXORGateway(Integer vertex);

	void installStructured();
	
	void materializeDecisions();
	void dematerializeDecisions();
}
