package com.haier.interx.converter;

import com.google.gson.JsonObject;

/**
 * @description 查询条件转换器
 * @date 2017/11/29
 * @author Niemingming
 */
public interface QueryParamConverter {
    /**
     * @description 对查询条件进行处理
     * @date 2017/11/29
     * @author Niemingming
     */
    public JsonObject convertParam(JsonObject queryCon);
}
