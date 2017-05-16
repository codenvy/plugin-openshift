class addon {
  # creating openshift.properties
  file { "/opt/che/config/openshift.properties":
    ensure  => "present",
    content => template("addon/openshift.properties.erb"),
    mode    => "644",
  }
}
