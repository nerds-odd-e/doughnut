import type { RouteLocation } from "vue-router"

// Route metadata definitions (path, name, props, meta) without component imports
// This allows Storybook to import route definitions without pulling in page components
export interface RouteMetadata {
  path: string
  name: string
  props?: boolean | ((route: RouteLocation) => Record<string, unknown>)
  meta?: Record<string, unknown>
}

export const routeMetadata: RouteMetadata[] = [
  { path: "/", name: "root" },
  {
    path: "/d/notebooks",
    name: "notebooks",
  },
  {
    path: "/d/notebooks/:notebookId/edit",
    name: "notebookEdit",
    props: (route: RouteLocation) => ({
      notebookId: Number(route.params.notebookId),
    }),
  },
  {
    path: "/n:noteId",
    name: "noteShow",
    props: (route: RouteLocation) => ({ noteId: Number(route.params.noteId) }),
    meta: { useNoteStorageAccessor: true },
  },
  {
    path: "/d/circles/:circleId",
    name: "circleShow",
    props: true,
    meta: { useNoteStorageAccessor: true },
  },
  {
    path: "/d/bazaar",
    name: "bazaar",
  },
  {
    path: "/d/assessmentAndCertificateHistory",
    name: "assessmentAndCertificateHistory",
  },
  {
    path: "/d/admin-dashboard",
    name: "adminDashboard",
    props: true,
  },
  {
    path: "/d/circles/join/:invitationCode?",
    name: "circleJoin",
    props: true,
  },
  {
    path: "/d/message-center/:conversationId?",
    name: "messageCenter",
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
    props: (route: RouteLocation) => ({
      notebookId: Number(route.params.notebookId),
    }),
  },
  {
    path: "/d/assimilate",
    name: "assimilate",
    meta: { useNoteStorageAccessor: true },
  },
  {
    path: "/d/assimilate/:noteId",
    name: "assimilateSingleNote",
    props: (route: RouteLocation) => ({ noteId: Number(route.params.noteId) }),
    meta: { useNoteStorageAccessor: true },
  },
  {
    path: "/d/recall",
    name: "recall",
    meta: { useNoteStorageAccessor: true },
  },
  {
    path: "/d/failure-report-list/show/:failureReportId",
    name: "failureReport",
    props: true,
  },
  {
    path: "/users/identify",
    name: "nonproductionOnlyLogin",
  },
  {
    path: "/d/recent",
    name: "recent",
  },
  {
    path: "/d/circles",
    name: "circles",
  },
  {
    path: "/d/generate-token",
    name: "manageMCPTokens",
  },
  {
    path: "/d/memory-trackers/:memoryTrackerId",
    name: "memoryTrackerShow",
    props: (route: RouteLocation) => ({
      memoryTrackerId: Number(route.params.memoryTrackerId),
    }),
  },
]
