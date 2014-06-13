package org.sakaiproject.usergenerator.tool;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

public class UGUser
{
	public String firstName = "";
	public String lastName = "";
	public String email = "";
	public boolean bad = true;
	public String password = "";
	
	public UGUser(String firstName,String lastName,String email)
	{
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		
		try
		{
			InternetAddress emailAddr = new InternetAddress(email);
			
			String[] tokens = email.split("@");
			
			if(tokens.length == 2)
				this.bad = false;
		}
		catch (AddressException ex) {}
	}

	public void setPassword(String password)
	{
		this.password = password;
	}
}
