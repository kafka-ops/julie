class Project < ApplicationRecord
  has_many :topics, dependent: :destroy
  belongs_to :topology
end
