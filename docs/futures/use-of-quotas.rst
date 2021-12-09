Use of Quotas
*******************************

Define the use of quotas kafka config in JulieOps
Kafka quotas design can be read in the official doc in the next link: https://kafka.apache.org/documentation/#design_quotas

The quotas define a user consumption/producer byte limits or con be configured a request_rate network usage.
With this point can be modified the quotas usage with a GitOps culture.

Defining the quota usage
-----------

As an operator of Julie Ops you can define a set of custom plans, this would be a file that look like this:

.. code-block:: YAML

      kafka:
        quotas:
          - principal: "User:App0"
            producer_byte_rate: 1024
            consumer_byte_rate: 1024
            request_percentage: 50.0
          - principal: "User:App1"
            producer_byte_rate: 2048
            consumer_byte_rate: 2048
          - principal: "User:App2"
            request_percentage: 80.0


Using quotas in the Topology
-----------

The quotas usage are defined to be relate with a user consumer or producer

.. code-block:: YAML

  context: "contextOrg"
  source: "source"
  projects:
    - name: "foo"
      topics:
        - name: "foo"
          config:
            replication.factor: "1"
            num.partitions: "1"
          consumers:
            - principal: "User:App0"
            - principal: "User:App1"
          producers:
            - principal: "User:App0"
        - name: "fooBar"
          config:
            replication.factor: "1"
        - name: "barFoo"
          config:
            replication.factor: "1"
        - name: "barFooBar"
          config:
            replication.factor: "1"
  platform:
     kafka:
       quotas:
         - principal: "User:App0"
           producer_byte_rate: 1024
           consumer_byte_rate: 1024
           request_percentage: 50.0
         - principal: "User:App1"
           producer_byte_rate: 2048
           consumer_byte_rate: 2048
         - principal: "User:App2"
           request_percentage: 80.0

