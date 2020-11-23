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
    <el-input-number :min="35" :precision="0" size="mini" controls-position="right" placeholder="最低场数" v-model="lowerMatches"/>
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
    <el-input-number :min="0" :precision="0" size="mini" controls-position="right" placeholder="最低场数" v-model="lowerMatches_1"/>
    <el-button
      plain
      size="mini"
      @click="this.drawBar"
    >
      query
    </el-button>
    <div id="phBar" style="width: 950px; height: 400px"/>
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
        edges: [],

        lowerWins_1: 0,
        lowerMatches_1: 0,

        bar: null,
        barOption: null,
        playerDegrees: [],
        hereDegrees: [],
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
            text: 'Player vs. Hero Graph',
            top: 'top',
            left: 'left'
          },
          color: ['darkred', 'darkblue'],
          legend: {data: ['player', 'hero']},
          tooltip: {
            show: false
          },
          series: [
            {
              name: 'Player vs. Hero',
              categories: [{name: 'player'}, {name: 'hero'}],
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
              // edgeSymbol: 'circle',
              lineStyle: {
                width: 0.5,
                curveness: 0.3,
                opacity: 0.7,
                emphasis: {
                  width: 5,
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
                      return v.name + '\r\n' + 'PR: '+  v.value.toFixed(2)
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
                  value: v.PR,
                  symbolSize: v.type === 'player' ? v.PR * 30 : v.PR * 3 / 2,
                  category: v.type === 'player' ? 0 : 1,
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
      },
      initBar () {
        let dom = document.getElementById('phBar')
        this.bar = echarts.init(dom)
        this.barOption = {
          title: [{
            text: '选手/英雄顶点度数',
            left: 'center',
            textAlign: 'center'
          }],
          tooltip: {},
          color: ['darkred', 'darkblue'],
          legend: {data: ['player', 'hero']},
          grid: [{
            top: 50,
            width: '45%',
            bottom: 0,
            left: 20,
            containLabel: true
          }, {
            top: 50,
            width: '45%',
            bottom: 0,
            left: '50%',
            containLabel: true
          }],
          xAxis: [{
            type: 'value',
            splitLine: {
              show: false
            }
          }, {
            type: 'value',
            gridIndex: 1,
            splitLine: {
              show: false
            }
          }],
          yAxis: [{
            type: 'category',
            data: this.playerDegrees.map(function (item) {
              return item.name
            }),
            axisLabel: {
              interval: 0,
              rotate: 30
            },
            splitLine: {
              show: false
            }
          }, {
            gridIndex: 1,
            type: 'category',
            data: this.hereDegrees.map(function (item) {
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
            data: this.playerDegrees.map(function (item) {
              return item.outDegrees
            })
          }, {
            type: 'bar',
            stack: 'component',
            xAxisIndex: 1,
            yAxisIndex: 1,
            z: 3,
            label: {
              normal: {
                position: 'right',
                show: true
              }
            },
            data: this.hereDegrees.map(function (item) {
              return item.inDegrees
            })
          }]
        }
        this.bar.setOption(this.barOption)
      },
      drawBar () {
        let data = {
          graph: 'player_hero',
          lowerWins: this.lowerWins_1,
          lowerMatches: this.lowerMatches_1
        }
        this.$http.get('/api/get_degrees', {params: data})
          .then((res) => {
            let data = res.data
            if (data.code === 'SUCCESS') {
              let degrees = data.value
              this.playerDegrees = degrees.player
              this.hereDegrees = degrees.hero
              this.playerDegrees = this.playerDegrees.reverse()
              this.hereDegrees = this.hereDegrees.reverse()
              this.barOption.yAxis[0].data = this.playerDegrees.map(function (item) {
                return item.name
              })
              this.barOption.yAxis[1].data = this.hereDegrees.map(function (item) {
                return item.name
              })
              this.barOption.series[0].data = this.playerDegrees.map(function (item) {
                return item.outDegrees
              })
              this.barOption.series[1].data = this.hereDegrees.map(function (item) {
                return item.inDegrees
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
