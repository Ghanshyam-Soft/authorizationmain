package com.staples.payment.authorization.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import com.azure.spring.cloud.autoconfigure.aad.AadResourceServerWebSecurityConfigurerAdapter;

import lombok.val;

@EnableWebSecurity
public class APISecurityConfig extends AadResourceServerWebSecurityConfigurerAdapter
{
	private final boolean swaggerAuthEnabled;
	private final boolean swaggerEnabled;

	public APISecurityConfig(@Value("${swaggerAuth.enable:false}") boolean swaggerAuthEnabled, @Value("${swagger.enable:false}") boolean swaggerEnabled)
	{
		super();
		this.swaggerAuthEnabled = swaggerAuthEnabled;
		this.swaggerEnabled = swaggerEnabled;
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception
	{
		val auth = "APPROLE_AuthUser";
		val token = "APPROLE_TokenUser";
		val admin = "APPROLE_PaymentsAdminPCI";
		val amex = "APPROLE_AmexPWPUser";

		super.configure(http);

		http
				.csrf((csrf) -> csrf.disable())
				.authorizeHttpRequests(requests ->
				{
					requests
							.antMatchers("/gpas/refreshcache").hasAuthority(admin)
							.antMatchers("/gpas/auth/v01").hasAuthority(auth)
							.antMatchers("/gpas/token/v01").hasAuthority(token)
							.antMatchers("/gpas/rewards/getbalance").hasAnyAuthority(amex)
							.antMatchers("/admin/health").permitAll();

					if(swaggerEnabled && !swaggerAuthEnabled)
					{
						requests
								.antMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
					}

					requests.anyRequest().hasAuthority(admin);
				})
				.sessionManagement(management -> management
						.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
	}
}