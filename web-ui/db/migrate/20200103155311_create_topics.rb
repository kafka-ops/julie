class CreateTopics < ActiveRecord::Migration[6.0]
  def change
    create_table :topics do |t|
      t.string :name
      t.string :config
      t.belongs_to :project

      t.timestamps
    end
  end
end
