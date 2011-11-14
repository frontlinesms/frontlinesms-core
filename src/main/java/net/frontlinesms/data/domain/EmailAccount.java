/*
 * FrontlineSMS <http://www.frontlinesms.com>
 * Copyright 2007, 2008 kiwanja
 * 
 * This file is part of FrontlineSMS.
 * 
 * FrontlineSMS is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 * 
 * FrontlineSMS is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with FrontlineSMS. If not, see <http://www.gnu.org/licenses/>.
 */
package net.frontlinesms.data.domain;

import javax.persistence.*;

/**
 * Data object representing an e-mail account.  An e-mail account can be uniquely identified by his account name.
 * @author Alex Anderson
 */
@Entity
public class EmailAccount {
//> CONSTANTS
	public static final int DEFAULT_POP_PORT = 110;
	public static final int DEFAULT_POPSSL_PORT = 992;
	public static final int DEFAULT_SMTP_PORT = 25;
	public static final int DEFAULT_SMTPS_PORT = 465;
	
	public static final String FIELD_IS_FOR_RECEIVING = "isForReceiving";

//> PROPERTIES
	/** Unique id for this entity.  This is for hibernate usage. */
	@Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(unique=true,nullable=false,updatable=false)
	private long id;
	/** Username. It should be unique within the system, but may be changed. */
	@Column(unique=true, nullable=true, updatable=true) // XXX: Should now be possible to have a username twice (Sending + Receiving). Does this require a database reload?
	private String accountName;
	private String accountServer;
	private String accountPassword;
	private Integer accountServerPort;
	private boolean useSsl;
	private Long lastCheck;
	@Column(name=FIELD_IS_FOR_RECEIVING)
	private Boolean isForReceiving = true;
	private String protocol;
	private Boolean enabled = true;
	
//> CONSTRUCTORS
	/** Empty constructor required for hibernate */
	EmailAccount() {}
	
	/**
	 * Creates an email account with the specified attributes.
	 * @param accountName The e-mail address of this account.
	 * @param accountServer The e-mail server.
	 * @param accountServerPort The e-mail server port.
	 * @param accountPassword The password to connect to this account.
	 * @param useSsl 
	 */
	public EmailAccount(String accountName, String accountServer, int accountServerPort, String accountPassword, boolean useSsl, boolean isForReceiving, String protocol) {
		this.accountName = accountName;
		this.accountServer = accountServer;
		this.accountServerPort = accountServerPort;
		this.accountPassword = accountPassword;
		this.useSsl = useSsl;
		this.isForReceiving = isForReceiving;
		this.protocol = protocol;
	}
	
//> ACCESSOR METHODS
	/**
	 * Returns this account's name.
	 * @return {@link #accountName}
	 */
	public String getAccountName() {
		return this.accountName;
	}
	
	/**
	 * Gets this account's server as a String.
	 * @return {@link #accountServer}
	 */
	public String getAccountServer() {
		return this.accountServer;
	}
	
	/**
	 * Gets this account's password as a String.
	 * @return {@link #accountPassword}
	 */
	public String getAccountPassword() {
		return this.accountPassword;
	}
	
	/**
	 * Gets this account's server port.
	 * @return {@link #accountServerPort}
	 */
	public int getAccountServerPort() {
		return this.accountServerPort;
	}

	/**
	 * Return this account's SSL value. 
	 * @return {@link #useSsl}
	 */
	public boolean useSsl() {
		return this.useSsl;
	}
	
	/**
	 * @return <code>true</code> if this account is used for receiving e-mails, <code>false</code> otherwise.
	 */
	public boolean isForReceiving() {
		return (this.isForReceiving == null ? false : this.isForReceiving.booleanValue());
	}
	
	/**
	 * Sets this account's name.
	 * @param name new value for {@link #accountName}
	 */
	public void setAccountName(String name) {
		this.accountName = name;
	}
	
	/**
	 * Sets this account's server as a String.
	 * @param server new value for {@link #accountServer}
	 */
	public void setAccountServer(String server) {
		this.accountServer = server;
	}
	
	/**
	 * Sets this account's password as a String.
	 * @param password new value for {@link #accountPassword} 
	 */
	public void setAccountPassword(String password) {
		this.accountPassword = password;
	}

	/**
	 * Sets this account's server port.
	 * @param port new value for {@link #accountServerPort}
	 */
	public void setAccountServerPort(int port) {
		this.accountServerPort = port;
	}
	
	/**
	 * Sets this account's SSL value. 
	 * @param ssl new value for {@link #useSsl}
	 */
	public void setUseSSL(boolean ssl) {
		this.useSsl = ssl;
	}

//> GENERATED CODE
	/** @see java.lang.Object#hashCode() */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((accountName == null) ? 0 : accountName.hashCode());
		result = prime * result
				+ ((accountPassword == null) ? 0 : accountPassword.hashCode());
		result = prime * result
				+ ((accountServer == null) ? 0 : accountServer.hashCode());
		result = prime
				* result
				+ ((accountServerPort == null) ? 0 : accountServerPort
						.hashCode());
		result = prime * result + (useSsl ? 1231 : 1237);
		return result;
	}

	/** @see java.lang.Object#equals(java.lang.Object) */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EmailAccount other = (EmailAccount) obj;
		if (accountName == null) {
			if (other.accountName != null)
				return false;
		} else if (!accountName.equals(other.accountName))
			return false;
		if (accountPassword == null) {
			if (other.accountPassword != null)
				return false;
		} else if (!accountPassword.equals(other.accountPassword))
			return false;
		if (accountServer == null) {
			if (other.accountServer != null)
				return false;
		} else if (!accountServer.equals(other.accountServer))
			return false;
		if (accountServerPort == null) {
			if (other.accountServerPort != null)
				return false;
		} else if (!accountServerPort.equals(other.accountServerPort))
			return false;
		if (useSsl != other.useSsl)
			return false;
		return true;
	}
	
	/** 
	 * 
	 * @param emailAccount
	 * @return <code>true</code> if this object is the same database entity than the one given in parameter, <code>false</code> otherwise. 
	 */
	public boolean isSameDatabaseEntity(EmailAccount emailAccount) {
		return (this.id == emailAccount.id);
	}

	public void setLastCheck(Long lastCheck) {
		this.lastCheck = lastCheck;
	}

	public Long getLastCheck() {
		return lastCheck;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public Boolean isEnabled() {
		return enabled;
	}

}
