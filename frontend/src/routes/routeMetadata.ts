import type { RouteLocation, RouteRecordRaw } from "vue-router"

// Route metadata definitions (path, name, props, meta) without component imports
// This allows Storybook to import route definitions without pulling in page components
export interface RouteMetadata {
  path: string
  /** Omitted when `redirect` is set (legacy URL only). */
  name?: string
  alias?: string | string[]
  props?: boolean | ((route: RouteLocation) => Record<string, unknown>)
  meta?: Record<string, unknown>
  /** When set, this entry is redirect-only (no `name` / page component). */
  redirect?: RouteRecordRaw["redirect"]
}

export const routeMetadata: RouteMetadata[] = [
  { path: "/", name: "root" },
  {
    path: "/notebooks",
    name: "notebooks",
  },
  {
    path: "/notebooks/groups/:groupId",
    name: "notebookGroup",
    props: (route: RouteLocation) => ({
      groupId: Number(route.params.groupId),
    }),
  },
  {
    path: "/notebooks/:notebookId/book",
    name: "bookReading",
    props: (route: RouteLocation) => ({
      notebookId: Number(route.params.notebookId),
    }),
  },
  {
    path: "/notebooks/:notebookId(\\d+)/folders/:folderId(\\d+)",
    name: "folderPage",
    props: (route: RouteLocation) => ({
      notebookId: Number(route.params.notebookId),
      folderId: Number(route.params.folderId),
    }),
  },
  {
    path: "/notebooks/:notebookId(\\d+)",
    name: "notebookPage",
    props: (route: RouteLocation) => ({
      notebookId: Number(route.params.notebookId),
    }),
  },
  {
    path: "/n/:noteId(\\d+)",
    redirect: (to) => {
      const raw = to.params.noteId
      const id = Array.isArray(raw) ? raw[0] : raw
      return `/n${id ?? ""}`
    },
  },
  {
    path: "/n:noteId(\\d+)",
    name: "noteShow",
    props: (route: RouteLocation) => ({
      noteId: Number(route.params.noteId),
    }),
    meta: { useNoteStorageAccessor: true },
  },
  {
    path: "/circles/:circleId",
    name: "circleShow",
    props: true,
    meta: { useNoteStorageAccessor: true },
  },
  {
    path: "/bazaar",
    name: "bazaar",
  },
  {
    path: "/admin-dashboard",
    name: "adminDashboard",
    props: true,
  },
  {
    path: "/circles/join/:invitationCode?",
    name: "circleJoin",
    props: true,
  },
  {
    path: "/message-center/:conversationId?",
    name: "messageCenter",
    props: (route: RouteLocation) => ({
      conversationId: route.params.conversationId
        ? Number(route.params.conversationId)
        : undefined,
    }),
    meta: { useNoteStorageAccessor: true },
  },
  {
    path: "/recall",
    name: "recall",
    meta: { useNoteStorageAccessor: true },
  },
  {
    path: "/failure-report-list/show/:failureReportId",
    name: "failureReport",
    props: true,
  },
  {
    path: "/users/identify",
    name: "nonproductionOnlyLogin",
  },
  {
    path: "/recent",
    name: "recent",
  },
  {
    path: "/circles",
    name: "circles",
  },
  {
    path: "/generate-token",
    name: "manageAccessTokens",
  },
  {
    path: "/memory-trackers/:memoryTrackerId",
    name: "memoryTrackerShow",
    props: (route: RouteLocation) => ({
      memoryTrackerId: Number(route.params.memoryTrackerId),
    }),
  },
]
