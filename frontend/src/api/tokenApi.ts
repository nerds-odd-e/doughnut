import { DoughnutApi } from '@/generated/backend'
import type { Token } from '@/types/token'

export const useTokenApi = () => {
  const api = new DoughnutApi()

  return {
    async generateToken(userId: number): Promise<Token> {
      return await api.restUserController.createUserToken(userId)
    },

    async deleteToken(userId: number): Promise<void> {
      await api.restUserController.deleteUserToken(userId)
    },

    async getTokens(userId: number): Promise<Token[]> {
      const response = await api.restUserController.getUserTokens(userId)
      return response
    }
  }
} 