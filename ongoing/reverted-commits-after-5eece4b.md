# Terry Yin commits reverted after 5eece4b8f

Single revert commit: `6a5df5126` — *Revert Terry Yin commits after 5eece4b*.

`5eece4b8f7169379e6af8643ba6f4afa80c715b4` was **not** reverted.

## Reverted commit IDs (reverse order of `git revert`, i.e. newest first)

1. `5fb9150c267b20eec069a841ba73feb504e6df91` — Enhance CLI modal architecture with terminal resize handling and layout updates
2. `fcf3d890d3451c7c07639442ab4f7af66ea73bbd` — Refine testing guidelines for observable behavior across layers
3. `efdcfb14ae11fd19db0458e9efb73810d0e1c80e` — Revert "Enhance cursor handling in CLI section parser for improved input navigation"
4. `2dba23e1946b6b46b73acb4a36452c455d2d15d8` — Enhance cursor handling in CLI section parser for improved input navigation
5. `cf4a9f39bb653a47cf5b8aa8bfa68b5e7468a720` — Refine CLI modal architecture and finalize Ink migration
6. `d61b1f3a9ca3e423d7e15c2dea782566bb2520c5` — Add session confirmation interaction and tests for yes/no input handling *(cherry-picked back onto `main`)*

## Reapply later (Terry-only; chronological order)

Other authors’ commits that landed between these on `main` are still present; replay only the above if you intend to restore Terry’s changes without redoing dependency bumps:

**Cherry-picked onto `main`:** `d61b1f3a9ca3e423d7e15c2dea782566bb2520c5` — Add session confirmation interaction and tests for yes/no input handling (`ongoing/cli-modal-architecture.md` Notes conflict resolved: Phase I complete, next J/K).

```bash
git cherry-pick cf4a9f39bb653a47cf5b8aa8bfa68b5e7468a720 \
  2dba23e1946b6b46b73acb4a36452c455d2d15d8 \
  efdcfb14ae11fd19db0458e9efb73810d0e1c80e \
  fcf3d890d3451c7c07639442ab4f7af66ea73bbd \
  5fb9150c267b20eec069a841ba73feb504e6df91
```

Resolve conflicts as needed; you may drop `efdcfb14a…` if you instead keep the cursor-handling behavior from `2dba23e19…`.
