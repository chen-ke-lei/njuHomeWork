import Vue from 'vue'
import Router from 'vue-router'
import DashBoard from '@/views/DashBoard'
import StreamDashBoard from '@/components/StreamDashBoard'

Vue.use(Router)

export default new Router({
  routes: [
    {
      path: '/',
      component: DashBoard,
      children: [
        {
          path: '',
          name: 'StreamDashBoard',
          component: StreamDashBoard
        }
      ]
    }
  ]
})
