import type { RouteComponent, RouteRecordRaw } from "vue-router"
import { routeMetadata } from "./routeMetadata"
import {
  legacyDeeplinkPrefixRedirect,
  routeRecordsFromMetadata,
} from "./routeRecordsFromMetadata"

function dummyComponent(name: string): RouteComponent {
  return {
    template: `<div>${name} (Mock)</div>`,
  }
}

/** Metadata table with dummy components so href can compile without page imports. */
export const dummyRouteRecordsFromMetadata: RouteRecordRaw[] = [
  ...routeRecordsFromMetadata(
    routeMetadata,
    (name) => dummyComponent(name),
    dummyComponent("notebookSidebar")
  ),
  legacyDeeplinkPrefixRedirect,
]
