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
    migrationInProgress: false,
    moreBatchesRemain: false,
    completedBatchOrdinal: 0,
    batchTotalPlanned: 0,
    batchPhaseSummary: "",
  }

  let runBatchSpy: ReturnType<typeof mockSdkService<"runDataMigrationBatch">>
  let getSpy: ReturnType<typeof mockSdkService<"getAdminDataMigrationStatus">>

  beforeEach(() => {
    getSpy = mockSdkService("getAdminDataMigrationStatus", idleDto)
    runBatchSpy = mockSdkService("runDataMigrationBatch", idleDto)
  })

  it("shows intro copy about migrating index-folder topology", async () => {
    const wrapper = helper.component(DataMigrationPanel).mount()
    await flushPromises()

    expect(wrapper.text()).toContain("index-folder")
    expect(wrapper.text()).toContain("small HTTP batches")
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

  it("clicking Run migration chains runDataMigrationBatch until complete", async () => {
    const midProgress: AdminDataMigrationStatusDto = {
      migrationInProgress: true,
      moreBatchesRemain: true,
      completedBatchOrdinal: 1,
      batchTotalPlanned: 3,
      batchPhaseSummary: "Topology batch 1/2",
      completedOnce: false,
      message:
        "Detached child folders 0; moved normal notes 0; cleared relation note folders 0;",
      detachedChildFoldersFromIndexFolder: 0,
      updatedNormalNotesDetachedFromIndex: 0,
      updatedRelationNotesClearedFolder: 0,
      deletedObsoleteNotebookNameRootFolders: 0,
      notebookCountSlugScan: 0,
    }
    const migrated: AdminDataMigrationStatusDto = {
      migrationInProgress: false,
      moreBatchesRemain: false,
      completedBatchOrdinal: 3,
      batchTotalPlanned: 3,
      batchPhaseSummary: "Done",
      completedOnce: true,
      lastCompletedAt: "2026-04-01T08:30:12.345Z",
      message:
        "Detached child folders 1; moved normal notes 2; cleared relation note folders 0; slug regen scanned 3 notebooks.",
      detachedChildFoldersFromIndexFolder: 1,
      updatedNormalNotesDetachedFromIndex: 2,
      updatedRelationNotesClearedFolder: 0,
      deletedObsoleteNotebookNameRootFolders: 0,
      notebookCountSlugScan: 3,
    }

    let callIdx = 0
    runBatchSpy.mockImplementation(async () => {
      callIdx++
      if (callIdx === 1) {
        return wrapSdkResponse(midProgress)
      }
      return wrapSdkResponse(migrated)
    })

    const wrapper = helper.component(DataMigrationPanel).mount()
    await flushPromises()

    await wrapper
      .find('[data-testid="run-data-migration-button"]')
      .trigger("click")
    await flushPromises()

    expect(runBatchSpy).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain("slug regen scanned 3 notebooks")
    expect(
      wrapper.find('[data-testid="data-migration-progress"]').exists()
    ).toBe(false)
  })

  it("shows error when runDataMigrationBatch fails", async () => {
    vi.spyOn(
      AdminDataMigrationController,
      "runDataMigrationBatch"
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
