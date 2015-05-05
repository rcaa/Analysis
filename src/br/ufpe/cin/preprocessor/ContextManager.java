package br.ufpe.cin.preprocessor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class ContextManager {

	// for class, feature, line number
	private Map<String, Map<String, Set<Integer>>> mapClassFeatures;
	// for features and their line number
	private Map<String, Set<Integer>> mapFeatures;
	private Map<Integer, Set<String>> mapLineNumberFeature;
	// for controlling the pairs ifdef-endifs
	public static Stack<String> stackDirectives;

	// singleton
	private final static ContextManager instance = new ContextManager();

	private ContextManager() {
		this.mapClassFeatures = new HashMap<>();
		mapFeatures = new HashMap<String, Set<Integer>>();
		mapLineNumberFeature = new HashMap<Integer, Set<String>>();
		stackDirectives = new Stack<String>();
	}

	public static ContextManager getContext() {
		return instance;
	}

	public void addInfo(Integer lineNumber, String feature) {
		// verifica se jah existe a feature no map
		if (mapLineNumberFeature.containsKey(lineNumber)) {
			Set<String> setOldValues = mapLineNumberFeature.get(lineNumber);
			setOldValues.add(feature);
			mapLineNumberFeature.put(lineNumber, setOldValues);
			return;
		}
		Set<String> set = new HashSet<String>();
		set.add(feature);

		mapLineNumberFeature.put(lineNumber, set);
	}

	public void addFeatureInfo(String key, Integer value) {
		// verifica se jah existe a feature no map
		if (mapFeatures.containsKey(key)) {
			Set<Integer> setOldValues = mapFeatures.get(key);
			setOldValues.add(value);
			mapFeatures.put(key, setOldValues);
			return;
		}
		Set<Integer> setLineNumbers = new HashSet<Integer>();
		setLineNumbers.add(value);

		mapFeatures.put(key, setLineNumbers);
	}

	public Map<String, Set<Integer>> getMapFeatures() {
		return mapFeatures;
	}

	public void addDirective(String ifdef) {
		stackDirectives.push(ifdef);
	}

	public void removeTopDirective() {
		stackDirectives.pop();
	}

	public boolean stackIsEmpty() {
		return stackDirectives.isEmpty();
	}

	public int stackSize() {
		return stackDirectives.size();
	}

	public void clearAll() {
		mapFeatures.clear();
		stackDirectives.clear();
	}

	public Map<String, Map<String, Set<Integer>>> getMapClassFeatures() {
		return mapClassFeatures;
	}
}
