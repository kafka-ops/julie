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