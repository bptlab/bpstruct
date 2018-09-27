package de.hpi.bpt.bpstruct.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import de.hpi.bpt.process.Process;
import de.hpi.bpt.process.serialize.JSON2Process;
import de.hpi.bpt.process.serialize.SerializationException;

/**
 * This class represents an incoming request to the {@link BPStructResource}.
 * @author Christian Wiggert
 *
 */
public class StructureRequest {
	
	private Process process;
	private boolean returnJSON;
	private boolean returnDOT;
	private List<String> errors;
	
	/**
	 * Parses the request content string and returns the according StructureRequest object. 
	 * @param json - a string containing the JSON representation of the request content
	 * @return a StructureRequest object
	 */
	public static StructureRequest parseRequestString(String json) {
		JSONObject obj;
		try {
			obj = new JSONObject(json);
		} catch (JSONException e) {
			e.printStackTrace();
			obj = new JSONObject();
			try {
				obj.put("errors", Arrays.asList(e.getMessage()));
			} catch (JSONException e1) {}
		}
		return new StructureRequest(obj);
	}
	
	public StructureRequest(JSONObject jsonObj) {
		errors = new ArrayList<String>();
		if (jsonObj.has("errors")) {
			// something went wrong while parsing the JSON
			try {
				for (int i = 0; i < jsonObj.getJSONArray("errors").length(); i++) {
					errors.add(jsonObj.getJSONArray("errors").getString(i));
				}
			} catch(JSONException e) {
				e.printStackTrace();
				errors.add("An error occured while parsing the request.");
			}
			process = null;
			returnJSON = false;
			returnDOT = false;
		} else {
			// retrieve the process and the options
			try {
				if (jsonObj.has("process"))
					process = JSON2Process.convert(jsonObj.getJSONObject("process"));
				else {
					process = null;
					errors.add("No Process was submitted.");
				}
				if (jsonObj.has("options")) {
					JSONObject opts = jsonObj.getJSONObject("options");
					returnJSON = (opts.has("json") && opts.getBoolean("json")) || !opts.has("json");
					returnDOT = (opts.has("dot") && opts.getBoolean("dot"));
				} else {
					returnJSON = true;
					returnDOT = false;
				}
			} catch (SerializationException e) {
				e.printStackTrace();
				errors.add(e.getMessage()); 
			} catch (JSONException e) {
				e.printStackTrace();
				errors.add(e.getMessage());
			}
		}
		
	}
	
	/**
	 * Returns the process contained in the request.
	 * @return
	 */
	public Process getProcess() {
		return process;
	}
	
	/**
	 * Whether or not the service shall return a JSON representation of the result.
	 * @return
	 */
	public boolean returnJSON() {
		return returnJSON;
	}
	
	/**
	 * Whether or not the service shall return a DOT representation of the result.
	 * @return
	 */
	public boolean returnDOT() {
		return returnDOT;
	}
	
	/**
	 * Indicates whether there occured an error while parsing the request.
	 * @return true if an error occured
	 */
	public boolean corrupted() {
		return !errors.isEmpty();
	}
	
	/**
	 * Returns the error messages of all occured errors.
	 * @return
	 */
	public List<String> getErrors() {
		return errors;
	}

}
