package com.chat.utils.aptos.request.v1.rpc.body;

import com.chat.utils.aptos.request.v1.model.SubmitTransaction;
import com.chat.utils.aptos.request.v1.model.TransactionViewPayload;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author liqiang
 */
@Data
@NoArgsConstructor
public class EncodeViewBody extends TransactionViewPayload implements IAptosRequestBody {

}