package com.taojintianxia.github.cornucopia.freemarker.javagenerator;

import freemarker.template.Configuration;
import freemarker.template.Template;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Nianjun Sun
 * @date 2020/5/29 18:29
 */
public class FreemarkerDemo {
    private static final String TEMPLATE_PATH = "";
    private static final String CLASS_PATH = "com.taojintianxia.github.cornucopia.freemarker.javagenerator/hello";

    public static void main(String[] args) {
        // step1 创建freeMarker配置实例
        Configuration configuration = new Configuration();
        Writer out = null;
        try {
            // step2 获取模版路径
            configuration.setDirectoryForTemplateLoading(new File(TEMPLATE_PATH));
            // step3 创建数据模型
            Map<String, Object> dataMap = new HashMap<String, Object>();
            dataMap.put("classPath", "com.freemark.hello");
            dataMap.put("className", "User");
            dataMap.put("Id", "Id");
            dataMap.put("userName", "userName");
            dataMap.put("password","password");
            // step4 加载模版文件
            Template template = configuration.getTemplate("java.ftl");
            // step5 生成数据
            File docFile = new File(CLASS_PATH + "\\" + "User.java");
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(docFile)));
            // step6 输出文件
            template.process(dataMap, out);
            System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^User.java 文件创建成功 !");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != out) {
                    out.flush();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

}
