v0.12:
* Add support for platform wide acls for control center in teh topology description file.
* Port support for schema registry and confluent control center roles in the rbac provider. Now the two providers are future pair.
* Fix the usage of the source field, it should not be mandatory as the others list and the custom topology serdes can cover a dynamic list 
of fields between the team and the first project separator. 
* Add code to support passing a directory, only topologies for a single team are supported for now.
* Add support for passing a directory, instead of a single file as topology. If a directory is passed, all topologies are expected to be from
the same team and all subsequent projects will be at append one after the other.
* Implement the necessary methods to pass back the status from the rbac modules, so the reply and clean up is possible with multiple runs


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
