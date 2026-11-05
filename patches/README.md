# Dependency patches

Applied by pnpm through `patchedDependencies` in `pnpm-workspace.yaml`. Managing them: `.agents/skills/dependency-patches/SKILL.md`.

## daisyui@5.7.50.patch

- **Bug:** `utilities/join/object.js` puts relative selectors inside `@scope (.join)`, for example `> :where(:focus, :has(:focus))`. Lightningcss (`cssMinify: 'lightningcss'`, plus Tailwind's own optimize pass) can't parse them, reports `Invalid empty selector`, and **drops** the rules. In production, focused or hovered `daisy-join` items lose their z-index.
- **Fix:** the two selectors become `:scope > :where(...)`.
- **Guard:** `failOnCssOptimizeWarnings` in `frontend/vite.config.ts` fails `vite build` on `Found N warnings while optimizing generated CSS`. Unit tests can't catch this because Vitest uses dev CSS, which is never optimized.
- **Still needed?** Run `grep -c '"> :where(' node_modules/daisyui/utilities/join/object.js` in the new daisyUI version. A result of 0 means upstream fixed it.

## find-cypress-specs@1.54.9.patch

Not listed in `patchedDependencies`, so it isn't applied.
