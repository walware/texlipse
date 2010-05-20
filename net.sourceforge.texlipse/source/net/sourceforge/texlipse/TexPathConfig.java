/*******************************************************************************
 * Copyright (c) 2008 TeXlipse-Team and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package net.sourceforge.texlipse;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.osgi.util.NLS;

public class TexPathConfig {
	
	
	public static final String TEXFILE_PATH_VARIABLE = "%texfile_path"; //$NON-NLS-1$
	public static final String TEXFILE_LOC_VARIABLE = "%texfile_loc"; //$NON-NLS-1$
	public static final String SOURCEFILE_PATH_VARIABLE = "%file_path"; //$NON-NLS-1$
	public static final String SOURCEFILE_LOC_VARIABLE = "%file_loc"; //$NON-NLS-1$
	
	private static final Pattern TEXFILE_PATH_PATTERN = Pattern.compile(Pattern.quote(TEXFILE_PATH_VARIABLE));
	private static final Pattern TEXFILE_LOC_PATTERN = Pattern.compile(Pattern.quote(TEXFILE_LOC_VARIABLE));
	private static final Pattern SOURCEFILE_PATH_PATTERN = Pattern.compile(Pattern.quote(SOURCEFILE_PATH_VARIABLE));
	private static final Pattern SOURCEFILE_LOC_PATTERN = Pattern.compile(Pattern.quote(SOURCEFILE_LOC_VARIABLE));
	
	
	public static IContainer resolveDirectory(String dir, IFile texFile, IFile sourceFile) throws CoreException {
		if (dir == null || dir.length() == 0) {
			return texFile.getParent();
		}
		dir = dir.trim();
		if (sourceFile == null) {
			sourceFile = texFile;
		}
		
		if (dir.indexOf('%') >= 0) {
			Matcher matcher;
			matcher = TEXFILE_PATH_PATTERN.matcher(dir);
			if (matcher.find()) {
				dir = matcher.replaceAll(Matcher.quoteReplacement(texFile.getFullPath().toString()));
			}
			matcher = TEXFILE_LOC_PATTERN.matcher(dir);
			if (matcher.find()) {
				dir = matcher.replaceAll(Matcher.quoteReplacement(texFile.getLocation().toString()));
			}
			matcher = SOURCEFILE_PATH_PATTERN.matcher(dir);
			if (matcher.find()) {
				dir = matcher.replaceAll(Matcher.quoteReplacement(sourceFile.getFullPath().toString()));
			}
			matcher = SOURCEFILE_LOC_PATTERN.matcher(dir);
			if (matcher.find()) {
				dir = matcher.replaceAll(Matcher.quoteReplacement(sourceFile.getLocation().toString()));
			}
		}
		
		if (dir.indexOf('$') >= 0) {
			final IStringVariableManager variableManager = VariablesPlugin.getDefault().getStringVariableManager();
			try {
				dir = variableManager.performStringSubstitution(dir, true);
			}
			catch (CoreException e) {
				throw new CoreException(new Status(IStatus.ERROR, Texlipse.PLUGIN_ID, -1,
						NLS.bind("Invalid directory ''{0}'': variable error", dir), e));
			}
		}
		
		IPath path = new Path(dir);
		if (!path.isAbsolute()) {
			return sourceFile.getParent().getFolder(path);
		}
		
		// Search best path in workspace
		IContainer container = ResourcesPlugin.getWorkspace().getRoot().getContainerForLocation(path);
		if (container != null) {
			return container;
		}
		IContainer[] containers = ResourcesPlugin.getWorkspace().getRoot().findContainersForLocation(path);
		if (containers.length > 0) {
			if (containers.length > 1) {
				for (int i = 0; i < containers.length; i++) {
					if (containers[i].getProject().equals(sourceFile.getProject())) {
						return containers[i];
					}
				}
				for (int i = 0; i < containers.length; i++) {
					if (containers[i].getProject().equals(texFile.getProject())) {
						return containers[i];
					}
				}
			}
			return containers[0];
		}
		else {
			throw new CoreException(new Status(IStatus.ERROR, Texlipse.PLUGIN_ID, -1,
					NLS.bind("Invalid directory ''{0}'': not in workspace.", path.toOSString()), null));
		}
	}
	
	
	private IFile fTexFile;
	private String fOutputFormat;
	private IFile fOutputFile;
	
	
	public TexPathConfig(final IFile texFile, IContainer outputDir, final String outputFormat) {
		assert (texFile != null);
		assert (outputFormat != null);
		
		fTexFile = texFile;
		fOutputFormat = outputFormat.toLowerCase();
		
		String name = texFile.getName();
		int dot = name.lastIndexOf(".");
		if (dot >= 0) {
			name = name.substring(0, dot);
		}
		if (outputDir == null) {
			outputDir = texFile.getParent();
		}
		fOutputFile = outputDir.getFile(new Path(name+'.'+getOutputFormat()));
	}
	
	
	public IFile getTexFile() {
		return fTexFile;
	}
	
	public String getOutputFormat() {
		return fOutputFormat;
	}
	
	public IFile getOutputFile() {
		return fOutputFile;
	}
	
}
