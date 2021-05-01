Managing Connectors
*******************************

As a Kafka deployment is growing it becomes natural to start ingress or egress of data from the cluster in a structured way.
This is usually done using one, or multiple, Kafka Connect clusters.
So as natural, as a user, you aim to handle your connectors as well from within your gitops pipeline, and you can do now this with
JulieOps since version 3.0

**IMPORTANT**: This functionality is available only since version 3.0 of JulieOps.

How does it work
-----------

Dealing with connectors works as with other artefacts in your deployment, you need to first define them in your yaml, for example like:

.. code-block:: YAML

  context: "context"
  company: "company"
  env: "env"
  source: "source"
  projects:
    - name: "projectA"
      connectors:
        artefacts:
            - path: "connectors/source-jdbc.json"
              server: "connector0"
              name: "source-jdbc"
            - path: "connectors/sink-jdbc.json"
              server: "connector0"
              name: "sink-jdbc"
        access_control:
            - principal: "User:Connect1"
      topics:
        - name: "foo"
        - name: "bar"
          dataType: "avro"

If you have been using JulieOps, or previously Kafka Topology Builder, you will notice a change in the YAML format.
Since the release of this feature with 3.0, you can define in the connector section both, artefacts and access control principals.

**NOTE**: This change is implemented in backwards compatible mode, in case no header keywords is used (artefact or access_control)
the old behaviour would be expected, only principals :smile:

As with Schemas, you would need to have in your filesystem a directory with your connectors, in the example it would be the directory
connectors, relative to the location of the topology file.

**NOTE**: Remember, files can be either relative to the topology file or absolute.

Another important fields when defining a connector will be:

* *server*: This would define the target server alias, as defined in the configuration, where the connector will be created.
* *name*: The connector name as used in the Kafka Connect REST api.

Configuring Kafka Connect servers
-----------

In this configuration, you can define as many servers to be used during the Kafka Connect artefact creation, this could be done like this:

.. code-block:: bash

    platform.servers.connect = ["connector0:10.0.0.1", "connector1:10.0.1.1"]

**NOTE**: if the configuration contains an alias not configured here, an exception will be raised, errors will be raised as well in case of this
config being empty and willing to manage connectors.