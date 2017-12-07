package com.haier.interx.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.haier.interx.config.ServiceConfiguration;
import com.haier.interx.converter.AlertStationConverter;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Date;

/**
 * @description 对外服务接口
 * @date 2017/11/28
 * @author Niemingming
 */
@Controller
@RequestMapping("/api")
public class ApiService {
    @Autowired
    private ServiceConfiguration serviceConfiguration;
    @Autowired
    private AlertStationConverter alertStationConverter;

    /**
     * @description 查询当前告警列表，默认会加上登录人项目条件,POST请求
     * POST /api/queryAlertingList/{personId}?addpro=1  //可以不传addpro参数。默认是1
     * {
     *     pageinfo:{//分页信息如果不传，表示不分页
     *         currentPage:10, //当前第几页，从1开始
     *         pageSize:10  //查询多少条，默认是10，
     *     },
     *     query:{//查询条件，遵循mongo的查询格式
     *          alertname:"testalert",
     *          "labels.job":"tomcat",
     *          times:{$gte:"10"}
     *     }
     * }
     * 返回数据格式为：
     * {
     *     success:bool(true/false),//表示是否成功
     *     code:0/1 ,//执行结果编码，目前只有0成功，1失败
     *     total:long,//表示查询到的记录数
     *     currentTime:1500000,//服务调用时间
     *     data:{
     *         page:{
     *             total:4,
     *             currentPage:1
     *         },
     *         list:[]
     *     }, //表示返回的记录列表详情
     *     hint:string //服务器返回的提示信息，一般在访问失败时给出。
     * }
     * @date 2017/11/21
     * @author Niemingming
     */
    @ResponseBody
    @RequestMapping("/queryAlertingList")
    public void queryAlertingList(@RequestBody String queryCon,
                                  @RequestParam(value = "addpro",defaultValue = "1") String addpro,
                                  HttpServletRequest request, HttpServletResponse response){

        String endpoint = serviceConfiguration.getAlertstationurl() + "/queryAlertingList";
        //传入endpoint调用后台POST服务
        invokePostRequest(endpoint, queryCon,addpro,request,response);

    }
    /**
     * @description 调用post服务接口
     * @date 2017/11/29
     * @author Niemingming
     */
    private void invokePostRequest(String endpoint, String queryCon,
                                   String addpro, HttpServletRequest request,
                                   HttpServletResponse response) {
        Gson gson = new Gson();
        JsonObject queryJson = gson.fromJson(queryCon,JsonObject.class);
        //获取权限信息
        CheckResult checkResult = checkPermissionByPersonId(request);
        //如果没有权限，返回提示信息
        if (!checkResult.hasPermission){
            writeNoPermission(checkResult,response);
            return;
        }
        //如果有过滤条件才处理
        if (queryJson.get("query") != null
                && queryJson.get("query").isJsonObject()){
            //处理日期格式
            alertStationConverter.convertParam(queryJson.get("query").getAsJsonObject());
        }
        //鉴权通过返回的鉴权信息中含有工号。
        if ("1".equals(addpro)){//如果需要追加校验信息
            try {
                addProFilter(queryJson,checkResult.info);
            } catch (IOException e) {
                e.printStackTrace();
                checkResult.info = "获取人员相关项目异常！";
                writeNoPermission(checkResult,response);
                return;
            }
        }
        try {
            System.out.println(gson.toJson(queryJson));
            JsonObject invokRes = sendPost(endpoint,gson.toJson(queryJson));
            invokRes.addProperty("currentTime",new Date().getTime()/1000);
            writeInvokeResult(invokRes,response);
        } catch (IOException e) {
            e.printStackTrace();
            checkResult.info = "调用接口服务异常！";
            writeNoPermission(checkResult,response);
            return;
        }
    }

