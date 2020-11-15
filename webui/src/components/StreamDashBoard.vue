<template>
  <div>
    <el-row>
      <el-col :span="10">
        <HeroMatches></HeroMatches>
      </el-col>
      <el-col :span="10"></el-col>
    </el-row>
    <el-row>
      <el-col :span="10">
        <SiteMessage></SiteMessage>
      </el-col>
      <el-col :span="10"></el-col>
    </el-row>
  </div>
</template>

<script>
  import HeroMatches from './HeroMatches'
  import SiteMessage from './SiteMessage'
  export default {
    name: 'StreamDashBoard',
    components: {SiteMessage, HeroMatches}
  }
</script>

<style scoped>

</style>
