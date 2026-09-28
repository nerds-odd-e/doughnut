export const browserLocation = {
  assign(url: string) {
    window.location.href = url
  },
  reload() {
    window.location.reload()
  },
}
