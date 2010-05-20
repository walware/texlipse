/*
 * $Id: TexDocumentSetupParticipant.java,v 1.2 2006/05/02 18:35:48 borisvl Exp $
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package net.sourceforge.texlipse.editor;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;

/**
 * 
 * @author Antti Pirinen
 */
public class TexDocumentSetupParticipant implements IDocumentSetupParticipant {
	
	
	public TexDocumentSetupParticipant() {
	}
	
	
	public void setup(final IDocument document) {
		if (document instanceof IDocumentExtension3) {
			final IDocumentExtension3 extension3= (IDocumentExtension3) document;
			
			final IDocumentPartitioner partitioner = new FastPartitioner(
							new TexFastPartitionScanner(),
							ITexDocumentConstants.TEX_PARTITION_TYPES);
			partitioner.connect(document);
			
			extension3.setDocumentPartitioner(ITexDocumentConstants.TEX_PARTITIONING, partitioner);
		}
	}
	
}
