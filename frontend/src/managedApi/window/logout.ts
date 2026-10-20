export const logout = async () => {
  await fetch("/logout", {
    method: "POST",
  })
  window.location.href = "/bazaar"
}
