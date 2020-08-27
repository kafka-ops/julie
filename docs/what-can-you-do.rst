What can you do with Kafka Topology Builder
*******************************

In a nutshell with Kafka Topology Builder you can manage your topics and acls in an structure and autonomous way.
As well you can manage your topics schemas as register.

In this chapter we will introduce the different things one can configure in each topology file(s).

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
    team: "team"
    source: "source"
    projects:
      - name: "foo"
        consumers:
          - principal: "User:App0"

Consumer definition with principal "User:App0" and without an specific consumer group, for this configuration an ACL will be created to accept any consumer group.

.. code-block:: YAML

  ---
    team: "team"
    source: "source"
    projects:
      - name: "foo"
        consumers:
          - principal: "User:App0"
            group: "foo

Consumer definition with principal "User:App0" and consumer group name "foo".
