/**
 * 
 */
package net.frontlinesms.data.repository.hibernate;

import java.util.Collection;
import java.util.List;


import net.frontlinesms.data.DuplicateKeyException;
import net.frontlinesms.data.domain.Contact;
import net.frontlinesms.data.domain.Group;
import net.frontlinesms.data.domain.Keyword;
import net.frontlinesms.data.domain.KeywordAction;
import net.frontlinesms.data.repository.ContactDao;
import net.frontlinesms.data.repository.GroupDao;
import net.frontlinesms.data.repository.GroupMembershipDao;
import net.frontlinesms.data.repository.KeywordActionDao;
import net.frontlinesms.data.repository.KeywordDao;
import net.frontlinesms.events.EventBus;
import net.frontlinesms.junit.HibernateTestCase;
import static org.mockito.Mockito.*;

import org.springframework.beans.factory.annotation.Required;

/**
 * Test class for {@link HibernateGroupDao}
 * @author Alex   Anderson <alex@frontlinesms.com>
 * @author Morgan Belkadi  <morgan@frontlinesms.com>
 */

public class HibernateGroupDaoTest extends HibernateTestCase {
//> PROPERTIES
//	/** Embedded shared test code from InMemoryDownloadDaoTest - Removes need to CopyAndPaste shared test code */
//	private final ReusableGroupDaoTest test = new ReusableGroupDaoTest() { /* nothing needs to be added */ };
	
	private ContactDao contactDao;
	//@Autowired()
	private GroupDao groupDao;
	private GroupMembershipDao groupMembershipDao;
	private KeywordDao keywordDao;
	private KeywordActionDao keywordActionDao;

//> TEST METHODS
	public void testDelete() throws DuplicateKeyException {
		Group 	myGroup  = createGroup("My Group"),
				myGroup2 = createGroup("My Group 2");
		
		Contact george  = createContact("George", myGroup),
				abitbol = createContact("Abitbol", myGroup2);
		
		// Delete
		final int BEFORE_DELETE_GROUP_COUNT = 2;
		groupDao.deleteGroup(myGroup, false);
		assertTrue("Group has not been deleted. Expected: <1> group left, but was: <" + groupDao.getGroupCount() + ">", groupDao.getGroupCount() < BEFORE_DELETE_GROUP_COUNT);
		assertNull("Wrong group has been deleted", groupDao.getGroupByPath(myGroup.getPath()));
		assertTrue("Contact has been deleted and wasn't supposed to", contactDao.getFromMsisdn(george.getPhoneNumber()).equals(george));
		
		
		final int AFTER_FIRST_DELETE_GROUP_COUNT = 1;
		groupDao.deleteGroup(myGroup2, true);
		assertTrue("Group has not been deleted. Expected: <0> groups left, but was: <" + groupDao.getGroupCount() + ">", groupDao.getGroupCount() < AFTER_FIRST_DELETE_GROUP_COUNT);
		assertNull("Wrong group has been deleted", groupDao.getGroupByPath(myGroup.getPath()));
		assertNull("Contact has not been deleted and was supposed to", contactDao.getFromMsisdn(abitbol.getPhoneNumber()));
	}
	
	/** Check that deleting a group's child is successful, and does not affect the group itself. 
	 * @throws DuplicateKeyException */
	public void testChildDelete() throws DuplicateKeyException {
		Group parent = new Group(getRootGroup(), "parent");
		groupDao.saveGroup(parent);
		Group child = new Group(parent, "child");
		groupDao.saveGroup(child);
		
		Collection<Group> fetchedChildren = groupDao.getChildGroups(parent);
		assertEquals(1, fetchedChildren.size());
		assertEquals(child, fetchedChildren.toArray(new Group[0])[0]);

		// Delete the child
		groupDao.deleteGroup(child, false);

		// Confirm that the child has been removed
		fetchedChildren = groupDao.getChildGroups(parent);
		assertEquals(0, fetchedChildren.size());
		Group fetchedChild = groupDao.getGroupByPath("/parent/child");
		assertNull(fetchedChild);
		
		// Confirm that the original group is still present
		assertEquals(parent, groupDao.getGroupByPath("/parent"));
	}

	/** Check that deleting a group's parent has the expected effect on the child groups 
	 * @throws DuplicateKeyException */
	public void testParentDelete() throws DuplicateKeyException {
		Group parent = new Group(getRootGroup(), "parent");
		groupDao.saveGroup(parent);
		Group child = new Group(parent, "child");
		groupDao.saveGroup(child);
		
		// Confirm that the parent and child are both present in the database
		Group fetchedParent = groupDao.getGroupByPath("/parent");
		
		assertEquals(parent, fetchedParent);
		Group fetchedChild = groupDao.getGroupByPath("/parent/child");
		assertEquals(child, fetchedChild);
		
		mock(EventBus.class);
		
		// Delete the parent group
		groupDao.deleteGroup(parent, false);
		//verify(mockEventBus, new Times(2)).notifyObservers(any(FrontlineEventNotification.class));
		
		// Confirm that the parent and child are both deleted from the database
		assertNull(groupDao.getGroupByPath("/parent"));
		assertNull(groupDao.getGroupByPath("/parent/child"));
	}
	
