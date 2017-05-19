class addon {
  # creating openshift.properties
  file { "/opt/che/config/openshift.properties":
    ensure  => "present",
    content => template("addon/openshift.properties.erb"),
    mode    => "644",
  } ->
  file { "/opt/che/config/plugin-conf":
    ensure  => "directory",
    mode    => "755",
  } ->
  # creating ws-agent.properties
  file { "/opt/che/config/plugin-conf/ws-agent.properties":
    ensure  => "present",
    content => template("addon/ws-agent.properties.erb"),
    mode    => "644",
  }
}
