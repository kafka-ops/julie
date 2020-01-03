class ProjectsController < ApplicationController

    def index
      @projects = Project.all
    end

    def new
      @project = Project.new
    end

end
