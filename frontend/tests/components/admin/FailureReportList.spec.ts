import FailureReportList from "@/components/admin/FailureReportList.vue"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService, wrapSdkResponse } from "@tests/helpers"

describe("FailureReportList", () => {
  describe("displaying failure reports", () => {
    it("shows the list of failure reports", async () => {
      const report1 = makeMe.aFailureReport
        .withId(1)
        .withErrorName("RuntimeException")
        .please()
      const report2 = makeMe.aFailureReport
        .withId(2)
        .withErrorName("NullPointerException")
        .please()
      mockSdkService("failureReports", [report1, report2])

      const wrapper = helper.component(FailureReportList).withRouter().mount()
      await flushPromises()

      expect(wrapper.text()).toContain("RuntimeException")
      expect(wrapper.text()).toContain("NullPointerException")
    })

    it("shows badge with count when there are failure reports", async () => {
      const reports = [
        makeMe.aFailureReport.withId(1).please(),
        makeMe.aFailureReport.withId(2).please(),
      ]
      mockSdkService("failureReports", reports)

      const wrapper = helper.component(FailureReportList).withRouter().mount()
      await flushPromises()

      expect(wrapper.find(".daisy-badge-error").text()).toBe("2")
    })

    it("shows empty state when no failure reports exist", async () => {
      mockSdkService("failureReports", [])

      const wrapper = helper.component(FailureReportList).withRouter().mount()
      await flushPromises()

      expect(wrapper.text()).toContain("All Clear!")
      expect(wrapper.text()).toContain("No failure reports found.")
    })

    it("formats date time in readable format", async () => {
      const report = makeMe.aFailureReport
        .withId(1)
        .withCreateDatetime("2026-03-01T10:30:00Z")
        .please()
      mockSdkService("failureReports", [report])

      const wrapper = helper.component(FailureReportList).withRouter().mount()
      await flushPromises()

      expect(wrapper.text()).toContain("Mar")
      expect(wrapper.text()).toContain("2026")
    })

    it("shows report ID badge", async () => {
      const report = makeMe.aFailureReport.withId(42).please()
      mockSdkService("failureReports", [report])

      const wrapper = helper.component(FailureReportList).withRouter().mount()
      await flushPromises()

      expect(wrapper.text()).toContain("#42")
    })
  })

  describe("selecting and deleting reports", () => {
    it("shows delete button when reports are selected", async () => {
      const report = makeMe.aFailureReport.withId(1).please()
      mockSdkService("failureReports", [report])

      const wrapper = helper.component(FailureReportList).withRouter().mount()
      await flushPromises()

      const deleteButtonInHeader = wrapper
        .findAll(".daisy-btn-error")
        .find((btn) => btn.text().includes("Delete Selected"))
      expect(deleteButtonInHeader).toBeUndefined()

      const checkbox = wrapper.find('input[type="checkbox"]')
      await checkbox.setValue(true)
      await flushPromises()

      const deleteButton = wrapper
        .findAll(".daisy-btn-error")
        .find((btn) => btn.text().includes("Delete Selected"))
      expect(deleteButton?.text()).toContain("Delete Selected (1)")
    })

    it("opens confirmation modal when delete button is clicked", async () => {
      const report = makeMe.aFailureReport.withId(1).please()
      mockSdkService("failureReports", [report])

      const wrapper = helper.component(FailureReportList).withRouter().mount()
      await flushPromises()

      const checkbox = wrapper.find('input[type="checkbox"]')
      await checkbox.setValue(true)
      await flushPromises()

      await wrapper.find(".daisy-btn-error").trigger("click")
      await flushPromises()

      expect(wrapper.find(".daisy-modal-open").exists()).toBe(true)
      expect(wrapper.text()).toContain("Confirm Deletion")
      expect(wrapper.text()).toContain("This action cannot be undone")
    })

    it("deletes selected reports when confirmed", async () => {
      const report = makeMe.aFailureReport.withId(1).please()
      const failureReportsSpy = mockSdkService("failureReports", [report])
      const deleteSpy = mockSdkService("deleteFailureReports", undefined)

      const wrapper = helper.component(FailureReportList).withRouter().mount()
      await flushPromises()

      const checkbox = wrapper.find('input[type="checkbox"]')
      await checkbox.setValue(true)
      await flushPromises()

      await wrapper.find(".daisy-btn-error").trigger("click")
      await flushPromises()

      failureReportsSpy.mockResolvedValue(wrapSdkResponse([]))

      const confirmButton = wrapper
        .findAll(".daisy-modal-action button")
        .find((b) => b.text() === "Delete")
      await confirmButton!.trigger("click")
      await flushPromises()

      expect(deleteSpy).toHaveBeenCalledWith({
        body: [1],
      })
    })

    it("closes modal when cancel is clicked", async () => {
      const report = makeMe.aFailureReport.withId(1).please()
      mockSdkService("failureReports", [report])

      const wrapper = helper.component(FailureReportList).withRouter().mount()
      await flushPromises()

      const checkbox = wrapper.find('input[type="checkbox"]')
      await checkbox.setValue(true)
      await wrapper.find(".daisy-btn-error").trigger("click")
      await flushPromises()

      expect(wrapper.find(".daisy-modal-open").exists()).toBe(true)

      const cancelButton = wrapper
        .findAll(".daisy-modal-action button")
        .find((b) => b.text() === "Cancel")
      await cancelButton!.trigger("click")
      await flushPromises()

      expect(wrapper.find(".daisy-modal-open").exists()).toBe(false)
    })

    it("allows selecting multiple reports", async () => {
      const report1 = makeMe.aFailureReport.withId(1).please()
      const report2 = makeMe.aFailureReport.withId(2).please()
      mockSdkService("failureReports", [report1, report2])

      const wrapper = helper.component(FailureReportList).withRouter().mount()
      await flushPromises()

      const checkboxes = wrapper.findAll('input[type="checkbox"]')
      await checkboxes[0]!.setValue(true)
      await checkboxes[1]!.setValue(true)
      await flushPromises()

      expect(wrapper.find(".daisy-btn-error").text()).toContain(
        "Delete Selected (2)"
      )
    })
  })

  describe("trigger test exception", () => {
    it("shows trigger button", async () => {
      mockSdkService("failureReports", [])

      const wrapper = helper.component(FailureReportList).withRouter().mount()
      await flushPromises()

      const triggerButton = wrapper
        .findAll("button")
        .find((btn) => btn.text().includes("Trigger Test Exception"))
      expect(triggerButton?.exists()).toBe(true)
    })

    it("calls triggerFailure API and refreshes list when clicked", async () => {
      mockSdkService("failureReports", [])
      const triggerSpy = mockSdkService("triggerFailure", undefined)

      const wrapper = helper.component(FailureReportList).withRouter().mount()
      await flushPromises()

      const triggerButton = wrapper
        .findAll("button")
        .find((btn) => btn.text().includes("Trigger Test Exception"))
      await triggerButton!.trigger("click")
      await flushPromises()

      expect(triggerSpy).toHaveBeenCalled()
    })
  })

  describe("error handling", () => {
    it("displays error alert when API returns an error", async () => {
      mockSdkService("failureReports", [])
      const spy = mockSdkService("failureReports", [])
      spy.mockResolvedValue({
        data: undefined,
        error: { message: "Access denied" },
        request: {} as Request,
        response: {} as Response,
      })

      const wrapper = helper.component(FailureReportList).withRouter().mount()
      await flushPromises()

      expect(wrapper.find(".daisy-alert-error").exists()).toBe(true)
    })
  })
})
