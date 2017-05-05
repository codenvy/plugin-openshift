# Copyright (c) 2012-2016 Codenvy, S.A.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#   Tyler Jewell - Initial Implementation
#

# Initializes an empty directory with the templates needed to configure and run Che.
#
# build:
#   docker build -t codenvy/che-openshift-init:<version> .
#
# use (to copy files onto folder):
#   docker run -v $(pwd):/che codenvy/che-openshift-init:<version>
#
# use (to run puppet config):
#   docker run <puppet-mounts> --entrypoint=/usr/bin/puppet eclipse/che-init:<version> apply <puppet-apply-options>


FROM eclipse/che-init:nightly
