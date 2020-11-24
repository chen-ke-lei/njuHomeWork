<template>
  <div id="playerWin">
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
      <span style="color: #2F4F4F; font-size: 12px; font-weight: bold">选手胜场次数</span>
    </div>
    <div id="pic" style="margin-left: 35px">
      <img :src="qrcode">
    </div>
  </div>

</template>

<script>
  // import { site_option } from "../echarts/echartsUtil.js";
  export default {
    name: 'PlayerWin',
    props: {
      createWspath: {
        type: String,
        default: "ws://192.168.0.100:8080/websocket/playerWin_",
      },

      serviceHost: {
        type: String,
        default: "http://192.168.0.100:8080",
      },
    },

    mounted() {
      this.createWs();
    },

    beforeDestroy () {
      if (this.bufferTimer) window.clearInterval(this.bufferTimer)
    },

    data() {
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

      let qrcode=""
      let groupId = new Date().getTime();
      this.createWspath = this.createWspath + groupId;

      return {
        queryDates:[],
        socket: null,
        bufferTimer:'',
        groupId: groupId,
        yearOptions: {shortcuts: dateShortcuts},
        topic: "playerWin",
        load: false,
        qrcode : qrcode,
      };
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
        this.load = false;
        this.$http.get(
          this.serviceHost +
          "/stop_consumer?socketname=" +
          this.topic +
          "_" +
          this.groupId
        );
        if (this.bufferTimer) window.clearInterval(this.bufferTimer)
        this.bufferTimer = ''
      },
      start() {
        this.load = true;
        // this.clear();
        //console.log(endTime);
        let data = {
          topic: this.topic,
          groupId: this.groupId + "",
          start: this.queryDates[0] ? this.queryDates[0] : '',
          end: this.queryDates[1] ? this.queryDates[1] : '',
          hero: "",
        };
        this.$http
          .post(this.serviceHost + "/start_consumer", data)
          .then((res) => {
            console.log(res);
          });
      },
      onmessage(msg) {
         this.qrcode ='data:image/png;base64,'+decodeURI(msg.data);
         console.log(this.qrcode)
      },
    },
  };
</script>
