<template>
  <div>
    <el-row>
      <el-col :span="14">
        <HeroMatches></HeroMatches>
      </el-col>
      <el-col :span="6">
        <SiteMessage></SiteMessage>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="10">
        <HeroWinRate></HeroWinRate>
      </el-col>
      <el-col :span="10"></el-col>
    </el-row>
  </div>
</template>

<script>
  import HeroMatches from './HeroMatches'
  import SiteMessage from './SiteMessage'
  import HeroWinRate from './HeroWinRate'
  export default {
    name: 'StreamDashBoard',
    components: {HeroWinRate, SiteMessage, HeroMatches}
  }
</script>

<style scoped>

</style>
