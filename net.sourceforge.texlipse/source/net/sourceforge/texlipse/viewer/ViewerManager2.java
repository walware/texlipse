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

package net.sourceforge.texlipse.viewer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;

import net.sourceforge.texlipse.PathUtils;
import net.sourceforge.texlipse.TexPathConfig;
import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.properties.TexlipseProperties;
import net.sourceforge.texlipse.viewer.util.ViewerErrorScanner;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationListener;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import de.walware.ecommons.debug.core.util.LaunchUtils;
import de.walware.ecommons.io.win.DDE;
import de.walware.ecommons.io.win.DDEClient;

public class ViewerManager2 implements ILaunchConfigurationListener {
	
	
	private static interface ProcessRunnable {
		
		boolean check(ViewerConfiguration config);
		void found(ViewerConfiguration config, String name);
		
	}
	
	
	private int fDebugMode;
	
	private ILaunchConfigurationType fConfigType;
	private Map<ILaunchConfiguration, ViewerConfiguration> fConfigMap;
	private List<ViewerConfiguration> fReusableViewerConfigs;
	
	
	public ViewerManager2() {
		fDebugMode = IStatus.WARNING;
		
		fConfigMap = new HashMap<ILaunchConfiguration, ViewerConfiguration>();
		fReusableViewerConfigs = new ArrayList<ViewerConfiguration>();
		
		final ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		fConfigType = launchManager.getLaunchConfigurationType(TexLaunchConfigurationDelegate.CONFIGURATION_ID);
		launchManager.addLaunchConfigurationListener(this);
		try {
			ILaunchConfiguration[] launchConfigs = launchManager.getLaunchConfigurations(fConfigType);
			for (int i = 0; i < launchConfigs.length; i++) {
				try {
					load(launchConfigs[i]);
				}
				catch (CoreException e) {
					log("Error occurred when register a config initially.", e); //$NON-NLS-1$
				}
			}
		}
		catch (CoreException e) {
			log("Error occurred when fetching the configs initially.", e); //$NON-NLS-1$
		}
	}
	
	public void dispose() {
		final ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		if (launchManager != null) {
			launchManager.removeLaunchConfigurationListener(this);
		}
	}
	
	private void log(String message, Exception e) {
		
	}
	
	private synchronized void load(ILaunchConfiguration launchConfig) throws CoreException {
		ViewerConfiguration config = new ViewerConfiguration(launchConfig);
		fConfigMap.put(launchConfig, config);
		if (DDE.isSupported() &&
				(config.getDdeCloseCommand() != null || config.getDdeViewCommand() != null)) {
			fReusableViewerConfigs.add(config);
		}
	}
	
	private synchronized void remove(ILaunchConfiguration launchConfig) {
		ViewerConfiguration viewerConfig = fConfigMap.remove(launchConfig);
		if (viewerConfig != null) {
			fReusableViewerConfigs.remove(viewerConfig);
		}
	}
	
	private synchronized ViewerConfiguration[] getReuseableConfigs() {
		return fReusableViewerConfigs.toArray(new ViewerConfiguration[fReusableViewerConfigs.size()]);
	}
	
	public synchronized List<ViewerConfiguration> getAvailableConfigurations(final String outputFormat) {
		final Collection<ViewerConfiguration> allConfigs = fConfigMap.values();
		final List<ViewerConfiguration> formatConfigs = new ArrayList<ViewerConfiguration>();
		if (outputFormat == null) {
			formatConfigs.addAll(allConfigs);
			return formatConfigs;
		}
		final String formatId = outputFormat.toLowerCase();
		for (ViewerConfiguration config : allConfigs) {
			if (config.supportsFormat(formatId)) {
				formatConfigs.add(config);
			}
		}
		return formatConfigs;
	}
	
	public synchronized ViewerConfiguration getConfiguration(final String name) {
		Collection<ViewerConfiguration> allConfigs = fConfigMap.values();
		for (ViewerConfiguration config : allConfigs) {
			if (config.getName().equals(name)) {
				return config;
			}
		}
		return null;
	}
	
	public void launchConfigurationAdded(ILaunchConfiguration launchConfig) {
		try {
			if (launchConfig.getType() == fConfigType) {
				load(launchConfig);
			}
		} catch (CoreException e) {
			log("Error occurred when register new config.", e); //$NON-NLS-1$
		}
	}
	
	public void launchConfigurationChanged(ILaunchConfiguration launchConfig) {
		try {
			if (launchConfig.getType() == fConfigType && !launchConfig.isWorkingCopy()) {
				remove(launchConfig);
				load(launchConfig);
			}
		} catch (CoreException e) {
			log("Error occurred when update changed config.", e); //$NON-NLS-1$
		}
	}
	
