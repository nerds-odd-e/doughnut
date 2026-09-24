// Decide whether a reconciled candidate may be pushed: held proof, caller
// validation, or an explicit needs-validation / validation-failed stop.
export function validationAccepted(result) {
  if (result === true) return true;
  if (result && typeof result === "object" && result.ok === true) return true;
  return false;
}

export async function ensureApplicableProof({
  validate,
  candidate,
  context,
  proofAlreadyHeld,
}) {
  if (proofAlreadyHeld) {
    return { ok: true };
  }
  if (!validate) {
    return { ok: false, status: "needs-validation" };
  }
  const result = await validate(candidate, context);
  if (validationAccepted(result)) {
    return { ok: true };
  }
  return { ok: false, status: "validation-failed", validation: result };
}

export function stopped(status, fields) {
  return {
    ok: false,
    publication: "stopped",
    status,
    receipt: null,
    ...fields,
  };
}

export function needsValidation(fields) {
  return {
    ok: false,
    publication: "reconciled",
    status: "needs-validation",
    receipt: null,
    ...fields,
  };
}

export function proofGateResult(gate, fields) {
  if (gate.status === "needs-validation") {
    return needsValidation(fields);
  }
  return stopped("validation-failed", {
    ...fields,
    validation: gate.validation,
  });
}
