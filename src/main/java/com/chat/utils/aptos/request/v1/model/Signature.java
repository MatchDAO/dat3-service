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
public class Signature implements Serializable {

    public static final String ED25519_SIGNATURE = "ed25519_signature";

    @JsonProperty("type")
    String type;

    @JsonProperty("public_key")
    String publicKey;

    @JsonProperty("signature")
    String signature;

}