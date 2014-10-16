/*
 * Copyright 2009-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cloudfoundry.maven;

import java.util.List;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudRoute;

import org.apache.maven.plugin.MojoExecutionException;
import org.cloudfoundry.client.lib.CloudFoundryException;

import org.springframework.http.HttpStatus;

/**
 * Clean all applications, services and routes of a space
 *
 * @author Julie Garrone
 * @since 1.0.3
 * @goal clean-space
 * @phase process-sources
 */

public class CleanSpace extends AbstractApplicationAwareCloudFoundryMojo {

	@Override
	protected void doExecute() throws MojoExecutionException {
		
		final List<CloudApplication> applications = getClient().getApplications();
		getLog().info("Deleting applications:" + applications.size() + " applications to delete.");
		for (CloudApplication app : applications) {
			getLog().info("Deleting application '" + app.getName() + "'");

			try {
				getClient().deleteApplication(app.getName());
			} catch (CloudFoundryException e) {
				if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
					getLog().info("Application '" + app.getName() + "' does not exist");
				} else {
					throw new MojoExecutionException(String.format("Error while deleting application '%s'. Error message: '%s'. Description: '%s'",
							app.getName(), e.getMessage(), e.getDescription()), e);
				}
			}
		}
		
		final List<CloudService> services = getClient().getServices();
		getLog().info("Deleting services:" + services.size() + " services to delete.");
		for (CloudService service : services) {
			try {
				getLog().info(String.format("Deleting service '%s'", service.getName()));
				getClient().deleteService(service.getName());
			} catch (NullPointerException e) {
				getLog().info(String.format("Service '%s' does not exist", service.getName()));
			}
		}
		
		List<CloudRoute> routes = getClient().deleteOrphanedRoutes();
		getLog().info("Deleting orphaned routes:"+ routes.size() + " routes to delete.");
		for (CloudRoute route : routes) {
			getLog().info(String.format("Deleted route '%s'", route.getName()));
		}
	}
}