package com.kirisamemarisa.seckillsystem.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 后台分页包装。{@code total} 是总条数，{@code records} 是当前页数据。
 */
@Data
@AllArgsConstructor
public class PageResult<T> {
    private long total;
    private long page;
    private long pageSize;
    private List<T> records;
}
