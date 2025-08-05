import { createRouter, createWebHashHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'

const routes = [
  {
    path: '/',
    name: 'home',
    component: HomeView,
    children: [
      {
        path: '/pageBeanHistory',
        name: 'pageBeanHistory',
        component: () => import('../views/HomeViewChildren/PageBeanHistoryView.vue')
      }
    ]
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue')
  },
  {
    path: '/register',
    name:'register',
    component: () => import('../views/RegisterView.vue')
  },
  {
    path: '/retrieve',
    name: 'retrieve',
    component: () => import('../views/RetrieveView.vue')
  },
  {
    path: '/ddz',
    name: 'ddz',
    component: () => import('../views/game/DDZView.vue')
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
