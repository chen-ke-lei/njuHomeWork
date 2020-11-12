<template>
  <div>
    <div
      ref="siteMessage"
      :style="{ height: height, width: width, margin: 'auto' }"
    ></div>
    <button
      @click="this.start"
      :style="{ marginTop: '30px', height: '40px', width: '70px' }"
    >
      start!
    </button>
  </div>
</template>

<script>
// import { site_option } from "../echarts/echartsUtil.js";
// alert(site_option);
export default {
  name: "siteMessage",
  props: {
    className: {
      type: String,
      default: "chart",
    },
    id: {
      type: String,
      default: "chart",
    },
    width: {
      type: String,
      default: "600px",
    },
    height: {
      type: String,
      default: "400px",
    },
    createWspath: {
      type: String,
      default: "ws://localhost:8080/websocket/siteMessage_",
    },
    startWsPath: {
      type: String,
      default: "/api/start_consumer?socketname=siteMessage_",
    },
  },
  mounted() {
    this.initChart();
    this.createWs();
  },
  data() {
    let timeStamp = new Date().getTime();
    this.createWspath = this.createWspath + timeStamp;
    this.startWsPath = this.startWsPath + timeStamp;
    return {
      myChart: null,
      option: null,
      socket: null,
    };
  },
  methods: {
    initChart() {
      const chart = this.$refs.siteMessage;
      if (chart) {
        this.myChart = this.$echarts.init(chart);
        this.option = {
          backgroundColor: "#1C1C1C",
          title: {
            text: "位置信息统计",
            textStyle: {
              color: "#828282",
              fontSize: 15,
            },
          },
          legend: {
            data: ["伤害", "承伤", "治疗"],
            textStyle: {
              color: "#A4D3EE",
            },
          },
          tooltip: {
            //过滤掉统计的series
            trigger: "axis",
            axisPointer: {
              // 坐标轴指示器，坐标轴触发有效
              type: "shadow", // 默认为直线，可选为：'line' | 'shadow'
            },
            formatter: function (params) {
              var res = params[0].name + "<br/>";
              for (let i = 0; i < params.length - 1; i++) {
                res += params[i].seriesName + ":" + params[i].value + "<br/>";
              }
              return res;
            },
          },
          grid: {
            left: "3%",
            right: "4%",
            bottom: "3%",
            containLabel: true,
          },
          xAxis: [
            {
              type: "category",
              data: ["上单", "中单", "打野", "辅助", "射手"],
              axisLine: {
                show: true,
                lineStyle: {
                  color: "#fff",
                },
              },
            },
          ],
          yAxis: [
            {
              type: "value",
              axisTick: {},
              axisLine: {
                lineStyle: {
                  color: "#DCDCDC",
                },
              },
              splitLine: {
                show: true,
                lineStyle: {
                  type: "dashed",
                  color: "#aaa",
                },
              },
            },
          ],
          series: [
            {
              name: "伤害",
              type: "bar",
              stack: "site",

              data: [],
              itemStyle: {
                normal: {
                  show: true,
                  color: "#A52A2A",
                  //   barBorderRadius: 50,
                  borderWidth: 0,
                  borderColor: "#333",
                },
              },
              barWidth: "30%",
            },
            {
              name: "承伤",
              type: "bar",
              stack: "site",
              data: [],
              itemStyle: {
                normal: {
                  show: true,
                  color: "#FFDAB9",
                  //   barBorderRadius: 50,
                  borderWidth: 0,
                  borderColor: "#333",
                },
              },
              barWidth: "30%",
              barGap: "100%",
            },
            {
              name: "治疗",
              type: "bar",
              stack: "site",
              data: [],
              itemStyle: {
                normal: {
                  show: true,
                  color: "#20B2AA",
                  barBorderRadius: [20, 20, 0, 0],
                  borderWidth: 0,
                  borderColor: "#333",
                },
              },
              barWidth: "30%",
              barGap: "100%",
            },

            {
              name: "统计",
              type: "bar",
              stack: "site",
              data: [], //模拟数据
              label: {
                normal: {
                  offset: [0, -10], //左右，上下
                  show: true,
                  position: "insideLeft",
                  formatter: function (params) {
                    if (params.data == 0) {
                      return "0%";
                    } else {
                      return (params.data * 100).toFixed(2) + "%";
                    }
                  },
                  fontSize: 14,
                  fontWeight: "bold",
                  textStyle: { color: "#199ED8" },
                },
              },
              itemStyle: {
                normal: {
                  color: "rgba(128, 128, 128, 0)",
                },
              },
            },
          ],
        };
        this.myChart.setOption(this.option);
      }
      // alert(this.myChart)
    },

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
    onmessage(msg) {
      let data = msg.data;
      let jsondata = JSON.parse(data);
      let demage = this.option.series[0].data;
      let taken = this.option.series[1].data;
      let heal = this.option.series[2].data;
      for (let i in jsondata) {
        let re = jsondata[i];
        let x = 0;
        if (re.site == "TOP") {
          x = 0;
        } else if (re.site == "MIDDLE") {
          x = 1;
        } else if (re.site == "JUNGLE") {
          x = 2;
        } else if (re.site == "SUPPORT") x = 3;
        else x = 4;
        demage[x] = re.damageDealt;
        taken[x] = re.damageTaken;
        heal[x] = re.heal;
      }
      this.option.series[0].data = demage;
      this.option.series[1].data = taken;
      this.option.series[2].data = heal;
      let rate = this.option.series[3].data;
      let sum = 0;
      for (let i = 0; i < 5; i++) {
        sum = sum + demage[i] + taken[i] + heal[i];
      }
      for (let i = 0; i < 5; i++) {
        rate[i] = (demage[i] + taken[i] + heal[i]) / sum;
      }
      this.option.series[3].data = rate;
      this.myChart.setOption(this.option);
    },
  },
};
</script>
