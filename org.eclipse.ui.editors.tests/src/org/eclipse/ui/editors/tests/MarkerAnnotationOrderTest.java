/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.editors.tests;

import java.io.BufferedInputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.osgi.framework.Bundle;

import org.eclipse.core.runtime.ContributorFactorySimple;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IMarker;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;

import org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel;

import org.eclipse.ui.editors.text.EditorsUI;

public class MarkerAnnotationOrderTest extends TestCase {

	IContributor pointContributor= null;

	Object masterToken= null;

	public static Test suite() {
		return new TestSuite(MarkerAnnotationOrderTest.class);
	}

	protected void setUp() throws Exception {
		//add the marker updater extension point
		IExtensionRegistry registry= Platform.getExtensionRegistry();
		pointContributor= ContributorFactorySimple.createContributor(Long.toString(EditorTestPlugin.getDefault().getBundle().getBundleId()));

		try {
			BufferedInputStream bis= new BufferedInputStream(getClass().getResourceAsStream("plugin.xml"));

			Field field=
					org.eclipse.core.internal.registry.ExtensionRegistry.class
							.getDeclaredField("masterToken");
			field.setAccessible(true);
			masterToken= field.get(registry);
			registry.addContribution(bis, pointContributor, true, null, null, masterToken);
		} catch (Exception ex) {
			fail("update marker setup failed to execute");
			EditorTestPlugin.getDefault().getLog().log(new Status(IStatus.ERROR, EditorTestPlugin.PLUGIN_ID, ex.getMessage()));
		}
	}

	protected void tearDown() throws Exception {
		// remove the marker updater extension point
		IExtensionRegistry registry= Platform.getExtensionRegistry();
		IExtension[] extensions = registry.getExtensions(pointContributor);
		for (int i= 0; i < extensions.length; i++) {
			if ("org.eclipse.ui.editors.markerUpdaters".equals(extensions[i].getExtensionPointUniqueIdentifier()))
				registry.removeExtension(extensions[i], masterToken);
		}
	}
	
	public void testDirectDependency() {
		final ArrayList list= new ArrayList(2);
		Bundle bundle= Platform.getBundle(EditorsUI.PLUGIN_ID);
		ILog log= Platform.getLog(bundle);
		log.addLogListener(new ILogListener() {

			public void logging(IStatus status, String plugin) {
				list.add(status);
			}
		});

		TestMarkerAnnotationModel t1= new TestMarkerAnnotationModel();
		Position position= new Position(0);
		position.delete();
		IDocument d= null;
		try {
			t1.updateMarker(d, null, position);
		} catch (CoreException e) {
			fail("update marker failed to execute");
			EditorTestPlugin.getDefault().getLog().log(new Status(IStatus.ERROR, EditorTestPlugin.PLUGIN_ID, e.getMessage()));
		}

		assertEquals("Wrong number of messages", 2, list.size());
		assertEquals(
				"Wrong Message for first status",
				"Marker Updater 'org.eclipse.ui.texteditor.BasicMarkerUpdaterTest2' and 'org.eclipse.ui.texteditor.BasicMarkerUpdaterTest1' depend on each other, 'org.eclipse.ui.texteditor.BasicMarkerUpdaterTest2' will run before 'org.eclipse.ui.texteditor.BasicMarkerUpdaterTest1'",
				((Status)list.get(0)).getMessage());
		assertEquals(
				"Wrong Message for second status",
				"Marker Updater 'org.eclipse.ui.texteditor.BasicMarkerUpdaterTest4' and 'org.eclipse.ui.texteditor.BasicMarkerUpdaterTest1' depend on each other, 'org.eclipse.ui.texteditor.BasicMarkerUpdaterTest4' will run before 'org.eclipse.ui.texteditor.BasicMarkerUpdaterTest1'",
				((Status)list.get(1)).getMessage());

	}

	public class TestMarkerAnnotationModel extends AbstractMarkerAnnotationModel {
		protected IMarker[] retrieveMarkers() throws CoreException {
			return null;
		}

		protected void deleteMarkers(IMarker[] markers) throws CoreException {
		}

		protected void listenToMarkerChanges(boolean listen) {
		}

		protected boolean isAcceptable(IMarker marker) {
			return false;
		}

	}

}
