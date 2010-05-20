/* 
 * Copyright (c) 2008 by the TeXlipse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package net.sourceforge.texlipse.builder;

import net.sourceforge.texlipse.properties.TexlipseProperties;

/**
 * Run the external Xelatex program.
 * 
 * @author Boris von Loesch
 */
public class XelatexRunner extends LatexRunner {

    /**
     * Create a new ProgramRunner.
     */
    public XelatexRunner() {
		super(BuilderRegistry.XELATEX_RUNNER_ID);
    }
    
    protected String getWindowsProgramName() {
		return "xelatex.exe"; //$NON-NLS-1$
    }
    
    protected String getUnixProgramName() {
		return "xelatex"; //$NON-NLS-1$
    }
    
    public String getDescription() {
        return "XeLatex program";
    }
    
    /**
     * @return output file format (pdf)
     */
    public String getOutputFormat() {
        return TexlipseProperties.OUTPUT_FORMAT_PDF;
    }

}
