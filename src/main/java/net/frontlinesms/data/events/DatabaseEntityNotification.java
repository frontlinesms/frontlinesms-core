package net.frontlinesms.data.events;

import net.frontlinesms.events.FrontlineEventNotification;

/**
 * A superclass for notifications involving database entities
 * @param <E> database entity to which the event refers
 * @author Dieterich Lawson <dieterich@medic.frontlinesms.com>
 * @author Alex Anderson <alex@frontlinesms.com>
 */
public abstract class DatabaseEntityNotification<E> implements FrontlineEventNotification {

	/**
	 * The object that this notification is about, which has presumably just been updated
	 */
	protected E databaseEntity;
	
	public DatabaseEntityNotification(E databaseEntity){
		this.databaseEntity = databaseEntity;
	}
	
	public E getDatabaseEntity(){
		return databaseEntity;
	}
}
