package com.chat.utils.aptos.request.v1.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class CoinStore implements Serializable {

    @JsonProperty("type")
    String type;

    @JsonProperty("data")
    Data data;

    @lombok.Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public class Data implements Serializable {

        @JsonProperty("coin")
        Coin coin;

        @JsonProperty("frozen")
        boolean frozen;

    }

}