	public void launchConfigurationRemoved(ILaunchConfiguration launchConfig) {
		remove(launchConfig);
	}
	
	
	String translatePatternForViewer(String template, final TexPathConfig config) {
		return translatePattern(template, config.getOutputFile(), config.getTexFile(),
				(config.getOutputFile() != null) ? config.getOutputFile().getParent().getLocation() : null);
	}
	
	String translatePattern(String template, final IFile defaultFile, final IFile texFile, final IPath outputDir) {
		// in DDE and separated arguments, we don't need surrounding ""
		if (template.indexOf(ViewerManager.FILENAME_PATTERN) >= 0) {
			final String fileName = defaultFile.getName();
			template = template.replaceAll(ViewerManager.FILENAME_PATTERN, Matcher.quoteReplacement(fileName));
		}
		if (template.indexOf(ViewerManager.FILENAME_FULLPATH_PATTERN) >= 0) {
			final String filePath = defaultFile.getLocation().toOSString();
			template = template.replaceAll(ViewerManager.FILENAME_FULLPATH_PATTERN, Matcher.quoteReplacement(filePath));
		}
		if (template.indexOf(ViewerManager.LINE_NUMBER_PATTERN) >= 0) {
			final String line = Integer.toString((texFile != null) ? getCurrentLineNumber(texFile) : 1);
			template = template.replaceAll(ViewerManager.LINE_NUMBER_PATTERN, line);
		}
		if (template.indexOf(ViewerManager.TEX_FILENAME_PATTERN) >= 0) {
			final String fileName = (texFile != null) ? texFile.getFullPath().toOSString() : "-"; //$NON-NLS-1$
			template = template.replaceAll(ViewerManager.TEX_FILENAME_PATTERN, Matcher.quoteReplacement(fileName));
		}
		return template;
	}
	
	public void closeDocInViewer(final TexPathConfig pathConfig) {
		final ViewerConfiguration[] configs = getReuseableConfigs();
		if (configs.length == 0) {
			return;
		}
		if (DDE.isSupported()) {
			searchViewerProcess(configs, new ProcessRunnable() {
				public boolean check(ViewerConfiguration config) {
					return (config.getDdeCloseCommand() != null && config.supportsFormat(pathConfig.getOutputFormat()));
				}
				public void found(ViewerConfiguration config, String name) {
					closeDocInViewer(pathConfig, config);
				}
			});
		}
		
	}
	
