defmodule Dnd.Application do
  # See https://hexdocs.pm/elixir/Application.html
  # for more information on OTP Applications
  @moduledoc false

  use Application

  def start(_type, _args) do
    children = [
      # Start the Telemetry supervisor
      DndWeb.Telemetry,
      # Start the PubSub system
      {Phoenix.PubSub, name: Dnd.PubSub},
      Dnd.Battle,
      # Start the Endpoint (http/https)
      DndWeb.Endpoint,
      # Start a worker by calling: Dnd.Worker.start_link(arg)
      # {Dnd.Worker, arg}
    ]

    # See https://hexdocs.pm/elixir/Supervisor.html
    # for other strategies and supported options
    opts = [strategy: :one_for_one, name: Dnd.Supervisor]
    Supervisor.start_link(children, opts)
  end

  # Tell Phoenix to update the endpoint configuration
  # whenever the application is updated.
  def config_change(changed, _new, removed) do
    DndWeb.Endpoint.config_change(changed, removed)
    :ok
  end
end
