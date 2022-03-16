import { App } from 'vue';

function createPopup() {
  return {
    install(app: App) {
      if(!app.config.globalProperties.$popups) {
        app.config.globalProperties.$popups = {};
      }
    }

  }
}

function usePopups() {
}

export default createPopup
export { usePopups }