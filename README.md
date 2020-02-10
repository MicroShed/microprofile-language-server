
# MicroProfile Language Server

MicroProfile language server implementation for Java files.

## Clients

This repository only contains the server implementation. The following clients consume this server:

- [MicroProfile Language Support for VS Code](https://github.com/MicroShed/microprofile-lsp-client-vscode)

## To Build

1. Compile and build jar.
```
mvn clean package
```
2. Find the packaged jar in the `target` directory.
2. Follow the build instructions for the client that you are building to consume the above jar.

## Language Capabilities

#### Diagnostic warnings 
- **MP Health**: `Liveness`/`Readiness`/`Health` annotation, `HealthCheck` interface implementation
  - Quick fix
- **MP Rest Client and CDI**: `RegisterRestClient` annotation, CDI injection with `Inject` annotation and `RestClient` annotation 
  - Quick fix
  	- Limitation: cross file validation searches for files under the assumption that files exist under `src/main/java` directory, otherwise searches all files under workspace root.

#### Code complete suggestions with additional info
- **MP Health**: `Liveness`/`Readiness`/`Health` annotation (appears whenever the language server is enabled, can be improved to be based on dependency on classpath)

#### Source actions
- **MP OpenAPI**: Right-click in a class to generate `Operation` OpenAPI annotation via source actions
	- If there is already an `Operation` annotation on the current method, the `Generate OpenAPI Annotations` option will not appear.
	- An `Operation` annotation will be generated for any number of methods in a single class with a Response return type who do not already have an `Operation` annotation.

#### Code snippets with fields that you can fill in
- **MP OpenAPI**: OpenAPI `Operation` and `Parameter` annotations

#### Logging
- To log, see `logMessage()` method in `MicroProfileLanguageServer.java`
