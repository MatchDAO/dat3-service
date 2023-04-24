package com.chat.utils.aptos.request.v1.model;

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
public class Change<T> implements Serializable {

    @JsonProperty("address")
    String address;

    @JsonProperty("state_key_hash")
    String stateKeyHash;

    @JsonProperty("type")
    String type;

    @JsonProperty("data")
    T data;

}