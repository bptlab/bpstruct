package ee.ut.bpstruct.bpmn.test.cyclic;

import ee.ut.bpstruct.bpmn.test.StructuringTest;

public class Model8690Test extends StructuringTest {

	public Model8690Test() {
		this.MODEL_NAME = "model8690";
		this.MODEL_PATH_TPL = "models/cyclic/%s.bpmn";
		this.OUTPUT_PATH_TPL = "tmp/cyclic/%s.dot";
		this.CAN_STRUCTURE = true;
	}

}

