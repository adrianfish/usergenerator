/**
 * Copyright 2009 The Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.opensource.org/licenses/ecl1.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.usergenerator.tool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.sakaiproject.authz.api.Role;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.component.api.ComponentManager;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.tool.api.Placement;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;

import de.rrze.idmone.utils.jpwgen.BlankRemover;
import de.rrze.idmone.utils.jpwgen.PwGenerator;

/**
 * All Sakai API calls go in here. If Sakai changes all we have to do if mod this file.
 * 
 * @author Adrian Fish (a.fish@lancaster.ac.uk)
 */
public class SakaiProxy
{
	private Logger logger = Logger.getLogger(SakaiProxy.class);

	private ServerConfigurationService serverConfigurationService = null;

	private UserDirectoryService userDirectoryService = null;
	
	private SiteService siteService = null;

	private ToolManager toolManager;
	
	public SakaiProxy()
	{
		if (logger.isDebugEnabled())
			logger.debug("SakaiProxy()");

		ComponentManager componentManager = org.sakaiproject.component.cover.ComponentManager.getInstance();
		serverConfigurationService = (ServerConfigurationService) componentManager.get(ServerConfigurationService.class);
		userDirectoryService = (UserDirectoryService) componentManager.get(UserDirectoryService.class);
		siteService = (SiteService) componentManager.get(SiteService.class);
		toolManager = (ToolManager) componentManager.get(ToolManager.class);
	}

	public Site getCurrentSite()
	{
		try
		{
			return siteService.getSite(getCurrentSiteId());
		}
		catch (Exception e)
		{
			logger.error("Failed to get current site.", e);
			return null;
		}
	}

	public String getCurrentSiteId()
	{
		Placement placement = toolManager.getCurrentPlacement();
		if (placement == null)
		{
			logger.warn("Current tool placement is null.");
			return null;
		}

		return placement.getContext();
	}

	public String getDisplayNameForUser(String creatorId)
	{
		try
		{
			User sakaiUser = userDirectoryService.getUser(creatorId);
			return sakaiUser.getDisplayName();
		}
		catch (Exception e)
		{
			return creatorId; // this can happen if the user does not longer exist in the system
		}
	}

	public String getServerUrl()
	{
		return serverConfigurationService.getServerUrl();
	}

	public User getCurrentUser()
	{
		try
		{
			return userDirectoryService.getCurrentUser();
		}
		catch (Throwable t)
		{
			logger.error("Exception caught whilst getting current user.", t);
			if (logger.isDebugEnabled())
				logger.debug("Returning null ...");
			return null;
		}
	}

	public String getPortalUrl()
	{
		return serverConfigurationService.getServerUrl() + "/portal";
	}

	public String getCurrentPageId()
	{
		Placement placement = toolManager.getCurrentPlacement();

		if (placement instanceof ToolConfiguration)
			return ((ToolConfiguration) placement).getPageId();

		return null;
	}

	public String getCurrentToolId()
	{
		return toolManager.getCurrentPlacement().getId();
	}

	public Site getSite(String siteId)
	{
		try
		{
			return siteService.getSite(siteId);
		}
		catch(IdUnusedException e)
		{
			logger.error("No site with id of '" + siteId + "'. Returning null ...");
			return null;
		}
	}

	public List<User> getUsers(Collection<String> userIds)
	{
		return userDirectoryService.getUsers(userIds);
	}

	public boolean addUsers(List<UGUser> users,String role)
	{
		String flags = "-N " + users.size() + " --no-numerals -M 100 -s 8";
		flags = BlankRemover.itrim(flags);
		String[] ar = flags.split(" ");
		PwGenerator generator = new PwGenerator();
		generator.getDefaultBlacklistFilter().addToBlacklist("badpassword");
		List <String> passwords = generator.process(ar);
		
		Site site = getCurrentSite();
		
		int index = 0;
		
		List<User> addedSakaiUsers = new ArrayList<User>();
		
		for (Iterator iter = passwords.iterator(); iter.hasNext();)
		{
			String password = (String) iter.next();
			UGUser user = users.get(index++);
			user.setPassword(password);
			
			User sakaiUser = null;
			
			try
			{
				sakaiUser = userDirectoryService.getUserByEid(user.email);
				user.setPassword("EXISTING SAKAI USER");
			}
			catch(UserNotDefinedException unde) {}
			
			try
			{
				boolean created = false;
				
				if(sakaiUser == null)
				{
					sakaiUser = userDirectoryService.addUser(null, user.email, user.firstName, user.lastName, user.email, password, "guest", null);
					created = true;
				}
				
				site.addMember(sakaiUser.getId(), role, true, false);
				
				if(created)
				{
					addedSakaiUsers.add(sakaiUser);
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
				removeUsers(addedSakaiUsers);
				return false;
			}
		}
		
		try
		{
			siteService.save(site);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			removeUsers(addedSakaiUsers);
			return false;
		}
		
		return true;
	}
	
	private void removeUsers(List<User> users)
	{
		Site site = getCurrentSite();
		
		for(User user : users)
		{
			site.removeMember(user.getId());
			
			try
			{
				userDirectoryService.removeUser(userDirectoryService.editUser(user.getId()));
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public Set<Role> getRolesInCurrentSite()
	{
		return getCurrentSite().getRoles();
	}
}
