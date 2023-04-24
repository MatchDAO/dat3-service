package com.chat.controller;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chat.common.AuthToken;
import com.chat.common.ChatPage;
import com.chat.common.R;
import com.chat.entity.dto.CreatorDto;
import com.chat.entity.dto.CreatorSearchDto;
import com.chat.service.impl.CreatorServiceImpl;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * <p>
 * 首页popular用户
 * </p>
 *
 * @author jetBrains
 * @since 2022-11-17
 */
@RestController
@RequestMapping()
//@AuthToken
public class CreatorController {

    @Resource
    private CreatorServiceImpl creatorService;

    @PostMapping("/popular")
    public R popular(@RequestBody(required = false) CreatorSearchDto query) {
        Page<CreatorDto> page = new Page();
        if (query != null) {
            Integer current = query.getCurrent();
            Integer size = query.getSize();
            page.setCurrent((current == null || current < 0) ? 0 : current);
            page.setSize((size == null || size < 0 || size > 20) ? 10 : size);
        }

        ChatPage<CreatorDto> creatorDtos = creatorService.popular(page, query);
        if (!CollUtil.isEmpty(creatorDtos.getRecords())) {
            return R.success(creatorDtos);
        }
        return R.success();
    }

    @PostMapping("/report")
    public R report(@RequestBody CreatorSearchDto query) {
        System.out.println(query);
        return R.success(query);
    }

}
