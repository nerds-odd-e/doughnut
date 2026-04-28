import SlugMigrationStatus from "@/components/admin/SlugMigrationStatus.vue"
import { WikiSlugMigrationAdminController } from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it, vi } from "vitest"
import helper, {
  mockSdkService,
  wrapSdkError,
  wrapSdkResponse,
} from "@tests/helpers"

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

  it("runs folder batches then note batches until complete", async () => {
    mockSdkService("getStatus", {
      foldersMissingSlug: 1,
      notesMissingSlug: 2,
    })
    vi.spyOn(
      WikiSlugMigrationAdminController,
      "batchMigrateFolders"
    ).mockResolvedValue(
      wrapSdkResponse({
        processedInBatch: 1,
        status: {
          foldersMissingSlug: 0,
          notesMissingSlug: 2,
        },
      }) as Awaited<
        ReturnType<
          (typeof WikiSlugMigrationAdminController)["batchMigrateFolders"]
        >
      >
    )
    vi.spyOn(
      WikiSlugMigrationAdminController,
      "batchMigrateNotes"
    ).mockResolvedValue(
      wrapSdkResponse({
        processedInBatch: 2,
        status: {
          foldersMissingSlug: 0,
          notesMissingSlug: 0,
        },
      }) as Awaited<
        ReturnType<
          (typeof WikiSlugMigrationAdminController)["batchMigrateNotes"]
        >
      >
    )

    const wrapper = helper.component(SlugMigrationStatus).mount()
    await flushPromises()

    await wrapper.find("button.daisy-btn-primary").trigger("click")
    await flushPromises()

    expect(
      WikiSlugMigrationAdminController.batchMigrateFolders
    ).toHaveBeenCalledWith({
      query: { limit: 100 },
    })
    expect(
      WikiSlugMigrationAdminController.batchMigrateNotes
    ).toHaveBeenCalledWith({
      query: { limit: 100 },
    })

    expect(wrapper.text()).toContain("Slug migration is complete")
  })

  it("shows API error when a batch fails", async () => {
    mockSdkService("getStatus", {
      foldersMissingSlug: 1,
      notesMissingSlug: 0,
    })
    vi.spyOn(
      WikiSlugMigrationAdminController,
      "batchMigrateFolders"
    ).mockResolvedValue(
      wrapSdkError("Batch failed") as Awaited<
        ReturnType<
          (typeof WikiSlugMigrationAdminController)["batchMigrateFolders"]
        >
      >
    )

    const wrapper = helper.component(SlugMigrationStatus).mount()
    await flushPromises()

    await wrapper.find("button.daisy-btn-primary").trigger("click")
    await flushPromises()

    expect(wrapper.text()).toContain("Batch failed")
    expect(wrapper.text()).not.toContain("Migrating…")
  })

  it("shows stuck message when a batch makes no progress", async () => {
    mockSdkService("getStatus", {
      foldersMissingSlug: 2,
      notesMissingSlug: 0,
    })
    vi.spyOn(
      WikiSlugMigrationAdminController,
      "batchMigrateFolders"
    ).mockResolvedValue(
      wrapSdkResponse({
        processedInBatch: 0,
        status: {
          foldersMissingSlug: 2,
          notesMissingSlug: 0,
        },
      }) as Awaited<
        ReturnType<
          (typeof WikiSlugMigrationAdminController)["batchMigrateFolders"]
        >
      >
    )

    const wrapper = helper.component(SlugMigrationStatus).mount()
    await flushPromises()

    await wrapper.find("button.daisy-btn-primary").trigger("click")
    await flushPromises()

    expect(wrapper.text()).toContain(
      "Migration made no progress on folders while some folders still lack a slug"
    )
  })
})
