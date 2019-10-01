package com.adobe.cq.social.badges.resource.migrator.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.settings.SlingSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BadgesMigrationUtils {

	private static final Logger log = LoggerFactory.getLogger(BadgesMigrationUtils.class);



	public static String getNewBadgeContentPath(String currentBadgeContentPath, ResourceResolver resourceResolver) {

		if(currentBadgeContentPath.startsWith("/content") || currentBadgeContentPath.startsWith("/libs") )
			return currentBadgeContentPath;

		String contentImagePath = currentBadgeContentPath.replaceFirst("/etc", BadgingConstants.CONTENT_IMAGES_PATH);
		Resource contentResource = resourceResolver.getResource(contentImagePath);
		if(contentResource != null)
			return contentResource.getPath();

		String libsImagePath = currentBadgeContentPath.replaceFirst("/etc", BadgingConstants.LIBS_IMAGES_PATH);
		Resource libsResource = resourceResolver.getResource(libsImagePath);
		if(libsResource != null)
			return libsResource.getPath();

		return null;
	}
	
	public static boolean isPublishMode(SlingSettingsService slingSettingsService){
		return slingSettingsService != null && slingSettingsService.getRunModes().contains("publish");
	}

	public static List<String>  getAllUsers(SlingHttpServletRequest request) throws Exception {
		List<String> finalUsers= new ArrayList<String>();
		HashSet<String> users = new HashSet<String>();
		users.add("pageexporterservice");
		users.add("projects-service");
		users.add("suggestionservice");
		users.add("media-service");
		users.add("authentication-service");
		users.add("snapshotservice");
		users.add("ocs-lifecycle");
		users.add("tagmanagerservice");
		users.add("searchpromote-service");
		users.add("device-identification-service");
		users.add("commerce-orders-service");
		users.add("commerce-frontend-service");
		users.add("commerce-backend-service");
		users.add("recs-deleted-products-listener-service");
		users.add("dam-sync-service");
		users.add("audiencemanager-syncsegments-service");
		users.add("audiencemanager-configlistener-service");
		users.add("campaign-reader");
		users.add("targetservice");
		users.add("webservice-support-servicelibfinder");
		users.add("webservice-support-replication");
		users.add("webservice-support-statistics");
		users.add("oauthservice");
		users.add("spellchecker-service");
		users.add("dam-teammgr-service");
		users.add("activitypurgesrv");
		users.add("idsjobprocessor");
		users.add("dynamic-media-replication");
		users.add("resourcecollectionservice");
		users.add("msm-service");
		users.add("dtmservice");
		users.add("communities-ugc-writer");
		users.add("communities-user-admin");
		users.add("communities-workflow-launcher");
		users.add("communities-utility-reader");
		users.add("version-purge-service");
		users.add("fd-service");
		users.add("analyticsservice");
		users.add("statistics-service");
		users.add("anonymous");
		users.add("replication-receiver");
		users.add("campaign-remote");
		users.add("author");
		users.add("admin");
		Session session = request.getResourceResolver().adaptTo(Session.class);

		String q = "/jcr:root/home/users//element(*, rep:User)";

		Query query = session.getWorkspace().getQueryManager().createQuery(q, Query.XPATH);
		for (NodeIterator i = query.execute().getNodes(); i.hasNext();) {
			try {
				Node node = i.nextNode();
				String usrPath = node.getPath();
				if(!users.contains(node.getProperty("rep:principalName").getString())
						&& !usrPath.startsWith("/home/users/system") 
						&& !usrPath.startsWith("/home/users/geometrixx")
						&& !usrPath.startsWith("/home/users/mac/")
						&& !usrPath.startsWith("/home/users/media/")
						&& !usrPath.startsWith("/home/users/we-retail/")){
					String authId = node.getProperty("rep:principalName").getString();
					finalUsers.add(authId);
				}
			} catch (Exception e) {
				log.error("[BADGEHELPINGTASK] error in user details : " + e);
			}
		}
		return finalUsers;

	}

}