	/** Check that 2 lists cannot be created with the same path 
	 * @throws DuplicateKeyException */
	public void testDuplicateKeys() throws DuplicateKeyException {
		// Test 2 groups at root level
		Group group1 = new Group(getRootGroup(), "name");
		groupDao.saveGroup(group1);
		Group group2 = new Group(getRootGroup(), "name");
		try {
			groupDao.saveGroup(group2);
			fail("Second attempt to create identical group should have failed.");
		} catch(DuplicateKeyException ex) {/* expected */}
		
		// Test 2 child groups
		Group child1 = new Group(group1, "child");
		groupDao.saveGroup(child1);
		Group child2 = new Group(group1, "child");
		try {
			groupDao.saveGroup(child2);
			fail("Second attempt to create identical group should have failed.");
		} catch(DuplicateKeyException ex) {/* expected */}
	}
	
	/**
	 * Check that saving and deleting a group with special SQL characters in its name
	 * functions correctly.  This should verify that our HQL is not open to SQL Injection
	 * issues.
	 * @throws DuplicateKeyException
	 */
	public void testSqlInjection() throws DuplicateKeyException {
		Group myGroup = new Group(getRootGroup(), "Dan's Team");
		groupDao.saveGroup(myGroup);
		groupDao.deleteGroup(myGroup, false);
	}
	
	/**
	 * Test that deleting a group that has an associated {@link KeywordAction} is successful.
	 * @throws DuplicateKeyException 
	 */
	public void testDeleteWithKeywordAction() throws DuplicateKeyException {
		Group myGroup = new Group(getRootGroup(), "My Group");
		groupDao.saveGroup(myGroup);
		
		Keyword keyword = new Keyword("key", "A test keyword");
		keywordDao.saveKeyword(keyword);
		
		KeywordAction action = KeywordAction.createGroupJoinAction(keyword, myGroup, 0, Long.MAX_VALUE);
		keywordActionDao.saveKeywordAction(action);
		
		groupDao.deleteGroup(myGroup, false);
	}
	
	/**
	 * Test deleting a group which has members.
	 * @throws DuplicateKeyException 
	 */
	public void testDeleteWithMembers() throws DuplicateKeyException {
		Group myGroup = new Group(getRootGroup(), "My Group");
		groupDao.saveGroup(myGroup);
		
		Contact contact = new Contact("Alice", "123465789", null, null, null, true);
		contactDao.saveContact(contact);
		
		groupMembershipDao.addMember(myGroup, contact);
		
		groupDao.deleteGroup(myGroup, false);
	}
	
	public void testGroupOrder() throws DuplicateKeyException {
		Group barcelona = createGroup(getRootGroup(), "FC Barcelona");
		Group lyon = createGroup(getRootGroup(), "Olympique Lyonnais");
		Group munchen = createGroup(getRootGroup(), "Bayern Munchen");
		Group milan = createGroup(getRootGroup(), "Inter Milan");
		
		this.groupDao.saveGroup(barcelona);
		this.groupDao.saveGroup(lyon);
		this.groupDao.saveGroup(munchen);
		this.groupDao.saveGroup(milan);
		
		Group[] expectedResult = new Group[] { munchen, barcelona, milan, lyon };
		// We test the order of the "All Groups" request
		this.assertEquals(expectedResult, this.groupDao.getAllGroups());
		// We test the order of the child groups of Root
		this.assertEquals(expectedResult, this.groupDao.getChildGroups(getRootGroup()));
	}
	
	/**
	 * Check if both arrays passed in parameter are identical, order included
	 * @param expectedResult
	 * @param actualResult
	 */
	private void assertEquals(Group[] expectedResult, List<Group> actualResult) {
		assertEquals(expectedResult.length, actualResult.size());
		
		for (int i = 0 ; i < expectedResult.length ; ++i) {
			assertEquals(expectedResult[i], actualResult.get(i));
		}
	}

	private Contact createContact(String name, Group... groups) throws DuplicateKeyException {
		// Make a phone number up that should be unique
		String phoneNumber = Integer.toString(name.hashCode());
		return createContact(name, phoneNumber, groups);
	}
	
	/** Creates a contact with specified name and phone number. 
	 * @throws DuplicateKeyException */
	private Contact createContact(String name, String phoneNumber, Group... groups) throws DuplicateKeyException {
		// Make a phone number up that should be unique
		Contact contact = new Contact(name, phoneNumber, null, null, null, true);
		this.contactDao.saveContact(contact);
		
		for(Group group : groups) {
			this.groupMembershipDao.addMember(group, contact);
		}
		
		return contact;
	}
	
	/** Create a top-level group. */
	private Group createGroup(String name, Contact... contacts) throws DuplicateKeyException {
		return createGroup(getRootGroup(), name, contacts);
	}
	
	/** Create a group with specified parent. */
	private Group createGroup(Group parent, String name, Contact... contacts) throws DuplicateKeyException {
		Group group = new Group(parent, name);
		this.groupDao.saveGroup(group);
		
		for(Contact contact : contacts) {
			this.groupMembershipDao.addMember(group, contact);
		}
		
		return group;
	}
	
//> TEST SETUP/TEARDOWN
	
//> ACCESSORS
	/** @param groupDao The DAO to use for the test. */
	@Required
	public void setGroupDao(GroupDao groupDao) {
		this.groupDao = groupDao;
	}

	@Required
	public void setContactDao(ContactDao contactDao) {
		this.contactDao = contactDao;
	}

	@Required
	public void setGroupMembershipDao(GroupMembershipDao groupMembershipDao) {
		this.groupMembershipDao = groupMembershipDao;
	}

	@Required
	public void setKeywordActionDao(KeywordActionDao keywordActionDao) {
		this.keywordActionDao = keywordActionDao;
	}

	@Required
	public void setKeywordDao(KeywordDao keywordDao) {
		this.keywordDao = keywordDao;
	}
	
	private Group getRootGroup() {
		return new Group(null, null);
	}
}
