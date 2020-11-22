<template>
  <div id="heroWinRate">
    <span style="font-size: 8px">选手胜场词云</span>
    <el-date-picker
      v-model="queryDates"
      type="daterange"
      format="yyyy-MM-dd"
      value-format="yyyyMMdd"
      range-separator="至"
      start-placeholder="起始日期"
      end-placeholder="结束日期"
      :editable="true"
      :picker-options="yearOptions"
      size="mini"
      style="width: 240px"
    >
    </el-date-picker>
    <br/>
    <el-button
      plain
      icon="el-icon-video-play"
      size="mini"
      @click="this.mock"
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
    <div id="myChart"></div>
  </div>
  <div id="pic">
    <img src="'data:image/png;base64,'+qrcode">
  </div>
</template>

<script>
  // import { site_option } from "../echarts/echartsUtil.js";
  // alert(site_option);
  export default {
    name: 'PlayerWin',
    props: {
      createWspath: {
        type: String,
        default: "ws://198.162.0.100/websocket/playerWin_",
      },

      serviceHost: {
        type: String,
        default: "http://198.162.0.100 :8080",
      },
    },

    data() {
      let qrcode=""
      let groupId = new Date().getTime();
      this.createWspath = this.createWspath + groupId;

      return {
        socket: null,
        bufferTimer:'',
        groupId: groupId,
        topic: "playerWin",
        load: false,
        qrcode : qrcode
      };
    },

    mounted() {
      this.createWs();
    },

    beforeDestroy () {
      window.clearInterval(this.bufferTimer)
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

      stop(){
        window.clearInterval(this.bufferTimer)
        this.load = false;
        this.$http.get(
          this.serviceHost +
          "/stop_consumer?socketname=" +
          this.topic +
          "_" +
          this.groupId
        );
      },

      start() {
        this.clear()
        this.$http({
          methods: "post",
          url: this.startWsPath,
        }).then((res) => {});
      },
      onmessage(msg) {
        let qrcode = msg;
      },
    },

  };
</script>
