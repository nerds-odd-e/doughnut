import { RouteLocation, RouteRecordRaw } from "vue-router";
import HomePage from "@/pages/HomePage.vue";
import BazaarPage from "@/pages/BazaarPage.vue";
import NotebooksPage from "@/pages/NotebooksPage.vue";
import NoteShowPage from "@/pages/NoteShowPage.vue";
import ReviewHome from "@/pages/ReviewHome.vue";
import RepeatPage from "@/pages/RepeatPage.vue";
import InitialReviewPage from "@/pages/InitialReviewPage.vue";
import CircleShowPage from "@/pages/CircleShowPage.vue";
import CircleJoinPage from "@/pages/CircleJoinPage.vue";
import FailureReportListPage from "@/pages/FailureReportListPage.vue";
import FailureReportPage from "@/pages/FailureReportPage.vue";
import AnsweredQuestionPage from "@/pages/AnsweredQuestionPage.vue";
import AdminDashboard from "@/pages/AdminDashboard.vue";
import NonproductionOnlyLoginPage from "@/pages/NonproductionOnlyLoginPage.vue";
import NestedPage from "../pages/commons/NestedPage";

const NestedInitialReviewPage = NestedPage(InitialReviewPage, "initial");

const NestedRepeatPage = NestedPage(RepeatPage, "repeat");

const noteAndLinkRoutes = [
  {
    path: "notebooks",
    name: "notebooks",
    component: NotebooksPage,
  },
  {
    path: `notes/:noteId`,
    name: "noteShow",
    component: NoteShowPage,
    props: (route: RouteLocation) => ({ noteId: Number(route.params.noteId) }),
    meta: { useNoteStorageAccessor: true, userProp: true },
  },

  {
    path: `answers/:answerId`,
    name: "answer",
    component: AnsweredQuestionPage,
    props: true,
    meta: { useNoteStorageAccessor: true },
  },
  {
    path: "circles/:circleId",
    name: "circleShow",
    component: CircleShowPage,
    props: true,
    meta: { useNoteStorageAccessor: true },
  },
];

const nestedNoteAndLinkRoutes = (prefix: string) =>
  noteAndLinkRoutes.map((route) => ({ ...route, name: prefix + route.name }));

const routes = [
  ...noteAndLinkRoutes.map((route) => ({ ...route, path: `/${route.path}` })),
  { path: "/", name: "root", component: HomePage, meta: { userProp: true } },
  {
    path: "/bazaar",
    name: "bazaar",
    component: BazaarPage,
    meta: { userProp: true },
  },
  {
    path: "/admin-dashboard",
    name: "adminDashboard",
    component: AdminDashboard,
    props: true,
  },
  {
    path: "/circles/join/:invitationCode?",
    name: "circleJoin",
    component: CircleJoinPage,
    props: true,
    meta: { userProp: true },
  },
  {
    path: "/bazaar/notes/:noteId",
    name: "bnoteShow",
    component: NoteShowPage,
    props: true,
  },
  { path: "/reviews", name: "reviews", component: ReviewHome },
  {
    path: "/reviews/initial",
    name: "initial",
    component: NestedInitialReviewPage,
    children: nestedNoteAndLinkRoutes("initial-"),
    meta: { useNoteStorageAccessor: true },
  },
  {
    path: "/reviews/repeat",
    name: "repeat",
    component: NestedRepeatPage,
    children: [...nestedNoteAndLinkRoutes("repeat-")],
    meta: { useNoteStorageAccessor: true },
  },
  {
    path: "/failure-report-list",
    name: "failureReportList",
    component: FailureReportListPage,
  },
  {
    path: "/failure-report-list/show/:failureReportId",
    name: "failureReport",
    component: FailureReportPage,
    props: true,
  },
  {
    path: "/users/identify",
    name: "nonproductionOnlyLogin",
    component: NonproductionOnlyLoginPage,
  },
] as RouteRecordRaw[];

export default routes;
