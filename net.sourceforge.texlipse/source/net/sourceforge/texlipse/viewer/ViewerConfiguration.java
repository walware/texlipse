package net.sourceforge.texlipse.viewer;

import java.util.regex.Pattern;

import net.sourceforge.texlipse.Texlipse;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;

public class ViewerConfiguration {

	// attribute suffixes
	public static final String ATTRIBUTE_COMMAND = ".command"; //$NON-NLS-1$
	public static final String ATTRIBUTE_ARGUMENTS = ".arguments"; //$NON-NLS-1$
	public static final String ATTRIBUTE_DDE_VIEW_COMMAND = ".ddeViewCommand"; //$NON-NLS-1$
	public static final String ATTRIBUTE_DDE_VIEW_SERVER = ".ddeViewServer"; //$NON-NLS-1$
	public static final String ATTRIBUTE_DDE_VIEW_TOPIC = ".ddeViewTopic"; //$NON-NLS-1$
	public static final String ATTRIBUTE_DDE_CLOSE_COMMAND = ".ddeCloseCommand"; //$NON-NLS-1$
	public static final String ATTRIBUTE_DDE_CLOSE_SERVER = ".ddeCloseServer"; //$NON-NLS-1$
	public static final String ATTRIBUTE_DDE_CLOSE_TOPIC = ".ddeCloseTopic"; //$NON-NLS-1$
	public static final String ATTRIBUTE_FORMAT = ".format"; //$NON-NLS-1$
	public static final String ATTRIBUTE_INVERSE_SEARCH = ".inverse"; //$NON-NLS-1$
	public static final String ATTRIBUTE_FORWARD_SEARCH = ".forward"; //$NON-NLS-1$

	// inverse search attibute values
	public static final String INVERSE_SEARCH_NO = "no"; //$NON-NLS-1$
	public static final String INVERSE_SEARCH_RUN = "run"; //$NON-NLS-1$
	public static final String INVERSE_SEARCH_STD = "std"; //$NON-NLS-1$

	
	private ILaunchConfiguration launchConfig;

	private String name;
	private String format;
	private String processCommand;
	private Pattern processPattern;
	private String ddeViewCommand;
	private String ddeCloseCommand;
	
	private String prefix;

	
	ViewerConfiguration() {
	}

	ViewerConfiguration(ILaunchConfiguration launchConfig) throws CoreException {
		this.launchConfig = launchConfig;
		name = launchConfig.getName();
		prefix = launchConfig.getAttribute("viewerCurrent", "");

		processCommand = launchConfig.getAttribute(prefix+ATTRIBUTE_COMMAND, (String) null);
		if (processCommand == null || processCommand.length() == 0) {
			throw new CoreException(new Status(IStatus.ERROR, Texlipse.PLUGIN_ID, -1, "Missing command for viewer", null));
		}
		
		format = launchConfig.getAttribute(prefix+ATTRIBUTE_FORMAT, (String) null);
		if (format == null || format.length() == 0) {
			throw new CoreException(new Status(IStatus.ERROR, Texlipse.PLUGIN_ID, -1, "Missing format for viewer", null));
		}
		else {
			format = format.toLowerCase();
		}
		
		// default process pattern
		int idx1 = processCommand.lastIndexOf('/');
		int idx2 = processCommand.lastIndexOf('\\');
		String exe;
		if (idx2 > idx1) {
			exe = processCommand.substring(idx2+1);
		}
		else if (idx1 >= 0) {
			exe = processCommand.substring(idx1+1);
		}
		else {
			exe = processCommand;
		}
		idx1 = exe.lastIndexOf('.');
		if (idx1 >= 0) {
			exe = exe.substring(0, idx1);
		}
		processPattern = Pattern.compile(Pattern.quote(exe.toLowerCase()));
		
		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			ddeViewCommand = launchConfig.getAttribute(prefix+ATTRIBUTE_DDE_VIEW_COMMAND, (String) null);
			ddeCloseCommand = launchConfig.getAttribute(prefix+ATTRIBUTE_DDE_CLOSE_COMMAND, (String) null);
		}
	}

	public String getName() {
		return name;
	}

	public Pattern getProcessPattern() {
		return processPattern;
	}

	public ILaunchConfiguration getLaunchConfig() {
		return launchConfig;
	}

	public String getProcessCommand() {
		return processCommand;
	}

	public String getInverseSearchType() throws CoreException {
		return launchConfig.getAttribute(prefix+ATTRIBUTE_INVERSE_SEARCH, INVERSE_SEARCH_NO);
	}

	public String getDdeViewServer() throws CoreException {
		return launchConfig.getAttribute(prefix+ATTRIBUTE_DDE_VIEW_SERVER, ""); //$NON-NLS-1$
	}

	public String getDdeViewTopic() throws CoreException {
		return launchConfig.getAttribute(prefix+ATTRIBUTE_DDE_VIEW_TOPIC, ""); //$NON-NLS-1$
	}

	public String getDdeViewCommand() {
		return ddeViewCommand;
	}

	public String getDdeCloseServer() throws CoreException {
		return launchConfig.getAttribute(prefix+ATTRIBUTE_DDE_CLOSE_SERVER, ""); //$NON-NLS-1$
	}

	public String getDdeCloseTopic() throws CoreException {
		return launchConfig.getAttribute(prefix+ATTRIBUTE_DDE_CLOSE_TOPIC, ""); //$NON-NLS-1$
	}

	public String getDdeCloseCommand() {
		return ddeCloseCommand;
	}
	
	public boolean supportsFormat(String id) {
		return format.equals(id);
	}
}
