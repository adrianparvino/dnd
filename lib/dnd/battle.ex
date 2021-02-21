defmodule Dnd.Battle do
  use GenServer

  def start_link(_opts) do
    GenServer.start_link(__MODULE__, [], name: Dnd.Battle)
  end

  def init([]) do
    {:ok, ["Cat 1", "Meownster 1", "Meownster 2", "Meownster 3", "Cat 2", "Cat 3"]}
  end

  def characters(registry) do
    GenServer.call(registry, :characters)
  end

  def next(registry) do
    GenServer.cast(registry, :next)
  end

  def set_characters(registry, characters) do
    GenServer.cast(registry, {:set_characters, characters})
  end

  def handle_call(:characters, _from, state) do
    {:reply, state, state}
  end

  def handle_cast(:next, [x | xs]) do
    new_state = xs ++ [x]
    {:noreply, new_state}
  end

  def handle_cast({:set_characters, characters}, _) do
    {:noreply, characters}
  end
end
