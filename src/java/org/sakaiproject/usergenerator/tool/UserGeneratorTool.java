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

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.sakaiproject.user.api.User;

/**
 * This servlet handles all of the REST type stuff. At some point this may all
 * move into an EntityProvider.
 * 
 * @author Adrian Fish (a.fish@lancaster.ac.uk)
 */
public class UserGeneratorTool extends HttpServlet
{
	private Logger logger = Logger.getLogger(UserGeneratorTool.class);

	private SakaiProxy sakaiProxy;

	public void destroy()
	{
		logger.info("destroy");

		super.destroy();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		if (logger.isDebugEnabled()) logger.debug("doGet()");
		
		if(sakaiProxy == null)
			throw new ServletException("yaftForumService and sakaiProxy MUST be initialised.");
		
		User user = sakaiProxy.getCurrentUser();
		
		if(user == null)
		{
			// We are not logged in
			throw new ServletException("getCurrentUser returned null.");
		}
		
		request.getSession().setAttribute("toolId", sakaiProxy.getCurrentToolId());

		response.setContentType("text/html");
		RequestDispatcher dispatcher = this.getServletContext().getRequestDispatcher("/usergenerator.jsp");
		dispatcher.include(request, response);
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String action = request.getParameter("action");
		
		if("create".equals(action))
		{
			if(!handleCreate(request,response))
			{
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"Failed to create the users.");
				return;
			}
			
			response.setContentType("text/html");
			RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/report.jsp");
			dispatcher.include(request, response);
			return;
		}
		else if("downloadReport".equals(action))
		{
			List<UGUser> users = (List<UGUser>) request.getSession().getAttribute("users");
			
			HSSFWorkbook wb = new HSSFWorkbook();
			HSSFSheet sheet = wb.createSheet();
			
			HSSFRow headerRow = sheet.createRow(0);
			headerRow.createCell(0).setCellValue("Login");
			headerRow.createCell(1).setCellValue("First Name");
			headerRow.createCell(2).setCellValue("Last Name");
			headerRow.createCell(3).setCellValue("Password");
			
			int rowNum = 1;
			for(UGUser user : users)
			{
				HSSFRow row = sheet.createRow(rowNum++);
				row.createCell(0).setCellValue(user.email);
				row.createCell(1).setCellValue(user.firstName);
				row.createCell(2).setCellValue(user.lastName);
				row.createCell(3).setCellValue(user.password);
			}
			
			String fileName = (String) request.getSession().getAttribute("filename");
			fileName = fileName.replaceAll(" ", "_");
			int dotIndex = fileName.lastIndexOf(".");
			fileName = fileName.substring(0,dotIndex) + "_report.xls";
			
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType("application/ms-excel");
			response.setHeader("Content-Disposition", "filename=" + fileName);
			BufferedOutputStream bos = new BufferedOutputStream(response.getOutputStream());
			wb.write(bos);
			bos.close();
			return;
		}
		
		FileItem fi = (FileItem) request.getAttribute("file");
		
		if(fi == null)
		{
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Where's the file?");
			return;
		}
		
		byte[] data = fi.get();
		
		request.getSession().setAttribute("filename", fi.getName());
		
		List<UGUser> users = new ArrayList<UGUser>();
		
		try
		{
			if(fi.getName().endsWith(".xls")) {
				POIFSFileSystem fs = new POIFSFileSystem(new ByteArrayInputStream(data));
				HSSFWorkbook wb = new HSSFWorkbook(fs);
				HSSFSheet sheet = wb.getSheetAt(0);
			
				for(Iterator rows = sheet.iterator();rows.hasNext();)
				{
					HSSFRow row = (HSSFRow) rows.next();
				
					if(row.getPhysicalNumberOfCells() < 3) continue;
				
					HSSFCell firstNameCell = row.getCell(0);
					if(HSSFCell.CELL_TYPE_STRING != firstNameCell.getCellType()) continue;
					String firstName = firstNameCell.getStringCellValue().trim();
				
					HSSFCell lastNameCell = row.getCell(1);
					if(HSSFCell.CELL_TYPE_STRING != lastNameCell.getCellType()) continue;
					String lastName = lastNameCell.getStringCellValue().trim();
				
					HSSFCell emailCell = row.getCell(2);
					if(HSSFCell.CELL_TYPE_STRING != emailCell.getCellType()) continue;
					String email = emailCell.getStringCellValue().trim().toLowerCase();
				
					users.add(new UGUser(firstName,lastName,email));
				}
			} else if(fi.getName().endsWith(".xlsx")) {
				XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(data));
				XSSFSheet sheet = wb.getSheetAt(0);
			
				for(Iterator rows = sheet.iterator();rows.hasNext();)
				{
					XSSFRow row = (XSSFRow) rows.next();
				
					if(row.getPhysicalNumberOfCells() < 3) continue;
				
					XSSFCell firstNameCell = row.getCell(0);
					if(XSSFCell.CELL_TYPE_STRING != firstNameCell.getCellType()) continue;
					String firstName = firstNameCell.getStringCellValue().trim();
				
					XSSFCell lastNameCell = row.getCell(1);
					if(XSSFCell.CELL_TYPE_STRING != lastNameCell.getCellType()) continue;
					String lastName = lastNameCell.getStringCellValue().trim();
				
					XSSFCell emailCell = row.getCell(2);
					if(XSSFCell.CELL_TYPE_STRING != emailCell.getCellType()) continue;
					String email = emailCell.getStringCellValue().trim().toLowerCase();
				
					users.add(new UGUser(firstName,lastName,email));
				}
			}
		}
		catch(IOException ioe)
		{
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "I need an Excel file.");
			return;
		}
			
		request.getSession().setAttribute("users", users);
		
		request.setAttribute("roles", sakaiProxy.getRolesInCurrentSite());
			
		response.setContentType("text/html");
		RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/userconfirmation.jsp");
		dispatcher.include(request, response);
	}
	
	private boolean handleCreate(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		List<UGUser> users = (List<UGUser>) request.getSession().getAttribute("users");
		
		String role = request.getParameter("role");
		
		if(role == null || role.length() == 0) role = "access";
		
		return sakaiProxy.addUsers(users,role);
	}

	/**
	 * Sets up the SakaiProxy instance
	 */
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);

		if (logger.isDebugEnabled()) logger.debug("init");

		try
		{
			sakaiProxy = new SakaiProxy();
		}
		catch (Throwable t)
		{
			throw new ServletException("Failed to initialise UserGeneratorTool servlet.", t);
		}
	}
}
