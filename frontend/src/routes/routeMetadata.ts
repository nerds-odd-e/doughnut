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

function firstPathParam(
  params: RouteLocation["params"],
  name: string
): string | undefined {
  const raw = params[name]
  return Array.isArray(raw) ? raw[0] : raw
}

function noteIdFromRoute(route: RouteLocation): Record<string, unknown> {
  return { noteId: Number(route.params.noteId) }
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
    redirect: (to) => ({
      name: "noteShow",
      params: { noteId: firstPathParam(to.params, "noteId") },
    }),
  },
  {
    path: "/n/:noteId(\\d+)/p/:propertyKey",
    redirect: (to) => ({
      name: "noteProperty",
      params: {
        noteId: firstPathParam(to.params, "noteId"),
        propertyKey: firstPathParam(to.params, "propertyKey"),
      },
      query: to.query,
      hash: to.hash,
    }),
  },
  {
    path: "/n:noteId(\\d+)",
    name: "noteShow",
    props: noteIdFromRoute,
    meta: { useNoteStorageAccessor: true },
  },
  {
    path: "/n:noteId(\\d+)/p/:propertyKey",
    name: "noteProperty",
    props: noteIdFromRoute,
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
    path: "/circles",
    name: "circles",
  },
  {
    path: "/memory-trackers/:memoryTrackerId",
    name: "memoryTrackerShow",
    props: (route: RouteLocation) => ({
      memoryTrackerId: Number(route.params.memoryTrackerId),
    }),
  },
  { path: "/settings", name: "settingsGeneral" },
  { path: "/settings/recent", name: "settingsRecent" },
  { path: "/settings/access-tokens", name: "settingsAccessTokens" },
  { path: "/settings/recall-stats", name: "settingsRecallStats" },
]
