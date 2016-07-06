/**
 * Copyright (c) 2015-2016, Chill Zhuang 庄骞 (smallchill@163.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.smallchill.system.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.smallchill.common.base.BaseController;
import com.smallchill.common.vo.User;
import com.smallchill.core.annotation.Before;
import com.smallchill.core.annotation.Permission;
import com.smallchill.core.aop.AopContext;
import com.smallchill.core.constant.ConstShiro;
import com.smallchill.core.interfaces.ILoader;
import com.smallchill.core.interfaces.IQuery;
import com.smallchill.core.plugins.dao.Blade;
import com.smallchill.core.plugins.dao.Db;
import com.smallchill.core.shiro.ShiroKit;
import com.smallchill.core.toolbox.Func;
import com.smallchill.core.toolbox.Maps;
import com.smallchill.core.toolbox.ajax.AjaxResult;
import com.smallchill.core.toolbox.kit.CacheKit;
import com.smallchill.core.toolbox.kit.StrKit;
import com.smallchill.system.meta.intercept.PasswordValidator;
import com.smallchill.system.meta.intercept.UserIntercept;
import com.smallchill.system.meta.intercept.UserValidator;
import com.smallchill.system.model.RoleExt;

@Controller
@RequestMapping("/user")
public class UserController extends BaseController implements ConstShiro{
	private static String LIST_SOURCE = "User.list";
	private static String BASE_PATH = "/system/user/";
	private static String CODE = "user";
	private static String PERFIX = "TFW_USER";

	@RequestMapping("/")
	@Permission({ ADMINISTRATOR, ADMIN })
	public ModelAndView index() {
		ModelAndView view = new ModelAndView(BASE_PATH + "user.html");
		view.addObject("code", CODE);
		return view;
	}
	
	/**
	 * 分页aop
	 * 普通用法
	 */
	@ResponseBody
	@RequestMapping(KEY_LIST)
	@Permission({ ADMINISTRATOR, ADMIN })
	public Object list() {
		Object gird = paginate(LIST_SOURCE, new UserIntercept());
		return gird;
	}
	
	@RequestMapping(KEY_ADD)
	@Permission({ ADMINISTRATOR, ADMIN })
	public ModelAndView add() {
		ModelAndView view = new ModelAndView(BASE_PATH + "user_add.html");
		view.addObject("code", CODE);
		return view;
	}
	
	@RequestMapping(KEY_EDIT + "/{id}")
	@Permission({ ADMINISTRATOR, ADMIN })
	public ModelAndView edit(@PathVariable String id) {
		ModelAndView view = new ModelAndView(BASE_PATH + "user_edit.html");
		User user = Blade.create(User.class).findById(id);
		Maps maps = Maps.parse(user);
		maps.set("roleName", Func.getRoleName(user.getRoleid()));
		view.addObject("user", maps);
		view.addObject("code", CODE);
		return view;
	}
	
	@RequestMapping("/editMySelf/{id}")
	public ModelAndView editMySelf(@PathVariable String id) {
		ModelAndView view = new ModelAndView(BASE_PATH + "user_edit.html");
		User user = Blade.create(User.class).findById(id);
		Maps maps = Maps.parse(user);
		maps.set("roleName", Func.getRoleName(user.getRoleid()));
		view.addObject("user", maps);
		view.addObject("code", CODE);
		view.addObject("methodName", "editMySelf");
		return view;
	}
	
	@RequestMapping("/editPassword/{id}")
	public ModelAndView editPassword(@PathVariable String id){
		ModelAndView view = new ModelAndView(BASE_PATH + "user_edit_password.html");
		User user = Blade.create(User.class).findById(id);
		view.addObject("user", user);
		view.addObject("code", CODE);
		return view;
	}

	@ResponseBody
	@Before(PasswordValidator.class)
	@RequestMapping("/updatePassword")
	public AjaxResult updatePassword(){
		Blade blade = Blade.create(User.class);
		String userId = getPara("user.id");
		String password = getPara("user.newPassword");
		User user = blade.findById(userId);
		String salt = user.getSalt();
		user.setPassword(ShiroKit.md5(password, salt));
		user.setVersion(user.getVersion() + 1);
		boolean temp = blade.update(user);
		if (temp) {
			return success(UPDATE_SUCCESS_MSG);
		} else {
			return error(UPDATE_FAIL_MSG);
		}
	}

	@RequestMapping(KEY_VIEW + "/{id}")
	@Permission({ ADMINISTRATOR, ADMIN })
	public ModelAndView view(@PathVariable String id) {
		ModelAndView view = new ModelAndView(BASE_PATH + "user_view.html");
		User user = Blade.create(User.class).findById(id);
		Maps maps = Maps.parse(user);
		maps.set("deptName", Func.getDeptName(user.getDeptid()))
			.set("roleName", Func.getRoleName(user.getRoleid()))
			.set("sexName", Func.getDictName(101, user.getSex()));
		view.addObject("user", maps);
		view.addObject("code", CODE);
		return view;
	}
	
	
	@ResponseBody
	@Before(UserValidator.class)
	@RequestMapping(KEY_SAVE)
	@Permission({ ADMINISTRATOR, ADMIN })
	public AjaxResult save() {
		User user = mapping(PERFIX, User.class);
		String pwd = user.getPassword();
		String salt = ShiroKit.getRandomSalt(5);
		String pwdMd5 = ShiroKit.md5(pwd, salt);
		user.setPassword(pwdMd5);
		user.setSalt(salt);
		user.setStatus(3);
		user.setCreatetime(new Date());
		boolean temp = Blade.create(User.class).save(user);
		if (temp) {
			CacheKit.removeAll(DEPT_CACHE);
			CacheKit.removeAll(DICT_CACHE);
			CacheKit.removeAll(USER_CACHE);
			return success(SAVE_SUCCESS_MSG);
		} else {
			return error(SAVE_FAIL_MSG);
		}
	}
	
	@ResponseBody
	@Before(UserValidator.class)
	@RequestMapping(KEY_UPDATE)
	public AjaxResult update() {
		User user = mapping(PERFIX, User.class);
		if(StrKit.notBlank(PERFIX + "PASSWORD")){
			String pwd = user.getPassword();
			User oldUser = Blade.create(User.class).findById(user.getId());
			if(!pwd.equals(oldUser.getPassword())){
				String salt = oldUser.getSalt();
				String pwdMd5 = ShiroKit.md5(pwd, salt);
				user.setPassword(pwdMd5);
			}
		}
		user.setVersion(getParaToInt("VERSION", 0) + 1);
		boolean temp = Blade.create(User.class).update(user);
		if (temp) {
			CacheKit.removeAll(DEPT_CACHE);
			CacheKit.removeAll(DICT_CACHE);
			CacheKit.removeAll(USER_CACHE);
			return success(UPDATE_SUCCESS_MSG);
		} else {
			return error(UPDATE_FAIL_MSG);
		}
	}

	@ResponseBody
	@RequestMapping(KEY_DEL)
	@Permission({ ADMINISTRATOR, ADMIN })
	public AjaxResult del(@RequestParam String ids) {
		boolean temp = Blade.create(User.class).updateBy("status = #{status}", "id in (#{join(ids)})", Maps.create().set("status", 5).set("ids", ids.split(",")));
		if (temp) {
			return success(DEL_SUCCESS_MSG);
		} else {
			return error(DEL_FAIL_MSG);
		}
	}
	
	@ResponseBody
	@RequestMapping("/reset")
	@Permission({ ADMINISTRATOR, ADMIN })
	public AjaxResult reset(@RequestParam String ids) {
		Blade blade = Blade.create(User.class);
		String [] idArr = ids.split(",");
		int cnt = 0;
		for(String id : idArr){
			User user = blade.findById(id);
			String pwd = "111111";
			String salt = user.getSalt();
			String pwdMd5 = ShiroKit.md5(pwd, salt);
			user.setVersion(((user.getVersion() == null) ? 0 : user.getVersion()) + 1);
			user.setPassword(pwdMd5);
			boolean temp = blade.update(user);
			if(temp){
				cnt++;
			}
		}
		if (cnt == idArr.length) {
			return success("重置密码成功");
		} else {
			return error("重置密码失败");
		}
	}
	
	@ResponseBody
	@RequestMapping("/auditOk")
	public AjaxResult auditOk(@RequestParam String ids) {
		Blade blade = Blade.create(User.class);
		Maps countMap = Maps.create().set("ids", ids.split(","));
		int cnt = blade.count("id in (#{join(ids)}) and (roleId='' or roleId is null)", countMap);
		if (cnt > 0) {
			return warn("存在没有分配角色的账号!");
		}
		Maps updateMap = Maps.create().set("status", 1).set("ids", ids.split(","));
		boolean temp = blade.updateBy("status = #{status}", "id in (#{join(ids)})", updateMap);
		if (temp) {
			return success("审核成功!");
		} else {
			return error("审核失败!");
		}
	}
	
	@ResponseBody
	@RequestMapping("/auditRefuse")
	public AjaxResult auditRefuse(@RequestParam String ids) {
		Maps updateMap = Maps.create().set("status", 4).set("ids", ids.split(","));
		boolean temp = Blade.create(User.class).updateBy("status = #{status}", "id in (#{join(ids)})", updateMap);
		if (temp) {
			return success("审核拒绝成功!");
		} else {
			return error("审核拒绝失败!");
		}
	}
	
	@ResponseBody
	@RequestMapping("/ban")
	public AjaxResult ban(@RequestParam String ids) {
		Maps updateMap = Maps.create().set("ids", ids.split(","));
		boolean temp = Blade.create(User.class).updateBy("status = (CASE WHEN STATUS=2 THEN 3 ELSE 2 END)", "id in (#{join(ids)})", updateMap);
		if (temp) {
			return success("操作成功!");
		} else {
			return error("操作失败!");
		}
	}
	
	@ResponseBody
	@RequestMapping("/restore")
	public AjaxResult restore(@RequestParam String ids) {
		Maps updateMap = Maps.create().set("status", 3).set("ids", ids.split(","));
		boolean temp = Blade.create(User.class).updateBy("status = #{status}", "id in (#{join(ids)})", updateMap);
		if (temp) {
			return success("还原成功!");
		} else {
			return error("还原失败!");
		}
	}
	
	@ResponseBody
	@RequestMapping(KEY_REMOVE)
	public AjaxResult remove(@RequestParam String ids) {
		boolean temp = Blade.create(User.class).deleteByIds(ids) > 0;
		if (temp) {
			CacheKit.removeAll(USER_CACHE);
			return success("删除成功!");
		} else {
			return error("删除失败!");
		}
	}
	
	@RequestMapping("/extrole/{id}/{roleName}")
	public ModelAndView extrole(@PathVariable String id, @PathVariable String roleName) {
		User user = Blade.create(User.class).findById(id);
		String roleId = user.getRoleid();
		ModelAndView view = new ModelAndView(BASE_PATH + "user_extrole.html");
		view.addObject("userId", id);
		view.addObject("roleId", roleId);
		view.addObject("roleName", Func.decodeUrl(roleName));
		return view;
	}
	
	@ResponseBody
	@RequestMapping("/menuTreeIn")
	public AjaxResult menuTreeIn(@RequestParam String userId) {
		Db dao = Db.init();
		Map<String, Object> roleIn = dao.selectOne("select ROLEIN from tfw_role_ext where userId = #{userId}", Maps.create().set("userId",userId));
		
		String in = "0";
		if (!Func.isEmpty(roleIn)) {
			in = Func.format(roleIn.get("ROLEIN"));
		}
		
		StringBuilder sb = Func.builder(
				"select m.id \"id\",(select id from tfw_menu  where code=m.pCode) \"pId\",name \"name\",(case when m.levels=1 then 'true' else 'false' end) \"open\",(case when r.id is not null then 'true' else 'false' end) \"checked\"",
				" from tfw_menu m",
				" left join (select id from tfw_menu where id in (" + in + ")) r",
				" on m.id=r.id",
				" where m.status=1 order by m.levels,m.num asc"
				);
		
		List<Maps> menu = dao.selectList(sb.toString());
		return json(menu);
	}
	
	@ResponseBody
	@RequestMapping("/menuTreeOut")
	public AjaxResult menuTreeOut(@RequestParam String userId) {
		Db dao = Db.init();
		Map<String, Object> roleOut = dao.selectOne("select ROLEOUT from tfw_role_ext where userId = #{userId}", Maps.create().set("userId",userId));
		
		String out = "0";
		if (!Func.isEmpty(roleOut)) {
			out = Func.format(roleOut.get("ROLEOUT"));
		}
		
		StringBuilder sb = Func.builder(
				"select m.id \"id\",(select id from tfw_menu  where code=m.pCode) \"pId\",name \"name\",(case when m.levels=1 then 'true' else 'false' end) \"open\",(case when r.id is not null then 'true' else 'false' end) \"checked\"",
				" from tfw_menu m",
				" left join (select id from tfw_menu where id in (" + out + ")) r",
				" on m.id=r.id",
				" where m.status=1 order by m.levels,m.num asc"
				);
		
		List<Maps> menu = dao.selectList(sb.toString());
		return json(menu);
	}
	
	@ResponseBody
	@RequestMapping("/saveRoleExt")
	public AjaxResult saveRoleExt() {
		Blade blade = Blade.create(RoleExt.class);
		String userId = getPara("userId");
		String roleIn = getPara("idsIn", "0");
		String roleOut = getPara("idsOut", "0");
		RoleExt roleExt = blade.findFirstBy("userId = #{userId}", Maps.create().set("userId", userId));	
		boolean flag = false;
		if (Func.isEmpty(roleExt)) {
			roleExt = new RoleExt();
			flag = true;
		}
		roleExt.setUserid(userId);  
		roleExt.setRolein(roleIn); 
		roleExt.setRoleout(roleOut);
		if (flag) {
			blade.save(roleExt);
		} else {
			blade.update(roleExt);
		}
		CacheKit.removeAll(MENU_CACHE);
		return success("配置成功!"); 
	}
	
	@RequestMapping("/roleAssign/{id}/{name}/{roleId}")
	public ModelAndView roleAssign(@PathVariable String id, @PathVariable String name, @PathVariable String roleId) {
		ModelAndView view = new ModelAndView(BASE_PATH + "user_roleassign.html");
		setAttr("id", id);
		setAttr("roleId", roleId);
		setAttr("name", Func.decodeUrl(name));
		return view;
	}
	
	@ResponseBody
	@RequestMapping("/saveRole")
	public AjaxResult saveRole(@RequestParam String id, @RequestParam String roleIds) {
		Maps maps = Maps.create();
		maps.set("roleIds", roleIds).set("id", id.split(","));
		boolean temp = Blade.create(User.class).updateBy("ROLEID = #{roleIds}", "id in (#{join(id)})", maps);
		if (temp) {
			CacheKit.removeAll(ROLE_CACHE);
			CacheKit.removeAll(MENU_CACHE);
			return success("配置成功!");
		} else {
			return error("配置失败!");
		}
	}
	
	@ResponseBody
	@RequestMapping("/userTreeList")
	public AjaxResult userTreeList() {
		List<Map<String, Object>> dept = CacheKit.get(DEPT_CACHE, "user_tree_all",
				new ILoader() {
					public Object load() {
						return Db.init().selectList("select id \"id\",pId \"pId\",simpleName as \"name\",(case when (pId=0 or pId is null) then 'true' else 'false' end) \"open\" from  TFW_DEPT order by pId,num asc", Maps.create(), new AopContext(), new IQuery() {
							
							@Override
							public void queryBefore(AopContext ac) {
								// TODO Auto-generated method stub
								
							}
							
							@SuppressWarnings("unchecked")
							@Override
							public void queryAfter(AopContext ac) {
								List<Map<String, Object>> list = (List<Map<String, Object>>) ac.getObject();
								List<Map<String, Object>> _list = new ArrayList<>(); 
								for (Map<String, Object> map : list) {
									String [] deptIds = map.get("id").toString().split(",");
									List<User> users = Blade.create(User.class).findBy("DEPTID in (#{join(deptId)})", Maps.create().set("deptId", deptIds));
									for (User user : users) {
										for (String deptId : deptIds) {
											Map<String, Object> userMap = Maps.createHashMap();
											userMap.put("id", user.getId() + 9999);
											userMap.put("pId", deptId);
											userMap.put("name", user.getName());
											userMap.put("open", "false");
											userMap.put("iconSkin", "iconPerson");
											_list.add(userMap);
										}
									}
								}
								list.addAll(_list);
							}
						});
					}
				});

		return json(dept);
	}
	
	
}
