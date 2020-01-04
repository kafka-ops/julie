class AdminController < ApplicationController

  def index
    @users = User.all
  end

  def edit
    @user = User.find(params[:id])
    @user_projects = @user.projects
    @all_projects = Project.all - @user_projects
  end

  def user_add_project
    @user = User.find(params[:id])
    project = Project.find(params[:project])
    if !@user.projects.include?(project)
      @user.projects << project
      @user.save
    end
    redirect_to admin_path
  end

end
