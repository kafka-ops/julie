Out of the box topic management
*******************************

From time to time a team would be using a tool that might not follow the normative naming convention, but still be interested in managing this topics with JulieOps.
For this purpose we introduce the out of the box topic management.

One example of this topics would be the *__debezium_heartbeat* when using Debezium CDC.

If you want to use special, or out of box topics, you would need to.

Your Topology example
-----------

.. code-block:: YAML
    ---
    context: "oltp"
    projects:
        - name: "foo"
          consumers:
             - principal: "User:NewApp2"
          topics:
            - name: "foo"
            - dataType: "avro"
              name: "bar"
            - dataType: "json"
              name: "zet"
    special_topics:
        - name: "foo"
          config:
            replication.factor: "1"
            num.partitions: "1"
        - name: "bar"
          dataType: "avro"
          schemas:
            value.schema.file: "schemas/bar-value.avsc"

As you can deduct from the previous topology example, the special topics are nothing new.
Within the *special_topics* list you can request a list of topics with all the properties and features as the ones within the projects, however there are some differences.

Some differences to be aware of
-----------

The special topics would not play the same role as the ones created in a project context.
This means that all consumers, producers, kstreams, etc defined within the project would not create ACL(s) for this topics.

If you need to create acls, it is only possible right now to use dedicated *consumers* and *producers* items in same fashion as you can do for a project topic.
If you configure this properties you would allow principals to read, or, write to the special topic.