	public boolean openDocInViewer(TexPathConfig pathConfig, final ViewerConfiguration viewerConfig) {
		try {
			if (DDE.isSupported() && viewerConfig.getDdeViewCommand() != null) {
				final String server = viewerConfig.getDdeViewServer();
				final String topic = viewerConfig.getDdeViewTopic();
				final String command = translatePatternForViewer(viewerConfig.getDdeViewCommand(), pathConfig);
				
				final AtomicBoolean ok = new AtomicBoolean(false);
				final AtomicReference<CoreException> error = new AtomicReference<CoreException>();
				searchViewerProcess(new ViewerConfiguration[] { viewerConfig }, new ProcessRunnable() {
					public boolean check(ViewerConfiguration config) {
						return !ok.get();
					}
					public void found(ViewerConfiguration config, String name) {
						try {
							DDEClient.execute(server, topic, command);
							ok.set(true);
						}
						catch (final CoreException e) {
							error.set(e);
						}
					}
				});
				if (ok.get()) {
					return true;
				}
				if (startViewerProcess(pathConfig, viewerConfig)) {
					if (viewerConfig.getProcessCommand().contains(ViewerManager.FILENAME_FULLPATH_PATTERN)) {
						return true;
					}
					for (int i = 0; DDE.isSupported() && i < 3; i++) {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							Thread.interrupted();
						}
						try {
							DDEClient.execute(server, topic, command);
							return true;
						}
						catch (final CoreException e) {
							error.set(e);
						}
					}
				}
				if (error.get() != null) {
					throw error.get();
				}
				// error
				return false;
			}
			else {
				startViewerProcess(pathConfig, viewerConfig);
			}
		}
		catch (final CoreException e) {
			log(NLS.bind("Error occurred when opening doc using ''{0}''.", viewerConfig.getName()), e); //$NON-NLS-1$
		}
		return false;
	}
	
	private void closeDocInViewer(final TexPathConfig pathConfig, final ViewerConfiguration viewerConfig) {
		if (DDE.isSupported() && viewerConfig.getDdeCloseCommand() != null) {
			// sendDDECloseCommand
			try {
				final String server = viewerConfig.getDdeCloseServer();
				final String topic = viewerConfig.getDdeCloseTopic();
				final String command = translatePatternForViewer(viewerConfig.getDdeCloseCommand(), pathConfig);
				
				DDEClient.execute(server, topic, command);
			}
			catch (CoreException e) {
				log(NLS.bind("Error occurred when closing doc using ''{0}''.", viewerConfig.getName()), e); //$NON-NLS-1$
			}
		}
	}
	
	/**
	 * Returns the current line number of the current page, if possible.
	 * 
	 * @author Anton Klimovsky
	 * @return the current line number of the current page
	 */
	private int getCurrentLineNumber(final IFile file) {
		final Display display = PlatformUI.getWorkbench().getDisplay();
		// The "srcltx" package's line numbers seem to start from 1
		// it is also the case with latex's --source-specials option
		final AtomicInteger lineNumber = new AtomicInteger(1);
		display.syncExec(new Runnable() {
			public void run(){
				final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				if (page == null) {
					return;
				}
				final IEditorPart editor = page.getActiveEditor();
				if (!(editor instanceof ITextEditor)) {
					return;
				}
				final IEditorInput editorInput = editor.getEditorInput();
				final IFile editorFile = (IFile) editorInput.getAdapter(IFile.class);
				if (file.equals(editorFile)) {
					final ISelection selection = ((ITextEditor) editor).getSelectionProvider().getSelection();
					if (selection instanceof ITextSelection) {
						lineNumber.set(((ITextSelection) selection).getStartLine() + 1);
					}
				}
			}
		});
		return lineNumber.get();
	}
	
	private void searchViewerProcess(final ViewerConfiguration[] configs, final ProcessRunnable runnable) {
		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			BufferedReader in = null;
			try {
				Process p = Runtime.getRuntime().exec(new String[] { "TASKLIST", "/FO", "CSV", "/NH" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				in = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String s;
				while ((s = in.readLine()) != null) {
					final int endIdx = s.indexOf('"', 1);
					if (endIdx < 0) {
						continue;
					}
					String name = s.substring(1, endIdx).toLowerCase();
					for (ViewerConfiguration config : configs) {
						if (runnable.check(config) && config.getProcessPattern().matcher(name).find()) {
							runnable.found(config, name);
						}
					}
				}
			}
			catch (IOException e) {
			}
			finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException ignore) {
					}
				}
			}
		}
	}
	
	private boolean startViewerProcess(TexPathConfig pathConfig, final ViewerConfiguration viewerConfig) throws CoreException {
		final String[] cmds = DebugPlugin.parseArguments(viewerConfig.getProcessCommand());
		for (int i = 0; i < cmds.length; i++) {
			translatePatternForViewer(cmds[i], pathConfig);
		}
		
		final ProcessBuilder pBuilder = new ProcessBuilder(cmds);
		Map<String, String> prefEnvp = PathUtils.getPreferenceMap(TexlipseProperties.VIEWER_ENV_SETTINGS);
		LaunchUtils.configureEnvironment(pBuilder.environment(), viewerConfig.getLaunchConfig(), prefEnvp);
		
		Process process;
		try {
			process = pBuilder.start();
		} catch (IOException e) {
			new CoreException(TexlipsePlugin.stat("Could not start previewer '"
					+ viewerConfig.getName() + "'. Please make sure you have entered "
					+ "the correct path and filename in the viewer preferences.", e));
			return false;
		}
		
		int code = 0;
		try {
			Thread.sleep(100);
			code = process.exitValue();
		}
		catch (InterruptedException e) {
			Thread.interrupted();
		}
		catch (IllegalThreadStateException e) {
		}
		
		boolean started = (code == 0 || (code == 1 && cmds[0].toLowerCase().indexOf("acrobat.exe") >= 0)); //$NON-NLS-1$
		if (!started || fDebugMode < IStatus.ERROR) {
			Thread thread = new Thread(new ViewerErrorScanner(process));
			thread.setDaemon(true);
			thread.start();
		}
		if (started) {
			connectInverseSearch(viewerConfig.getInverseSearchType(), pathConfig, process);
		}
		return true;
	}
	
	/**
	 * Start a listener thread for the viewer program's standard output.
	 * 
	 * @param in input stream where the output of a viewer program will be available
	 * @param viewer the name of the viewer
	 */
	private void connectInverseSearch(final String type, final TexPathConfig pathConfig, final Process process) {
//		if (type.equals(ViewerConfiguration.INVERSE_SEARCH_RUN)) {
//			FileLocationServer server = FileLocationServer.getInstance();
//			server.setListener(new FileLocationOpener(file.getProject()));
//			if (!server.isRunning()) {
//				new Thread(server).start();
//			}
//		}
//		else if (type.equals(ViewerConfiguration.INVERSE_SEARCH_STD)) {
//			new Thread(new ViewerOutputScanner(file.getProject(), process.getInputStream())).start();
//		}
	}
	
}
