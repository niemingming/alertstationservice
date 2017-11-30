package com.haier.interx.converter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.haier.interx.config.ServiceConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @description 告警站的参数转换器。这里主要处理日期格式，由字符串转为Unix秒
 * @date 2017/11/29
 * @author Niemingming
 */
@Component
public class AlertStationConverter implements  QueryParamConverter{
    /*日期格式串*/
    private String format = "yyyy-MM-dd";
    @Autowired
    private ServiceConfiguration serviceConfiguration;

    /**
     * @description 高精站查询条件处理。主要处理日期查询条件
     * @date 2017/11/29
     * @author Niemingming
     */
    @Override
    public JsonObject convertParam(JsonObject queryCon) {
        List<String> keys = serviceConfiguration.getAlertstationconverter();
        for (String key : queryCon.keySet()){
            JsonObject newObj = new JsonObject();
            //需要转换的日期格式,且不为空
            if (keys.contains(key)&&!queryCon.get(key).isJsonNull()){
                if (queryCon.get(key).isJsonPrimitive()){
                    JsonPrimitive startDate = queryCon.get(key).getAsJsonPrimitive();
                    dealWithDate(newObj,startDate,"$gte");
                    //覆盖原来的key值
                    queryCon.add(key,newObj);
                }else if (queryCon.get(key).isJsonArray()){
                    JsonArray dates = queryCon.get(key).getAsJsonArray();
                    //判断数组长度，如果为1，表示开始之后。
                    if (dates.size() == 1){
                        dealWithDate(newObj,dates.get(0).getAsJsonPrimitive(),"$gte");
                    }else if (dates.size() == 2){
                        dealWithDate(newObj,dates.get(0).getAsJsonPrimitive(),"$gte");
                        dealWithDate(newObj,dates.get(1).getAsJsonPrimitive(),"$lt");
                    }
                    queryCon.add(key,newObj);
                }
            }
        }
        return null;
    }
    /**
     * @description 处理日期字符串
     * @date 2017/11/29
     * @author Niemingming
     */
    private void dealWithDate(JsonObject newObj, JsonPrimitive startDate, String expression) {
        if (startDate.isNumber()){//如果是数字，表示只传了开始时间
            newObj.addProperty(expression,startDate.getAsLong());
        }else if (startDate.isString()){//如果是字符串，表示传了日期格式
            newObj.addProperty(expression,getDateUnixSecond(startDate.getAsString()));
        }
    }

    /**
     * @description 根据字符串获得Unix秒
     * @date 2017/11/29
     * @author Niemingming
     */
    private Long getDateUnixSecond(String datestr) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        try {
            Date date = simpleDateFormat.parse(datestr);
            return date.getTime()/1000;
        } catch (ParseException e) {
            e.printStackTrace();
            return 0l;
        }
    }
}
