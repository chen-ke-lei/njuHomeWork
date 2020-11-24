<template>
  <div id="heroMatches">
    <el-date-picker
      v-model="queryDates"
      type="daterange"
      format="yyyy-MM-dd"
      value-format="yyyyMMdd"
      range-separator="至"
      start-placeholder="起始日期"
      end-placeholder="结束日期"
      :editable=true
      :picker-options="yearOptions"
      size="small"
    >
    </el-date-picker>
    <br/>
    <el-button
      plain
      icon="el-icon-video-play"
      size="mini"
      @click="this.start"
    >
      start
    </el-button>
    <el-button
      plain
      icon="el-icon-video-pause"
      size="mini"
      @click="this.stop"
    >
      stop
    </el-button>
    <br/>
    <div style="padding-left: 100px; float: left">
      <span style="color: #2F4F4F; font-size: 12px; font-weight: bold">英雄出场次数</span>
    </div>
  </div>
</template>

<script>
  /**
   * @original author: TangliziGit
   * @original project url: https://github.com/TangliziGit/ColumnsAnimation
   */
  import * as d3 from 'd3'
  import $ from 'jquery'

  const config = {
    SVGWidth: 650,
    SVGHeight: 400,
    TimerFontSize: '25px',
    MaxNumber: 10,
    Padding: {
      left: 100,
      right: 80,
      top: 20,
      bottom: 0
    },
    ColorClass: ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I'],
    IntervalTime: 0.33333,
    XTicks: 10
  }

  const innerWidth = config.SVGWidth - config.Padding.left - config.Padding.right
  const innerHeight = config.SVGHeight - config.Padding.top - config.Padding.bottom - 50
  const intervalTime = config.IntervalTime

  const tValue = d => d.updateTime
  const xValue = d => Number(d.playNum)
  const yValue = d => d.hero

  export default {
    name: 'HeroMatches',
    mounted () {
      this.initChart()
      this.createWs()
    },
    beforeDestroy () {
      if (this.bufferTimer) window.clearInterval(this.bufferTimer)
    },
    data () {
      const dateShortcuts = []
      for (let i = 2020; i > 2010; i--) {
        let option = {}
        option['text'] = i + ''
        option['onClick'] = function (picker) {
          const start = new Date(i, 0, 1)
          const end = new Date(i, 11, 31)
          picker.$emit('pick', [start, end])
        }
        dateShortcuts.push(option)
      }
      return {
        queryDates: [],
        yearOptions: {shortcuts: dateShortcuts},

        topic: 'heroMatches',
        groupId: new Date().getTime(),

        mini: 0,
        bufferTimer: '',
        heroOnBoard: {},
        dataBuffer: [],
        dataOnBoard: [],

        xAxis: null,
        yAxis: null,
        xScale: null,
        yScale: null,
        g: null,
        xAxisG: null,
        yAxisG: null,
        timerCounter: 0,
        timer: null
      }
    },
    methods: {
      mock () {
        let iter = 0
        let _this = this
        let inter = setInterval(function next () {
          iter++
          let num = Math.floor(Math.random() * 100)
          if (_this.dataOnBoard.length >= config.MaxNumber) {
            if (num > _this.mini) {
              _this.dataOnBoard.pop()
              _this.dataOnBoard.push({'hero': String(iter), 'playNum': num})
              _this.dataOnBoard.sort((x, y) => xValue(y) - xValue(x))
              _this.mini = xValue(_this.dataOnBoard[_this.dataOnBoard.length - 1])
            }
          } else {
            _this.dataOnBoard.push({'hero': String(iter), 'playNum': num})
            _this.dataOnBoard.sort((x, y) => xValue(y) - xValue(x))
            _this.mini = xValue(_this.dataOnBoard[_this.dataOnBoard.length - 1])
          }
          _this.timerCounter = iter
          _this.refresh(_this.dataOnBoard)
          if (iter >= 100) {
            window.clearInterval(inter)
          }
        }, 3000 * intervalTime)
      },
      processBuffer () {
        if (this.dataBuffer.length > 0) {
          let batch = this.dataBuffer.length >= 30 ? 30 : this.dataBuffer.length
          for (let i = 0; i < batch; i++) {
            let heroMatches = this.dataBuffer[0]
            this.dataBuffer.shift()

            // 在榜且值更大
            if (this.heroOnBoard[yValue(heroMatches)] && this.heroOnBoard[yValue(heroMatches)] >= xValue(heroMatches)) {
              continue
            }
            // 在榜但值较小
            if (this.heroOnBoard[yValue(heroMatches)] && this.heroOnBoard[yValue(heroMatches)] < xValue(heroMatches)) {
              // 更新值
              for (let ind = 0; ind < this.dataOnBoard.length; ind++) {
                if (yValue(this.dataOnBoard[ind]) === yValue(heroMatches)) {
                  this.dataOnBoard[ind].playNum = xValue(heroMatches)
                  this.heroOnBoard[yValue(heroMatches)] = xValue(heroMatches)
                  break
                }
              }
            }
            // 不在榜
            else if (!this.heroOnBoard[yValue(heroMatches)]) {
              if (this.dataOnBoard.length >= config.MaxNumber) {
                if (xValue(heroMatches) > this.mini) {
                  let popped = this.dataOnBoard.pop()
                  delete this.heroOnBoard[yValue(popped)]
                  this.dataOnBoard.push(heroMatches)
                  this.heroOnBoard[yValue(heroMatches)] = xValue(heroMatches)
                }
              } else {
                this.dataOnBoard.push(heroMatches)
                this.heroOnBoard[yValue(heroMatches)] = xValue(heroMatches)
              }
            }
            this.dataOnBoard.sort((x, y) => xValue(y) - xValue(x))
            this.timerCounter = tValue(heroMatches)
            this.mini = xValue(this.dataOnBoard[this.dataOnBoard.length - 1])
          }
        }
        this.refresh(this.dataOnBoard)
      },
      getColorClass (d) {
        let tmp = 0
        for (let index = 0; index < yValue(d).length; index++) {
          tmp = tmp + yValue(d).charCodeAt(index)
        }
        return config.ColorClass[tmp % config.ColorClass.length]
      },
      initChart () {
        const svg = d3.select('#heroMatches').append('svg')
          .attr('width', config.SVGWidth)
          .attr('height', config.SVGHeight)

        this.g = svg.append('g')
          .attr('transform', `translate(${config.Padding.left}, ${config.Padding.top})`)
        this.xAxisG = this.g.append('g')
          .attr('transform', `translate(0, ${innerHeight})`)
        this.yAxisG = this.g.append('g')

        this.xAxisG.append('text')
          .attr('class', 'axis-label')
          .attr('x', innerWidth / 2)
          .attr('y', 100)

        this.xScale = d3.scaleLinear()
        this.yScale = d3.scaleBand()
          .paddingInner(0.3)
          .paddingOuter(0)

        this.xAxis = d3.axisBottom()
          .scale(this.xScale)
          .ticks(config.XTicks)
          .tickPadding(20)
          .tickFormat(d => d)
          .tickSize(-innerHeight)
        this.yAxis = d3.axisLeft()
          .scale(this.yScale)
          .tickPadding(5)
          .tickSize(-innerWidth)

        this.timer = this.g.append('text')
          .attr('class', 'timer')
          .attr('font-size', config.TimerFontSize)
          .attr('fill-opacity', 0)
          .attr('x', innerWidth - 40)
          .attr('y', innerHeight)
      },
      refresh (data) {
        let _this = this
        this.xScale
          .domain([0, d3.max(data, xValue)])
          .range([0, innerWidth])
        this.yScale
          .domain(data.map(yValue).reverse())
          .range([innerHeight, 0])
        this.xAxisG
          .transition(_this.g)
          .duration(3000 * intervalTime)
          .ease(d3.easeLinear)
          .call(_this.xAxis)
        this.yAxisG
          .transition(_this.g)
          .duration(3000 * intervalTime)
          .ease(d3.easeLinear)
          .call(_this.yAxis)
        this.yAxisG.selectAll('.tick').remove()
        this.timer.data(data)
          .transition().duration(3000 * intervalTime)//.delay(1000 * intervalTime * this.isFirst)
          .attr('fill-opacity', 1)
          .tween('text', function (d) {
            let self = this
            let i = d3.interpolate(self.textContent, _this.timerCounter)
            return function (t) {
              self.textContent = Math.round(i(t))
            }
          })

        // start
        let bar = this.g.selectAll('.bar').data(data, yValue)

        // Enter Items
        let barEnter = bar.enter().insert('g', '.axis')
          .attr('class', 'bar')
          .attr('transform', function (d) {
            return 'translate(0,' + _this.yScale(yValue(d)) + ')'
          })
        barEnter.append('g').attr('class', function (d) {
          return _this.getColorClass(d)
        })
        barEnter.append('rect')
          .attr('width', d => _this.xScale(xValue(d)))
          .attr('fill-opacity', 0)
          .attr('height', 26).attr('y', 50)
          .transition('a')
          .attr('class', d => _this.getColorClass(d))
          .delay(500 * intervalTime)
          .duration(2490 * intervalTime)
          .attr('y', 0)
          .attr('width', d => _this.xScale(xValue(d)))
          .attr('fill-opacity', 1)
        barEnter.append('text')
          .attr('y', 50)
          .attr('fill-opacity', 0)
          .transition('2')
          .delay(500 * intervalTime)
          .duration(2490 * intervalTime)
          .attr('fill-opacity', 1)
          .attr('y', 0)
          .attr('class', function (d) {
            return 'label ' + _this.getColorClass(d)
          })
          .attr('x', -5)
          .attr('y', 20)
          .attr('text-anchor', 'end')
          .text(yValue)
        barEnter.append('text')
          .attr('x', d => _this.xScale(xValue(d)))
          .attr('y', 50)
          .attr('fill-opacity', 0)
          .text(xValue)
          .transition()
          .delay(500 * intervalTime)
          .duration(2490 * intervalTime)
          .attr('fill-opacity', 1)
          .attr('y', 0)
          .attr('class', function (d) {
            return 'value ' + _this.getColorClass(d)
          })
          .tween('text', function (d) {
            let self = this
            let i = d3.interpolate(self.textContent, xValue(d))
            return function (t) {
              self.textContent = Math.round(i(t))
            }
          })
          .attr('x', d => _this.xScale(xValue(d)) + 10)
          .attr('y', 22)
        barEnter.append('text')
          .attr('x', d => _this.xScale(xValue(d)))
          .attr('stroke', function (d) {
            return $('.' + _this.getColorClass(d)).css('fill')
          })
          .attr('class', 'barInfo')
          .attr('y', 50)
          .attr('stroke-width', '0px')
          .attr('fill-opacity', 0)
          .transition()
          .delay(500 * intervalTime)
          .duration(2490 * intervalTime)
          .text(yValue)
          .attr('x', d => _this.xScale(xValue(d)) - 10)
          .attr('fill-opacity', 1)
          .attr('y', 2)
          .attr('dy', '.5em')
          .attr('text-anchor', 'end')
          .attr('stroke-width', '1px')

        // Update Items
        let barUpdate = bar.transition().duration(2990 * intervalTime).ease(d3.easeLinear)
        barUpdate.select('rect')
          .attr('width', d => _this.xScale(xValue(d)))
        barUpdate.select('.barInfo')
          .attr('x', d => _this.xScale(xValue(d)) - 10)
          .attr('fill-opacity', 1)
          .attr('stroke-width', '1px')
        barUpdate.select('.value')
          .tween('text', function (d) {
            let self = this
            let i = d3.interpolate(self.textContent, xValue(d))
            return function (t) {
              self.textContent = Math.round(i(t))
            }
          })
          .duration(2990 * intervalTime)
          .attr('x', d => _this.xScale(xValue(d)) + 10)

        // Exit Items
        let barExit = bar.exit()
          .attr('fill-opacity', 1)
          .transition().duration(2500 * intervalTime)
        barExit
          .attr('transform', 'translate(0,' + 780 + ')')
          .remove()
          .attr('fill-opacity', 0)
        barExit.select('rect')
          .attr('fill-opacity', 0)
        barExit.select('.value')
          .attr('fill-opacity', 0)
        barExit.select('.barInfo')
          .attr('fill-opacity', 0)
          .attr('stroke-width', '0px')

        bar.data(data, yValue)
          .transition()
          .delay(500 * intervalTime)
          .duration(2490 * intervalTime)
          .attr('transform', function (d) {
            return 'translate(0,' + _this.yScale(yValue(d)) + ')'
          })
      },
      clear () {
        this.mini = 0
        this.heroOnBoard = {}
        this.dataBuffer = []
        this.dataOnBoard = []
        d3.select('svg').remove()
        this.initChart()
      },
      createWs () {
        if (typeof WebSocket === 'undefined') {
          alert('当前浏览器不支持WebSocket')
          return
        }
        this.socket = new WebSocket('ws://192.168.0.100:8080/websocket/' + this.topic + '_' + this.groupId)
        this.socket.onopen = function () {
          console.log('socket 创建成功')
        }
        this.socket.onerror = function (err) {
          console.log(err)
        }
        this.socket.onmessage = this.onmessage
        this.socket.onclose = function () {
          console.log('socket 退出')
        }
      },
      onmessage (msg) {
        let jsonArr = JSON.parse(msg.data)
        for (let i = 0; i < jsonArr.length; i++) {
          let heroMatches = jsonArr[i]
          this.dataBuffer.push(heroMatches)
        }
      },
      start () {
        this.clear()
        let data = {
          topic: this.topic,
          groupId: this.groupId + '',
          start: this.queryDates[0] ? this.queryDates[0] : '',
          end: this.queryDates[1] ? this.queryDates[1] : '',
          hero: '',
        }
        this.$http
          .post('/api/start_consumer', data)
          .then((res) => {
          })
        this.bufferTimer = setInterval(this.processBuffer, 3000 * intervalTime)
      },
      stop () {
        this.$http.get(
          'api/stop_consumer?socketname=' +
          this.topic +
          '_' +
          this.groupId
        )
        if (this.bufferTimer) window.clearInterval(this.bufferTimer)
        this.bufferTimer = ''
      }
    }
  }
</script>

<style>
  .domain {
    display: none;
  }

  .tick line {
    stroke: #C0C0BB;
  }

  .tick text {
    fill: #8E8883;
    font-size: 8pt;
  }

  .days {
    fill: rgb(92, 92, 92);
    font-weight: bold;
    font-size: 55pt;
  }

  .timer {
    fill: #686868;
  }

  .value {
    fill: rgb(138, 46, 46);
    font-size: 13pt;
    font-weight: 400;
  }

  .barInfo {
    fill: rgb(255, 255, 255);
    font-size: 13pt;
    font-weight: 800;
  }

  .I {
    fill: gold;
  }

  .H {
    fill: cyan;
  }

  .G {
    fill: darkmagenta;
  }

  .F {
    fill: darkgreen;
  }

  .E {
    fill: tan;
  }

  .D {
    fill: darkorange;
  }

  .C {
    fill: darkblue;
  }

  .B {
    fill: darkred;
  }

  .A {
    fill: steelblue;
  }
</style>
