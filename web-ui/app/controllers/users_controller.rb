class UsersController < ApplicationController
  def new
  end

  def create
   @user = User.create(params.require(:user).permit(:username, :password))
   session[:user_id] = @user.id
   redirect_to '/welcome'
end
end
