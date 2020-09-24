What can you do with Kafka Topology Builder
*******************************

In a nutshell with Kafka Topology Builder you can manage your topics and acls in an structure and autonomous way.
As well you can manage your topics schemas as register.

In this chapter we will introduce the different things one can configure in each topology file(s).

Topics
-----------
The first and foremost important thing we aim to manage is topics, setup the partitions count and replication factor and as well configure their specific characteristics.

Partitions Count and Replication Factor
^^^^^^^^^^^

In following configuration the reader is seeing an example of how to create a topic with _replication.factor_ and _num.partitions_, in that case all one.

.. code-block:: YAML

  ---
    context: "context"
    projects:
      - name: "projectA"
        topics:
          - name: "foo"
            config:
              replication.factor: "1"
              num.partitions: "1"

Users can as well increase later the number of partitions and the Topology Builder will handle it properly.

ACLs
-----------

In the topology descriptor files users can create permissions for different types of applications, Consumers, Producers, Kafka streams apps or Kafka Connectors.
With this roles, users can easy create the permissions that map directly to their needs.

Consumers
^^^^^^^^^^^

As a user you can configure consumers for each project.
Consumer have a principal and optionally a consumer group name, if the consumer group is not defined a group ACL with "*" will be created.


.. code-block:: YAML

  ---
    context: "context"
    source: "source"
    projects:
      - name: "foo"
        consumers:
          - principal: "User:App0"

Consumer definition with principal "User:App0" and without an specific consumer group, for this configuration an ACL will be created to accept any consumer group.

.. code-block:: YAML

  ---
    context: "context"
    source: "source"
    projects:
      - name: "foo"
        consumers:
          - principal: "User:App0"
            group: "foo

Consumer definition with principal "User:App0" and consumer group name "foo".


Connectors
^^^^^^^^^^^

In a similar fashion as with the previous roles, users can setup specific Kafka Connect setups.
Each one of them will be composed of a principal, this would be the user used by the connect to
connect to Kafka and a list of topics that this principal needs to read or write to, remember
Connectors can either read (Sink) or write (Source) into Apache Kafka and they do it to topics.

.. code-block:: YAML

  ---
    context: "context"
    source: "source"
    projects:
      - name: "foo"
        connectors:
          - principal: "User:Connect1"
            connectors:
              - "jdbc-sync"
              - "ibmmq-source"
            topics:
              read:
                - "topicA"
                - "topicB"
          - principal: "User:Connect1"
            group: "group"
            status_topic: "status"
            offset_topic: "offset"
            configs_topic: "configs"
            topics:
              write:
                - "topicA"
                - "topicB"

If you are having more than one Kafka Connect cluster you can specify a custom group, status, offset and config topics.

When using RBAC, you can add under each principal the connectors it can use and this principals will only have visibility over them.

Schema Registry
^^^^^^^^^^^

Under the platform section users can define the permissions required for handling Schema Registry clusters, optionally you can configure the
topic name and group used for the communication.

.. code-block:: YAML

  ---
    context: "context"
    platform:
        schema_registry:
          instances:
            - principal: "User:SchemaRegistry01"
              topic: "foo"
              group: "bar"
            - principal: "User:SchemaRegistry02"
              topic: "zet"
          rbac:
            Operator:
              - principal: "User:Hans"
              - principal: "User:Bob"

If you are using rbac, under the specific section users can attach their own cluster wide role principles.


Topic naming convention
^^^^^^^^^^^^^^^^^^^^^^^

Topic names will be chosen according to the scheme:

.. code-block:: YAML

  [context].[source].[project-name].[topic-name]


It is possible to give a finer structure to the topic names by specifying additional fields between
the `company` and `projects` fields. Optionally, a `dataType` can be specified, which will be suffixed to the topic name.
For example:

.. code-block:: YAML

  context: "context"
  company: "company"
  env: "env"
  source: "source"
  projects:
    - name: "projectA"
      topics:
        - name: "foo"
        - name: "bar"
          dataType: "avro"


will lead to topic names

.. code-block:: YAML

  context.company.env.source.projectA.foo
  context.company.env.source.projectA.bar.avro


What ACLs are created
^^^^^^^^^^^^^^^^^^^^^
Kafka Topology Builder will assign the following ACLs:

