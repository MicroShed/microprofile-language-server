package com.microprofile.lsp.internal.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ResourceOperation;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import com.microprofile.lsp.internal.GenerateOpenAPIAnnotations;
import com.microprofile.lsp.internal.Utils;

import org.eclipse.lsp4j.TextEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class SourceAssistHelper {

	public static List<Either<Command, CodeAction>> getAssists(List<Diagnostic> diagnostics,
			TextDocumentItem document) {

		final String SOURCE_GENERATE = CodeActionKind.Source + ".generate";
		List<Either<Command, CodeAction>> sourceAssistsList = new ArrayList<>();

		// Range editRange = new Range(new Position(0,0), new Position(0,10));

		// generate OpenAPI annotations source action
		CodeAction openAPIAnnotations = new CodeAction("Generate OpenAPI Annotations");
		openAPIAnnotations.setKind(CodeActionKind.Source);

		// get compilation unit and type
		CompilationUnit cu = Utils.toCompilationUnit(document);
		List types = cu.types();

		List<TextEdit> edits = new ArrayList<>();

		for (Iterator iter = types.iterator(); iter.hasNext();) {
			Object next = iter.next();
			if (next instanceof TypeDeclaration) {

				TypeDeclaration type = (TypeDeclaration) next;
				GenerateOpenAPIAnnotations operation = new GenerateOpenAPIAnnotations(type, cu);
				edits.addAll(operation.generateAnnotations(document));
			}
		}

		HashMap<String, List<TextEdit>> changes = new HashMap<>();
		changes.put(document.getUri(), edits);

		WorkspaceEdit editWork = new WorkspaceEdit();
		editWork.setChanges(changes);

		openAPIAnnotations.setEdit(editWork);

		// add to codeActions
		Optional<Either<Command, CodeAction>> codeActionFromProposal = Optional.of(Either.forRight(openAPIAnnotations));
		sourceAssistsList.add(codeActionFromProposal.get());
		return sourceAssistsList;

	}
}
