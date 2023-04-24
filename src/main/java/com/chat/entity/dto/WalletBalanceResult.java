package com.chat.entity.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class WalletBalanceResult {
    private int code;
    private List<WalletBalance> balances;
    private String message;
}
