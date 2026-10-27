import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import {
  getListenerPids,
  isOwnedByApplicationTree,
} from '../../../scripts/sut-listener-pids.mjs'
import { verifyLiveSutOwner } from '../../../scripts/sut-owner.mjs'

const alive = (pid) => {
  try {
    process.kill(pid, 0)
    return true
  } catch {
    return false
  }
}

export async function createProcessDurabilityProof({
  root,
  tlsProxy,
  target,
  restartApplication,
  bearerFetch,
  evidence,
}) {
  const origin = tlsProxy.httpsOrigin
  const resource = `${origin}/api/mcp`
  const databaseUrl = target.databaseUrl
  assert.match(target.database, /^doughnut_e2e_wt_/)
  let finalPids = []
  const discovery = async () => {
    const result = []
    for (const route of [
      '/.well-known/oauth-authorization-server',
      '/.well-known/oauth-protected-resource/api/mcp',
    ]) {
      const reply = await bearerFetch(`${origin}${route}`)
      assert.equal(reply.status, 200)
      result.push(await reply.json())
    }
    assert.equal(result[0].issuer, origin)
    assert.equal(result[1].resource, resource)
    return result
  }
  const metadata = await discovery()
  const replace = async () => {
    const before = await verifyLiveSutOwner(root)
    const [oldJvm] = await getListenerPids(target.backendPort)
    assert.ok(oldJvm)
    assert.match(
      execFileSync('ps', ['-p', String(oldJvm), '-o', 'comm='], {
        encoding: 'utf8',
      }),
      /java/
    )
    assert.equal(
      await isOwnedByApplicationTree(oldJvm, before.applicationGroupId),
      true
    )
    const restarted = await restartApplication()
    assert.equal(restarted.ok, true)
    const after = await verifyLiveSutOwner(root)
    const newPids = await getListenerPids(target.backendPort)
    assert.equal(newPids.length, 1)
    const [newJvm] = newPids
    assert.equal(alive(oldJvm), false)
    assert.notEqual(newJvm, oldJvm)
    assert.match(
      execFileSync('ps', ['-p', String(newJvm), '-o', 'comm='], {
        encoding: 'utf8',
      }),
      /java/
    )
    assert.equal(
      await isOwnedByApplicationTree(newJvm, after.applicationGroupId),
      true
    )
    assert.equal(after.pid, before.pid)
    assert.notEqual(after.applicationGroupId, before.applicationGroupId)
    assert.equal(after.applicationGroupId, restarted.applicationGroupId)
    assert.equal(after.runnerLeaseHeld, true)
    assert.equal(tlsProxy.httpsOrigin, origin)
    assert.equal(target.databaseUrl, databaseUrl)
    assert.deepEqual(await discovery(), metadata)
    evidence.replacements.push({
      oldJvm,
      newJvm,
      supervisor: after.pid,
      oldGroup: before.applicationGroupId,
      newGroup: after.applicationGroupId,
    })
    finalPids = [after.pid, newJvm]
    console.log(
      JSON.stringify({
        replacement: evidence.replacements.length,
        oldJvm,
        newJvm,
        stableOriginAndDiscovery: true,
        database: target.database,
      })
    )
  }
  return {
    replace,
    assertClosed: async () => {
      assert.equal(
        finalPids.every((pid) => !alive(pid)),
        true
      )
      assert.equal((await verifyLiveSutOwner(root)).ok, false)
      await assert.rejects(tlsProxy.trustedFetch(`${tlsProxy.httpsOrigin}/`))
    },
  }
}
