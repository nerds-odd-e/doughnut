import { mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { spawnSync } from 'node:child_process'

export type McpTlsMaterial = {
  caCertPem: string
  serverKeyPem: string
  serverCertPem: string
}

/**
 * Ephemeral test CA + leaf cert for the MCP HTTPS E2E fixture.
 * SAN covers localhost and 127.0.0.1; clients must verify with caCertPem only.
 */
export function generateMcpTestTlsMaterial(): McpTlsMaterial {
  const dir = mkdtempSync(join(tmpdir(), 'donut-mcp-tls-'))
  try {
    const caKey = join(dir, 'ca.key')
    const caCert = join(dir, 'ca.crt')
    const serverKey = join(dir, 'server.key')
    const serverCsr = join(dir, 'server.csr')
    const serverCert = join(dir, 'server.crt')
    const ext = join(dir, 'server.ext')
    writeFileSync(
      ext,
      [
        'basicConstraints=CA:FALSE',
        'keyUsage=digitalSignature,keyEncipherment',
        'extendedKeyUsage=serverAuth',
        'subjectAltName=DNS:localhost,IP:127.0.0.1',
        '',
      ].join('\n')
    )
    runOpenSsl([
      'req',
      '-x509',
      '-newkey',
      'rsa:2048',
      '-nodes',
      '-keyout',
      caKey,
      '-out',
      caCert,
      '-days',
      '1',
      '-subj',
      '/CN=Donut MCP E2E Test CA',
    ])
    runOpenSsl([
      'req',
      '-newkey',
      'rsa:2048',
      '-nodes',
      '-keyout',
      serverKey,
      '-out',
      serverCsr,
      '-subj',
      '/CN=localhost',
    ])
    runOpenSsl([
      'x509',
      '-req',
      '-in',
      serverCsr,
      '-CA',
      caCert,
      '-CAkey',
      caKey,
      '-CAcreateserial',
      '-out',
      serverCert,
      '-days',
      '1',
      '-extfile',
      ext,
    ])
    return {
      caCertPem: readFileSync(caCert, 'utf8'),
      serverKeyPem: readFileSync(serverKey, 'utf8'),
      serverCertPem: readFileSync(serverCert, 'utf8'),
    }
  } finally {
    rmSync(dir, { recursive: true, force: true })
  }
}

function runOpenSsl(args: string[]) {
  const result = spawnSync('openssl', args, { encoding: 'utf8' })
  if (result.status !== 0) {
    throw new Error(
      `openssl ${args.join(' ')} failed: ${result.stderr || result.stdout || result.error}`
    )
  }
}
