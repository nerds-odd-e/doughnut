import type {
  RouteComponent,
  RouteLocationRaw,
  RouteRecordRaw,
} from "vue-router"
import type { RouteMetadata } from "./routeMetadata"

const sidebarLayoutIndexNames = new Set([
  "noteShow",
  "notebookPage",
  "folderPage",
])

export function relativePathUnder(
  parentPath: string,
  childPath: string
): string {
  if (childPath === parentPath) {
    return ""
  }
  const prefix = `${parentPath}/`
  if (!childPath.startsWith(prefix)) {
    throw new Error(`Path "${childPath}" is not under "${parentPath}"`)
  }
  return childPath.slice(prefix.length)
}

export const legacyDeeplinkPrefixRedirect: RouteRecordRaw = {
  path: "/d/:pathMatch(.*)*",
  redirect: (to): RouteLocationRaw => {
    const pm = to.params.pathMatch
    if (pm === undefined || pm === "") return "/"
    const suffix = Array.isArray(pm) ? pm.join("/") : String(pm)
    return suffix === "" ? "/" : `/${suffix}`
  },
}

function sidebarParentPathsFrom(
  metadataList: readonly RouteMetadata[]
): string[] {
  return metadataList
    .filter(
      (metadata) =>
        metadata.name !== undefined &&
        sidebarLayoutIndexNames.has(metadata.name)
    )
    .map((metadata) => metadata.path)
}

function sidebarParentPathFor(
  path: string,
  parentPaths: readonly string[]
): string | undefined {
  let best: string | undefined
  for (const parentPath of parentPaths) {
    if (path === parentPath || path.startsWith(`${parentPath}/`)) {
      if (best === undefined || parentPath.length > best.length) {
        best = parentPath
      }
    }
  }
  return best
}

function sidebarParentRecord(
  parentPath: string,
  children: RouteMetadata[],
  componentFor: (name: string) => RouteComponent,
  sidebarLayout: RouteComponent
): RouteRecordRaw {
  const parent: RouteRecordRaw = {
    path: parentPath,
    component: sidebarLayout,
    children: children.map((metadata) => {
      const name = metadata.name!
      return {
        path: relativePathUnder(parentPath, metadata.path),
        name,
        component: componentFor(name),
        props: metadata.props,
        meta: metadata.meta,
      }
    }),
  }
  if (children.some((child) => child.name === "noteShow")) {
    parent.meta = { noteRouteFamily: true }
  }
  const indexChild = children.find((child) => child.path === parentPath)
  if (indexChild?.alias !== undefined) {
    parent.alias = indexChild.alias
  }
  return parent
}

export function routeRecordsFromMetadata(
  metadataList: readonly RouteMetadata[],
  componentFor: (name: string) => RouteComponent,
  sidebarLayout: RouteComponent
): RouteRecordRaw[] {
  const parentPaths = sidebarParentPathsFrom(metadataList)
  const childrenByParent = new Map<string, RouteMetadata[]>()
  for (const metadata of metadataList) {
    if (metadata.redirect !== undefined) {
      continue
    }
    const parentPath = sidebarParentPathFor(metadata.path, parentPaths)
    if (parentPath === undefined) {
      continue
    }
    const siblings = childrenByParent.get(parentPath) ?? []
    siblings.push(metadata)
    childrenByParent.set(parentPath, siblings)
  }

  const emittedParents = new Set<string>()
  const records: RouteRecordRaw[] = []
  for (const metadata of metadataList) {
    if (metadata.redirect !== undefined) {
      records.push({
        path: metadata.path,
        redirect: metadata.redirect,
      } as RouteRecordRaw)
      continue
    }
    const parentPath = sidebarParentPathFor(metadata.path, parentPaths)
    if (parentPath === undefined) {
      records.push({
        ...metadata,
        component: componentFor(metadata.name!),
      } as RouteRecordRaw)
      continue
    }
    if (emittedParents.has(parentPath)) {
      continue
    }
    emittedParents.add(parentPath)
    records.push(
      sidebarParentRecord(
        parentPath,
        childrenByParent.get(parentPath)!,
        componentFor,
        sidebarLayout
      )
    )
  }
  return records
}
