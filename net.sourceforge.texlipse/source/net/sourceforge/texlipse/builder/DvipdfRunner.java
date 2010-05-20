/*
 * $Id: DvipdfRunner.java,v 1.2 2006/04/04 22:14:05 borisvl Exp $
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.builder;

import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.PlatformUI;


/**
 * Run the external dvipdf program.
 * 
 * @author Kimmo Karlsson
 * @author Boris von Loesch
 */
public class DvipdfRunner extends AbstractProgramRunner {

    public DvipdfRunner() {
		super(BuilderRegistry.DVIPDF_RUNNER_ID);
    }

    protected String getWindowsProgramName() {
		return "dvipdfm.exe"; //$NON-NLS-1$
    }

    protected String getUnixProgramName() {
		return "dvipdf"; //$NON-NLS-1$
    }

    public String getDescription() {
        return "Dvipdf program";
    }

    public String getInputFormat() {
        return TexlipseProperties.OUTPUT_FORMAT_DVI;
    }

    public String getOutputFormat() {
        return TexlipseProperties.OUTPUT_FORMAT_PDF;
    }

    /**
     * Parse the output of dvipdf program.
     * 
     * @param resource the input file that was processed
     * @param output the output of the external program
     * @return true, if error messages were found in the output, false otherwise
     */
    protected boolean parseErrors(IResource resource, String output) {
        if (output.indexOf("Unable to open ") >= 0) {
            PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
                public void run() {
                    MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), 
                            "Error", "Unable to create the pdf file. Please close all pdf viewers!");
                }
            });
            return true;
        }
        //TODO: more dvipdf error parsing
        return false;
    }
}
