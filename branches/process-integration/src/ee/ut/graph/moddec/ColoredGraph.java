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
package ee.ut.graph.moddec;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * Efficient implementation of a simple graph: (Vertices, Edges, labels)
 * Only for reading, cannot be modified
 */
public class ColoredGraph {
	public Set<Integer> vertices;

	private Map<Integer,Set<Integer>> outgoingEdges;
	private Map<Integer,Set<Integer>> incomingEdges;
	public Map<Integer,String> labels;
	public Map<String, Integer> inverse;
	private int vertexId = 0;
	
	private ColoredGraph(Set<Integer> vertices, Map<Integer,Set<Integer>> outgoingEdges, Map<Integer,Set<Integer>> incomingEdges, Map<Integer,String> labels, Map<String, Integer> inverse){
		this.vertices = vertices;
		this.outgoingEdges = outgoingEdges;
		this.incomingEdges = incomingEdges;
		this.labels = labels;
		this.inverse = inverse;
	}
	
	public ColoredGraph() {
		vertices = new HashSet<Integer>();
		outgoingEdges = new HashMap<Integer, Set<Integer>>();
		incomingEdges = new HashMap<Integer, Set<Integer>>();
		labels = new HashMap<Integer, String>();
		inverse = new HashMap<String, Integer>();
	}
	
	public void addVertex(String label) {
		Integer v = vertexId++;
		vertices.add(v);
		labels.put(v, label);
		inverse.put(label, v);
		incomingEdges.put(v, new HashSet<Integer>());
		outgoingEdges.put(v, new HashSet<Integer>());
	}
		
	public void addEdge(String v1, String v2) {
		Integer _v1 = inverse.get(v1);
		Integer _v2 = inverse.get(v2);
		outgoingEdges.get(_v1).add(_v2);
		incomingEdges.get(_v2).add(_v1);
	}
	
	public Set<Integer> getVertices() {
		return vertices;
	}
	
	public Set<Integer> postSet(int vertex) {
		return outgoingEdges.get(vertex);
	}

	public Set<Integer> preSet(int vertex) {
		return incomingEdges.get(vertex);
	}

	public LinkedList<String> getLabels(){
		return new LinkedList<String>(labels.values());
	}
	
	public String getLabel(int vertex) {
		return labels.get(vertex);
	}
	public Set<String> getLabels(Set<Integer> nodes){
		Set<String> result = new HashSet<String>();
		
		for (Integer node: nodes){
			result.add(getLabel(node));
		}
		
		return result;
	}
	public Integer getVertex(String label){
		for (Integer v: vertices){
			if (labels.get(v).equals(label)){
				return v;
			}
		}
		return Integer.MAX_VALUE;
	}
	
	/**
	 * @return vertices that do not have an incoming edge.
	 */
	public Set<Integer> sourceVertices(){
		Set<Integer> result = new HashSet<Integer>();
		for (Integer i: vertices){
			if (incomingEdges.get(i).isEmpty()){
				result.add(i);
			}
		}
		return result;
	}
	
	/**
	 * @return vertices that do not have an outgoing edge.
	 */	
	public Set<Integer> sinkVertices(){
		Set<Integer> result = new HashSet<Integer>();
		for (Integer i: vertices){
			if (outgoingEdges.get(i).isEmpty()){
				result.add(i);
			}
		}
		return result;
	}
	
	public String toString(){
		String result = "";
		for (Integer i: vertices){
			result += i + "(" + labels.get(i) + ") {";
			for (Iterator<Integer> j = incomingEdges.get(i).iterator(); j.hasNext();){
				int vertex = j.next();
				result += vertex;// + "(" + labels.get(vertex) + ")";
				result += j.hasNext()?",":"";
			}
			result += "} {";
			for (Iterator<Integer> j = outgoingEdges.get(i).iterator(); j.hasNext();){
				int vertex = j.next();
				result += vertex;// + "(" + labels.get(vertex) + ")";
				result += j.hasNext()?",":"";
			}
			result += "}\n";
		}
		return result;
	}
	
