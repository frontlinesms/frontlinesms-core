/**
 * 
 */
package net.frontlinesms.data.repository.hibernate;


import net.frontlinesms.data.DuplicateKeyException;
import net.frontlinesms.data.domain.EmailAccount;
import net.frontlinesms.data.repository.EmailAccountDao;
import net.frontlinesms.email.EmailUtils;
import net.frontlinesms.junit.HibernateTestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * Test class for {@link HibernateEmailAccountDao}
 * @author Alex
 */
public class HibernateEmailAccountDaoTest extends HibernateTestCase {
//> PROPERTIES
	/** Logging object */
	private final Log log = LogFactory.getLog(getClass());
	/** Instance of this DAO implementation we are testing. */
	private EmailAccountDao emailAccountDao;
	
//> TEST METHODS
	/**
	 * Test basic functionality of the DAO.
	 */
	public void testSaveRetrieve() throws DuplicateKeyException {
		assertEquals("Checking there are no unexpected entries in the email DAO.", 0, emailAccountDao.getAllEmailAccounts().size());

		boolean useSsl = false;
		String accountName = "test@frontlinesms.com";
		String accountServer = "FrontlineSMS Test";
		int accountServerPort = 123;
		String accountPassword = "secretpassword";
		EmailAccount account = new EmailAccount(accountName, accountServer, accountServerPort, accountPassword, useSsl, false, EmailUtils.SMTP);
		emailAccountDao.saveEmailAccount(account);
		assertEquals(1, emailAccountDao.getAllEmailAccounts().size());
		
		EmailAccount retrievedAccount = emailAccountDao.getAllEmailAccounts().toArray(new EmailAccount[0])[0];
		assertEquals(accountName, retrievedAccount.getAccountName());
		assertEquals(accountServer, retrievedAccount.getAccountServer());
		assertEquals(accountServerPort, retrievedAccount.getAccountServerPort());
		assertEquals(accountPassword, retrievedAccount.getAccountPassword());
		assertEquals(useSsl, retrievedAccount.useSsl());
		
		
		assertEquals(1, emailAccountDao.getAllEmailAccounts().size());

		emailAccountDao.deleteEmailAccount(account);
		
		assertEquals(0, emailAccountDao.getAllEmailAccounts().size());
	}
	
	/**
	 * Test handling of duplicate accounts being saved.
	 * @throws DuplicateKeyException
	 */
	public void testDuplicates() throws DuplicateKeyException {
		boolean useSsl = false;
		String accountName = "test@frontlinesms.com";
		String accountServer = "FrontlineSMS Test";
		int accountServerPort = 123;
		String accountPassword = "secretpassword";
		EmailAccount account = new EmailAccount(accountName, accountServer, accountServerPort, accountPassword, useSsl, false, EmailUtils.SMTP);
		emailAccountDao.saveEmailAccount(account);
		EmailAccount duplicateAccount = new EmailAccount(accountName, accountServer, accountServerPort, accountPassword, useSsl, false, EmailUtils.SMTP);
		try {
			emailAccountDao.saveEmailAccount(duplicateAccount);
			fail("Should have thrown DKE");
		} catch(DuplicateKeyException ex) { /* expected */ }		
	}
//> TEST SETUP/TEARDOWN
	
//> ACCESSORS
	/** @param d The DAO to use for the test. */
	@Required
	public void setEmailAccountDao(EmailAccountDao d)
	{
		// we can just set the DAO once in the test
		this.emailAccountDao = d;
	}
}
