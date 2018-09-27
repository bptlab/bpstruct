package ee.ut.bpstruct.bpmn.test.oulsnam;

import ee.ut.bpstruct.bpmn.test.StructuringTest;

public class ModelBLTest extends StructuringTest {

	public ModelBLTest() {
		this.MODEL_NAME = "BL";
		this.MODEL_PATH_TPL = "models/oulsnam/%s.bpmn";
		this.OUTPUT_PATH_TPL = "tmp/oulsnam/%s.dot";
		this.CAN_STRUCTURE = true;
	}

}

