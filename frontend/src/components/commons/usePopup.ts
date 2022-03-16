import useStore from "../../store/createPiniaStore";

function usePopups() {
    const piniaStore = useStore();
    const popups = {
      alert(msg: string) {
        return new Promise((resolve, reject) => {
          piniaStore.popupInfo = { type: "alert", message: msg, doneResolve: resolve };
        });
      },

      confirm(msg: string) {
        return new Promise((resolve, reject) => {
          piniaStore.popupInfo = { type: "confirm", message: msg, doneResolve: resolve };
        });
      },

      dialog(component: any, attrs: unknown) {
        return new Promise((resolve, reject) => {
          piniaStore.popupInfo = { type: "dialog", component, attrs, doneResolve: resolve };
        });
      }
    }
    return { piniaStore, popups}
}

export default usePopups