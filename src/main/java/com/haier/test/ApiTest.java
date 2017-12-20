package com.haier.test;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ApiTest {

    private static String host = "localhost";

    public static void main(String[] args) throws IOException {
//        testPermission();
//        for (int i = 0; i < 10000; i++)
//            queryList();
//        queryById();
//        queryHistoryList();
//        queryHistoryById();
        searchHistoryList();
//        queryGroup();
//        testgson();
//        queryLevelCode();
//        queryCode("queryAlertLevels");
//        queryCode("queryAlertCategories");
//        queryCode("queryAlertTypes");
//        queryCode("queryAlertCode");
//        queryCode("queryProjectDict");
//        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        System.out.println(simpleDateFormat.format(new Date()));
    }
    /**
     * @description 校验登录信息验证方法
     * @date 2017/11/29
     * @author Niemingming
     */
    public static void testPermission() throws IOException {
//        Hm_lvt_82116c626a8d504a5c0675073362ef6f:1511415004
//        gr_user_id:b3af81ba-bf0d-4f42-9ff0-b72f452dbc23
//        msid:b2f6fd7dZ6328f704Z160056e3a40Z933e
//        csid:7DCB590375B63B892298879990ADEB92
//        JSESSIONID:EFB56BADCE82EDB36F917C3E52A7A947
        HttpGet get = new HttpGet("http://t.c.haier.net/me");
        HttpClientContext context = HttpClientContext.create();
        CookieStore cookieStore = new BasicCookieStore();
//        context.setCookieStore(cookieStore);
        BasicClientCookie cookie = new BasicClientCookie("csid","6D975395247F0FA4B658B46193E180F6");
//        context.getCookieStore().addCookie(cookie);
        cookie.setDomain("t.c.haier.net");
        cookieStore.addCookie(cookie);
        cookie = new BasicClientCookie("JSESSIONID","EFB56BADCE82EDB36F917C3E52A7A947");
        cookie.setDomain("t.c.haier.net");
//        cookieStore.addCookie(cookie);
        HttpClient client = HttpClients.custom().setDefaultCookieStore(getCookieStore()).build();
        HttpResponse response = client.execute(get);

        System.out.println(response.getStatusLine());
        System.out.println(EntityUtils.toString(response.getEntity()));
    }
    /**
     * @description
     * 1999BFE7158F927B3175D4A30D233305
     */
    public static  BasicCookieStore getCookieStore(){
        BasicCookieStore store = new BasicCookieStore();
        BasicClientCookie cookie = new BasicClientCookie("csid","5356B5489080F28A6C224D1FE5A49CF9");
        cookie.setDomain(host);
        store.addCookie(cookie);
        cookie = new BasicClientCookie("JSESSIONID","51FD6243D6BB715B08768E8701920B03");
        cookie.setDomain(host);
        store.addCookie(cookie);
        cookie = new BasicClientCookie("msid","ba4b0254Z6328f704Z160714e49deZ9263");
        cookie.setDomain(host);
        store.addCookie(cookie);
        cookie = new BasicClientCookie("gr_user_id","9989a998-34cf-42a6-8267-3b9da89b722d");
        cookie.setDomain(host);
        store.addCookie(cookie);
        cookie = new BasicClientCookie("Hm_lvt_82116c626a8d504a5c0675073362ef6f","1511415004");
        cookie.setDomain(host);
        store.addCookie(cookie);

        return store;
    }

    public  static  void queryList() throws IOException {
        HttpClient client = HttpClients.custom().setDefaultCookieStore(getCookieStore()).build();
        HttpPost post = new HttpPost("http://" + host + ":8082/api/queryAlertingList");
        //不分页查询，列表查询为POST请求方式，条件为project=
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{")
                .append("   pageinfo:{currentPage:1,pageSize:7},")
                .append("  query:{")
                .append(" \"project\":\"HMMS\",")
                .append("   \"startsAt\":[\"2017-11-28 12:43:44\",\"2017-11-30 15:54:22\"],")
                .append(" \"alertCategory\":[\"machine\"]")
                .append("  }")
                .append("}");
        StringEntity stringEntity = new StringEntity(stringBuilder.toString());
        post.setEntity(stringEntity);
        HttpResponse response = client.execute(post);
        HttpEntity res = response.getEntity();
        System.out.println(EntityUtils.toString(res));
    }

    public static void queryById() throws IOException {
        HttpClient client = HttpClients.custom().setDefaultCookieStore(getCookieStore()).build();
        HttpGet get = new HttpGet("http://" + host + ":8082/api/queryAlertingById/C70C0CC99E24B87B5552578AB9FE4F70");
        HttpResponse response = client.execute(get);
        HttpEntity res = response.getEntity();
        System.out.println(EntityUtils.toString(res));
    }

    public static void queryHistoryList() throws IOException {
        HttpClient client = HttpClients.custom().setDefaultCookieStore(getCookieStore()).build();
        HttpPost post = new HttpPost("http://" + host + ":8082/api/queryHistoryList");
        //不分页查询，列表查询为POST请求方式，条件为project=
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{")
                .append("   pageinfo:{currentPage:1,pageSize:2},")
                .append("  query:{")
                .append("  \"project\":[\"JHZX\",\"HDYBC\"],")
                .append("   \"startsAt\":[\"2017-11-30 19\",\"2017-11-30 23:59:59\"]")
                .append("  }")
                .append("}");
        StringEntity stringEntity = new StringEntity(stringBuilder.toString(),"UTF-8");
        post.setEntity(stringEntity);
        HttpResponse response = client.execute(post);
        HttpEntity res = response.getEntity();
        System.out.println(EntityUtils.toString(res));
    }

    public static void queryHistoryById() throws IOException {
        HttpClient client = HttpClients.custom().setDefaultCookieStore(getCookieStore()).build();
        HttpGet get = new HttpGet("http://" + host + ":8082/api/queryHistoryById/alert-201711/F3D41251FB46DD2255B72CE34BDE502F-1512036158");
        HttpResponse response = client.execute(get);
        HttpEntity res = response.getEntity();
        System.out.println(EntityUtils.toString(res));
    }

    public static void  searchHistoryList() throws IOException {
        HttpClient client = HttpClients.custom().setDefaultCookieStore(getCookieStore()).build();
        HttpPost post = new HttpPost("http://" + host + ":8082/api/searchHistoryList/node-tcp-conn-toomuch");
        //不分页查询，列表查询为POST请求方式，条件为project=
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{")
                .append("   pageinfo:{currentPage:1,pageSize:2},")
                .append("  query:{")
                .append("  \"project\":[\"JHZX\",\"HDYBC\"],")
                .append("   \"startsAt\":[\"2017-11-30 19\",\"2017-12-30 23:59:59\"]")
                .append("  }")
                .append("}");
        StringEntity stringEntity = new StringEntity(stringBuilder.toString(),"UTF-8");
        post.setEntity(stringEntity);
        HttpResponse response = client.execute(post);
        HttpEntity res = response.getEntity();
        System.out.println(EntityUtils.toString(res));
    }

    public  static  void queryGroup() throws IOException {
        HttpClient client = HttpClients.custom().setDefaultCookieStore(getCookieStore()).build();
        HttpPost post = new HttpPost("http://" + host + ":8082/api/queryAlertingByGroup");
        //不分页查询，列表查询为POST请求方式，条件为project=
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{")
                .append("  query:{")
                .append(" \"project\":[\"project1\",\"project2\",\"HDYBC\",\"HMMS\"]")
                .append("  },")
                .append(" group:[\"project\"]")
                .append("}");
        StringEntity stringEntity = new StringEntity(stringBuilder.toString());
        post.setEntity(stringEntity);
        HttpResponse response = client.execute(post);
        HttpEntity res = response.getEntity();
        System.out.println(EntityUtils.toString(res));
    }

    public static void queryLevelCode() throws IOException {
        HttpClient client = HttpClients.custom().setDefaultCookieStore(getCookieStore()).build();
        HttpGet get = new HttpGet("http://" + host + ":8082/api/queryAlertLevels");
        HttpResponse response = client.execute(get);
        HttpEntity res = response.getEntity();
        System.out.println(EntityUtils.toString(res));
    }

    public static  void queryCode(String uri) throws IOException {
        HttpClient client = HttpClients.custom().setDefaultCookieStore(getCookieStore()).build();
        HttpGet get = new HttpGet("http://" + host + ":8082/api/" + uri);
        HttpResponse response = client.execute(get);
        HttpEntity res = response.getEntity();
        System.out.println(EntityUtils.toString(res));
    }
}
