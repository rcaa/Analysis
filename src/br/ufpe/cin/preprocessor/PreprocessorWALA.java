package br.ufpe.cin.preprocessor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PreprocessorWALA {

	private String defs = ""; // defs are the features (e.g. DEBUG, LOGGING,
								// etc)
	private String incdir;

	public PreprocessorWALA(String sourceDirectory) {
		try {
			File[] files = new File(sourceDirectory).listFiles();
			ContextManager.getContext().getClassDirectories(files); //preeche uma lista com todos os arquivos Java do diretorio
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void execute() throws PreprocessorException {
		ContextManager context = ContextManager.getContext();

		if (context.getSrcFiles() == null || context.getSrcFiles().isEmpty()) {
			throw new PreprocessorException(
					"Some parameter missed. Make sure that definition list and input files are provided.");
		}

		try {
			preprocess();

			System.out.println(context.getMapClassFeatures());

		} catch (IOException e) {
			throw new PreprocessorException("IO error while preprocessing");
		}
	}

	private void preprocess() throws IOException {
		ContextManager context = ContextManager.getContext();

		BufferedReader br = null; // for reading from file

		List<String> srcFiles = context.getSrcFiles();

		for (String srcFile : srcFiles) {
			br = new BufferedReader(new FileReader(srcFile));

			/**
			 * Gets the defined features, split them by "," and, finally, remove
			 * all duplicate white spaces
			 */
			Set<String> set = new HashSet<String>(Arrays.asList(defs
					.replaceAll("\\s+", "").split(",")));

			// Compiles the regex then sets the pattern for tags
			Pattern pattern = Pattern.compile(Tag.regex,
					Pattern.CASE_INSENSITIVE);

			String line;
			int lineNumber = 0; // for counting the line number
			int currentLevel = 0; // for controling the tags (e.g. ifdefs and
									// endifs)
			int removeLevel = -1; // if -1 can write, otherwise cannot
			boolean skip = false; // this flag serves to control code within a
									// certain feature

			// reading line-by-line from input file
			while ((line = br.readLine()) != null) {
				lineNumber++;

				/**
				 * Creates a matcher that will match the given input against
				 * this pattern.
				 */
				Matcher matcher = pattern.matcher(line);

				/**
				 * Matches the defined pattern with the current line
				 */
				if (matcher.matches()) {
					/**
					 * MatchResult is unaffected by subsequent operations
					 */
					MatchResult result = matcher.toMatchResult();
					String dir = result.group(1).toLowerCase(); // preprocessor
																// directives
					String param = result.group(2); // feature's name

					if (Tag.IF.equals(dir)) {
						// adds if X on the stack
						context.addDirective(dir + " "
								+ param.replaceAll("\\s", ""));

						// verifies if the feature was defined
						if (defs.replaceAll("\\s+", "").length() > 0) {
							skip = !set.contains(param);
						} else {
							skip = false;
						}

						if (removeLevel == -1 && skip) {
							removeLevel = currentLevel;
						}
						currentLevel++;
						continue;
					} else if (Tag.IFNDEF.equals(dir)) {

						context.addDirective(dir + " " + param);

						if (defs.replaceAll("\\s+", "").length() > 0) {
							skip = set.contains(param);
						} else {
							skip = false;
						}

						if (removeLevel == -1 && skip) {
							removeLevel = currentLevel;
						}
						currentLevel++;
						continue;
					} else if (Tag.ELSE.equals(dir)) {
						currentLevel--;
						if (currentLevel == removeLevel) {
							removeLevel = -1;
						}
						if (removeLevel == -1 && !skip) {
							removeLevel = currentLevel;
						}
						currentLevel++;
						continue;
					} else if (Tag.ENDIF.equals(dir)) {

						if (context.stackIsEmpty()) {
							System.out
									.println("#endif encountered without corresponding #ifdef or #ifndef");
							return;
						}

						context.removeTopDirective();

						currentLevel--;
						if (currentLevel == removeLevel) {
							removeLevel = -1;
						}
						continue;
					} else if (Tag.INCLUDE.equals(dir)) {
						// include(param, bw);
						continue;
					}
				} else {
					/**
					 * verifies if the current line does not have text (code)
					 */
					if (!line.trim().isEmpty()) {
						/**
						 * Add information on mapping between feature expression
						 * and line number.
						 */
						addInfoOnMapping(context, lineNumber);
					}
				}
			}

			String className = prepareClassName(srcFile);

			context.getMapClassFeatures()
					.put(className,
							new HashMap<String, Set<Integer>>(context
									.getMapFeatures()));
			context.clearAll();
		}
		if (br != null) {
			br.close();
		}
	}

	private String prepareClassName(String srcFile) {
		String[] strings = srcFile.split("/");
		String className = "";
		int srcPosition = -1;
		for (int i = 0; i < (strings.length - 1); i++) {
			if (strings[i].equals("src")) {
				srcPosition = i;
			}
			if (srcPosition != -1) {
				if (className.isEmpty()) {
					className = strings[i + 1];
				} else {
					className = className + "." + strings[i + 1];
				}
			}
		}
		if (className.endsWith(".java")) {
			className = className.substring(0, className.length() - 5);
		}
		return className;
	}

	private void addInfoOnMapping(ContextManager context, Integer infoLine) {
		// verifica pelo contexto se o a linha atual pertence alguma
		// feature se sim, add no map. Caso contrario, faz nada.
		Stack<String> auxStack = new Stack<String>();

		// copy stack
		for (int i = 0; i < context.stackSize(); i++) {
			auxStack.add(ContextManager.stackDirectives.get(i));
		}

		while (!auxStack.isEmpty()) {
			// gets feature's name
			String feature = auxStack.peek().split(" ")[1];

			if (auxStack.peek().contains(Tag.IFNDEF)) {
				feature = "~" + feature;
			}

			// add info about line number of a certain feature
			context.addFeatureInfo(feature, infoLine);
			context.addInfo(infoLine, feature);

			auxStack.pop();
		}

		auxStack.clear();
	}

	public String getDefs() {
		return defs;
	}

	public String getIncdir() {
		return incdir;
	}

	public void setDefs(String defs) {
		this.defs = defs;
	}

	public void setIncdir(String incdir) {
		this.incdir = incdir;
	}

	// public static void main(String[] args) {
	//
	// // sets the IO files
	// ContextManager manager = ContextManager.getContext();
	// manager.setSrcfile("Out.groovy");
	// manager.setDestfile("Testclass2.groovy");
	//
	// PreprocessorWALA pp = new PreprocessorWALA();
	//
	// String defs = "";// "A , SOMA";
	// pp.setDefs(defs);
	//
	// // TODO create the graphic interface
	// try {
	// pp.execute();
	// } catch (PreprocessorException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// }
}