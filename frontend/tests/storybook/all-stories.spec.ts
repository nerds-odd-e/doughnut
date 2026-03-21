import { describe, it } from "vitest"
import { render } from "vitest-browser-vue"
import type { Meta, StoryObj } from "@storybook/vue3"
import { createRouter, createWebHistory } from "vue-router"
import type { RouteRecordRaw } from "vue-router"
import { routeMetadata } from "@/routes/routeMetadata"
import { ref } from "vue"
import type { User } from "@generated/doughnut-backend-api"
import makeMe from "doughnut-test-fixtures/makeMe"
import { storyFiles } from "../../storyFiles.generated"

// Mock router for components that use vue-router (same as Storybook preview)
const mockRoutes: RouteRecordRaw[] = routeMetadata.map((metadata) => ({
  ...metadata,
  component: {
    template: `<div>${metadata.name} (Mock)</div>`,
  },
}))

const router = createRouter({
  history: createWebHistory("/"),
  routes: mockRoutes,
})

describe("All Storybook Stories", () => {
  const storyFilesList = storyFiles

  if (storyFilesList.length === 0) {
    it("should find story files", () => {
      throw new Error("No story files found. Check the glob pattern.")
    })
  }

  storyFilesList.forEach(({ filePath, storyFile }) => {
    const meta = storyFile.default as Meta | undefined
    if (!meta) {
      return
    }

    const title = meta.title || filePath

    if (meta.parameters?.test?.disable) {
      return
    }

    if (!meta.component) {
      return
    }

    describe(title, () => {
      Object.entries(storyFile).forEach(([storyName, story]) => {
        if (storyName === "default") {
          return
        }

        if (typeof story !== "object" || story === null) {
          return
        }

        const storyObj = story as StoryObj

        if (storyObj.parameters?.test?.disable) {
          return
        }

        it(`renders ${storyName}`, async () => {
          const Component = meta.component
          if (!Component) {
            throw new Error(
              `No component found for story ${storyName} in ${filePath}`
            )
          }

          const args = storyObj.args || {}
          const mockUser = makeMe.aUser.please()

          render(Component, {
            props: args,
            global: {
              plugins: [router],
              provide: {
                currentUser: ref<User | undefined>(mockUser),
              },
            },
          })
        })
      })
    })
  })
})
