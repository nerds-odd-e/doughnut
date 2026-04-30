<template>
  <div class="daisy-flex daisy-flex-col daisy-h-full">
    <div
      v-if="loadError !== null"
      class="daisy-alert daisy-alert-error daisy-my-4"
      role="alert"
    >
      {{ loadError }}
    </div>
    <ContentLoader v-else-if="resolvedNoteId === undefined" />
    <NoteShow
      v-else
      v-bind="{
        noteId: resolvedNoteId,
        expandChildren: true,
        isMinimized: isContentMinimized,
      }"
    >
      <template #note-conversation="{ noteRealm: conversationRealm }">
        <div
          v-if="Boolean(route.query.conversation)"
          class="conversation-wrapper daisy-border-t daisy-border-base-200 daisy-flex-1 daisy-flex daisy-flex-col daisy-bg-base-100/50"
        >
          <NoteConversation
            :note-id="conversationRealm.id"
            :is-maximized="isContentMinimized"
            @close-dialog="handleCloseConversation(conversationRealm)"
            @toggle-maximize="toggleMaximize"
          />
        </div>
      </template>
    </NoteShow>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed, onMounted } from "vue"

import { useRoute, useRouter } from "vue-router"
import NoteShow from "../components/notes/NoteShow.vue"
import NoteConversation from "../components/conversations/NoteConversation.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import {
  currentActiveNoteId,
  currentNotebookId,
  notebookSidebarNotebookPageContext,
  resetNotebookSidebarState,
} from "@/composables/useCurrentNoteSidebarState"
import { noteShowByNotebookSlugLocationFromNoteRealm } from "@/routes/noteShowLocation"
import type { NoteRealm } from "@generated/doughnut-backend-api"

const router = useRouter()
const route = useRoute()
const storageAccessor = useStorageAccessor()

const props = defineProps({
  slug: { type: String, required: false },
  notebookId: { type: Number, required: false },
  noteSlugPath: { type: String, required: false },
})

const ambiguousSlugError = ref<string | null>(null)
const ambiguousSlugResolvedNoteId = ref<number | undefined>(undefined)
const notebookSlugError = ref<string | null>(null)
const notebookSlugResolvedNoteId = ref<number | undefined>(undefined)
let notebookSlugLoadGeneration = 0

const isNotebookSlugEntry = computed(
  () =>
    props.notebookId != null &&
    !Number.isNaN(props.notebookId) &&
    props.noteSlugPath !== undefined &&
    props.noteSlugPath !== ""
)

const loadError = computed(
  () => notebookSlugError.value ?? ambiguousSlugError.value
)

watch(loadError, (err) => {
  if (err != null) {
    resetNotebookSidebarState()
  }
})

watch(
  () => props.slug,
  async (s) => {
    if (isNotebookSlugEntry.value) {
      return
    }
    ambiguousSlugError.value = null
    ambiguousSlugResolvedNoteId.value = undefined
    if (s === undefined || s === "") {
      return
    }
    try {
      const realm = await storageAccessor.value
        .storedApi()
        .loadNoteByAmbiguousBasename(s)
      ambiguousSlugResolvedNoteId.value = realm.id
    } catch (e: unknown) {
      ambiguousSlugError.value =
        e instanceof Error ? e.message : "Could not load note"
    }
  },
  { immediate: true }
)

watch(
  () => [props.notebookId, props.noteSlugPath] as const,
  async ([nb, path]) => {
    notebookSlugError.value = null
    const generation = ++notebookSlugLoadGeneration
    notebookSlugResolvedNoteId.value = undefined
    if (nb == null || Number.isNaN(nb) || path === undefined || path === "") {
      return
    }
    try {
      const realm = await storageAccessor.value
        .storedApi()
        .loadNoteByNotebookSlug(nb, path)
      if (generation !== notebookSlugLoadGeneration) return
      notebookSlugResolvedNoteId.value = realm.id
    } catch (e: unknown) {
      if (generation !== notebookSlugLoadGeneration) return
      notebookSlugError.value =
        e instanceof Error ? e.message : "Could not load note"
    }
  },
  { immediate: true }
)

const resolvedNoteId = computed((): number | undefined => {
  if (isNotebookSlugEntry.value) {
    if (notebookSlugResolvedNoteId.value == null) {
      return undefined
    }
    return storageAccessor.value.refOfNoteRealm(
      notebookSlugResolvedNoteId.value
    ).value?.id
  }
  if (
    props.slug !== undefined &&
    props.slug !== "" &&
    ambiguousSlugResolvedNoteId.value != null
  ) {
    return storageAccessor.value.refOfNoteRealm(
      ambiguousSlugResolvedNoteId.value
    ).value?.id
  }
  return undefined
})

const noteRealm = computed(() => {
  const id = resolvedNoteId.value
  if (id == null) return undefined
  return storageAccessor.value.refOfNoteRealm(id).value
})

watch(
  resolvedNoteId,
  (id) => {
    if (id != null) {
      currentActiveNoteId.value = id
    }
  },
  { immediate: true }
)

watch(
  () => ({
    slug: props.slug,
    notebookId: props.notebookId,
    isSlug: isNotebookSlugEntry.value,
    realm: noteRealm.value,
  }),
  ({ slug, notebookId, isSlug, realm }) => {
    if (isSlug && notebookId != null && !Number.isNaN(notebookId)) {
      currentNotebookId.value = notebookId
      return
    }
    if (slug !== undefined && slug !== "") {
      if (realm?.notebookId != null) {
        currentNotebookId.value = realm.notebookId
      }
      return
    }
    currentNotebookId.value = undefined
  },
  { immediate: true }
)

const isContentMinimized = ref(false)

const toggleMaximize = () => {
  isContentMinimized.value = !isContentMinimized.value
}

const handleCloseConversation = (conversationRealm: NoteRealm) => {
  isContentMinimized.value = false
  router.replace({
    ...noteShowByNotebookSlugLocationFromNoteRealm(conversationRealm),
    query: {},
  })
}

onMounted(() => {
  notebookSidebarNotebookPageContext.value = undefined
})
</script>

<style scoped>
.conversation-wrapper {
  max-height: 100%;
  overflow: hidden;
}
</style>
