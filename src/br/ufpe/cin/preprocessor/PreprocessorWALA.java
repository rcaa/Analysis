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

	public PreprocessorWALA(String sourceDirectory) {
		try {
			File[] files = new File(sourceDirectory).listFiles();
			// preeche uma lista com todos os arquivos Java do diretorio
			SrcManager.getSrcManager().getClassDirectories(files);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void execute() throws PreprocessorException {
		ContextManager context = ContextManager.getContext();
		SrcManager srcmanager = SrcManager.getSrcManager();

		if (srcmanager.getSrcFiles() == null || srcmanager.getSrcFiles().isEmpty()) {
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

	private void preprocess() throws IOException, PreprocessorException {
		ContextManager context = ContextManager.getContext();
		SrcManager srcmanager = SrcManager.getSrcManager();

		BufferedReader br = null; // for reading from file

		List<String> srcFiles = srcmanager.getSrcFiles();

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

			int lineNumber = 0; // for counting the line number
			int currentLevel = 0; // for controling the tags (e.g. ifdefs and
									// endifs)
			int removeLevel = -1; // if -1 can write, otherwise cannot
			boolean skip = false; // this flag serves to control code within a
									// certain feature

			iteratingOverSrcLines(context, br, set, pattern, lineNumber,
					currentLevel, removeLevel, skip);

			mappingClassesAndFeatures(context, srcFile);
		}
		if (br != null) {
			br.close();
		}
	}

	private void iteratingOverSrcLines(ContextManager context,
			BufferedReader br, Set<String> set, Pattern pattern,
			int lineNumber, int currentLevel, int removeLevel, boolean skip)
			throws IOException, PreprocessorException {
		String line;
		// reading line-by-line from input file
		while ((line = br.readLine()) != null) {
			lineNumber++;

			/**
			 * Creates a matcher that will match the given input against this
			 * pattern.
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
					addingIFTagOnStack(context, set, currentLevel, removeLevel,
							dir, param);
					continue;
				} else if (Tag.ELSE.equals(dir)) {
					addingElseTagOnStack(currentLevel, removeLevel, skip);
					continue;
				} else if (Tag.ENDIF.equals(dir)) {

					addingEndifTagOnStack(context, currentLevel, removeLevel);
					continue;
				}
			} else {
				/**
				 * verifies if the current line does not have text (code)
				 */
				if (!line.trim().isEmpty()) {
					/**
					 * Add information on mapping between feature expression and
					 * line number.
					 */
					addInfoOnMapping(context, lineNumber);
				}
			}
		}
	}

	private void addingEndifTagOnStack(ContextManager context,
			int currentLevel, int removeLevel) throws PreprocessorException {
		if (context.stackIsEmpty()) {
			throw new PreprocessorException("#endif encountered without "
					+ "corresponding #ifdef");
		}

		context.removeTopDirective();

		currentLevel--;
		if (currentLevel == removeLevel) {
			removeLevel = -1;
		}
	}

	private void addingElseTagOnStack(int currentLevel, int removeLevel,
			boolean skip) {
		currentLevel--;
		if (currentLevel == removeLevel) {
			removeLevel = -1;
		}
		if (removeLevel == -1 && !skip) {
			removeLevel = currentLevel;
		}
		currentLevel++;
	}

	private void addingIFTagOnStack(ContextManager context, Set<String> set,
			int currentLevel, int removeLevel, String dir, String param) {
		boolean skip;
		// adds if X on the stack
		context.addDirective(dir + " " + param.replaceAll("\\s", ""));

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
	}

	private void mappingClassesAndFeatures(ContextManager context,
			String srcFile) {
		String className = prepareClassName(srcFile);

		context.getMapClassFeatures().put(className,
				new HashMap<String, Set<Integer>>(context.getMapFeatures()));
		context.clearAll();
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

			// if (auxStack.peek().contains(Tag.IFNDEF)) {
			// feature = "~" + feature;
			// }

			// add info about line number of a certain feature
			context.addFeatureInfo(feature, infoLine);
			context.addInfo(infoLine, feature);

			auxStack.pop();
		}

		auxStack.clear();
	}
}