package ee.ut.bpstruct.bpmn.test.cyclic;

import ee.ut.bpstruct.bpmn.test.StructuringTest;

public class Model9110Test extends StructuringTest {

	public Model9110Test() {
		this.MODEL_NAME = "model9110";
		this.MODEL_PATH_TPL = "models/cyclic/%s.bpmn";
		this.OUTPUT_PATH_TPL = "tmp/cyclic/%s.dot";
		this.CAN_STRUCTURE = true;
	}

}

