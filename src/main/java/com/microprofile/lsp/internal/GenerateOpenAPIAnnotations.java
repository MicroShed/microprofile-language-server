/*******************************************************************************
* Copyright (c) 2018 Microsoft Corporation and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Microsoft Corporation - initial API and implementation
*******************************************************************************/

package com.microprofile.lsp.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextEdit;

public class GenerateOpenAPIAnnotations {
	private final static int generateVisibility = Modifier.PUBLIC;
	private final static boolean generateComments = true;

	private final TypeDeclaration type;
	private CompilationUnit astRoot;

	public GenerateOpenAPIAnnotations(TypeDeclaration type, CompilationUnit astRoot) {
		Assert.isNotNull(type);
		this.type = type;
		this.astRoot = astRoot;
	}
	
	
	
	/**
	 * Generate the OpenAPI annotations given a TextDocumentItem Currently only
	 * generates Operation annotation 
	 * 
	 * @param document TextDocumentItem
	 * @return list of TextEdits
	 */
	public List<TextEdit> generateAnnotations(TextDocumentItem document) {
		List<TextEdit> edits = new ArrayList<>();

		Range range = new Range();
		AST ast = this.astRoot.getAST();
		final ASTRewrite astRewrite = ASTRewrite.create(astRoot.getAST());

		ListRewrite listRewriter = null;
		final AbstractTypeDeclaration declaration = type;
		listRewriter = astRewrite.getListRewrite(declaration, declaration.getBodyDeclarationsProperty());

		// if the node selected is a method declaration with Response return type add
		// Operation annotation
		List<ASTNode> nodes = listRewriter.getOriginalList();
		List<ASTNode> responseReturnNodes = new ArrayList<>();

		for (ASTNode node : nodes) {
			boolean operationFlag = false;
			if (node.getNodeType() == node.METHOD_DECLARATION) {
				// check if the return object is a Response
				MethodDeclaration methodNode = (MethodDeclaration) node;

				if (methodNode.getReturnType2().toString().equals("Response")) {
					List param = methodNode.parameters();

					List modifiers = methodNode.modifiers();
					for (Iterator iter = modifiers.iterator(); iter.hasNext();) {
						Object next = iter.next();
						if (next instanceof IExtendedModifier) {
							IExtendedModifier modifier = (IExtendedModifier) next;
							if (modifier.isAnnotation()) {
								Annotation annotation = (Annotation) modifier;
								MicroProfileLanguageServer.logMessage(MessageType.Info,
										"annotation: " + annotation.getTypeName());
								if (annotation.getTypeName().toString().equals("Operation")) {
									operationFlag = true;
									break;
								}
							}
						}
					}

					if (!operationFlag) {
						responseReturnNodes.add(node);
					}

				}
			}

		}
		MicroProfileLanguageServer.logMessage(MessageType.Info, "responseReturnNodes " + responseReturnNodes.size());

		for (ASTNode node : responseReturnNodes) {
			try {
				range = Utils.toRange(node.getStartPosition(), 0, document.getText());
				if (range != null) {
					StringBuilder buf = new StringBuilder();
					buf.append("@Operation(\n" + "        summary=\"\",\n" + "        description=\"\"\n" + "    )\n");
					String stub = buf.toString();

					TextEdit textEdit = new TextEdit(range, stub);
					edits.add(textEdit);
				}
			} catch (BadLocationException e) {
				MicroProfileLanguageServer.logMessage(MessageType.Error, "BadLocationException " + e);
			}

		}
		
		return edits;
	}

}