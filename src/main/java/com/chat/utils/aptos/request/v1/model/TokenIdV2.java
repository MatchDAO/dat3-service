package com.chat.utils.aptos.request.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author liqiang
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenIdV2 extends TokenId {

    @JsonProperty("owner")
    String owner;

    public String getTokenIdOwner() {
        return this.getNftUniqueKeyVersion() + "@" + this.owner;
    }

    public static TokenIdV2 fromTokenIdOwner(String value) {
        String[] values = value.split("@");

        TokenIdV2 tokenIdV2 = new TokenIdV2();
        tokenIdV2.setTokenDataId(TokenDataId.builder()
                .creator(values[0])
                .collection(values[1])
                .name(values[2])
                .build());
        tokenIdV2.setPropertyVersion(values[3]);
        tokenIdV2.setOwner(values[4]);

        return tokenIdV2;
    }

}