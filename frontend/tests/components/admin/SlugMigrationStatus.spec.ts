import SlugMigrationStatus from "@/components/admin/SlugMigrationStatus.vue"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it } from "vitest"
import helper, { mockSdkService } from "@tests/helpers"

describe("SlugMigrationStatus", () => {
  it("shows loading message initially", async () => {
    mockSdkService("getStatus", {
      foldersMissingSlug: 0,
      notesMissingSlug: 0,
    })
    const wrapper = helper.component(SlugMigrationStatus).mount()
    expect(wrapper.text()).toContain("Loading slug migration status...")
  })

  it("shows that migration is resumable and server-side", async () => {
    mockSdkService("getStatus", {
      foldersMissingSlug: 0,
      notesMissingSlug: 0,
    })
    const wrapper = helper.component(SlugMigrationStatus).mount()
    expect(wrapper.text()).toContain("bounded batches on the server")
    expect(wrapper.text()).toContain("Progress is saved in the database")
  })

  it("displays counts after loading", async () => {
    mockSdkService("getStatus", {
      foldersMissingSlug: 7,
      notesMissingSlug: 42,
    })

    const wrapper = helper.component(SlugMigrationStatus).mount()
    await flushPromises()

    expect(wrapper.text()).toContain("Folders missing slug")
    expect(wrapper.text()).toContain("7")
    expect(wrapper.text()).toContain("Notes missing slug")
    expect(wrapper.text()).toContain("42")
    expect(wrapper.text()).toContain("bounded batches on the server")
  })
})
