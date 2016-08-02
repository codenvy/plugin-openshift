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
package org.eclipse.che.ide.ext.openshift.client.util;

import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.api.workspace.shared.dto.SourceStorageDto;
import org.eclipse.che.ide.api.resources.Project;
import org.eclipse.che.ide.dto.DtoFactory;

/**
 * //
 *
 * @author Vitalii Parfonov
 */
public class DtoConverter {


    public static ProjectConfigDto toDto(DtoFactory dtoFactory, Project project) {
        if (project == null) {
            return dtoFactory.createDto(ProjectConfigDto.class);
        }
        final SourceStorageDto sourceDto = dtoFactory.createDto(SourceStorageDto.class);

        if (project.getSource() != null) {
            sourceDto.setLocation(project.getSource().getLocation());
            sourceDto.setType(project.getSource().getType());
            sourceDto.setParameters(project.getSource().getParameters());
        }

        final ProjectConfigDto dto = dtoFactory.createDto(ProjectConfigDto.class)
                                               .withName(project.getName())
                                               .withPath(project.getPath())
                                               .withDescription(project.getDescription())
                                               .withType(project.getType())
                                               .withMixins(project.getMixins())
                                               .withAttributes(project.getAttributes())
                                               .withSource(sourceDto);

        return dto;
    }
}
