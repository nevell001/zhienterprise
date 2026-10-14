package com.zhi.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhi.system.domain.Page;
import com.zhi.system.service.IPageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 自定义页面后台控制器单元测试
 *
 * @author test
 * @date 2026-09-10
 */
@WebMvcTest(controllers = PageController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class PageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IPageService pageService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    private Page buildPage() {
        Page page = new Page();
        page.setId(1L);
        page.setTitle("关于本站");
        page.setSlug("about-site");
        page.setContent("# 关于");
        page.setStatus("1");
        return page;
    }

    @Test
    void testList() throws Exception {
        List<Page> list = new ArrayList<>();
        list.add(buildPage());
        when(pageService.selectPageList(any(Page.class))).thenReturn(list);

        mockMvc.perform(get("/system/page/list"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.rows[0].slug").value("about-site"));
    }

    @Test
    void testGetInfo() throws Exception {
        when(pageService.selectPageById(1L)).thenReturn(buildPage());

        mockMvc.perform(get("/system/page/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value("关于本站"));
    }

    @Test
    void testAdd() throws Exception {
        when(pageService.insertPage(any(Page.class))).thenReturn(1);

        mockMvc.perform(post("/system/page")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildPage())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(pageService).insertPage(any(Page.class));
    }

    @Test
    void testEdit() throws Exception {
        when(pageService.updatePage(any(Page.class))).thenReturn(1);

        mockMvc.perform(put("/system/page")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildPage())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(pageService).updatePage(any(Page.class));
    }

    @Test
    void testChangeStatus() throws Exception {
        when(pageService.changePageStatus(eq(1L), eq("1"))).thenReturn(1);

        mockMvc.perform(put("/system/page/changeStatus/1/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(pageService).changePageStatus(1L, "1");
    }

    @Test
    void testRemove() throws Exception {
        when(pageService.deletePageByIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/system/page/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(pageService).deletePageByIds(any(Long[].class));
    }
}
