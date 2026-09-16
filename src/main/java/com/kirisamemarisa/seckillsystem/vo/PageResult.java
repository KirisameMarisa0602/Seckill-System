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
    /** 符合条件的总条数，不是当前页条数。 */
    private long total;
    /** 当前页码，从 1 起。 */
    private long page;
    /** 每页条数。 */
    private long pageSize;
    /** 当前页数据。 */
    private List<T> records;
}
