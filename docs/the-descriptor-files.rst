Understanding the descriptor files
*******************************

As a descriptive tool Julie Ops uses a file (or set of files) to record how the cluster should look like, this files are call descriptors.

Because users might have complex deployments, the structure is flexible enough to accommodate them, we will cover them in this section.

The File format
-----------

Currently the tool supports reading YAML and JSON files. **Note** that all topologies will need to be using the same file format, a mixture is not supported.

In this guide we will use yaml as the core format, for JSON examples please refer to the examples directory.

The file type is configured using the *topology.file.type* configuration variable.

```
topology.file.type=JSON
```

Getting started
-----------

The KTB files use a common structure for describing the cluster entities, this attributes are setup around:

* **context** : It is commonly used to describe where a collection of entities are coming from. This value could be for example a team name, a line of business or simply the origin of the topics (the data center).
This attribute is used as a primary key to group all the entities.

* **project**: In each *context* there could be one or more projects, this attribute is the next level of personalisation as it is usually used to group all final entities such as permissions (acls/rbac), topics and schemas. Each Topology can have multiple of them.

In between the *context* and the *project* attribute the user can define a free list of attributes in form of a key and value.
This list of attributes is going to be listed, in order, by default during the topic name composition in between the context and the project attribute.

A descriptor header like:

.. code-block:: YAML

  ---
    context: "context"
    source: "source"
    projects:
      - name: "foo"
        topics:
          - name: "foo"
            config:
              replication.factor: "1"
              num.partitions: "1"
          - dataType: "avro"
            name: "bar"
            config:
              replication.factor: "1"
              num.partitions: "1"

will by default create topic names with the prefix *context.source.foo*, in detail this topology will create two topics.

* *context.source.foo.foo* with one partition and a replication factor of one.
* *context.source.foo.bar.avro* with a single partition and a single replica.

The separator and order of attribute can be personalised using Julie Ops configuration file.
The relevant properties are:

- **Property**: *topology.topic.prefix.format*, to set the full topic naming format.
- **Property**: *topology.project.prefix.format*, to set the project level name format, it should be a subset of the previous one.
- **Property**: *topology.topic.prefix.separator*, to select a custom separator between attributes.

more details can be found in the :doc:`config-values` section where the most important configuration details are explained.

What can you do in the descriptor files?
-----------

The `example` directory in the project contains a set of example topologies that you can use to get an idea of how to
use the different features. For example:
* RBAC roles with Confluent Platform
* Administration of schemas for the topics
* Using plans to create common topic configurations
* Add metadata to different elements in the topology

Add metadata to your topology
-----------

In addition to definitions needed for creating the required topics, acls, rbac bindings etc in your Kafka cluster KTB
also supports annotating your topology through metadata. This can allow you to make the descriptor files easier to read,
e.g. for someone not into the details of what a given topic is all about.

But you can also utilise it to do the following:
* Add validations that also use metadata. E.g. combine topic metadata and topic name to enforce more refined topic naming rules.
* Generate documentation from the your descriptor files (with some tool) and include metadata create better documentation on topics and users.

You can add metadata to the following elements in the topology:
* topics
* consumers
* producers
* streams
* connectors

A short example is given below, have a look in the `example` directory for a more complete example.

.. code-block:: YAML

    ---
    context: "context"
    projects:
      - name: "foo with metadata"
        consumers:
          - principal: "User:App0"
            metadata:
              system: "System0"
        producers:
          - principal: "User:App1"
            metadata:
              system: "System1"
        topics:
          - name: "topicA"
            metadata:
              domain: "Sales"
              owner: "DepartmentA"
            config:
              replication.factor: "3"
              num.partitions: "3"

Manage only topics, the optional files
-----------

Not all the attributes are mandatory in the descriptor file, it is currently possible to:

* Have a file with only topics, so no acls are defined using the abstractions provided by the consumers, producers, streams, etc attributes.
* Build a topology with partial acls, if you are not using any stream application, there is no need to define it, same for other access control properties.
* When defining a topic it is possible to use:
  * *dataType* when as a user it is aimed to specify the data type of the topic.
  * *schemas* if the reader is interested to register schemas for the topic.

