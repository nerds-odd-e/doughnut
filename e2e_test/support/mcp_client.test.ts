import assert from 'node:assert/strict'
import { after, describe, test } from 'node:test'
import { fileURLToPath } from 'node:url'
import mcpClient from './mcp_client'

const fixturePath = fileURLToPath(
  new URL('./mcp_stdio_fixture.mjs', import.meta.url)
)

function processExists(pid: number) {
  try {
    process.kill(pid, 0)
    return true
  } catch {
    return false
  }
}

describe('McpClient disconnect', () => {
  let spawnedPid: number | undefined

  after(async () => {
    try {
      await mcpClient.disconnectMcpServer()
    } finally {
      if (spawnedPid !== undefined) {
        try {
          process.kill(spawnedPid, 'SIGKILL')
        } catch {
          // already exited
        }
      }
    }
  })

  test('spawned MCP server process exits before disconnect resolves', async () => {
    await mcpClient.spawnAndConnectMcpServer({
      baseUrl: 'http://unused.example',
      accessToken: 'unused',
      args: [fixturePath],
    })
    const pid = mcpClient.transport?.pid
    assert.ok(pid, 'connected transport should expose a child pid')
    spawnedPid = pid
    assert.equal(processExists(pid), true)

    await mcpClient.disconnectMcpServer()

    assert.equal(processExists(pid), false)
  })
})
