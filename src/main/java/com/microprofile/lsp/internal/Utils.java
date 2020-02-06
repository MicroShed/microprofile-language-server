package com.microprofile.lsp.internal;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultLineTracker;
import org.eclipse.jface.text.ILineTracker;
import org.eclipse.jface.text.IRegion;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentItem;

public final class Utils {

	private Utils() {

	}

	/**
	 * Given a TextDocumentItem creates an ASTParser and CompilationUnit
	 * 
	 * @param document TextDocumentItem
	 * @return CompilationUnit of current document
	 */
	public static CompilationUnit toCompilationUnit(TextDocumentItem document) {
		if (document != null) {
			return toCompilationUnit(document.getUri(), document.getText().toString());
		} else {
			return null;
		}
	}

	/**
	 * Given a uri and source text create an ASTParser and Compilation Unit
	 * 
	 * @param docURI  String
	 * @param docText String
	 * @return CompilationUnit of current document
	 */
	public static CompilationUnit toCompilationUnit(String docURI, String docText) {
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		Map<String, String> options = JavaCore.getOptions();
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_7, options);
		parser.setCompilerOptions(options);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setStatementsRecovery(true);
		parser.setBindingsRecovery(true);
		parser.setResolveBindings(true);
		String unitName = docURI.substring(docURI.lastIndexOf("/"));
		parser.setUnitName(unitName);
		parser.setSource(docText.toCharArray());

		CompilationUnit cu = (CompilationUnit) parser.createAST(null); // cu is not null
		return cu;
	}

	/**
	 * 
	 * Calculates Range object from offset, length, and document as String
	 * 
	 * @param startPosition offset of startPosition
	 * @param length        length of string
	 * @param text          current document as String
	 * @return range Range object to be used in diagnostic warnings
	 * @throws BadLocationException exception thrown
	 */
	public static Range toRange(int startPosition, int length, String text) throws BadLocationException {
		ILineTracker lineTracker = new DefaultLineTracker();
		lineTracker.set(text);

		int lineNumber = lineTracker.getLineNumberOfOffset(startPosition);
		IRegion region = lineTracker.getLineInformation(lineNumber);
		int diff = startPosition - region.getOffset();

		Range range = new Range(new Position(lineNumber, diff), new Position(lineNumber, diff + length));
		return range;
	}
	
	/**
	 * toOffset
	 * 
	 * @param position Position
	 * @param text     String
	 * @return Integer
	 * @throws BadLocationException exception thrown
	 */
	public static int toOffset(Position position, String text) throws BadLocationException {
		ILineTracker lineTracker = new DefaultLineTracker();
		lineTracker.set(text);

		IRegion region = lineTracker.getLineInformation(position.getLine());

		int lineStart = region.getOffset();
		return lineStart + position.getCharacter();

	}

	public static URI toURI(String uriString) {
		if (uriString == null || uriString.isEmpty()) {
			return null;
		}
		try {
			URI uri = new URI(uriString);
			if (Platform.OS_WIN32.equals(Platform.getOS()) && URIUtil.isFileURI(uri)) {
				uri = URIUtil.toFile(uri).toURI();
			}
			return uri;
		} catch (URISyntaxException e) {
			return null;
		}
	}

}
