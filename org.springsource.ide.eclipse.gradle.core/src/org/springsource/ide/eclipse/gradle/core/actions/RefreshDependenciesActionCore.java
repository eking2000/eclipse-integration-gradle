/*******************************************************************************
 * Copyright (c) 2012 VMWare, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * VMWare, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.util.Continuation;
import org.springsource.ide.eclipse.gradle.core.util.JobUtil;
import org.springsource.ide.eclipse.gradle.core.util.Joinable;
import org.springsource.ide.eclipse.gradle.core.util.JoinableContinuation;


/**
 * The 'core' counterpart for RefreshDependenciesAction.
 * 
 * @author Kris De Volder
 */
public class RefreshDependenciesActionCore {

	/**
	 * Exposes what this UI action does through an easy to call static method.
	 */
	private static void callOn(final List<IProject> projects, Continuation<Void> cont) {
		if (!projects.isEmpty()) {
			JobUtil.schedule(JobUtil.LIGHT_RULE, new Continuable("Refresh project models", projects.size()*2, cont) {
				@Override
				public void doit(Continuation<Void> cont, IProgressMonitor monitor) throws CoreException {
					final List<GradleProject> gps = new ArrayList<GradleProject>(projects.size());
					for (IProject p : projects) {
						GradleProject gp = GradleCore.create(p);
						gp.invalidateGradleModel();
						gps.add(gp);
						monitor.worked(1);
					}
					for (GradleProject gp : gps) {
						gp.getGradleModel(new SubProgressMonitor(monitor, 1));
					}
					JobUtil.schedule(new Continuable("Refresh project dependencies", projects.size(), cont) {
						@Override
						public void doit(Continuation<Void> cont, IProgressMonitor monitor) throws Exception {
							for (GradleProject gp : gps) {
								gp.refreshDependencies(new SubProgressMonitor(monitor, 1));
							}
							cont.apply(null);
						}
					});
				}
			});
		} else {
			cont.apply(null);
		}
	}

	/**
	 * A convenience method that wraps the callback-style method so it returns a 'Joinable' instead.
	 */
	public static Joinable<Void> callOn(List<IProject> asList) {
		JoinableContinuation<Void> k = new JoinableContinuation<Void>();
		callOn(asList, k);
		return k;
	}

	/**
	 * A convenience method used in testing with a single project. Blocks until the refresh is done.
	 */
	public static void synchCallOn(IProject p) throws Exception {
		JoinableContinuation<Void> k = new JoinableContinuation<Void>();
		callOn(Arrays.asList(p), k);
		k.join();
	}

}
