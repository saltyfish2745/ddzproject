import { createApp } from 'vue'
import App from './App.vue'
import './registerServiceWorker'
import router from './router'
import store from './store'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import elementIcon from './plugins/icons';


const app = createApp(App)
app.use(store)
app.use(router)
app.use(ElementPlus)
app.use(elementIcon)
app.mount('#app')
