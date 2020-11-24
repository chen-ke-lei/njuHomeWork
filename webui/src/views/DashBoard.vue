<template>
  <el-container>
    <el-header style="padding: 0">
      <el-menu @select="navigation"
               mode="horizontal"
               background-color="#6a005f"
               text-color="#ffffff">
        <el-menu-item index="stream">Stream</el-menu-item>
        <el-submenu index="graph">
          <template slot="title">Graph</template>
          <el-menu-item index="graph-one">
            Player vs. Hero
          </el-menu-item>
          <el-menu-item index="graph-two">
            Player vs. Player
          </el-menu-item>
        </el-submenu>
      </el-menu>
    </el-header>
    <el-main>
      <router-view/>
    </el-main>
  </el-container>
</template>

<script>
  export default {
    name: 'DashBoard',
    methods: {
      navigation (key) {
        switch (key) {
          case 'stream':
            this.$router.push('/')
            break
          case 'graph-one':
            this.$router.push('/ph_graph')
            break
          case 'graph-two':
            this.$router.push('/pp_graph')
            break
          default:
            break
        }
      },
    }
  }
</script>

<style scoped>

</style>
