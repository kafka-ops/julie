Managing Principals
*******************************

One of the common actions required when managing a cluster is the operation of principals (aka Users in kafka).
The KTB can help you manage this principals by parsing the involved topologies.

*NOTE* This feature is currently experimental, please use it with care and let us know anything that is missing.

How does it work
-----------

As this feature is considered experimental for now, the reader should first enable it, this is done by adding the _topology.features.experimental_
property in the configuration.


.. code-block:: bash

  topology.features.experimental=true (by default all experimental features are disabled)

Once enabled, this feature will run in between the TopicManager and the BindingsManager, so right before the Bindings are created all required
principals are present in the cluster.

This process is done by parsing the topology description and extracting the principals, once done the tool will calculate which principals are
required to be create and which ones are no longer necessary and can be disabled. For sure always if the related delete option is enabled.

If desired by organisational purposes a user can decide to filter witch Service Account principals can be managed, this is done using the
*topology.service.accounts.managed.prefixes* configuration setting. Check :ref:`config` for details.

Principals Manager Providers
-----------

As a user of KTB you can select which providers to use, they could be for example Confluent Cloud or SASL/SCRAM (once `#2 <https://github.com/kafka-ops/kafka-topology-builder/issues/2>`_ is implemented),
the tool will contact the server and manage the user creation.

In the current version you can only use the Confluent Cloud provider.

Confluent Cloud Provider
-----------

If the reader is using the Confluent Cloud, you can manage the principals described in the Topology.
This operations are dependant on the *ccloud* CLI, as a user you should have this tool available in your system.

Another requirement is you should be logged in more details `here <https://docs.confluent.io/ccloud-cli/current/command-reference/ccloud_login.html>`_.
Once this is done, the KTB will rely on CLI to manage the principals.

Enabling principal translation
-----------

As a user of KTB with Confluent Cloud, you can chose to use the Service Account ID or the Service Account Name as a label describing your user.
If you want the users to be managed by Kafka Topology Builder you have to use the principals by your Service Account name, the Topology
will look like this:

.. code-block:: YAML

  ---
    context: "context"
    source: "source"
    projects:
      - name: "foo"
        consumers:
          - principal: "User:ApplicationName"

internally this would be registered in Confluent Cloud and use internally the Service Account ID for managing all required ACLs.

As this is an experimental feature you will be required to enable it.

.. code-block:: bash

  topology.translation.principal.enabled=true (by default all experimental features are disabled)