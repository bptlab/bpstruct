package de.hpi.bpt.bpstruct.rest;

import org.json.JSONException;
import org.json.JSONObject;

import de.hpi.bpt.process.serialize.Process2DOT;
import de.hpi.bpt.process.serialize.Process2JSON;
import de.hpi.bpt.process.serialize.SerializationException;
import ee.ut.bpstruct2.BPStructResult;

public class StructureResponse extends GenericResponse {
	
	private BPStructResult result;
	
	public StructureResponse(StructureRequest req) {
		super(req);
		result = null;
	}
	/**
	 * Set the result of processing the request.
	 * @param res - a {@link BPStructResult}
	 */
	public void setResult(BPStructResult res) {
		result = res;
	}
	
	@Override
	protected JSONObject renderSuccess(JSONObject obj) throws JSONException {
		try {
			if (result != null) {
				if (result.isStructured()) {
					if (request.returnJSON())
						obj.put("process", new JSONObject(Process2JSON.convert(result.getProcess())));
					if (request.returnDOT()) 
						obj.put("dot", Process2DOT.convert(result.getProcess()));
					obj.put("hasChanged", result.hasChanged());
				} else 
					addError("Process " + request.getProcess().getName() + " could not be structured.");
			}
		} catch (SerializationException e) {
			e.printStackTrace();
			addError(e.getMessage());
		}
		return obj;
	}
}
