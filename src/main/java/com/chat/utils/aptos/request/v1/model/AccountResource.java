package com.chat.utils.aptos.request.v1.model;

import cn.hutool.json.JSONObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author liqiang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResource implements Serializable {

    @JsonProperty("type")
    String type;

    @JsonProperty("data")
    JSONObject data;

}