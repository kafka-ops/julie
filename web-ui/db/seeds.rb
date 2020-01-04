# This file should contain all the record creation needed to seed the database with its default values.
# The data can then be loaded with the rails db:seed command (or created alongside the database with db:setup).
#
# Examples:
#
#   movies = Movie.create([{ name: 'Star Wars' }, { name: 'Lord of the Rings' }])
#   Character.create(name: 'Luke', movie: movies.first)


topology = Topology.create(team: "Team", source: "Source")
Project.create(name: "project", topology: topology)
p = Project.create(name: "another", topology: topology)

t = Topic.create(name: "foo", config: "")
t2 = Topic.create(name: "bar", config: "")

p.topics << t
p.topics << t2

p.save
