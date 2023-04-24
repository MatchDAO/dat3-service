package com.chat.entity.dto;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 
 * </p>
 *
 * @author jetBrains
 * @since 2022-10-20
 */

@Data
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class UserDto implements Serializable {

    private static final long serialVersionUID = 11214235342523L;

    private String userCode;

    private String userName;

    private String email;


    private String wallet;

    private String portrait;

    private String status;

    private LocalDateTime createdTime;

    private String invitationCode;
    private Boolean firstLogin;

    private String emUuid;
    private String bio;
    private String tag;

    private String address;
    private Integer gender;

}
