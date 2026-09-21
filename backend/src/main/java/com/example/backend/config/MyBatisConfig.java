package com.example.backend.config;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;

import org.springframework.context.annotation.Bean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@org.springframework.context.annotation.Configuration
public class MyBatisConfig {

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {

        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);

        factoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver()
                        .getResources("classpath:mapper/**/*.xml")
        );

        // MyBatis Configuration
        org.apache.ibatis.session.Configuration mybatisConfig =
                new org.apache.ibatis.session.Configuration();

        // 쿼리마다 SQL/파라미터/결과 행을 전부 System.out으로 직접 찍는
        // StdOutImpl이 켜져 있었다. 이게 콘솔 I/O 자체를 막아서(특히 결과 행이
        // 많은 조회일수록) 응답 속도를 크게 떨어뜨리고 있었다(관광지 검색: SQL
        // 실행만 약 400ms 소요 — 로그 없이 DB에 직접 같은 쿼리를 날리면 0.1초
        // 이내). 로그 구현체를 지정하지 않으면 MyBatis가 SLF4J(Logback)를 자동
        // 감지해서 쓰는데, 기본 로그 레벨(INFO)에서는 이 SQL 디버그 로그 자체가
        // 찍히지 않아 그 오버헤드가 사라진다.
        mybatisConfig.setMapUnderscoreToCamelCase(true);

        factoryBean.setConfiguration(mybatisConfig);

        return factoryBean.getObject();
    }

    @Bean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}