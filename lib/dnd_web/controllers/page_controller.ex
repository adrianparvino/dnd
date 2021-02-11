defmodule DndWeb.PageController do
  use DndWeb, :controller

  def index(conn, _params) do
    render(conn, "index.html")
  end
end
