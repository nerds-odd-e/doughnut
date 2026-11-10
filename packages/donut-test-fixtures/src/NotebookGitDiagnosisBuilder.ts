import type { Report } from '@generated/donut-backend-api'
import Builder from './Builder'

export default class NotebookGitDiagnosisBuilder extends Builder<Report> {
  private report: Report = {
    acceptedHead: 'a'.repeat(40),
    acceptedTree: 'b'.repeat(40),
    liveTree: 'c'.repeat(40),
    differences: [],
  }

  matching() {
    this.report.liveTree = this.report.acceptedTree
    return this
  }

  difference(
    path: string,
    before: Uint8Array | null,
    after: Uint8Array | null
  ) {
    this.report.differences!.push({
      path,
      acceptedBlobId: before === null ? null : 'd'.repeat(40),
      liveBlobId: after === null ? null : 'e'.repeat(40),
      acceptedContent:
        before === null ? null : Buffer.from(before).toString('base64'),
      liveContent:
        after === null ? null : Buffer.from(after).toString('base64'),
    })
    return this
  }

  do(): Report {
    return this.report
  }
}
