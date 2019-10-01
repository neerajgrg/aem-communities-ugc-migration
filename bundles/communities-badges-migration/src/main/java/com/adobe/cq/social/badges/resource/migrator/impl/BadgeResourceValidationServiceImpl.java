package com.adobe.cq.social.badges.resource.migrator.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.social.badges.resource.migrator.internal.BadgesMigrationUtils;
import com.adobe.cq.social.badges.resource.migrator.internal.BadgingConstants;
import com.adobe.cq.social.badges.resource.migrator.service.BadgeResourceValidationService;
import com.adobe.cq.social.badging.api.BadgingService;
import com.adobe.cq.social.community.api.CommunityContext;
import com.adobe.cq.social.scoring.api.ScoringService;
import com.adobe.cq.social.user.internal.UserBadgeUtils;
import com.adobe.cq.social.user.internal.UserProfileBadge;
import com.adobe.granite.security.user.UserManagementService;

@Component(immediate = true)
@Service(value = BadgeResourceValidationService.class)
public class BadgeResourceValidationServiceImpl implements BadgeResourceValidationService{

	private final Logger log = LoggerFactory.getLogger(BadgeResourceValidationService.class);

	@Reference
	private UserManagementService userManagementService;

	@Reference
	private EventAdmin eventAdmin;

	@Reference
	private BadgingService badgingService;

	@Reference
	private ScoringService scoringService;


	@Override
	public void validateNewBadges(SlingHttpServletRequest req, long delay, SlingHttpServletResponse resp) throws Exception {

		log.info("[BADGEMIGRATIONTASK] BadgeValidation Start Time : " + System.currentTimeMillis() / 1000);


		ResourceResolver resourceResolver = req.getResourceResolver();
		Resource componentResource = resourceResolver.getResource(BadgingConstants.COMPONENT_RESOURCE_PATH);
		Resource resource = req.getResource();

		try{
			for(String authId : BadgesMigrationUtils.getAllUsers(req)) {
				log.info("[BADGEHELPINGTASK] user is  : " + authId);
				validateBadges(resourceResolver, authId, resource, componentResource);
			}
		} catch (Exception e) {
			log.error("[BADGEHELPINGTASK] error in user details : " + e);
		}
		log.info("[BadgesMigrationTask] BadgeValidation End TIme : " + System.currentTimeMillis() / 1000);

	}


	private void validateBadges(ResourceResolver resourceResolver,String authId, Resource resource, Resource componentResource) throws Exception{

		UserBadgeUtils badgeUtils = new UserBadgeUtils(resourceResolver, badgingService, authId);
		CommunityContext communityContext = resource.adaptTo(CommunityContext.class);
		boolean isValid = true;
		if (communityContext != null && StringUtils.isBlank(communityContext.getSiteId())) {
			communityContext = null;
		}
		List<UserProfileBadge> allBadges = badgeUtils.getBadges(communityContext, BadgingService.ALL_BADGES);

		Set<String> userBadgesSet = new HashSet<String>();
		for (UserProfileBadge badge:allBadges){
			String badgePath = badge.getUserBadgeResource().getValueMap().get("badgeContentPath", String.class);
			if(userBadgesSet.contains(badgePath)){
				log.error("Validation failed, Duplicate Badge: userid:{} badgePath:{}", authId , badgePath);
				return;
			}else{
				userBadgesSet.add(badgePath);
			}

		}
		for (UserProfileBadge badge:allBadges) {
			Resource badgeResource = badge.getUserBadgeResource();
			ValueMap  badgeMap = badgeResource.getValueMap();
			String currentBadgeContentPath = badgeMap.get("badgeContentPath", String.class);
			//String newBadgeContentPath = BadgesMigrationUtils.getNewBadgeContentPath(currentBadgeContentPath, resourceResolver);
			if(currentBadgeContentPath.startsWith("/etc")){
				log.info("Validation failed: userid:{} badgePath:{}", authId , currentBadgeContentPath);
				isValid = false;
			}
		}

		if(isValid) {
			log.info("Validation Passed: userid:{}",authId);
		}

	}






}
