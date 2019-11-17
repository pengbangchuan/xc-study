package com.xuecheng.manage_cms.service;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.CmsTemplate;
import com.xuecheng.framework.domain.cms.request.QueryPageRequest;
import com.xuecheng.framework.domain.cms.response.CmsCode;
import com.xuecheng.framework.domain.cms.response.CmsPageResult;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import com.xuecheng.manage_cms.dao.CmsPageRepository;
import com.xuecheng.manage_cms.dao.CmsTemplateRepository;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Service
public class PageService {

    @Autowired
    private CmsPageRepository cmsPageRepository;

    @Autowired
    private CmsTemplateRepository cmsTemplateRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private GridFSBucket gridFSBucket;

    @Autowired
    private GridFsTemplate gridFsTemplate;

    /**
     *
     * @param page 当前页码
     * @param size 页面显示的个数
     * @param queryPageRequest 查询条件
     * @return 页面列表
     */
    public QueryResponseResult findList(int page,int size, QueryPageRequest queryPageRequest) {
        if(queryPageRequest == null){
            queryPageRequest = new QueryPageRequest();
        }

        if (page <= 0){
            page = 1;
        }
        page = page - 1; // mongdb分页是从0开始的
        if (size <= 0){
            size = 10;
        }

        //创建条件查询对象
        CmsPage cmsPage = new CmsPage();
        //设置查询条件
        if(StringUtils.isNotEmpty(queryPageRequest.getSiteId())){
            cmsPage.setSiteId(queryPageRequest.getSiteId());//站点id
        }
        if(StringUtils.isNotEmpty(queryPageRequest.getPageAliase())){
            cmsPage.setPageAliase(queryPageRequest.getPageAliase());//别名
        }

        //自定义条件查询
        //定义条件匹配器
        ExampleMatcher exampleMatcher = ExampleMatcher.matching();
        //页面别名模糊查询，需要自定义字符串的匹配器实现模糊查询         
        // ExampleMatcher.GenericPropertyMatchers.contains() 包含
        // ExampleMatcher.GenericPropertyMatchers.startsWith()//开头匹配
        exampleMatcher = exampleMatcher.withMatcher("pageAliase", ExampleMatcher.GenericPropertyMatchers.contains());
        Example<CmsPage> example = Example.of(cmsPage, exampleMatcher);
        Pageable pageable = PageRequest.of(page, size);
        Page<CmsPage> all = cmsPageRepository.findAll(example,pageable);
        QueryResult queryResult  = new QueryResult();
        queryResult.setList(all.getContent());
        queryResult.setTotal(all.getTotalElements());
        QueryResponseResult queryResponseResult = new QueryResponseResult(CommonCode.SUCCESS,queryResult);
        return queryResponseResult;
    }

    /**
     * 添加页面
     * @param cmsPage
     * @return
     */
    public CmsPageResult add(CmsPage cmsPage){
        // 非法参数抛异常
        if(cmsPage==null){
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        cmsPage.setPageId(null);
        //先查询判断页面是否存在
        CmsPage page = cmsPageRepository.findByPageNameAndSiteIdAndPageWebPath(cmsPage.getPageName(), cmsPage.getSiteId(), cmsPage.getPageWebPath());
        if(page != null){
            ExceptionCast.cast(CmsCode.CMS_ADDPAGE_EXISTSNAME);
        }
        // 设置页面id为null mongodb自动生成
        CmsPage cmsPage1 = cmsPageRepository.save(cmsPage);
        return new CmsPageResult(CommonCode.SUCCESS,cmsPage1);
    }

    /**
     *
     * @param pageId
     * @return
     */
    public String getPageHtml(String pageId){
        // 获取页面模板数据
        Map model = this.getModelByPageId(pageId);
        if (model == null){
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_DATAISNULL);
        }
        // 获取页面模板
        String templateContent = this.getTemplateByPageId(pageId);
        if (StringUtils.isEmpty(templateContent)){
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_TEMPLATEISNULL);
        }
        // 生成模板返回
        String html = this.generateHtml(templateContent, model);
        if (StringUtils.isEmpty(html)){
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_HTMLISNULL);
        }
        return html;
    }


    //------------------------------------------------------------------//


    /**
     *
     * @param pageId
     * @return
     */
    private Map getModelByPageId(String pageId){
        // 通过pageId查询页面信息
        Optional<CmsPage> optional = cmsPageRepository.findById(pageId);
        if(!optional.isPresent()){
            ExceptionCast.cast(CmsCode.CMS_PAGE_NOTSAVE);
        }
        CmsPage cmsPage = optional.get();
        String dataUrl = cmsPage.getDataUrl();
        if (StringUtils.isEmpty(dataUrl)){
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_DATAURLISNULL);
        }
        // 通过datUrl远程调用获取模型信息
        ResponseEntity<Map> entity = restTemplate.getForEntity(dataUrl, Map.class);
        Map body = entity.getBody();
        return body;
    }

    /**
     *
     * @param pageId
     * @return
     */
    private String getTemplateByPageId(String pageId){
        //页面id查询页面信息
        Optional<CmsPage> optional = cmsPageRepository.findById(pageId);
        if(!optional.isPresent()){
                ExceptionCast.cast(CmsCode.CMS_PAGE_NOTSAVE);
        }
        CmsPage cmsPage = optional.get();
        String templateId = cmsPage.getTemplateId();
        if (StringUtils.isEmpty(templateId)){
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_TEMPLATEISNULL);
        }
        //查询页面模板信息
        Optional<CmsTemplate> optional1 = cmsTemplateRepository.findById(templateId);
        CmsTemplate cmsTemplate = optional1.get();

        // 从gridfs文件系统中下载模板
        String templateFileId = cmsTemplate.getTemplateFileId();
        //根据文件id查询文件
        GridFSFile gridFSFile = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(templateFileId)));
        //打开下载流对象
        GridFSDownloadStream gridFSDownloadStream = gridFSBucket.openDownloadStream(gridFSFile.getObjectId());
        //创建GridFsResource对象，获取流
        GridFsResource gridFsResource = new GridFsResource(gridFSFile,gridFSDownloadStream);
        //从流中取数据
        try {
            String content = IOUtils.toString(gridFsResource.getInputStream(), "utf-8");
            return content;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String generateHtml(String templateContent,Map model){
        //创建配置类
        Configuration configuration = new Configuration(Configuration.getVersion());
        //加载模板
        StringTemplateLoader stringTemplateLoader = new StringTemplateLoader();
        stringTemplateLoader.putTemplate("template",templateContent);
        //配置模板加载器
        configuration.setTemplateLoader(stringTemplateLoader);
        try {
            //获取模板
            Template template = configuration.getTemplate("template","utf-8");
            String content = FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
            return content;
        } catch (IOException | TemplateException e) {
            e.printStackTrace();
        }
        return null;
    }

}
