import Vue from 'vue'
import Router from 'vue-router'
import DashBoard from '@/views/DashBoard'
import StreamDashBoard from '@/components/StreamDashBoard'
import PHGraphDashBoard from '@/components/PHGraphDashBoard'
import PPGraphDashBoard from '@/components/PPGraphDashBoard'

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
        },
        {
          path: 'ph_graph',
          name: 'PHGraphDashBoard',
          component: PHGraphDashBoard
        },
        {
          path: 'pp_graph',
          name: 'PPGraphDashBoard',
          component: PPGraphDashBoard
        }
      ]
    }
  ]
})
