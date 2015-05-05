package br.ufpe.cin.analyses;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import br.ufpe.cin.policy.Policy;
import br.ufpe.cin.preprocessor.ContextManager;
import br.ufpe.cin.preprocessor.PreprocessorException;
import br.ufpe.cin.preprocessor.PreprocessorWALA;

import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;
import com.ibm.wala.util.io.CommandLine;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.sdg.SDGClass;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.api.sdg.SDGProgramPart;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import gnu.trove.map.TObjectIntMap;

public class MainAnalysis {

	public static void main(String[] args) {
		Properties p = CommandLine.parse(args);
		try {
			MainAnalysis m = new MainAnalysis();
			m.run(p);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void run(Properties p) throws WalaException,
			IllegalArgumentException, IOException, UnsoundGraphException,
			CancelException {

		// Primeiro passo logico
		preprocess(p);

		// Segundo passo logico

		JavaMethodSignature entryMethod = JavaMethodSignature
				.mainMethodOfClass(p.getProperty("main"));

		// mapeamento de features e linhas
		ContextManager context = ContextManager.getContext();
		Map<String, Map<String, Set<Integer>>> mapClassFeatures = context
				.getMapClassFeatures();

		// obtenho a policy
		Policy policy = new Policy(p.getProperty("policyDirectory"));

		// montar o SDG graph
		IFCAnalysisConfig ana = new IFCAnalysisConfig();
		SDGProgram program = ana.prepareAnalysis(p.getProperty("classpath"),
				entryMethod);
		Collection<SDGClass> classes = program.getClasses();

		// rotulo statements e expressions
		List<SDGProgramPart> sources = new ArrayList<SDGProgramPart>();
		List<SDGProgramPart> sinks = new ArrayList<SDGProgramPart>();
		ana.prepareListsOfSourceAndSinks(classes, mapClassFeatures, policy,
				sources, sinks);

		// rodo as analises
		IFCAnalysis ifc = ana.runAnalysis(sources, sinks, program);
		Collection<? extends IViolation<SecurityNode>> result = ifc.doIFC();
		TObjectIntMap<IViolation<SDGProgramPart>> resultByProgramPart = ifc
				.groupByPPPart(result);
		System.out.println(resultByProgramPart);
	}

	private void preprocess(Properties p) {
		try {
			String sourceDirectory = p.getProperty("sourceDirectory");
			PreprocessorWALA pp = new PreprocessorWALA(sourceDirectory);
			pp.execute();
		} catch (PreprocessorException e) {
			e.printStackTrace();
		}
	}
}
