/**
 * 
 */
package net.frontlinesms.data.repository.hibernate;

import net.frontlinesms.data.DuplicateKeyException;
import net.frontlinesms.data.Order;
import net.frontlinesms.data.domain.Email;
import net.frontlinesms.data.domain.EmailAccount;
import net.frontlinesms.data.repository.EmailAccountDao;
import net.frontlinesms.data.repository.EmailDao;
import net.frontlinesms.email.EmailUtils;
import net.frontlinesms.junit.HibernateTestCase;

import org.springframework.beans.factory.annotation.Required;

/**
 * Test class for {@link HibernateEmailDao}
 * @author Alex
 */
public class HibernateEmailDaoTest extends HibernateTestCase {
//> PROPERTIES
	/** Instance of this DAO implementation we are testing. */
	private EmailDao emailDao;
	/** Dao for email accounts */
	private EmailAccountDao emailAccountDao;

//> TEST METHODS
	public void test() throws DuplicateKeyException {
		EmailAccount emailAccount = new EmailAccount("test@frontlinesms.net", "frontlinesms.net", 123, "secretpassword", false, false, EmailUtils.SMTP);
		emailAccountDao.saveEmailAccount(emailAccount);
		
		assertEquals(0, emailDao.getAllEmails().size());
		assertEquals(emailDao.getAllEmails().size(), emailDao.getEmailCount());
		assertEquals(emailDao.getAllEmails(), emailDao.getEmailsWithLimitWithoutSorting(0, Integer.MAX_VALUE));
		assertEquals(emailDao.getAllEmails(), emailDao.getEmailsWithLimit(Email.Field.EMAIL_CONTENT, Order.ASCENDING, 0, Integer.MAX_VALUE));
		
		Email email = new Email(emailAccount, "all@myfriends.com", "About the weekend", "Hey guys,\nJust a quick note about the weekend.  I can't wait.\rLove from Mr. Test");
		emailDao.saveEmail(email);

		assertEquals(1, emailDao.getAllEmails().size());
		assertEquals(emailDao.getAllEmails().size(), emailDao.getEmailCount());
		assertEquals(emailDao.getAllEmails(), emailDao.getEmailsWithLimitWithoutSorting(0, Integer.MAX_VALUE));
		assertEquals(emailDao.getAllEmails(), emailDao.getEmailsWithLimit(Email.Field.FROM, Order.ASCENDING, 0, Integer.MAX_VALUE));
		
		emailDao.deleteEmail(email);

		assertEquals(0, emailDao.getAllEmails().size());
		assertEquals(emailDao.getAllEmails().size(), emailDao.getEmailCount());
		assertEquals(emailDao.getAllEmails(), emailDao.getEmailsWithLimitWithoutSorting(0, Integer.MAX_VALUE));
		assertEquals(emailDao.getAllEmails(), emailDao.getEmailsWithLimit(Email.Field.STATUS, Order.ASCENDING, 0, Integer.MAX_VALUE));
	}

//> TEST SETUP/TEARDOWN
	
//> ACCESSORS
	/** @param d The DAO to use for the test. */
	@Required
	public void setEmailDao(EmailDao d)
	{
		// we can just set the DAO once in the test
		this.emailDao = d;
	}

	/** @param d The {@link EmailAccountDao} to use */
	@Required
	public void setEmailAccountDao(EmailAccountDao d) {
		this.emailAccountDao = d;
	}
}
