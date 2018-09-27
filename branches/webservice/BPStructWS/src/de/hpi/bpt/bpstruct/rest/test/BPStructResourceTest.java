package de.hpi.bpt.bpstruct.rest.test;

import junit.framework.TestCase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.hpi.bpt.bpstruct.rest.BPStructResource;
import de.hpi.bpt.process.Gateway;
import de.hpi.bpt.process.GatewayType;
import de.hpi.bpt.process.Process;
import de.hpi.bpt.process.Task;
import de.hpi.bpt.process.serialize.Process2JSON;
import de.hpi.bpt.process.serialize.SerializationException;

public class BPStructResourceTest extends TestCase {
	
	public void testStructureFullEmptyProcess() {
		BPStructResource bpsr = new BPStructResource();
		Process process = new Process("foo");
		try {
			String result = bpsr.structureMax(String.format("{process: %s}", Process2JSON.convert(process)));
			assertFalse(result.equals(""));
			JSONObject resObj = new JSONObject(result);
			assertTrue(resObj.has("errors"));
			JSONArray errors = resObj.getJSONArray("errors");
			assertEquals(3, errors.length());
			assertEquals("Process " + process.getName() + " contains no task", errors.get(0));
			assertEquals("Process " + process.getName() + " has no source task.", errors.get(1));
			assertEquals("Process " + process.getName() + " has no sink task.", errors.get(2));
		} catch (SerializationException e) {
			e.printStackTrace();
			fail("There shouldn't be a SerializationException.");
		} catch (JSONException e) {
			e.printStackTrace();
			fail("The returned JSON string is messed up.");
		}
	}
	
	public void testStructureFullUnconnectedTasks() {
		BPStructResource bpsr = new BPStructResource();
		Process process = new Process("foo");
		Task t1 = new Task("t1");
		Task t2 = new Task("t2");
		Gateway g1 = new Gateway(GatewayType.AND);
		t1.setId("" + t2.getId());
		process.addTask(t1);
		process.addTask(t2);
		process.addTask(new Task("t3"));
		process.addGateway(g1);
		try {
			String result = bpsr.structureMax(String.format("{process: %s}", Process2JSON.convert(process)));
			assertFalse(result.equals(""));
			JSONObject resObj = new JSONObject(result);
			assertTrue(resObj.has("errors"));
			JSONArray errors = resObj.getJSONArray("errors");
			assertEquals(3, errors.length());
			//assertEquals("The ID " + t1.getId() + " occurs multiple times.", errors.get(0));
			assertEquals("Gateway " + g1.getId() + " has no incoming flow.", errors.get(0));
			assertEquals("Gateway " + g1.getId() + " has no outgoing flow.", errors.get(1));
			assertEquals("Gateway " + g1.getId() + " has less than three flows.", errors.get(2));
		} catch (SerializationException e) {
			e.printStackTrace();
			fail("There shouldn't be a SerializationException.");
		} catch (JSONException e) {
			e.printStackTrace();
			fail("The returned JSON string is messed up.");
		}
	}
	
	public void testStructureFullUnstructuredOrGateway() {
		BPStructResource bpsr = new BPStructResource();
		Process process = new Process("foo");
		Task i = new Task("i");
		Task o = new Task("o");
		Gateway or = new Gateway(GatewayType.OR);
		Gateway and1 = new Gateway(GatewayType.AND);
		Gateway and2 = new Gateway(GatewayType.AND);
		Gateway and3 = new Gateway(GatewayType.AND);
		process.addControlFlow(i, or);
		process.addControlFlow(or, and1);
		process.addControlFlow(or, and2);
		process.addControlFlow(and1, and2);
		process.addControlFlow(and1, and3);
		process.addControlFlow(and2, and3);
		process.addControlFlow(and3, o);
		try {
			String result = bpsr.structureMax(String.format("{process: %s}", Process2JSON.convert(process)));
			assertFalse(result.equals(""));
			JSONObject resObj = new JSONObject(result);
			assertTrue(resObj.has("errors"));
			JSONArray errors = resObj.getJSONArray("errors");
			assertEquals(1, errors.length());
			assertEquals("Gateway " + or.getId() + " is an unstructured OR-Gateway.", errors.get(0));
		} catch (SerializationException e) {
			e.printStackTrace();
			fail("There shouldn't be a SerializationException.");
		} catch (JSONException e) {
			e.printStackTrace();
			fail("The returned JSON string is messed up.");
		}
	}
	
	public void testCheckStructure() {
		BPStructResource bpsr = new BPStructResource();
		Process process = new Process("foo");
		Task t1 = new Task("t1");
		Task t2 = new Task("t2");
		Task t3 = new Task("t3");
		Gateway g1 = new Gateway(GatewayType.AND);
		process.addControlFlow(t1, g1);
		process.addControlFlow(g1, t2);
		process.addControlFlow(g1, t3);
		try {
			String result = bpsr.checkStructure(String.format("{process: %s}", Process2JSON.convert(process)));
			assertFalse(result.equals(""));
			JSONObject resObj = new JSONObject(result);
			assertTrue(resObj.has("isStructured"));
			assertTrue(resObj.getBoolean("isStructured"));
		} catch (SerializationException e) {
			e.printStackTrace();
			fail("There shouldn't be a SerializationException.");
		} catch (JSONException e) {
			e.printStackTrace();
			fail("The returned JSON string is messed up.");
		}
	}
	
