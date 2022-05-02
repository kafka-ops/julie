
v4.2.5 / 2022-05-02
===================

  * Fix the acl creation in the hybrid ccloud provider (#494)

v4.2.4 / 2022-04-28
===================

  * [neat] make hybrid ccloud provider list acls use admin client

v4.2.3 / 2022-04-27
===================

  * [bug] fix a stupid missed thing when loading the hybrid provider, this code really needs to be done cleaner

v4.2.2 / 2022-04-27
===================

  * [Feature] Introduce the concept of an hybrid ccloud provider, to set acls via admin client and translation via api (#492)

v4.2.1 / 2022-04-26
===================

  * [Feature] Add feature flag to make the remote state verification backwards compatible again (#491)
  * [Feature] Allow setting log level as debug in the code (#490)
  * [Feature] allow insecure https connection when using mds, default to false
  * [Neat] add a method to simple sanitize a string that could contain empty values
  * [Feature] add a pre-flight check for valid clusterIds in your platform

v4.2.0 / 2022-04-13
===================

  * [Big] Fix Confluent Cloud Translation mechanism when the Service Account does not have a type prefix (default user) (#485)
  * [Feature] Introduce the concept of an AuditLog for JulieOps (#484)
  * [Feature] Add support for out of the box topics, an special topics list managed by JulieOps (#482)
  * [Feature] Add Kafka Streams applicationId as internal topics, if available (#481)

v4.1.3 / 2022-04-08
===================

* Detect divergences between local state and the remote cluster current status (#478)
* Add validators backwards compatibility (#480)

v4.1.2 / 2022-04-06
===================

* [Security] Fix CWE-787 CVE-2020-36518 for jackson-databind (#476)
* Add support for deploying packaged released to Maven Central (#473) (#475) (#477)
* Add a JSON schema description of the topology/descriptor file sintax (#471) (#472)
* [Test] Refactor and add tests related to Confluent Cloud service account translation feature (#468)
* Allowing to configure the redis bucket used by JulieOps (#465)
* [Test] Clarify S3 Backend IT test (#464)
* [Bug] Fix RedisBackend bootstrap, NullPointerException (#462)
* [Bug] Issue fix (456) for resolution of service account mapping (Translation of principals) (#459)

v4.1.1 / 2022-02-05
===================

* Fix Confluent Cloud ACL(s) API usage, so ACL(s) are finally created properly (#444)
* Fix config passing for topology validator for regular expressions (#443)
* Bump log4j-api from 2.17.0 to 2.17.1 (#436)

v4.1.0 / 2021-12-30
===================

* [ksqlDB] when using ACLs configure all internal topics with ALL permissions for the ksql server user (#433)
* Bring Principal Management for Confluent Cloud out of Experimental into Production ready feature (#435)
* Use Confluent Cloud API when integrating with the Confluent fully managed service (#431)
* Throw an exception when an invalid plan is used (#426)
* Add docker to the SAN to make it run in our in-house Gitlab (#421)
* Improved execution log for topics and schemas (#383)

v4.0.1 / 2021-12-20
==================

* Bump log4j to 2.17.0, prevent latest Log4j CVE, ref https://logging.apache.org/log4j/2.x/security.html (#427)

v4.0.0 / 2021-12-10
===================

* neat: Adapt CI jobs and other actions to latest versions (#419)
* port the main changelog to master
* fix&feature: establish service accounts prefix filter a primary criteria when available to filter the list of access control rules (#418)
* Bump log4j-api from 2.13.3 to 2.15.0 (#416)
* fix: issue with connector and subject permissions being ignored when more than one (#415)
* Add producer Idempotence permissions to Confluent RBAC (#414)
* Fix: Topic config values should take precedence over plan config values (#410)
* Quotas implementation, only quotas based in user principal (#376)
* fix request scope to hold proper pattern type (literal, prefix) in rbac
* add proper clusterIds for RBAC clear bindings operations, not only kafka one
* add support in the custom roles for subject and connect attributes (#406)
* fix incorrect rbac resource name for subjects and connectors (#405)
* ammend order for delete in ksql to be reverse from creation (#400)
* add Parameter for initial Kafka Backend consumer retry
* raise initial load as done if tried at least five times

v4.0.0 / 2021-12-10
==================

* Fix/feature: establish service accounts prefix filter a primary criteria when available to filter the list of access control rules (#418)
* Security fix: Bump log4j-api from 2.13.3 to 2.15.0 (#416)
* fix: issue with connector and subject permissions being ignored when more than one (#415)
* feature: Add producer Idempotence permissions to Confluent RBAC (#414)
* fix: Topic config values should take precedence over plan config values (#410)
* feature: Quotas implementation, only quotas based in user principal (#376)
* feature: add support in the custom roles for subject and connect attributes (#406)
* fix: incorrect rbac resource name for subjects and connectors (#405)
* fix: ammend order for delete in ksql to be reverse from creation (#400)
* fix: add Parameter for initial Kafka Backend consumer retry
* fix: Raise initial load as done if tried at least five times for the Kafka Backend

v3.3.3 / 2021-11-23
===================

* fix: request scope to hold proper pattern type (literal, prefix) in rbac (#407)
* fix: add proper clusterIds for RBAC clear bindings operations, not only kafka one (#407)

v3.3.2 / 2021-11-22
===================

* feat: add support in the custom roles for subject and connect attributes (#406)
* fix: incorrect rbac resource name for subjects and connectors (#405)

v3.3.1 / 2021-11-17
===================

* fix: order for delete in ksql to be reverse from creation (#400)
* add Parameter for initial Kafka Backend consumer retry
* raise initial load as done if tried at least five times

v3.3.0 / 2021-11-13
===================

* fix: delete problem on resource used from the rbac api (wrong resource type) (#398)
* fix/handling of connectors only without defining acls (#397)
* fix: Kafka as a Backend for JulieOps resiliency. Ensure state is not corrupted. (#396)
* feat: add exactly-once to kstreams and create transactionalid ACLs (#388)
* enable the use of patterns within the DLQ allow and deny list (#393)
* Kafka Backend group ID configuration make easier (#391)
* fix: ksqldb delele artefacts delete order, first tables then streams, the reverse of the creation (#392)
* add env vars for kafka.config.topic and instance.id (#390)
* KafkaBackendFix (state initialisation) and exit JVM if exception if raised (#386)
* Add pre install hook verification for requirement java  (#371)
* Relax the conditions for kstream application to not require read and write topics all the topic. included as well more easy to catch error messages (#369)
* Add the missing TransactionID permission for producers when using rbac (#370)
* KsqlDB query application order forced (first streams, then tables) (#368)

v3.2.0 / 2021-10-20
===================

* Introduce Kafka as backend store implementation (#359)
* Add the capability to generate DLQ auto generated topic for a given topology (#357)

v3.1.1 / 2021-10-20
===================

* Enable a feature flag to activate multiple topologies with different context parsing feature, by default only single context will be possible (#360)
* Fix bug that optional values are clean up from the jinja context in case of not being present (#354)
* Feat: add developer manage rbac (#351)
* Add the S3 endpoint as direct config for JulieOps  (#347)

v3.1.0 / 2021-10-05
===================

* Restructure multiple topology loading policy to prevent the restriction of a single context per directory (#341)
* Add a possibility to add custom roles definitions for julieops (#336)

v3.0.4 / 2021-10-05
===================

* Use put method in connector creation, so the operation is idempotent, createOrUpdate (#345)

v3.0.3 / 2021-10-03
===================

* Make projects an optional field, so platform only topologies are easier possible (#343)
* Make topics an optional parameters, so only connector topologies can easier exist (#342)

v3.0.2 / 2021-09-30
===================

* Configure basic auch for Kafka Connect REST api operations (#339)

v3.0.1 / 2021-09-29
===================

* Fix Kerberos library in the docker image, use full jvm in order to avoid missing values (#338)

v3.0.0 / 2021-09-17
===================

* Configurable RBAC role bindings for schema registry subjects (#283) (#284)
* Add missing Schema Registry ACLs (#297)
* Fix: RBAC Subject and Cluster level binding (#301)
* extends RedisBackend to store a full state copy (the new json)  (#333)
* Add a connector management to the sasl example (#332)
* Make FileBackend VCS diff friendly (#317)
* Fix: ssl context contruction when no ssl configuration is provided. (#327)
* Fix: stupid error in checking server labels when creating a connector (#325)
* Enable the option to pass custom truststore and keystore with custom http clients (#324)
* Add missing ACLS for Control Center to work properly (#322)
* Enable group acls for consumers within the rbac bindings api (#321)
* Ammend docker example test ports for openldap image that changed over time (#320)
* Ksql: Add SSL support and  bugfixes (#308)
* Fix problem with legacy release build identation
* Fix build job to sign rpms for release and nightly builds (#318)
* Allow reading of old-style state files. backwards compatible file backend (#315)
* add manual dispatch for nightly build artifacts
* docs: add producer example with transactionID (#311)
* Upgrade to latest version of JDK11 (#316)
* Add automated signing of packages in pipeline and amend docs (#302)
* Fix: Unrecognized field "resource_id" (#307) (#309)
* Add kerberos example for julieops (#310)
* Add support for KSQL queries (#275)
* support prefix for producer transaction id and consumer group (#279)
* avoid connection to mds when running in validate mode
* fix issue when listing transactionId resourceType in the RBAC provider as name is transient from the ACLs default one
* Support Connector deployment for Kafka Connect (#234)
* Add a validate only flag process to facilitate descriptor testing in feature branches and add ConfigurationKeyValidation and TopicNameRegexValidation validations (#274)

v2.1.2 / 2021-04-30
===================
* Bump version for httpclient.version to avoid regression in internal domain validation HTTPCLIENT-2047 (a18ea2d81f8074ca0844582c62b646323b19db02)

v2.1.1 / 2021-04-30
===================

* Amend dependencies to be fully supporting Confluent RBAC implementation  (#272)
* Fix problem with passing a dir that containts another dir for topologies (#269)
* Amend listAcls function for rbac providers so delete and listing after execution works as well when using RBAC (#266)
* Sync integration test Kafka version with the version in the pom. (#260)
* Add overridingClientConfig parameter to allow override and fallbacks ot the original configuration. This would be useful when passing parameters that need to be adapted in some execution scenarios like deployments or CI/CD.
* verify CLI parameters as first thing in the process before creating the main handler object  (#253)

v2.1.0 / 2021-04-03
===================

* Use Set as internal datastructure for 'principals to be created' instead of List.
* Add gcp backend and general cleanup
* Introduce an s3 backend
* Add control center and schema registry principals on the platform level to be handled by the service account manager as well (#247)
* Add error handling when building acls and specially when the group prefix is empty (#245)
* Update topic partition and replication factor to use cluster defaults  if nothing is available in the topology (#244)
* Skip Julie principal when dealing with ACLs, if present (#246)

v2.0.1 / 2021-03-16
===================

* Upgrade dependencies to Kafka 2.7 and Confluent 6.1, plus some other minor ones
* Add support and checks for non ascii characters in schema content (#230)
* Add integration test for SchemaRegistry Management in multiple formats (#229)
* Add more clear error for descriptor files without the topics section in the serdes (#227)
* Fix FileBackend bug in windows to use FileWriter due to closing problems with RandomAccessFile (#222)
* Add support for JsonSchema and Protobuf when working with Schema Registry (#216)

v2.0.0 / 2021-02-27
===================

* Entity rename to JulieOps as a new project name (#213)
* [Sonar 2095] Fix, resources should be closed (#207)

v1.5.1 / 2021-02-25
===================

* Bump jackson.version from 2.10.3 to 2.12.1 (#211)
* File backend fix (#210)
* Support topology validations with fully qualified class names as configuration values (backwards compatible) (#204)
* Add topology metadata support for topics and users (#200)
* README update
* updated github action to take into account the new version branches

v1.5.0 / 2021-01-16
===================

* Add ability to use the name as the end topic name (#194)
* Support ACLs from Cluster (#188)
* Enable to run integration test parametrised vs multiple cp versions (#189)

v1.4.0 / 2021-01-03
===================

* [PrincipalManager] Improved error handling when using Confluent Cloud  (#186)
* Add support for a prefix list to filter which principals are allowed to be managed (#185)
* Enable remote state fetching from the cluster for the principals (#187)
* Add support for absolute paths when fetching schemas from disk (#184)
* Add support for custom SchemaTypes and Compatibility level when managing schemas (#182)
* Support multiple schema per topic (#183)
* Adding ability to pass KTB options for the jvm or the KTB itself via an environment variable (#177)
* Allow topic state bookkeeping from the cluster backend state system   (#180)
* Fix that principle manager was not adding principles defined within consumers/producers at topic levels (#179)
* Improve AccessControlManagerTest (#168)
* Option to disable creation of the CLUSTER CREATE ACL for connectors (#173)
* Add Kafka Streams integration test bed (#169)

v1.3.0 / 2020-12-14
===================

* Fix RPM/DEB packaging update   (#172)
* Managed principals using topology Builder, include support for ccloud CLI (#135)
* Add an action to build and push artifacts for KTB (#166)
* Introduced to detailed allow delete options to be used from the config files.
  One for now dedicated to topics and one for the bindings (#163)
* Keep order in file backend for generated items (#161)
* Nightly docker image build, regular push to docker hub (#153)
* Add workflows with github actions (#152)
* Integration test verifying that producers can produce and consumers can consume (#150)
* Fail on parsing of invalid topologies (#149)
* ACL integration tests using testcontainers (#131)
* Test cleanup (#145)
* Add streams acls for consumer group and support for applicationId (#144)
* Fix bug with a topology/config not using schemas (#143)

1.2.2 / 2020-11-24
==================

* fix path of topology builder gen jar in the deb package mapping (#140)
* Add schema.registry.url to docs and examples (#139)
* make usage of key schemas optional, but throw an exception if key is present but not value schemas (#128)


1.2.0 / 2020-11-16
==================

* Add custom plans to help standardise topic config (#129)
* mockito-all was abandoned in 2015. Using mockito-core instead. (#130)
* fix bug with state management for acls and deletion of removed rules (#127)
* Add topic level Access Control Rules (ACLs/RBAC) (#124)
* Upgrade testcontainers to support recent Docker for Mac. (#121)

1.1.0 / 2020-10-30
==================

* Grouping acls for consumers and producers when the function is selected via configuration (#120)
* Enable JSON and YAML as Topology descriptor file formats. (#108)
* Add support for Transactional and Idempotence producing and consuming in KTB (#119)
* Fix missing key in the deserializer for project causing projects to not properly extract the producers and perse not create the necessary acls
* ammend deb sign script

1.0.0 / 2020-10-23
==================

* Extended the logging support to many components and classes in the KTB with the idea to support better troubleshooting. (Closes #101)
* Add a filter to delete only topic configurations that are explicitly set, not the ones that use default values. (closes #99)
* Fix a bug with wrongly set PatternType for Consumer group acls level. (closes #111)
* Add test and documentation for list type config values as handled using the new config library.
* Add a Health Check function used during the creation of the internal admin client. This function will describe the cluster and perse test if the setup credentials are ok.
  This fixes (#112)

v1.0.0-rc2:
* Check for required configuration values for the configuration of RBAC, if not present it raises a Configuration error
* Update Log4j version to 2.13.3 to prevent CVE-2020-9488
* Add an option to not delete internal topics, including an option configure rules to filter internal topics that can't not be removed config with  _kafka.internal.topic.prefixes_,
  by default this filters all topics starting with underscore. Users can configure more than one filter in the property file.
* Made internal topics for Kafka Connect, Schema Registry and Confluent Control Center configurable.
  By introducing this, users can now handle non-standard instances of Schema Registry or having for example more than
  a single Kafka Connect cluster (situation more than common).
* Add a rest api component, available under server-api and created using the Micronaut Framework. This component is an independent artifact, that wrap the
  Kafka Topology Builder library in a single and uniform web application that can be used to automate and centralise the configuration management for clusters.
* Remove the requirement of writing in the configuration file the access control method, as of now will be ACLs as default value and if other (like RBAC) is required
  it will have to be properly configured.
* Merge #22 to provide the initial support for schema management. Now you can manage schemas with Confluent Schema Registry using the Kafka Topology Builder. Thanks Kiril.
* Add an option to specify custom consumer groups in the topology descriptor
* Add option to increase the partition count in an already created topic.
* Add support for cluster level acls for kafka, schema registry and kafka connect. This solve issues #42.
* Add support for schema level permissions in the rbac mode. This solve issue #44.
* Add support for connect level permissions in the rbac mode. This solve issue #45.
* Implement an initial method for building an execution plan including an option to print a dry-run. This solve issue #62.
* Rename team as context as it was a confusing wording. This solve issue #68.
* Fix bug on removal of acls. This solve issue #72.
* Fix bug with schema registration and file path reference,  now by default all are relative to the main directory where the topology file is staying.
* Improved delete and general management of acls logic, only create what is necessary, only delete what is mandatory.
* Fixed a problem with the schema registry client that got no configs, so it would not be possible to use with security contrain systems such as confluent cloud.
* Add the possibility to define a topic and project pattern using the jinja format, like this users can tune the order of the variables without hampeting the full idea of the structure
* Add a framework to add incoming validation for topology files, this could be configured with the topology.validations config array.
* Added the lightbend config library to support a more structured config management, including usage of environment variables and multiple config formats.
* Made the broker CLI param optional, however one of both config or CLI has to be present, CLI takes always precedence over the config value.

v1.0.0-rc1:
* Add support for platform wide acls for control center in teh topology description file.
* Port support for schema registry and confluent control center roles in the rbac provider. Now the two providers are future pair.
* Fix the usage of the source field, it should not be mandatory as the others list and the custom topology serdes can cover a dynamic list
  of fields between the team and the first project separator.
* Add code to support passing a directory, only topologies for a single team are supported for now.
* Add support for passing a directory, instead of a single file as topology. If a directory is passed, all topologies are expected to be from
  the same team and all subsequent projects will be at append one after the other.
* Implement the necessary methods to pass back the status from the rbac modules, so the reply and clean up is possible with multiple runs
* Added an option to use redis as external system to keep the acls status list. Now people can use either files or redis.
  Note in the future this subsystem should be externalised as plugins
* raise up error handling in case of non available cluster
* Make usage of the maven jdeb plugin to build a deb package to use in distributions such as Debian and relative like the Ubuntu family
* Fix parser problem with non required parameters, such as the ones for the acls where an empty array had to be in place, now all principals for acls if not required can be ignored and will have no effect
* Ammend param and typo issues with rbac that where not facilitating the execution flow, updated as well the examples to match a correct execution flow

v0.11:
* Add support for platform wide acls for schema registry in the topology description file.
* Rebird the option to disable deletes when not required anymore.
* Code refactoring in several classes
* add a option, activated by default to print the current status of topics created and acls being set after each run. this is very valuable to know the current state of the cluster after each execution
* add release mechanism for docker images with the kafka topology builder (#21)
* Add version number to maven-compiler-plugin (#16)
* Add support to keeping notes of generated acls (#15)
* Add support for storing the current generated acls within a state file. This is useful to delete
  acls that are known and generated previously by the topology builder.

v0.10.3:
* updated ACLs for producers and consumers to include the describe permission in order to properly
  allow for metadata recollection.
* Fix wrong verification for parameters in the CLI.

v0.10.2:
* Add improved connection handling when talking with the RBAC MDS server
* Extended the test suite for rbac and isoleted test with the SASL plain suit.

v0.10.1:
* Fix the RPM builder script, build an rpm that create proper users for running the application.
* Fix param passing to the RBAC provider from the properties file.
* Add more clear example configuration within the RPM artifacts.

v0.10:
* Separate access control providers between simple ACLs and RBAC provider.
* Introduced an option to select with Access Control mechanism is used using the application configuration file.
* Add initial rbac support for application roles.
* Add the option to define custom rbac (per project) roles. This option allows you to define inside the project level role and principals mapping such as ResourceOwner, ... this will be automatically applied per topic if using the rbac acces control provider

v0.9:
* RPM package for installation in Redhat based systems.
* Fully descriptive managed ACLs (via a descriptor file) - Create, Read, Update and Delete.
* Fully descriptive and structured topic management (via a descriptor file) - Create, Read, Update and Delete.
* Simple Roles for Connectors, Streams application, Consumers and Producers.