package com.dxd.oj.service;

import com.dxd.oj.model.vo.HomeOverviewVO;

public interface HomeService {

    HomeOverviewVO getHomeOverview(String token);
}
