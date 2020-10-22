v1.0.0:
* Extended the logging support to many components and classes in the KTB with the idea to support better troubleshooting. (Closes #101)
* Add a filter to delete only topic configurations that are explicitly set, not the ones that use default values. (closes #99)
* Fix a bug with wrongly set PatternType for Consumer group acls level. (closes #111)
* Add test and documentation for list type config values as handled using the new config library.
* Add a Health Check function used during the creation of the internal admin client. This function will describe the cluster and perse test if the setup credentials are ok.
This fixes (#112)
 
v1.0.0-rc2:
* Check for required configuration values for the configuration of RBAC, if not present it raises a Configuration error
* Update Log4j version to 2.13.3 to prevent CVE-2020-9488
* Add an option to not delete internal topics, including an option configure rules to filter internal topics that can't not be removed config with  _kafka.internal.topic.prefixes_,
by default this filters all topics starting with underscore. Users can configure more than one filter in the property file.
* Made internal topics for Kafka Connect, Schema Registry and Confluent Control Center configurable.
By introducing this, users can now handle non-standard instances of Schema Registry or having for example more than
a single Kafka Connect cluster (situation more than common).
* Add a rest api component, available under server-api and created using the Micronaut Framework. This component is an independent artifact, that wrap the 
Kafka Topology Builder library in a single and uniform web application that can be used to automate and centralise the configuration management for clusters.
* Remove the requirement of writing in the configuration file the access control method, as of now will be ACLs as default value and if other (like RBAC) is required
it will have to be properly configured.
* Merge #22 to provide the initial support for schema management. Now you can manage schemas with Confluent Schema Registry using the Kafka Topology Builder. Thanks Kiril.
* Add an option to specify custom consumer groups in the topology descriptor
* Add option to increase the partition count in an already created topic.
* Add support for cluster level acls for kafka, schema registry and kafka connect. This solve issues #42.
* Add support for schema level permissions in the rbac mode. This solve issue #44.
* Add support for connect level permissions in the rbac mode. This solve issue #45.
* Implement an initial method for building an execution plan including an option to print a dry-run. This solve issue #62.
* Rename team as context as it was a confusing wording. This solve issue #68.
* Fix bug on removal of acls. This solve issue #72.
* Fix bug with schema registration and file path reference,  now by default all are relative to the main directory where the topology file is staying.
* Improved delete and general management of acls logic, only create what is necessary, only delete what is mandatory.
* Fixed a problem with the schema registry client that got no configs, so it would not be possible to use with security contrain systems such as confluent cloud.
* Add the possibility to define a topic and project pattern using the jinja format, like this users can tune the order of the variables without hampeting the full idea of the structure
* Add a framework to add incoming validation for topology files, this could be configured with the topology.validations config array.
* Added the lightbend config library to support a more structured config management, including usage of environment variables and multiple config formats.
* Made the broker CLI param optional, however one of both config or CLI has to be present, CLI takes always precedence over the config value.

v1.0.0-rc1:
* Add support for platform wide acls for control center in teh topology description file.
* Port support for schema registry and confluent control center roles in the rbac provider. Now the two providers are future pair.
* Fix the usage of the source field, it should not be mandatory as the others list and the custom topology serdes can cover a dynamic list 
of fields between the team and the first project separator. 
* Add code to support passing a directory, only topologies for a single team are supported for now.
* Add support for passing a directory, instead of a single file as topology. If a directory is passed, all topologies are expected to be from
the same team and all subsequent projects will be at append one after the other.
* Implement the necessary methods to pass back the status from the rbac modules, so the reply and clean up is possible with multiple runs
* Added an option to use redis as external system to keep the acls status list. Now people can use either files or redis.
Note in the future this subsystem should be externalised as plugins
* raise up error handling in case of non available cluster
* Make usage of the maven jdeb plugin to build a deb package to use in distributions such as Debian and relative like the Ubuntu family
* Fix parser problem with non required parameters, such as the ones for the acls where an empty array had to be in place, now all principals for acls if not required can be ignored and will have no effect
* Ammend param and typo issues with rbac that where not facilitating the execution flow, updated as well the examples to match a correct execution flow

v0.11:
* Add support for platform wide acls for schema registry in the topology description file.
* Rebird the option to disable deletes when not required anymore.
* Code refactoring in several classes
* add a option, activated by default to print the current status of topics created and acls being set after each run. this is very valuable to know the current state of the cluster after each execution
* add release mechanism for docker images with the kafka topology builder (#21)
* Add version number to maven-compiler-plugin (#16)
* Add support to keeping notes of generated acls (#15)
* Add support for storing the current generated acls within a state file. This is useful to delete
acls that are known and generated previously by the topology builder.

v0.10.3:
* updated ACLs for producers and consumers to include the describe permission in order to properly
allow for metadata recollection.
* Fix wrong verification for parameters in the CLI.

v0.10.2:
* Add improved connection handling when talking with the RBAC MDS server
* Extended the test suite for rbac and isoleted test with the SASL plain suit. 

v0.10.1:
* Fix the RPM builder script, build an rpm that create proper users for running the application.
* Fix param passing to the RBAC provider from the properties file.
* Add more clear example configuration within the RPM artifacts.

v0.10:
* Separate access control providers between simple ACLs and RBAC provider.
* Introduced an option to select with Access Control mechanism is used using the application configuration file.
* Add initial rbac support for application roles.
* Add the option to define custom rbac (per project) roles. This option allows you to define inside the project level role and principals mapping such as ResourceOwner, ... this will be automatically applied per topic if using the rbac acces control provider

v0.9:
* RPM package for installation in Redhat based systems.
* Fully descriptive managed ACLs (via a descriptor file) - Create, Read, Update and Delete.
* Fully descriptive and structured topic management (via a descriptor file) - Create, Read, Update and Delete.
* Simple Roles for Connectors, Streams application, Consumers and Producers.
