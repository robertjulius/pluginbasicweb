package com.ganesha.plugin;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;

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

	public static void main(String[] args) throws Exception {
		File file = new File(Utils.class.getClassLoader().getResource("")
				.toURI());

		file = file.getParentFile().listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				if (name.equals("templates")) {
					return true;
				} else {
					return false;
				}
			}
		})[0];

		scanFilesRecursively(file);
	}

	public static void scanFilesRecursively(File file) {
		File[] childs = file.listFiles();
		if (childs != null && childs.length > 0) {
			for (File child : childs) {
				scanFilesRecursively(child);
			}
		} else {
			if (file.isFile()) {
				System.out.println("files.add(\"" + file.getPath() + "\");");
			} else {
				/*
				 * TODO
				 */
			}
		}
	}
}
