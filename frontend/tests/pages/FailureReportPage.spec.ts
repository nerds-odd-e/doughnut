import FailureReportPage from "@/pages/FailureReportPage.vue"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"

describe("FailureReportPage", () => {
  describe("displaying failure report details", () => {
    it("shows the failure report details", async () => {
      const report = makeMe.aFailureReport
        .withId(1)
        .withErrorName("RuntimeException")
        .withErrorDetail("Stack trace here\nLine 2")
        .please()
      const reportForView = makeMe.aFailureReportForView
        .withFailureReport(report)
        .withGithubIssueUrl("https://github.com/test/repo/issues/123")
        .please()
      mockSdkService("showFailureReport", reportForView)

      const wrapper = helper
        .component(FailureReportPage)
        .withRouter()
        .withProps({ failureReportId: 1 })
        .mount()
      await flushPromises()

      expect(wrapper.text()).toContain("RuntimeException")
      expect(wrapper.text()).toContain("Stack trace here")
      expect(wrapper.text()).toContain("Line 2")
    })

    it("fetches data with correct parameters", async () => {
      const reportForView = makeMe.aFailureReportForView.please()
      const spy = mockSdkService("showFailureReport", reportForView)

      helper
        .component(FailureReportPage)
        .withRouter()
        .withProps({ failureReportId: 42 })
        .mount()
      await flushPromises()

      expect(spy).toHaveBeenCalledWith({
        path: { failureReport: 42 },
      })
    })

    it("shows the report ID", async () => {
      const report = makeMe.aFailureReport.withId(42).please()
      const reportForView = makeMe.aFailureReportForView
        .withFailureReport(report)
        .please()
      mockSdkService("showFailureReport", reportForView)

      const wrapper = helper
        .component(FailureReportPage)
        .withRouter()
        .withProps({ failureReportId: 42 })
        .mount()
      await flushPromises()

      expect(wrapper.text()).toContain("#42")
    })

    it("formats datetime in readable format", async () => {
      const report = makeMe.aFailureReport
        .withCreateDatetime("2026-03-01T10:30:00Z")
        .please()
      const reportForView = makeMe.aFailureReportForView
        .withFailureReport(report)
        .please()
      mockSdkService("showFailureReport", reportForView)

      const wrapper = helper
        .component(FailureReportPage)
        .withRouter()
        .withProps({ failureReportId: 1 })
        .mount()
      await flushPromises()

      expect(wrapper.text()).toContain("Mar")
      expect(wrapper.text()).toContain("2026")
    })
  })

  describe("github issue link", () => {
    it("shows github issue link when available", async () => {
      const reportForView = makeMe.aFailureReportForView
        .withGithubIssueUrl("https://github.com/test/repo/issues/456")
        .please()
      mockSdkService("showFailureReport", reportForView)

      const wrapper = helper
        .component(FailureReportPage)
        .withRouter()
        .withProps({ failureReportId: 1 })
        .mount()
      await flushPromises()

      const link = wrapper.find('a[target="_blank"]')
      expect(link.exists()).toBe(true)
      expect(link.attributes("href")).toBe(
        "https://github.com/test/repo/issues/456"
      )
      expect(link.text()).toContain("View GitHub Issue")
    })

    it("does not show github issue link when not available", async () => {
      const reportForView = makeMe.aFailureReportForView
        .withoutGithubIssueUrl()
        .please()
      mockSdkService("showFailureReport", reportForView)

      const wrapper = helper
        .component(FailureReportPage)
        .withRouter()
        .withProps({ failureReportId: 1 })
        .mount()
      await flushPromises()

      expect(wrapper.find('a[target="_blank"]').exists()).toBe(false)
    })
  })

  describe("navigation", () => {
    it("has back to list link", async () => {
      const reportForView = makeMe.aFailureReportForView.please()
      mockSdkService("showFailureReport", reportForView)

      const wrapper = helper
        .component(FailureReportPage)
        .withRouter()
        .withProps({ failureReportId: 1 })
        .mount()
      await flushPromises()

      const backLink = wrapper.find(".router-link")
      expect(backLink.exists()).toBe(true)
      expect(backLink.text()).toContain("Back to List")
    })
  })

  describe("error detail display", () => {
    it("shows error details in collapsible section", async () => {
      const report = makeMe.aFailureReport
        .withErrorDetail("Detailed stack trace information")
        .please()
      const reportForView = makeMe.aFailureReportForView
        .withFailureReport(report)
        .please()
      mockSdkService("showFailureReport", reportForView)

      const wrapper = helper
        .component(FailureReportPage)
        .withRouter()
        .withProps({ failureReportId: 1 })
        .mount()
      await flushPromises()

      expect(wrapper.find(".daisy-collapse").exists()).toBe(true)
      expect(wrapper.text()).toContain("Error Details")
      expect(wrapper.text()).toContain("Detailed stack trace information")
    })

    it("preserves whitespace in error details", async () => {
      const report = makeMe.aFailureReport
        .withErrorDetail("Line 1\nLine 2\n  Indented line")
        .please()
      const reportForView = makeMe.aFailureReportForView
        .withFailureReport(report)
        .please()
      mockSdkService("showFailureReport", reportForView)

      const wrapper = helper
        .component(FailureReportPage)
        .withRouter()
        .withProps({ failureReportId: 1 })
        .mount()
      await flushPromises()

      const preElement = wrapper.find("pre")
      expect(preElement.exists()).toBe(true)
      expect(preElement.text()).toContain("Line 1")
      expect(preElement.text()).toContain("Line 2")
      expect(preElement.text()).toContain("Indented line")
    })
  })
})
