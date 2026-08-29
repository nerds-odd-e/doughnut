import type {
  RouteComponent,
  RouteLocationRaw,
  RouteRecordRaw,
} from "vue-router"
import HomePage from "@/pages/HomePage.vue"
import BazaarPage from "@/pages/BazaarPage.vue"
import NotebooksPage from "@/pages/NotebooksPage.vue"
import NoteShowPage from "@/pages/NoteShowPage.vue"
import RecallPage from "@/pages/RecallPage.vue"
import CircleShowPage from "@/pages/CircleShowPage.vue"
import CircleJoinPage from "@/pages/CircleJoinPage.vue"
import FailureReportPage from "@/pages/FailureReportPage.vue"
import AdminDashboardPage from "@/pages/AdminDashboardPage.vue"
import NonproductionOnlyLoginPage from "@/pages/NonproductionOnlyLoginPage.vue"
import MessageCenterPage from "../pages/MessageCenterPage.vue"
import CirclesPage from "@/pages/CirclesPage.vue"
import MemoryTrackerPage from "@/pages/MemoryTrackerPage.vue"
import NotebookPage from "@/pages/NotebookPage.vue"
import FolderPage from "@/pages/FolderPage.vue"
import NotebookGroupPage from "@/pages/NotebookGroupPage.vue"
import BookReadingPage from "@/pages/BookReadingPage.vue"
import SettingsPage from "@/pages/SettingsPage.vue"
import GeneralSettingsTab from "@/pages/settings/GeneralSettingsTab.vue"
import RecentSettingsTab from "@/pages/settings/RecentSettingsTab.vue"
import AccessTokensSettingsTab from "@/pages/settings/AccessTokensSettingsTab.vue"
import RecallStatsSettingsTab from "@/pages/settings/RecallStatsSettingsTab.vue"
import NotebookSidebarLayout from "@/layouts/NotebookSidebarLayout.vue"
import { routeMetadata } from "./routeMetadata"

// Legacy bookmarks used a `/d/…` prefix; strip it and re-resolve (see `legacyDeeplinkPrefixRedirect`).

// Map route names to components
const componentMap: Record<string, RouteComponent> = {
  root: HomePage,
  notebooks: NotebooksPage,
  notebookGroup: NotebookGroupPage,
  noteShow: NoteShowPage,
  circleShow: CircleShowPage,
  bazaar: BazaarPage,
  adminDashboard: AdminDashboardPage,
  circleJoin: CircleJoinPage,
  messageCenter: MessageCenterPage,
  recall: RecallPage,
  failureReport: FailureReportPage,
  nonproductionOnlyLogin: NonproductionOnlyLoginPage,
  circles: CirclesPage,
  memoryTrackerShow: MemoryTrackerPage,
  notebookPage: NotebookPage,
  folderPage: FolderPage,
  bookReading: BookReadingPage,
}

const notebookSidebarNestedRouteNames = new Set([
  "noteShow",
  "notebookPage",
  "folderPage",
])

const settingsTabComponents = {
  settingsGeneral: GeneralSettingsTab,
  settingsRecent: RecentSettingsTab,
  settingsAccessTokens: AccessTokensSettingsTab,
  settingsRecallStats: RecallStatsSettingsTab,
}

type SettingsTabName = keyof typeof settingsTabComponents

function isSettingsTabName(name: string | undefined): name is SettingsTabName {
  return name !== undefined && name in settingsTabComponents
}

function routeMetadataByName(name: string) {
  const found = routeMetadata.find((metadata) => metadata.name === name)
  if (found === undefined) {
    throw new Error(`routeMetadata is missing named entry "${name}"`)
  }
  return found
}

function relativePathUnder(parentPath: string, childPath: string): string {
  if (childPath === parentPath) {
    return ""
  }
  const prefix = `${parentPath}/`
  if (!childPath.startsWith(prefix)) {
    throw new Error(`Path "${childPath}" is not under "${parentPath}"`)
  }
  return childPath.slice(prefix.length)
}

// Combine route metadata with components
const routesFromMetadata: RouteRecordRaw[] = routeMetadata
  .filter((metadata) => !isSettingsTabName(metadata.name))
  .map((metadata) => {
    if (metadata.redirect !== undefined) {
      return {
        path: metadata.path,
        redirect: metadata.redirect,
      } as RouteRecordRaw
    }
    const name = metadata.name!
    if (notebookSidebarNestedRouteNames.has(name)) {
      const parent: RouteRecordRaw = {
        path: metadata.path,
        component: NotebookSidebarLayout,
        children: [
          {
            path: "",
            name,
            component: componentMap[name]!,
            props: metadata.props,
            meta: metadata.meta,
          },
        ],
      }
      if (metadata.alias !== undefined) {
        parent.alias = metadata.alias
      }
      return parent
    }
    return {
      ...metadata,
      component: componentMap[name]!,
    }
  }) as RouteRecordRaw[]

const legacyDeeplinkPrefixRedirect: RouteRecordRaw = {
  path: "/d/:pathMatch(.*)*",
  redirect: (to): RouteLocationRaw => {
    const pm = to.params.pathMatch
    if (pm === undefined || pm === "") return "/"
    const suffix = Array.isArray(pm) ? pm.join("/") : String(pm)
    return suffix === "" ? "/" : `/${suffix}`
  },
}

const settingsGeneralMetadata = routeMetadataByName("settingsGeneral")

const settingsNestedRoute: RouteRecordRaw = {
  path: settingsGeneralMetadata.path,
  component: SettingsPage,
  children: (Object.keys(settingsTabComponents) as SettingsTabName[]).map(
    (name) => {
      const metadata = routeMetadataByName(name)
      return {
        path: relativePathUnder(settingsGeneralMetadata.path, metadata.path),
        name,
        component: settingsTabComponents[name],
      }
    }
  ),
}

const routes: RouteRecordRaw[] = [
  ...routesFromMetadata,
  settingsNestedRoute,
  legacyDeeplinkPrefixRedirect,
]

export default routes
