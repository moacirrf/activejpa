/**
 * 
 */
package org.activejpa.entity;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Id;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.PluralAttribute;

import org.activejpa.ActiveJpaException;
import org.activejpa.jpa.JPA;
import org.activejpa.jpa.JPAContext;
import org.apache.commons.beanutils.BeanUtils;



/**
 * <p> Base class for all entities. Embeds entity manager in it and provides a bunch of DAL abstractions to make data access a lot simpler.
 * The static methods are to be implemented by the entity classes and will be done at runtime through activejpa instrumentation.
 * 
 * <p> This allows activerecord style of usage,
 * 
 * <pre> 
 * Person.findById(1L);
 * Person.where("firstName", "Ganesh", "lastName", "Subramanian");
 * Person.count();
 * Person.collection("accounts")
 * 
 * @author ganeshs
 *
 */
public abstract class Model extends BaseObject {
	
	private static final String NIE = "Your models are not instrumented. Make sure you run with -javaagent:activejpa-instrument.jar";
	
	public Model() {
	}
	
	/**
	 * Loads the given attributes to this model
	 * 
	 * @param attributes
	 */
	public void updateAttributes(Map<String, Object> attributes) {
		try {
			BeanUtils.populate(this, attributes);
			persist();
		} catch (Exception e) {
			throw new ActiveJpaException("Failed while updating the attributes", e);
		}
	}
	
	/**
	 * The model identifier. Override and annotate with {@link Id}
	 * 
	 * @return
	 */
	public abstract Serializable getId();
	
	/**
	 * Returns the entity identified by the id
	 * 
	 * @param id
	 * @return
	 */
	public static <T extends Model> T findById(Serializable id) {
		throw new UnsupportedOperationException(NIE);
	}
	
	/**
	 * Returns the total count of rows in the table
	 * 
	 * @return
	 */
	public static long count() {
		throw new UnsupportedOperationException(NIE);
	}
	
	/**
	 * Returns the count of rows matching the given filter
	 * 
	 * @param filter
	 * @return
	 */
	public static long count(Filter filter) {
		throw new UnsupportedOperationException(NIE);
	}
	
	/**
	 * Returns all the rows in the table
	 * 
	 * @return
	 */
	public static <T extends Model> List<T> all() {
		throw new UnsupportedOperationException(NIE);
	}
	
	/**
	 * Deletes all the rows from the table
	 */
	public static void deleteAll() {
		throw new UnsupportedOperationException(NIE);
	}
	
	/**
	 * Deletes the rows matching the given filter
	 * 
	 * @param filter
	 */
	public static void deleteAll(Filter filter) {
		throw new UnsupportedOperationException(NIE);
	}
	
	/**
	 * Checks if an entity exists with the given id
	 * 
	 * @param id
	 * @return
	 */
	public static boolean exists(Serializable id) {
		throw new UnsupportedOperationException(NIE);
	}
	
	/**
	 * Returns a list of entities matching the given key value pairs. The key value pairs are supplied as arguments like (key1, value1, key2, value2)
	 * 
	 * @param paramValues
	 * @return
	 */
	public static <T extends Model> List<T> where(Object... paramValues) {
		throw new UnsupportedOperationException(NIE);
	}
	
	/**
	 * Returns a list of entities matching the given filter
	 * 
	 * @param filter
	 * @return
	 */
	public static <T extends Model> List<T> where(Filter filter) {
		throw new UnsupportedOperationException(NIE);
	}
	
	/**
	 * Returns a single row matching the given key value pairs. The key value pairs are supplied as arguments like (key1, value1, key2, value2)
	 * 
	 * <p> If more than one row matches or no row matches, throws an exception
	 * 
	 * @param paramValues
	 * @return
	 * @throws NoResultException
	 * @throws NonUniqueResultException
	 */
	public static <T extends Model> T one(Object... paramValues) {
		throw new UnsupportedOperationException(NIE);
	}
	
	/**
	 * Returns the first row matching the given key value pairs. The key value pairs are supplied as arguments like (key1, value1, key2, value2)
	 * 
	 * @param paramValues
	 * @return
	 */
	public static <T extends Model> T first(Object... paramValues) {
		throw new UnsupportedOperationException(NIE);
	}
	
	/**
	 * Starts the transaction if its not active already. Returns back the new transaction or existing active one.
	 * 
	 * @return
	 */
	public static EntityTransaction beginTxn() {
		JPA.instance.getDefaultConfig().getContext().beginTxn();
		return getEntityManager().getTransaction();
	}
	
