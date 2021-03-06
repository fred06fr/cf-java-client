/*
 * Copyright 2009-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cloudfoundry.client.lib;

import org.cloudfoundry.client.lib.archive.ApplicationArchive;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.ApplicationStats;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.DebugMode;
import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.CloudInfo;
import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.CloudStack;
import org.cloudfoundry.client.lib.domain.CrashesInfo;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.client.lib.rest.CloudControllerClient;
import org.cloudfoundry.client.lib.rest.CloudControllerClientFactory;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.util.Assert;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A Java client to exercise the Cloud Foundry API.
 *
 * @author Ramnivas Laddad
 * @author A.B.Srinivasan
 * @author Jennifer Hickey
 * @author Dave Syer
 * @author Thomas Risberg
 */
public class CloudFoundryClient implements CloudFoundryOperations {

	private CloudControllerClient cc;

	private CloudInfo info;

	/**
	 * Construct client for anonymous user. Useful only to get to the '/info' endpoint.
	 */

	public CloudFoundryClient(URL cloudControllerUrl) {
		this(null, cloudControllerUrl, null, (HttpProxyConfiguration) null, false);
	}

	public CloudFoundryClient(URL cloudControllerUrl, boolean trustSelfSignedCerts) {
		this(null, cloudControllerUrl, null, (HttpProxyConfiguration) null, trustSelfSignedCerts);
	}

	public CloudFoundryClient(URL cloudControllerUrl, HttpProxyConfiguration httpProxyConfiguration) {
		this(null, cloudControllerUrl, null, httpProxyConfiguration, false);
	}

	public CloudFoundryClient(URL cloudControllerUrl, HttpProxyConfiguration httpProxyConfiguration,
	                          boolean trustSelfSignedCerts) {
		this(null, cloudControllerUrl, null, httpProxyConfiguration, trustSelfSignedCerts);
	}

	/**
	 * Construct client without a default org and space.
	 */

	public CloudFoundryClient(CloudCredentials credentials, URL cloudControllerUrl) {
		this(credentials, cloudControllerUrl, null, (HttpProxyConfiguration) null, false);
	}

	public CloudFoundryClient(CloudCredentials credentials, URL cloudControllerUrl,
	                          boolean trustSelfSignedCerts) {
		this(credentials, cloudControllerUrl, null, (HttpProxyConfiguration) null, trustSelfSignedCerts);
	}

	public CloudFoundryClient(CloudCredentials credentials, URL cloudControllerUrl,
	                          HttpProxyConfiguration httpProxyConfiguration) {
		this(credentials, cloudControllerUrl, null, httpProxyConfiguration, false);
	}

	public CloudFoundryClient(CloudCredentials credentials, URL cloudControllerUrl,
	                          HttpProxyConfiguration httpProxyConfiguration, boolean trustSelfSignedCerts) {
		this(credentials, cloudControllerUrl, null, httpProxyConfiguration, trustSelfSignedCerts);
	}

	/**
	 * Construct a client with a default CloudSpace.
	 */

	public CloudFoundryClient(CloudCredentials credentials, URL cloudControllerUrl, CloudSpace sessionSpace) {
		this(credentials, cloudControllerUrl, sessionSpace, null, false);
    }

	public CloudFoundryClient(CloudCredentials credentials, URL cloudControllerUrl, CloudSpace sessionSpace,
	                          boolean trustSelfSignedCerts) {
		this(credentials, cloudControllerUrl, sessionSpace, null, trustSelfSignedCerts);
	}

	public CloudFoundryClient(CloudCredentials credentials, URL cloudControllerUrl, CloudSpace sessionSpace,
	                          HttpProxyConfiguration httpProxyConfiguration) {
		this(credentials, cloudControllerUrl, sessionSpace, httpProxyConfiguration, false);
	}

	public CloudFoundryClient(CloudCredentials credentials, URL cloudControllerUrl, CloudSpace sessionSpace,
	                          HttpProxyConfiguration httpProxyConfiguration, boolean trustSelfSignedCerts) {
		Assert.notNull(cloudControllerUrl, "URL for cloud controller cannot be null");
		CloudControllerClientFactory cloudControllerClientFactory =
				new CloudControllerClientFactory(httpProxyConfiguration, trustSelfSignedCerts);
		this.cc = cloudControllerClientFactory.newCloudController(cloudControllerUrl, credentials, sessionSpace);
	}

	/**
	 * Construct a client with a default space name and org name.
	 */

	public CloudFoundryClient(CloudCredentials credentials, URL cloudControllerUrl, String orgName, String spaceName) {
		this(credentials, cloudControllerUrl, orgName, spaceName, null, false);
	}

	public CloudFoundryClient(CloudCredentials credentials, URL cloudControllerUrl, String orgName, String spaceName,
	                          boolean trustSelfSignedCerts) {
		this(credentials, cloudControllerUrl, orgName, spaceName, null, trustSelfSignedCerts);
	}

	public CloudFoundryClient(CloudCredentials credentials, URL cloudControllerUrl, String orgName, String spaceName,
							  HttpProxyConfiguration httpProxyConfiguration) {
		this(credentials, cloudControllerUrl, orgName, spaceName, httpProxyConfiguration, false);
	}

