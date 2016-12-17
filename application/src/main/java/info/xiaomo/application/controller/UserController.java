package info.xiaomo.application.controller;

import info.xiaomo.application.model.UserModel;
import info.xiaomo.application.service.UserService;
import info.xiaomo.core.constant.Err;
import info.xiaomo.core.constant.GenderType;
import info.xiaomo.core.controller.BaseController;
import info.xiaomo.core.controller.Result;
import info.xiaomo.core.exception.UserNotFoundException;
import info.xiaomo.core.untils.MD5Util;
import info.xiaomo.core.untils.MailUtil;
import info.xiaomo.core.untils.RandomUtil;
import info.xiaomo.core.untils.TimeUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.hibernate.service.spi.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.List;

/**
 * 把今天最好的表现当作明天最新的起点．．～
 * いま 最高の表現 として 明日最新の始発．．～
 * Today the best performance  as tomorrow newest starter!
 * Created by IntelliJ IDEA.
 *
 * @author: xiaomo
 * @github: https://github.com/qq83387856
 * @email: hupengbest@163.com
 * @QQ_NO: 83387856
 * @Date: 2016/4/1 17:51
 * @Description: 用户控制器
 * @Copyright(©) 2015 by xiaomo.
 **/
@RestController
@RequestMapping("/user")
@Api(value = "UserController", description = "用户相关api")
public class UserController extends BaseController {

    private final UserService service;

    @Autowired
    public UserController(UserService service) {
        this.service = service;
    }

    /**
     * 根据id 查找用户
     *
     * @param id id
     * @return result
     */
    @ApiOperation(value = "查找用户", notes = "查找用户", httpMethod = "GET", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @RequestMapping(value = "findById/{id}", method = RequestMethod.GET)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "唯一id", required = true, dataType = "Long", paramType = "path"),
    })
    public Result findUserById(@PathVariable("id") Long id) {
        UserModel userModel = service.findUserById(id);
        if (userModel == null) {
            return new Result(Err.USER_NOT_FOUND.getCode(), Err.USER_NOT_FOUND.getMessage());
        }
        return new Result(userModel);
    }

    /**
     * 添加用户
     */
    @ApiOperation(value = "添加用户", notes = "添加用户", httpMethod = "POST", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @RequestMapping(value = "addUser", method = RequestMethod.POST)
    public Result addUser(@RequestBody UserModel user) {
        UserModel userModel = service.findUserByEmail(user.getEmail());
        if (userModel != null) {
            return new Result(Err.USER_REPEAT.getCode(), Err.USER_REPEAT.getMessage());
        }
        String salt = RandomUtil.createSalt();
        user.setPassword(MD5Util.encode(user.getPassword(), salt));
        user.setSalt(salt);
        service.addUser(user);
        return new Result(user);
    }

    /**
     * 注册
     *
     * @return result
     */
    @ApiOperation(value = "注册", notes = "注册用户并发送验证链接到邮箱", httpMethod = "POST", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "用户名", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "密码", required = true, dataType = "String", paramType = "path")
    })
    @RequestMapping(value = "register/{email}/{password}", method = RequestMethod.POST)
    public Result register(@PathVariable("email") String email, @PathVariable("password") String password) throws Exception {
        UserModel userModel = service.findUserByEmail(email);
        //邮箱被占用
        if (userModel != null) {
            return new Result(Err.USER_REPEAT.getCode(), Err.USER_REPEAT.getMessage());
        }
        String redirectValidateUrl = MailUtil.redirectValidateUrl(email, password);
        MailUtil.send(email, "帐号激活邮件", redirectValidateUrl);
        return new Result(redirectValidateUrl);
    }


    /**
     * 登录
     *
     * @return result
     */
    @ApiOperation(value = "登录", notes = "登录", httpMethod = "POST", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "email", value = "邮箱", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "password", value = "密码", required = true, dataType = "String", paramType = "path")
    })
    @RequestMapping(value = "login/{email}/{password}", method = RequestMethod.GET)
    public Result login(@PathVariable("email") String email, @PathVariable("password") String password) {
        UserModel userModel = service.findUserByEmail(email);
        //找不到用户
        if (userModel == null) {
            return new Result(Err.USER_NOT_FOUND.getCode(), Err.USER_NOT_FOUND.getMessage());
        }
        //密码不正确
        if (!MD5Util.encode(password, userModel.getSalt()).equals(userModel.getPassword())) {
            return new Result(Err.AUTH_FAILED.getCode(), Err.AUTH_FAILED.getMessage());
        }
        return new Result(userModel);
    }


    /**
     * 修改密码
     *
     * @return model
     * @throws UserNotFoundException UserNotFoundException
     */
    @ApiOperation(value = "修改密码", notes = "修改密码", httpMethod = "POST", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @RequestMapping(value = "changePassword", method = RequestMethod.POST)
    public Result changePassword(@RequestBody UserModel user) throws UserNotFoundException {
        UserModel userByEmail = service.findUserByEmail(user.getEmail());
        if (userByEmail == null) {
            return new Result(Err.USER_NOT_FOUND.getCode(), Err.USER_NOT_FOUND.getMessage());
        }
        String salt = RandomUtil.createSalt();
        userByEmail.setPassword(MD5Util.encode(user.getPassword(), salt));
        userByEmail.setNickName(user.getNickName());
        userByEmail.setSalt(salt);
        UserModel updateUser = service.updateUser(userByEmail);
        return new Result(updateUser);
    }

    /**
     * 更新用户信息
     *
     * @return model
     * @throws UserNotFoundException UserNotFoundException
     */
    @ApiOperation(value = "更新用户信息", notes = "更新用户信息", httpMethod = "POST", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @RequestMapping(value = "update", method = RequestMethod.POST)
    public Result update(@RequestBody UserModel user) throws UserNotFoundException {
        UserModel userModel = service.findUserByEmail(user.getEmail());
        if (userModel == null) {
            return new Result(Err.USER_NOT_FOUND.getCode(), Err.USER_NOT_FOUND.getMessage());
        }
        userModel = new UserModel();
        userModel.setEmail(user.getEmail());
        userModel.setNickName(user.getNickName());
        UserModel updateUser = service.updateUser(userModel);
        return new Result(updateUser);
    }

    /**
     * 返回所有用户数据
     *
     * @return result
     */
    @ApiOperation(value = "返回所有用户数据", notes = "返回所有用户数据", httpMethod = "GET", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @RequestMapping(value = "findAll", method = RequestMethod.GET)
    public Result getAll() {
        List<UserModel> pages = service.findAll();
        if (pages == null || pages.size() <= 0) {
            return new Result(Err.NULL_DATA.getCode(), Err.NULL_DATA.getMessage());
        }
        return new Result(pages);
    }


    /**
     * 根据id删除用户
     *
     * @param id id
     * @return result
     */
    @RequestMapping(value = "delete/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "根据id删除用户", notes = "根据id删除用户", httpMethod = "GET", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "唯一id", required = true, dataType = "Long", paramType = "path"),
    })
    public Result deleteUserById(@PathVariable("id") Long id) throws UserNotFoundException {
        UserModel userModel = service.deleteUserById(id);
        if (userModel == null) {
            return new Result(Err.USER_NOT_FOUND.getCode(), Err.USER_NOT_FOUND.getMessage());
        }
        return new Result(userModel);
    }


}
