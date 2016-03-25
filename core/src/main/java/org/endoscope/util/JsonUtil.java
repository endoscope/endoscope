package org.endoscope.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JsonUtil {
    private ObjectMapper mapper;

    public JsonUtil(){
        this(false);
    }
    public JsonUtil(boolean indent){
        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        if( indent ){
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
        }
    }

    public String toJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void toJson(Object obj, File file) {
        try {
            mapper.writeValue(file, obj);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void toJson(Object obj, OutputStream out) throws IOException {
        mapper.writeValue(out, obj);
    }

    public <T> T fromJson(Class<T> clazz, String json) {
        try {
            return mapper.readValue(json, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T fromJson(Class<T> clazz, File file) {
        try {
            return mapper.readValue(file, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T fromJson(Class<T> clazz, InputStream src){
        try {
            return mapper.readValue(src, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
