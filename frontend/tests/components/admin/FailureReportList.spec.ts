import { FailureReportController } from "@generated/doughnut-backend-api/sdk.gen"
import FailureReportList from "@/components/admin/FailureReportList.vue"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import {
  aFailureReport,
  deleteCancelButton,
  deleteConfirmButton,
  deleteModalIsOpen,
  deleteSelectedButton,
  failureReportListRouter,
  mockFailureReportsList,
  mountFailureReportList,
  openDeleteModalForFirstReports,
  rowSelectEls,
  triggerTestExceptionButton,
  wrapSdkResponse,
} from "./failureReportListTestSupport"

describe("FailureReportList", () => {
  describe("displaying failure reports", () => {
    it("shows failure reports with count badge and ids", async () => {
      const wrapper = await mountFailureReportList([
        aFailureReport(1, "RuntimeException"),
        aFailureReport(2, "NullPointerException"),
      ])

      expect(wrapper.text()).toContain("RuntimeException")
      expect(wrapper.text()).toContain("NullPointerException")
      expect(wrapper.find(".daisy-badge-error").text()).toBe("2")
      expect(wrapper.text()).toContain("#1")
      expect(wrapper.text()).toContain("#2")
    })

    it("shows empty state when no failure reports exist", async () => {
      const wrapper = await mountFailureReportList([])

      expect(wrapper.text()).toContain("All Clear!")
      expect(wrapper.text()).toContain("No failure reports found.")
    })

    it("formats date time in readable format", async () => {
      const report = makeMe.aFailureReport
        .withId(1)
        .withCreateDatetime("2026-03-01T10:30:00Z")
        .please()
      const wrapper = await mountFailureReportList([report])

      expect(wrapper.text()).toContain("Mar")
      expect(wrapper.text()).toContain("2026")
    })
  })

  describe("selecting and deleting reports", () => {
    it.each([1, 2])(
      "shows delete button when %i report(s) are selected",
      async (selectedCount) => {
        const wrapper = await mountFailureReportList([
          aFailureReport(1),
          aFailureReport(2),
        ])

        expect(deleteSelectedButton(wrapper).exists()).toBe(false)

        const rowChecks = rowSelectEls(wrapper)
        for (let i = 0; i < selectedCount; i++) {
          await rowChecks[i]!.setValue(true)
        }
        await flushPromises()

        expect(deleteSelectedButton(wrapper).text()).toContain(
          `Delete Selected (${selectedCount})`
        )
      }
    )

    it("closes delete confirmation modal when cancel is clicked", async () => {
      const wrapper = await mountFailureReportList([aFailureReport(1)])
      await openDeleteModalForFirstReports(wrapper, 1)

      expect(deleteModalIsOpen(wrapper)).toBe(true)
      expect(wrapper.text()).toContain("Confirm Deletion")
      expect(wrapper.text()).toContain("This action cannot be undone")

      await deleteCancelButton(wrapper).trigger("click")
      await flushPromises()

      expect(deleteModalIsOpen(wrapper)).toBe(false)
    })

    it("deletes selected reports when confirmed", async () => {
      const report = aFailureReport(1)
      const failureReportsSpy = mockFailureReportsList([report])
      const deleteSpy = mockSdkService(
        FailureReportController,
        "deleteFailureReports",
        undefined
      )

      const wrapper = await mountFailureReportList([report])
      await openDeleteModalForFirstReports(wrapper, 1)

      failureReportsSpy.mockResolvedValue(wrapSdkResponse([]))

      await deleteConfirmButton(wrapper).trigger("click")
      await flushPromises()

      expect(deleteSpy).toHaveBeenCalledWith({
        body: [1],
      })
    })
  })

  describe("trigger test exception", () => {
    it("calls triggerFailure API and refreshes list when clicked", async () => {
      mockFailureReportsList([])
      const triggerSpy = mockSdkService(
        FailureReportController,
        "triggerFailure",
        undefined
      )

      const wrapper = await mountFailureReportList([])
      const triggerButton = triggerTestExceptionButton(wrapper)
      expect(triggerButton?.exists()).toBe(true)

      await triggerButton!.trigger("click")
      await flushPromises()

      expect(triggerSpy).toHaveBeenCalled()
    })
  })

  describe("error handling", () => {
    it("displays error alert when API returns an error", async () => {
      const spy = mockFailureReportsList([])
      spy.mockResolvedValue({
        data: undefined,
        error: { message: "Access denied" },
        request: {} as Request,
        response: {} as Response,
      })

      const wrapper = helper
        .component(FailureReportList)
        .withRouter(failureReportListRouter)
        .mount()
      await flushPromises()

      expect(wrapper.find(".daisy-alert-error").exists()).toBe(true)
    })
  })
})
