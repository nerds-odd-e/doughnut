<template>
  <PopButton
    btn-class="btn btn-light"
    title="View Certificate"
  >
    <CertificatePopup
      :notebook-id="notebookId"
    >
    </CertificatePopup>
  </PopButton>
</template>

<script setup lang="ts">
defineProps({
  notebookId: {
    type: Number,
    required: true,
  },
})
</script>