    /**
     * @description 根据记录id查询告警详情
     * 请求方式：
     * GET /api/queryAlertingById/id
     * 返回结果：
     * {
     *     success:bool(true/false),//表示是否成功
     *     code:0/1 ,//执行结果编码，目前只有0成功，1失败
     *     currentTime:1500000,//服务调用时间
     *     data:{}, //表示返回的记录详情
     *     hint:string //服务器返回的提示信息，一般在访问失败时给出。
     * }
     * @date 2017/11/22
     * @author Niemingming
     */
    @ResponseBody
    @RequestMapping(value = "/queryAlertingById/{id}",method = RequestMethod.GET)
    public void queryAlertingById(@PathVariable String id,HttpServletRequest request,HttpServletResponse response) {
        String endpoint = serviceConfiguration.getAlertstationurl() + "/queryAlertingById/" + id;
        invokeGetRequest(endpoint,request,response);
    }

    /**
     * @description 根据查询条件，查询历史记录，POST请求方式
     *POST /api/queryHistoryList
     * {
     *     pageinfo:{//分页信息如果不传，表示不分页
     *         currentPage:10, //当前第几页，从1开始计数
     *         pageSize:10  //查询多少条，默认是10，
     *     },
     *     query:{//查询条件，遵循mongo的查询格式
     *          alertname:"testalert",
     *          "labels.job":"tomcat",
     *          times:{$gte:"10"}
     *     }
     * }
     * 返回数据格式为：
     * {
     *     success:bool(true/false),//表示是否成功
     *     code:0/1 ,//执行结果编码，目前只有0成功，1失败
     *     total:long,//表示查询到的记录数
     *     currentTime:150000,//服务调用时间
     *     data:{
     *         page:{
     *             total:4,
     *             currentPage:1
     *         },
     *         list:[]
     *     }, //表示返回的记录列表详情
     *     hint:string //服务器返回的提示信息，一般在访问失败时给出。
     * }
     *
     * @date 2017/11/21
     * @author Niemingming
     */
    @ResponseBody
    @RequestMapping(value = "/queryHistoryList",method = RequestMethod.POST)
    public void queryHistoryList(@RequestBody String queryCon,
                                 @RequestParam(value = "addpro",defaultValue = "1") String addpro,
                                 HttpServletRequest request,HttpServletResponse response) {
        String endpoint = serviceConfiguration.getAlertstationurl() + "/queryHistoryList";
        invokePostRequest(endpoint,queryCon,addpro,request,response);
    }

    /**
     * @description 根据index和id查询历史详情
     * GET /api/queryHistoryById/{index}/{id}
     * 返回数据格式为：
     * {
     *     success:bool(true/false),//表示是否成功
     *     code:0/1 ,//执行结果编码，目前只有0成功，1失败
     *     currentTime:150000,//服务调用时间
     *     data:{}, //表示返回的记录列表详情
     *     hint:string //服务器返回的提示信息，一般在访问失败时给出。
     * }
     *
     *
     * @date 2017/11/22
     * @author Niemingming
     */
    @ResponseBody
    @RequestMapping(value = "/queryHistoryById/{index}/{id}",method = RequestMethod.GET)
    public void queryHistoryById (@PathVariable String index,@PathVariable String id ,
                                  HttpServletRequest request,HttpServletResponse response) {
        String endpoint = serviceConfiguration.getAlertstationurl() + "/queryHistoryById/" + index + "/" + id;
        invokeGetRequest(endpoint,request,response);
    }

    /**
     * @description 根据关键字查询历史告警记录
     * 查询格式
     * GET /api/searchHistoryList/{searchstr}
     * 返回数据格式：
     * {
     *     success:bool(true/false),//表示是否成功
     *     code:0/1 ,//执行结果编码，目前只有0成功，1失败
     *     currentTime:150000,//服务调用时间
     *     total:long,//表示查询到的记录数
     *     data:[], //表示返回的记录列表详情
     *     hint:string //服务器返回的提示信息，一般在访问失败时给出。
     * }
     * @date 2017/11/22
     * @author Niemingming
     */
    @ResponseBody
    @RequestMapping("/searchHistoryList/{searchstr}")
    public void searchHistoryList(@PathVariable String searchstr,@RequestParam(value = "currentPage",defaultValue = "1") int currentPage,
                                  @RequestParam(value = "pageSize",defaultValue = "10") int pageSize,HttpServletRequest request,HttpServletResponse response) {
        String endpoint = serviceConfiguration.getAlertstationurl() + "/searchHistoryList/"
                + searchstr + "?currentPage=" + currentPage + "&pageSize=" + pageSize;
        invokeGetRequest(endpoint,request,response);
    }

