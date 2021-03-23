{ pkgs ? import <nixpkgs> {} }:

pkgs.mkShell {
  buildInputs = [
    pkgs.elixir
    pkgs.openjdk11
    pkgs.yarn
    pkgs.nodejs_latest
    pkgs.python2
    pkgs.clojure
    pkgs.inotify-tools

    # keep this line if you use bash
    pkgs.bashInteractive
  ];
}
