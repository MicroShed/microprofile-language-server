package com.microprofile.lsp.internal;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextDocumentSyncKind;

import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

import com.microprofile.lsp.internal.completion.MicroProfileCompletion;
import com.microprofile.lsp.internal.diagnostic.MicroProfileDiagnostic;
import com.microprofile.lsp.internal.helper.QuickFixHelper;
import com.microprofile.lsp.internal.helper.SourceAssistHelper;

public class MicroProfileLanguageServer implements LanguageServer, LanguageClientAware {

	private WorkspaceService workspaceService;
	static LanguageClient client;
	private WorkspaceFolder workspaceFolder;
	private File astRootFolder;
	private List<Either<Command, CodeAction>> codeActionsList = new ArrayList<>();

	public MicroProfileLanguageServer() {
		workspaceService = new MicroProfileWorkspaceService();
	}

	@Override
	public void connect(LanguageClient client) {
		this.client = client;
	}

	@Override
	public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
		logMessage(MessageType.Log, ">> initialize MicroProfile Language Server");

		/*
		 * Workspace Folder
		 */
		String name;
		try {
			name = Paths.get(new URI(params.getRootUri()).getPath()).getFileName().toString();
		} catch (Exception e) {
			name = "";
			logMessage(MessageType.Error, "Could not find a root workspace folder: " + e);

		}
		WorkspaceFolder workspaceFolder = new WorkspaceFolder();
		workspaceFolder.setName(name);
		workspaceFolder.setUri(params.getRootUri());
		logMessage(MessageType.Info, "workspaceFolder set: " + workspaceFolder.getUri());
		this.workspaceFolder = workspaceFolder;

		// initialize root of project to build ast from
		boolean javaDirExists = false;

		// if a src/main/java directory exists use this as root of project, otherwise
		// use workspace root
		try {
			URI uri = new URI(workspaceFolder.getUri());
			Stream<Path> walk = Files.walk(Paths.get(uri));
			List<String> results = walk.filter(Files::isDirectory).map(x -> x.toString()).collect(Collectors.toList());
			for (String result : results) {
				File dir = new File(result);
				if ((dir.getParentFile() != null) && (dir.getParentFile().getParentFile() != null)) {
					if (dir.getName().equals("java") && dir.getParentFile().getName().equals("main")
							&& dir.getParentFile().getParentFile().getName().equals("src")) {
						this.astRootFolder = dir;
						javaDirExists = true;
					}
				}
			}
		} catch (URISyntaxException | IOException e) {
			logMessage(MessageType.Error, "Unable to set astRootFolder: " + e.toString());
		}

		if (!javaDirExists) {
			this.astRootFolder = new File(workspaceFolder.getUri());
		}
		logMessage(MessageType.Info, "astRootFolder set: " + this.astRootFolder.getAbsolutePath());

		// add trigger characters for CompletionOptions
		CompletionOptions completionOptions = new CompletionOptions();
		List<String> triggerCharacters = new ArrayList<String>();
		triggerCharacters.add("@");

