import { vi } from "vitest"

/**
 * Manual mock picked up by `vi.mock("@/components/commons/Popups/usePopups")`
 * with no factory. Most callers only need popups out of the render path;
 * specs that assert on alert/confirm/options still override via
 * `vi.mocked(usePopups).mockReturnValue(...)`.
 */
const usePopups = vi.fn(() => ({
  popups: {
    register: vi.fn(),
    alert: vi.fn().mockResolvedValue(true),
    confirm: vi.fn().mockResolvedValue(true),
    options: vi.fn().mockResolvedValue(null),
    done: vi.fn(),
    peek: vi.fn(),
  },
}))

export default usePopups
