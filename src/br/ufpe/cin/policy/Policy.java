package br.ufpe.cin.policy;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

public class Policy {

	private String feature;
	private String operator;
	private String clazz;
	private String programElement;

	public Policy(String policyDirectory) {
		Path path = FileSystems.getDefault().getPath(policyDirectory);
		try {
			String policy = new String(Files.readAllBytes(path));
			if (policy.contains("noflow")) {
				String[] elements = policy.split(" ");
				this.clazz = elements[0];
				this.programElement = elements[1].substring(1,
						elements[1].length() - 1);
				this.setOperator(elements[2]);
				this.feature = elements[3];
			} else if (policy.contains("noset")) {
				String[] elements = policy.split(" ");
				this.feature = elements[0];
				this.setOperator(elements[1]);
				this.clazz = elements[2];
				this.programElement = elements[3].substring(1,
						elements[3].length() - 1);
			}
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
		return this.clazz + " {" + this.programElement + "}" + " "
				+ this.getOperator() + " " + this.feature + ";";
	}

	public String getOperator() {
		return operator;
	}

	public void setOperator(String operator) {
		this.operator = operator;
	}
}