	/**
	 * @param vertex Vertex to determine the postSet for
	 * @param silent Set of vertices that should not be considered
	 * @return the postSet(vertex), in which all v \in silent are (recursively) replaced by their postSet(v)
	 */
	public Set<Integer> nonSilentPostSet(Integer vertex, Set<Integer> silent){
		return nonSilentPostSetHelper(vertex, silent, new HashSet<Integer>()); 
	}
	private Set<Integer> nonSilentPostSetHelper(Integer vertex, Set<Integer> silent, Set<Integer> visited){
		Set<Integer> result = new HashSet<Integer>();
		Set<Integer> visitedP = new HashSet<Integer>(visited);
		visitedP.add(vertex);
		
		for (Integer post: postSet(vertex)){
			if (!visited.contains(post)){
				if (silent.contains(post)){
					result.addAll(nonSilentPostSetHelper(post,silent,visitedP));
				}else{
					result.add(post);
				}
			}
		}
		return result;
	}
	
	/**
	 * @param vertex Vertex to determine the preSet for
	 * @param silent Set of vertices that should not be considered
	 * @return the preSet(vertex), in which all v \in silent are (recursively) replaced by their preSet(v)
	 */
	public Set<Integer> nonSilentPreSet(Integer vertex, Set<Integer> silent){
		return nonSilentPreSetHelper(vertex, silent, new HashSet<Integer>()); 
	}
	private Set<Integer> nonSilentPreSetHelper(Integer vertex, Set<Integer> silent, Set<Integer> visited){
		Set<Integer> result = new HashSet<Integer>();
		Set<Integer> visitedP = new HashSet<Integer>(visited);
		visitedP.add(vertex);
		
		for (Integer pre: preSet(vertex)){
			if (!visited.contains(pre)){
				if (silent.contains(pre)){
					result.addAll(nonSilentPreSetHelper(pre,silent,visitedP));
				}else{
					result.add(pre);
				}
			}
		}
		return result;
	}
	
	/**
	 * Returns A COPY OF the graph, such that all vertices from the given set are removed.
	 * All paths (v1,v),(v,v2) via a vertex v from that set are replaced by direct arcs (v1,v2). 
	 * 
	 * Formally: for G = (V, E, l)
	 * return (V-vertices, E', l-(vertices x labels)), where
	 * E' = E - ((V x vertices) U (vertices X V))
	 *    U {(v1, v2)|v \in vertices, (v1,v) \in E \land (v,v2) \in E}    
	 */
	public ColoredGraph removeVertices(Set<Integer> toRemove){
		Set<Integer> newVertices = new HashSet<Integer>(vertices);
		newVertices.removeAll(toRemove);

		Map<Integer,Set<Integer>> newOutgoingEdges = new HashMap<Integer,Set<Integer>>();
		Map<Integer,Set<Integer>> newIncomingEdges = new HashMap<Integer,Set<Integer>>();;
		Map<Integer,String> newLabels = new HashMap<Integer,String>();
		Map<String, Integer> newInverse = new HashMap<String, Integer>();
		
		for (Integer newVertex: newVertices){
			newOutgoingEdges.put(newVertex, nonSilentPostSet(newVertex,toRemove));
			newIncomingEdges.put(newVertex, nonSilentPreSet(newVertex,toRemove));
			newLabels.put(newVertex, labels.get(newVertex));
			newInverse.put(labels.get(newVertex), newVertex);
			
		}
		
		return new ColoredGraph(newVertices, newOutgoingEdges, newIncomingEdges, newLabels, newInverse);
	}
	
