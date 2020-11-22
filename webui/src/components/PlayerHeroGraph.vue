<template>
  <div>
    <el-select
      v-model="queryPlayers"
      multiple
      collapse-tags
      filterable
      placeholder="请选择选手"
      size="mini"
    >
      <el-option
        v-for="item in playerOptions"
        :key="item"
        :label="item"
        :value="item">
      </el-option>
    </el-select>
    <el-select
      v-model="queryHeroes"
      multiple
      collapse-tags
      filterable
      placeholder="请选择英雄"
      size="mini"
    >
      <el-option
        v-for="item in heroOptions"
        :key="item"
        :label="item"
        :value="item">
      </el-option>
    </el-select>
    <span style="color: #2F4F4F; font-size: 12px; font-weight: bold">最低胜场</span>
    <el-input-number :min="0" :precision="0" size="mini" controls-position="right" placeholder="最低胜场" v-model="lowerWins"/>
    <span style="color: #2F4F4F; font-size: 12px; font-weight: bold">最低场数</span>
    <el-input-number :min="0" :precision="0" size="mini" controls-position="right" placeholder="最低场数" v-model="lowerMatches"/>
    <el-button
      plain
      size="mini"
      @click="this.draw"
    >
      query
    </el-button>
    <div id="phGraph" style="width: 100%; height: 900px; overflow:hidden;"></div>
  </div>
</template>

<script>
  import echarts from 'echarts'

  export default {
    name: 'PlayerHeroGraph',
    data () {
      const players = require('../assets/players')
      const heroes = require('../assets/heroes')
      return {
        lowerWins: 0,
        lowerMatches: 0,
        queryPlayers: [],
        playerOptions: players,
        queryHeroes: [],
        heroOptions: heroes,

        graph: null,
        option: null,
        vertices: [],
        edges: []
      }
    },
    mounted () {
      this.initGraph()
    },
    methods: {
      initGraph () {
        let dom = document.getElementById('phGraph')
        this.graph = echarts.init(dom)
        this.option = {
          title: {
            text: 'Player vs. Hero Graph',
            top: 'top',
            left: 'middle'
          },
          legend: [],
          tooltip: {
            show: false
          },
          series: [
            {
              name: 'Player vs. Hero',
              symbolSize: 12,
              type: 'graph',
              layout: 'force', // 'circular'
              force: {
                edgeLength: 15,
                repulsion: 100,
                gravity: 0.2,
                layoutAnimation: true
              },
              edges: this.edges,
              data: this.vertices,
              categories: null,
              roam: true,
              focusNodeAdjacency: true,
              label: {
                normal: {
                  show: true
                }
              },
              edgeLabel: {
                normal: {
                  formatter: '{c}',
                  show: true
                }
              },
              edgeSymbol: 'circle',
              lineStyle: {
                width: 0.5,
                curveness: 0.3,
                opacity: 0.7
              },
              emphasis: {
                label: {
                  position: 'right',
                  show: true
                }
              },
              itemStyle: {
                normal: {
                  color: '#6495ED'
                },
                //鼠标放上去有阴影效果
                emphasis: {
                  shadowColor: '#3721db',
                  shadowOffsetX: 0,
                  shadowOffsetY: 0,
                  shadowBlur: 40
                }
              }
            }
          ]
        }
        this.graph.setOption(this.option)
      },
      draw () {
        let data = {
          graph: 'player_hero',
          players: this.queryPlayers.join(','),
          heroes: this.queryHeroes.join(','),
          lowerWins: this.lowerWins,
          lowerMatches: this.lowerMatches
        }
        this.$http.get('/api/get_graph', {params: data})
          .then((res) => {
            let data = res.data
            if (data.code === 'SUCCESS') {
              let graph = data.value
              this.vertices = graph.vertices.map(function (v) {
                return {
                  id: '' + v.id,
                  name: v.name,
                  itemStyle: {
                    color: v.type === 'player' ? 'darkred' : 'darkblue'
                  }
                }
              })
              this.edges = graph.edges.map(function (e) {
                return {
                  source: '' + e.srcId,
                  target: '' + e.dstId,
                  value: e.wins + '/' + e.matches
                }
              })
              this.option.series[0].data = this.vertices
              this.option.series[0].edges = this.edges
              this.graph.setOption(this.option)
            }
          })
      }
    }
  }
</script>

<style scoped>

</style>
