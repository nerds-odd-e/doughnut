<template>
  <div>
    <LinkTypeSelect
      field="linkType"
      scope-name="link"
      v-model="formData.typeId"
      :errors="formErrors.typeId"
      :inverse-icon="true"
    />
    <div>
      Target: <strong>{{ targetNote.title }}</strong>
    </div>
    <CheckInput
      scope-name="link"
      v-model="formData.moveUnder"
      :errors="formErrors.moveUnder"
      field="alsoMoveToUnderTargetNote"
    />

    <RadioButtons
      v-if="!!formData.moveUnder"
      scope-name="link"
      v-model="formData.asFirstChild"
      :errors="formErrors.asFirstChild"
      :options="[
        { value: true, label: 'as its first child' },
        { value: false, label: 'as its last child' },
      ]"
    />

    <button class="btn btn-secondary go-back-button" @click="$emit('goBack')">
      <SvgGoBack />
    </button>
    <button class="btn btn-primary" @click.once="createLink()">
      Create Link
    </button>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import LinkTypeSelect from "./LinkTypeSelect.vue";
import CheckInput from "../form/CheckInput.vue";
import RadioButtons from "../form/RadioButtons.vue";
import SvgGoBack from "../svgs/SvgGoBack.vue";
import useStoredLoadingApi from "../../managedApi/useStoredLoadingApi";
import usePopups from "../commons/Popups/usePopup";

export default defineComponent({
  setup() {
    return { ...useStoredLoadingApi({ hasFormError: true }), ...usePopups() };
  },
  name: "LinkNoteFinalize",
  props: {
    note: { type: Object as PropType<Generated.Note>, required: true },
    targetNote: { type: Object as PropType<Generated.Note>, required: true },
  },
  components: { LinkTypeSelect, SvgGoBack, CheckInput, RadioButtons },
  emits: ["success", "goBack"],
  data() {
    return {
      formData: {
        asFirstChild: false,
        typeId: undefined as number | undefined,
        moveUnder: false,
      } as Generated.LinkRequest,
      formErrors: {
        asFirstChild: undefined as string | undefined,
        typeId: undefined as string | undefined,
        moveUnder: undefined as string | undefined,
      },
    };
  },
  methods: {
    async createLink() {
      if (this.formData.moveUnder && this.note.parentId === null) {
        if (
          !(await this.popups.confirm(
            `"${this.note.title}" is a top level notebook. Do you want to move it under other notebook?`
          ))
        ) {
          return;
        }
      }
      this.storedApi
        .createLink(this.note.id, this.targetNote.id, this.formData)
        .then((r) => this.$emit("success", r.notes[0]))
        .catch((res) => {
          this.formErrors = res;
        });
    },
  },
});
</script>
