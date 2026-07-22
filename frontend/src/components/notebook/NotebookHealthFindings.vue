<template>
  <div data-testid="notebook-health-findings" class="space-y-3">
    <div
      v-for="group in groups"
      :key="group.ruleId"
      class="daisy-collapse daisy-collapse-arrow border border-base-300 bg-base-200/50 rounded-lg"
      :data-testid="`notebook-health-group-${group.ruleId}`"
    >
      <input type="checkbox" :checked="findingCount(group) > 0" />
      <div
        class="daisy-collapse-title min-h-0 py-3 text-sm font-semibold text-base-content"
      >
        {{ group.title }} ({{ findingCount(group) }})
      </div>
      <div class="daisy-collapse-content text-sm">
        <ul
          v-if="(group.items?.length ?? 0) > 0"
          class="flex flex-col gap-2"
        >
          <li v-for="(item, index) in group.items" :key="itemKey(item, index)">
            {{ item.label }}
          </li>
        </ul>

        <template v-if="group.ruleId === 'dead_wiki_links'">
          <div
            v-for="entry in deadWikiLinkEntries(group)"
            :key="entry.key"
            class="mt-2 space-y-1"
            data-testid="notebook-health-dead-link-note"
          >
            <router-link
              v-if="entry.noteId != null"
              :to="noteShowLocation(entry.noteId)"
              class="font-semibold text-base-content no-underline hover:underline"
              data-testid="notebook-health-dead-link-note-title"
            >
              {{ entry.title }}
            </router-link>
            <div v-else class="font-semibold text-base-content">
              {{ entry.title }}
            </div>
            <ul class="flex flex-col gap-1 pl-3">
              <li
                v-for="(item, index) in entry.items"
                :key="itemKey(item, index)"
              >
                <router-link
                  v-if="item.noteId != null"
                  :to="noteShowLocation(item.noteId)"
                  class="text-base-content/80 no-underline hover:underline"
                  data-testid="notebook-health-dead-link-token"
                >
                  {{ item.label }}
                </router-link>
                <span v-else>{{ item.label }}</span>
              </li>
            </ul>
          </div>
        </template>

        <template v-else>
          <div
            v-for="(child, childIndex) in group.children ?? []"
            :key="childKey(child, childIndex)"
            class="daisy-collapse daisy-collapse-arrow border border-base-300 bg-base-200/50 rounded-lg mt-2"
          >
            <input type="checkbox" :checked="findingCount(child) > 0" />
            <div
              class="daisy-collapse-title min-h-0 py-3 text-sm font-semibold text-base-content"
            >
              {{ child.title }}
            </div>
            <div class="daisy-collapse-content">
              <ul class="flex flex-col gap-2">
                <li
                  v-for="(item, index) in child.items ?? []"
                  :key="itemKey(item, index)"
                >
                  {{ item.label }}
                </li>
              </ul>
            </div>
          </div>
        </template>

        <p
          v-if="findingCount(group) === 0"
          class="text-sm text-base-content/70"
        >
          No findings
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type {
  HealthFindingGroup,
  HealthFindingItem,
} from "@generated/doughnut-backend-api"
import { noteShowLocation } from "@/routes/noteShowLocation"

defineProps<{
  groups: HealthFindingGroup[]
}>()

function findingCount(group: HealthFindingGroup): number {
  const itemCount = group.items?.length ?? 0
  const childItemCount = (group.children ?? []).reduce(
    (sum, child) => sum + (child.items?.length ?? 0),
    0
  )
  return itemCount + childItemCount
}

function itemKey(item: HealthFindingItem, index: number): string {
  return `${item.folderId ?? item.noteId ?? item.label ?? "item"}-${index}`
}

function childKey(child: HealthFindingGroup, index: number): string {
  return `${child.ruleId}-${child.title}-${index}`
}

function childNoteId(child: HealthFindingGroup): number | undefined {
  return child.items?.find((item) => item.noteId != null)?.noteId
}

function deadWikiLinkEntries(group: HealthFindingGroup) {
  return (group.children ?? []).map((child, index) => ({
    key: childKey(child, index),
    title: child.title,
    noteId: childNoteId(child),
    items: child.items ?? [],
  }))
}
</script>
