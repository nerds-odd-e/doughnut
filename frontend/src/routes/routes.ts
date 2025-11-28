import type { RouteLocation, RouteRecordRaw } from "vue-router"
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

// Please start most of the path with "/d/"
// so that the server will render the page correctly
// when refreshing the page or directly accessing the URL.
const routes = [
  { path: "/", name: "root", component: HomePage },
  {
    path: "/d/notebooks",
    name: "notebooks",
    component: NotebooksPage,
  },
  {
    path: `/n:noteId`,
    name: "noteShow",
    component: NoteShowPage,
    props: (route: RouteLocation) => ({ noteId: Number(route.params.noteId) }),
    meta: { useNoteStorageAccessor: true },
  },
  {
    path: "/d/circles/:circleId",
    name: "circleShow",
    component: CircleShowPage,
    props: true,
    meta: { useNoteStorageAccessor: true },
  },
  {
    path: "/d/bazaar",
    name: "bazaar",
    component: BazaarPage,
  },
  {
    path: "/d/assessmentAndCertificateHistory",
    name: "assessmentAndCertificateHistory",
    component: AssessmentAndCertificateHistoryPage,
  },
  {
    path: "/d/admin-dashboard",
    name: "adminDashboard",
    component: AdminDashboardPage,
    props: true,
  },
  {
    path: "/d/circles/join/:invitationCode?",
    name: "circleJoin",
    component: CircleJoinPage,
    props: true,
  },
  {
    path: "/d/message-center/:conversationId?",
    name: "messageCenter",
    component: MessageCenterPage,
    props: (route: RouteLocation) => ({
      conversationId: route.params.conversationId
        ? Number(route.params.conversationId)
        : undefined,
    }),
    meta: { useNoteStorageAccessor: true },
  },
  {
    path: "/d/assessment/notebook/:notebookId",
    name: "assessment",
    component: AssessmentPage,
    props: (route: RouteLocation) => ({
      notebookId: Number(route.params.notebookId),
    }),
  },
  {
    path: "/d/assimilate",
    name: "assimilate",
    component: AssimilationPage,
    meta: { useNoteStorageAccessor: true },
  },
  {
    path: "/d/recall",
    name: "recall",
    component: RecallPage,
    meta: { useNoteStorageAccessor: true },
  },
  {
    path: "/d/failure-report-list/show/:failureReportId",
    name: "failureReport",
    component: FailureReportPage,
    props: true,
  },
  {
    path: "/users/identify",
    name: "nonproductionOnlyLogin",
    component: NonproductionOnlyLoginPage,
  },
  {
    path: "/d/recent",
    name: "recent",
    component: RecentPage,
  },
  {
    path: "/d/circles",
    name: "circles",
    component: CirclesPage,
  },
  {
    path: "/d/generate-token",
    name: "manageMCPTokens",
    component: ManageMCPTokensPage,
  },
  {
    path: "/d/memory-trackers/:memoryTrackerId",
    name: "memoryTrackerShow",
    component: MemoryTrackerPage,
    props: (route: RouteLocation) => ({
      memoryTrackerId: Number(route.params.memoryTrackerId),
    }),
  },
] as RouteRecordRaw[]

export default routes
