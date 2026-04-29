import DataMigrationPanel from "@/components/admin/DataMigrationPanel.vue"
import { AdminDataMigrationController } from "@generated/doughnut-backend-api/sdk.gen"
import type { AdminDataMigrationStatusDto } from "@generated/doughnut-backend-api/types.gen"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, expect, it, vi } from "vitest"
import helper, {
  mockSdkService,
  wrapSdkError,
  wrapSdkResponse,
} from "@tests/helpers"

describe("DataMigrationPanel", () => {
  const idleDto: AdminDataMigrationStatusDto = {
    completedOnce: false,
    message: "No migration has completed in this server process yet.",
    detachedChildFoldersFromIndexFolder: 0,
    updatedNormalNotesDetachedFromIndex: 0,
    updatedRelationNotesClearedFolder: 0,
    deletedObsoleteNotebookNameRootFolders: 0,
    notebookCountSlugScan: 0,
  }

  let runSpy: ReturnType<typeof mockSdkService<"runDataMigration">>
  let getSpy: ReturnType<typeof mockSdkService<"getAdminDataMigrationStatus">>

  beforeEach(() => {
    getSpy = mockSdkService("getAdminDataMigrationStatus", idleDto)
    runSpy = mockSdkService("runDataMigration", idleDto)
  })

  it("shows intro copy about migrating index-folder topology", async () => {
    const wrapper = helper.component(DataMigrationPanel).mount()
    await flushPromises()

    expect(wrapper.text()).toContain("index-folder")
    expect(wrapper.text()).toContain("retained for this running server")
  })

  it("loads status from the server when mounted", async () => {
    const wrapper = helper.component(DataMigrationPanel).mount()
    await flushPromises()

    expect(getSpy).toHaveBeenCalled()
    expect(wrapper.text()).toContain(
      "No migration has completed in this server process yet."
    )
    expect(wrapper.text()).toContain("Last run:")
  })

  it("renders Run migration primary control", async () => {
    const wrapper = helper.component(DataMigrationPanel).mount()
    await flushPromises()

    const btn = wrapper.find('[data-testid="run-data-migration-button"]')
    expect(btn.exists()).toBe(true)
    expect(btn.text()).toContain("Run migration")
  })

  it("clicking Run migration calls runDataMigration and updates summary", async () => {
    const migrated: AdminDataMigrationStatusDto = {
      completedOnce: true,
      lastCompletedAt: "2026-04-01T08:30:12.345Z",
      message:
        "Detached child folders 1; moved normal notes 2; cleared relation note folders 0;",
      detachedChildFoldersFromIndexFolder: 1,
      updatedNormalNotesDetachedFromIndex: 2,
      updatedRelationNotesClearedFolder: 0,
      deletedObsoleteNotebookNameRootFolders: 0,
      notebookCountSlugScan: 3,
    }
    runSpy.mockResolvedValue(wrapSdkResponse(migrated))

    const wrapper = helper.component(DataMigrationPanel).mount()
    await flushPromises()

    await wrapper
      .find('[data-testid="run-data-migration-button"]')
      .trigger("click")
    await flushPromises()

    expect(runSpy).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain("Detached child folders 1")
  })

  it("shows error when runDataMigration fails", async () => {
    vi.spyOn(
      AdminDataMigrationController,
      "runDataMigration"
    ).mockResolvedValue(wrapSdkError("Forbidden"))

    const wrapper = helper.component(DataMigrationPanel).mount()
    await flushPromises()

    await wrapper
      .find('[data-testid="run-data-migration-button"]')
      .trigger("click")
    await flushPromises()

    expect(wrapper.text()).toContain("Forbidden")
  })
})
