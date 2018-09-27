/* 
 * Copyright (C) 2010 - Artem Polyvyanyy, Luciano Garcia Banuelos
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ee.ut.bpstruct.bpmn.test;

import java.io.File;
import java.io.PrintStream;

import junit.framework.TestCase;

import org.apache.log4j.PropertyConfigurator;

import ee.ut.bpstruct.Restructurer;
import ee.ut.bpstruct.RestructurerVisitor;
import ee.ut.bpstruct.bpmn.BPMN2Helper;

/**
 * Abstract structuring test
 */
public abstract class StructuringTest extends TestCase {
	protected String MODEL_NAME = "";
	protected String MODEL_PATH_TPL = "";
	protected String OUTPUT_PATH_TPL = "";
	protected boolean CAN_STRUCTURE = true;
	
	public void testStructuring() throws Exception {
		PropertyConfigurator.configure("log4j.properties");
		BPMN2Helper helper = new BPMN2Helper(this.MODEL_PATH_TPL, this.MODEL_NAME);
		File debugdir = new File("debug");
		if (!debugdir.exists()) debugdir.mkdir();
		helper.setDebugDir(debugdir);
		Restructurer r = new Restructurer(helper, new RestructurerVisitor(helper));

		assertTrue(this.CAN_STRUCTURE == r.process());
		if (this.CAN_STRUCTURE)
			r.serializeDot(new PrintStream(String.format(this.OUTPUT_PATH_TPL, this.MODEL_NAME)));
	}
}
