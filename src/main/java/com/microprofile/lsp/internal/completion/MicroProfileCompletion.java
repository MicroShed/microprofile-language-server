package com.microprofile.lsp.internal.completion;

import java.util.List;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionParams;


import com.microprofile.lsp.internal.MicroProfileConstants;

public class MicroProfileCompletion {

	public void updateCompletions(List<CompletionItem> completions, CompletionParams position) {

		// Health annotation completion item
		if (position.getContext().getTriggerCharacter().equals("@")) {
			/**
			 * MicroProfile Health
			 */
			CompletionItem healthAnnotation = new CompletionItem();
			
			healthAnnotation.setLabel("Health - org.eclipse.microprofile.health");
			healthAnnotation.setKind(CompletionItemKind.Class);
			healthAnnotation.setInsertText("Health");
			healthAnnotation.setData(MicroProfileConstants.HealthAnnotationCompletion);
			healthAnnotation.setDetail("The @Health annotation allows this bean to be discovered automatically when the http://HOST:PORT/health endpoint receives a request.");
			healthAnnotation.setSortText(" " + healthAnnotation.getSortText());
			completions.add(healthAnnotation);
			
			/**
			 * MicroProfile OpenAPI
			 */
			
			// @Operation 
			CompletionItem operationAnnotation = new CompletionItem();
			operationAnnotation.setLabel("Operation - org.eclipse.microprofile.openapi.annotations");
			operationAnnotation.setKind(CompletionItemKind.Class);
			operationAnnotation.setInsertText("Operation(\n" + "        summary=\"\",\n" + "        description=\"\"\n" +
					"    )");
			completions.add(operationAnnotation);
			
			// @Parameter
			CompletionItem parameterAnnotation = new CompletionItem();
			parameterAnnotation.setLabel("Parameter - org.eclipse.microprofile.openapi.annotations.parameters");
			parameterAnnotation.setKind(CompletionItemKind.Class);
			parameterAnnotation.setInsertText("Parameter(\n" + "        description = \"\"\n" + "    )");
			completions.add(parameterAnnotation);
			
		}
	}

}
