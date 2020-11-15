<template>
  <div id="myChart"></div>
</template>

<script>
  import echarts from 'echarts'
  export default {
    name: 'HeroWinRate',
    data () {
      return {
        // 实时数据数组
        date: [],
        yieldRate: [],
        // 折线图echarts初始化选项
        createWspath: {
          type: String,
          default: "ws://localhost:8080/websocket/heroWinRate_",
        },
        startWsPath: {
          type: String,
          default: "/api/start_consumer?socketname=heroWinRate_",
        },
        echartsOption: {
          legend: {
            data: ['英雄胜率'],
          },
          xAxis: {
            name: '时间',
            nameTextStyle: {
              fontWeight: 600,
              fontSize: 18
            },
            type: 'category',
            boundaryGap: false,
            data: this.date,	// 绑定实时数据数组
          },
          yAxis: {
            name: '胜率',
            nameTextStyle: {
              fontWeight: 600,
              fontSize: 18
            },
            type: 'value',
            scale: true,
            boundaryGap: ['15%', '15%'],
            axisLabel: {
              interval: 'auto',
              formatter: '{value} %'
            }
          },
          tooltip: {
            trigger: 'axis',
          },
          series: [
            {
              name:'英雄胜率',
              type:'line',
              smooth: false,
              data: this.yieldRate,	// 绑定实时数据数组
            },
          ]
        }
      }
    },
    mounted () {
      this.myChart = echarts.init(document.getElementById('myChart'), 'light');	// 初始化echarts, theme为light
      this.myChart.setOption(this.echartsOption);	// echarts设置初始化选项
      setInterval(this.test1, 1500);	// 每三秒更新实时数据到折线图
/*      this.test();*/
    },
    methods: {
      createWs() {
        if (typeof WebSocket === "undefined") {
          return;
        }
        this.socket = new WebSocket(this.createWspath);
        this.socket.onopen = function () {
          this.record = new Map();
          console.log("socket 创建成功");
        };
        this.socket.onerror = function (err) {
          console.log(err);
        };

        this.socket.onmessage = this.onmessage;
        this.socket.onclose = function () {
          console.log("socket 退出");
        };
      },
      start() {
        this.$http({
          methods: "get",
          url: this.startWsPath,
        }).then((res) => {});
      },

      test(){
        var testData=[{"winRate":0.5,"updateTime":2011},
                      {"winRate":0.6,"updateTime":2012},
                      {"winRate":0.2,"updateTime":2012.06},
                      {"winRate":0.7,"updateTime":2018}]

        for(var i = 0; i < 4; i++) {


          this.yieldRate.push((testData[i].winRate * 100).toFixed(3));
          this.date.push(testData[i].updateTime);
          // 重新将数组赋值给echarts选项
          this.echartsOption.xAxis.data = this.date;
          this.echartsOption.series[0].data = this.yieldRate;
          this.myChart.setOption(this.echartsOption);
        }
      },

      test1(){
        var testData=[{"winRate":0.5,"updateTime":2011},
          {"winRate":0.6,"updateTime":2012},
          {"winRate":0.2,"updateTime":2012.06},
          {"winRate":0.7,"updateTime":2018},
          {"winRate":0.53,"updateTime":2015},
          {"winRate":0.61,"updateTime":2014},
          {"winRate":0.55,"updateTime":2013},
          {"winRate":0.42,"updateTime":2019},
          {"winRate":0.88,"updateTime":2020},
          {"winRate":0.37,"updateTime":2019.05},]




          this.yieldRate.push((testData[Math.floor(Math.random()*10)].winRate * 100).toFixed(3));
          this.date.push(testData[Math.floor(Math.random()*10)].updateTime);
          // 重新将数组赋值给echarts选项
          this.echartsOption.xAxis.data = this.date;
          this.echartsOption.series[0].data = this.yieldRate;
          this.myChart.setOption(this.echartsOption);

      },





      onmessage(msg) {
        let data = msg.data;
        let jsondata = JSON.parse(data);
        this.yieldRate.push((jsondata.winRate * 100).toFixed(3));
        this.date.push(jsondata.updateTime);
        // 重新将数组赋值给echarts选项
        this.echartsOption.xAxis.data = this.date;
        this.echartsOption.series[0].data = this.yieldRate;
        this.myChart.setOption(this.echartsOption);
      },

      // 获取当前时间
      getTime : function() {
        var ts = arguments[0] || 0;
        var t, h, i, s;
        t = ts ? new Date(ts * 1000) : new Date();
        h = t.getHours();
        i = t.getMinutes();
        s = t.getSeconds();
        // 定义时间格式
        return (h < 10 ? '0' + h : h) + ':' + (i < 10 ? '0' + i : i) + ':' + (s < 10 ? '0' + s : s);
      },
      // 添加实时数据
      addData : function() {
        // 从接口获取数据并添加到数组
        this.$axios.get('url').then((res) => {
          this.yieldRate.push((res.data.actualProfitRate * 100).toFixed(3));
          this.date.push(this.getTime(Math.round(new Date().getTime() / 1000)));
          // 重新将数组赋值给echarts选项
          this.echartsOption.xAxis.data = this.date;
          this.echartsOption.series[0].data = this.yieldRate;
          this.myChart.setOption(this.echartsOption);
        });
      }
    }
  }
</script>

<style>
  #myChart{
    width: 100%;
    height: 500px;
    margin: 0 auto;
  }
</style>
