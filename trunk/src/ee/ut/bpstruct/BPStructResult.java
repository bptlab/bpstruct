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
package ee.ut.bpstruct;

import de.hpi.bpt.process.Process;

/**
 * Class to store structuring result
 */
public class BPStructResult {
	protected Process proc = null;
	protected boolean isStructured = false;
	protected boolean hasChanged = false;
	
	public Process getProcess() {
		return this.proc;
	}
	
	public boolean isStructured() {
		return this.isStructured;
	}
	
	public boolean hasChanged() {
		return this.hasChanged;
	}
}
