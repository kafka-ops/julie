Controlling the access
*******************************

ACLs
-----------

In the topology descriptor files users can create permissions for different types of applications, Consumers, Producers, Kafka streams apps or Kafka Connectors.
With this roles, users can easily create the permissions that map directly to their needs.

If desired by organisational purposes e.g. shared cluster a user can decide to filter which ACLS will be managed by prefix, this is done using the managed prefixes config.

Topic acls will be managed if the topic matches
*topology.topic.managed.prefixes* configuration setting. Check :ref:`config` for details.

Group acls will be managed if the group matches
*topology.group.managed.prefixes* configuration setting. Check :ref:`config` for details.

All others currently and for the above if a global wildcard, will be managed if principle matches
*topology.service.accounts.managed.prefixes* configuration setting. Check :ref:`config` for details.


Consumers
^^^^^^^^^^^

As a user you can configure consumers for each project.
Consumer have a principal and optionally a consumer group name. The consumer group ACL is by default defined for all groups ("*").
Users can customize this ACL by defining a *group* attribute for each consumer.


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

In the default mode the KTB will create dedicated ACL for each user and topic pair. For organisations that aim not to have dedicated pair of rules the KTB offer the option
to optimise the number of ACLs using prefixed rules.

The optimised ACLs/RBAC can be enabled using the *topology.acls.optimized* configuration property.

Producers
^^^^^^^^^^^
As a user of KTB you can configure the required set of producers for your application.

Producers have a principal.

.. code-block:: YAML

  ---
    context: "context"
    source: "source"
    projects:
      - name: "foo"
        producers:
          - principal: "User:App0"

In the default mode the KTB will create dedicated ACL for each user and topic pair. For organisations that aim not to have dedicated pair of rules the KTB offer the option
to optimise the number of ACLs using prefixed rules.

The optimised ACLs/RBAC can be enabled using the *topology.acls.optimized* configuration property.

Streams
^^^^^^^^^^^

Users can also setup Kafka Streams applications.
Each one of them will be composed of a principal and a list of topics that this principal needs to read and write.
The principal is the user used by the streams app to connect to Kafka. You can also optionally specify the *applicationId*.

.. code-block:: YAML

  ---
    context: "context"
    source: "source"
    projects:
      - name: "foo"
        streams:
          - principal: "User:App0"
            topics:
              read:
                - "topicA"
              write:
                - "topicB"

KTB will create the necessary ACLs for reading and writing topics, as well as ACLs needed by the app to create/manage internal topics.
The ACLs for the consumer group and for internal topic creation are prefixed.
The resource name (prefix) is by default the topic name prefix in the project.
For the example above the prefix will by default be "context.source.foo".

As you see in the next example this can be overridden by specifying an *applicationId* in the topology.

.. code-block:: YAML

  ---
    context: "context"
    source: "source"
    projects:
      - name: "foo"
        streams:
          - principal: "User:App0"
            applicationId: "streamsApplicationId"
            topics:
              read:
                - "topicA"
              write:
                - "topicB"


When the *applicationId* is specified this is used as the resource prefix in the ACLs for consumer groups
and internal topics for the streams app. In the above example the prefix will be "streamsApplicationId".

Connectors
^^^^^^^^^^^

In a similar fashion as with the previous roles, users can setup specific Kafka Connect setups.
Each one of them will be composed of a principal, this would be the user used by the connector to
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

What ACLs are created
^^^^^^^^^^^^^^^^^^^^^
Kafka Topology Builder will assign the following ACLs:

* each principal in the `consumers` list will get `READ` and `DESCRIBE` permissions on each topic. In addition `READ` access on every consumer group (by default) or the group specified in the topology.

* each principal in the `producers` list will get `WRITE` and `DESCRIBE` permissions on each topic. In addition if a *transactionId* is specified a WRITE and DESCRIBE ACL is created on the transactionId resource. And if either *transactionId* or *idempotence* is specified for the producer the IDEMPOTENT_WRITE ALLOW acl is created.

* each principal in the `streams` list will get

  * `READ` access on every topic in its `read` sub-object
  * `WRITE` access on every topic `write` sub-object
  * `ALL` access on every topic starting with the fully-qualified project name (by default) or the given applicationId. These are `PREFIXED` ACLs.
  * `READ` access on consumer groups starting with the fully-qualified project name (by default) or the given applicationId. These are `PREFIXED` ACLs.

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

Having multiple Kafka Connect clusters
^^^^^^^^^^^

A more than common scenario in many organisations is to have multiple Kafka Connect clusters.
The Kafka Topology Builder will allow you to configure and manage them using a single Topology, using a descriptor yaml like this one:

.. code-block:: YAML

  ---
    context: "context"
    projects:
      - name: "projectA"
        consumers:
          - principal: "User:App0"
          - principal: "User:App1"
        producers:
          - principal: "User:App3"
          - principal: "User:App4"
        connectors:
          - principal: "User:Connect1"
            group: "group"
            status_topic: "status"
            offset_topic: "offset"
            configs_topic: "configs"
            topics:
              read:
                - "topicA"
                - "topicB"

The reader can see with the previous YAML code block that *User:Connect1* will be authorized for a custom set of group, status, offset and configs topics.
This future is very flexible as single topology files can be used to describe permission for multiple Connect clusters.

Access for specific Connectors
^^^^^^^^^^^

It is possible in RBAC to assign permission for a given principal to access a given set of Connectors.
This is possible with the Kafka Topology Builder with a topology like the one below, where *User:Connect1* will have access to connectors *jdbc-sync* and *jdbc-source*.

.. code-block:: YAML

  ---
    context: "context"
    source: "source"
    projects:
      - name: "foo"
        consumers:
          - principal: "User:App0"
          - principal: "User:App1"
        connectors:
          - principal: "User:Connect1"
            connectors:
              - "jdbc-sync"
              - "ibmmq-source"
            topics:
              read:
                - "topicA"
                - "topicB"
          - principal: "User:Connect2"
            topics:
              write:
                - "topicC"
                - "topicD"

Access for specific Schemas
^^^^^^^^^^^

It is possible in RBAC to assign permission for a given principal to access a given set of Schemas.
This is possible with the Kafka Topology Builder with a topology like the one below, where *User:App0* will
have access to schemas in subjects *transactions* and *User:App1* to subject *contracts*.

.. code-block:: YAML

  ---
    context: "context"
    source: "source"
    projects:
      - name: "foo"
        consumers:
          - principal: "User:App0"
          - principal: "User:App1"
        streams:
          - principal: "User:App0"
            topics:
              read:
                - "topicA"
                - "topicB"
              write:
                - "topicC"
                - "topicD"
        schemas:
          - principal: "User:App0"
            subjects:
              - "transactions"
          - principal: "User:App1"
            subjects:
              - "contracts"


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

*NOTE*: The syntax support having multiple schema registry instance where the reader can configure specific *schema topics* and *groups*.
This capability allows a high degree of personalisation for the permissions being generated.
