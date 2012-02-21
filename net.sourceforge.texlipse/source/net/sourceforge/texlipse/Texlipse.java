package net.sourceforge.texlipse;

import net.sourceforge.texlipse.viewer.ViewerManager2;


/**
 * Public constants and services
 */
public class Texlipse {
	
	
	public static final String PLUGIN_ID = "net.sourceforge.texlipse"; //$NON-NLS-1$
	
	
	public static ViewerManager2 getViewerManager() {
		return TexlipsePlugin.getDefault().getViewerManager2();
	}
	
}
