const { defineConfig } = require('@vue/cli-service')
const AutoImport = require('unplugin-auto-import/webpack')
const Components = require('unplugin-vue-components/webpack')

module.exports = defineConfig({
  transpileDependencies: true,
  
  devServer: {
    port: 8081  // 这里将默认端口修改为8081
  }
})

