Example for a descriptor file
*******************************

The acutal topology will be defined then in a yaml file like the ones to be found in the example folder of the project.
The following examples will try and explain the actual functionality a bit more in detail.

First Example - Deploying topics only
-----------

.. literalinclude:: ../example/descriptor_only_topics.yaml
  :language: YAML


This descriptor will create the two topics specified in the topics list.
The naming of actual topics follows this scheme:


``team_name = team + source(if given) + topic.name + topic.dataType(if given)``

The config can contain any parameters that are allowed for topic-creation via admin-client (https://kafka.apache.org/documentation/#topicconfigs)

In this very example you will get the following topics:

- planetexpress.source.natas.foo
- planetexpress.source.natas.avro


Second Example - Deploying topics with project wide ACLs
-----------

Going on step further one can also create project-wide ACLs for topics

.. literalinclude:: ../example/descriptor_project_wide_acls.yaml
  :language: YAML

This descriptor will add the exact same topics as before.
Additionally all principals defined in `consumers` will have the DeveloperRead role, the `producers` will have the DeveloperWrite Role on all topics defined in the topics section:
::
   Principal  |     Role       | ResourceType |                Name                 | PatternType
 +-----------+----------------+--------------+-------------------------------------+-------------+
  User:Bender | DeveloperRead  | Topic        | planetexpress.source.natas.bar.avro | LITERAL
  User:Bender | DeveloperRead  | Topic        | planetexpress.source.natas.foo      | LITERAL
  User:Fry    | DeveloperRead  | Topic        | planetexpress.source.natas.bar.avro | LITERAL
  User:Fry    | DeveloperRead  | Topic        | planetexpress.source.natas.foo      | LITERAL
  User:Fry    | DeveloperWrite | Topic        | planetexpress.source.natas.bar.avro | LITERAL
  User:Fry    | DeveloperWrite | Topic        | planetexpress.source.natas.foo      | LITERAL
  User:Lila   | DeveloperRead  | Topic        | planetexpress.source.natas.bar.avro | LITERAL
  User:Lila   | DeveloperRead  | Topic        | planetexpress.source.natas.foo      | LITERAL
  User:Lila   | DeveloperWrite | Topic        | planetexpress.source.natas.bar.avro | LITERAL
  User:Lila   | DeveloperWrite | Topic        | planetexpress.source.natas.foo      | LITERAL

In this particular case only `Fry` and `Lila` can read and write from/to the topics - `Bender` can only consume.

Third Example - Adding ACLs for Connectors and Streams
-----------

TODO


Fourth Example - Setting project wide RBAC roles
-----------

.. literalinclude:: ../example/descriptor_project_wide_rbac.yaml
  :language: YAML


By adding a `rbac` block one can additionally set project wide permissions:
::
   Principal  |     Role       | ResourceType |                Name                 | PatternType
 +-----------+----------------+--------------+-------------------------------------+-------------+
  User:Zoidberg   | DeveloperManage  | Topic        | torchwood.mainframe.pilos | PREFIXED
  User:Professor  | ResourceOwner    | Topic        | torchwood.mainframe.pilos | PREFIXED

`Jack` will get the `ResourceOwner` role on `torchwood.mainframe.pilos*` meaning he can
delete, create and access topics matching that wildcard.
`Ianto` can create and delete the topics matching the wildcard, but not access the data, since he has the limited role
`DeveloperManage`.


Fifth Example - Platform wide RBAC
-----------

TODO


