import { describe, it } from "vitest"
import { render } from "@testing-library/vue"
import type { Meta, StoryObj } from "@storybook/vue3"
import { createRouter, createWebHistory } from "vue-router"
import type { RouteRecordRaw } from "vue-router"
import { routeMetadata } from "../../src/routes/routeMetadata"
import { ref } from "vue"
import type { User } from "@generated/backend"
import makeMe from "../fixtures/makeMe"

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

type StoryFile = {
  default: Meta
  [name: string]: StoryObj | Meta
}

function getAllStoryFiles() {
  const storyFiles = Object.entries(
    import.meta.glob<StoryFile>("../../src/**/*.stories.@(js|jsx|mjs|ts|tsx)", {
      eager: true,
    })
  )

  return storyFiles.map(([filePath, storyFile]) => {
    return { filePath, storyFile }
  })
}

describe("All Storybook Stories", () => {
  const storyFiles = getAllStoryFiles()

  if (storyFiles.length === 0) {
    it("should find story files", () => {
      throw new Error("No story files found. Check the glob pattern.")
    })
  }

  storyFiles.forEach(({ filePath, storyFile }) => {
    const meta = storyFile.default
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

        it(`renders ${storyName}`, () => {
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
