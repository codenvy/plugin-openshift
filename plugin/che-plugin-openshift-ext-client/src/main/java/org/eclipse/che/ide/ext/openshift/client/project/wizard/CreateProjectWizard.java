/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.ide.ext.openshift.client.project.wizard;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singletonList;
import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_APPLICATION_VARIABLE_NAME;
import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_NAMESPACE_VARIABLE_NAME;
import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_PROJECT_TYPE_ID;

import com.google.common.base.Strings;
import com.google.gwt.core.client.JsArrayMixed;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.promises.client.PromiseProvider;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.api.workspace.shared.dto.SourceStorageDto;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.project.MutableProjectConfig;
import org.eclipse.che.ide.api.wizard.AbstractWizard;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftServiceClient;
import org.eclipse.che.ide.ext.openshift.client.dto.NewApplicationRequest;
import org.eclipse.che.ide.ext.openshift.shared.dto.BuildConfig;
import org.eclipse.che.ide.ext.openshift.shared.dto.BuildSource;
import org.eclipse.che.ide.ext.openshift.shared.dto.DeploymentConfig;
import org.eclipse.che.ide.ext.openshift.shared.dto.GitBuildSource;
import org.eclipse.che.ide.ext.openshift.shared.dto.ImageStream;
import org.eclipse.che.ide.ext.openshift.shared.dto.Parameter;
import org.eclipse.che.ide.ext.openshift.shared.dto.Project;
import org.eclipse.che.ide.ext.openshift.shared.dto.Route;
import org.eclipse.che.ide.ext.openshift.shared.dto.Service;
import org.eclipse.che.ide.ext.openshift.shared.dto.Template;

/**
 * Complete wizard handler. Handle create operation and tries to create configs on openshift.
 *
 * @author Vlad Zhukovskiy
 * @author Sergii Leschenko
 * @author Vitaliy Guliy
 */
public class CreateProjectWizard extends AbstractWizard<NewApplicationRequest> {
  private static final String APPLICATION_PARAMETER_NAME = "APPLICATION_NAME";
  private static final String APPLICATION_LABEL_NAME = "application";

  private final OpenshiftServiceClient openshiftClient;
  private final DtoFactory dtoFactory;
  private final AppContext appContext;
  private PromiseProvider promises;

  private Project osProject;
  private Template osTemplate;
  private org.eclipse.che.ide.api.resources.Project cdProject;

  @Inject
  public CreateProjectWizard(
      @Assisted NewApplicationRequest newApplicationRequest,
      OpenshiftServiceClient openshiftClient,
      DtoFactory dtoFactory,
      AppContext appContext,
      PromiseProvider promises) {
    super(newApplicationRequest);
    this.openshiftClient = openshiftClient;
    this.dtoFactory = dtoFactory;
    this.appContext = appContext;
    this.promises = promises;
  }

  @Override
  public void complete(final CompleteCallback callback) {
    ensureOSProjectCreated()
        .thenPromise(
            new Function<Void, Promise<Void>>() {
              @Override
              public Promise<Void> apply(Void arg) throws FunctionException {
                return processOSTemplate();
              }
            })
        .thenPromise(
            new Function<Void, Promise<Void>>() {
              @Override
              public Promise<Void> apply(Void arg) throws FunctionException {
                return processOSTemplateMeta();
              }
            })
        .thenPromise(
            new Function<Void, Promise<Void>>() {
              @Override
              public Promise<Void> apply(Void arg) throws FunctionException {
                return importOSProject();
              }
            })
        .thenPromise(
            new Function<Void, Promise<Void>>() {
              @Override
              public Promise<Void> apply(Void arg) throws FunctionException {
                return setupOSMixin();
              }
            })
        .then(
            new Operation<Void>() {
              @Override
              public void apply(Void ignored) throws OperationException {
                callback.onCompleted();
              }
            })
        .catchError(
            new Operation<PromiseError>() {
              @Override
              public void apply(PromiseError error) throws OperationException {
                callback.onFailure(error.getCause());
              }
            });
  }

  private Promise<Void> ensureOSProjectCreated() {
    if (dataObject.getProject() != null) {
      osProject = dataObject.getProject();

      return promises.resolve(null);
    } else if (dataObject.getProjectRequest() != null) {
      return openshiftClient
          .createProject(dataObject.getProjectRequest())
          .then(
              new Function<Project, Void>() {
                @Override
                public Void apply(Project project) throws FunctionException {
                  osProject = project;
                  dataObject.setProject(project);

                  return null;
                }
              });
    }

    throw new IllegalStateException("Project is undefined");
  }

  private Promise<Void> processOSTemplate() {
    checkNotNull(osProject);

    final Template template = dataObject.getTemplate();
    setUpApplicationName(template);

    return openshiftClient
        .processTemplate(osProject.getMetadata().getName(), template)
        .then(
            new Function<Template, Void>() {
              @Override
              public Void apply(Template template) throws FunctionException {

                osTemplate = template;

                return null;
              }
            });
  }

