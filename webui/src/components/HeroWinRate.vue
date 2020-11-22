<template>
  <div id="heroWinRate">
    <span style="font-size: 8px">英雄胜率</span>
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

</template>

<script>
  import echarts from 'echarts'
  export default {
    name: 'HeroWinRate',
    props: {
      createWspath: {
        type: String,
        default: "ws://192.168.0.100:8080/websocket/heroWinRate_",
      },

      serviceHost: {
        type: String,
        default: "http://192.168.0.100:8080",
      },
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
      const heroes = require('../assets/heroes')

      let groupId = new Date().getTime();
      this.createWspath = this.createWspath + groupId;


      return {
        bufferTimer: '',
        dataBuffer: [],
        queryDates: [],
        yearOptions: {shortcuts: dateShortcuts},
        queryHeroes: [],
        heroOptions: heroes,
        // 实时数据数组
        date: [],
        yieldRate: [],
        // 折线图echarts初始化选项


        socket: null,
        groupId: groupId,
        topic: "heroWinRate",
        load: false,

        echartsOption: {
          /*          animation: true,
                    animationDuration: 1000,
                    animationEasing: 'cubicInOut',
                    animationDurationUpdate: 1000,
                    animationEasingUpdate: 'cubicInOut',*/
          legend: {
            data: [],
          },
          xAxis: {
            name: '时间',
/*            nameTextStyle: {
              fontWeight: 600,
              fontSize: 18
            },*/
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
          dataZoom:[{
            type: 'slider',
            xAxisIndex: 0,
            filterMode: 'empty'
          },
            {
              type: 'slider',
              yAxisIndex: 0,
              filterMode: 'empty'
            },
            {
              type: 'inside',
              xAxisIndex: 0,
              filterMode: 'empty'
            },
            {
              type: 'inside',
              yAxisIndex: 0,
              filterMode: 'empty'
            }],
          series: [
            /*{
             name:'1',
             type:'line',
             smooth: false,
             data: this.yieldRate,	// 绑定实时数据数组
           },
           {
             name:'2',
             type:'line',
             smooth: false,
             data: [],	// 绑定实时数据数组
           },
           /*{
          name:'3',
             type:'line',
             smooth: false,
             data: [],	// 绑定实时数据数组
           },*/
          ]
        }
      }
    },
    mounted () {
      this.myChart = echarts.init(document.getElementById('myChart'), 'light');	// 初始化echarts, theme为light
      this.myChart.setOption(this.echartsOption);	// echarts设置初始化选项
            this.createWs();

      /*setInterval(this.test1, 1500);*/
      /*      this.test();*/
    },
    beforeDestroy () {
      window.clearInterval(this.bufferTimer)
    },
    methods: {
      mock(){
        if(this.queryHeroes.length>5){alert("最多可选择5个英雄！")}
        else {
          this.load = true;
          let data = {
            topic: this.topic,
            groupId: this.groupId + "",
            start: this.queryDates[0] ? this.queryDates[0] : '',
            end: this.queryDates[1] ? this.queryDates[1] : '',
            hero: this.queryHeroes.join(","),
          };

          //初始化legend、横纵坐标、databuffer
          this.echartsOption.legend.data = this.queryHeroes;
          let newSeries=[];
          for(let i=0;i<this.queryHeroes.length;i++){
            let newS={
              name:this.queryHeroes[i],
              type:'line',
              smooth: false,
              data: [],
            }
            newSeries.push(newS);
          }
          this.echartsOption.series=newSeries;
          this.date=[];
          this.dataBuffer=[];
          this.echartsOption.xAxis.data=this.date;
          this.myChart.setOption(this.echartsOption);
          /*setInterval(this.test1, 1500);*/

          this.bufferTimer = setInterval(this.processBuffer, 2000)

          this.$http
            .post(this.serviceHost + "/start_consumer", data)
            .then((res) => {
              console.log(res);
            });
        }
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

      processBuffer () {
        if (this.dataBuffer.length > 0) {
          let batch = this.dataBuffer.length >= 20 ? 20 : this.dataBuffer.length;

          let size=this.date.length;
          //console.log(size);


          //初始化最近的日期
          let latestTime;
          if(this.date.length>0){
            latestTime=this.date[this.date.length-1];
          }
          else {
            latestTime = "20100101";
          }


          for (let j=0;j<this.echartsOption.series.length;j++) {
            let c=0;

            //倒序，取最新的胜率数据
            for(let i = batch-1; i >=0; i--){

              //判断英雄名是否相同
              if(this.dataBuffer[i].hero===this.echartsOption.series[j].name){

                //判断新数据的updatetime是否比当前最近日期大；并且胜率数据是否有改变; 或者当前英雄没有胜率
                if(this.dataBuffer[i].updateTime>latestTime){

                  //console.log(this.echartsOption.series[j].name+": "+this.dataBuffer[i].winRate);

                  //记录胜率
                  this.echartsOption.series[j].data.push((this.dataBuffer[i].winRate * 100).toFixed(3));
                  if(size==this.date.length)
                  size=size+1;



                    latestTime = this.dataBuffer[i].updateTime;



               }else{
                  if(this.echartsOption.series[j].data.length===0
                    ||this.dataBuffer[i].winRate!=this.echartsOption.series[j].data[this.echartsOption.series[j].data.length-1]){
                    //记录胜率
                    this.echartsOption.series[j].data.push((this.dataBuffer[i].winRate * 100).toFixed(3));
                    //console.log(this.echartsOption.series[j].name+": "+(this.dataBuffer[i].winRate * 100).toFixed(3));
                    if(size==this.date.length)
                    size=size+1;
                  }
                }
                break;
              }
              c=c+1;
            }

            //如果当前批次没有该英雄的胜率数据
            if(c===batch&&size>this.date.length){

              //如果该英雄目前还没有胜率数据，设为0
              if(this.echartsOption.series[j].data.length===0){
                //console.log(this.echartsOption.series[j].name+": "+0);
                //没有胜率数据就push字符串“x”
                this.echartsOption.series[j].data.push("x");
              }

              //如果该英雄有数据，则取最近一次胜率数据，保持不变
              else{
                //console.log(this.echartsOption.series[j].name+": "+this.echartsOption.series[j].data[lastIndex-1]);
                let lastIndex=this.echartsOption.series[j].data.length;
                this.echartsOption.series[j].data.push(this.echartsOption.series[j].data[lastIndex-1]);
              }
            }

          }

          //添加最新时间为横坐标值
          for (let h=0;h<this.echartsOption.series.length;h++){
            if (this.echartsOption.series[h].data.length<size){
              //console.log(this.echartsOption.series[h].name+":"+this.echartsOption.series[h].data[size-2])
              if(this.echartsOption.series[h].data.length>0)
              this.echartsOption.series[h].data.push(this.echartsOption.series[h].data[this.echartsOption.series[h].data.length-1]);
              else
                this.echartsOption.series[h].data.push("x" );
            }
          }
          if (this.date.length===0||latestTime>this.date[size-2])
          this.date.push(latestTime);
          else {
            this.date.push((parseInt(latestTime)+1).toString());
          }
/*          console.log(this.date[size-1]);*/
/*          this.date.push(20120520);*/
          this.echartsOption.xAxis.data = this.date;
          //console.log(this.echartsOption.xAxis.data);
          //console.log(this.echartsOption.series[0].data[size-1]);
          //绘制折线图
          //console.log(latestTime);
          this.myChart.setOption(this.echartsOption);

          //循环删除buffer当前批次数据
          for(let x=0;x<batch;x++) {
            this.dataBuffer.shift();
          }


        }
      },

      test(){
        let testData=[{"winRate":0.5,"updateTime":2011},
          {"winRate":0.6,"updateTime":2012},
          {"winRate":0.2,"updateTime":2012.06},
          {"winRate":0.7,"updateTime":2018}]

        for(let i = 0; i < 4; i++) {


          this.yieldRate.push((testData[i].winRate * 100).toFixed(3));
          this.date.push(testData[i].updateTime);
          // 重新将数组赋值给echarts选项
          this.echartsOption.xAxis.data = this.date;
          this.echartsOption.series[0].data = this.yieldRate;
          this.myChart.setOption(this.echartsOption);
        }
      },

      test1(){
        let testData=[{"winRate":0.5,"updateTime":"20120520"},
          {"winRate":-0.6,"updateTime":"20130204"},
          {"winRate":0,"updateTime":"20150530"},
          {"winRate":0.7,"updateTime":"20160321"},
          {"winRate":0.53,"updateTime":"20161111"},
          {"winRate":-0.61,"updateTime":"20170614"},
          {"winRate":0.55,"updateTime":"20171221"},
          {"winRate":0.02,"updateTime":"20180503"},
          {"winRate":0.18,"updateTime":"20190905"},
          {"winRate":0,"updateTime":"20200202"},]




        this.yieldRate.push((testData[Math.floor(Math.random()*10)].winRate * 100).toFixed(3));
        if(this.yieldRate.length>5){
          this.echartsOption.series[1].data.push((testData[Math.floor(Math.random()*10)].winRate * 100).toFixed(3))
        }
        else
          this.echartsOption.series[1].data.push("abc");
        this.date.push(testData[Math.floor(Math.random()*10)].updateTime);
        // 重新将数组赋值给echarts选项
        this.echartsOption.xAxis.data = this.date;
        this.echartsOption.series[0].data = this.yieldRate;
        this.myChart.setOption(this.echartsOption);

      },





      onmessage(msg) {
        if(this.load) {
          let data = msg.data;
          let jsondata = JSON.parse(data);
          for (let i = 0; i < jsondata.length; i++) {
            let singleData=jsondata[i];
            for (let j = 0; j < this.echartsOption.series.length; j++) {
              if(singleData.hasOwnProperty('hero')) {

                if (this.echartsOption.series[j].name === singleData.hero) {
                  //console.log(singleData)
                  this.dataBuffer.push(singleData);
                }
              }
            }
          }
/*          this.yieldRate.push((jsondata.winRate * 100).toFixed(3));
          this.date.push(jsondata.updateTime);
          // 重新将数组赋值给echarts选项
          this.echartsOption.xAxis.data = this.date;
          this.echartsOption.series[0].data = this.yieldRate;
          this.myChart.setOption(this.echartsOption);*/
        }
      },

      // 获取当前时间
      getTime : function() {
        let ts = arguments[0] || 0;
        let t, h, i, s;
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
