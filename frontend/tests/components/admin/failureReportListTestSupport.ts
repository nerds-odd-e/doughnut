import type {
  FailureReport,
  FailureReportsResponse,
} from "@generated/donut-backend-api"
import { FailureReportController } from "@generated/donut-backend-api/sdk.gen"
import FailureReportList from "@/components/admin/FailureReportList.vue"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { dummyRouteRecordsFromMetadata } from "@/routes/dummyRouteRecords"
import { createMemoryHistory, createRouter } from "vue-router"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"

export const failureReportListRouter = createRouter({
  history: createMemoryHistory(),
  routes: dummyRouteRecordsFromMetadata,
})

export function aFailureReport(id: number, errorName?: string) {
  const builder = makeMe.aFailureReport.withId(id)
  return errorName
    ? builder.withErrorName(errorName).please()
    : builder.please()
}

export function mockFailureReportsList(reports: FailureReport[]) {
  return mockSdkService(
    FailureReportController,
    "failureReports",
    reports as unknown as FailureReportsResponse
  )
}

export async function mountFailureReportList(reports: FailureReport[]) {
  mockFailureReportsList(reports)
  const wrapper = helper
    .component(FailureReportList)
    .withRouter(failureReportListRouter)
    .mount()
  await flushPromises()
  return wrapper
}

export function rowSelectEls(wrapper: VueWrapper) {
  return wrapper.findAll('[data-testid="failure-report-row-select"]')
}

export function deleteSelectedButton(wrapper: VueWrapper) {
  return wrapper.find('[data-testid="failure-report-delete-selected"]')
}

export function deleteConfirmButton(wrapper: VueWrapper) {
  return wrapper.find('[data-testid="failure-report-delete-confirm"]')
}

export function deleteCancelButton(wrapper: VueWrapper) {
  return wrapper.find('[data-testid="failure-report-delete-cancel"]')
}

export function deleteModalIsOpen(wrapper: VueWrapper) {
  return wrapper.find(".daisy-modal-open").exists()
}

export function triggerTestExceptionButton(wrapper: VueWrapper) {
  return wrapper
    .findAll("button")
    .find((btn) => btn.text().includes("Trigger Test Exception"))
}