    /**
     * @description 提供按照某个指定字段分组查询功能POST请求
     * POST /api/queryAlertingByGroup
     * {
     *     query:{//查询条件，遵循mongo的查询格式
     *          alertname:"testalert",
     *          "labels.job":"tomcat",
     *          times:{$gte:"10"}
     *     },
     *     group:["level","labels.project"]//可以按照多个字段，也可以按照一个字段
     * }
     * 返回数据格式为：
     * {
     *     success:bool(true/false),//表示是否成功
     *     code:0/1 ,//执行结果编码，目前只有0成功，1失败
     *     currentTime:1500000,//服务调用时间
     *     data:{
     *         page:{
     *             total:4,
     *             currentPage:1
     *         },
     *         list:[]
     *     }, //表示返回的记录列表详情
     * @date 2017/11/26
     * @author Niemingming
     */
    @ResponseBody
    @RequestMapping(value="/queryAlertingByGroup",method = RequestMethod.POST)
    public void queryAlertingByGroup(@RequestBody String queryCon, @RequestParam(value = "addpro",defaultValue = "1")String addpro,
                                     HttpServletRequest request,HttpServletResponse response) {
        String endpoint = serviceConfiguration.getAlertstationurl() + "/queryAlertingByGroup";
        invokePostRequest(endpoint,queryCon,addpro,request,response);
    }
    /**
     * @description 查询公共编码中的告警级别编码
     * GET /api/queryAlertLevels
     * 返回数据：
     * {
     *      success:true,
     *      code:0/1,
     *      currentTime:150000,//服务调用时间
     *      data:{
     *          error:"紧急",
     *          warn:"严重",
     *          info:"一般",
     *          debug:"提示"
     *      }
     * }
     * @date 2017/11/27
     * @author Niemingming
     */
    @ResponseBody
    @RequestMapping("/queryAlertLevels")
    public void queryAlertLevels(HttpServletRequest request,HttpServletResponse response) {
        String endpoint = serviceConfiguration.getAlertstationurl() + "/queryAlertLevels";
        invokeGetRequest(endpoint,request,response);
    }
    /**
     * @description 查询告警分类
     * GET /api/queryAlertCategories
     * 返回数据：
     * {
     *      success:true,
     *      code:0/1,
     *      currentTime:1500000,//服务调用时间
     *      data:{
     *          machine:"机器",
     *          app:"应用"
     *      }
     * }
     * @date 2017/11/28
     * @author Niemingming
     */
    @ResponseBody
    @RequestMapping("/queryAlertCategories")
    public void queryAlertCategories(HttpServletRequest request,HttpServletResponse response) {
        String endpoint = serviceConfiguration.getAlertstationurl() + "/queryAlertCategories";
        invokeGetRequest(endpoint,request,response);
    }
    /**
     * @description 查询告警类型
     * GET /api/queryAlertTypes
     * 返回数据：
     * {
     *      success:true,
     *      code:0/1,
     *      currentTime:150000,//服务调用时间
     *      data:{
     *         cockpit_schedule_task_exit: 容器实例退出,
     *         mysql_status_handlers_read_rnd: mysql索引不合理,
     *         service_down: 服务不可用,
     *         node_reboot: 系统重启,
     *         node_cpu_pct_threshold_exceeded: 节点CPU使用率过高,
     *         node_mem_threshold_exceeded: 节点剩余内存不足,
     *         node_mem_pct_threshold_exceeded: 节点内存使用率过高,
     *         node_fs_pct_threshold_exceeded: 节点文件系统使用率过高,
     *         node_tcp_conn_toomuch: 节点TCP连接数过高,
     *         node_disk_io_util_threshold_exceeded: 节点磁盘IO过高,
     *         redis_service_down: redis服务不可用,
     *         redis_mem_pct_threshold_exceeded: Redis内存使用率过高,
     *         redis_mem_threshold_exceeded: Redis内存不足,
     *         redis_toomany_command_executed: Redis命令执行频繁,
     *         redis_dangerous_command_executed: Redis执行危险命令
     *      }
     * }
     * @date 2017/11/28
     * @author Niemingming
     */
    @ResponseBody
    @RequestMapping("/queryAlertTypes")
    public void queryAlertTypes(HttpServletRequest request,HttpServletResponse response) {
        String endpoint = serviceConfiguration.getAlertstationurl() + "/queryAlertTypes";
        invokeGetRequest(endpoint,request,response);
    }

