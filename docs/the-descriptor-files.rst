Understanding the descriptor files
*******************************

As a descriptive tool the KTB uses a file (or set of files) to record how the cluster should look like, this files are call descriptors.

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

The separator and order of attribute can be personalised using the KTB configuration file.
The relevant properties are:

- **Property**: *topology.topic.prefix.format*, to set the full topic naming format.
- **Property**: *topology.project.prefix.format*, to set the project level name format, it should be a subset of the previous one.
- **Property**: *topology.topic.prefix.separator*, to select a custom separator between attributes.

more details can be found in the :doc:`config-values` section where the most important configuration details are explained.

Manage only topics, the optional files
-----------

Not all the attributes are mandatory in the descriptor file, it is currently possible to:

* Have a file with only topics, so no acls are defined using the abstractions provided by the consumers, producers, streams, etc attributes.
* Build a topology with partial acls, if you are not using any stream application, there is no need to define it, same for other access control properties.
* When defining a topic it is possible to use:
  * *dataType* when as a user it is aimed to specify the data type of the topic.
  * *schemas* if the reader is interested to register schemas for the topic.

