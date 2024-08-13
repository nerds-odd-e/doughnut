<template>
  <ContainerPage
    v-bind="{
      contentExists: true,
      title: 'Welcome To Assessment History',
    }"
  >
    <div>
      <AssessmentHistoryTable />
    </div>
  </ContainerPage>
</template>

<script lang="ts">
import { PropType, defineComponent } from "vue"
import { User } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import AssessmentHistoryTable from "@/components/assessments/AssessmentHistory.vue"
import ContainerPage from "./commons/ContainerPage.vue"

export default defineComponent({
  setup() {
    return useLoadingApi()
  },
  components: { ContainerPage, AssessmentHistoryTable },
  props: {
    user: {
      type: Object as PropType<User>,
      required: false,
    },
  },
})
</script>
