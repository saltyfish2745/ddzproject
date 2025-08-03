import * as components from "@element-plus/icons-vue";
export default {
    install: (app) => {
        for (const key in components) {
            const componentconfig = components[key];
            app.component(componentconfig.name, componentconfig);
        }
    },
};