	public CloudFoundryClient(CloudCredentials credentials, URL cloudControllerUrl, String orgName, String spaceName,
	                          HttpProxyConfiguration httpProxyConfiguration, boolean trustSelfSignedCerts) {
		Assert.notNull(cloudControllerUrl, "URL for cloud controller cannot be null");
		CloudControllerClientFactory cloudControllerClientFactory =
				new CloudControllerClientFactory(httpProxyConfiguration, trustSelfSignedCerts);
		this.cc = cloudControllerClientFactory.newCloudController(cloudControllerUrl, credentials, orgName, spaceName);
	}

	/**
	 * Construct a client with a pre-configured CloudControllerClient
	 */
	public CloudFoundryClient(CloudControllerClient cc) {
		this.cc = cc;
	}

	public void setResponseErrorHandler(ResponseErrorHandler errorHandler) {
		cc.setResponseErrorHandler(errorHandler);
	}

	public URL getCloudControllerUrl() {
		return cc.getCloudControllerUrl();
	}

	public CloudInfo getCloudInfo() {
		if (info == null) {
			info = cc.getInfo();
		}
		return info;
	}

	public List<CloudSpace> getSpaces() {
		return cc.getSpaces();
	}

	public List<CloudOrganization> getOrganizations() {
		return cc.getOrganizations();
	}

	public void register(String email, String password) {
		cc.register(email, password);
	}

	public void updatePassword(String newPassword) {
		cc.updatePassword(newPassword);
	}

	public void updatePassword(CloudCredentials credentials, String newPassword) {
		cc.updatePassword(credentials, newPassword);
	}

	public void unregister() {
		cc.unregister();
	}

	public OAuth2AccessToken login() {
		return cc.login();
	}

	public void logout() {
		cc.logout();
	}

	public List<CloudApplication> getApplications() {
		return cc.getApplications();
	}

	public CloudApplication getApplication(String appName) {
		return cc.getApplication(appName);
	}

	public CloudApplication getApplication(UUID appGuid) {
		return cc.getApplication(appGuid);
	}

	public ApplicationStats getApplicationStats(String appName) {
		return cc.getApplicationStats(appName);
	}

	public void createApplication(String appName, Staging staging, Integer memory, List<String> uris,
								  List<String> serviceNames) {
		cc.createApplication(appName, staging, memory, uris, serviceNames);
	}

	public void createApplication(String appName, Staging staging, Integer disk, Integer memory, List<String> uris,
								  List<String> serviceNames) {
		cc.createApplication(appName, staging, disk, memory, uris, serviceNames);
	}

	public void createService(CloudService service) {
		cc.createService(service);
	}

	@Override
	public void createUserProvidedService(CloudService service, Map<String, Object> credentials, String syslogDrainUrl) {
		cc.createUserProvidedService(service, credentials, syslogDrainUrl);
	}

	@Override
	public void createUserProvidedService(CloudService service, Map<String, Object> credentials) {
		cc.createUserProvidedService(service, credentials);
	}

	@Override
	public List<CloudRoute> deleteOrphanedRoutes() {
	return cc.deleteOrphanedRoutes();
	}

	public void uploadApplication(String appName, String file) throws IOException {
		cc.uploadApplication(appName, new File(file), null);
	}

	public void uploadApplication(String appName, File file) throws IOException {
		cc.uploadApplication(appName, file, null);
	}

	public void uploadApplication(String appName, File file, UploadStatusCallback callback) throws IOException {
		cc.uploadApplication(appName, file, callback);
	}

	public void uploadApplication(String appName, ApplicationArchive archive) throws IOException {
		cc.uploadApplication(appName, archive, null);
	}

	public void uploadApplication(String appName, ApplicationArchive archive, UploadStatusCallback callback) throws IOException {
		cc.uploadApplication(appName, archive, callback);
	}

	public StartingInfo startApplication(String appName) {
		return cc.startApplication(appName);
	}

	public void debugApplication(String appName, DebugMode mode) {
		cc.debugApplication(appName, mode);
	}

	public void stopApplication(String appName) {
		cc.stopApplication(appName);
	}

	public StartingInfo restartApplication(String appName) {
		return cc.restartApplication(appName);
	}

	public void deleteApplication(String appName) {
		cc.deleteApplication(appName);
	}

	public void deleteAllApplications() {
		cc.deleteAllApplications();
	}

	public void deleteAllServices() {
		cc.deleteAllServices();
	}

	public void updateApplicationDiskQuota(String appName, int disk) {
		cc.updateApplicationDiskQuota(appName, disk);
	}

	public void updateApplicationMemory(String appName, int memory) {
		cc.updateApplicationMemory(appName, memory);
	}

	public void updateApplicationInstances(String appName, int instances) {
		cc.updateApplicationInstances(appName, instances);
	}

	public void updateApplicationServices(String appName, List<String> services) {
		cc.updateApplicationServices(appName, services);
	}

	public void updateApplicationStaging(String appName, Staging staging) {
		cc.updateApplicationStaging(appName, staging);
	}

