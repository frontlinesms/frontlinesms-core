/**
 * 
 */
package net.frontlinesms.data.repository.hibernate;

import java.util.List;

import javax.persistence.Entity;

import net.frontlinesms.data.DuplicateKeyException;
import net.frontlinesms.data.EntityField;
import net.frontlinesms.data.Order;
import net.frontlinesms.data.events.EntityDeletedNotification;
import net.frontlinesms.data.events.EntitySavedNotification;
import net.frontlinesms.data.events.EntityUpdatedNotification;
import net.frontlinesms.data.events.EntityDeleteWarning;
import net.frontlinesms.events.EventBus;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.transform.DistinctRootEntityResultTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * @author Alex
 * @param <E> Entity that this dao is for
 */
public abstract class BaseHibernateDao<E> extends HibernateDaoSupport {
	/** Logging object */
	final Log log = LogFactory.getLog(getClass());
	
	/** Class that this dao deals with. */
	private final Class<E> clazz;
	/** The unqualified name of {@link #clazz} */
	private final String className;
	/** EventNotifier that sends out FrontlineEvents **/
	@Autowired
	private EventBus eventBus;
	
	/**
	 * @param clazz
	 */
	protected BaseHibernateDao(Class<E> clazz) {
		if(clazz.getAnnotation(Entity.class) == null)
			throw new IllegalArgumentException("Cannot create DAO for class " + clazz.getName() +
					" as it is not annotated with " + Entity.class.getName());
		this.clazz = clazz;
		this.className = clazz.getName();
	}
	
	public void setEventBus(EventBus eventBus){
		this.eventBus = eventBus;
	}
	
	protected EventBus getEventBus() {
		return eventBus;
	}
	
	/**
	 * Save an entity, without checking for exceptions thrown for duplicate keys or unique columns.
	 * @param entity entity to save
	 */
	protected void saveWithoutDuplicateHandling(E entity) {
		log.trace("Saving entity: " + entity);
		this.getHibernateTemplate().save(entity);
		log.trace("Entity saved.");
		eventBus.notifyObservers(new EntitySavedNotification<E>(entity));
	}
	
	/**
	 * Saves an entity .
	 * @param entity entity to save 
	 * @throws DuplicateKeyException if there was a {@link ConstraintViolationException} thrown while saving
	 */
	protected void save(E entity) throws DuplicateKeyException {
		try {
			saveWithoutDuplicateHandling(entity);
		} catch(RuntimeException ex) {
			if(isClashOfUniqueColumns(ex)) {
				throw new DuplicateKeyException(ex);
			} else {
				throw ex;
			}
		}
	}
	
	/**
	 * Checks if a {@link Throwable} was caused by a clash of unique items.
	 * @param t {@link Throwable} thrown
	 * @return <code>true</code> if the {@link Throwable} represents a clash of unique column values
	 */
	private boolean isClashOfUniqueColumns(Throwable t) {
		Throwable cause = t.getCause();
		return cause != null
				&& (cause instanceof ConstraintViolationException
						|| cause instanceof NonUniqueObjectException);
	}
	
	/**
	 * Updates an entity. 
	 * @param entity entity to update
	 * @throws DuplicateKeyException if there was a {@link ConstraintViolationException} thrown while updating
	 */
	protected void update(E entity) throws DuplicateKeyException {
		try {
			updateWithoutDuplicateHandling(entity);
		} catch(RuntimeException ex) {
			if(isClashOfUniqueColumns(ex)) {
				throw new DuplicateKeyException(ex);
			} else {
				throw ex;
			}
		}
	}

	/**
	 * Updates an entity, without checking for exceptions thrown for duplicate keys or unique columns. 
	 * @param entity entity to update
	 */
	protected void updateWithoutDuplicateHandling(E entity) {
		log.trace("Updating entity: " + entity);
		this.getHibernateTemplate().update(entity);
		log.trace("Entity updated.");
		eventBus.notifyObservers(new EntityUpdatedNotification<E>(entity));
	}
	
	/**
	 * Deletes an entity. 
	 * @param entity entity to delete
	 */
	protected void delete(E entity) {
		eventBus.notifyObservers(new EntityDeleteWarning<E>(entity));
		log.trace("Deleting entity: " + entity);
		this.getHibernateTemplate().delete(entity);
		log.trace("Entity deleted.");
		eventBus.notifyObservers(new EntityDeletedNotification<E>(entity));
	}
	
	/**
	 * Gets all entities of type {@link #clazz}.
	 * @return list of all entities of type {@link #clazz}
	 */
	protected List<E> getAll() {
		return this.getList(getCriterion());
	}
	
