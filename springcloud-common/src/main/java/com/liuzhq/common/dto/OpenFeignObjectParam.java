package com.liuzhq.common.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;

/**
 * OpenFeignObjectParam
 * @Description feign 中传对象参数方式
 * @Author liuzhq
 * @Date 2023/12/11 20:42
 * @Version 1.0
 */
@Data
@ApiModel
@AllArgsConstructor
@NoArgsConstructor
public class OpenFeignObjectParam implements Serializable {

    @ApiModelProperty(value = "id", required = true)
    private Integer id;

    @ApiModelProperty(value = "姓名", required = true)
    private String name;

    @ApiModelProperty(value = "年龄", required = true)
    private Integer age;

    @ApiModelProperty(value = "文件", required = true)
    private MultipartFile file;

    @ApiModelProperty(value = "文件数组", required = true)
    private MultipartFile[] files;

    @ApiModelProperty(value = "返回结果")
    private String result;

    public OpenFeignObjectParam(String name, Integer age) {
        this.name = name;
        this.age = age;
    }
}
