
##############################
# Openshift configuration
$che_openshift_api_endpoint = getValue("CHE_OPENSHIFT_API_ENDPOINT","https://api.codenvy.openshift.com")
$che_oauth_openshift_authuri = getValue("CHE_OAUTH_OPENSHIFT_AUTHURI","https://api.codenvy.openshift.com/oauth/authorize")
$che_oauth_openshift_tokenuri = getValue("CHE_OAUTH_OPENSHIFT_TOKENURI","https://api.codenvy.openshift.com/oauth/token")
$che_oauth_openshift_clientid = getValue("CHE_OAUTH_OPENSHIFT_CLIENTID","che")
$che_oauth_openshift_clientsecret = getValue("CHE_OAUTH_OPENSHIFT_CLIENTSECRET","ask-admin")
$che_oauth_openshift_redirecturis = getValue("CHE_OAUTH_OPENSHIFT_REDIRECTURIS","http://localhost:\${SERVER_PORT}/wsmaster/api/oauth/callback")

include addon
