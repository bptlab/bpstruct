/* 
 * Copyright (C) 2011 - Luciano Garcia Banuelos
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
package ee.ut.bpstruct.eventstruct;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class Tuple {
	Set<Integer> first;
	Set<Integer> second;
	Set<Integer> third;
	Integer rep;
	
	public Tuple(Set<Integer> f, Set<Integer> s, Set<Integer> t, Integer r) { first = f; second = s; third = t; rep = r;}
	public Set<Integer> getFirst() { return first; }
	public Set<Integer> getSecond() { return second; }
	public String toString() {
		return String.format("(%s,%s,%s)", first, second, third);
	}
	public boolean equals(Object obj) {
		if (obj == null) return false;
        if (!(obj instanceof Tuple)) return false;
        Tuple that = (Tuple) obj;
        
        return this.first.equals(that.first) && this.second.equals(that.second);
	}
	public int hashCode() {
		return (first == null ? 0 : first.hashCode()) + (second == null ? 0 : second.hashCode() * 37);
	}
	
	public Object clone() {
		return new Tuple(new LinkedHashSet<Integer>(first), new HashSet<Integer>(second),
				new HashSet<Integer>(third), rep);
	}
}
