package com.liuzhq.common.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * JSON序列化/反序列化工具类（底层：Jackson）
 */
@Component
public class JsonUtils {
    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    // 改为 private static final，确保静态方法可访问
    private static final ObjectMapper mapper;

    // 移除非必要的getter，静态mapper直接使用
    // public ObjectMapper getMapper() { return mapper; }

    static {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DEFAULT_DATE_FORMAT);

        mapper = new ObjectMapper();
        mapper.setDateFormat(dateFormat);
        mapper.setAnnotationIntrospector(new JacksonAnnotationIntrospector() {
            private static final long serialVersionUID = -5854941510519564900L;

            @Override
            public Object findSerializer(Annotated a) {
                if (a instanceof AnnotatedMethod) {
                    AnnotatedElement m = a.getAnnotated();
                    DateTimeFormat an = m.getAnnotation(DateTimeFormat.class);
                    if (an != null && !DEFAULT_DATE_FORMAT.equals(an.pattern())) {
                        return new JsonDateSerializer(an.pattern());
                    }
                }
                return super.findSerializer(a);
            }
        });
        // 新增：忽略未知字段（JSON中有但类中没有的字段，避免反序列化失败）
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static JSONObject empty() {
        return new JSONObject();
    }

    /**
     * 对象转JSON字符串
     */
    public static String toJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("转换json字符失败! 目标对象：" + (obj != null ? obj.getClass().getName() : "null"), e);
        }
    }

    /**
     * JSON字符串转单个对象（非静态方法，需注入Bean调用）
     */
    public <T> T toObject(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return mapper.readValue(json, clazz);
        } catch (IOException e) {
            throw new RuntimeException("将json字符转换为对象时失败! 目标类型：" + clazz.getName(), e);
        }
    }

    /**
     * JSON字符串转泛型对象（非静态方法）
     */
    public <T> T toObject(String json, TypeReference<T> typeReference) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return mapper.readValue(json, typeReference);
        } catch (IOException e) {
            throw new RuntimeException("将json字符转换为泛型对象时失败! 目标类型：" + typeReference.getType().getTypeName(), e);
        }
    }

    /**
     * JSON字符串转对象数组（非静态方法）
     */
    public <T> T[] toArray(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return mapper.readValue(json, mapper.getTypeFactory().constructArrayType(clazz));
        } catch (IOException e) {
            throw new RuntimeException("将json字符转换为对象数组时失败! 数组元素类型：" + clazz.getName(), e);
        }
    }

    /**
     * JSON字符串转List集合（改为静态方法，修复调用逻辑）
     */
    public static <T> List<T> toList(String json, Class<T> elementType) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            // 构建List<elementType>的类型
            return mapper.readValue(
                    json,
                    mapper.getTypeFactory().constructCollectionType(List.class, elementType)
            );
        } catch (IOException e) {
            // 增强异常信息：打印原始JSON，便于排查
            throw new RuntimeException(
                    "将json字符转换为List集合时失败! 集合元素类型：" + elementType.getName()
                            + "，原始JSON：" + json.substring(0, Math.min(json.length(), 200)), // 只打印前200字符，避免过长
                    e
            );
        }
    }

    public static <T> List<T> parseArray(String jsonStr, Class<T> clazz) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            throw new RuntimeException("JSON字符串为空");
        }
        try {
            // 方式1：FastJSON直接解析数组
            return JSON.parseArray(jsonStr, clazz);
        } catch (Exception e) {
            throw new RuntimeException("将json字符转换为List集合时失败! 集合元素类型：" + clazz.getName(), e);
        }
    }

    /**
     * 自定义Date序列化器
     */
    public static class JsonDateSerializer extends JsonSerializer<Date> {
        private SimpleDateFormat dateFormat;

        public JsonDateSerializer(String format) {
            dateFormat = new SimpleDateFormat(format);
        }

        @Override
        public void serialize(Date date, JsonGenerator gen, SerializerProvider provider) throws IOException {
            if (date != null) {
                gen.writeString(dateFormat.format(date));
            } else {
                gen.writeNull();
            }
        }
    }
}