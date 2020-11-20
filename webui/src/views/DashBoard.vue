<template>
  <el-container>
    <el-header style="padding: 0">
      <el-menu :default-active="this.$route.name"
               @select="navigation"
               mode="horizontal"
               background-color="#6a005f"
               text-color="#ffffff">
        <el-menu-item index="stream">Stream</el-menu-item>
        <el-submenu index="graph">
          <template slot="title">Graph</template>
          <el-menu-item index="graph-one">
            Graph1
          </el-menu-item>
          <el-menu-item index="graph-two">
            Graph2
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
            this.$router.push('/')
            break
          case 'graph-two':
            this.$router.push('/')
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
