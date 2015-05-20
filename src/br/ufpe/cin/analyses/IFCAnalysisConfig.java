package br.ufpe.cin.analyses;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import br.ufpe.cin.policy.Policy;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.NullProgressMonitor;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.lattice.BuiltinLattices;
import edu.kit.joana.api.sdg.SDGAttribute;
import edu.kit.joana.api.sdg.SDGClass;
import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.api.sdg.SDGInstruction;
import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.api.sdg.SDGProgramPart;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;

public class IFCAnalysisConfig {

	public SDGProgram prepareAnalysis(String classPath,
			JavaMethodSignature entryMethod) throws ClassHierarchyException,
			IOException, UnsoundGraphException, CancelException {
		/**
		 * the class path is either a directory or a jar containing all the
		 * classes of the program which you want to analyze
		 */
		// String classPath =
		// "/Users/rodrigoandrade/Documents/workspaces/Doutorado/wala/SimpleExamples/bin/";

		/**
		 * the entry method is the main method which starts the program you want
		 * to analyze
		 */
		// JavaMethodSignature entryMethod = JavaMethodSignature
		// .mainMethodOfClass("Main");

		/**
		 * For multi-threaded programs, it is currently necessary to use the jdk
		 * 1.4 stubs
		 */
		SDGConfig config = new SDGConfig(classPath, entryMethod.toBCString(),
				Stubs.JRE_14);

		/**
		 * compute interference edges to model dependencies between threads (set
		 * to false if your program does not use threads)
		 */
		config.setComputeInterferences(false);

		/**
		 * additional MHP analysis to prune interference edges (does not matter
		 * for programs without multiple threads)
		 */
		// config.setMhpType(MHPType.PRECISE);

		/**
		 * precision of the used points-to analysis - INSTANCE_BASED is a good
		 * value for simple examples
		 */
		config.setPointsToPrecision(PointsToPrecision.OBJECT_SENSITIVE);

		/**
		 * exception analysis is used to detect exceptional control-flow which
		 * cannot happen
		 */
		config.setExceptionAnalysis(ExceptionAnalysis.INTERPROC);

		/** build the PDG */
		SDGProgram program = SDGProgram.createSDGProgram(config, System.out,
				new NullProgressMonitor());

		/** optional: save PDG to disk */
		// SDGSerializer.toPDGFormat(program.getSDG(), new FileOutputStream(
		// "/Users/rodrigoandrade/Dropbox/Temp/SDGDirect.pdg"));
		return program;
	}

	public IFCAnalysis runAnalysis(List<SDGProgramPart> sources,
			List<SDGProgramPart> sinks, SDGProgram program) {
		IFCAnalysis ana = new IFCAnalysis(program);

		/** annotate sources and sinks */

		annotateSources(sources, program, ana);

		annotateSinks(sinks, program, ana);
		// ana.addSourceAnnotation(program.getPart("model.User.passwordHash"),
		// BuiltinLattices.STD_SECLEVEL_HIGH);
		// ana.addSinkAnnotation(program.getMethod("util.Log.loggingAction(Ljava/lang/Object;)V"),
		// BuiltinLattices.STD_SECLEVEL_LOW);
		return ana;
	}

	private void annotateSources(List<SDGProgramPart> sources,
			SDGProgram program, IFCAnalysis ana) {
		for (SDGProgramPart source : sources) {
			if (source instanceof SDGMethod) {
				ana.addSourceAnnotation(program.getMethod(source.toString()),
						BuiltinLattices.STD_SECLEVEL_HIGH);
			} else if (source instanceof SDGAttribute) {
				ana.addSourceAnnotation(program.getPart(source.toString()),
						BuiltinLattices.STD_SECLEVEL_HIGH);
			} else if (source instanceof SDGInstruction) {
				Collection<SDGInstruction> instructions = program
						.getInstruction(
								source.getOwningMethod().getSignature(),
								((SDGInstruction) source).getBytecodeIndex());
				for (SDGInstruction sdgInstruction : instructions) {
					ana.addSourceAnnotation(sdgInstruction,
							BuiltinLattices.STD_SECLEVEL_HIGH);
				}
			} else {
				ana.addSourceAnnotation(source,
						BuiltinLattices.STD_SECLEVEL_HIGH);
			}
		}
	}

	private void annotateSinks(List<SDGProgramPart> sinks, SDGProgram program,
			IFCAnalysis ana) {
		for (SDGProgramPart sink : sinks) {
			if (sink instanceof SDGMethod) {
				ana.addSinkAnnotation(program.getMethod(sink.toString()),
						BuiltinLattices.STD_SECLEVEL_LOW);
			} else if (sink instanceof SDGAttribute) {
				ana.addSinkAnnotation(program.getPart(sink.toString()),
						BuiltinLattices.STD_SECLEVEL_LOW);
			} else if (sink instanceof SDGInstruction) {
				Collection<SDGInstruction> instructions = program
						.getInstruction(sink.getOwningMethod().getSignature(),
								((SDGInstruction) sink).getBytecodeIndex());
				for (SDGInstruction sdgInstruction : instructions) {
					ana.addSinkAnnotation(sdgInstruction,
							BuiltinLattices.STD_SECLEVEL_LOW);
				}
			} else {
				ana.addSinkAnnotation(sink, BuiltinLattices.STD_SECLEVEL_LOW);
			}
		}
	}

	public void prepareListsOfSourceAndSinks(Collection<SDGClass> classes,
			Map<String, Map<String, Set<Integer>>> mapClassFeatures,
			Policy policy, List<SDGProgramPart> sources,
			List<SDGProgramPart> sinks) {
		for (SDGClass sdgClass : classes) {
			if (!mapClassFeatures.containsKey(sdgClass.toString())) {
				continue;
			}

			// por enquanto so marca atributo como source
			for (SDGAttribute sdgAttribute : sdgClass.getAttributes()) {
				if (sdgAttribute.toString().equals(
						policy.getSensitiveResource())) {
					if (policy.getOperator().equals("noflow")) {
						sources.add(sdgAttribute);
					} else if (policy.getOperator().equals("noset")) {
						sinks.add(sdgAttribute);
					}
				}
			}

			// por enquanto so marca instrucao de metodo como sink
			for (SDGMethod sdgMethod : sdgClass.getMethods()) {
				IMethod meth = sdgMethod.getMethod();
				List<SDGInstruction> methodInstructions = sdgMethod
						.getInstructions();
				for (SDGInstruction sdgInstruction : methodInstructions) {
					Map<String, Set<Integer>> mapFeatures = mapClassFeatures
							.get(sdgClass.toString());
					Set<Integer> lineNumbers = mapFeatures.get(policy
							.getFeature());

					Integer sourceLine = meth.getLineNumber(sdgInstruction
							.getBytecodeIndex());

					if (lineNumbers != null && lineNumbers.contains(sourceLine)) {
						if (policy.getOperator().equals("noflow")) {
							sinks.add(sdgInstruction);
						} else if (policy.getOperator().equals("noset")) {
							sources.add(sdgInstruction);
						}

					}
				}
			}
		}
	}

}
