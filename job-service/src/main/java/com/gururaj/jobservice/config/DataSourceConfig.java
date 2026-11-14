package com.gururaj.jobservice.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        String rawUrl = properties.getUrl();
        String username = properties.getUsername();
        String password = properties.getPassword();
        String jdbcUrl = rawUrl;

        if (rawUrl != null) {
            if (rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://")) {
                try {
                    String uriString = rawUrl.replaceFirst("^(postgres|postgresql)://", "http://");
                    URI uri = URI.create(uriString);

                    String host = uri.getHost();
                    int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                    String path = uri.getPath();

                    if (uri.getUserInfo() != null) {
                        String[] userInfo = uri.getUserInfo().split(":", 2);
                        username = userInfo[0];
                        if (userInfo.length > 1) {
                            password = userInfo[1];
                        }
                    }

                    jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
                    if (uri.getQuery() != null) {
                        jdbcUrl += "?" + uri.getQuery();
                    } else if (host != null && (host.contains(".render.com") || host.contains(".railway.app") || host.contains(".aivencloud.com"))) {
                        jdbcUrl += "?sslmode=require";
                    }
                } catch (Exception e) {
                    jdbcUrl = rawUrl.replaceFirst("^(postgres|postgresql)://", "jdbc:postgresql://");
                }
            }
        }

        properties.setUrl(jdbcUrl);
        if (username != null) {
            properties.setUsername(username);
        }
        if (password != null) {
            properties.setPassword(password);
        }

        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .driverClassName("org.postgresql.Driver")
                .url(jdbcUrl)
                .username(username)
                .password(password)
                .build();
    }
}
