package com.staples.payment.authorization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.retry.annotation.EnableRetry;

import com.staples.payment.shared.repo.base.InsertUpdateRepositoryImpl;

@SpringBootApplication
@EnableRetry
@ComponentScan(basePackages = {"com.staples.payment.shared", "com.staples.payment.authorization"})
@EntityScan(basePackages = {"com.staples.payment.shared", "com.staples.payment.authorization"})
@EnableJpaRepositories(basePackages = {"com.staples.payment.shared", "com.staples.payment.authorization"}, repositoryBaseClass = InsertUpdateRepositoryImpl.class)
@EnableFeignClients(basePackages = {"com.staples.payment.shared", "com.staples.payment.authorization"})
@ConfigurationPropertiesScan
public class PaymentAuthorizationApplication
{
	public static void main(String[] args)
	{
		SpringApplication app = new SpringApplication(PaymentAuthorizationApplication.class);

		app.addListeners(new ApplicationPidFileWriter());

		app.run(args);
	}
}
