class Project < ApplicationRecord
  has_many :topics, dependent: :destroy
  belongs_to :topology
  has_and_belongs_to_many :users
end
