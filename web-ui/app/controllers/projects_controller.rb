class ProjectsController < ApplicationController

  before_action :authenticate_user!

    def index
      @projects = current_user.projects
      @user_types = [ 'Consumer', 'Producer', 'Connector', 'Streams']
    end

    def new
      @project = Project.new
    end

    def add_topic
    end

    def add_user
    end

end
