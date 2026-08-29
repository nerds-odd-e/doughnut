import type { RouteRecordRaw } from "vue-router"
import { routeMetadata } from "./routeMetadata"

/** Metadata table with dummy components so href can compile without page imports. */
export const dummyRouteRecordsFromMetadata: RouteRecordRaw[] =
  routeMetadata.map((metadata) => {
    if (metadata.redirect !== undefined) {
      return {
        path: metadata.path,
        redirect: metadata.redirect,
      } as RouteRecordRaw
    }
    return {
      ...metadata,
      component: {
        template: `<div>${metadata.name} (Mock)</div>`,
      },
    }
  }) as RouteRecordRaw[]
