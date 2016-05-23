/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.ext.openshift.client.deploy;

import org.eclipse.che.ide.ext.openshift.shared.dto.BuildConfig;
import org.eclipse.che.ide.ext.openshift.shared.dto.BuildConfigSpec;
import org.eclipse.che.ide.ext.openshift.shared.dto.BuildOutput;
import org.eclipse.che.ide.ext.openshift.shared.dto.Container;
import org.eclipse.che.ide.ext.openshift.shared.dto.DeploymentConfig;
import org.eclipse.che.ide.ext.openshift.shared.dto.DeploymentConfigSpec;
import org.eclipse.che.ide.ext.openshift.shared.dto.ImageStream;
import org.eclipse.che.ide.ext.openshift.shared.dto.ImageStreamStatus;
import org.eclipse.che.ide.ext.openshift.shared.dto.ImageStreamTag;
import org.eclipse.che.ide.ext.openshift.shared.dto.NamedTagEventList;
import org.eclipse.che.ide.ext.openshift.shared.dto.ObjectMeta;
import org.eclipse.che.ide.ext.openshift.shared.dto.ObjectReference;
import org.eclipse.che.ide.ext.openshift.shared.dto.PodSpec;
import org.eclipse.che.ide.ext.openshift.shared.dto.PodTemplateSpec;
import org.eclipse.che.ide.ext.openshift.shared.dto.Route;
import org.eclipse.che.ide.ext.openshift.shared.dto.RouteSpec;
import org.eclipse.che.ide.ext.openshift.shared.dto.Service;
import org.eclipse.che.ide.ext.openshift.shared.dto.ServiceSpec;
import org.eclipse.che.ide.ext.openshift.shared.dto.TagEvent;

import java.util.List;

/**
 * Class for combining objects that are related to application
 *
 * @author Sergii Leschenko
 */
public class Application {
    /**
     * {@link BuildConfig} is a basic object of application. We can search other objects by this object.<br/>
     * {@link BuildConfig} is linked to {@link ImageStreamTag} by {@link ObjectReference} that you can get by calling <br/>
     * {@link BuildConfig#getSpec()} and then {@link BuildConfigSpec#getOutput()} and then {@link BuildOutput#getTo()}.
     *
     * <p/>{@link ObjectReference} should return {@code "ImageStreamTag"} when you call {@link ObjectReference#getKind()} <br/>
     * and image stream tag name as {@code "imageStreamName:tagName"} when you call {@link ObjectReference#getName()}
     */
    private BuildConfig buildConfig;

    /**
     * {@link BuildConfig} sends build result to image stream tag of this image.<br/>
     */
    private ImageStream outputImageStream;

    /**
     * ImageStreamTagReference is link between {@link BuildConfig} and {@link DeploymentConfig}.
     *
     * <p/>If {@link #outputImageStream} doesn't have required build tag, then this reference equals to <br/>
     * {@code "imageStreamName:tagName"}.<br/>
     * And if {@link #outputImageStream} has required tag - the value can be retrieved from {@link #outputImageStream}<br/>
     * You should find needed instance of {@link NamedTagEventList} by calling {@link ImageStream#getStatus()}<br/>
     * and then {@link ImageStreamStatus#getTags()}. Then you can get last item from {@link NamedTagEventList#getItems()} and then <br/>
     * you can get tag reference by calling {@link TagEvent#getDockerImageReference()} from found instance
     */
    private String imageStreamTagReference;

    /**
     * {@link DeploymentConfig} is linked with {@link BuildConfig} by {@link #imageStreamTagReference}.
     *
     * <p/>{@link Container#getImage()} of {@link DeploymentConfig} should equal to {@link #imageStreamTagReference}.<br/>
     * {@link Container#getImage()} can have no imageStreamTag. By default it equals to 'latest'.<br/>
     * Needed instance of {@link Container} can be retrieved by next calling {@link DeploymentConfig#getSpec()} <br/>
     * then {@link DeploymentConfigSpec#getTemplate()} and then {@link PodTemplateSpec#getSpec()}<br/>
     * and you should get first item from {@link PodSpec#getContainers()}
     */
    private List<DeploymentConfig> deploymentConfigs;

    /**
     * {@link Service} is linked to {@link DeploymentConfig}.
     *
     * <p/>{@link DeploymentConfig} creates new pod that will contain all labels from {@link ObjectMeta} <br/>
     * that you can get by calling next methods {@link DeploymentConfig#getSpec()} and then {@link DeploymentConfigSpec#getTemplate()}<br/>
     * and then {@link PodTemplateSpec#getMetadata()} {@link ObjectMeta}.
     * And {@link Service} selects pods by selectors that you can get by {@link Service#getSpec()} and then <br/>
     * {@link ServiceSpec#getSelector()}. So {@link Service} relates to {@link DeploymentConfig} if any of pair(key=value) from <br/>
     * service's {@code selector} equals to any of pair(key=value) from PodTemplateSpec's labels
     */
    private List<Service> services;

    /**
     * {@link Route} is linked to {@link Service} by {@link ObjectReference} that you can get by calling <br/>
     * {@link Route#getSpec()} and then {@link RouteSpec#getTo()}.
     *
     * <p>{@link ObjectReference} should return {@code "Service"} when you call {@link ObjectReference#getKind()}}<br/>
     * and some name of service when you call {@link ObjectReference#getName()}
     */
    private List<Route> routes;

    public BuildConfig getBuildConfig() {
        return buildConfig;
    }

    public void setBuildConfig(BuildConfig buildConfig) {
        this.buildConfig = buildConfig;
    }

    public ImageStream getOutputImageStream() {
        return outputImageStream;
    }

    public void setOutputImageStream(ImageStream outputImageStream) {
        this.outputImageStream = outputImageStream;
    }

    public String getImageStreamTagReference() {
        return imageStreamTagReference;
    }

    public void setImageStreamTagReference(String imageStreamTagReference) {
        this.imageStreamTagReference = imageStreamTagReference;
    }

    public List<DeploymentConfig> getDeploymentConfigs() {
        return deploymentConfigs;
    }

    public void setDeploymentConfigs(List<DeploymentConfig> deploymentConfigs) {
        this.deploymentConfigs = deploymentConfigs;
    }

    public List<Service> getServices() {
        return services;
    }

    public void setServices(List<Service> services) {
        this.services = services;
    }

    public List<Route> getRoutes() {
        return routes;
    }

    public void setRoutes(List<Route> routes) {
        this.routes = routes;
    }
}
