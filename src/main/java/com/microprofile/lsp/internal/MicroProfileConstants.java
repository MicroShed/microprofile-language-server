package com.microprofile.lsp.internal;

import org.eclipse.jdt.core.compiler.IProblem;

/**
 * Class for MicroProfile defined constants
 *
 */
public final class MicroProfileConstants {

	// diagnostic codes for MicroProfile
	public static final int MicroProfile = 0x00100000;
	// code for @Health annotation
	public static final int MicroProfileHealthAnnotation = IProblem.Internal + MicroProfile + 1;
	// code for implements HealthCheck
	public static final int MicroProfileHealthCheck = IProblem.Internal + MicroProfile + 2;

	// code for @Inject annotation
	public static final int MicroProfileInjectAnnotation = IProblem.Internal + MicroProfile + 3;
	// code for @RestClient annotation
	public static final int MicroProfileRestClientAnnotation = IProblem.Internal + MicroProfile + 4;
	// code for @Inject and @RestClient annotation
	public static final int MicroProfileInjectRestClientAnnotation = IProblem.Internal + MicroProfile + 5;
	
	// completion item codes for MicroProfile
	public static final int HealthAnnotationCompletion = IProblem.Internal + MicroProfile + 6;
	
}