	protected static <T extends Model> T findById(Class<T> clazz, Serializable id) {
		return getEntityManager().find(clazz, id);
	}
	
	protected static <T extends Model> T one(Class<T> clazz, Object... paramValues) {
		return createQuery(clazz, paramValues).getSingleResult();
	}
	
	protected static <T extends Model> T first(Class<T> clazz, Object... paramValues) {
		List<T> list = where(clazz, paramValues);
		if (list != null && ! list.isEmpty()) {
			return list.get(0);
		}
		return null;
	}
	
	protected static <T extends Model> List<T> where(Class<T> clazz, Object... paramValues) {
		return createQuery(clazz, paramValues).getResultList();
	}
	
	protected static <T extends Model> List<T> where(Class<T> clazz, Filter filter) {
		return createQuery(clazz, filter).getResultList();
	}
	
	protected static <T extends Model> long count(final Class<T> clazz) {
		return count(clazz, new Filter());
	}
	
	protected static <T extends Model> long count(final Class<T> clazz, Filter filter) {
		CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
		CriteriaQuery<Long> cQuery = builder.createQuery(Long.class);
		Root<T> root = cQuery.from(clazz);
		cQuery.select(builder.count(root));
		filter.constructQuery(builder, cQuery, root);
		TypedQuery<Long> query = getEntityManager().createQuery(cQuery);
		filter.setParameters(query);
		return query.getSingleResult();
	}
	
	protected static <T extends Model> List<T> all(Class<T> clazz) {
		return getEntityManager().createQuery("from " + clazz.getSimpleName(), clazz).getResultList();
	}
	
	protected static <T extends Model> void deleteAll(Class<T> clazz) {
		deleteAll(clazz, new Filter());
	}
	
	protected static <T extends Model> void deleteAll(Class<T> clazz, Filter filter) {
		StringWriter writer = new StringWriter();
		writer.append("delete from ").append(clazz.getSimpleName());
		if (! filter.getConditions().isEmpty()) {
			writer.append(" where ").append(filter.constructQuery());
		}
		Query query = getEntityManager().createQuery(writer.toString());
		filter.setParameters(query);
		query.executeUpdate();
	}
	
	protected static <T extends Model> boolean exists(Class<T> clazz, Serializable id) {
		return findById(clazz, id) != null;
	}
	
	/**
	 * Save this entity to the persistence context
	 */
	public void persist() {
		execute(new Executor<Void>() {
			@Override
			public Void execute(EntityManager manager) {
				manager.persist(Model.this);
				return null;
			}
		}, false);
	}
	
	/**
	 * Delete this entity from the persistence context
	 */
	public void delete() {
		execute(new Executor<Void>() {
			@Override
			public Void execute(EntityManager manager) {
				manager.remove(Model.this);
				return null;
			}
		}, false);
	}
	
	/**
	 * Merge this entity with the one from the persistence context
	 */
	public void merge() {
		execute(new Executor<Void>() {
			@Override
			public Void execute(EntityManager manager) {
				manager.merge(Model.this);
				return null;
			}
		}, false);
	}
	
	/**
	 * Reload this entity from the persistence context
	 */
	public void refresh() {
		execute(new Executor<Void>() {
			@Override
			public Void execute(EntityManager manager) {
				manager.refresh(Model.this);
				return null;
			}
		}, true);
	}
	
	/**
	 * Returns the collection object identified by the given name
	 * 
	 * @param name
	 * @return
	 */
	public <T extends Model> EntityCollection<T> collection(String name) {
		ManagedType<? extends Model> type = getEntityManager().getMetamodel().managedType(getClass());
		Class<T> elementType = null;
		if (type.getAttribute(name).isCollection()) {
			elementType = ((PluralAttribute)type.getAttribute(name)).getElementType().getJavaType();
		} else {
			// Throw exception
			return null;
		}
		return new EntityCollection<T>(this, name, elementType);
	}
	
	protected static <T> T execute(Executor<T> executor, boolean readOnly) {
		JPAContext context = JPA.instance.getDefaultConfig().getContext();
		boolean beganTxn = false;
		if (! context.isTxnOpen()) {
			context.beginTxn();
			beganTxn = true;
		}
		try {
			return executor.execute(getEntityManager());
		} finally {
			if (beganTxn) {
				context.closeTxn(readOnly);
			}
		}
	}
	
	private static interface Executor<T> {
		
		T execute(EntityManager manager);
	}
}