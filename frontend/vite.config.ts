import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { execSync } from 'child_process'
import fs from 'fs'
import path from 'path'

// 获取当前 git 标签版本号，如果失败则回退到 package.json 或默认版本 v2.1.2
let version = 'v2.1.2'
try {
  version = execSync('git describe --tags --abbrev=0 --always').toString().trim()
} catch (e) {
  try {
    const pkgPath = path.resolve(__dirname, 'package.json')
    const pkg = JSON.parse(fs.readFileSync(pkgPath, 'utf-8'))
    version = pkg.version ? `v${pkg.version}` : 'v2.1.2'
  } catch (err) {
    // keep default
  }
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  envDir: '../.local-configs',
  define: {
    // 注入全局常量以在应用中实时读取版本号
    'import.meta.env.VITE_APP_VERSION': JSON.stringify(version)
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/images': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
