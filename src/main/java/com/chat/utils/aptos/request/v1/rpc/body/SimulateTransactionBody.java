package com.chat.utils.aptos.request.v1.rpc.body;

import com.chat.utils.aptos.request.v1.model.SimulateTransaction;
import com.chat.utils.aptos.request.v1.model.SubmitTransaction;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author liqiang
 */
@Data
@NoArgsConstructor
public class SimulateTransactionBody extends SimulateTransaction implements IAptosRequestBody {

}