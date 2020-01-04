class AdminController < ApplicationController

  before_action :authenticate_user!
  before_action :validate_user!

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

  def delete
    user    = User.find(params[:user_id])
    project_to_delete = Project.find(params[:project_id])

    user.projects.delete(project_to_delete)
    redirect_to admin_path
  end

  private

  def validate_user!
    redirect_to new_user_session_path unless current_user
    unless current_user.id == User.first.id
      flash[:alert] = "Only super users are allowed to see the admin panel"
      redirect_to root_path
    end
  end

end
