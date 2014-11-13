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

import org.apache.maven.plugin.MojoExecutionException;
import org.cloudfoundry.client.lib.ApplicationLogListener;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.InstanceInfo;
import org.cloudfoundry.maven.common.SystemProperties;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import static org.cloudfoundry.maven.common.UiUtils.renderApplicationLogEntry;


/**
 * Get file
 *
 * @author Julie Garrone
 * @since 1.0.3
 * @goal getFile
 * @phase process-sources
 */

public class GetFile extends AbstractApplicationAwareCloudFoundryMojo {

	/**
	 * @parameter expression="${cf.filepath}"
	 */
	private String filepath;

	/**
	 * @parameter expression="${cf.destpath}"
	 */
	private String destpath;
	
	/**
	 * Is getting file mandatory? (i.e. fails the build if can't).
   * @parameter expression="${cf.isGetFileMandatory}"
   */
  private Boolean isGetFileMandatory;

	/**
	 * If the file path was specified via the command line ({@link SystemProperties})
	 * then use that property. Otherwise return the filepath as injected via Maven.
	 *
	 * @return Returns the filepath.
	 */
	public String getFilepath() {
		final String property = getCommandlineProperty(SystemProperties.FILE_PATH);
		if (property != null) {
			return property;
		} else {
			return this.filepath;
		}
	}

	/**
	 * If the destination path was specified via the command line ({@link SystemProperties})
	 * then that property is used. Otherwise return the destpath,
	 * if destpath is Null return the "target/"+appname+"_"+filepath.
	 * 
	 * @return Returns the destination path where to create the file.
	 */
	public String getDestpath() {
		final String property = getCommandlineProperty(SystemProperties.DEST_PATH);
		if (property != null) {
			return property;
		}  else if (this.destpath == null) {
			return "target/"+getAppname()+"_"+filepath;
		} else {
			return this.destpath;
		}
	}
	
	/**
   * If the mandatory option was specified via the command line ({@link SystemProperties})
   * then use that property. Otherwise return the property as injected via Maven.
   *
   * @return Returns true if getting file is mandatory (i.e. fails the build if cant).
   */
  public Boolean getIsGetFileMandatory() {
    final String property = getCommandlineProperty(SystemProperties.IS_GET_FILE_MANDATORY);
    if (property != null) {
      return Boolean.parseBoolean(property);
    } else {
      return this.isGetFileMandatory;
    }
  }

	@Override
	protected void doExecute() throws MojoExecutionException {
		try {
			getLog().info(String.format("Getting file '%s' for '%s', for all instances", getFilepath(), getAppname()));	
			getLog().info("Getting instances list...");  
			List<InstanceInfo> instances=getClient().getApplicationInstances(getAppname()).getInstances();
			for(InstanceInfo instance:instances) {
			  getLog().info(String.format("Getting file for instance %d", instance.getIndex()));  
  			String fileString = getClient().getFile(getAppname(),instance.getIndex(), getFilepath());
  			File destFile = new File(getDestpath());
  			destFile=new File(destFile.getParent(),"instance_"+instance.getIndex()+"_"+destFile.getName());
  			File parent = destFile.getParentFile();
  			if(!parent.exists() && !parent.mkdirs()){
  			    throw new IllegalStateException("Couldn't create dir: " + parent);
  			}
  			FileUtils.writeStringToFile(destFile, fileString);
  			getLog().info(String.format("File '%s' of application '%s' for instance %d available at '%s'", getFilepath(), getAppname(),instance.getIndex(), destFile.getPath()));
			}
		} catch (CloudFoundryException e) {
			if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
				throw new MojoExecutionException(String.format("Application '%s' does not exist", getAppname()), e);
			} else {
			  if (!getIsGetFileMandatory().booleanValue()) {
			      getLog().warn(String.format("Error getting file for application '%s'. Error message: '%s'. Description: '%s'", getAppname(), e.getMessage(), e.getDescription()), e);            			    
			  } else {
			      throw new MojoExecutionException(String.format("Error getting file for application '%s'. Error message: '%s'. Description: '%s'", getAppname(), e.getMessage(), e.getDescription()), e);
			  }
			}
		} catch (Exception e) {
		  if (!getIsGetFileMandatory().booleanValue()) {
        getLog().warn(String.format("Error getting file for application '%s'. Error message: '%s'", getAppname(), e.getMessage()), e);                     
      } else {
         throw new MojoExecutionException(String.format("Error getting file for application '%s'. Error message: '%s'", getAppname(), e.getMessage()), e);
      }
    }
	}
}