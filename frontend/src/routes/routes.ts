import type { RouteRecordRaw } from "vue-router"
import HomePage from "@/pages/HomePage.vue"
import BazaarPage from "@/pages/BazaarPage.vue"
import NotebooksPage from "@/pages/NotebooksPage.vue"
import NoteShowPage from "@/pages/NoteShowPage.vue"
import AssessmentAndCertificateHistoryPage from "@/pages/AssessmentAndCertificateHistoryPage.vue"
import AssessmentPage from "@/pages/AssessmentPage.vue"
import RecallPage from "@/pages/RecallPage.vue"
import AssimilationPage from "@/pages/AssimilationPage.vue"
import CircleShowPage from "@/pages/CircleShowPage.vue"
import CircleJoinPage from "@/pages/CircleJoinPage.vue"
import FailureReportPage from "@/pages/FailureReportPage.vue"
import AdminDashboardPage from "@/pages/AdminDashboardPage.vue"
import NonproductionOnlyLoginPage from "@/pages/NonproductionOnlyLoginPage.vue"
import MessageCenterPage from "../pages/MessageCenterPage.vue"
import RecentPage from "@/pages/RecentPage.vue"
import CirclesPage from "@/pages/CirclesPage.vue"
import ManageMCPTokensPage from "@/pages/ManageMCPTokensPage.vue"
import MemoryTrackerPage from "@/pages/MemoryTrackerPage.vue"
import NotebookPage from "@/pages/NotebookPage.vue"
import { routeMetadata } from "./routeMetadata"

// Please start most of the path with "/d/"
// so that the server will render the page correctly
// when refreshing the page or directly accessing the URL.

// Map route names to components
const componentMap: Record<string, unknown> = {
  root: HomePage,
  notebooks: NotebooksPage,
  noteShow: NoteShowPage,
  circleShow: CircleShowPage,
  bazaar: BazaarPage,
  assessmentAndCertificateHistory: AssessmentAndCertificateHistoryPage,
  adminDashboard: AdminDashboardPage,
  circleJoin: CircleJoinPage,
  messageCenter: MessageCenterPage,
  assessment: AssessmentPage,
  assimilate: AssimilationPage,
  recall: RecallPage,
  failureReport: FailureReportPage,
  nonproductionOnlyLogin: NonproductionOnlyLoginPage,
  recent: RecentPage,
  circles: CirclesPage,
  manageMCPTokens: ManageMCPTokensPage,
  memoryTrackerShow: MemoryTrackerPage,
  notebookEdit: NotebookPage,
}

// Combine route metadata with components
const routes: RouteRecordRaw[] = routeMetadata.map((metadata) => ({
  ...metadata,
  component: componentMap[metadata.name],
})) as RouteRecordRaw[]

export default routes
