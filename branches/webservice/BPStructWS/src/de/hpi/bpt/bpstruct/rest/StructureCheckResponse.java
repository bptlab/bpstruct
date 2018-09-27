package de.hpi.bpt.bpstruct.rest;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class implements the response of a structure check request to {@link BPStructResource}.
 * @author Christian Wiggert
 *
 */
public class StructureCheckResponse extends GenericResponse {

	private boolean result;
	
	public StructureCheckResponse(StructureRequest req) {
		super(req);
		result = false;
	}

	/**
	 * Set the result of processing the request.
	 * @param res - boolean value representing the result
	 */
	public void setResult(boolean res) {
		result = res;
	}
	
	@Override
	protected JSONObject renderSuccess(JSONObject obj) throws JSONException {
		obj.put("isStructured", result);
		return obj;
	}

}
