package com.easy.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.easy.common.bean.User;
import com.easy.common.exception.ServiceException;
import com.easy.common.mapper.membermapper.IUserMapper;
import com.easy.common.service.IFileService;
import com.easy.common.util.*;
import com.easy.user.service.ISmsService;
import com.easy.user.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl extends ServiceImpl<IUserMapper, User> implements IUserService {

    /** 验证码缓存前缀 */
    private static final String SMS_CODE_PREFIX = "pension:sms:code:";
    /** 发送频率限制前缀 */
    private static final String SMS_FREQ_PREFIX = "pension:sms:freq:";
    /** 每日发送次数前缀 */
    private static final String SMS_DAILY_PREFIX = "pension:sms:daily:";
    /** 验证码有效期（秒） */
    private static final long SMS_CODE_EXPIRE_SECONDS = 300;
    /** 同一手机号发送间隔（秒） */
    private static final long SMS_FREQ_SECONDS = 60;
    /** 同一手机号每日发送上限 */
    private static final long SMS_DAILY_LIMIT = 10;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ISmsService smsService;

    @Autowired
    private IFileService fileService;

    @Override
    public Result sendSmsCode(String phone) {
        if (phone == null || !RegexUtil.phone(phone)) {
            throw new ServiceException(400, "手机号格式不正确");
        }
        //同一手机号60秒内只能发送一次
        Boolean first = redisTemplate.opsForValue()
                .setIfAbsent(SMS_FREQ_PREFIX + phone, "1", SMS_FREQ_SECONDS, TimeUnit.SECONDS);
        if (first == null || !first) {
            throw new ServiceException(400, "验证码发送过于频繁，请稍后再试");
        }
        //每日发送上限
        Long dailyCount = redisTemplate.opsForValue().increment(SMS_DAILY_PREFIX + phone);
        if (dailyCount != null && dailyCount == 1) {
            long secondsToMidnight = Duration.between(LocalDateTime.now(),
                    LocalDateTime.now().toLocalDate().plusDays(1).atStartOfDay()).getSeconds();
            redisTemplate.expire(SMS_DAILY_PREFIX + phone, secondsToMidnight, TimeUnit.SECONDS);
        }
        if (dailyCount != null && dailyCount > SMS_DAILY_LIMIT) {
            throw new ServiceException(400, "今日验证码发送次数已达上限");
        }
        //生成并缓存验证码
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        redisTemplate.opsForValue().set(SMS_CODE_PREFIX + phone, code, SMS_CODE_EXPIRE_SECONDS, TimeUnit.SECONDS);
        smsService.sendVerifyCode(phone, code);

        HashMap<String, Object> data = new HashMap<>();
        data.put("expire", SMS_CODE_EXPIRE_SECONDS);
        return Result.success("验证码已发送，5分钟内有效", data);
    }

    /**
     * 校验并消费短信验证码，校验通过后立即失效
     */
    private void verifySmsCode(String phone, String code) {
        if (code == null || code.isBlank()) {
            throw new ServiceException(400, "请输入短信验证码");
        }
        Object cached = redisTemplate.opsForValue().get(SMS_CODE_PREFIX + phone);
        if (cached == null || !code.equals(cached.toString())) {
            throw new ServiceException(400, "验证码错误或已过期");
        }
        redisTemplate.delete(SMS_CODE_PREFIX + phone);
    }

    @Override
    @Transactional
    public Result register(User user) {
        //验证手机号和密码是否符合规范
        if (user.getPhone() == null || !(RegexUtil.phone(user.getPhone()) && user.getPassword() != null
                && RegexUtil.password(user.getPassword()))) {
            throw new ServiceException(400, "输入的手机号或密码不符合规范");
        }
        //校验短信验证码
        verifySmsCode(user.getPhone(), user.getSmsCode());
        //验证用户是否重复
        LambdaQueryWrapper<User> lqw = new LambdaQueryWrapper<>();
        lqw.eq(User::getPhone, user.getPhone());
        long count = this.count(lqw);
        if (count > 0) {
            throw new ServiceException(400, "手机号已经被注册");
        }
        //密码加密
        user.setPassword(EncodeUtil.encode(user.getPassword()));
        //存储数据
        user.setPoints(1000);
        user.setRole("MEMBER");
        if (this.save(user)) {
            return Result.success("注册成功,赠送1000积分!!", null);
        } else {
            throw new ServiceException(500, "服务繁忙,稍后重试");
        }
    }

    @Override
    @Transactional
    public Result login(User user) {
        boolean smsLogin = user.getSmsCode() != null && !user.getSmsCode().isBlank();
        //格式：短信登录校验验证码，密码登录校验密码
        if (user.getPhone() == null || !RegexUtil.phone(user.getPhone())
                || (!smsLogin && (user.getPassword() == null || !RegexUtil.password(user.getPassword())))) {
            throw new ServiceException(400, "输入的手机号或密码不符合规范");
        }
        //找账号
        LambdaQueryWrapper<User> lqw = new LambdaQueryWrapper<>();
        lqw.eq(User::getPhone, user.getPhone());
        User one = this.getOne(lqw);
        if (one == null) {
            throw new ServiceException(400, smsLogin ? "手机号或验证码错误" : "手机号或密码错误");
        }
        //校验凭证
        if (smsLogin) {
            verifySmsCode(user.getPhone(), user.getSmsCode());
        } else if (!EncodeUtil.check(user.getPassword(), one.getPassword())) {
            throw new ServiceException(400, "手机号或密码错误");
        }
//        if (!one.getRole().equals("MEMBER")) {
//            throw new ServiceException(400, "只允许会员登录");
//        }
        if (!one.getStatus().equals("ENABLED")) {
            throw new ServiceException(400, "账号异常,请联系管理员");
        }

        String tokenType;
        if ("MEMBER".equals(one.getRole())) {
            tokenType = TokenUtil.USER_TYPE_USER;
        } else if ("ADMIN".equals(one.getRole())) {
            tokenType = TokenUtil.USER_TYPE_ADMIN;
        } else {
            throw new ServiceException(400, "该账号无登录权限");
        }
        //生成token
        TokenUtil.TokenPair pair = TokenUtil.getTokenPair(one.getId(), tokenType);
        //返回
        HashMap<String, Object> data = new HashMap<>();
        data.put("tokenPair", pair);
        //密码不能出后台
        one.setPassword(null);
        data.put("user", one);
        return Result.success("登陆成功", data);
    }

    @Override
    public Result refresh(String accessToken, String refreshToken) {
        if (!TokenUtil.verifyRefreshToken(refreshToken, accessToken)) {
            throw new ServiceException(401, "令牌验证失败");
        }
        // 获取Token中的userId
        Object useridObj = TokenUtil.getPayload(accessToken, TokenUtil.USER_KEY);
        // 获取Token中的用户类型（自动适配管理员/会员）
        Object userTypeObj = TokenUtil.getPayload(accessToken, TokenUtil.USER_TYPE_KEY);
        if (userTypeObj == null) {
            throw new ServiceException(401, "令牌类型无效");
        }
        // 用原Token的用户类型生成新Token
        TokenUtil.TokenPair pair = TokenUtil.getTokenPair(
                Long.parseLong(useridObj.toString()),
                userTypeObj.toString()
        );
        return Result.success(pair);
    }

    @Override
    public Result logout(String accessToken, String refreshToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            throw new ServiceException(401, "未登录");
        }
        if (!TokenUtil.verifyAccessToken(accessToken)) {
            throw new ServiceException(401, "令牌失效");
        }

        TokenUtil.addToBlackList(accessToken);

        if (refreshToken != null && !refreshToken.isEmpty()) {
            TokenUtil.addToBlackList(refreshToken);
        }
        return Result.success("退出成功", null);
    }

    @Override
    public Result getProfile() {

        Long userID = CurrentUser.getCurrentUserID();
        User user = this.getById(userID);
        if (userID == null) {
            throw new ServiceException(404, "用户不存在");
        }
        user.setPassword(null);
        return Result.success("获取成功", user);
    }

    @Override
    @Transactional
    public Result updateProfile(User user) {
        Long userID = CurrentUser.getCurrentUserID();
        User existing = this.getById(userID);
        if (existing == null) {
            throw new ServiceException(404, "用户不存在");
        }

        String oldAvatar = existing.getAvatar();
        boolean avatarChanged = user.getAvatar() != null
                && !user.getAvatar().equals(oldAvatar);

        // 只允许更新允许修改的字段
        if (user.getReal_name() != null) {
            existing.setReal_name(user.getReal_name());
        }
        if (user.getGender() != null) {
            existing.setGender(user.getGender());
        }
        if (user.getBirth_date() != null) {
            existing.setBirth_date(user.getBirth_date());
        }
        if (user.getAvatar() != null) {
            existing.setAvatar(user.getAvatar());
        }
        if (user.getMember_level() != null) {
            existing.setMember_level(user.getMember_level());
        }

        if (user.getPhone() != null) {
            LambdaQueryWrapper<User> lqw = new LambdaQueryWrapper<>();
            lqw.eq(User::getPhone, user.getPhone())
                    .ne(User::getId, userID);
            if (this.count(lqw) > 0) {
                throw new ServiceException(400, "手机号已被其他账号使用");
            }
            existing.setPhone(user.getPhone());
        }

        if (this.updateById(existing)) {
            if (avatarChanged) {
                deleteOldAvatarAfterCommit(oldAvatar, existing.getAvatar());
            }
            existing.setPassword(null);
            return Result.success("更新成功", existing);
        } else {
            throw new ServiceException(500, "更新失败，稍后重试");
        }
    }

    /**
     * 数据库事务提交后再删除旧头像，避免资料保存失败时误删仍在使用的头像。
     */
    private void deleteOldAvatarAfterCommit(String oldAvatar, String newAvatar) {
        if (oldAvatar == null || oldAvatar.equals(newAvatar)) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    fileService.deleteAvatarByUrl(oldAvatar);
                }
            });
        } else {
            fileService.deleteAvatarByUrl(oldAvatar);
        }
    }

    @Override
    @Transactional
    public Result changePassword(String oldPassword, String newPassword) {
        Long userID = CurrentUser.getCurrentUserID();
        User user = this.getById(userID);
        if (user == null) {
            throw new ServiceException(404, "用户不存在");
        }

        //校验原密码是否正确
        if (!EncodeUtil.check(oldPassword, user.getPassword())) {
            throw new ServiceException(400, "原密码错误");
        }

        if (!RegexUtil.password(newPassword)) {
            throw new ServiceException(400, "新密码不符合规范");
        }

        user.setPassword(EncodeUtil.encode(newPassword));
        if (this.updateById(user)) {
            return Result.success("密码修改成功", null);
        } else {
            throw new ServiceException(500, "密码修改失败,请稍后重试");
        }
    }

    @Override
    @Transactional
    public Result resetPassword(String phone, String newPassword, String smsCode) {
        if (phone == null || !RegexUtil.phone(phone)) {
            throw new ServiceException(400, "手机号格式不正确");
        }
        if (newPassword == null || !RegexUtil.password(newPassword)) {
            throw new ServiceException(400, "密码不符合规范");
        }
        //校验短信验证码
        verifySmsCode(phone, smsCode);

        LambdaQueryWrapper<User> lqw = new LambdaQueryWrapper<>();
        lqw.eq(User::getPhone, phone);
        User user = this.getOne(lqw);
        if (user == null) {
            throw new ServiceException(404, "该手机号未注册");
        }

        user.setPassword(EncodeUtil.encode(newPassword));
        if (this.updateById(user)) {
            return Result.success("密码重置成功，请使用新密码登录", null);
        } else {
            throw new ServiceException(500, "密码重置失败，请稍后重试");
        }
    }

}
