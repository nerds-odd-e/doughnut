# Doughnut

## About

Doughnut is Personal Knowledge Management ([PKM](https://en.wikipedia.org/wiki/Personal_knowledge_management)) tool combining [zettelkasten](https://eugeneyan.com/writing/note-taking-zettelkasten/) style of knowledge capture with some features to enhance learning (spaced-repetition, smart reminders) and ability to share knowledge bits with other people (for buddy/team learning).

## Getting started

1. Install nix

Find instruction at nixos.org (multi-user installation).

For macOS:
```
 sh <(curl -L https://nixos.org/nix/install) --darwin-use-unencrypted-nix-store-volume
```

For Linux:
```
sh <(curl -L https://nixos.org/nix/install) --daemon
```

(NB: if the install script fails to add sourcing of `nix.sh` in `.bashrc` or `.profile`, you can do it manually `source /etc/profile.d/nix.sh`)


2. Setup and run doughnut for the first time

    clone this repo
    run `nix-shell --pure`
    run `idea-community &`
    open doughnut project in idea
    click import gradle project
    wait for deps resolution
    run `cd backend`
    run `gradle bootRun`
    open `localhost:8080`

