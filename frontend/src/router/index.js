import Vue from 'vue'
import VueRouter from 'vue-router'
import TaskList from '../views/TaskList.vue'
import TaskDetail from '../views/TaskDetail.vue'
import CacheData from '../views/CacheData.vue'

Vue.use(VueRouter)

const routes = [
  {
    path: '/',
    name: 'TaskList',
    component: TaskList
  },
  {
    path: '/task/:id',
    name: 'TaskDetail',
    component: TaskDetail
  },
  {
    path: '/cache',
    name: 'CacheData',
    component: CacheData
  }
]

const router = new VueRouter({
  mode: 'history',
  base: process.env.BASE_URL,
  routes
})

export default router
