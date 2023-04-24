package com.chat.entity.dto;

import lombok.*;

@Data
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class WalletBalance {

    private String address;
    private String token;
    private String amount;
    private String frozen;
}
