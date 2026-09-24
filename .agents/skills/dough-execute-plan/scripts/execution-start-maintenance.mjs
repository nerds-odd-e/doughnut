import { refreshDefaultCheckout } from "./maintain-default-checkout.mjs";
import { remoteOf } from "./workspace-publication-ownership.mjs";

// Startup reports a local refresh failure separately from remote acceptance.
export async function maintenance(request) {
  try {
    return await refreshDefaultCheckout({
      checkout: request.integration,
      declaredOwner: request.declaredOwner,
      requester: request.requester,
      remote: remoteOf(request),
      integrationBranch: request.target,
    });
  } catch (error) {
    return {
      result: "deferred",
      reason: "refresh-failed",
      error: error.stderr || error.message,
    };
  }
}
