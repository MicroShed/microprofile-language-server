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
			healthAnnotation.setSortText(" 3" + healthAnnotation.getSortText());
			completions.add(healthAnnotation);

			CompletionItem livenessAnnotation = new CompletionItem();
			
			livenessAnnotation.setLabel("Liveness - org.eclipse.microprofile.health");
			livenessAnnotation.setKind(CompletionItemKind.Class);
			livenessAnnotation.setInsertText("Liveness");
			livenessAnnotation.setData(MicroProfileConstants.LivenessAnnotationCompletion);
			livenessAnnotation.setDetail("The @Liveness annotation indicates that this bean is a liveness health check procedure.");
			livenessAnnotation.setSortText(" 1" + livenessAnnotation.getSortText());
			completions.add(livenessAnnotation);

			CompletionItem readinessAnnotation = new CompletionItem();
			
			readinessAnnotation.setLabel("Readiness - org.eclipse.microprofile.health");
			readinessAnnotation.setKind(CompletionItemKind.Class);
			readinessAnnotation.setInsertText("Readiness");
			readinessAnnotation.setData(MicroProfileConstants.ReadinessAnnotationCompletion);
			readinessAnnotation.setDetail("The @Readiness annotation indicates that this bean is a readiness health check procedure.");
			readinessAnnotation.setSortText(" 2" + readinessAnnotation.getSortText());
			completions.add(readinessAnnotation);
			
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