* each principal in the `consumers` list will get `READ` and `DESCRIBE` permissions on each topic in the containing project as well as `READ` access on every consumer group
* each principal in the `producers` list will get `WRITE` and `DESCRIBE` permissions on each topic in the containing project
* each principal in the `streams` list will get

  * `READ` access on every topic in its `read` sub-object
  * `WRITE` access on every topic `write` sub-object
  * `ALL` access on every topic starting with fully-qualified project name, e.g. ``context.company.env.source.projectA`` in the example above. These are `PREFIXED` ACLs.

* each principal for a connector will get

  * read and write access on the corresponding `status_topic`, `offset_topic`, and `config_topics` (`LITERAL` ACLs)

    * these fields default to `connect-status`, `connect-status`, and `connect-configs`. Hence access to these topics will be granted to the Connect principal if the fields are not explicitly given.
  * `CREATE` access on the cluster resource
  * `READ` access on every topic in the corresponding `topics.read` subobject
  * `WRITE` access on every topic in the corresponding `topics.write` subobject
  * `READ` access on the group specified in the corresponding `group` field
    * if no `group` is specified, rights to `connect-cluster` will be granted

* the principal for a `schema_registy` platform component will be given `DESCRIBE_CONFIGS`, `READ`, and `WRITE` access to each topic.

* the principal for a `control_center` platform component will be given:

  * `DESCRIBE` and `DESCRIBE_CONFIGS` on the cluster resource
  * `READ` on every consumer group starting with the corresponding `appId` (`PREFIXED` ACLs)
  * `CREATE`, `DESCRIBE`, `READ`, and `WRITE` access on each topic starting with the corresponding `appId` (`PREFIXED`)
  * `CREATE`, `DESCRIBE`, `READ`, and `WRITE` access on the `_confluent-metrics`, `_confluent-command`, and `_confluent-monitoring` topics

Which ACLs does the user running Kafka Topology Builder need?
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The principal which the Kafka Topology Builder uses to authenticate towards the Kafka cluster should have the following rights:

* `ALTER` on the cluster resource to create and delete ACLs
* `DESCRIBE` on the cluster resource
* the following operations be allowed for topic resources prefixed with the current context:

  * `ALTER_CONFIGS`, `CREATE`, and `DESCRIBE`
  * `ALTER` when changing the number of partitions should be allowed
  * `DELETE` when topic deletion should be allowed


See https://docs.confluent.io/current/kafka/authorization.html for an overview of ACLs. When setting up the topology builder for a specific context,
prefixed ACLs can be used for all topic-level operations.

When using Confluent Cloud, a *service account* with the proper rights to run the topology builder for the context `samplecontext` could be generated as follows using the Confluent Cloud CLI `ccloud`:

.. code-block:: bash

  ccloud service-account create sa-for-ktb --description 'A service account for the Kafka Topology Builder'
  # note the Id for the service account, we will use 123456 below

  ccloud kafka acl create --allow --service-account 123456 --cluster-scope --operation ALTER
  ccloud kafka acl create --allow --service-account 123456 --cluster-scope --operation DESCRIBE
  ccloud kafka acl create --allow --service-account 123456 --topic samplecontext --prefix --operation ALTER_CONFIGS
  ccloud kafka acl create --allow --service-account 123456 --topic samplecontext --prefix --operation CREATE
  ccloud kafka acl create --allow --service-account 123456 --topic samplecontext --prefix --operation DESCRIBE
  ccloud kafka acl create --allow --service-account 123456 --topic samplecontext --prefix --operation ALTER
  ccloud kafka acl create --allow --service-account 123456 --topic samplecontext --prefix --operation DELETE









RBAC
-----------

Cluster wide roles
^^^^^^^^^^^

In the RBAC module users can add cluster wide roles to principals. This roles can be attached to each one of the clusters available in the confluent platform.

This functionality will, as of the time of writing this documentation, work for Kafka, Kafka Connect and Schema Registry clusters.
It might be extended in the future for other clusters in the platform.

.. code-block:: YAML

  ---
    context: "context"
    source: "source"
    platform:
        kafka:
          rbac:
            SecurityAdmin:
              - principal: "User:Foo"
            ClusterAdmin:
              - principal: "User:Boo"
        kafka_connect:
          rbac:
            SecurityAdmin:
              - principal: "User:Foo"
        schema_registry:
          instances:
            - principal: "User:SchemaRegistry01"
              topic: "foo"
              group: "bar"
            - principal: "User:SchemaRegistry02"
              topic: "zet"
          rbac:
            Operator:
              - principal: "User:Hans"
              - principal: "User:Bob"


In the previous example the reader can see how to add cluster wide roles into each of the available clusters, all roles go under the rbac label.
