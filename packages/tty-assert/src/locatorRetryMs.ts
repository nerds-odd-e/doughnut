/**
 * Default delay between poll attempts when `timeoutMs` is positive; Cypress adapter
 * should use the same value for `cy.wait` between buffer re-reads.
 *
 * Lives in a dependency-free module so browser-bundled E2E code can import it without
 * pulling in PTY/xterm/Node builtins.
 */
export const TTY_ASSERT_LOCATOR_DEFAULT_RETRY_MS = 50