  private Promise<Void> processOSTemplateMeta() {
    checkNotNull(osTemplate);

    final List<Promise<?>> promises = new ArrayList<>();

    for (Object o : osTemplate.getObjects()) {
      final JSONObject object = (JSONObject) o;
      final JSONValue metadata = object.get("metadata");
      final String namespace = osProject.getMetadata().getName();
      ((JSONObject) metadata).put("namespace", new JSONString(namespace));
      final String kind = ((JSONString) object.get("kind")).stringValue();
      switch (kind) {
        case "DeploymentConfig":
          DeploymentConfig dConfig =
              dtoFactory.createDtoFromJson(object.toString(), DeploymentConfig.class);
          promises.add(openshiftClient.createDeploymentConfig(dConfig));
          break;
        case "BuildConfig":
          BuildConfig bConfig = dtoFactory.createDtoFromJson(object.toString(), BuildConfig.class);

          HashMap<String, String> importOptions = new HashMap<>();
          BuildSource source = bConfig.getSpec().getSource();

          GitBuildSource gitSource = source.getGit();
          String branch = gitSource.getRef();
          if (!Strings.isNullOrEmpty(branch)) {
            importOptions.put("branch", branch);
          }

          String contextDir = source.getContextDir();
          if (!Strings.isNullOrEmpty(contextDir)) {
            importOptions.put("keepDir", contextDir);
          }

          dataObject
              .getProjectConfigDto()
              .withSource(
                  dtoFactory
                      .createDto(SourceStorageDto.class)
                      .withType("git")
                      .withLocation(gitSource.getUri())
                      .withParameters(importOptions));

          promises.add(openshiftClient.createBuildConfig(bConfig));
          break;
        case "ImageStream":
          ImageStream stream = dtoFactory.createDtoFromJson(object.toString(), ImageStream.class);
          promises.add(openshiftClient.createImageStream(stream));
          break;
        case "Route":
          Route route = dtoFactory.createDtoFromJson(object.toString(), Route.class);
          promises.add(openshiftClient.createRoute(route));
          break;
        case "Service":
          Service service = dtoFactory.createDtoFromJson(object.toString(), Service.class);
          promises.add(openshiftClient.createService(service));
          break;
      }
    }

    return Promises.all(promises.toArray(new Promise<?>[promises.size()]))
        .then(
            new Function<JsArrayMixed, Void>() {
              @Override
              public Void apply(JsArrayMixed result) throws FunctionException {
                return null;
              }
            });
  }

  private Promise<Void> importOSProject() {
    final MutableProjectConfig config = new MutableProjectConfig(dataObject.getProjectConfigDto());

    return appContext
        .getWorkspaceRoot()
        .importProject()
        .withBody(config)
        .send()
        .then(
            new Function<org.eclipse.che.ide.api.resources.Project, Void>() {
              @Override
              public Void apply(org.eclipse.che.ide.api.resources.Project project)
                  throws FunctionException {
                cdProject = project;

                return null;
              }
            });
  }

  private Promise<Void> setupOSMixin() {
    checkNotNull(cdProject);
    checkNotNull(osProject);

    MutableProjectConfig updateConfig = new MutableProjectConfig(cdProject);
    updateConfig
        .getAttributes()
        .put(OPENSHIFT_APPLICATION_VARIABLE_NAME, singletonList(cdProject.getName()));
    updateConfig
        .getAttributes()
        .put(OPENSHIFT_NAMESPACE_VARIABLE_NAME, singletonList(osProject.getMetadata().getName()));
    updateConfig.getMixins().add(OPENSHIFT_PROJECT_TYPE_ID);

    return cdProject
        .update()
        .withBody(updateConfig)
        .send()
        .then(
            new Function<org.eclipse.che.ide.api.resources.Project, Void>() {
              @Override
              public Void apply(org.eclipse.che.ide.api.resources.Project project)
                  throws FunctionException {
                cdProject = project;

                return null;
              }
            });
  }

  /**
   * Sets value for application name parameter and add it to all objects in template
   *
   * @param template template for setting of application name
   */
  private void setUpApplicationName(Template template) {
    Parameter appNameParam = null;
    for (Parameter parameter : template.getParameters()) {
      if (APPLICATION_PARAMETER_NAME.equals(parameter.getName())) {
        appNameParam = parameter;
        break;
      }
    }

    if (appNameParam == null) {
      appNameParam = dtoFactory.createDto(Parameter.class).withName(APPLICATION_PARAMETER_NAME);
      template.getParameters().add(appNameParam);
    }

    appNameParam.setValue(dataObject.getProjectConfigDto().getName());

    // set up application name labels in objects of template
    for (Object object : template.getObjects()) {
      final JSONString kind = getJsonStringOrNull((JSONObject) object, "kind");

      JSONObject metadata = provideExistingJsonObject((JSONObject) object, "metadata");

      if (kind != null && "BuildConfig".equals(kind.stringValue())) {
        metadata.put("name", new JSONString("${" + APPLICATION_PARAMETER_NAME + "}"));
      }

      JSONObject labels = provideExistingJsonObject(metadata, "labels");
      labels.put(APPLICATION_LABEL_NAME, new JSONString("${" + APPLICATION_PARAMETER_NAME + "}"));
    }
  }

  /**
   * Returns {@link JSONObject} instance that is field with specified name of specified source
   * object.<br>
   * Note: creates new {@link JSONObject} in given source if it is absent or it is not instance of
   * {@link JSONObject}.
   */
  private JSONObject provideExistingJsonObject(JSONObject source, String objectName) {
    final JSONValue jsonValue = source.get(objectName);
    if (jsonValue == null || !(jsonValue instanceof JSONObject)) {
      JSONObject result = new JSONObject();
      source.put(objectName, result);
      return result;
    }
    return ((JSONObject) jsonValue);
  }

  /**
   * Returns {@link JSONString} instance that is field with specified name of specified source
   * object <br>
   * or null if it is absent or it is not instance of {@link JSONString}.
   */
  private JSONString getJsonStringOrNull(JSONObject source, String name) {
    final JSONValue jsonValue = source.get(name);
    if (jsonValue == null || !(jsonValue instanceof JSONString)) {
      return null;
    }
    return ((JSONString) jsonValue);
  }
}
