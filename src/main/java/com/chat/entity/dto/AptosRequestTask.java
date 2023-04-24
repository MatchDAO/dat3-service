package com.chat.entity.dto;

import com.chat.utils.aptos.request.v1.model.TransactionPayload;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AptosRequestTask {
    public String sig;
    public TransactionPayload payload;
    public Boolean simulate;
}