import type { Preview } from "@storybook/vue3-vite"
import { setup } from "@storybook/vue3-vite"
import { createRouter, createWebHistory } from "vue-router"
import { dummyRouteRecordsFromMetadata } from "../src/routes/dummyRouteRecords"
import "../src/assets/daisyui.css"
import { ref } from "vue"
import type { User } from "@generated/donut-backend-api"
import makeMe from "donut-test-fixtures/makeMe"

const router = createRouter({
  history: createWebHistory("/"),
  routes: dummyRouteRecordsFromMetadata,
})

setup((app) => {
  app.use(router)
  // Provide mock currentUser for components that need it (e.g., GlobalBar search button)
  const mockUser: User = makeMe.aUser.please()
  app.provide("currentUser", ref<User | undefined>(mockUser))
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
