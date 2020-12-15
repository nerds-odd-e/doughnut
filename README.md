# doughnut
Learning aide, note taking, team learning, etc.

Install nix

Find instruction at nixos.org. For macOS some the installing command is
different:

```
 sh <(curl -L https://nixos.org/nix/install) --darwin-use-unencrypted-nix-store-volume
```

Create a nix config file in ~/.config/nixpkgs/config.nix with content:
```
{ allowUnfree = true; }
```

<<<<<<< HEAD
=======

nix-shell --pure
>>>>>>> a1d0272 (readme)
