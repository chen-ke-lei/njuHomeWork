<template>
  <div>
    <el-date-picker
      v-model="dateValue"
      size="small"
      type="daterange"
      value-format="yyyy-MM-dd"
      :picker-options="dateOptions"
      range-separator="至"
      start-placeholder="开始日期"
      end-placeholder="结束日期"
    >
    </el-date-picker>
    <br />
    <el-button plain icon="el-icon-video-play" size="mini" @click="this.start">
      start
    </el-button>
    <el-button plain icon="el-icon-video-pause" size="mini" @click="this.stop">
      stop
    </el-button>
    <div
      ref="siteMessage"
      :style="{ height: height, width: width, margin: 'auto' }"
    ></div>
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
      default: "480px",
    },
    height: {
      type: String,
      default: "330px",
    },
    createWspath: {
      type: String,
      default: "ws://192.168.0.100:8080/websocket/siteMessage_",
    },
    serviceHost: {
      type: String,
      default: "http://192.168.0.100:8080",
    },
    title: {
      type: String,
      default: "位置信息统计",
    },
    time: {
      type: String,
      default: "",
    },
  },
  mounted() {
    this.initChart();
    this.createWs();
  },
  data() {
    let groupId = new Date().getTime();
    const dateShortcuts = [];
    for (let i = 2020; i > 2010; i--) {
      let option = {};
      option["text"] = i + "";
      option["onClick"] = function (picker) {
        const start = new Date(i, 0, 1);
        const end = new Date(i, 11, 31);
        picker.$emit("pick", [start, end]);
      };
      dateShortcuts.push(option);
    }
    this.createWspath = this.createWspath + groupId;
    return {
      myChart: null,
      option: null,
      socket: null,
      groupId: groupId,
      topic: "siteMessage",
      dateValue: [],
      dateOptions: { shortcuts: dateShortcuts },
      load: false,
    };
  },
  methods: {
    initChart() {
      const chart = this.$refs.siteMessage;
      if (chart) {
        this.myChart = this.$echarts.init(chart);
        this.option = {
          //   backgroundColor: "#" + (0xffffff - 0x1c1c1c),
          title: {
            text: this.title + this.time,
            textStyle: {
              color: "#2F4F4F",
              fontSize: 12,
            },
          },
          legend: {
            data: ["伤害", "承伤", "治疗"],
            textStyle: {
              color: "#8B7355",
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
                  color: "#363636",
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
                  color: "#1C1C1C",
                },
              },
              splitLine: {
                show: true,
                lineStyle: {
                  type: "dashed",
                  color: "#1C1C1C",
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
                  color: "darkred",
                  //   barBorderRadius: 50,
                  borderWidth: 0,
                  borderColor: "darkred",
                },
              },
              barWidth: "50%",
            },
            {
              name: "承伤",
              type: "bar",
              stack: "site",
              data: [],
              itemStyle: {
                normal: {
                  show: true,
                  color: "darkblue",
                  //   barBorderRadius: 50,
                  borderWidth: 0,
                  borderColor: "darkblue",
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
                  color: "darkgreen",
                  barBorderRadius: [20, 20, 0, 0],
                  borderWidth: 0,
                  borderColor: "darkgreen",
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
                  fontSize: 10,
                  fontWeight: "bold",
                  textStyle: { color: "#00BFFF" },
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
    clear() {
      this.option.series[0].data = [];
      this.option.series[1].data = [];
      this.option.series[2].data = [];
      this.option.series[3].data = [];
      for (let i = 0; i < 100; i++) this.myChart.setOption(this.option);
    },
    start() {
      this.load = true;
      this.time = "";
      this.clear();
      console.log(this.dateValue);
      let data = {
        topic: this.topic,
        groupId: this.groupId + "",
        start: this.dateValue[0],
        end: this.dateValue[1],
        hero: "",
      };
      this.$http
        .post(this.serviceHost + "/start_consumer", data)
        .then((res) => {
          console.log(res);
        });
    },
    stop() {
      this.load = false;
      this.$http.get(
        this.serviceHost +
          "/stop_consumer?socketname=" +
          this.topic +
          "_" +
          this.groupId
      );
    },
    loadData(data) {
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
        if (this.time == "" || this.time < re.updateTime)
          this.time = re.updateTime;
        demage[x] = re.damageDealt;
        taken[x] = re.damageTaken;
        heal[x] = re.heal;
      }
      this.option.series[0].data = demage;
      this.option.series[1].data = taken;
      this.option.series[2].data = heal;
      this.option.title.text = this.title + "-" + this.time;
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
    onmessage(msg) {
      let data = msg.data;
      if (this.load) this.loadData(data);
    },
  },
};
</script>
