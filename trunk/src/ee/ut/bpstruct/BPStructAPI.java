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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.hpi.bpt.graph.algo.rpst.RPST;
import de.hpi.bpt.graph.algo.tctree.TCType;
import de.hpi.bpt.process.ControlFlow;
import de.hpi.bpt.process.Gateway;
import de.hpi.bpt.process.GatewayType;
import de.hpi.bpt.process.Node;
import de.hpi.bpt.process.Process;
import de.hpi.bpt.process.Task;

/**
 * BPStruct API version 0-1-0
 * 
 * The entry point to the BPStruct functionality 
 */
public class BPStructAPI {
	
	/**
	 * Structure a process
	 * 
	 * @param p Process to structure
	 * @param ac Acyclic configuration
	 * @param cc Cyclic configuration
	 * @return Structuring result
	 * @throws Exception
	 */
	public static BPStructResult structure(Process p, AcyclicConfiguration ac, CyclicConfiguration cc) throws Exception {
		BPStructResult result = new BPStructResult();
		
		File debugdir = new File("bpstruct2");
		if (!debugdir.exists()) debugdir.mkdir();

		Restructurer str = new Restructurer(p);
		
		if (str.perform()) {
			result.proc = str.proc;
			result.hasChanged = true;
			result.isStructured = true;
		}
		else {
			result.proc = p;
			result.hasChanged = false;
			result.isStructured = false;
		}
		
		return result;
	}
	
	/**
	 * Check if a process is already structured.
	 * @param process to check
	 * @return true if process is structured
	 */
	public static boolean checkStructure(Process process) {
		Process copy = null;
		try {
			copy = (Process) process.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		
		List<Node> sources = new ArrayList<Node>();
		List<Node> sinks = new ArrayList<Node>();
		// check if the process has multiple sources or sinks
		for (Node node:copy.getNodes()) {
			if (copy.getIncomingEdges(node).isEmpty())
				sources.add(node);
			if (copy.getOutgoingEdges(node).isEmpty())
				sinks.add(node);
		}
		if (sources.size() > 1) {
			// add a single source and connect it to the former sources
			Task start = new Task("_start_");
			Gateway gate = new Gateway(GatewayType.XOR);
			copy.addEdge(start, gate);
			for (Node node:sources)
				copy.addEdge(gate, node);
		}
		if (sinks.size() > 1) {
			// add a single sink and connect it to the former sinks
			Task end = new Task("_end_");
			Gateway gate = new Gateway(GatewayType.XOR);
			copy.addEdge(gate, end);
			for (Node node:sinks)
				copy.addEdge(node, gate);
		}
		RPST<ControlFlow, Node> rpst = new RPST<ControlFlow, Node>(copy);
		return rpst.getVertices(TCType.R).size() == 0;
	}
}
