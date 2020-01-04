Rails.application.routes.draw do
  #devise_for :users
  # For details on the DSL available within this file, see https://guides.rubyonrails.org/routing.html

  resources :topologies
  resources :projects

  devise_for :users, :controllers => { registrations: 'registrations'}

  root to: "main#index"

  get 'admin', to: 'admin#index'
  get 'admin/:id/edit', to: 'admin#edit', as: 'admin_users'

  post 'admin/:id/add_project', to: 'admin#user_add_project', as: 'admin_user_add_project'

end
