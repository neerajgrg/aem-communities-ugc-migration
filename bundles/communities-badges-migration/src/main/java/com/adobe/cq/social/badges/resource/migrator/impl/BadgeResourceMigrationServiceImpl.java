package com.adobe.cq.social.badges.resource.migrator.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import com.adobe.cq.social.badges.resource.migrator.service.BadgeResourceMigrationService;
import com.adobe.cq.social.badging.api.BadgingService;
import com.adobe.cq.social.community.api.CommunityContext;
import com.adobe.cq.social.scoring.api.ScoringConstants;
import com.adobe.cq.social.scoring.api.ScoringService;
import com.adobe.cq.social.user.internal.UserBadgeUtils;
import com.adobe.cq.social.user.internal.UserProfileBadge;
import com.adobe.granite.security.user.UserManagementService;

@Component(immediate = true)
@Service(value = BadgeResourceMigrationService.class)
public class BadgeResourceMigrationServiceImpl implements BadgeResourceMigrationService{

	private final Logger log = LoggerFactory.getLogger(BadgeResourceMigrationService.class);

	@Reference
	private UserManagementService userManagementService;

	@Reference
	private EventAdmin eventAdmin;

	@Reference
	private BadgingService badgingService;

	@Reference
	private ScoringService scoringService;


	@Override
	public void createNewBadges(SlingHttpServletRequest req , SlingHttpServletResponse resp) throws Exception {
		
		log.info("[BADGEMIGRATIONTASK] BadgeCreation Start Time : " + System.currentTimeMillis() / 1000);

		ResourceResolver resourceResolver = req.getResourceResolver();
		Resource componentResource = resourceResolver.getResource(BadgingConstants.COMPONENT_RESOURCE_PATH);
		final String[] badgingRules =
				componentResource.getValueMap().get(BadgingService.BADGING_RULES_PROP, String[].class);
		Resource resource = req.getResource();
		int usrCount =0;
		try{
			for(String authId : BadgesMigrationUtils.getAllUsers(req)) {
				log.info("[BADGEHELPINGTASK] user is : " + authId);
				createNewEarnedBadges(resourceResolver, authId, resource, badgingRules, componentResource);
				createNewAssignedBadges(resourceResolver, authId, resource, componentResource);
				usrCount++;
			}
		} catch (Exception e) {
			log.error("[BADGEHELPINGTASK] error in user details : " + e);
		}

		log.info("[BADGEHELPINGTASK] total Users Count : " + usrCount);
		log.info("[BadgesMigrationTask] BadgeCreation End TIme : " + System.currentTimeMillis() / 1000);

	}

	private int createNewEarnedBadges(ResourceResolver resourceResolver,String authId, Resource resource, String[] badgionRules, Resource componentResource) throws Exception{

		UserBadgeUtils badgeUtils = new UserBadgeUtils(resourceResolver, badgingService, authId);
		CommunityContext communityContext = resource.adaptTo(CommunityContext.class);
		if (communityContext != null && StringUtils.isBlank(communityContext.getSiteId())) {
			communityContext = null;
		}
		List<UserProfileBadge> earnedBadges = badgeUtils.getBadges(communityContext, BadgingService.EARNED_BADGES);
		Set<String> userBadgesSet = new HashSet<String>();
		for (UserProfileBadge badge:earnedBadges){
			userBadgesSet.add(badge.getUserBadgeResource().getValueMap().get("badgeContentPath", String.class));
		}
		Collections.sort(earnedBadges, new CustomComparator());
		int count = 0 ;
		for (UserProfileBadge badge:earnedBadges) {
			Resource badgeResource = badge.getUserBadgeResource();
			ValueMap  badgeMap = badgeResource.getValueMap();
			String currentBadgeContentPath = badgeMap.get("badgeContentPath", String.class);
			String newBadgeContentPath = BadgesMigrationUtils.getNewBadgeContentPath(currentBadgeContentPath, resourceResolver);
			if(currentBadgeContentPath.startsWith("/etc") && !userBadgesSet.contains(newBadgeContentPath)){
				final Map<String, Object> badgingProps = new HashMap<String, Object>();
				Calendar earnedDate  = badge.getUserBadgeResource().getValueMap().get(BadgingService.BADGE_EARNED_DATE_PROP, Calendar.class);
				String badgingRulePath = badgeMap.get("badgingRule", String.class);
				Resource badgingRuleResource = getBadgingRuleResource(badgingRulePath, badgionRules, resourceResolver);
				if(badgingRuleResource == null){
					log.debug("[BADGEMIGRATIONTASK] badgingRulePath is not correct, please check authId: {} , badgeRulePath:{}", authId, badgingRulePath);
				}
				
				int score = badge.getEarnedScore();
				badgingProps.put(ScoringConstants.COMPONENT_PATH_PROP, BadgingConstants.COMPONENT_RESOURCE_PATH);
				badgingProps.put(BadgingService.BADGE_EARNED_SCORE_PROP, score);
				badgingProps.put(BadgingService.BADGE_EARNED_DATE_PROP, earnedDate);
				badgingService.saveBadge(resourceResolver, authId, badgingRuleResource, componentResource, newBadgeContentPath, false,badgingProps);
				log.info("[BADGEMIGRATIONTASK] new earned badges created for authId:{} badge: {}" , authId, newBadgeContentPath);
				count++;
			}
		}
		return count;
	}

