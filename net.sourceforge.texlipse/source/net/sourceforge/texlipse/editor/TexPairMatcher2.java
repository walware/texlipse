/*******************************************************************************
 * Copyright (c) 2007-2008 TeXlipse-Team and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package net.sourceforge.texlipse.editor;

import org.eclipse.jface.text.IDocument;

import de.walware.ecommons.text.ITokenScanner;
import de.walware.ecommons.ui.text.PairMatcher;


/**
 * A pair finder class for implementing the pair matching.
 */
public class TexPairMatcher2 extends PairMatcher {
	
	
	public static final char[][] BRACKETS = { {'{', '}'}, {'(', ')'}, {'[', ']'} };
	
	
	public TexPairMatcher2() {
		this(new TexHeuristicTokenScanner());
	}
	
	public TexPairMatcher2(final ITokenScanner scanner) {
		super(BRACKETS, ITexDocumentConstants.TEX_PARTITIONING, new String[] {
				IDocument.DEFAULT_CONTENT_TYPE,
				ITexDocumentConstants.TEX_MATH_CONTENT_TYPE,
				ITexDocumentConstants.TEX_VERBATIM,
		}, scanner, '\\');
	}
	
	
}
