package com.haier.interx.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @description 请求服务配置信息
 * @date 2017/11/28
 * @author Niemingming
 */
@Component
@ConfigurationProperties(prefix = "service")
public class ServiceConfiguration {
    /*权限服务地址，用于获取登录人相关的项目信息*/
    private String authorityurl;
    /*告警板后台服务地址*/
    private String alertstationurl;
    /*单点登录验证服务*/
    private String permissionurl;
    /*鉴权服务地址*/
    private  String permissionhost;
    /*告警站要处理的时间key值*/
    private List<String> alertstationconverter;

    public String getAuthorityurl() {
        return authorityurl;
    }
    public void setAuthorityurl(String authorityurl) {
        this.authorityurl = authorityurl;
    }
    public String getAlertstationurl() {
        return alertstationurl;
    }
    public void setAlertstationurl(String alertstationurl) {
        this.alertstationurl = alertstationurl;
    }

    public String getPermissionurl() {
        return permissionurl;
    }

    public void setPermissionurl(String permissionurl) {
        this.permissionurl = permissionurl;
    }

    public String getPermissionhost() {
        return permissionhost;
    }

    public void setPermissionhost(String permissionhost) {
        this.permissionhost = permissionhost;
    }

    public List<String> getAlertstationconverter() {
        return alertstationconverter;
    }

    public void setAlertstationconverter(List<String> alertstationconverter) {
        this.alertstationconverter = alertstationconverter;
    }
}
