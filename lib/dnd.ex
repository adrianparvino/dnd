defmodule Dnd do
  @moduledoc """
  Dnd keeps the contexts that define your domain
  and business logic.

  Contexts are also responsible for managing your data, regardless
  if it comes from the database, an external API or others.
  """

  use Supervisor

  def start_link(opts) do
    Supervisor.start_link(__MODULE__, :ok, opts)
  end

  @impl true
  def init(:ok) do
    children = [
      Dnd.Battle
    ]

    Supervisor.init(children, strategy: :one_for_one)
  end
end
