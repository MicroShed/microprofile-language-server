package com.microprofile.lsp.internal.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import com.microprofile.lsp.internal.MicroProfileConstants;
import com.microprofile.lsp.internal.Utils;

public class QuickFixHelper {

	public static List<Either<Command, CodeAction>> codeActionsList = new ArrayList<>();

	/**
	 * Takes in diagnostics and computes quick fix code actions
	 * 
	 * @param diagnostics
	 * @param document
	 * @return
	 * @throws BadLocationException
	 */
	public static List<Either<Command, CodeAction>> getCodeActionList(List<Diagnostic> diagnostics,
			TextDocumentItem document, CodeActionParams params) throws BadLocationException {
		if (diagnostics != null) {

			for (Diagnostic diagnostic : diagnostics) {
				if (diagnostic.getCode() == null) {
					continue;
				}

				/*
				 * MicroProfile Rest Client
				 */
				// add @Inject
				if (diagnostic.getCode().equals(Integer.toString(MicroProfileConstants.MicroProfileInjectAnnotation))) {
					String title = "Add missing @Inject annotation";
					Range editRange = new Range(new Position(diagnostic.getRange().getStart().getLine(), 0),
							new Position(diagnostic.getRange().getStart().getLine(), 0));
					String editStr = "@Inject\n";
					createCodeAction(title, editRange, editStr, document);
				}

				// add @RestClient
				if (diagnostic.getCode()
						.equals(Integer.toString(MicroProfileConstants.MicroProfileRestClientAnnotation))) {

					String title = "Add missing @RestClient annotation";
					Range editRange = new Range(new Position(diagnostic.getRange().getStart().getLine(), 0),
							new Position(diagnostic.getRange().getStart().getLine(), 0));
					String editStr = "@RestClient\n";
					createCodeAction(title, editRange, editStr, document);

				}

				// add @RestClient and @Inject
				if (diagnostic.getCode()
						.equals(Integer.toString(MicroProfileConstants.MicroProfileInjectRestClientAnnotation))) {
					String title = "Add missing @Inject and @RestClient annotations";
					Range editRange = new Range(new Position(diagnostic.getRange().getStart().getLine(), 0),
							new Position(diagnostic.getRange().getStart().getLine(), 0));
					String editStr = "@Inject\n  @RestClient\n";
					createCodeAction(title, editRange, editStr, document);

				}

				/*
				 * MicroProfile Health
				 */
				if (diagnostic.getCode().equals(Integer.toString(MicroProfileConstants.MicroProfileHealthAnnotation))) {
					// add @Liveness
					String title = "Add missing @Liveness annotation";
					Range editRange = new Range(new Position(diagnostic.getRange().getStart().getLine(), 0),
							new Position(diagnostic.getRange().getStart().getLine(), 0));
					String editStr = "@Liveness \n";
					createCodeAction(title, editRange, editStr, document);

					// add @Readiness
					title = "Add missing @Readiness annotation";
					editStr = "@Readiness \n";
					createCodeAction(title, editRange, editStr, document);

					// add @Health
					title = "Add missing @Health annotation";
					editStr = "@Health \n";
					createCodeAction(title, editRange, editStr, document);
				}

				// add HealthCheck interface
				if (diagnostic.getCode().equals(Integer.toString(MicroProfileConstants.MicroProfileHealthCheck))) {
					// quick fix code action
					String title = "Add missing HealthCheck interface";
					String editStr = " implements HealthCheck";

					Range editRange = new Range();
					CompilationUnit cu = Utils.toCompilationUnit(document);
					List types = cu.types();

					for (Iterator iter = types.iterator(); iter.hasNext();) {
						Object next = iter.next();
						if (next instanceof TypeDeclaration) {

							TypeDeclaration type = (TypeDeclaration) next;
							try {
								editRange = Utils.toRange(
										type.getName().getStartPosition() + type.getName().getLength(), 0,
										document.getText());
							} catch (BadLocationException e) {
								e.printStackTrace();
							}
						}
					}
					createCodeAction(title, editRange, editStr, document);
				}
			}
		}
		return codeActionsList;

	}

	/**
	 * Creates a quick fix code action given a title, range, edit and document
	 * 
	 * @param title     text that appears in title of quick fix
	 * @param editRange where to insert quick fix edit
	 * @param editStr   text for edit of quick fix
	 * @param document  current TextDocumentItem
	 */
	private static void createCodeAction(String title, Range editRange, String editStr, TextDocumentItem document) {
		CodeAction codeAction = new CodeAction(title);
		codeAction.setKind(CodeActionKind.QuickFix);
		HashMap<String, List<TextEdit>> changes = new HashMap<>();
		List<TextEdit> edits = createTextEdit(editRange, editStr);
		changes.put(document.getUri(), edits);

		WorkspaceEdit editWork = new WorkspaceEdit();
		editWork.setChanges(changes);

		codeAction.setEdit(editWork);

		// add to codeActions
		Optional<Either<Command, CodeAction>> codeActionFromProposal = Optional.of(Either.forRight(codeAction));
		codeActionsList.add(codeActionFromProposal.get());
	}

	/**
	 * Takes in a Range and String, returns a List of TextEdits To be used when
	 * creating code actions
	 * 
	 * @param range
	 * @param editStr
	 * @return
	 */
	private static List<TextEdit> createTextEdit(Range range, String editStr) {
		TextEdit editText = new TextEdit(range, editStr);
		List<TextEdit> edits = new ArrayList<>();
		edits.add(editText);

		return edits;
	}
}
