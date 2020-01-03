Rails.application.routes.draw do
  #devise_for :users
  # For details on the DSL available within this file, see https://guides.rubyonrails.org/routing.html

  resources :topologies

  devise_for :users, :controllers => { registrations: 'registrations'}

  root to: "main#index"
end
