package com.xuecheng.manage_cms.dao;

import com.xuecheng.framework.domain.cms.CmsPage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Optional;

@SpringBootTest
@RunWith(SpringRunner.class)
public class CmsPageRepositoryTest {

    @Autowired
    private CmsPageRepository cmsPageRepository;

    @Test
    public void cmsFindAll(){
        List<CmsPage> list = cmsPageRepository.findAll();
        System.out.println(list);
    }

    @Test
    public void cmsFindAllPage(){

        //从0开始分页
        int page = 0;
        // 分页大小
        int size = 10;
        Pageable Pageable = PageRequest.of(page,size);
        Page<CmsPage> all =cmsPageRepository.findAll(Pageable);
        System.out.println("----------");
        System.out.println(all);
    }

    public void testUpdate(){
        //查询对象
        Optional<CmsPage> optional = cmsPageRepository.findById("adsf");
        if(optional.isPresent()){
            CmsPage cmsPage = optional.get();
        }

    }

}