	/**
	 * Given subset of vertices of this graph, the method builds the corresponding subgraph.
	 * 
	 * @param _vertices Set of vertices in the subgraph
	 * @return The subgraph
	 */
	public ColoredGraph subgraph(Set<Integer> _vertices) {
		Set<Integer> newVertices = new HashSet<Integer>(vertices);
		newVertices.retainAll(_vertices);

		Map<Integer,Set<Integer>> newOutgoingEdges = new HashMap<Integer,Set<Integer>>();
		Map<Integer,Set<Integer>> newIncomingEdges = new HashMap<Integer,Set<Integer>>();;
		Map<Integer,String> newLabels = new HashMap<Integer,String>();
		Map<String, Integer> newInverse = new HashMap<String, Integer>();

		for (Integer newVertex: newVertices) {
			HashSet<Integer> vertexSet = new HashSet<Integer>();
			for (Integer source: preSet(newVertex)) {
				if (newVertex == source) continue;
				if (newVertices.contains(source))
					vertexSet.add(source);
			}
			newIncomingEdges.put(newVertex, vertexSet);
			
			vertexSet = new HashSet<Integer>();
			for (Integer target: postSet(newVertex)) {
				if (newVertex == target) continue;
				if (newVertices.contains(target))
					vertexSet.add(target);
			}
			newOutgoingEdges.put(newVertex, vertexSet);
			
			newLabels.put(newVertex, labels.get(newVertex));
			newInverse.put(labels.get(newVertex), newVertex);
		}
		
		return new ColoredGraph(newVertices, newOutgoingEdges, newIncomingEdges, newLabels, newInverse);
	}
	
	public boolean hasEdge(int s, int t) {
		return outgoingEdges.get(s).contains(t);
	}
	
	public boolean distinguishes(int x, int y, int z) {
		return (hasEdge(x, y) != hasEdge(x, z)) || (hasEdge(y, x) != hasEdge(z, x));
	}

	public String createLabel(Set<Integer> vertices) {
		boolean firsttime = true;
		StringBuffer buff = new StringBuffer();
		for (Integer v : vertices) {
			if (firsttime)
				firsttime = false;
			else
				buff.append(".");
			buff.append(getLabel(v));
		}
		return buff.toString();
	}
	
	public ColoredGraph reverseColor() {
		ColoredGraph complement = new ColoredGraph();
		complement.vertices = new HashSet<Integer>(vertices);
		complement.labels.putAll(labels);
		complement.inverse.putAll(inverse);
		
		for (Integer s: vertices) {
			if (!complement.outgoingEdges.containsKey(s)) complement.outgoingEdges.put(s, new HashSet<Integer>());
			for (Integer t: vertices) {
				if (!complement.incomingEdges.containsKey(t)) complement.incomingEdges.put(t, new HashSet<Integer>());
				if (s.equals(t)) continue;
				if (hasEdge(s, t) && !hasEdge(t, s))
					complement.addEdge(labels.get(s), labels.get(t));
				else if (!hasEdge(s, t) && !hasEdge(t, s))
					complement.addEdge(labels.get(s), labels.get(t));
			}
		}
		
		return complement;
	}
	
	public boolean isTransitive() {
		Map<Integer, Integer> indexMap = new HashMap<Integer, Integer>();
		int index = 0;
		for (Integer v: vertices)
			indexMap.put(v, index++);
		
		int[][] adjMatrix = new int[index][index];
		int[][] adjMatrixp = new int[index][index];
		
		for (Integer s: outgoingEdges.keySet())
			for (Integer t: outgoingEdges.get(s))
				if (s != t)  // avoid self loops
					adjMatrix[indexMap.get(s)][indexMap.get(t)] = adjMatrixp[indexMap.get(s)][indexMap.get(t)] = 1;
		
		transitiveClosure(adjMatrixp);
		
		for (int i = 0; i < index; i++)
			for (int j = 0; j < index; j++) {
				if (i == j) continue;
				if (adjMatrix[i][j] != adjMatrixp[i][j])
					return false;	
			}
		return true;
	}
	
	// Transitive closure using Floyd-Warshall's Algorithm
	private void transitiveClosure(int [][]adj) {
		for (int k = 0; k < adj.length; k++)
			for (int i = 0; i < adj.length; i++)
				for (int j = 0; j < adj.length; j++)
					adj[i][j] = adj[i][j] | (adj[i][k] & adj[k][j]);
	}

	public void toDot(PrintStream out) {
		out.println("digraph G {");
		for (Integer source: getVertices())
			for (Integer target: postSet(source))
				out.printf("\t%s -> %s;\n", getLabel(source), getLabel(target));
		out.println("}");
	}

	public Object toDot() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		toDot(new PrintStream(out));
		return out.toString();
	}
}
