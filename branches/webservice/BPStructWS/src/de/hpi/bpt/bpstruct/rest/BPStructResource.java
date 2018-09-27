package de.hpi.bpt.bpstruct.rest;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import de.hpi.bpt.process.Gateway;
import de.hpi.bpt.process.checks.structural.ProcessStructureChecker;
import de.hpi.bpt.process.petri.PetriNet;
import ee.ut.bpstruct2.BPStructAPI;

@Path("/v1")
public class BPStructResource {
	
	/**
	 * Structures a process, which is submitted as JSON formatted String.
	 * @param content - process in JSON format
	 * @return structured process, if possible, in JSON format
	 */
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/structure/max")
	public String structureMax(String content) {
		StructureRequest req = StructureRequest.parseRequestString(content);
		StructureResponse res = new StructureResponse(req);
		if (!req.corrupted()) {
			res.addErrors(ProcessStructureChecker.checkStructure(req.getProcess()));
			if (!res.failed()) {
				PetriNet net = null;
				boolean isSound = true;
				/*try {
					net = Process2PetriNet.convert(req.getProcess());
					net.setInitialMarking();
					isSound = LolaSoundnessChecker.isSound(net);
				} catch (TransformationException e) {
					e.printStackTrace();
					res.addError(e.getMessage());
				} catch (IOException e) {
					e.printStackTrace();
					res.addError("Cannot check soundness.");
				} catch (SerializationException e) {
					e.printStackTrace();
					res.addError(e.getMessage());
				}*/
				if (isSound) {
					// Process holds structural constraints
					// and is sound
					// try to structure it
					int count = 0;
					for (Gateway gw: req.getProcess().getGateways())
						if (gw.getName().isEmpty())
							gw.setName("gw"+count++);
					try {
						res.setResult(BPStructAPI.structure(req.getProcess(), null, null));	
					} catch (Exception e) {
						e.printStackTrace();
						res.addError("An error occured while structuring the process.");
					}
				} else
					res.addError("Process " + req.getProcess().getName() + " is not sound.");
			}	
		}
		
		//if (res == null)
		//	result = createErrorContent(Arrays.asList("Something mysterious happened and caused this error."));
		return res.renderResponse();
	}
	
	/**
	 * Runs a structure check and returns the result.
	 * @param content - process to check in JSON format
	 * @return 
	 */
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/check/structure")
	public String checkStructure(String content) {
		StructureRequest req = StructureRequest.parseRequestString(content);
		StructureCheckResponse res = new StructureCheckResponse(req);
		if (req.getProcess() != null)
			res.setResult(BPStructAPI.checkStructure(req.getProcess()));
		return res.renderResponse();
	}

}
