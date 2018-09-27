package ee.ut.bpstruct.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintStream;

import junit.framework.TestCase;
import de.hpi.bpt.process.Process;
import de.hpi.bpt.process.serialize.JSON2Process;
import de.hpi.bpt.process.serialize.Process2DOT;
import ee.ut.bpstruct.Restructurer;

/**
 * Abstract structuring test
 * 
 * TODO this must be finalized
 */
public abstract class StructuringTest extends TestCase {
	protected boolean isWellStructured = false;
	protected boolean isMaxStructured = false;
	protected boolean canBeStructured = false;
	
	protected String MODEL_NAME = "";
	protected String MODEL_PATH_TPL = "models/unstruct/%s.json";
	protected String OUTPUT_PATH_TPL = "models/struct/%s.dot";
	
	
	public void testStructuring() throws Exception {
		// READ PROCESS MODEL FROM FILE
		File file = new File(MODEL_PATH_TPL + MODEL_NAME + ".json");
		String line;
		BufferedReader in = new BufferedReader(new FileReader(file));
		StringBuilder strb = new StringBuilder();
		while ((line = in.readLine()) != null)
			strb.append(line);
		
		Process PM = JSON2Process.convert(strb.toString());

		Restructurer str = new Restructurer(PM);
		
		if (str.perform())
			try {
				String filename = String.format(this.OUTPUT_PATH_TPL, this.MODEL_NAME);
				PrintStream out = new PrintStream(filename);
				out.print(Process2DOT.convert(str.proc));
				out.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		else
			System.out.println("Model cannot be restructured");

	}
}
