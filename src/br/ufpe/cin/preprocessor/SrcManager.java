package br.ufpe.cin.preprocessor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SrcManager {

	private List<String> srcFiles; // input file paths
	
	private final static SrcManager instance = new SrcManager();

	private SrcManager() {
		this.srcFiles = new ArrayList<>();
	}
	
	public static SrcManager getSrcManager() {
		return instance;
	}

	public List<String> getSrcFiles() {
		return srcFiles;
	}

	/**
	 * Searches for files to have preprocessor tags mapped
	 * 
	 * @param files
	 * @throws IOException
	 */
	public void getClassDirectories(File[] files) throws IOException {
		for (File file : files) {
			if (file.isDirectory()) {
				getClassDirectories(file.listFiles());
			} else {
				// caso deseje-se considerar outros tipos de arquivos, eh so
				// adicionar a terminacao
				if (file.getName().endsWith(".java")) {
					srcFiles.add(file.getCanonicalPath());
				}
			}
		}
	}

}
