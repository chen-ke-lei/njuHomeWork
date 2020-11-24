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
    <span style="color: #2F4F4F; font-size: 12px; font-weight: bold">最低胜场</span>
    <el-input-number :min="0" :precision="0" size="mini" controls-position="right" placeholder="最低胜场"
                     v-model="lowerWins"/>
    <span style="color: #2F4F4F; font-size: 12px; font-weight: bold">最低场数</span>
    <el-input-number :min="200" :precision="0" size="mini" controls-position="right" placeholder="最低场数"
                     v-model="lowerMatches"/>
    <!--    <span style="color: #2F4F4F; font-size: 12px; font-weight: bold">社区发现</span>-->
    <!--    <el-switch-->
    <!--      v-model="stronglyConnected">-->
    <!--    </el-switch>-->
    <el-button
      plain
      size="mini"
      @click="this.drawGraph"
    >
      query
    </el-button>
    <div id="phGraph" style="width: 100%; height: 800px; margin-left:80px; margin-right: 80px; overflow:hidden;"/>
    <span style="color: #2F4F4F; font-size: 12px; font-weight: bold">最低胜场</span>
    <el-input-number :min="0" :precision="0" size="mini" controls-position="right" placeholder="最低胜场" v-model="lowerWins_1"/>
    <span style="color: #2F4F4F; font-size: 12px; font-weight: bold">最低场数</span>
    <el-input-number :min="200" :precision="0" size="mini" controls-position="right" placeholder="最低场数" v-model="lowerMatches_1"/>
    <el-button
      plain
      size="mini"
      @click="this.drawBar"
    >
      query
    </el-button>
    <div id="ppBar" style="width: 950px; height: 400px"/>
  </div>
</template>

<script>
  import echarts from 'echarts'

  export default {
    name: 'PlayerPlayerGraph',
    data () {
      const players = require('../assets/players')
      return {
        lowerWins: 0,
        lowerMatches: 0,
        queryPlayers: [],
        playerOptions: players,
        stronglyConnected: false,

        graph: null,
        option: null,
        vertices: [],
        edges: [],

        lowerWins_1: 0,
        lowerMatches_1: 0,

        bar: null,
        barOption: null,
        triangleCount: [],

        colors: ['gold', 'darkmagenta', 'cyan', 'darkgreen', 'tan', 'darkorange', 'darkblue', 'darkred', 'steelblue']
      }
    },
    mounted () {
      this.initGraph()
      this.initBar()
    },
    methods: {
      initGraph () {
        let dom = document.getElementById('phGraph')
        this.graph = echarts.init(dom)
        this.option = {
          title: {
            text: 'Player vs. Player Graph',
            top: 'top',
            left: 'left'
          },
          tooltip: {
            show: false
          },
          series: [
            {
              name: 'Player vs. Player',
              type: 'graph',
              layout: 'force', // 'circular'
              force: {
                // edgeLength: 15,
                repulsion: 80,
                gravity: 0.2,
                layoutAnimation: true
              },
              edges: this.edges,
              data: this.vertices,
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
              // symbolSize: 12,
              edgeSymbol: ['circle', 'arrow'],
              lineStyle: {
                width: 0.5,
                curveness: 0.3,
                opacity: 0.7,
                emphasis: {
                  width: 2,
                  color: 'source'
                }
              },
              itemStyle: {
                //鼠标放上去有阴影效果
                emphasis: {
                  shadowColor: '#3721db',
                  shadowOffsetX: 0,
                  shadowOffsetY: 0,
                  shadowBlur: 40,
                  label: {
                    show: true,
                    position: 'right',
                    formatter: function (v) {
                      let label = v.name
                      if (v.value !== -1) {
                        label += '\r\n' + 'Community: ' + v.value
                      }
                      return label
                    }
                  }
                }
              }
            }
          ]
        }
        this.graph.setOption(this.option)
      },
      drawGraph () {
        let data = {
          graph: 'player_player',
          players: this.queryPlayers.join(','),
          lowerWins: this.lowerWins,
          lowerMatches: this.lowerMatches,
          stronglyConnected: this.stronglyConnected
        }
        this.$http.get('/api/get_graph', {params: data})
          .then((res) => {
            let _this = this
            let data = res.data
            if (data.code === 'SUCCESS') {
              let graph = data.value
              this.vertices = graph.vertices.map(function (v) {
                return {
                  id: '' + v.id,
                  name: v.name,
                  value: v.community,
                  symbolSize: 12,
                  itemStyle: {
                    color: v.community === -1 ? 'darkred' : _this.colors[v.community % _this.colors.length]
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
      },
      initBar () {
        let dom = document.getElementById('ppBar')
        this.bar = echarts.init(dom)
        this.barOption = {
          title: [{
            text: '选手三角数',
            left: 'center',
            textAlign: 'center'
          }],
          tooltip: {},
          grid: [{
            top: 50,
            width: '45%',
            bottom: 0,
            left: '35%',
            containLabel: true
          }],
          xAxis: [{
            type: 'value',
            splitLine: {
              show: false
            }
          }],
          yAxis: [{
            type: 'category',
            data: this.triangleCount.map(function (item) {
              return item.name
            }),
            axisLabel: {
              interval: 0,
              rotate: 30
            },
            splitLine: {
              show: false
            }
          }],
          series: [{
            type: 'bar',
            stack: 'chart',
            z: 3,
            label: {
              normal: {
                position: 'right',
                show: true
              }
            },
            data: this.triangleCount.map(function (item) {
              return item.count
            })
          }]
        }
        this.bar.setOption(this.barOption)
      },
      drawBar () {
        let data = {
          graph: 'player_player',
          lowerWins: this.lowerWins_1,
          lowerMatches: this.lowerMatches_1
        }
        this.$http.get('/api/get_trianglecount', {params: data})
          .then((res) => {
            let data = res.data
            if (data.code === 'SUCCESS') {
              this.triangleCount = data.value
              this.triangleCount = this.triangleCount.reverse()
              this.barOption.yAxis[0].data = this.triangleCount.map(function (item) {
                return item.name
              })
              this.barOption.series[0].data = this.triangleCount.map(function (item) {
                return item.count
              })
              this.bar.setOption(this.barOption)
            }
          })
      }
    }
  }
</script>

<style scoped>

</style>
