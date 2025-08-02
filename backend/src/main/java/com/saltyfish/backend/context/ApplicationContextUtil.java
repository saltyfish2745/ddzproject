package com.saltyfish.backend.context;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

// 用于在不归Spring容器管理的类中获取Spring的注入对象
// 这个类是一个工具类，用于获取Spring的ApplicationContext对象
@Component
public class ApplicationContextUtil implements ApplicationContextAware {
    // 使用静态变量持有ApplicationContext对象
    private static ApplicationContext applicationContext;

    // 实现ApplicationContextAware接口的setApplicationContext方法
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ApplicationContextUtil.applicationContext = applicationContext;
    }

    // 通过applicationContext获取bean对象
    public static <T> T getBean(Class<T> clazz) {
        return applicationContext.getBean(clazz);
    }

    public static Object getBean(String beanName) {
        return applicationContext.getBean(beanName);
    }
}
