class TopologiesController < ApplicationController

  def new
    @topology = Topology.new
  end
end
