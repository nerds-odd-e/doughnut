import { FailureReportController } from "@generated/donut-backend-api/sdk.gen"
import FailureReportPage from "@/pages/FailureReportPage.vue"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it } from "vitest"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"

function mountFailureReport(failureReportId: number) {
  return helper
    .component(FailureReportPage)
    .withRouter()
    .withProps({ failureReportId })
    .mount()
}

describe("FailureReportPage", () => {
  it("shows report id, error name, stack detail, and github issue link", async () => {
    const report = makeMe.aFailureReport
      .withId(42)
      .withErrorName("RuntimeException")
      .withErrorDetail("Stack trace here\nLine 2")
      .please()
    const reportForView = makeMe.aFailureReportForView
      .withFailureReport(report)
      .withGithubIssueUrl("https://github.com/test/repo/issues/123")
      .please()
    mockSdkService(FailureReportController, "showFailureReport", reportForView)

    const wrapper = mountFailureReport(42)
    await flushPromises()

    expect(wrapper.text()).toContain("#42")
    expect(wrapper.text()).toContain("RuntimeException")
    expect(wrapper.text()).toContain("Stack trace here")
    expect(wrapper.text()).toContain("Line 2")
    const link = wrapper.find('a[target="_blank"]')
    expect(link.attributes("href")).toBe(
      "https://github.com/test/repo/issues/123"
    )
    expect(link.text()).toContain("View GitHub Issue")
  })

  it("fetches data with the failure report id from the route", async () => {
    const spy = mockSdkService(
      FailureReportController,
      "showFailureReport",
      makeMe.aFailureReportForView.please()
    )

    mountFailureReport(42)
    await flushPromises()

    expect(spy).toHaveBeenCalledWith({
      path: { failureReport: 42 },
    })
  })

  it("formats create datetime in a readable form", async () => {
    const report = makeMe.aFailureReport
      .withCreateDatetime("2026-03-01T10:30:00Z")
      .please()
    mockSdkService(
      FailureReportController,
      "showFailureReport",
      makeMe.aFailureReportForView.withFailureReport(report).please()
    )

    const wrapper = mountFailureReport(1)
    await flushPromises()

    expect(wrapper.text()).toContain("Mar")
    expect(wrapper.text()).toContain("2026")
  })

  it("omits github issue link when unavailable", async () => {
    mockSdkService(
      FailureReportController,
      "showFailureReport",
      makeMe.aFailureReportForView.withoutGithubIssueUrl().please()
    )

    const wrapper = mountFailureReport(1)
    await flushPromises()

    expect(wrapper.find('a[target="_blank"]').exists()).toBe(false)
  })

  it("links back to the failure report list", async () => {
    mockSdkService(
      FailureReportController,
      "showFailureReport",
      makeMe.aFailureReportForView.please()
    )

    const wrapper = mountFailureReport(1)
    await flushPromises()

    expect(wrapper.find(".router-link").text()).toContain("Back to List")
  })

  it("preserves whitespace in error details", async () => {
    const report = makeMe.aFailureReport
      .withErrorDetail("Line 1\nLine 2\n  Indented line")
      .please()
    mockSdkService(
      FailureReportController,
      "showFailureReport",
      makeMe.aFailureReportForView.withFailureReport(report).please()
    )

    const wrapper = mountFailureReport(1)
    await flushPromises()

    const preElement = wrapper.find("pre")
    expect(preElement.text()).toContain("Line 1")
    expect(preElement.text()).toContain("Line 2")
    expect(preElement.text()).toContain("Indented line")
  })
})
