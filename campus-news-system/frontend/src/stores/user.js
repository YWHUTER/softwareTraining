import { defineStore } from 'pinia'
import { login as loginApi, register as registerApi, getUserInfo } from '@/api/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: JSON.parse(localStorage.getItem('user') || 'null')
  }),
  
  getters: {
    isLogin: (state) => !!state.token,
    isAdmin: (state) => {
      return state.user?.roles?.some(role => role.roleName === 'ADMIN') || false
    },
    isTeacher: (state) => {
      return state.user?.roles?.some(role => role.roleName === 'TEACHER') || false
    },
    isStudent: (state) => {
      return state.user?.roles?.some(role => role.roleName === 'STUDENT') || false
    }
  },
  
  actions: {
    async login(loginForm) {
      const data = await loginApi(loginForm)
      this.token = data.token
      this.user = data.user
      localStorage.setItem('token', data.token)
      localStorage.setItem('user', JSON.stringify(data.user))
    },
    
    async register(registerForm) {
      const data = await registerApi(registerForm)
      this.token = data.token
      this.user = data.user
      localStorage.setItem('token', data.token)
      localStorage.setItem('user', JSON.stringify(data.user))
    },
    
    async fetchUserInfo() {
      const data = await getUserInfo()
      this.user = data
      localStorage.setItem('user', JSON.stringify(data))
    },
    
    setUser(user) {
      this.user = user
      localStorage.setItem('user', JSON.stringify(user))
    },
    
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }
  }
})
