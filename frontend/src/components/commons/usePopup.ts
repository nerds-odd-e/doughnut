import { Slot } from "vue"

interface PopupInfo {
  type: 'alert' | 'confirm' | 'dialog'
  message?: string
  doneResolve: ((value: unknown) => void) | ((value: boolean) => void)
  component?: string
  slot?: Slot
  attrs?: unknown
}

class Popup {
  static popupDataWrap = {
    popupData: {} as { popupInfo?: PopupInfo }
  }
}

function usePopups() {
  const setPopupInfo = (info?: PopupInfo) => {
    Popup.popupDataWrap.popupData.popupInfo = info
  }
  return {
    popups: {
      register(data: {popupInfo?: PopupInfo}) {
        Popup.popupDataWrap.popupData = data
      },

      alert(msg: string) {
        return new Promise<boolean>((resolve) => {
          setPopupInfo({ type: "alert", message: msg, doneResolve: resolve });
        });
      },

      confirm(msg: string) {
        return new Promise<boolean>((resolve) => {
          setPopupInfo({ type: "confirm", message: msg, doneResolve: resolve });
        });
      },

      dialog(component: any, attrs: unknown) {
        return new Promise((resolve) => {
          setPopupInfo({ type: "dialog", component, attrs, doneResolve: resolve });
        });
      },

      dialog1(slot?: Slot) {
        return new Promise((resolve) => {
          setPopupInfo({ type: "dialog", slot, doneResolve: resolve });
        });
      },

      done(result: unknown) {
        const {popupInfo} = Popup.popupDataWrap.popupData
        if (!popupInfo) return
        if (popupInfo.doneResolve) popupInfo.doneResolve(result as boolean);
        setPopupInfo(undefined);
      }
    }
  }
}

export default usePopups
export { PopupInfo }