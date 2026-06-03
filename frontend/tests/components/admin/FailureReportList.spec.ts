import type {
  FailureReport,
  FailureReportsResponse,
} from "@generated/doughnut-backend-api"
import { FailureReportController } from "@generated/doughnut-backend-api/sdk.gen"
import FailureReportList from "@/components/admin/FailureReportList.vue"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { describe, expect, it } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService, wrapSdkResponse } from "@tests/helpers"

function mockFailureReportsList(reports: FailureReport[]) {
  return mockSdkService(
    FailureReportController,
    "failureReports",
    reports as unknown as FailureReportsResponse
  )
}

async function mountWithReports(reports: FailureReport[]) {
  mockFailureReportsList(reports)
  const wrapper = helper.component(FailureReportList).withRouter().mount()
  await flushPromises()
  return wrapper
}

async function openDeleteModalForFirstReports(
  wrapper: VueWrapper,
  count: number
) {
  const rowChecks = wrapper.findAll('[data-testid="failure-report-row-select"]')
  for (let i = 0; i < count; i++) {
    await rowChecks[i]!.setValue(true)
  }
  await wrapper
    .find('[data-testid="failure-report-delete-selected"]')
    .trigger("click")
  await flushPromises()
}

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
      const wrapper = await mountWithReports([report1, report2])

      expect(wrapper.text()).toContain("RuntimeException")
      expect(wrapper.text()).toContain("NullPointerException")
    })

    it("shows badge with count when there are failure reports", async () => {
      const reports = [
        makeMe.aFailureReport.withId(1).please(),
        makeMe.aFailureReport.withId(2).please(),
      ]
      const wrapper = await mountWithReports(reports)

      expect(wrapper.find(".daisy-badge-error").text()).toBe("2")
    })

    it("shows empty state when no failure reports exist", async () => {
      const wrapper = await mountWithReports([])

      expect(wrapper.text()).toContain("All Clear!")
      expect(wrapper.text()).toContain("No failure reports found.")
    })

    it("formats date time in readable format", async () => {
      const report = makeMe.aFailureReport
        .withId(1)
        .withCreateDatetime("2026-03-01T10:30:00Z")
        .please()
      const wrapper = await mountWithReports([report])

      expect(wrapper.text()).toContain("Mar")
      expect(wrapper.text()).toContain("2026")
    })

    it("shows report ID badge", async () => {
      const report = makeMe.aFailureReport.withId(42).please()
      const wrapper = await mountWithReports([report])

      expect(wrapper.text()).toContain("#42")
    })
  })

  describe("selecting and deleting reports", () => {
    it("shows delete button when reports are selected", async () => {
      const report = makeMe.aFailureReport.withId(1).please()
      const wrapper = await mountWithReports([report])

      expect(
        wrapper.find('[data-testid="failure-report-delete-selected"]').exists()
      ).toBe(false)

      await wrapper
        .find('[data-testid="failure-report-row-select"]')
        .setValue(true)
      await flushPromises()

      expect(
        wrapper.find('[data-testid="failure-report-delete-selected"]').text()
      ).toContain("Delete Selected (1)")
    })

    it("opens confirmation modal when delete button is clicked", async () => {
      const report = makeMe.aFailureReport.withId(1).please()
      const wrapper = await mountWithReports([report])
      await openDeleteModalForFirstReports(wrapper, 1)

      expect(wrapper.find(".daisy-modal-open").exists()).toBe(true)
      expect(wrapper.text()).toContain("Confirm Deletion")
      expect(wrapper.text()).toContain("This action cannot be undone")
    })

    it("deletes selected reports when confirmed", async () => {
      const report = makeMe.aFailureReport.withId(1).please()
      const failureReportsSpy = mockFailureReportsList([report])
      const deleteSpy = mockSdkService(
        FailureReportController,
        "deleteFailureReports",
        undefined
      )

      const wrapper = await mountWithReports([report])
      await openDeleteModalForFirstReports(wrapper, 1)

      failureReportsSpy.mockResolvedValue(wrapSdkResponse([]))

      await wrapper
        .find('[data-testid="failure-report-delete-confirm"]')
        .trigger("click")
      await flushPromises()

      expect(deleteSpy).toHaveBeenCalledWith({
        body: [1],
      })
    })

    it("closes modal when cancel is clicked", async () => {
      const report = makeMe.aFailureReport.withId(1).please()
      const wrapper = await mountWithReports([report])
      await openDeleteModalForFirstReports(wrapper, 1)

      expect(wrapper.find(".daisy-modal-open").exists()).toBe(true)

      await wrapper
        .find('[data-testid="failure-report-delete-cancel"]')
        .trigger("click")
      await flushPromises()

      expect(wrapper.find(".daisy-modal-open").exists()).toBe(false)
    })

    it("allows selecting multiple reports", async () => {
      const report1 = makeMe.aFailureReport.withId(1).please()
      const report2 = makeMe.aFailureReport.withId(2).please()
      const wrapper = await mountWithReports([report1, report2])

      const rowChecks = wrapper.findAll(
        '[data-testid="failure-report-row-select"]'
      )
      await rowChecks[0]!.setValue(true)
      await rowChecks[1]!.setValue(true)
      await flushPromises()

      expect(
        wrapper.find('[data-testid="failure-report-delete-selected"]').text()
      ).toContain("Delete Selected (2)")
    })
  })

  describe("trigger test exception", () => {
    it("shows trigger button", async () => {
      const wrapper = await mountWithReports([])

      const triggerButton = wrapper
        .findAll("button")
        .find((btn) => btn.text().includes("Trigger Test Exception"))
      expect(triggerButton?.exists()).toBe(true)
    })

    it("calls triggerFailure API and refreshes list when clicked", async () => {
      mockFailureReportsList([])
      const triggerSpy = mockSdkService(
        FailureReportController,
        "triggerFailure",
        undefined
      )

      const wrapper = await mountWithReports([])
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
      const spy = mockFailureReportsList([])
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
