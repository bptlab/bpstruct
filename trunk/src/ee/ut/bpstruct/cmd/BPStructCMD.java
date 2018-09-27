package ee.ut.bpstruct.cmd;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import de.hpi.bpt.process.Gateway;
import de.hpi.bpt.process.Process;
import de.hpi.bpt.process.serialize.JSON2Process;
import de.hpi.bpt.process.serialize.Process2DOT;
import de.hpi.bpt.process.serialize.Process2JSON;
import ee.ut.bpstruct.Restructurer;

/**
 * BPStruct Command Line Tool
 * 
 * @author Luciano García-Bañuelos and Artem Polyvyanyy
 * 
 * Usage:
 *    java -jar bpstruct.jar [options] <inputmodel>
 * Options:
 *    -dot       : Generate DOT file
 *    -odir FILE : Output directory
 */
public class BPStructCMD {

	public static void main(String args[]) throws Exception	{
		BPStructCMDOptions options = new BPStructCMDOptions();
		CmdLineParser parser = new CmdLineParser(options);
		File ifile;
		
		try {
			parser.parseArgument(args);
			
			if (!options.odir.exists()) options.odir.mkdir();
			if (!options.odir.isDirectory())
				throw new IOException("Cannot create/open output directory: " + options.odir.getName());
			
			ifile = new File(options.arguments.get(0));
			
			File parent = ifile.getParentFile();
			if (parent == null) parent = new File(".");
			
			if (!ifile.exists() || !ifile.isFile())
				throw new IOException("Cannot open input model file: " + ifile.getAbsoluteFile());
							
			String name = ifile.getName();

			PrintStream out = System.out;				
			System.setOut(new PrintStream("bpstruct.log"));
			
			String line;
			BufferedReader in = new BufferedReader(new FileReader(ifile));
			StringBuilder strb = new StringBuilder();
			while ((line = in.readLine()) != null)
				strb.append(line);
			Process proc = JSON2Process.convert(strb.toString());

			if (options.dot) { // serialize given model to DOT format
				File ofile = new File(options.odir, String.format("%s.dot", BPStructCMD.getFileNameWithoutExtension(name)));
				PrintStream outstr = new PrintStream(ofile);
				outstr.print(Process2DOT.convert(proc));
				outstr.close();
				out.printf("DOT file with input model serialized in: '%s'\n", ofile.getPath());
			} 
			else { // structure model
				// TODO check if naming of silent gateways is still required
				int count = 0;
				for (Gateway gw: proc.getGateways())
					if (gw.getName().isEmpty()) 
						gw.setName("gw"+count++);
				
				Restructurer str = new Restructurer(proc);
				
				if (str.perform()) {
					File ofile = new File(options.odir, String.format("%s.struct.json", BPStructCMD.getFileNameWithoutExtension(name)));
					PrintStream outstr = new PrintStream(ofile);
					outstr.print(Process2JSON.convert(str.proc));
					outstr.close();
					out.printf("JSON file with output model serialized in: '%s'\n", ofile.getPath());
				}
				else
					out.println("Model cannot be restructured");
			}
							
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			printUsage(parser);
		} catch (IndexOutOfBoundsException e) {
			System.err.println("Please specify the input model path");
			printUsage(parser);
		}
	}

	private static void printUsage(CmdLineParser parser) {
		System.err.println("\nUsage:");
		System.err.println("\tjava -jar bpstruct.jar [options] <inputmodel>\nOptions:");
		parser.printUsage(System.err);		
	}
	
    public static String getFileNameWithoutExtension(String fileName) {
        int whereDot = fileName.lastIndexOf('.');
        if (0 < whereDot && whereDot <= fileName.length() - 2 )
        	return fileName.substring(0, whereDot);

        return "";
    }
}