	public void updateApplicationUris(String appName, List<String> uris) {
		cc.updateApplicationUris(appName, uris);
	}

	public void updateApplicationEnv(String appName, Map<String, String> env) {
		cc.updateApplicationEnv(appName, env);
	}

	public void updateApplicationEnv(String appName, List<String> env) {
		cc.updateApplicationEnv(appName, env);
	}

	/**
	 * @deprecated use {@link #streamLogs(String, ApplicationLogListener)} or {@link #getRecentLogs(String)}
	 */
	public Map<String, String> getLogs(String appName) {
		return cc.getLogs(appName);
	}

	public StreamingLogToken streamLogs(String appName, ApplicationLogListener listener) {
	    return cc.streamLogs(appName, listener);
	}

	public List<ApplicationLog> getRecentLogs(String appName) {
		return cc.getRecentLogs(appName);
	}

	/**
	 * @deprecated use {@link #streamLogs(String, ApplicationLogListener)} or {@link #getRecentLogs(String)}
	 */
	public Map<String, String> getCrashLogs(String appName) {
		return cc.getCrashLogs(appName);
	}

	public String getStagingLogs(StartingInfo info, int offset) {
		return cc.getStagingLogs(info, offset);
	}

	public String getFile(String appName, int instanceIndex, String filePath) {
		return cc.getFile(appName, instanceIndex, filePath, 0, -1);
	}

	public String getFile(String appName, int instanceIndex, String filePath, int startPosition) {
		Assert.isTrue(startPosition >= 0,
				startPosition + " is not a valid value for start position, it should be 0 or greater.");
		return cc.getFile(appName, instanceIndex, filePath, startPosition, -1);
	}

	public String getFile(String appName, int instanceIndex, String filePath, int startPosition, int endPosition) {
		Assert.isTrue(startPosition >= 0,
				startPosition + " is not a valid value for start position, it should be 0 or greater.");
		Assert.isTrue(endPosition > startPosition,
				endPosition + " is not a valid value for end position, it should be greater than startPosition " +
						"which is " + startPosition + ".");
		return cc.getFile(appName, instanceIndex, filePath, startPosition, endPosition - 1);
	}

	public void openFile(String appName, int instanceIndex, String filePath, ClientHttpResponseCallback callback) {
		cc.openFile(appName, instanceIndex, filePath, callback);
	}

	public String getFileTail(String appName, int instanceIndex, String filePath, int length) {
		Assert.isTrue(length > 0, length + " is not a valid value for length, it should be 1 or greater.");
		return cc.getFile(appName, instanceIndex, filePath, -1, length);
	}

	// list services, un/provision services, modify instance

	public List<CloudService> getServices() {
		return cc.getServices();
	}

	public List<CloudServiceBroker> getServiceBrokers() {
		return cc.getServiceBrokers();
	}

	public CloudService getService(String service) {
		return cc.getService(service);
	}

	public void deleteService(String service) {
		cc.deleteService(service);
	}

	public List<CloudServiceOffering> getServiceOfferings() {
		return cc.getServiceOfferings();
	}

	public void bindService(String appName, String serviceName) {
		cc.bindService(appName, serviceName);
	}

	public void unbindService(String appName, String serviceName) {
		cc.unbindService(appName, serviceName);
	}

	public InstancesInfo getApplicationInstances(String appName) {
		return cc.getApplicationInstances(appName);
	}

	public InstancesInfo getApplicationInstances(CloudApplication app) {
		return cc.getApplicationInstances(app);
	}

	public CrashesInfo getCrashes(String appName) {
		return cc.getCrashes(appName);
	}

	public List<CloudStack> getStacks() {
		return cc.getStacks();
	}

	public CloudStack getStack(String name) {
		return cc.getStack(name);
	}

	public void rename(String appName, String newName) {
		cc.rename(appName, newName);
	}

	public List<CloudDomain> getDomainsForOrg() {
		return cc.getDomainsForOrg();
	}

	public List<CloudDomain> getPrivateDomains() {
		return cc.getPrivateDomains();
	}

	public List<CloudDomain> getSharedDomains() {
		return cc.getSharedDomains();
	}

	public List<CloudDomain> getDomains() {
		return cc.getDomains();
	}

	public CloudDomain getDefaultDomain() {
		return cc.getDefaultDomain();
	}

	public void addDomain(String domainName) {
		cc.addDomain(domainName);
	}

	public void deleteDomain(String domainName) {
		cc.deleteDomain(domainName);
	}

	public void removeDomain(String domainName) {
		cc.removeDomain(domainName);
	}

	public List<CloudRoute> getRoutes(String domainName) {
		return cc.getRoutes(domainName);
	}

	public void addRoute(String host, String domainName) {
		cc.addRoute(host, domainName);
	}

	public void deleteRoute(String host, String domainName) {
		cc.deleteRoute(host, domainName);
	}

	public void registerRestLogListener(RestLogCallback callBack) {
		cc.registerRestLogListener(callBack);
	}

	public void unRegisterRestLogListener(RestLogCallback callBack) {
		cc.unRegisterRestLogListener(callBack);
	}

}