    /**
     * @description 查询告警所有公共编码
     * GET /api/queryAlertCode
     * 返回数据：
     * {
     *      success:true,
     *      code:0/1,
     *      currentTime:1500000,//服务调用时间
     *      data:{
     *      alertLevel:{
     *          error:"紧急",
     *          warn:"严重",
     *          info:"一般",
     *          debug:"提示"
     *      },
     *      alertCategory:{
     *          machine:"机器",
     *          app:"应用"
     *      },
     *      alertType:{
     *         cockpit_schedule_task_exit: 容器实例退出,
     *         mysql_status_handlers_read_rnd: mysql索引不合理,
     *         service_down: 服务不可用,
     *         node_reboot: 系统重启,
     *         node_cpu_pct_threshold_exceeded: 节点CPU使用率过高,
     *         node_mem_threshold_exceeded: 节点剩余内存不足,
     *         node_mem_pct_threshold_exceeded: 节点内存使用率过高,
     *         node_fs_pct_threshold_exceeded: 节点文件系统使用率过高,
     *         node_tcp_conn_toomuch: 节点TCP连接数过高,
     *         node_disk_io_util_threshold_exceeded: 节点磁盘IO过高,
     *         redis_service_down: redis服务不可用,
     *         redis_mem_pct_threshold_exceeded: Redis内存使用率过高,
     *         redis_mem_threshold_exceeded: Redis内存不足,
     *         redis_toomany_command_executed: Redis命令执行频繁,
     *         redis_dangerous_command_executed: Redis执行危险命令
     *      }
     *      }
     * }
     * @date 2017/11/28
     * @author Niemingming
     */
    @ResponseBody
    @RequestMapping("/queryAlertCode")
    public void queryAlertCode(HttpServletRequest request,HttpServletResponse response) {
        String endpoint = serviceConfiguration.getAlertstationurl() + "/queryAlertCode";
        invokeGetRequest(endpoint,request,response);
    }
    /**
     * @description 查询登录人项目数据字典
     * @date 2017/11/30
     * @author Niemingming
     */
    @ResponseBody
    @RequestMapping("/queryProjectDict")
    public void queryProjectDict(HttpServletRequest request,HttpServletResponse response) {
        //首先校验登录人信息
        CheckResult checkResult = checkPermissionByPersonId(request);
        if (!checkResult.hasPermission){
            writeNoPermission(checkResult,response);
            return;
        }
        //权限通过后获取权限信息
        String endpoint = serviceConfiguration.getAuthorityurl() + checkResult.info;
        JsonObject result = new JsonObject();
        result.addProperty("success",false);
        result.addProperty("code",1);
        try {
            JsonObject projects = sendGet(endpoint);
            if (projects.get("data") != null){
                JsonArray dataList = projects.get("data").getAsJsonArray();
                JsonObject resPorjecs = new JsonObject();
                for (int i = 0; i < dataList.size(); i++ ) {
                    JsonObject project = dataList.get(i).getAsJsonObject();
                    String pro = "";
                    //判断是否由分组简称，如果有用分组，否则用项目简称
                    if (!project.get("projectMachineAbbreviated").isJsonNull()
                            && !"".equals(project.get("projectMachineAbbreviated").getAsString())) {
                        pro = project.get("projectMachineAbbreviated").getAsString();
                    }else if (project.get("projectAlmAbbreviated") != null){
                        pro = project.get("projectAlmAbbreviated").getAsString();
                    }
                    //获取项目的almname
                    String proName = project.get("projectAlmName").isJsonNull() ? "" : project.get("projectAlmName").getAsString();
                    if (resPorjecs.get(pro) != null && !resPorjecs.get(pro).isJsonNull()) {
                        resPorjecs.addProperty(pro,resPorjecs.get(pro).getAsString() + "," + proName);
                    }else {
                        resPorjecs.addProperty(pro,proName);
                    }
                }
                result.add("data",resPorjecs);
                result.addProperty("success",true);
                result.addProperty("code",0);
            }
        } catch (IOException e) {
            e.printStackTrace();
            result.addProperty("msg","调用项目获取接口出现异常！");
        }
        //输出内容
        writeInvokeResult(result,response);

    }


