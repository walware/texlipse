/*
 * $Id: AbstractBuilder.java,v 1.2 2008/04/27 10:05:42 borisvl Exp $
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.builder;

import net.sourceforge.texlipse.TexPathConfig;
import net.sourceforge.texlipse.Texlipse;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

/**
 * Generic builder.
 * 
 * @author Kimmo Karlsson
 */
public abstract class AbstractBuilder implements Runnable, Builder {
	
	
	
	
	public static boolean checkOutput(TexPathConfig pathConfig, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("Checking for output file", 50);
		try {
			final IContainer outputDir;
			final IContainer texDir;
			final IResource outputFile;
			try {
				outputDir = pathConfig.getOutputFile().getParent();
				texDir = pathConfig.getTexFile().getParent();
				
				texDir.refreshLocal(IResource.DEPTH_ONE, new SubProgressMonitor(monitor, 5));
				
				outputFile = texDir.findMember(pathConfig.getOutputFile().getName());
				if (!(outputFile instanceof IFile) || !outputFile.exists()) {
					return false;
				}
			}
			catch (CoreException e) {
				throw new CoreException(new Status(IStatus.ERROR, Texlipse.PLUGIN_ID, -1,
						"An error occured when checking for output file created by the TeX builder.", e));
			}
			
			if (!outputDir.equals(texDir)) {
				try {
					outputDir.refreshLocal(IResource.DEPTH_ONE, new SubProgressMonitor(monitor, 5));
					if (!outputDir.exists()) {
						if (outputDir instanceof IFolder) {
							((IFolder) outputDir).create(IResource.FORCE, true, new SubProgressMonitor(monitor, 5));
						}
						else {
							outputDir.getLocation().toFile().mkdirs();
						}
					}
					if (pathConfig.getOutputFile().exists()) {
						pathConfig.getOutputFile().delete(IResource.FORCE, new SubProgressMonitor(monitor, 1));
					}
					outputFile.move(pathConfig.getOutputFile().getFullPath(), IResource.FORCE, new SubProgressMonitor(monitor, 5));
				}
				catch (CoreException e) {
					throw new CoreException(new Status(IStatus.ERROR, Texlipse.PLUGIN_ID, -1,
							"Can not move output file to output directory (a possible cause: that the document is locked by your viewer).", e));
				}
			}
			return true;
		}
		finally {
			monitor.done();
		}
	}
	
	
    // the current progress monitor
    protected IProgressMonitor monitor;
    
    // true, when the build process is running
    protected volatile boolean buildRunning;

    // builder id in the builder registry
    protected int id;

    /**
     * Create a new builder.
     * @param mon
     */
    protected AbstractBuilder(int i) {
        id = i;
        monitor = null;
        buildRunning = false;
    }
    
    /**
     * @return id number
     */
    public int getId() {
        return id;
    }
    
    /**
     * Check to see if this builder is valid.
     * @return true, if this builder is ready for operation
     */
    public abstract boolean isValid();
    
    /**
     * Resets the builder to be ready for a new build.
     */
    public void reset(final IProgressMonitor mon) {
        monitor = mon;
        buildRunning = false;
    }
    
    /**
     * @return the name of the format this builder outputs
     */
    public abstract String getOutputFormat();
    
    public abstract String getSequence();
    
    /**
     * Run the build monitor. If the user interrupts the build, stop the execution.
     */
    public void run() {
        while(buildRunning) {
            try {
                Thread.sleep(500);
            } catch(InterruptedException e) {
            }
            if (monitor.isCanceled()) {
                stopBuild();
            }
        }
    }

    /**
     * Stops the execution of the external programs.
     */
    public abstract void stopRunners();
    
    /**
     * Stops the execution of the building process.
     */
    public void stopBuild() {
        buildRunning = false;
        stopRunners();
    }
    
	public void build(IFile resource) throws CoreException {
		build(new TexPathConfig(resource, null, getOutputFormat()));
	}
	
    /**
     * The main build method. This runs latex program once at the given directory.
     * @throws CoreException
     */
	public abstract void buildResource(TexPathConfig pathConfig) throws CoreException;
    
    /**
     * The main method.
     * 
     * @param resource the input file to compile
     * @throws CoreException if the build fails
     */
	public void build(final TexPathConfig pathConfig) throws CoreException {
        if (monitor == null) {
            throw new IllegalStateException();
        }
        
        buildRunning = true;
        Thread buildThread = new Thread(this);
        buildThread.start();
        
        try {
			buildResource(pathConfig);
        } finally {
        	   
	        buildRunning = false;
	        
	        try {
	            buildThread.join();
	        } catch (InterruptedException e) {
	            Thread.interrupted();
	            monitor.setCanceled(true);
	            stopBuild();
	        }
        }
    }

}