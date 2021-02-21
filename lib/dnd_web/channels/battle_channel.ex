defmodule DndWeb.BattleChannel do
  use Phoenix.Channel

  def join("battle", _message, socket) do
    characters = Dnd.Battle.characters(Dnd.Battle)

    {:ok, %{characters: characters}, socket}
  end

  def handle_in("next", %{}, socket) do
    Dnd.Battle.next(Dnd.Battle)

    broadcast_from!(socket, "next", %{})
    {:noreply, socket}
  end

  def handle_in("set-characters", payload, socket) do
    Dnd.Battle.set_characters(Dnd.Battle, payload)

    broadcast_from!(socket, "set-characters", %{cs: payload})
    {:noreply, socket}
  end
end
