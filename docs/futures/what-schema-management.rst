Managing Schemas
*******************************

Because not only from Topics and Access live the Kafka team, with the Kafka Topology Builder you can manage as well your schemas.
This functionality can be very useful for administering your cross environment, but as well to register specific schemas per topic.

An example topology for managing schemas would look like this (*only the topics section*).

.. code-block:: YAML

  ---
    context: "context"
    projects:
      - name: "projectA"
        topics:
          - name: "foo"
            schemas:
              value.schema.file: "schemas/foo-value.avsc"
            config:
              replication.factor: "1"
              num.partitions: "1"
          - name: "bar"
            dataType: "avro"
            subject.name.strategy: "TopicRecordNameStrategy"
            schemas:
              - key.schema.file: "schemas/foo-key.avsc"
                value.schema.file: "schemas/foo-value.avsc"
                value.record.type: "foo"
              - key.schema.file: "schemas/bar-key.avsc"
                value.schema.file: "schemas/bar-value.avsc"
                value.record.type: "bar"
              - key.schema.file: "schemas/zet-key.avsc"
                value.schema.file: "schemas/zet-value.avsc"
                value.record.type: "zet"
            config:
              replication.factor: "1"
              num.partitions: "1"

If using an example like this, for the topic _bar_ there is going to be an schema registered for key and value.
Currently the schemas are managed as files and need to be accessible for the KTB tool, for example inside the git repository.

A topology with absolute schema path would look like this:

.. code-block:: YAML

  ---
    context: "context"
    projects:
      - name: "projectA"
        topics:
          - name: "foo"
            schemas:
              value.schema.file: "/kafka/schemas/foo-value.avsc"
            config:
              replication.factor: "1"
              num.partitions: "1"

*NOTE*: The path for the files is relative to the location of the of the topology.

*NOTE*: Keep in mind that for a use case like in the example above you have to define the value for `schema.registry.url` in your properties file.

Where should you put your schema files
-----------

We recommend keeping your schemas relative to the location of your topologies files as it is easier to map all things bundler near by than in
distant locations.

However, if preferred for organisational purposes the reader can specify any absolute path in the topology and the tool will prefer it as an schema location.

*NOTE*: There is no way as of now to set a preferred location by configuration, so the user should write an absolute path where necessary in the topology file.

Multiple schemas support per topic
-----------

It is possible to setup multiple schemas per topic, this is a common use case when using a subject name strategy different than *TopicNameStrategy*.

If using multiple schemas, as it can be seen in the earlier example, the user must set the subject naming strategy for the topic to be one of the
other options.

The user should as well set the *record.type* for each of the keys as this is a value required when configuring the final subject.

Support multiple formats
-----------

It is currently possible to support schema files with all formats currently available in the Confluent Schema Registry.
As a user, you can set the format for each of the components in the schema section like this:

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
          - name: "bar"
            dataType: "avro"
            schemas:
              key.schema.file: "schemas/bar-key.avsc"
              key.format: "AVRO"
              value.schema.file: "schemas/bar-value.json"
              value.format: "JSON"
            config:
              replication.factor: "1"
              num.partitions: "1"


if the **format** keyword is not specified, the default value is *AVRO*.

Set the Schema Compatibility
-----------

In the Schema management section is possible to set the schema compatibility level like this:


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
          - name: "bar"
            dataType: "avro"
            schemas:
              value.schema.file: "schemas/bar-value.avsc"
              value.compatibility: "BACKWARD"
            config:
              replication.factor: "1"
              num.partitions: "1"

**NOTE**: The compatibility level will be set before summit the registered schema file, this is done like this to easy transitions and migrations.

Supported Schema Registry
-----------

There are multiple options when using an Schema Registry with Kafka, currently only the Confluent Schema Registry is supported.