<template>
  <div>
    <div id="pic">
      <img src="'data:image/png;base64,'+qrcode">
    </div>
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
    name: "PlayerWin",
    props: {
      className: {
        type: String,
        default: "img",
      },
      id: {
        type: String,
        default: "img",
      },
      createWspath: {
        type: String,
        default: "ws://localhost:8080/websocket/playerWin_",
      },
      startWsPath: {
        type: String,
        default: "/api/start_consumer?socketname=playerWin_",
      },
    },
    mounted() {
      this.createWs();
    },
    data() {
      let timeStamp = new Date().getTime();
      this.createWspath = this.createWspath + timeStamp;
      this.startWsPath = this.startWsPath + timeStamp;
      return {
        socket: null,
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
