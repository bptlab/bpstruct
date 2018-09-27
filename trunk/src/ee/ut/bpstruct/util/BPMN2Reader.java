package ee.ut.bpstruct.util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

import de.hpi.bpt.process.ControlFlow;
import de.hpi.bpt.process.Gateway;
import de.hpi.bpt.process.GatewayType;
import de.hpi.bpt.process.Node;
import de.hpi.bpt.process.Process;
import de.hpi.bpt.process.Task;

public class BPMN2Reader {

	public static Process parse(File file) throws JDOMException, IOException {
		Namespace BPMN2NS = Namespace.getNamespace("http://schema.omg.org/spec/BPMN/2.0");
		Document doc = new SAXBuilder().build(file);

		Process proc = new Process();
		Element procElem = doc.getRootElement().getChild("process", BPMN2NS);
		if (procElem == null) {
			BPMN2NS = Namespace.getNamespace("http://www.omg.org/spec/BPMN/20100524/MODEL");
			procElem = doc.getRootElement().getChild("process", BPMN2NS);
		}

		initProcess(proc, procElem, BPMN2NS);
		return proc;
	}

	protected static void initProcess(Process proc, Element procElem, Namespace BPMN2NS) {
		Map<String, Node> nodes = new HashMap<String, Node>();
		List<Element> edges = new LinkedList<Element>();
		for (Object obj : procElem.getChildren())
			if (obj instanceof Element) {
				Element elem = (Element) obj;
				String id = elem.getAttributeValue("id");
				if (id == null || id.isEmpty())
					System.out.println("oops");
				String name = elem.getAttributeValue("name");
				if (elem.getName().equals("task") || elem.getName().equals("startEvent") || elem.getName().equals("endEvent")) {					
					Task task = new Task(name);
//					proc.addTask(task);
					task.setId(id);
					nodes.put(task.getId(), task);
				} else if (elem.getName().equals("exclusiveGateway")) {
					Gateway gateway = new Gateway(GatewayType.XOR, name);
//					proc.addGateway(gateway);
					gateway.setId(id);
					nodes.put(id, gateway);
				} else if (elem.getName().equals("parallelGateway")) {
					Gateway gateway = new Gateway(GatewayType.AND, name);
//					proc.addGateway(gateway);
					gateway.setId(id);
					nodes.put(id, gateway);
				} else if (elem.getName().equals("inclusiveGateway")) {
					Gateway gateway = new Gateway(GatewayType.OR, name);
//					proc.addGateway(gateway);
					gateway.setId(id);
					nodes.put(id, gateway);
				} else if (elem.getName().equals("sequenceFlow"))
					edges.add(elem);
			}

		for (Element edge : edges) {
			Node src = nodes.get(edge.getAttributeValue("sourceRef"));
			Node tgt = nodes.get(edge.getAttributeValue("targetRef"));
			if (src != null && tgt != null) {
				ControlFlow flow = proc.addControlFlow(src, tgt);
				
				// TODO: Check with Artem the following:
				// It seems that when the process has multiple edges with the same source/target nodes, proc.addControlFlow(src, tgt) will return null
				if (flow != null) {
					String label = null;
					Element expr = edge.getChild("conditionExpression", BPMN2NS);
					if (expr != null)
						label = expr.getText();
					else
						label = "";
					flow.setLabel(label);
				}
			} else {
				throw new RuntimeException("Malformed graph");
			}
		}
	}

}
