# Copyright (c) 2012-2016 Codenvy, S.A.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#

# Initializes an empty directory with the templates needed to configure and run Che.
#

FROM eclipse/che-init:nightly
COPY /addon/addon.env /etc/puppet/addon.env
COPY /addon/addon.pp /etc/puppet/manifests/
COPY /addon/modules/ /etc/puppet/addon/
