package com.campus.news;

import com.campus.news.agent.tools.NewsAgentTools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * 测试自动审核功能
 */
@SpringBootApplication
public class TestAutoApproval {
    
    @Autowired
    private NewsAgentTools agentTools;
    
    public static void main(String[] args) {
        SpringApplication.run(TestAutoApproval.class, args);
    }
    
    @Bean
    public CommandLineRunner testRunner() {
        return args -> {
            System.out.println("==========================================");
            System.out.println("开始测试自动审核功能");
            System.out.println("==========================================");
            
            // 1. 查看所有待审核文章
            System.out.println("\n1. 查看待审核文章列表：");
            String pendingList = agentTools.listPendingArticles();
            System.out.println(pendingList);
            
            // 2. 测试批量审核
            System.out.println("\n2. 执行批量自动审核：");
            String batchResult = agentTools.batchAutoApprove(10);
            System.out.println(batchResult);
            
            // 3. 再次查看待审核文章
            System.out.println("\n3. 审核后再次查看待审核文章：");
            String pendingListAfter = agentTools.listPendingArticles();
            System.out.println(pendingListAfter);
            
            System.out.println("\n==========================================");
            System.out.println("测试完成！");
            System.out.println("==========================================");
        };
    }
}
