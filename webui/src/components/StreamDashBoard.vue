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
      <el-col :span="13">
        <HeroWinRate></HeroWinRate>
      </el-col>
      <el-col :span="7"></el-col>
        <player-win></player-win>
    </el-row>
  </div>
</template>

<script>
  import HeroMatches from './HeroMatches'
  import SiteMessage from './SiteMessage'
  import HeroWinRate from './HeroWinRate'
  import PlayerWin from './PlayerWin'
  export default {
    name: 'StreamDashBoard',
    components: {HeroWinRate, SiteMessage, HeroMatches,PlayerWin}
  }
</script>

<style scoped>

</style>
