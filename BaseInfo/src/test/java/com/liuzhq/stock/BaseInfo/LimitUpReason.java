package com.liuzhq.stock.BaseInfo;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import com.alibaba.fastjson.JSONObject;

public class LimitUpReason {
    public static void main(String[] args) throws Exception {
        String url = "https://flash-api.xuangubao.cn/api/pool/detail?pool_name=limit_up";
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet(url);
        String response = EntityUtils.toString(client.execute(get).getEntity(), "UTF-8");
        JSONObject json = JSONObject.parseObject(response);
        System.out.println("涨停原因列表：" + json.getJSONObject("data").getJSONArray("items"));
        client.close();
    }
}