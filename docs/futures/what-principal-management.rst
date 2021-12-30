Managing Principals
*******************************

One of the common actions required when managing a cluster is the operation of principals (aka Users in kafka).
Julie Ops can help you manage this principals by parsing the involved topologies.

How does it work
-----------

This feature is not enabled by default, if the reader is interested in managing as well principals it would be required to use the config property
_julie.enable.principal.management_.

.. code-block:: bash

  julie.enable.principal.management=true (disabled by default)

Once enabled, this feature will run in between the TopicManager and the BindingsManager, so right before the Bindings are created all required
principals are present in the cluster.

This process is done by parsing the topology description and extracting the principals, once done the tool will calculate which principals are
required to be create and which ones are no longer necessary and can be disabled. For sure always if the related delete option is enabled.

If desired by organisational purposes a user can decide to filter witch Service Account principals can be managed, this is done using the
*topology.service.accounts.managed.prefixes* configuration setting. Check :ref:`config` for details.

Principals Manager Providers
-----------

As a user of Julie Ops you can select which providers to use, they could be for example Confluent Cloud or SASL/SCRAM (once `#2 <https://github.com/kafka-ops/kafka-topology-builder/issues/2>`_ is implemented),
the tool will contact the server and manage the user creation.

In the current version you can only use the Confluent Cloud provider.

Confluent Cloud Provider
-----------

If the reader is using the Confluent Cloud, you can manage the principals described in the Topology.
This operation is taking advantage of the new *Confluent Cloud API*, the reader should have this properly configured in JulieOps as well as in Confluent Cloud.