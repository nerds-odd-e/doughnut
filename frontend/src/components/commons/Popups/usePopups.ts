interface PopupInfo {
  type: "alert" | "confirm" | "dialog"
  message?: string
  doneResolve: ((value: unknown) => void) | ((value: boolean) => void)
}

class Popup {
  static popupDataWrap = {
    popupData: {
      popupInfo: [] as PopupInfo[],
    },
  }

  static isListenerAttached = false

  static handleEscape = (event: KeyboardEvent) => {
    if (event.key === "Escape") {
      const popupInfo = Popup.popupDataWrap.popupData.popupInfo
      if (!popupInfo?.length) return

      event.preventDefault()
      event.stopPropagation()

      const topPopup = popupInfo[popupInfo.length - 1]
      if (!topPopup) return

      const popups = usePopups()
      popups.popups.done(topPopup.type !== "confirm")
    }
  }

  static attachListener() {
    if (!this.isListenerAttached) {
      document.addEventListener("keydown", this.handleEscape, true) // Use capture phase
      this.isListenerAttached = true
    }
  }

  static detachListener() {
    if (this.isListenerAttached) {
      document.removeEventListener("keydown", this.handleEscape, true)
      this.isListenerAttached = false
    }
  }
}

function usePopups() {
  const push = (info: PopupInfo) => {
    if (!Popup.popupDataWrap.popupData.popupInfo) {
      Popup.popupDataWrap.popupData.popupInfo = []
    }
    Popup.popupDataWrap.popupData.popupInfo.push(info)
    Popup.attachListener()
  }

  return {
    popups: {
      register(data: { popupInfo: PopupInfo[] }) {
        Popup.popupDataWrap.popupData = data
        if (data.popupInfo?.length > 0) {
          Popup.attachListener()
        }
      },

      alert(msg: string) {
        return new Promise<boolean>((resolve) => {
          push({ type: "alert", message: msg, doneResolve: resolve })
        })
      },

      confirm(msg: string) {
        return new Promise<boolean>((resolve) => {
          push({ type: "confirm", message: msg, doneResolve: resolve })
        })
      },

      done(result: unknown) {
        const popupInfo = Popup.popupDataWrap.popupData.popupInfo?.pop()
        if (!popupInfo) return

        if (!Popup.popupDataWrap.popupData.popupInfo?.length) {
          Popup.detachListener()
        }

        if (popupInfo.doneResolve) {
          popupInfo.doneResolve(result as boolean)
        }
      },

      peek() {
        return Popup.popupDataWrap.popupData.popupInfo
      },
    },
  }
}

export default usePopups
export type { PopupInfo }
