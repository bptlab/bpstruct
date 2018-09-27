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

import java.util.BitSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class MDTNode {
	
	public enum NodeType {
		LEAF, COMPLETE, LINEAR, PRIMITIVE
	};

	private NodeType type;
	private Set<MDTNode> children;
	private BitSet value;
	private int proxy;
	private int color;

	public MDTNode(BitSet value, int proxy) {
		type = NodeType.LEAF;
		children = new LinkedHashSet<MDTNode>();
		color = 0;
		this.value = value;
		this.proxy = proxy;
	}

	public int getProxy() { return proxy; }
	
	public void addChild(MDTNode child) {
		children.add(child);
	}

	public void addChildren(Set<MDTNode> children) {
		this.children.addAll(children);
	}
	
	public NodeType getType() {
		return type;
	}

	public void setType(NodeType type) {
		this.type = type;
	}

	public BitSet getValue() {
		return value;
	}

	public void setValue(BitSet value) {
		this.value = value;
	}

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
	}

	public Set<MDTNode> getChildren() {
		return children;
	}

	public String toString() {
		if (type == NodeType.LEAF)
			return value.toString();
		else if (type == NodeType.COMPLETE)
			return type + "_" + color + children;
		else
			return type.toString() + children;
	}
}
