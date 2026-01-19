package com.liuzhq.stock.BaseInfo.dto.innerClass;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RelatedPlate {

    @JSONField(name = "plate_name") // 反序列化映射plate_name
    @JsonProperty("plateName") // 序列化返回plateName
    private String plateName;

    @JSONField(name = "plate_reason")
    @JsonProperty("plateReason")
    private String plateReason;

}