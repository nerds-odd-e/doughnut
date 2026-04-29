<template>
  <ContentLoader v-if="notebook === undefined" />
  <div
    v-else-if="user !== undefined"
    class="daisy-flex daisy-flex-col daisy-h-full"
  >
    <GlobalBar>
      <button
        role="button"
        class="daisy-btn daisy-btn-sm daisy-btn-ghost"
        :class="{ 'sidebar-expanded': sidebarOpened }"
        title="toggle sidebar"
        @click="sidebarOpened = !sidebarOpened"
      >
        <div class="daisy-w-4 daisy-h-4">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
            class="w-4 h-4"
          >
            <template v-if="sidebarOpened">
              <path d="M19 12H5M12 19l-7-7 7-7" />
            </template>
            <template v-else>
              <line x1="3" y1="6" x2="21" y2="6" />
              <line x1="6" y1="12" x2="21" y2="12" />
              <line x1="3" y1="18" x2="21" y2="18" />
            </template>
          </svg>
        </div>
      </button>
      <div
        class="daisy-text-sm daisy-breadcrumbs daisy-max-w-full daisy-min-w-0"
      >
        <ul class="daisy-m-0 daisy-pl-0">
          <li>
            <router-link :to="{ name: 'notebooks' }">Notebooks</router-link>
          </li>
          <li v-if="notebook.circle">
            <router-link
              :to="{
                name: 'circleShow',
                params: { circleId: notebook.circle.id },
              }"
              >{{ notebook.circle.name }}</router-link
            >
          </li>
          <li>{{ notebook.title }}</li>
        </ul>
      </div>
    </GlobalBar>
    <div
      v-if="!isMdOrLarger && sidebarOpened"
      class="daisy-fixed daisy-inset-0 daisy-bg-black/50 daisy-z-30"
      @click="sidebarOpened = false"
    />
    <div
      class="daisy-h-full daisy-relative daisy-flex daisy-flex-1 daisy-min-h-0"
    >
      <aside
        :class="[
          'daisy-bg-base-200 daisy-w-72 daisy-transition-all daisy-ease-in-out daisy-overflow-y-auto',
          isMdOrLarger
            ? sidebarOpened
              ? 'daisy-relative'
              : 'daisy-hidden'
            : sidebarOpened
              ? 'daisy-translate-x-0 daisy-fixed daisy-top-0 daisy-left-0 daisy-z-40 daisy-h-full'
              : '-daisy-translate-x-full daisy-fixed daisy-top-0 daisy-left-0 daisy-h-full',
        ]"
      >
        <NoteSidebar
          v-if="sidebarRealm"
          v-bind="{
            noteRealm: sidebarRealm,
          }"
        />
      </aside>
      <main
        class="daisy-flex-1 daisy-px-4 daisy-container daisy-mx-auto daisy-overflow-y-auto"
      >
        <NotebookPageView
          :notebook="notebook"
          :user="user"
          :approval="approval"
          :approval-loaded="approvalLoaded"
          :additional-instructions="aiAssistant?.additionalInstructionsToAi || ''"
          :show-add-first-note="sidebarAnchorNoteId == null"
          @notebook-updated="handleNotebookUpdated"
        />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  inject,
  onMounted,
  onBeforeUnmount,
  ref,
  watch,
  computed,
  type Ref,
} from "vue"
import { useRoute } from "vue-router"
import type {
  Notebook,
  User,
  NotebookCertificateApproval,
  NotebookAiAssistant,
} from "@generated/doughnut-backend-api"
import {
  NotebookController,
  NotebookCertificateApprovalController,
} from "@generated/doughnut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import NotebookPageView from "./NotebookPageView.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import NoteSidebar from "@/components/notes/NoteSidebar.vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const route = useRoute()
const storageAccessor = useStorageAccessor()
const user = inject<Ref<User | undefined>>("currentUser")
const notebook = ref<Notebook | undefined>(undefined)
const approval = ref<NotebookCertificateApproval | undefined>(undefined)
const approvalLoaded = ref(false)
const aiAssistant = ref<NotebookAiAssistant | undefined>(undefined)

const sidebarOpened = ref(false)
const sidebarAnchorNoteId = ref<number | undefined>()
const windowWidth = ref(
  typeof window !== "undefined" ? window.innerWidth : 1024
)

const sidebarRealm = computed(() => {
  const id = sidebarAnchorNoteId.value
  if (id == null) return undefined
  return storageAccessor.value.refOfNoteRealm(id).value
})

const isMdOrLarger = computed(() => windowWidth.value >= 768)

const handleResize = () => {
  windowWidth.value = window.innerWidth
}

const resolveSidebarRealm = async (nb: Notebook) => {
  sidebarAnchorNoteId.value = undefined
  try {
    const realm = await storageAccessor.value
      .storedApi()
      .loadNoteByNotebookSlug(nb.id, "index")
    sidebarAnchorNoteId.value = realm.id
    return
  } catch {
    // No root index note for this slug path
  }
}

const fetchNotebook = async () => {
  const notebookId = Number(route.params.notebookId)
  const { data: result, error } = await NotebookController.get({
    path: { notebook: notebookId },
  })
  if (!error) {
    notebook.value = result!
  }
}

const fetchApproval = async () => {
  if (!notebook.value) return
  const { data: dto, error } = await apiCallWithLoading(() =>
    NotebookCertificateApprovalController.getApprovalForNotebook({
      path: { notebook: notebook.value!.id },
    })
  )
  if (!error) {
    approval.value = dto!.approval
    approvalLoaded.value = true
  } else {
    approvalLoaded.value = true
  }
}

const fetchAiAssistant = async () => {
  if (!notebook.value) return
  const { data: assistant, error } = await apiCallWithLoading(() =>
    NotebookController.getAiAssistant({
      path: { notebook: notebook.value!.id },
    })
  )
  if (!error) {
    aiAssistant.value = assistant!
  }
}

const handleNotebookUpdated = (updatedNotebook: Notebook) => {
  notebook.value = updatedNotebook
}

watch(notebook, async (nb) => {
  if (nb) {
    await resolveSidebarRealm(nb)
  } else {
    sidebarAnchorNoteId.value = undefined
  }
})

watch(
  () => notebook.value?.id,
  () => {
    if (!isMdOrLarger.value) {
      sidebarOpened.value = false
    }
  }
)

watch(
  () => route.params.notebookId,
  async () => {
    await fetchNotebook()
    if (notebook.value) {
      fetchApproval()
      fetchAiAssistant()
    }
  }
)

onMounted(async () => {
  window.addEventListener("resize", handleResize)
  if (windowWidth.value >= 768) {
    sidebarOpened.value = true
  }
  await fetchNotebook()
  if (notebook.value) {
    fetchApproval()
    fetchAiAssistant()
  }
})

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize)
})
</script>

<style scoped>
aside {
  max-height: 100%;
}

main {
  max-height: 100%;
}
</style>