	private Resource getBadgingRuleResource(String oldBadgingRulePath, String[] currentBadgingRules, ResourceResolver resourceResolver) {
		
		String oldRulePaths[] = oldBadgingRulePath.split("/");
		String oldRule = oldRulePaths[oldRulePaths.length-1];
		
		for(String newRulePath: currentBadgingRules){
			String currentRulePaths[] = newRulePath.split("/");
			String currentBadgingRule = currentRulePaths[currentRulePaths.length-1];
			
			if(oldRule.equals(currentBadgingRule)){
				return resourceResolver.getResource(newRulePath);
			}
		}
		return null;
	}

	public class CustomComparator implements Comparator<UserProfileBadge> {
		@Override
		public int compare(UserProfileBadge u1, UserProfileBadge u2) {
			Integer u1Score = u1.getEarnedScore();
			Integer u2Score =  u2.getEarnedScore();
			return u1Score.compareTo(u2Score);
		}
	}

	private int createNewAssignedBadges(ResourceResolver resourceResolver,String authId, Resource resource, Resource componentResource) throws Exception {

		UserBadgeUtils badgeUtils = new UserBadgeUtils(resourceResolver, badgingService, authId);
		CommunityContext communityContext = resource.adaptTo(CommunityContext.class);
		if (communityContext != null && StringUtils.isBlank(communityContext.getSiteId())) {
			communityContext = null;
		}
		List<UserProfileBadge>  assignBadges = badgeUtils.getBadges(communityContext, BadgingService.ASSIGNED_BADGES);
		Set<String> userBadgesSet = new HashSet<String>();
		for (UserProfileBadge badge:assignBadges){
			userBadgesSet.add(badge.getUserBadgeResource().getValueMap().get("badgeContentPath", String.class));
		}
		int count = 0; 
		for (UserProfileBadge badge: assignBadges) {
			String currentBadgeContentPath = badge.getUserBadgeResource().getValueMap().get("badgeContentPath", String.class);
			String newBadgeContentPath = BadgesMigrationUtils.getNewBadgeContentPath(currentBadgeContentPath, resourceResolver);
			if(currentBadgeContentPath.startsWith("/etc") && !userBadgesSet.contains(newBadgeContentPath)) {
				Calendar earnedDate  = badge.getUserBadgeResource().getValueMap().get(BadgingService.BADGE_EARNED_DATE_PROP, Calendar.class);
				final Map<String, Object> badgeProps = new HashMap<String, Object>();
				badgeProps.put(BadgingService.BADGE_EARNED_DATE_PROP, earnedDate);
				badgingService.saveBadge(resourceResolver, authId, null, componentResource, newBadgeContentPath, true, badgeProps);
				log.info("[BADGEMIGRATIONTASK] new assigned badges created for authId:{} badge: {}" , authId, newBadgeContentPath);
			}
		}
		return count;
	}


	@Override
	public void deleteOldBadges(SlingHttpServletRequest req,SlingHttpServletResponse resp ) throws Exception {
		
		log.info("[BADGEMIGRATIONTASK] Deleting badges Start Time : " + System.currentTimeMillis() / 1000);

		ResourceResolver resourceResolver = req.getResourceResolver();
		Resource componentResource = resourceResolver.getResource(BadgingConstants.COMPONENT_RESOURCE_PATH);
		Resource resource = req.getResource();
		int usrCount =0;
		try{
			for(String authId : BadgesMigrationUtils.getAllUsers(req)) {
				log.info("[BADGEHELPINGTASK] user is  : " + authId);
				deleteBadges(resourceResolver, authId, resource,  componentResource, true);
				deleteBadges(resourceResolver, authId, resource, componentResource, false);
				usrCount++;
			}
		} catch (Exception e) {
			log.error("[BADGEHELPINGTASK] error in user details : " + e);
		}
		
		log.info("[BADGEHELPINGTASK] Deletion ran for total Users : " + usrCount);
		log.info("[BadgesMigrationTask] Deleting old badges End TIme : " + System.currentTimeMillis() / 1000);
	}

	private void deleteBadges(ResourceResolver resourceResolver,String authId, Resource resource, Resource componentResource, boolean isAssigned) {

		UserBadgeUtils badgeUtils = new UserBadgeUtils(resourceResolver, badgingService, authId);
		CommunityContext communityContext = resource.adaptTo(CommunityContext.class);
		List<UserProfileBadge> badges = new ArrayList<UserProfileBadge>();
		if(isAssigned){
			badges = badgeUtils.getBadges(communityContext, BadgingService.ASSIGNED_BADGES);
		}else{
			badges = badgeUtils.getBadges(communityContext, BadgingService.EARNED_BADGES);
		}
		
		for (UserProfileBadge badge:badges){
			Resource badgeResource = badge.getUserBadgeResource();
			ValueMap  badgeMap = badgeResource.getValueMap();
			Resource badgingRuleResource =  resourceResolver.getResource(badgeMap.get("badgingRule", String.class));
			String currentBadgeContentPath = badgeMap.get("badgeContentPath", String.class);
			String newBadgeContentPath = BadgesMigrationUtils.getNewBadgeContentPath(currentBadgeContentPath, resourceResolver);
			if(currentBadgeContentPath.startsWith("/etc") && badges.contains(new Badge(newBadgeContentPath))) {
				try {
					log.info("deleting badge auth:{}  badge:{} {}", authId, currentBadgeContentPath);
					badgingService.deleteBadge(resourceResolver, authId, badgingRuleResource, componentResource, currentBadgeContentPath, isAssigned);
				} catch (Exception e) {
					log.error("error in deleting badge auth:{}  badge:{} {}", authId, currentBadgeContentPath, e );
				}
			}
		}
	}
}
