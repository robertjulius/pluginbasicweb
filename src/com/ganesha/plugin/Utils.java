package com.ganesha.plugin;

import java.io.ByteArrayInputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class Utils {

	public static void createFolder(IResource resource, IProgressMonitor monitor)
			throws CoreException {

		if (resource == null || resource.exists()) {
			return;
		}

		if (!resource.getParent().exists()) {
			createFolder(resource.getParent(), monitor);
		}

		switch (resource.getType()) {
		case IResource.FILE:
			((IFile) resource).create(new ByteArrayInputStream(new byte[0]),
					true, monitor);
			break;
		case IResource.FOLDER:
			((IFolder) resource).create(IResource.NONE, true, monitor);
			break;
		case IResource.PROJECT:
			((IProject) resource).create(monitor);
			((IProject) resource).open(monitor);
			break;
		}
	}
}
