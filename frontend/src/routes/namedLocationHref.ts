import {
  createMemoryHistory,
  createRouter,
  type RouteLocationNamedRaw,
} from "vue-router"
import { dummyRouteRecordsFromMetadata } from "./dummyRouteRecords"

const namedLocationHrefRouter = createRouter({
  history: createMemoryHistory(),
  routes: dummyRouteRecordsFromMetadata,
})

export function namedLocationHref({
  name,
  params,
  query,
}: RouteLocationNamedRaw): string {
  return namedLocationHrefRouter.resolve({ name, params, query }).href
}
