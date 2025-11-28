import type { Preview } from "@storybook/vue3"
import { setup } from "@storybook/vue3"
import { createRouter, createWebHistory } from "vue-router"
import type { RouteRecordRaw } from "vue-router"
import { routeMetadata } from "../src/routes/routeMetadata"
import "../src/assets/daisyui.css"

// Reuse route metadata from production code without importing page components
// This eliminates duplication - route definitions are defined once in routeMetadata.ts
const mockRoutes: RouteRecordRaw[] = routeMetadata.map((metadata) => ({
  ...metadata,
  component: {
    template: `<div>${metadata.name} (Mock)</div>`,
  },
}))

// Mock router for components that use vue-router
const router = createRouter({
  history: createWebHistory("/"),
  routes: mockRoutes,
})

setup((app) => {
  app.use(router)
})

const preview: Preview = {
  parameters: {
    controls: {
      matchers: {
        color: /(background|color)$/i,
        date: /Date$/i,
      },
    },
  },
}

export default preview
