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
public class Event<T> implements Serializable {

    @JsonProperty("guid")
    Guid guid;

    @JsonProperty("sequence_number")
    String sequenceNumber;

    @JsonProperty("type")
    String type;

    @JsonProperty("data")
    T data;

}