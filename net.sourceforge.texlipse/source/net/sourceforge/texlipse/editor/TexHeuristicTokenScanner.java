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

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextUtilities;

import de.walware.ecommons.text.BasicHeuristicTokenScanner;
import de.walware.ecommons.text.IPartitionConstraint;


/**
 *
 */
public class TexHeuristicTokenScanner extends BasicHeuristicTokenScanner {
	
	
	public TexHeuristicTokenScanner() {
		super(ITexDocumentConstants.TEX_PARTITIONING_CONFIG);
	}
	
	
	@Override
	protected int createForwardBound(final int start) throws BadLocationException {
		final IPartitionConstraint matcher = getPartitionConstraint();
		if (matcher.matches(ITexDocumentConstants.TEX_DEFAULT_CONTENT_TYPE)) {
			return UNBOUND;
		}
		final ITypedRegion partition = TextUtilities.getPartition(fDocument, getPartitioning(), start, false);
		return partition.getOffset()+partition.getLength();
	}
	
	@Override
	protected int createBackwardBound(final int start) throws BadLocationException {
		final IPartitionConstraint matcher = getPartitionConstraint();
		if (matcher.matches(ITexDocumentConstants.TEX_DEFAULT_CONTENT_TYPE)) {
			return -1;
		}
		final ITypedRegion partition = TextUtilities.getPartition(fDocument, getPartitioning(), start, false);
		return partition.getOffset();
	}
	
}