	public void testCheckStructure2() {
		BPStructResource bpsr = new BPStructResource();
		// first example from http://code.google.com/p/bpstruct/
		Process process = new Process("foo");
		Task i = new Task("i");
		Task a = new Task("a");
		Task b = new Task("b");
		Task c = new Task("c");
		Task d = new Task("d");
		Task e = new Task("e");
		Task f = new Task("f");
		Task o = new Task("o");
		Gateway t = new Gateway(GatewayType.XOR);
		Gateway u = new Gateway(GatewayType.AND);
		Gateway v = new Gateway(GatewayType.AND);
		Gateway w = new Gateway(GatewayType.XOR);
		Gateway x = new Gateway(GatewayType.XOR);
		Gateway y = new Gateway(GatewayType.AND);
		Gateway z = new Gateway(GatewayType.XOR);
		process.addControlFlow(i, t);
		process.addControlFlow(t, a);
		process.addControlFlow(t, b);
		process.addControlFlow(t, e);
		process.addControlFlow(a, u);
		process.addControlFlow(b, v);
		process.addControlFlow(e, f);
		process.addControlFlow(u, w);
		process.addControlFlow(u, x);
		process.addControlFlow(v, w);
		process.addControlFlow(v, x);
		process.addControlFlow(w, c);
		process.addControlFlow(x, d);
		process.addControlFlow(c, y);
		process.addControlFlow(d, y);
		process.addControlFlow(y, z);
		process.addControlFlow(f, z);
		process.addControlFlow(z, o);
		try {
			String result = bpsr.checkStructure(String.format("{process: %s}", Process2JSON.convert(process)));
			assertFalse(result.equals(""));
			JSONObject resObj = new JSONObject(result);
			assertTrue(resObj.has("isStructured"));
			assertFalse(resObj.getBoolean("isStructured"));
		} catch (SerializationException e1) {
			e1.printStackTrace();
			fail("There shouldn't be a SerializationException.");
		} catch (JSONException e1) {
			e1.printStackTrace();
			fail("The returned JSON string is messed up.");
		}
	}

	public void testMalformedJSON() {
		BPStructResource bpsr = new BPStructResource();
		String result = bpsr.structureMax("nonsense string");
		try {
			JSONObject resObj = new JSONObject(result);
			assertTrue(resObj.has("errors"));
			assertEquals(1, resObj.getJSONArray("errors").length());
		} catch (JSONException e) {
			e.printStackTrace();
			fail("The returned JSON string is messed up.");
		}
	}
	
	public void testMissingProperty() {
		BPStructResource bpsr = new BPStructResource();
		String result = bpsr.structureMax("{process: {tasks:[], gateways: [], flows: []}}");
		try {
			JSONObject resObj = new JSONObject(result);
			assertTrue(resObj.has("errors"));
			assertEquals(1, resObj.getJSONArray("errors").length());
			assertEquals("JSONObject[\"name\"] not found.", resObj.getJSONArray("errors").get(0));
		} catch (JSONException e) {
			e.printStackTrace();
			fail("The returned JSON string is messed up.");
		}
	}
	
	public void testWrongPropertyType() {
		BPStructResource bpsr = new BPStructResource();
		String result = bpsr.structureMax("{process: {name: \"foo\", tasks: 0, gateways: [], flows: []}}");
		try {
			JSONObject resObj = new JSONObject(result);
			assertTrue(resObj.has("errors"));
			assertEquals(1, resObj.getJSONArray("errors").length());
			assertEquals("JSONObject[\"tasks\"] is not a JSONArray.", resObj.getJSONArray("errors").get(0));
		} catch (JSONException e) {
			e.printStackTrace();
			fail("The returned JSON string is messed up.");
		}
	}
	
	public void testUnknownNode() {
		BPStructResource bpsr = new BPStructResource();
		String result = bpsr.structureMax("{process: {name: \"foo\", tasks: [], gateways: [], flows: [{src:'bar', tgt:'baz', label: 'gotYa'}]}}");
		try {
			JSONObject resObj = new JSONObject(result);
			assertTrue(resObj.has("errors"));
			assertEquals(1, resObj.getJSONArray("errors").length());
			assertEquals("Unknown node bar was referenced by a flow as 'src'.", resObj.getJSONArray("errors").get(0));
		} catch (JSONException e) {
			e.printStackTrace();
			fail("The returned JSON string is messed up.");
		}
	}
	
	public void testNullLabel() {
		BPStructResource bpsr = new BPStructResource();
		String result = bpsr.structureMax("{process: {name: 'foo', tasks: [{id: 'bar', label: 'A'}, {id: 'baz', label: 'B'}], gateways: [], flows: [{src:'bar', tgt:'baz', label: null}]}}");
		try {
			JSONObject resObj = new JSONObject(result);
			assertFalse(resObj.has("errors"));
		} catch (JSONException e) {
			e.printStackTrace();
			fail("The returned JSON string is messed up.");
		}
	}
	
	public void testWrongGatewayType() {
		BPStructResource bpsr = new BPStructResource();
		String result = bpsr.structureMax("{process: {name: 'foo', tasks: [], gateways: [{id: 'bar', type: 'FUZZY'}], flows: []}}");
		try {
			JSONObject resObj = new JSONObject(result);
			assertTrue(resObj.has("errors"));
			assertEquals(1, resObj.getJSONArray("errors").length());
			assertEquals("Couldn't determine GatewayType.", resObj.getJSONArray("errors").get(0));
		} catch (JSONException e) {
			e.printStackTrace();
			fail("The returned JSON string is messed up.");
		}
	}
}
