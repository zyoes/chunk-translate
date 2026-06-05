package com.example.chunktranslate;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.example")
@MapperScan("com.example.chunktranslate.mapper")
public class ChunkTranslateApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChunkTranslateApplication.class, args);
	}

}
