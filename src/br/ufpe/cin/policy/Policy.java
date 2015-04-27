package br.ufpe.cin.policy;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

public class Policy {

	private String feature;
	private String auth;
	private String clazz;
	private String programElement;

	public Policy(String policyDirectory) {
		Path path = FileSystems.getDefault().getPath(policyDirectory);
		try {
			String policy = new String(Files.readAllBytes(path));
			String[] elements = policy.split(" ");
			this.feature = elements[0];
			this.auth = elements[1];
			this.clazz = elements[2];
			this.programElement = elements[3].substring(1,
					elements[3].length() - 2);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getFeature() {
		return feature;
	}

	public void setFeature(String feature) {
		this.feature = feature;
	}

	public String getClazz() {
		return clazz;
	}

	public void setClazz(String clazz) {
		this.clazz = clazz;
	}

	public String getProgramElement() {
		return programElement;
	}

	public void setProgramElement(String programElement) {
		this.programElement = programElement;
	}
	
	public String getSensitiveResource() {
		return clazz + "." + programElement;
	}

	@Override
	public String toString() {
		return this.feature + " " + this.auth + " " + this.clazz + " {"
				+ this.programElement + "};";
	}

}