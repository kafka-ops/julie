class Topology < ApplicationRecord
  has_many :projects, dependent: :destroy

end
