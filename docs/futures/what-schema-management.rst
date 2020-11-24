Managing Schemas
*******************************

Because not only from Topics and Access live the Kafka team, with the Kafka Topology Builder you can manage as well your schemas.
This functionality can be very useful for administering your cross environment, but as well to register specific schemas per topic.

An example topology for managing schemas would look like this (*only the topics section*).

.. code-block:: YAML

  ---
    context: "context"
    company: "company"
    env: "env"
    source: "source"
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
              value.schema.file: "schemas/bar-value.avsc"
            config:
              replication.factor: "1"
              num.partitions: "1"

If using an example like this, for the topic _bar_ there is going to be an schema registered for key and value.
Currently the schemas are managed as files and need to be accessible for the KTB tool, for example inside the git repository.

*NOTE*: The path for the files is relative to the location of the of the topology.

*NOTE*: Keep in mind that for a usecase like in the example above you have to define the value for `schema.registry.url` in your properties file.


Supported Schema Registry
-----------

There are multiple options when using an Schema Registry with Kafka, currently only the Confluent Schema Registry is supported.
