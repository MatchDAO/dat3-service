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
public class GasEstimate implements Serializable {

    @JsonProperty("gas_estimate")
    long gasEstimate;
    @JsonProperty("deprioritized_gas_estimate")
    long deprioritizedGasEstimate;
    @JsonProperty("prioritized_gas_estimate")
    long prioritizedGasEstimate;

}