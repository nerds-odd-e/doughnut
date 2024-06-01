<template>
  <table v-if="notebooks" class="table">
    <tbody>
      <tr v-for="notebook in notebooks.notebooks" :key="notebook.id">
        <td>
          <NoteTopicWithLink
            v-bind="{ noteTopic: notebook.headNote.noteTopic }"
          />
        </td>
        <td>
          <button class="btn btn-dange" @click="removeFromBazaar(notebook)">
            Remove
          </button>
        </td>
      </tr>
    </tbody>
  </table>
</template>

<script lang="ts" setup>
import { onMounted, ref } from "vue";
import useLoadingApi from "@/managedApi/useLoadingApi";
import {
  NotebookViewedByUser,
  NotebooksViewedByUser,
} from "@/generated/backend";
import NoteTopicWithLink from "../notes/NoteTopicWithLink.vue";
import usePopups from "../commons/Popups/usePopups";

const { managedApi } = useLoadingApi();
const { popups } = usePopups();

const notebooks = ref<NotebooksViewedByUser | undefined>(undefined);

const fetchData = async () => {
  notebooks.value = await managedApi.restBazaarController.bazaar();
};

const removeFromBazaar = async (notebook: NotebookViewedByUser) => {
  if (
    await popups.confirm(
      `Are you sure you want to remove "${notebook.headNote.noteTopic.topicConstructor}" from the bazaar?`,
    )
  ) {
    //
  }
};

onMounted(() => {
  fetchData();
});
</script>
