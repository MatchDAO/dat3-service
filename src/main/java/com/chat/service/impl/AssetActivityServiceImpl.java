package com.chat.service.impl;

import com.chat.common.R;
import com.chat.entity.AssetActivity;
import com.chat.mapper.AssetActivityMapper;
import com.chat.service.AssetActivityService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.utils.MessageUtils;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author jetBrains
 * @since 2022-11-17
 */
@Service
public class AssetActivityServiceImpl extends ServiceImpl<AssetActivityMapper, AssetActivity> implements AssetActivityService {

 public R test(){
     return R.success(MessageUtils.getLocale("contact.us","SSS"));
    }
}
