latest:
* Add support for platform wide acls for schema registry in the topology description file. 

v0.11:
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
