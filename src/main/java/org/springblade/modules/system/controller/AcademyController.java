package org.springblade.modules.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.constant.RoleConstant;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.system.entity.Academy;
import org.springblade.modules.system.query.AcademyQuery;
import org.springblade.modules.system.service.AcademyService;
import org.springblade.modules.system.service.MajorService;
import org.springblade.modules.system.vo.AcademyVO;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 学院信息控制器
 *
 * @author Jover Zhang
 * @date 2021/10/25
 */
@RestController
@Api(tags = "学院信息")
@RequiredArgsConstructor
@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
@RequestMapping("/blade-system/academy")
public class AcademyController extends BladeController {

    private final AcademyService service;

    private final MajorService majorService;

    @GetMapping("/{id}")
    @ApiOperation("查看详情")
    @ApiOperationSupport(order = 1)
    public R<AcademyVO> detail(@PathVariable("id") long id) {
        return R.data(convert(service.getById(id)));
    }

    @GetMapping
    @ApiOperation("分页")
    @ApiOperationSupport(order = 2)
    public R<IPage<AcademyVO>> getPage(@Valid AcademyQuery query) {
        return R.data(service.getPage(query).convert(this::convert));
    }

    @GetMapping("/select")
    @ApiOperation("下拉数据源")
    @ApiOperationSupport(order = 3)
    public R<List<Academy>> select() {
        return R.data(service.list());
    }

    @ApiOperation("新增")
    @PostMapping("/save")
    @ApiOperationSupport(order = 4)
    public R<Void> create(@RequestBody Academy academy) {
        return R.status(service.saveOrUpdate(academy));
    }

    @ApiOperation("修改")
    @PostMapping("/update")
    @ApiOperationSupport(order = 5)
    public R<Void> update(@RequestBody Academy academy) {
        return R.status(service.saveOrUpdate(academy));
    }

    @ApiOperation("删除")
    @PostMapping("/remove")
    @ApiOperationSupport(order = 6)
    public R<Void> update(@ApiParam(value = "主键集合", required = true) @RequestParam String ids) {
        return R.status(service.removeByIds(Func.toLongList(ids)));
    }

    private AcademyVO convert(Academy v) {
        AcademyVO vo = new AcademyVO();
        vo.setId(v.getId());
        vo.setName(v.getName());
        vo.setSort(v.getSort());
        vo.setNumMajors(majorService.countByAcademyId(v.getId()));
        return vo;
    }

}