    /**
     * @description 调用get请求服务
     * @date 2017/11/29
     * @author Niemingming
     */
    private void invokeGetRequest(String endpoint, HttpServletRequest request, HttpServletResponse response) {
        //获取权限信息
        CheckResult checkResult = checkPermissionByPersonId(request);
        //如果没有权限，返回提示信息
        if (!checkResult.hasPermission){
            writeNoPermission(checkResult,response);
            return;
        }
        try {
            Gson gson = new Gson();
            JsonObject invokRes = sendGet(endpoint);
            invokRes.addProperty("currentTime",new Date().getTime()/1000);
            writeInvokeResult(invokRes,response);
        } catch (IOException e) {
            e.printStackTrace();
            checkResult.info = "调用接口服务异常！";
            writeNoPermission(checkResult,response);
            return;
        }
    }

    /**
     * @description 在查询条件中增加项目的过滤条件
     * @date 2017/11/29
     * @author Niemingming
     */
    private void addProFilter(JsonObject queryJson, Object info) throws IOException {
        //获取所有查询条件对象
        if (queryJson.get("query") == null){
            queryJson.add("query",new JsonObject());
        }
        JsonObject query = queryJson.get("query").getAsJsonObject();

        //获取人员相关项目
        String endpoint = serviceConfiguration.getAuthorityurl() + info;
        JsonObject projects = sendGet(endpoint);
        if (projects.get("data") != null){
            JsonArray dataList = projects.get("data").getAsJsonArray();
            JsonArray projectFilter = new JsonArray();
            for (int i = 0; i < dataList.size(); i++ ) {
                JsonObject project = dataList.get(i).getAsJsonObject();
                String pro = "";
                //判断是否由分组简称，如果有用分组，否则用项目简称
                if (!project.get("projectMachineAbbreviated").isJsonNull()
                        && !"".equals(project.get("projectMachineAbbreviated").getAsString())) {
                    pro = project.get("projectMachineAbbreviated").getAsString();
                }else if (project.get("projectAlmAbbreviated") != null){
                    pro = project.get("projectAlmAbbreviated").getAsString();
                }
                projectFilter.add(pro);
            }
            //判断有没有传递项目条件，如果传入了，那么就以传入为准。不在附加
            //表示传入了项目，需要校验有没有查询该项目的权限
            if (query.get("project") != null){
                //目前只至此字符串和数组两种方式。
                if (query.get("project").isJsonPrimitive()) {
                    //如果为字符串，且不被包含，name该条件不应该传入
                    if (!projectFilter.contains(query.get("project"))) {
                        query.addProperty("project","");
                    }
                }else if (query.get("project").isJsonArray()) {
                    //如果为数组，需要遍历判断
                    JsonArray custormPros = query.get("project").getAsJsonArray();
                    for (int j = custormPros.size() - 1; j >= 0; j-- ){
                        try{
                            if (!projectFilter.contains(custormPros.get(j))){
                                custormPros.remove(j);
                            }
                        }catch (Exception e){
                            //出现异常，表示异常数据
                            custormPros.remove(j);
                        }
                    }
                }else {
                    //其他情况下认为，要查询的这次数据没有记录。
                    query.addProperty("project","");
                }
                return;
            }else {
                query.add("project",projectFilter);
            }
        }else if (query.get("project") != null){
            //无项目权限，但是传入了项目条件，需要去除
            query.addProperty("projecct","");
        }
    }

