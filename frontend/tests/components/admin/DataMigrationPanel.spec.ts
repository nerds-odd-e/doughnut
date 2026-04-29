import DataMigrationPanel from "@/components/admin/DataMigrationPanel.vue"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it } from "vitest"
import helper from "@tests/helpers"

describe("DataMigrationPanel", () => {
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

  it("clicking Run migration completes without error", async () => {
    const wrapper = helper.component(DataMigrationPanel).mount()
    await flushPromises()

    await wrapper.find("button.daisy-btn-primary").trigger("click")
    await flushPromises()

    expect(wrapper.find("button.daisy-btn-primary").exists()).toBe(true)
  })
})
