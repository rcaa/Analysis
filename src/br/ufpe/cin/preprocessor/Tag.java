package br.ufpe.cin.preprocessor;

public final class Tag {

	public static final String comment = "//";

	public static final String IF = "#if";

	// public static final String IFNDEF = "#ifndef";

	public static final String ELSE = "#else";

	public static final String ENDIF = "#endif";

	// public static final String INCLUDE = "#include";

	public static final String regex = "^\\s*" + comment + "(" + IF + "|"
	// + IFNDEF + "|"
			+ ELSE + "|" + ENDIF
			// + "|" + INCLUDE
			+ ")\\s*(.*)\\s*$";

//	public static final String ifdefRegex = "^\\s*" + comment + "(" + IF
//	// + "|" + IFNDEF
//			+ "|" + ELSE + ")\\s*(.*)\\s*$";
}