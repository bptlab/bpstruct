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

/**
 * Configuration of acyclic structuring
 * 
 * The configuration is applied per unstructured acyclic fragment 
 *
 * FULL - replace only if fragment can be fully structured; otherwise keep as is
 * MAXI - replace only if fragment can be fully or maximally structured; otherwise keep as is
 * BEST - always replace the fragment with structured, maximally-structured (even for inherently unstructured fragments)
 */
public enum AcyclicConfiguration {
	FULL,
	MAXI,
	BEST
}
