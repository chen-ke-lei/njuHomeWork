<template>
  <img :src="defaultImg" >
</template>

<script>

import siteMessage from './SiteMessage.vue'
import defaultImg  from './PlayerWin'

export default {
  name: 'HelloWorld',
  components: {siteMessage},
  data () {
    return {
      defaultImg:defaultImg
    }
  }
}
  new Vue({
    data:{
      imgurl:"base64路径"
    }
  })
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
h1, h2 {
  font-weight: normal;
}
ul {
  list-style-type: none;
  padding: 0;
}
li {
  display: inline-block;
  margin: 0 10px;
}
a {
  color: #42b983;
}
</style>
