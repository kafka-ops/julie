class TopologiesController < ApplicationController

  def index
  end
  
  def new
    @topology = Topology.new
  end
end
