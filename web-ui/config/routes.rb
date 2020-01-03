Rails.application.routes.draw do

  resources :users, only: [:new, :create]
  get  'login', to: 'sessions#new'
  post 'login', to: 'sessions#create'
  get  'welcome', to: 'sessions#welcome'
  # For details on the DSL available within this file, see https://guides.rubyonrails.org/routing.html

  root to: "main#index"
end
