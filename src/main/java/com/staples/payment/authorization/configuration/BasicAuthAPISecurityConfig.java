package com.staples.payment.authorization.configuration;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.val;

@Configuration
@Order(1)
@ConditionalOnProperty(value = "swaggerAuth.enable")
public class BasicAuthAPISecurityConfig extends WebSecurityConfigurerAdapter
{
	private final String swaggerUsername;
	private final String swaggerPassword;

	public BasicAuthAPISecurityConfig(@Value("${swagger.username:#{null}}") String swaggerUsername, @Value("${swagger.password:#{null}}") String swaggerPassword)
	{
		super();
		this.swaggerUsername = swaggerUsername;
		this.swaggerPassword = swaggerPassword;
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception
	{
		val admin = "admin";
		val swagger = "swagger";

		http
				.httpBasic(withDefaults())
				.csrf((csrf) -> csrf.disable())
				.requestMatchers(matchers -> matchers
						.antMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/login*"))
				.authorizeHttpRequests(requests -> requests
						.antMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").hasAnyAuthority(admin, swagger)
						.antMatchers("/login*").permitAll())
				.formLogin(formLogin -> formLogin
						.permitAll());
	}

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception
	{
		if(swaggerUsername != null && swaggerPassword != null)
		{
			auth.inMemoryAuthentication()
					.withUser(swaggerUsername).password(swaggerPassword)
					.authorities("swagger");
		}
		else
		{
			throw new RuntimeException("Username and password are required for swagger basic auth");
		}
	}

	@Bean
	PasswordEncoder createPwdEncoder()
	{
		return new BCryptPasswordEncoder(12);
	}
}
