import { defineConfig } from 'cypress'
import ciConfig from './e2e_test/config/ci'

/** Default for `cypress open`; `pnpm cy:run` uses `e2e_test/config/ci.ts` explicitly (same config + `expose` below). */
export default defineConfig({
  ...ciConfig,
  expose: {
    RECORD_E2E_TIMING: process.env.RECORD_E2E_TIMING,
  },
})
