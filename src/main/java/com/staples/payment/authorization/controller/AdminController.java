package com.staples.payment.authorization.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.staples.payment.shared.cache.BusinessMasterCache;
import com.staples.payment.shared.request.AdminRequest;
import com.staples.payment.shared.request.CacheRefresh;

@RestController
@RequestMapping(value = "/admin", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
public class AdminController
{
	private BusinessMasterCache businessMasterCache;

	public AdminController(BusinessMasterCache businessMasterCache)
	{
		this.businessMasterCache = businessMasterCache;
	}

	@PostMapping({"/gpas/refreshcache"})
	public ResponseEntity<String> refreshCache(@RequestBody AdminRequest adminRequest)
	{
		StringBuffer status = new StringBuffer();
		CacheRefresh cacheRefresh = adminRequest.getCacheRefresh();
		if(cacheRefresh != null)
		{
			if(cacheRefresh.isRefreshMerchantCache())
			{
				businessMasterCache.populateCache();
				status.append("businessMasterCache refresh : success.");
			}
		}
		else
		{
			status.append("invalid input. No cache refreshed.");
		}

		ResponseEntity<String> adminResp = new ResponseEntity<>(status.toString(), HttpStatus.OK);

		return adminResp;
	}
}
