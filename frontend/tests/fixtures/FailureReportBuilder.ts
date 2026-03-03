import type { FailureReport, FailureReportForView } from "@generated/backend"
import Builder from "./Builder"

class FailureReportBuilder extends Builder<FailureReport> {
  private report: FailureReport

  constructor() {
    super()
    const id = Math.floor(Math.random() * 10000)
    this.report = {
      id,
      errorName: "RuntimeException",
      errorDetail:
        "Error occurred at com.example.service.Service.process(Service.java:42)\nCaused by: NullPointerException",
      issueNumber: 123,
      createDatetime: new Date().toISOString(),
    }
  }

  withId(id: number): this {
    this.report.id = id
    return this
  }

  withErrorName(errorName: string): this {
    this.report.errorName = errorName
    return this
  }

  withErrorDetail(errorDetail: string): this {
    this.report.errorDetail = errorDetail
    return this
  }

  withIssueNumber(issueNumber: number): this {
    this.report.issueNumber = issueNumber
    return this
  }

  withoutIssueNumber(): this {
    this.report.issueNumber = undefined
    return this
  }

  withCreateDatetime(datetime: string): this {
    this.report.createDatetime = datetime
    return this
  }

  do(): FailureReport {
    return { ...this.report }
  }
}

class FailureReportForViewBuilder extends Builder<FailureReportForView> {
  private reportForView: FailureReportForView

  constructor() {
    super()
    const reportBuilder = new FailureReportBuilder()
    this.reportForView = {
      failureReport: reportBuilder.please(),
      githubIssueUrl: "https://github.com/test/repo/issues/123",
    }
  }

  withFailureReport(report: FailureReport): this {
    this.reportForView.failureReport = report
    return this
  }

  withGithubIssueUrl(url: string): this {
    this.reportForView.githubIssueUrl = url
    return this
  }

  withoutGithubIssueUrl(): this {
    this.reportForView.githubIssueUrl = undefined
    return this
  }

  do(): FailureReportForView {
    return { ...this.reportForView }
  }
}

export { FailureReportBuilder, FailureReportForViewBuilder }
