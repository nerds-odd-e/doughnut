import { beforeEach, describe, it } from "vitest"
import { render } from "vitest-browser-vue"
import type { Meta, StoryObj } from "@storybook/vue3"
import { createRouter, createWebHistory } from "vue-router"
import { dummyRouteRecordsFromMetadata } from "@/routes/dummyRouteRecords"
import { ref } from "vue"
import type { User } from "@generated/donut-backend-api"
import makeMe from "donut-test-fixtures/makeMe"
import { mockShowNote } from "@tests/helpers"
import { storyFiles } from "../../storyFiles.generated"

const router = createRouter({
  history: createWebHistory("/"),
  routes: dummyRouteRecordsFromMetadata,
})

describe("All Storybook Stories", () => {
  const storyFilesList = storyFiles

  beforeEach(() => {
    mockShowNote()
  })

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
