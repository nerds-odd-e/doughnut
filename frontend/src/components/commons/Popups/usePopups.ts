interface BasePopupInfo {
  // biome-ignore lint/suspicious/noExplicitAny: Popup resolve needs to accept various return types
  doneResolve: (value: any) => void
}

interface AlertPopupInfo extends BasePopupInfo {
  type: "alert"
  message: string
}

interface ConfirmPopupInfo extends BasePopupInfo {
  type: "confirm"
  message: string
}

interface OptionsPopupInfo extends BasePopupInfo {
  type: "options"
  message: string
  options: { label: string; value: string }[]
}

type PopupInfo = AlertPopupInfo | ConfirmPopupInfo | OptionsPopupInfo

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
      // For confirm: false (cancel), for alert: true (ok), for options: null (cancel)
      const result =
        topPopup.type === "confirm"
          ? false
          : topPopup.type === "alert"
            ? true
            : null
      popups.popups.done(result)
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

      options(msg: string, opts: { label: string; value: string }[]) {
        return new Promise<string | null>((resolve) => {
          push({
            type: "options",
            message: msg,
            options: opts,
            doneResolve: resolve,
          })
        })
      },

      done(result: unknown) {
        const popupInfo = Popup.popupDataWrap.popupData.popupInfo?.pop()
        if (!popupInfo) return

        if (!Popup.popupDataWrap.popupData.popupInfo?.length) {
          Popup.detachListener()
        }

        if (popupInfo.doneResolve) {
          popupInfo.doneResolve(result)
        }
      },

      peek() {
        return Popup.popupDataWrap.popupData.popupInfo
      },
    },
  }
}

export default usePopups
export type { PopupInfo, OptionsPopupInfo }
