/*******************************************************************************
 * Copyright (c) 2004-2008 TeXlipse-Team and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package net.sourceforge.texlipse.editor;

import org.eclipse.jface.text.IDocument;

import de.walware.ecommons.text.IPartitionConstraint;
import de.walware.ecommons.text.PartitioningConfiguration;


public interface ITexDocumentConstants {
	
	public final static String TEX_PARTITIONING = "__tex_partitioning"; //$NON-NLS-1$
	
	public final static String TEX_DEFAULT_CONTENT_TYPE = IDocument.DEFAULT_CONTENT_TYPE;
	public final static String TEX_DEFAULT_EXPL_CONTENT_TYPE = "__tex_default"; //$NON-NLS-1$
	public final static String TEX_COMMENT_CONTENT_TYPE = "__tex_commentPartition"; //$NON-NLS-1$
	public static final String TEX_MATH_CONTENT_TYPE = "__tex_mathPartition"; //$NON-NLS-1$
	public static final String TEX_VERBATIM = "__tex_VerbatimPartition"; //$NON-NLS-1$
//	public static final String TEX_CURLY_BRACKETS = "__tex_curlyBracketPartition"; //$NON-NLS-1$
//	public static final String TEX_SQUARE_BRACKETS = "__tex_squareBracketPartition"; //$NON-NLS-1$
	
	public static final String[] TEX_PARTITION_TYPES = new String[] {
			TEX_DEFAULT_CONTENT_TYPE,
			TEX_COMMENT_CONTENT_TYPE,
			TEX_MATH_CONTENT_TYPE,
			TEX_VERBATIM,
	};
	
	public static final PartitioningConfiguration TEX_PARTITIONING_CONFIG = new PartitioningConfiguration(
			TEX_PARTITIONING, new IPartitionConstraint() {
				public boolean matches(String partitionType) {
					return (partitionType == TEX_DEFAULT_CONTENT_TYPE
							|| partitionType == TEX_DEFAULT_EXPL_CONTENT_TYPE);
				}
			});
	
}
