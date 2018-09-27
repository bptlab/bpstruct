package ee.ut.bpstruct;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.hpi.bpt.process.Process;
import de.hpi.bpt.process.checks.structural.ProcessStructureChecker;
import de.hpi.bpt.process.serialize.JSON2Process;
import de.hpi.bpt.process.serialize.Process2DOT;
import de.hpi.bpt.process.serialize.Process2JSON;
import de.hpi.bpt.process.serialize.SerializationException;

/**
 * This Class wraps the functionality of the {@link BPStructAPI}
 * in a simple command line application.
 * @author Christian Wiggert
 *
 */
public class BPStructCommandLineTool {

	/**
	 * Runs the command line application.<br>
	 * Supports the following flags:
	 * <ul>
	 * 	<li>d: save as DOT</li>
	 * 	<li>c: check the structure</li>
	 * 	<li>s: structure the process </li>
	 * </ul>
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length == 1 && args[0].equals("--help")) {
			showUsage(true);
			System.exit(0);
		} else if (args.length == 1 || args.length > 3) {
			showUsage(false);
			System.exit(1);
		}
		String flags = args[0];
		if (checkFlags(flags)) {
			Process process = loadProcess(args[1]);
			if (process != null) {
				if (flags.contains("c"))
					checkStructure(process);
				if (args.length == 3) {
					if (flags.contains("s"))
						process = structureFull(process);
					if (flags.contains("d"))
						writeDot(process, args[2]);
					else if (flags.contains("s"))
						writeProcess(process, args[2]);
				} else if (flags.contains("s") || flags.contains("d"))
					System.err.println("ERROR: The parameter with the output file is missing.");
			}
		}
	}
	
	/**
	 * Simply prints out the usage / command line options of the application.
	 * The parameter extended indicates whether the long or the short version shall be displayed.
	 * @param extended - true if the long version shall be displayed
	 */
	private static void showUsage(boolean extended) {
		if (!extended) 
			System.out.println("Usage: -[cds] <input file> [<output file>]");
		else {
			System.out.println("Usage: -<Options> <input file> [<output file>]");
			System.out.println("Options:");
			System.out.println("c: Checks the structure of a process and prints the result.");
			System.out.println("d: Saves the result as DOT. Can be used in combination with structuring or to simply transform a process to DOT. (requires <output file>)");
			System.out.println("s: Structures a process and saves the result to the output file.");
		}
	}
	
	/**
	 * Run a simple check whether the {@link Process} is already structured and print out the result.
	 * @param {@link Process} to check
	 */
	public static void checkStructure(Process process) {
		if (BPStructAPI.checkStructure(process)) {
			System.out.println("The process is structured.");
		} else {
			System.out.println("The process is unstructured.");
		}
	}

	/**
	 * Checks whether the given flags are correct.
	 * @param flags
	 * @return true if the flags are correct
	 */
	private static boolean checkFlags(String flags) {
		if (flags.matches("-[cds]{1,3}"))
			return true;
		String wrong = flags.replaceAll("[-cds]", "");
		System.err.println("ERROR: Unknown option(s): " + wrong);
		return false;
	}
	
	/**
	 * Loads the {@link Process} from the given file.
	 * @param filename of the file
	 * @return loaded {@link Process} or null if loading failed
	 */
	public static Process loadProcess(String filename) {
		StringBuilder sb = new StringBuilder();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(filename));
			String line;
			while ((line = reader.readLine()) != null)
				sb.append(line);
			reader.close();
		} catch (IOException e) {
			System.err.println("ERROR: Couldn't open file: " + filename);
		}
		Process process = null;
		try {
			process = JSON2Process.convert(sb.toString());
		} catch (SerializationException e) {
			System.err.println(e.getMessage());
		}
		return process; 
	}
	
	/**
	 * Serializes the given {@link Process} to DOT and writes it to the given file.
	 * @param {@link Process} to serialize
	 * @param filename
	 */
	public static void writeDot(Process process, String filename) {
		String content = Process2DOT.convert(process);
		writeContent(content, filename);
	}
	
	/**
	 * Serializes the given {@link Process} to JSON and writes it to the given file.
	 * @param {@link Process} to serialize
	 * @param filename
	 */
	public static void writeProcess(Process process, String filename) {
		String json = null;
		try {
			json = Process2JSON.convert(process);
		} catch (SerializationException e) {
			System.err.println("ERROR: " + e.getMessage());
		}
		writeContent(json, filename);
	}	
	
	/**
	 * Writes the given String content to the given file.
	 * @param content to write
	 * @param filename
	 */
	public static void writeContent(String content, String filename) {
		if (content != null) {
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
				writer.write(content);
				writer.close();
			} catch (IOException e) {
				System.err.println("ERROR: Couldn't write file: " + filename);
			}
		}
	}
	
	/**
	 * Structures the given {@link Process}.
	 * @param {@link Process} to structure
	 * @return null if an error occurs, otherwise the structured process
	 */
	public static Process structureFull(Process process) {
		Process result = null;
		List<String> errors = new ArrayList<String>();
		errors.addAll(ProcessStructureChecker.checkStructure(process));
		if (errors.isEmpty()) {
			BPStructResult bpres = null;
			try {
				bpres = BPStructAPI.structure(process, null, null);	
			} catch (Exception e) {
				errors.add("An error occured while structuring the process.");
			}
			if (bpres != null) {
				if (bpres.isStructured()) {
					result = bpres.getProcess();
				} else 
					errors.add("Process " + process.getName() + " could not be structured.");
			}
		}
		for (String error:errors) 
			System.err.println("ERROR: " + error);
		return result;
	}
	
}