		completionOptions.setTriggerCharacters(triggerCharacters);
		final InitializeResult res = new InitializeResult(new ServerCapabilities());
		res.getCapabilities().setCodeActionProvider(Boolean.TRUE);
		res.getCapabilities().setCompletionProvider(completionOptions);
		res.getCapabilities().setDefinitionProvider(Boolean.TRUE); // enables go to type definition/go to definition
		res.getCapabilities().setHoverProvider(Boolean.TRUE);
		res.getCapabilities().setReferencesProvider(Boolean.TRUE);
		res.getCapabilities().setTextDocumentSync(TextDocumentSyncKind.Full);
		res.getCapabilities().setDocumentSymbolProvider(Boolean.TRUE);
		return CompletableFuture.supplyAsync(() -> res);
	}

	public CompletableFuture<Object> shutdown() {
		return CompletableFuture.supplyAsync(() -> Boolean.TRUE);
	}

	@Override
	public void exit() {
	}

	public TextDocumentService getTextDocumentService() {
		return fullTextDocumentService;
	}

	public WorkspaceService getWorkspaceService() {
		return this.workspaceService;
	}

	private FullTextDocumentService fullTextDocumentService = new FullTextDocumentService() {

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.microprofile.lsp.internal.FullTextDocumentService#completion(org.eclipse.
		 * lsp4j.CompletionParams)
		 */
		@Override
		public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams position) {
			logMessage(MessageType.Log, ">> document/completion");
			List<CompletionItem> completions = new ArrayList<>();

			new MicroProfileCompletion().updateCompletions(completions, position);

			CompletionItem typescriptCompletionItem = new CompletionItem();
			typescriptCompletionItem.setLabel("TypeScript");
			typescriptCompletionItem.setKind(CompletionItemKind.Text);
			typescriptCompletionItem.setData(1.0);

			CompletionItem javascriptCompletionItem = new CompletionItem();
			javascriptCompletionItem.setLabel("JavaScript");
			javascriptCompletionItem.setKind(CompletionItemKind.Text);
			javascriptCompletionItem.setData(2.0);

			completions.add(typescriptCompletionItem);
			completions.add(javascriptCompletionItem);

			return CompletableFuture.completedFuture(Either.forLeft(completions));
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.microprofile.lsp.internal.FullTextDocumentService#resolveCompletionItem(
		 * org.eclipse.lsp4j.CompletionItem)
		 */
		@Override
		public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem item) {
			logMessage(MessageType.Log, ">> document/resolveCompletionItem");
			if (item.getData().equals(1.0)) {
				item.setDetail("TypeScript details");
				item.setDocumentation("TypeScript documentation");
			} else if (item.getData().equals(2.0)) {
				item.setDetail("JavaScript details");
				item.setDocumentation("JavaScript documentation");
			}

			return CompletableFuture.completedFuture(item);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.microprofile.lsp.internal.FullTextDocumentService#didChange(org.eclipse.
		 * lsp4j.DidChangeTextDocumentParams)
		 */
		@Override
		public void didChange(DidChangeTextDocumentParams params) {
			logMessage(MessageType.Log, ">> document/didChange");
			super.didChange(params);
			List<Diagnostic> diagnostics = new ArrayList<>();
			TextDocumentItem document = this.documents.get(params.getTextDocument().getUri());
			File astRootFolder = getAstRootFolder();
			try {
				new MicroProfileDiagnostic().updateDiagnostics(document.getUri(), diagnostics, document, astRootFolder);
			} catch (BadLocationException e) {
				logMessage(MessageType.Error,
						"Unable to update diagnostics, BadLocationException caught: " + e.toString());

			}
			client.publishDiagnostics(new PublishDiagnosticsParams(document.getUri(), diagnostics));
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.microprofile.lsp.internal.FullTextDocumentService#codeAction(org.eclipse.
		 * lsp4j.CodeActionParams)
		 */
		@Override
		public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(CodeActionParams params) {
			logMessage(MessageType.Log, ">> document/codeAction");
			codeActionsList.clear();


			TextDocumentItem document = this.documents.get(params.getTextDocument().getUri());
			CodeActionContext context = params.getContext();

			List<Diagnostic> diagnostics = context.getDiagnostics();

			try {
				codeActionsList = QuickFixHelper.getCodeActionList(diagnostics, document, params);
			} catch (BadLocationException e) {
				logMessage(MessageType.Error,
						"Unable to generate code actions, BadLocationException caught: " + e.toString());
			}

			for (Either<Command, CodeAction> action : codeActionsList) {
				CodeAction codeAction = action.getRight();
				codeAction.setDiagnostics(diagnostics);
			}

			codeActionsList.addAll(SourceAssistHelper.getAssists(diagnostics, document));
			logMessage(MessageType.Info, "codeActionsList size: " + codeActionsList.size());
			return CompletableFuture.completedFuture(codeActionsList);
		}
	};

	/**
	 * Method to log message in client, log shows up in "Output" console in vscode
	 * 
	 * @param type    MessageType
	 * @param message Message to log
	 */
	public static void logMessage(MessageType type, String message) {
		MessageParams msg = new MessageParams(type, message);
		client.logMessage(msg);
	}

	public WorkspaceFolder getWorkspaceFolder() {
		return this.workspaceFolder;
	}

	public File getAstRootFolder() {
		return this.astRootFolder;
	}

}
