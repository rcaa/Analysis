package br.ufpe.cin.preprocessor;

public class ProgramElementsPatterns {

	public static final String method = "((public|private|protected|static|final|native|synchronized|abstract|"
			+ "threadsafe|transient)+\\s)+[\\$_\\w\\<\\>\\[\\]]*\\s+[\\$_\\w]+\\([^\\)]*\\)?\\s*\\{?[^\\}]*\\}?";
}
