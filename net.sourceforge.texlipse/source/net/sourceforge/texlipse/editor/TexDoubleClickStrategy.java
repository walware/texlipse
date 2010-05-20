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
import org.eclipse.jface.text.DefaultTextDoubleClickStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextUtilities;

import de.walware.ecommons.text.BasicHeuristicTokenScanner;
import de.walware.ecommons.text.PairMatcher;


/**
 * If matching pairs found, selection of content inside matching brackets,
 * otherwise default word selection.
 */
public class TexDoubleClickStrategy extends DefaultTextDoubleClickStrategy {
	
	
	private final String fPartitioning;
	private PairMatcher fPairMatcher;
	private BasicHeuristicTokenScanner fScanner;
	
	
	public TexDoubleClickStrategy() {
		this(ITexDocumentConstants.TEX_PARTITIONING);
	}
	
	public TexDoubleClickStrategy(final String partitioning) {
		super();
		fPartitioning = partitioning;
		fScanner = new TexHeuristicTokenScanner();
		fPairMatcher = new TexPairMatcher2(fScanner);
	}
	
	
	@Override
	public void doubleClicked(final ITextViewer textViewer) {
		
		final int offset = textViewer.getSelectedRange().x;
		
		if (offset < 0)
			return;
		
		final IDocument document = textViewer.getDocument();
		try {
			ITypedRegion partition = TextUtilities.getPartition(document, fPartitioning, offset, true);
			String type = partition.getType();
			
			// Bracket-Pair-Matching in Code-Partitions
			if (ITexDocumentConstants.TEX_DEFAULT_CONTENT_TYPE.equals(type)
					|| ITexDocumentConstants.TEX_DEFAULT_EXPL_CONTENT_TYPE.equals(type)) {
				final IRegion region = fPairMatcher.match(document, offset);
				if (region != null && region.getLength() >= 2) {
					textViewer.setSelectedRange(region.getOffset() + 1, region.getLength() - 2);
					return;
				}
			}
			
			// For other partitions, use prefere new partitions (instead opened)
			partition = TextUtilities.getPartition(document, fPartitioning, offset, false);
			type = partition.getType();
			
			if (ITexDocumentConstants.TEX_MATH_CONTENT_TYPE.equals(type)) {
				final int partitionOffset = partition.getOffset();
				final int partitionEnd = partitionOffset + partition.getLength();
				if (partitionEnd - partitionOffset >= 4 && (
						offset == partitionOffset || offset == partitionOffset+1
						|| offset == partitionEnd || offset == partitionEnd-1)) {
					final char c0 = document.getChar(partitionOffset);
					final char c1 = document.getChar(partitionOffset+1);
					int start = -1;
					char[] endPattern = null;
					if (c0 == '$') {
						if (c1 == '$') {
							start = partitionOffset + 2;
							endPattern = "$$".toCharArray();
						}
						else {
							start = partitionOffset + 1;
							endPattern = "$".toCharArray();
						}
					}
					else if (c0 == '\\') {
						if (c1 == '[') {
							start = partitionOffset + 2;
							endPattern = "\\]".toCharArray();
						}
						else if (c1 == '(') {
							start = partitionOffset + 2;
							endPattern = "\\)".toCharArray();
						}
					}
					if (start >= 0) {
						textViewer.setSelectedRange(start, getEndOffset(document, partitionEnd, endPattern) - start);
					}
				} else {
					IRegion region = fPairMatcher.match(document, offset);
					if (region != null && region.getLength() >= 2) {
						textViewer.setSelectedRange(region.getOffset() + 1, region.getLength() - 2);
						return;
					}
					fScanner.configure(document);
					region = fScanner.findCommonWord(offset);
					if (region != null) {
						textViewer.setSelectedRange(region.getOffset(), region.getLength());
					}
					else {
						textViewer.setSelectedRange(offset, 0);
					}
				}
				return;
			}
			if (ITexDocumentConstants.TEX_VERBATIM.equals(type)) {
				final int partitionOffset = partition.getOffset();
				final int partitionEnd = partitionOffset + partition.getLength();
				final int start = partitionOffset+6;
				if (partitionEnd - partitionOffset >= 7 && (
						offset == start-1 || offset == start
						|| offset == partitionEnd || offset == partitionEnd-1)) {
					final String text = document.get(partitionOffset, 7);
					if (text.startsWith("\\verb")) {
						textViewer.setSelectedRange(start,
								getEndOffset(document, partitionEnd, new char[] { text.charAt(5) }) - start);
					}
					return;
				}
			}
			
			super.doubleClicked(textViewer);
			return;
		} catch (final BadLocationException e) {
		} catch (final NullPointerException e) {
		}
		// else
		textViewer.setSelectedRange(offset, 0);
	}
	
	private int getEndOffset(final IDocument document, int end, final char[] endPattern) throws BadLocationException {
		int i = endPattern.length-1;
		while (--end >= 0 && i >= 0) {
			if (document.getChar(end) != endPattern[i--]) {
				break;
			}
		}
		return end+1;
	}
	
}
