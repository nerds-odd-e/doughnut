import type { RouteLocation, RouteRecordRaw } from "vue-router"
import HomePage from "@/pages/HomePage.vue"
import BazaarPage from "@/pages/BazaarPage.vue"
import NotebooksPage from "@/pages/NotebooksPage.vue"
import NoteShowPage from "@/pages/NoteShowPage.vue"
import AssessmentAndCertificateHistoryPage from "@/pages/AssessmentAndCertificateHistoryPage.vue"
import AssessmentPage from "@/pages/AssessmentPage.vue"
import ReviewHome from "@/pages/ReviewHome.vue"
import RepeatPage from "@/pages/RepeatPage.vue"
import InitialReviewPage from "@/pages/InitialReviewPage.vue"
import CircleShowPage from "@/pages/CircleShowPage.vue"
import CircleJoinPage from "@/pages/CircleJoinPage.vue"
import FailureReportPage from "@/pages/FailureReportPage.vue"
import AnsweredQuestionPage from "@/pages/AnsweredQuestionPage.vue"
import AdminDashboardPage from "@/pages/AdminDashboardPage.vue"
import NonproductionOnlyLoginPage from "@/pages/NonproductionOnlyLoginPage.vue"
import NestedPage from "../pages/commons/NestedPage"
import MessageCenterPage from "../pages/MessageCenterPage.vue"

const NestedInitialReviewPage = NestedPage(InitialReviewPage, "initial")
const NestedRepeatPage = NestedPage(RepeatPage, "repeat")

// Please start most of the path with "/d/"
// so that the server will render the page correctly
// when refreshing the page or directly accessing the URL.

const noteAndLinkRoutes = [
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
    path: `/d/answers/:reviewQuestionInstanceId`,
    name: "answer",
    component: AnsweredQuestionPage,
    props: true,
    meta: { useNoteStorageAccessor: true },
  },
  {
    path: "/d/circles/:circleId",
    name: "circleShow",
    component: CircleShowPage,
    props: true,
    meta: { useNoteStorageAccessor: true },
  },
]

const nestedNoteAndLinkRoutes = (prefix: string) =>
  noteAndLinkRoutes.map((route) => ({ ...route, name: prefix + route.name }))

const routes = [
  ...noteAndLinkRoutes.map((route) => ({ ...route, path: `${route.path}` })),
  { path: "/", name: "root", component: HomePage },
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
  { path: "/d/reviews", name: "reviews", component: ReviewHome },
  {
    path: "/d/reviews/initial",
    name: "initial",
    component: NestedInitialReviewPage,
    children: nestedNoteAndLinkRoutes("initial-"),
    meta: { useNoteStorageAccessor: true },
  },
  {
    path: "/d/reviews/repeat",
    name: "repeat",
    component: NestedRepeatPage,
    children: [...nestedNoteAndLinkRoutes("repeat-")],
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
] as RouteRecordRaw[]

export default routes
