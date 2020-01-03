class CreateTopologies < ActiveRecord::Migration[6.0]
  def change
    create_table :topologies do |t|
      t.string :team
      t.string :source

      t.timestamps
    end
  end
end
