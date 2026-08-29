import type { RouteComponent, RouteRecordRaw } from "vue-router"
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
import {
  legacyDeeplinkPrefixRedirect,
  relativePathUnder,
  routeRecordsFromMetadata,
} from "./routeRecordsFromMetadata"

const componentMap: Record<string, RouteComponent> = {
  root: HomePage,
  notebooks: NotebooksPage,
  notebookGroup: NotebookGroupPage,
  noteShow: NoteShowPage,
  noteProperty: NoteShowPage,
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

const routesFromMetadata: RouteRecordRaw[] = routeRecordsFromMetadata(
  routeMetadata.filter((metadata) => !isSettingsTabName(metadata.name)),
  (name) => componentMap[name]!,
  NotebookSidebarLayout
)

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
