class ProjectsController < ApplicationController

  before_action :authenticate_user!

    def index
      @projects = Project.all
    end

    def new
      @project = Project.new
    end

end
