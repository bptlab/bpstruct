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

import java.util.Set;

public	class ReproductionProcess {
	Set<DNode> brConds;
	Set<DNode> events;
	Set<DNode> conditions;
	
	public ReproductionProcess(Set<DNode> events, Set<DNode> conds, Set<DNode> brConds) {
		this.events = events;
		this.conditions = conds;
		this.brConds = brConds;
	}
	public Set<DNode> getBranchingConditions() {
		return brConds;
	}
}
