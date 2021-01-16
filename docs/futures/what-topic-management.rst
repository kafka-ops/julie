Managing Topics and their Configuration
*******************************

The first and foremost important thing we aim to manage is topics, setup the partitions count and replication factor and as well configure their specific characteristics.

If desired by organisational purposes a user can decide to filter which topics can be managed, this is done using the
*topology.topics.managed.prefixes* configuration setting. Check :ref:`config` for details.

Topic naming convention
-----------

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


Partitions Count and Replication Factor
-----------

In following configuration the reader is seeing an example of how to create a topic with *replication.factor* and *num.partitions*, in that case all one.

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

Topic Level Consumers and Producers
-----------

It is possible to setup dedicated access control rules for specific topics instead of project scope.
An example configuration would look like this:

.. code-block:: YAML

  ---
    context: "context"
    projects:
      - name: "projectA"
        topics:
          - name: "foo"
            consumers:
              - principal: "User:App0"
            producers:
              - principal: "User:App1"
            config:
              replication.factor: "1"
              num.partitions: "1"

This type of Access Control rules allow the reader to setup dedicated access to single topics, without giving global project access.

Handling Configuration
-----------

For each topic, under the configuration attribute, it is possible to define the map of custom broker side configurations for the topic.

KTB is going to take care to apply the necessary changes and remove the ones that are not necessary anymore.