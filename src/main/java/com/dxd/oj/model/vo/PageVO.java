package com.dxd.oj.model.vo;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "分页响应数据")
public class PageVO<T> implements Serializable {

    @Schema(description = "当前页码（从1开始）", example = "1")
    private long current;

    @Schema(description = "每页条数", example = "10")
    private long size;

    @Schema(description = "总记录数", example = "68")
    private long total;

    @Schema(description = "总页数", example = "7")
    private long pages;

    @Schema(description = "当前页数据列表")
    private List<T> records;

    public static <T> PageVO<T> of(IPage<T> page) {
        PageVO<T> vo = new PageVO<>();
        vo.setCurrent(page.getCurrent());
        vo.setSize(page.getSize());
        vo.setTotal(page.getTotal());
        vo.setPages(page.getPages());
        vo.setRecords(page.getRecords());
        return vo;
    }
}
