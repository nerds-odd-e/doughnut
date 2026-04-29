/** Visible for unit tests; keeps sign-in return URL construction in one place. */
export const signInRedirectHref = (fromHref: string) =>
  `/users/identify?from=${fromHref}`

const loginOrRegisterAndHaltThisThread = async () => {
  window.location.href = signInRedirectHref(window.location.href)
  // eslint-disable-next-line @typescript-eslint/no-empty-function
  await new Promise(() => {
    // noop
  }) // I promise ... Wait, why am I still here?
}

export default loginOrRegisterAndHaltThisThread
