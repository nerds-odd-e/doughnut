<template>
  <div class="note-under-question bg-base-200 border-base-300">
    <div class="note-label bg-base-200 text-base-content">Note under question</div>
    <FocusedPropertyIndicator
      :note-id="noteTopology.id"
      :property-key="focusedPropertyKey"
      :property-value="focusedPropertyValue"
    />
    <div class="breadcrumb-wrapper">
        <Breadcrumb
          :ancestor-folders="noteRealm?.ancestorFolders ?? ancestorFolders ?? []"
          :breadcrumb-notebook-id="noteRealm?.notebookRealm.notebook.id ?? breadcrumbNotebookId"
        >
          <template #topLink>
            <li v-if="noteRealm">
              <router-link :to="{ name: 'notebookPage', params: { notebookId: String(noteRealm.notebookRealm.notebook.id) } }">
                {{ noteRealm.notebookRealm.notebook.name }}
              </router-link>
            </li>
          </template>
          <template #additional>
            <NoteTitleWithLink v-bind="{ noteTopology }" />
          </template>
        </Breadcrumb>
    </div>
    <slot />
  </div>
</template>

<script setup lang="ts">
import { computed, type PropType } from "vue"
import { useNoteStore } from "@/store/noteStore"
import type {
  FolderTrailSegment,
  NoteTopology,
} from "@generated/donut-backend-api"
import Breadcrumb from "@/components/toolbars/Breadcrumb.vue"
import NoteTitleWithLink from "@/components/notes/NoteTitleWithLink.vue"
import FocusedPropertyIndicator from "./FocusedPropertyIndicator.vue"

const props = defineProps({
  noteTopology: {
    type: Object as PropType<NoteTopology>,
    required: true,
  },
  ancestorFolders: {
    type: Array as PropType<FolderTrailSegment[] | undefined>,
    default: undefined,
  },
  breadcrumbNotebookId: {
    type: Number as PropType<number | undefined>,
    default: undefined,
  },
  focusedPropertyKey: {
    type: String as PropType<string | undefined>,
    default: undefined,
  },
  focusedPropertyValue: {
    type: String as PropType<string | undefined>,
    default: undefined,
  },
})

const noteStore = useNoteStore()
const noteRealm = computed(
  () => noteStore.getNoteRealmRefAndLoadWhenNeeded(props.noteTopology.id).value
)
</script>

<style lang="sass" scoped>
.note-under-question
  width: 100%
  min-width: 0
  border-radius: 5px
  padding: 8px 12px
  padding-top: 24px
  margin-top: 15px
  border: 2px solid
  position: relative

  .note-label
    position: absolute
    top: -12px
    left: 10px
    padding: 0 10px
    font-weight: 500
    font-size: 0.9rem
    border-radius: 3px
    z-index: 1

  .breadcrumb-wrapper
    width: 100%
    min-width: 0
    :deep(.daisy-breadcrumbs)
      overflow: visible
      ul
        flex-wrap: wrap
        white-space: normal
      li, a
        min-width: 0
        max-width: 100%
        overflow-wrap: anywhere
      li
        flex-shrink: 1
      a
        white-space: normal
</style>
