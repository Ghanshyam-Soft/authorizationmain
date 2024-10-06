package com.staples.payment.authorization.configuration;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.staples.payment.shared.cache.BusinessMasterCache;
import com.staples.payment.shared.cache.ResponseInfoCache;
import com.staples.payment.shared.constant.Bank;
import com.staples.payment.shared.repo.BusinessMasterRepo;
import com.staples.payment.shared.repo.ResponseInfoRepo;

@Configuration
public class DependencyBeanConfig
{
	@Bean
	ResponseInfoCache responseInfoCache(ResponseInfoRepo responseInfoRepo)
	{
		final List<Bank> bankIds = Arrays.asList(Bank.ACI, Bank.BRAINTREE, Bank.BAMBORA, Bank.CYB, Bank.AMXPWP);
		return new ResponseInfoCache(responseInfoRepo, bankIds);
	}

	@Bean
	BusinessMasterCache businessMasterCache(BusinessMasterRepo businessMasterRepo)
	{
		return new BusinessMasterCache(businessMasterRepo);
	}
}
