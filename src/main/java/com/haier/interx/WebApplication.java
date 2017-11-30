package com.haier.interx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * @description 告警板后台服务类，用于权限处理等操作
 * @date 2017/11/28
 * @author Niemingming
 */
@ComponentScan("com.haier.interx")
@SpringBootApplication
public class WebApplication {

    public static void main(String[] args){
        SpringApplication.run(WebApplication.class,args);
    }
}
