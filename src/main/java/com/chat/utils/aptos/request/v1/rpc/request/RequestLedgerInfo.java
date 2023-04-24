package com.chat.utils.aptos.request.v1.rpc.request;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author liqiang
 */
@Data
@Builder
@NoArgsConstructor
public class RequestLedgerInfo implements IAptosRequest {

    @Override
    public String path() {
        return "/v1";
    }

}