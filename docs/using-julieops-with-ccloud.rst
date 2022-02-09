Using JulieOps with Confluent Cloud
*******************************

Is it possible to use JulieOps with Confluent Cloud without any major inconvenience.
While in previous versions Julie took benefit of the core Kafka AdminClient, since the version latest version it leverages
the new and powerful Confluent Cloud API(s).

To know more about the API(s) you can read the reference documentation `here <https://docs.confluent.io/cloud/current/api.html#section/Introduction>`_.

What do you need ??
-----------

You might be wondering, what do you need? this is the minimal set of configuration you need to have in place.

.. code-block:: JAVA

    security.protocol=SASL_SSL
    sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule   required username="<CLUSTER_API_KEY>"   password="<CLUSTER_API_SECRET>";
    ssl.endpoint.identification.algorithm=https
    sasl.mechanism=PLAIN
    # Confluent Cloud Schema Registry
    schema.registry.url=<SCHEMA_REGISTRY_SERVER_URL>
    basic.auth.credentials.source=USER_INFO
    schema.registry.basic.auth.user.info=<SCHEMA_REGISTRY_API_KEY>:<SCHEMA_REGISTRY_API_SECRET>
    ccloud.environment=env-j9wgp
    ccloud.cluster.api.key=<CLUSTER_API_KEY>
    ccloud.cluster.api.secret=<CLUSTER_API_SECRET>
    ccloud.cloud.api.key=<CLOUD_API_KEY>
    ccloud.cloud.api.secret=<CLOUD_API_SECRET>
    topology.builder.ccloud.kafka.cluster.id=lkc-jkz1m
    ccloud.cluster.url=<CLUSTER_REST_URL>
    topology.builder.access.control.class = com.purbon.kafka.topology.roles.CCloudAclsProvider

While the first set are common configurations for any java application using Confluent Cloud there are some that are particular for JulieOps.
This are the specific configurations.

The <CLOUD_API_KEY> can be created using a CLI like this 

.. code-block:: JAVA

    confluent api-key create --resource cloud --description <key description>


.. code-block:: JAVA

    ccloud.environment=env-j9wgp
    ccloud.cluster.api.key=<CLUSTER_API_KEY>
    ccloud.cluster.api.secret=<CLUSTER_API_SECRET>
    ccloud.cloud.api.key=<CLOUD_API_KEY>
    ccloud.cloud.api.secret=<CLOUD_API_SECRET>
    topology.builder.ccloud.kafka.cluster.id=lkc-jkz1m
    ccloud.cluster.url=<CLUSTER_REST_URL>
    topology.builder.access.control.class = com.purbon.kafka.topology.roles.CCloudAclsProvider


In the previous block the reader can notice the required configs in order to make JulieOps work with the cloud.

* ccloud.environment -> The environment ID your cluster is in
* ccloud.cluster.api.key and ccloud.cluster.api.secret -> A pair of keys to operate in your Confluent Cloud Kafka cluster
* ccloud.cloud.api.key and ccloud.cloud.api.secret -> A pair of keys for using the Confluent Cloud Control Plane, required for some operations
* topology.builder.ccloud.kafka.cluster.id -> Your target cluster ID
* ccloud.cluster.url -> The REST endpoint URL for your target cluster
* topology.builder.access.control.class -> The access control manager dedicated for Confluent Cloud (default would be AdminClient, be aware)

You have working examples in the examples directory.

Create Service Account and ACL for Confluent Cloud
-----------

You can create a service-account using the CLI below.

.. code-block:: JAVA

    confluent iam service-account create User:<service-account-name> 

    Example : 
    confluent iam service-account create User:julieops-svc-acct --description "SA for JulieOps"
    +-------------+------------------------+
    | ID          | sa-6k1833              |
    | Name        | User:julieops-svc-acct |
    | Description | SA for JulieOps        |
    +-------------+------------------------+



Your topology file will look like this.

.. code-block:: yaml

    ---
    context: "io.confluent"
    projects:
    - name: "devops"
        topics:
        - name: "final"
            metadata:
            description: "this is a demo topic with write access"
            owner: "Audience"
            config:
            replication.factor: "3"
            num.partitions: "1"
            producers:
            - principal: "User:julieops-svc-acct"
            consumers:
            - principal: "User:julieops-svc-acct"



To confirm if ACL were applied, run the following command.

.. code-block:: JAVA

    confluent kafka acl list --service-account <service-account-id>
    Example : 
    confluent kafka acl list --service-account sa-6k1833 


Do I need anything else?
-----------

No, if you use this config in your properties file JulieOps will interact with Confluent Cloud as it does with other on-prem clusters.