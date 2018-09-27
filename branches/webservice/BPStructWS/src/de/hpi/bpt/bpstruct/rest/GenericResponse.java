package de.hpi.bpt.bpstruct.rest;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class represents an abstract response for the {@link BPStructResource} service.
 * @author Christian Wiggert
 *
 */
public abstract class GenericResponse {
	
	protected List<String> errors;
	protected StructureRequest request;
	
	public GenericResponse(StructureRequest req) {
		errors = new ArrayList<String>();
		request = req;
	}
	
	/**
	 * Add an error message
	 * @param error - string representing the error message
	 */
	public void addError(String error) {
		errors.add(error);
	}
	
	/**
	 * Add multiple error messages.
	 * @param errs - list containing multiple error strings
	 */
	public void addErrors(List<String> errs) {
		errors.addAll(errs);
	}
	
	/**
	 * Wraps the error messages into an according JSON representation.
	 * @return
	 */
	protected String createErrorContent() {
		JSONObject content = new JSONObject();
		try {
			content.put("errors", errors);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return content.toString();
	}
	
	/**
	 * Indicates whether any error occured while processing the request.
	 * @return true if an error occured
	 */
	public boolean failed() {
		return !errors.isEmpty();
	}
	
	/**
	 * Renders the response for the request.
	 * @return
	 */
	public String renderResponse() {
		JSONObject obj = new JSONObject();
		if (request.corrupted())
			addErrors(request.getErrors());
		else {
			try {
				renderSuccess(obj);
			} catch (JSONException e) {
				e.printStackTrace();
				addError(e.getMessage());
			}
		}
		if (!failed())
			return obj.toString();
		else
			return createErrorContent();
	}
	
	/**
	 * Renders just the success case (is used by renderResponse).
	 * @param obj
	 * @return
	 * @throws JSONException
	 */
	protected abstract JSONObject renderSuccess(JSONObject obj) throws JSONException;
}
