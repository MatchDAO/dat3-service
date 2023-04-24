package com.chat.utils.aptos.request.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * @author liqiang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings({"rawtypes"})
public class TransactionViewPayload implements Serializable {



    @JsonProperty("function")
    String function;

    @JsonProperty("arguments")
    List arguments;

    @JsonProperty("type_arguments")
    List typeArguments;

}