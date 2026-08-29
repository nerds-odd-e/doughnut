import {
  browserLocation,
  healthcheckPing,
  signInRedirectHref,
} from "./signInRedirect"

const loginOrRegisterAndHaltThisThread = async () => {
  browserLocation.assign(
    signInRedirectHref(window.location.href, await healthcheckPing())
  )
  // eslint-disable-next-line @typescript-eslint/no-empty-function
  await new Promise(() => {
    // noop
  }) // I promise ... Wait, why am I still here?
}

export default loginOrRegisterAndHaltThisThread