    /**
     * @description 输出无权限提示信息
     * @date 2017/11/29
     * @author Niemingming
     */
    private void writeNoPermission(CheckResult checkResult, HttpServletResponse response) {
        try {
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type","application/json");
            StringBuilder msg = new StringBuilder("{code:1,success:false,msg:")
                    .append(checkResult.info).append("}");
            response.getWriter().write(msg.toString());
            response.getWriter().flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * @description 输出接口调用返回结果
     * @date 2017/11/29
     * @author Niemingming
     */
    private void writeInvokeResult(JsonObject invokRes, HttpServletResponse response) {
        try {
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type","application/json");
            response.getWriter().write(new Gson().toJson(invokRes));
            response.getWriter().flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @description 根据登录人信息校验访问权限
     * 登录校验需要访问Http://t.c.haier.net/me
     * 同时携带cookie
     * csid即可。这里我们将接受到的cookie转发即可。
     * @date 2017/11/29
     * @author Niemingming
     */
    private CheckResult checkPermissionByPersonId(HttpServletRequest request) {
        CheckResult result = new CheckResult();
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            result.hasPermission = false;
            result.info = "未查询到登录人信息，请先登录！";
            return result;
        }
        //添加登录信息cookie
        BasicCookieStore cookieStore = new BasicCookieStore();
        for (Cookie cookie: cookies){
            BasicClientCookie clientCookie = new BasicClientCookie(cookie.getName(),cookie.getValue());
            clientCookie.setDomain(serviceConfiguration.getPermissionhost());
            cookieStore.addCookie(clientCookie);
        }
        HttpClient client = HttpClients.custom().setDefaultCookieStore(cookieStore).build();
        HttpGet get = new HttpGet(serviceConfiguration.getPermissionurl());
        try {
            HttpResponse response = client.execute(get);
            if (response.getStatusLine().getStatusCode() >= 400){
                get.abort();
                result.hasPermission = false;
                result.info = "登录信息有误，请重新登录！";
                return result;
            }
            String logininfo = EntityUtils.toString(response.getEntity());
            //获取工号
            JsonObject login = new Gson().fromJson(logininfo,JsonObject.class);
            if (login.get("username") == null ||"".equals(login.get("username"))){
                result.hasPermission = false;
                result.info = "未获取到登录人信息！";
                return result;
            }
            result.hasPermission = true;
            result.info = login.get("username").getAsString();
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            result.hasPermission = false;
            result.info = "验证登录信息出现异常！";
            return result;
        }
    }
    /**
     * @description 发送get请求方法体
     * @date 2017/11/29
     * @author Niemingming
     */
    private JsonObject sendGet(String endpoint) throws IOException {
        HttpGet get = new HttpGet(endpoint);
        return sendRequest(get);
    }
    /**
     * @description 发送post请求，并携带body请求体
     * @date 2017/11/29
     * @author Niemingming
     */
    private JsonObject sendPost(String endpoint, String requestBody) throws IOException {
        HttpPost post = new HttpPost(endpoint);
        StringEntity body = new StringEntity(requestBody,"UTF-8");
        body.setContentType("application/json;charset=UTF-8");
        post.setEntity(body);
        return sendRequest(post);
    }
    /**
     * @description 发送请求，并返回访问结果
     * @date 2017/11/29
     * @author Niemingming
     */
    private JsonObject sendRequest(HttpRequestBase request) throws IOException {
        HttpClient client = HttpClients.createDefault();
        //设置超时时间
        RequestConfig config = RequestConfig.custom().setSocketTimeout(10000).setConnectTimeout(10000).setConnectionRequestTimeout(10000).build();
        request.setConfig(config);
        try{
            HttpResponse response = client.execute(request);
            //如果连接失败，我们需要显示的通知终止连接
            if (response.getStatusLine().getStatusCode() != 200){
                request.abort();
            }
            return new Gson().fromJson(EntityUtils.toString(response.getEntity()),JsonObject.class);
        }catch (SocketTimeoutException e) {
            request.abort();//主动关闭连接
            e.printStackTrace();
            //访问超时
            JsonObject timeout = new JsonObject();
            timeout.addProperty("success",false);
            timeout.addProperty("code",1);
            timeout.addProperty("msg","接口服务访问超时!");
            return timeout;
        }

    }

    /**
     * @description 鉴权结果实体
     * @date 2017/11/29
     * @author Niemingming
     */
    public class CheckResult {
        /*是否具有权限*/
       public boolean hasPermission;
       /*附加信息*/
       public Object info;
    }

}
