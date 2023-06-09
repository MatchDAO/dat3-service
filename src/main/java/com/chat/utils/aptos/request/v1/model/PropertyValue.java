package com.chat.utils.aptos.request.v1.model;

import cn.hutool.core.collection.CollUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

/**
 * @author liqiang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyValue implements Serializable {

    public final static Set EXCLUDE_KEY = CollUtil.set(true,"TOKEN_BURNABLE_BY_CREATOR", "TOKEN_BURNABLE_BY_OWNER", "TOKEN_PROPERTY_MUTATBLE", "TOKEN_PROPERTY_MUTABLE");

    @JsonProperty("value")
    String value;

    @JsonProperty("type")
    String type;

}