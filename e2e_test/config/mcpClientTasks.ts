import { existsSync } from 'node:fs'
import { join } from 'node:path'
import mcpClient from '../support/mcp_client'
import { CLI_E2E_PNPM_SPAWN_ENV, runShellCommandSync } from './cliE2eRepo'
import { E2E_APP_BASE_URL } from './constants'

export function mcpClientTasks(repoRoot: string) {
  return {
    mcpClientConnectionInfo() {
      return mcpClient.connectionInfo()
    },
    async spawnAndConnectMcpServer({
      baseUrl,
      accessToken,
    }: {
      baseUrl: string
      accessToken: string
    }) {
      const apiBaseUrl =
        baseUrl && baseUrl !== 'undefined' ? baseUrl : E2E_APP_BASE_URL
      return await mcpClient.spawnAndConnectMcpServer({
        baseUrl: apiBaseUrl,
        accessToken,
      })
    },
    async callMcpToolWithParams({
      apiName,
      params,
    }: {
      apiName: string
      params: Record<string, any>
    }) {
      return await mcpClient.callMcpToolWithParams({ apiName, params })
    },
    async disconnectMcpServer() {
      return await mcpClient.disconnectMcpServer()
    },
    async bundleMcpServer() {
      const mcpServerDir = join(repoRoot, 'mcp-server')
      const bundlePath = join(mcpServerDir, 'dist', 'mcp-server.bundle.mjs')
      if (existsSync(bundlePath)) {
        return true
      }
      try {
        runShellCommandSync('pnpm bundle', {
          cwd: mcpServerDir,
          env: {
            ...process.env,
            ...CLI_E2E_PNPM_SPAWN_ENV,
          },
        })
        return true
      } catch (error) {
        console.error('Failed to bundle MCP server:', error)
        throw error
      }
    },
  }
}
