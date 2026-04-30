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
    expect(wrapper.text()).toContain("Admin data migrations")
  })

  it("clicking Run migration calls runDataMigrationBatch and updates summary", async () => {
    const reply: AdminDataMigrationStatusDto = { message: "after run" }
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
