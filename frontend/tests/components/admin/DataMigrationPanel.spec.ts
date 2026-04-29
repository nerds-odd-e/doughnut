import DataMigrationPanel from "@/components/admin/DataMigrationPanel.vue"
import { AdminDataMigrationController } from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, expect, it, vi } from "vitest"
import helper, { mockSdkService, wrapSdkError } from "@tests/helpers"

describe("DataMigrationPanel", () => {
  let runDataMigrationSpy: ReturnType<typeof mockSdkService<"runDataMigration">>

  beforeEach(() => {
    runDataMigrationSpy = mockSdkService("runDataMigration", undefined as never)
  })

  it("shows generalized intro about server-side batches and returning later", async () => {
    const wrapper = helper.component(DataMigrationPanel).mount()
    await flushPromises()

    expect(wrapper.text()).toContain("bounded batches on the server")
    expect(wrapper.text()).toContain("stored server-side")
    expect(wrapper.text()).not.toMatch(/slug/i)
  })

  it("shows idle status and last run placeholder", async () => {
    const wrapper = helper.component(DataMigrationPanel).mount()
    await flushPromises()

    expect(wrapper.text()).toContain("No migration job is running")
    expect(wrapper.text()).toContain("Last run:")
  })

  it("renders primary Run migration control", async () => {
    const wrapper = helper.component(DataMigrationPanel).mount()
    await flushPromises()

    const btn = wrapper.find("button.daisy-btn-primary")
    expect(btn.exists()).toBe(true)
    expect(btn.text()).toContain("Run migration")
  })

  it("clicking Run migration calls runDataMigration", async () => {
    const wrapper = helper.component(DataMigrationPanel).mount()
    await flushPromises()

    await wrapper.find("button.daisy-btn-primary").trigger("click")
    await flushPromises()

    expect(runDataMigrationSpy).toHaveBeenCalledTimes(1)
  })

  it("shows error when runDataMigration fails", async () => {
    vi.spyOn(
      AdminDataMigrationController,
      "runDataMigration"
    ).mockResolvedValue(wrapSdkError("Forbidden") as never)

    const wrapper = helper.component(DataMigrationPanel).mount()
    await flushPromises()

    await wrapper.find("button.daisy-btn-primary").trigger("click")
    await flushPromises()

    expect(wrapper.text()).toContain("Forbidden")
  })
})
