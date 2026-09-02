import type { Component } from "vue"

export type NavigationItemProps = {
  name?: string
  label: string
  icon: Component
  isActive: boolean
  badge?: number | string
  badgeClass?: string
  badgeTitle?: string
  hasDropdown?: boolean
  nonClickable?: boolean
}
