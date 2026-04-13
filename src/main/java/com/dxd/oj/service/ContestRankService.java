package com.dxd.oj.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dxd.oj.model.vo.ContestRankVO;

public interface ContestRankService {

    void refreshUserRank(Long contestId, Long userId);

    IPage<ContestRankVO> pageRank(Long contestId, long current, long pageSize);

    void rebuildRankCache(Long contestId);
}
