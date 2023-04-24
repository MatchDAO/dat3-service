package com.chat.entity.dto;

import lombok.*;

@Data
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class WalletTicker {
    private String symbol;
    private String symbol1;
    private String symbol2;
    private String price;
}
