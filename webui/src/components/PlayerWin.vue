<template>
  <div id="playerWin">
    <span style="font-size: 8px">选手胜场词云</span>
    <el-date-picker
      v-model="dateValue"
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
    <div id="pic">
      <img :src="qrcode">
    </div>
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
        default: "ws://192.168.0.100:8080/websocket/playerWin_",
      },

      serviceHost: {
        type: String,
        default: "http://192.168.0.100:8080",
      },
    },

    mounted() {
      alert(this.createWspath)
      this.createWs();
    },

    data() {
      let qrcode="data:image/png;base64,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cu9DtD051uy6q0BBxDhNZ0F0Nzd905IbSFEyiptHmrUpiFIQ6jfMavtpQUyKT1txGHmfJk2+ILqbvNyw3fun55fzCmGZxnOVcO95WD973uOCEv+56f3InnfRVhBDKZH9tpnc98cqzm8m1fsSCnpbyXLHnhxrbuzaOdbnKptku/rvdfIe1ITx2/Z9RGN/glgUqpCi8NQLYv0pY03DdmSsepFq+Xo0Kj++mLWXOFf81JO8/Ni1fc8UNp1ku8nyQwtYVNjq8k8l431vLJ9pQSzfQ/00s/Vp6ggoiBinKSyI/qblbkjqRlTdzOOy4PmbRa0glusOT/vNPlMarJEKYrLdtkxuAdEF0ZW/sM83LObPlahKYvQet4jH9WOHGX20nvNcQcxoLJOYRWtXEKti0zlRexn69FC0FVEkmgqi54txUhKj99FNbyiGSrnM6t8XCiLGaeoKYr38jVuHp3VSMqJ9qp50ZIpQW0HsrMM+oZum96NP6aMgnlPG/gSxgX6CKGWmw/oUxB7aC6LjzmdL+VtnFESM09T+kgoAbARuypgUXIsYJwoiAPTATRmTgmsR40RBBIAeuCljUnAtYpwoiADQAzdlTAquRYwTBREAeuCmjEnBtYhxoiACQA/clDEpuBYxThREAOiBmzImBdcixomCCAA9cFPGpOBaxDhREAGgB27KmBRcixgnCiIA9MBNGZOCaxHjREEEgB64KWNScC1inCiIANADN2VMCq5FjBMFEQB64KaMScG1iHGiIAJAD9yUMSm4FjFO614Q5YImhJBpzfHjx8OnGbCx5FrMXaOErEV4gggAAACFgggAAACFgggAAACFgggAAACFgggAAACFgggAAACFgggAAACFgggAAACFgggAAACFgggAAACFgggAAACFgggAAACFgggAAACFgggAAACFgggAAACFgggAAACFgggAAACFgggAAACFgggAAACFgggAAACFgggAAACFgggAAACFgggAAACFgggAAACFgggAAACFgggAAABlqgvi8sKc2TS3YJbD61jbPAAAADTbgIK4aLZt2mQ29U2m7FEQAQAA1t6GFcRti+GlWF4wc+m02OI2CiIAAMA62fiCGMph9qmhjVuOgggAALBuNrYgluVwziwkTW5xm51etMi2ghiVyeHZZrcuMk8xAQAA4GxgQZT/hmLoimJREpfNwpwtc3F7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"
      let groupId = new Date().getTime();
      this.createWspath = this.createWspath + groupId;

      return {
        socket: null,
        bufferTimer:'',
        groupId: groupId,
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
        this.load = true;
        // this.clear();
        //console.log(endTime);
        let data = {
          topic: this.topic,
          groupId: this.groupId + "",
          start: "",
          end: "",
          hero: "",
        };
        this.$http
          .post(this.serviceHost + "/start_consumer", data)
          .then((res) => {
            console.log(res);
          });
      },
      onmessage(msg) {
         this.qrcode ='data:image/png;base64'+msg.data;
         console.log(this.qrcode)
      },
    },
  };
</script>
