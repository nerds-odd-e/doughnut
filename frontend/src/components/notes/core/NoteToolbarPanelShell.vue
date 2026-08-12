<template>
  <div class="note-toolbar-panel-shell bg-dark" data-testid="note-toolbar-panel-shell">
    <slot />
  </div>
</template>

<style scoped>
.note-toolbar-panel-shell {
  position: relative;
  border-radius: 0 0 12px 12px;
  padding: 20px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  animation: dropDown 0.3s ease-out;
  transform-origin: top;
}

@keyframes dropDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 480px) {
  .note-toolbar-panel-shell {
    padding: 15px;
  }
}
</style>
