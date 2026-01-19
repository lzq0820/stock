package com.liuzhq.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.python.core.PyFunction;
import org.python.core.PyInteger;
import org.python.core.PyString;
import org.python.util.PythonInterpreter;

/**
 * Jython调用Python代码（仅支持Python 2.x）
 */
@Slf4j
public class JythonCaller {

    public static String executePythonCode(String codes, int klineType) {
        // 创建Python解释器
        try (PythonInterpreter interpreter = new PythonInterpreter()) {
            // 执行Python代码（直接嵌入）
            String pythonCode = "                import json\n" +
                                "                def get_stock_kline(codes, kline_type):\n" +
                                "                    result = {\n" +
                                "                        \"ret\": 200,\n" +
                                "                        \"msg\": \"success\",\n" +
                                "                        \"data\": [\n" +
                                "                            {\"code\": code, \"kline_type\": kline_type, \"price\": 100 + i}\n" +
                                "                            for i, code in enumerate(codes.split(\",\"))\n" +
                                "                        ]\n" +
                                "                    }\n" +
                                "                    return json.dumps(result, ensure_ascii=False)";
            interpreter.exec(pythonCode);

            // 获取Python函数并调用
            PyFunction function = interpreter.get("get_stock_kline", PyFunction.class);
            String result = (String) function.__call__(
                    new PyString(codes),
                    new PyInteger(klineType)
            ).__tojava__(String.class);

            return result;
        } catch (Exception e) {
            log.error("Jython执行Python代码异常", e);
            return "{\"ret\":500,\"msg\":\"" + e.getMessage() + "\"}";
        }
    }

    // 测试方法
    public static void main(String[] args) {
        String result = executePythonCode("TSLA.US,AAPL.US", 8);
        System.out.println(result);
    }
}