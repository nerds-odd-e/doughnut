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
    message: "stub message",
  }

  let runBatchSpy: ReturnType<typeof mockSdkService<"runDataMigrationBatch">>
  let getSpy: ReturnType<typeof mockSdkService<"getAdminDataMigrationStatus">>

  beforeEach(() => {
    getSpy = mockSdkService("getAdminDataMigrationStatus", idleDto)
    runBatchSpy = mockSdkService("runDataMigrationBatch", idleDto)
  })

  it("loads status when mounted", async () => {
    const wrapper = helper.component(DataMigrationPanel).mount()
    await flushPromises()

    expect(getSpy).toHaveBeenCalled()
    expect(wrapper.text()).toContain("stub message")
    expect(wrapper.text()).toContain("bounded batches")
  })

  it("clicking Run migration calls runDataMigrationBatch and updates summary", async () => {
    const reply: AdminDataMigrationStatusDto = {
      message: "after run",
      dataMigrationComplete: true,
    }
    runBatchSpy.mockResolvedValue(wrapSdkResponse(reply))

    const wrapper = helper.component(DataMigrationPanel).mount()
    await flushPromises()

    await wrapper
      .find('[data-testid="run-data-migration-button"]')
      .trigger("click")
    await flushPromises()

    expect(runBatchSpy).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain("after run")
  })

  it("continues batches until migration reports complete", async () => {
    const inProgress: AdminDataMigrationStatusDto = {
      message: "Step A: processed 1 item(s).",
      dataMigrationComplete: false,
      currentStepName: "example_step_one",
      stepStatus: "RUNNING",
      processedCount: 1,
      totalCount: 2,
    }
    const complete: AdminDataMigrationStatusDto = {
      message: "Migration is already complete.",
      dataMigrationComplete: true,
      stepStatus: "COMPLETED",
      processedCount: 3,
      totalCount: 3,
    }
    runBatchSpy.mockReset()
    runBatchSpy
      .mockResolvedValueOnce(wrapSdkResponse(inProgress))
      .mockResolvedValueOnce(wrapSdkResponse(complete))
      .mockResolvedValue(wrapSdkResponse(complete))

    const wrapper = helper.component(DataMigrationPanel).mount()
    await flushPromises()

    await wrapper
      .find('[data-testid="run-data-migration-button"]')
      .trigger("click")
    await flushPromises()

    expect(runBatchSpy).toHaveBeenCalledTimes(2)
    expect(
      wrapper.get('[data-testid="data-migration-counts"]').text()
    ).toContain("3 / 3")
    expect(wrapper.text()).toContain("Migration is already complete")
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