	/**
	 * Gets a list of E matching the supplied criteria.
	 * @param criteria
	 * @return a list of Es matching the supplied criteria
	 */
	@SuppressWarnings("unchecked")
	protected List<E> getList(DetachedCriteria criteria) {
		return this.getHibernateTemplate().findByCriteria(criteria);
	}
	
	/**
	 * Gets a list of E matching the supplied HQL query.
	 * @param hqlQuery HQL query
	 * @param values values to insert into the HQL query
	 * @return a list of Es matching the supplied query
	 */
	@SuppressWarnings("unchecked")
	protected List<E> getList(String hqlQuery, Object... values) {
		return this.getHibernateTemplate().find(hqlQuery, values);
	}
	
	/**
	 * Gets a list of E matching the supplied HQL query.
	 * @param hqlQuery HQL query
	 * @param startIndex the index of the first result object to be retrieved (numbered from 0)
	 * @param limit the maximum number of result objects to retrieve (or <=0 for no limit)
	 * @param values values to insert into the HQL query
	 * @return a list of Es matching the supplied query
	 */
	protected List<E> getList(String hqlQuery, int startIndex, int limit, Object... values) {
		List<E> list = getList(hqlQuery, values);
		if(limit <= 0) {
			return list.subList(startIndex, Integer.MAX_VALUE);
		} else {
			return list.subList(startIndex, Math.min(list.size(), startIndex + limit));
		}
	}
	
	/**
	 * Gets total number of this entity saved in the database.
	 * @return total number of this entity saved in the database
	 */
	protected int countAll() {
		return (int)((Long)this.getHibernateTemplate().find("select count(*) from " + this.className).get(0)).longValue();
	}
	
	/**
	 * Get all entities within a specific range.
	 * @param startIndex index of first entity to fetch
	 * @param limit maximum number of entities to fetch
	 * @return all entities within a specific range
	 */
	protected List<E> getAll(int startIndex, int limit) {
		return this.getList(getCriterion(), startIndex, limit);
	}
	
	/**
	 * Gets a unique result of type E from the supplied criteria.
	 * @param criteria
	 * @return a single E, or <code>null</code> if none was found.
	 */
	@SuppressWarnings("unchecked")
	protected E getUnique(DetachedCriteria criteria) {
		return (E) DataAccessUtils.uniqueResult(this.getList(criteria));
	}
	
	/**
	 * Gets a paged list of {@link #clazz}.
	 * @param criteria
	 * @param startIndex
	 * @param limit
	 * @return paged list fitting the supplied criteria.
	 */
	@SuppressWarnings("unchecked")
	protected List<E> getList(DetachedCriteria criteria, int startIndex, int limit) {
		return this.getHibernateTemplate().findByCriteria(criteria, startIndex, limit);
	}
	
	/**
	 * Gets a {@link DetachedCriteria} to sort by a particular field.
	 * @param sortBy
	 * @param order
	 * @return {@link DetachedCriteria} with order and sort field set
	 */
	protected DetachedCriteria getSortCriterion(EntityField<E> sortBy, Order order) {
		DetachedCriteria criteria = getCriterion();
		if(sortBy != null) {
			criteria.addOrder(order.getHibernateOrder(sortBy.getFieldName()));
		}
		return criteria;
	}
	
	/**
	 * Gets a {@link DetachedCriteria} to sort by a particular field.
	 * @return {@link DetachedCriteria} with order and sort field set
	 */
	protected DetachedCriteria getCriterion() {
		DetachedCriteria criteria = DetachedCriteria.forClass(this.clazz);
		criteria.setResultTransformer(DistinctRootEntityResultTransformer.INSTANCE);
		return criteria;
	}
	
	/**
	 * Gets an equals {@link Criterion} for the supplied field and value.  If the value is <code>null</code>,
	 * a isNull {@link Criterion} is created instead.
	 * @param field
	 * @param value
	 * @return an isNull {@link Criterion} if the value to equal is <code>null</code>, or a an equals {@link Criterion} if the value is not <code>null</code>
	 */
	protected Criterion getEqualsOrNull(EntityField<E> field, Object value) {
		if(value == null) {
			return Restrictions.isNull(field.getFieldName());
		} else {
			return Restrictions.eq(field.getFieldName(), value);
		}
	}
	
	/**
	 * Gets a count of the results for the supplied criteria.
	 * @param criteria
	 * @return number of result rows there are for the supplied criteria.
	 */
	protected int getCount(DetachedCriteria criteria) {
		criteria.setProjection(Projections.rowCount());
		return DataAccessUtils.intResult(this.getHibernateTemplate().findByCriteria(criteria));
	}

	/**
	 * Gets a count of the results for the supplied HQL query string.  The HQL query should
	 * be a COUNT statement.
	 */
	protected int getCount(String queryString, Object... values) {
		List<?> results = getHibernateTemplate().find(queryString, values);
		return DataAccessUtils.intResult(results);
	}
}
