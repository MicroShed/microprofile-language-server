package com.microprofile.lsp.internal.diagnostic;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticRelatedInformation;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentItem;

import com.microprofile.lsp.internal.MicroProfileConstants;
import com.microprofile.lsp.internal.MicroProfileLanguageServer;
import com.microprofile.lsp.internal.Utils;

/**
 * Class to configure MicroProfile specific diagnostics, some of which
 * correspond to a quick fix code action
 *
 */
public class MicroProfileDiagnostic {

	/**
	 * Update diagnostics with MicroProfile specific diagnostics
	 * 
	 * @param uri           current document uri
	 * @param diagnostics   list of diagnostics for current document
	 * @param document      current document
	 * @param astRootFolder src/main/java folder or root folder of workspace
	 * @throws BadLocationException
	 */
	public void updateDiagnostics(String uri, List<Diagnostic> diagnostics, TextDocumentItem document,
			File astRootFolder) throws BadLocationException {
		List<CompilationUnit> cus = createCompilationUnits(astRootFolder);
		CompilationUnit cu = Utils.toCompilationUnit(document);

		List imports = cu.imports();
		List types = cu.types();

		Range healthAnnotationRange = new Range();
		Range healthCheckInterfaceRange = new Range();
		boolean healthAnnotationFlag = false;
		boolean healthCheckInterfaceFlag = false;

		// iterate through Types of current class
		for (Iterator iter = types.iterator(); iter.hasNext();) {
			Object next = iter.next();
			if (next instanceof TypeDeclaration) {
				TypeDeclaration type = (TypeDeclaration) next;

				// gets interfaces of current type
				List superInterfaceTypes = type.superInterfaceTypes();

				// Get annotations on current type
				List typeModifiers = type.modifiers();

				/*
				 * MicroProfile RestClient Diagnostics
				 * 
				 * Diagnostic 1: Field on current type has Inject and RestClient annotations but
				 * corresponding interface does not have RegisterRestClient annotation
				 * 
				 * Diagnostic 2: Current type is an interface, does not have RegisterRestClient
				 * annotation but corresponding fields have Inject and RestClient annotation
				 * 
				 * Diagnostic 3: Field on current type has Inject and not RestClient annotations
				 * but corresponding interface has RegisterRestClient annotation
				 * 
				 * Diagnostic 4: Field on current type has RestClient and not Inject annotations
				 * but corresponding interface has RegisterRestClient annotation
				 * 
				 * Diagnostic 5: Field on current type has RestClient and Inject annotations but
				 * corresponding interface has RegisterRestClient annotation
				 */

				// REST DIAGNOSTIC 1
				FieldDeclaration[] fields = type.getFields();
				for (FieldDeclaration field : fields) {
					Annotation restClientAnnotation = hasAnnotation(field.modifiers(), "@RestClient");
					Annotation injectAnnotation = hasAnnotation(field.modifiers(), "@Inject");

					// match FieldDeclaration to corresponding InterfaceDeclaration
					List<CompilationUnit> interfaceCus = compilationUnitsOfInterfaces(cus);
					for (CompilationUnit interfaceCu : interfaceCus) {
						List interfaceTypes = interfaceCu.types();
						for (Iterator interfaceIter = interfaceTypes.iterator(); interfaceIter.hasNext();) {
							Object interfaceObj = interfaceIter.next();
							if (interfaceObj instanceof TypeDeclaration) {

								TypeDeclaration interfaceType = (TypeDeclaration) interfaceObj;

								if (interfaceType.getName().toString().equals(field.getType().toString())) { // if true
																												// found
																												// interface
																												// matching
																												// field
																												// declaration


									// check if corresponding interface has registerRestClientAnnotation
									Annotation registerRestClientAnnotation2 = hasAnnotation(interfaceType.modifiers(),
											"@RegisterRestClient");
									if (registerRestClientAnnotation2 == null) { // deliver diagnostic warning on field

										if ((restClientAnnotation != null) && (injectAnnotation != null)) {

											List<VariableDeclarationFragment> fragments = field.fragments();
											for (VariableDeclarationFragment fragment : fragments) {
												Range restClientRange = Utils.toRange(fragment.getStartPosition(),
														fragment.getLength(), document.getText());

												Diagnostic diag = createDiagnostic(uri, "The corresponding "
														+ field.getType().toString()
														+ " interface does not have the @RegisterRestClient annotation. The field "
														+ fragment.toString() + " will not be injected as a CDI bean.",
														restClientRange,
														"Remove the @Inject and @RestClient annotations or add the @RegisterRestClient annotation to the corresponding interface to inject as a CDI bean.",
														null);

												diagnostics.add(diag);
											}
										}
									} else {

										// REST DIAGNOSTIC 3
										if ((injectAnnotation != null) && (restClientAnnotation == null)) {
											List<VariableDeclarationFragment> fragments = field.fragments();
											for (VariableDeclarationFragment fragment : fragments) {

												Range restClientRange = Utils.toRange(fragment.getStartPosition(),
														fragment.getLength(), document.getText());

												Diagnostic diag = createDiagnostic(uri,
														"The Rest Client object should have the @RestClient annotation to be injected as a CDI bean.",
														restClientRange, "Add the @RestClient annotation.",
														Integer.toString(
																MicroProfileConstants.MicroProfileRestClientAnnotation));

												diagnostics.add(diag);
											}
										}

										// REST DIAGNOSTIC 4
										if ((injectAnnotation == null) && (restClientAnnotation != null)) {
											List<VariableDeclarationFragment> fragments = field.fragments();
											for (VariableDeclarationFragment fragment : fragments) {

												Range restClientRange = Utils.toRange(fragment.getStartPosition(),
														fragment.getLength(), document.getText());

												Diagnostic diag = createDiagnostic(uri,
														"The Rest Client object should have the @Inject annotation to be injected as a CDI bean.",
														restClientRange, "Add the @Inject annotation.",
														Integer.toString(
																MicroProfileConstants.MicroProfileInjectAnnotation));

												diagnostics.add(diag);
											}
										}

										// REST DIAGNOSTIC 5
										if ((injectAnnotation == null) && (restClientAnnotation == null)) {
											List<VariableDeclarationFragment> fragments = field.fragments();
											for (VariableDeclarationFragment fragment : fragments) {

												Range restClientRange = Utils.toRange(fragment.getStartPosition(),
														fragment.getLength(), document.getText());

												Diagnostic diag = createDiagnostic(uri,
														"The Rest Client object should have the @Inject and @RestClient annotations to be injected as a CDI bean.",
														restClientRange, "Add the @Inject and @RestClient annotations.",
														Integer.toString(
																MicroProfileConstants.MicroProfileInjectRestClientAnnotation));

												diagnostics.add(diag);
											}
										}

									}
								}
							}
						}
					}
				}

				Annotation registerRestClientAnnotation = hasAnnotation(typeModifiers, "@RegisterRestClient");
				if (registerRestClientAnnotation == null) {

					// REST DIAGNOSTIC 2
					if (type.isInterface()) {
						// find corresponding field declarations, check for Inject and RestClient
						// annotations
						for (CompilationUnit altCu : cus) {
							List altTypes = altCu.types();
							for (Iterator altIter = altTypes.iterator(); altIter.hasNext();) {
								Object altNext = altIter.next();
								if (altNext instanceof TypeDeclaration) {

									TypeDeclaration altType = (TypeDeclaration) altNext;
									FieldDeclaration[] altFields = altType.getFields();
									for (FieldDeclaration field : altFields) {

										Annotation restClientAnnotation = hasAnnotation(field.modifiers(),
												"@RestClient");

										Annotation injectAnnotation = hasAnnotation(field.modifiers(), "@Inject");
										if ((restClientAnnotation != null) && (injectAnnotation != null)) {

											// check if field is initializing interface object
											if (field.getType().toString().equals(type.getName().toString())) {
												Range restInterfaceRange = Utils.toRange(
														type.getName().getStartPosition(), type.getName().getLength(),
														document.getText());
												Diagnostic diag = createDiagnostic(uri,
														"This interface does not have the @RegisterRestClient annotation. Any references will not be injected as CDI beans.",
														restInterfaceRange,
														"Add the @RegisterRestClient annotation to this interface to inject any references as CDI beans.",
														null);

												diagnostics.add(diag);
											}
										}
									}
								}
							}
						}
					}

				}

				/*
				 * MicroProfile Health
				 * 
				 * DIAGNOSTIC 1: display Health annotation diagnostic message if Health/Liveness/Readiness
				 * annotation exists but HealthCheck interface is not implemented
				 * 
				 * DIAGNOSTIC 2: display HealthCheck diagnostic message if HealthCheck interface
				 * is implemented but Health/Liveness/Readiness annotation does not exist
				 */
				// iterate through annotations
				Annotation healthAnnotation = null;
				for (Iterator annotationIter = typeModifiers.iterator(); annotationIter.hasNext();) {
					Object next2 = annotationIter.next();
					if (next2 instanceof IExtendedModifier) {
						IExtendedModifier modifier = (IExtendedModifier) next2;
						if (modifier.isAnnotation()) {
							String annotation = modifier.toString();
							if (annotation.equals("@Health") || annotation.equals("@Liveness") || annotation.equals("@Readiness")) {

								healthAnnotationFlag = true;
								healthAnnotation = (Annotation) modifier;

								try {
									healthAnnotationRange = Utils.toRange(healthAnnotation.getStartPosition(),
											healthAnnotation.getLength(), document.getText());
								} catch (BadLocationException e1) {
									e1.printStackTrace();
								}

							}
						}
					}

				}

				// iterate through superInterfaces
				for (Iterator interfacesIter = superInterfaceTypes.iterator(); interfacesIter.hasNext();) {

					Object next2 = interfacesIter.next();
					if (next2.toString().equals("HealthCheck")) {
						healthCheckInterfaceFlag = true;
						try {
							healthCheckInterfaceRange = Utils.toRange(type.getName().getStartPosition(),
									type.getName().getLength(), document.getText());
						} catch (BadLocationException e) {

							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}

				// HEALTH DIAGNOSTIC 1
				if (healthAnnotationFlag && !healthCheckInterfaceFlag) {
					Diagnostic diag = createDiagnostic(uri,
							"The class using the @Liveness, @Readiness, or @Health annotation should implement the HealthCheck interface.",
							healthAnnotationRange,
							"Implement the HealthCheck interface in the class that uses this annotation.",
							Integer.toString(MicroProfileConstants.MicroProfileHealthCheck));

					diagnostics.add(diag);
				}

				// HEALTH DIAGNOSTIC 2
				if (healthCheckInterfaceFlag && !healthAnnotationFlag) {
					Diagnostic diag = createDiagnostic(uri,
							"The class " + type.getName()
									+ " implementing the HealthCheck interface should use the @Liveness, @Readiness, or @Health annotation.",
							healthCheckInterfaceRange, "Add the @Liveness, @Readiness, or @Health annotation to this class.",
							Integer.toString(MicroProfileConstants.MicroProfileHealthAnnotation));

					diagnostics.add(diag);
				}

			}
		}
	}

	private Annotation hasAnnotation(List modifiers, String annotationName) {
		for (Iterator annotationIter = modifiers.iterator(); annotationIter.hasNext();) {
			Object next2 = annotationIter.next();
			if (next2 instanceof IExtendedModifier) {
				IExtendedModifier modifier = (IExtendedModifier) next2;
				if (modifier.isAnnotation()) {
					if (modifier.toString().equals(annotationName)) {
						return (Annotation) modifier;
					}
				}
			}

		}
		return null;
	}

	// code is optional
	private Diagnostic createDiagnostic(String uri, String message, Range range, String relatedInfo, String code) {
		Diagnostic diag = new Diagnostic();
		diag.setSource("MicroProfile");
		diag.setMessage(message);
		diag.setSeverity(DiagnosticSeverity.Warning);
		diag.setRange(range);

		if (code != null) {
			diag.setCode(code);
		}
		DiagnosticRelatedInformation info = new DiagnosticRelatedInformation();
		info.setLocation(new Location(uri, range));
		info.setMessage(message);
		List<DiagnosticRelatedInformation> list = new ArrayList<>();
		list.add(info);
		diag.setRelatedInformation(list);

		return diag;
	}

	private List<CompilationUnit> createCompilationUnits(File astRootFolder) {
		List<CompilationUnit> cuList = new ArrayList<CompilationUnit>();

		// get all java files under astRootFolder
		try {
			URI uri = astRootFolder.toURI();
			Stream<Path> walk = Files.walk(Paths.get(uri));
			List<String> results = walk.filter(p -> p.toString().endsWith(".java")).map(x -> x.toString())
					.collect(Collectors.toList());
			for (String result : results) {
				File file = new File(result);

				String sourceCode = new String(Files.readAllBytes(file.toPath()));
				cuList.add(Utils.toCompilationUnit(file.toURI().toString(), sourceCode));
			}
		} catch (IOException e) {
			MicroProfileLanguageServer.logMessage(MessageType.Error,
					"Unable to walk through astRootFolder: " + e.toString());
		}
		return cuList;
	}

	private List<CompilationUnit> compilationUnitsOfInterfaces(List<CompilationUnit> cus) {
		List<CompilationUnit> interfaceCus = new ArrayList<CompilationUnit>();
		for (CompilationUnit cu : cus) {
			List types = cu.types();
			for (Iterator iter = types.iterator(); iter.hasNext();) {
				Object next = iter.next();
				if (next instanceof TypeDeclaration) {
					TypeDeclaration type = (TypeDeclaration) next;
					if (type.isInterface()) {
						interfaceCus.add(cu);
					}
				}
			}
		}
		return interfaceCus;
	}

}
