package com.ganesha.plugin.basicweb.wizards;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

import com.ganesha.plugin.Utils;
import com.ganesha.plugin.basicweb.GeneralConstants;
import com.ganesha.plugin.basicweb.Templates;

public class BasicWebProjectWizard extends Wizard implements INewWizard {

	private WizardNewProjectCreationPage wizardPage;

	private IConfigurationElement config;

	private IWorkbench workbench;

	private IStructuredSelection selection;

	private IProject project;

	@Override
	public void addPages() {
		wizardPage = new WizardNewProjectCreationPage(
				GeneralConstants.WIZARD_PAGE);
		wizardPage.setDescription(GeneralConstants.WIZARD_DESCRIPTION);
		wizardPage.setTitle(GeneralConstants.WIZARD_TITLE);
		addPage(wizardPage);
	}

	@Override
	public void init(IWorkbench arg0, IStructuredSelection arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean performFinish() {

		if (project != null) {
			return true;
		}

		final IProject projectHandle = wizardPage.getProjectHandle();

		URI projectURI = (!wizardPage.useDefaults()) ? wizardPage
				.getLocationURI() : null;

		IWorkspace workspace = ResourcesPlugin.getWorkspace();

		final IProjectDescription desc = workspace
				.newProjectDescription(projectHandle.getName());

		desc.setLocationURI(projectURI);

		/*
		 * Just like the ExampleWizard, but this time with an operation object
		 * that modifies workspaces.
		 */
		WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
			@Override
			protected void execute(IProgressMonitor monitor)
					throws CoreException {
				createProject(desc, projectHandle, monitor);
			}
		};

		/*
		 * This isn't as robust as the code in the BasicNewProjectResourceWizard
		 * class. Consider beefing this up to improve error handling.
		 */
		try {
			getContainer().run(true, true, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error",
					realException.getMessage());
			return false;
		}

		project = projectHandle;

		if (project == null) {
			return false;
		}

		/*
		 * TODO set perspective
		 */
		// BasicNewProjectResourceWizard.updatePerspective(config);
		// BasicNewProjectResourceWizard.selectAndReveal(project,
		// workbench.getActiveWorkbenchWindow());

		return true;
	}

	/**
	 * Adds a new file to the project.
	 * 
	 * @param container
	 * @param path
	 * @param contentStream
	 * @param monitor
	 * @throws CoreException
	 */
	private void addFileToProject(IContainer container, IPath path,
			InputStream contentStream, IProgressMonitor monitor)
			throws CoreException {
		final IFile file = container.getFile(path);

		if (file.exists()) {
			file.setContents(contentStream, true, true, monitor);
		} else {
			file.create(contentStream, true, monitor);
		}

	}

	private void createFile(IProject project, IPath path,
			InputStream contentStream, IProgressMonitor monitor)
			throws CoreException {

		IFile file = project.getFile(path);
		if (file.exists()) {
			file.setContents(contentStream, true, true, monitor);
		} else {
			file.create(contentStream, true, monitor);
		}
	}

	private void createFile(IProject project, IPath path,
			IProgressMonitor monitor) throws CoreException {

		InputStream contentStream = null;
		try {

			contentStream = openContentStream(path.toString(),
					project.getName());

			createFile(project, path, contentStream, monitor);

		} finally {
			try {
				if (contentStream != null) {
					contentStream.close();
				}
			} catch (IOException e) {
				IStatus status = new Status(IStatus.ERROR, this.getClass()
						.getName(), IStatus.OK, e.getLocalizedMessage(), null);
				throw new CoreException(status);
			}
		}
	}

	private void createProject(IProjectDescription description, IProject proj,
			IProgressMonitor monitor) throws CoreException,
			OperationCanceledException {
		try {

			monitor.beginTask("", 2000);

			proj.create(description, new SubProgressMonitor(monitor, 1000));

			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}

			proj.open(IResource.BACKGROUND_REFRESH, new SubProgressMonitor(
					monitor, 1000));

			for (int i = 0; i < Templates.count(); ++i) {
				IPath path = new Path(Templates.get(i));
				IFile file = proj.getFile(path);
				Utils.createFolder(file.getParent(), monitor);
				createFile(proj, path, monitor);
			}

			/*
			 * Okay, now we have the project and we can do more things with it
			 * before updating the perspective.
			 */
			IContainer container = proj;

			/* Add an XHTML file */
			addFileToProject(
					container,
					new Path("index.html"),
					new ByteArrayInputStream("This is sample content"
							.getBytes()), monitor);

		} catch (RuntimeException e) {
			IStatus status = new Status(IStatus.ERROR, this.getClass()
					.getName(), IStatus.OK, e.getLocalizedMessage(), null);
			throw new CoreException(status);
		} finally {
			monitor.done();
		}
	}

	private InputStream openContentStream(String filePath, String projectName)
			throws CoreException {

		String newLine = System.getProperty("line.separator");
		StringBuilder sb = new StringBuilder();

		InputStream input = null;
		BufferedReader reader = null;

		try {
			input = this.getClass().getClassLoader()
					.getResourceAsStream(filePath);
			reader = new BufferedReader(new InputStreamReader(input));

			String line;
			while ((line = reader.readLine()) != null) {
				line = line.replaceAll(
						"\\" + GeneralConstants.PROJECT_NAME_VAR, projectName);
				sb.append(line);
				sb.append(newLine);
			}

		} catch (IOException e) {
			IStatus status = new Status(IStatus.ERROR, this.getClass()
					.getName(), IStatus.OK, e.getLocalizedMessage(), null);
			throw new CoreException(status);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					IStatus status = new Status(IStatus.ERROR, this.getClass()
							.getName(), IStatus.OK, e.getLocalizedMessage(),
							null);
					throw new CoreException(status);
				}
			}
		}

		return new ByteArrayInputStream(sb.toString().getBytes());
	}
}