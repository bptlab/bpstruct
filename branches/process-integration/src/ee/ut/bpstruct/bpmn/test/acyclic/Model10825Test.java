package ee.ut.bpstruct.bpmn.test.acyclic;

import ee.ut.bpstruct.bpmn.test.StructuringTest;

public class Model10825Test extends StructuringTest {

	public Model10825Test() {
		this.MODEL_NAME = "model10825";
		this.MODEL_PATH_TPL = "models/acyclic/%s.bpmn";
		this.OUTPUT_PATH_TPL = "tmp/acyclic/%s.dot";
		this.CAN_STRUCTURE = true;
	}

}
