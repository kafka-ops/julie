Define custom roles for JulieOps
*******************************

While JulieOps offer you as a user the possibility to manage the ACLs (and RBAC if you're using the Confluent Platform) for most common
applications deployments such as Consumers, Producers, Kafka Streams, Connectors and ksqlDB, it would be for some cases amazing to be
be able to keep using the powerful abstractions of JulieOps but provide your own set of ACLs.

For example:

* If you are deploying a custom App and aim to give application specific roles
* Deploying applications that might not fit our of the box with generic permissions provided by JulieOps
* Or just if you are building your own roles based on Simple ACLs or Confluent RBAC

and more.

But, how can you get this with JulieOps.

Defining the your roles
-----------

First thing is to define your roles in a configuration file, this file should look like this:

.. code-block:: YAML

    roles:
      - name: "app"
        acls:
          - resourceType: "Topic"
            resourceName: "{{topic}}"
            patternType: "PREFIXED"
            host: "*"
            operation: "ALL"
            permissionType: "ALLOW"
          - resourceType: "Topic"
            resourceName: "sourceTopic"
            patternType: "LITERAL"
            host: "*"
            operation: "ALL"
            permissionType: "READ"
          - resourceType: "Topic"
            resourceName: "targetTopic"
            patternType: "LITERAL"
            host: "*"
            operation: "ALL"
            permissionType: "WRITE"
          - resourceType: "Group"
            resourceName: "{{group}}"
            patternType: "PREFIXED"
            host: "*"
            operation: "READ"
            permissionType: "ALLOW"

if you are using Confluent Platform RBAC functionality to define your own Access Control management, the only different property
per acl is **role**, so the file might look like this:

.. code-block:: YAML

    roles:
      - name: "app"
        acls:
          - resourceType: "Topic"
            resourceName: "{{topic}}"
            patternType: "PREFIXED"
            host: "*"
            role: "ResourceOwner"
          - resourceType: "Topic"
            resourceName: "sourceTopic"
            patternType: "LITERAL"
            host: "*"
            role: "DeveloperRead"
          - resourceType: "Topic"
            resourceName: "targetTopic"
            patternType: "LITERAL"
            host: "*"
            role: "DeveloperWrite"
          - resourceType: "Group"
            resourceName: "{{group}}"
            patternType: "PREFIXED"
            host: "*"
            role: "DeveloperRead"
          - resourceType: "Subject"
            resourceName: "Subject:foo"
            patternType: "LITERAL"
            host: "*"
            role: "DeveloperRead"
          - resourceType: "Connector"
            resourceName: "Connector:con"
            patternType: "LITERAL"
            host: "*"
            role: "SecurityAdmin"
          - resourceType: "KsqlCluster"
            resourceName: "KsqlCluster:ksql-cluster"
            patternType: "LITERAL"
            host: "*"
            role: "ResourceOwner"


Plug this into JulieOps
-----------

Once the roles are define, the only thing you need to do is to configure your deployment to use it. This can be done using this
configuration variable in your property file:


.. code-block:: JAVA

    julie.roles=/path/to/the/roles/file


How would my new topology file look like
-----------

Once the new roles are setup, your topology can start using them just as the previous "hardcoded" roles.
Your topology file could look like this:


.. code-block:: YAML

    context: "contextOrg"
    source: "source"
    projects:
      - name: "foo"
        foo:
          - principal: "User:banana"
            group: "foo"
        bar:
          - principal: "User:bandana"
            group: "bar"


More generic usage of roles
-----------

Lets assume you need to deploy multiple instances of an application that is not willing to use julie topic hierarchy,
requires multiple topics and custom acls. You could define a role for each instance, or for each group/topic,
but that would clutter the roles file quickly. You would however like to have these topics in julie, for acl management
and for documentation. For example kafka mirror maker needs many topics which would be nice to group together in julie
config.

For brevity example below gives too much permissions (ALL), but works as example how feature works.

.. code-block:: YAML

    roles:
      - name: "mirrorMaker"
        acls:
          - resourceType: "Topic"
            resourceName: "{{statusTopic}}"
            patternType: "LITERAL"
            host: "*"
            operation: "ALL"
            permissionType: "ALLOW"
          - resourceType: "Topic"
            resourceName: "{{offsetTopic}}"
            patternType: "LITERAL"
            host: "*"
            operation: "ALL"
            permissionType: "ALLOW"
          - resourceType: "Topic"
            resourceName: "{{configTopic}}"
            patternType: "LITERAL"
            host: "*"
            operation: "ALL"
            permissionType: "ALLOW"
          - resourceType: "Topic"
            resourceName: "{{targetPrefix}}"
            patternType: "PREFIXED"
            host: "*"
            operation: "ALL"
            permissionType: "ALLOW"
          - resourceType: "Topic"
            resourceName: "{{offsetSyncTopic}}"
            patternType: "LITERAL"
            host: "*"
            operation: "ALL"
            permissionType: "ALLOW"
          - resourceType: "Topic"
            resourceName: "{{checkpointsTopic}}"
            patternType: "LITERAL"
            host: "*"
            operation: "ALL"
            permissionType: "ALLOW"
          - resourceType: "Cluster"
            resourceName: "kafka-cluster"
            patternType: "LITERAL"
            host: "*"
            operation: "DESCRIBE"
            permissionType: "ALLOW"
          - resourceType: "Cluster"
            resourceName: "kafka-cluster"
            patternType: "LITERAL"
            host: "*"
            operation: "DESCRIBE_CONFIGS"
            permissionType: "ALLOW"
          - resourceType: "Group"
            resourceName: "{{group}}"
            patternType: "LITERAL"
            host: "*"
            operation: "ALL"
            permissionType: "ALLOW"

With previous roles file mirror maker can be defined in a clutter free manner in a project.

.. code-block:: YAML

    context: "contextOrg"
    source: "source"
    projects:
      - name: "foo"
        mirrorMaker:
          - principal: "User:banana"
            group: "foo"
            statusTopic: "test-cluster-status"
            offsetTopic: "test-cluster-offsets"
            configTopic: "test-cluster-configs"
            targetPrefix: "target-prefix."
            offsetSyncTopic: "mm2-offset-syncs.test-mm.internal"
            checkpointsTopic: "test-mm.checkpoints.internal"

Somewhat viable alternative to this would be to use special_topics, but they limit to topic and producer/consumer acl:s.